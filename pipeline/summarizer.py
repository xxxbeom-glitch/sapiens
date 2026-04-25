"""
기사 요약·시황·기업 분석: Google Gemini API (google-genai).
"""
from __future__ import annotations

import json
import logging
import os
import re
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Literal

from google import genai
from google.genai import types

logger = logging.getLogger(__name__)

# 제품 기본: Gemini 3.1 Flash-Lite (`generateContent`·텍스트/JSON 요약).
# 다른 모델: 환경변수 GEMINI_MODEL.
_DEFAULT_GEMINI_MODEL = "gemini-3.1-flash-lite-preview"

# SAPIENS_ARTICLE_LLM_JUDGE=1: 기사 JSON 요약을 [단일 LLM 호출·재시도 2회]만 사용.
# 미설정·0이면 _call_news_llm 기반 2회 재시도 경로.
def _article_llm_judge_enabled() -> bool:
    v = (os.environ.get("SAPIENS_ARTICLE_LLM_JUDGE") or "").strip().lower()
    return v in ("1", "true", "yes", "on")


def _gemini_model_id() -> str:
    return (os.environ.get("GEMINI_MODEL") or _DEFAULT_GEMINI_MODEL).strip() or _DEFAULT_GEMINI_MODEL


SELECTED_MODEL_CLAUDE = "claude"
SELECTED_MODEL_GEMINI = "gemini"

_RUNTIME_SELECTED_MODEL = SELECTED_MODEL_GEMINI

_token_input_total: int = 0
_token_output_total: int = 0
_token_model: str = ""
_token_usage_lock = threading.Lock()
_gemini_client: Any = None
_gemini_client_init_lock = threading.Lock()
SYSTEM = (
    "당신은 한국 투자자를 위한 금융 뉴스 에디터입니다.\n"
    "뉴스를 분석해서 핵심 내용을 명확하고 구체적으로 요약하세요.\n"
    "반드시 JSON 형식으로만 응답하세요."
)


_HANGUL_RE = re.compile(r"[가-힣]")
_LATIN_RE = re.compile(r"[A-Za-z]")

# 기사 summary_points: 앱/프롬프트·후처리 동기화. 각 항목은 70자 이내·완결된 한 문장(종결)을 권장·유도.
SUMMARY_POINT_MIN_LEN = 30
SUMMARY_POINT_MAX_LEN = 70

# 70자 이하·문장 '완결' 판별용(긴 어미·구두 먼저; 마침표 있음 우선)
_KO_POINT_CLOSERS: tuple[str, ...] = (
    "습니다.",
    "입니다.",
    "합니다.",
    "된다.",
    "있다.",
    "없다.",
    "했다.",
    "인다.",
    "였다.",
    "었다.",
    "어요.",
    "에요.",
    "이에요.",
    "죠.",
    "지요.",
    "잖아요.",
    "다.",
    "요.",
    "음.",
    "임.",
)


def _korean_bullet_looks_ended(s: str) -> bool:
    t = s.rstrip()
    if not t or len(t) < 2:
        return False
    for clo in _KO_POINT_CLOSERS:
        if t.endswith(clo):
            return True
    return bool(t) and t[-1] in (".", "!", "?", "…", "‥", "。．")


# 종결(다.)이 앞 70자에 없을 때, '절·어구' 끊김(연결어미·쉼표). min_j: rfind 인덱스 하한(너무 앞에서 자르지 않음)
_CLAUSE_CUT_TOKENS: tuple[tuple[str, int], ...] = (
    ("하며", 8),
    ("이며", 8),
    ("라며", 8),
    ("거나", 5),
    ("는데", 4),
    ("다가", 4),
    ("으면", 4),
    ("으나", 4),
    (", ", 18),
    (",", 20),
    ("·", 16),
    ("、", 16),
)


def _clip_at_last_space_before_max(s0: str, max_len: int) -> str | None:
    """[min_tok, max) 구간의 마지막 공백(어구)에서 자름. 단어/자모 중간 절단을 줄이기 위함."""
    if max_len < 2:
        return None
    win = s0[:max_len]
    for lo in (40, 32, 24, 18):
        if lo >= max_len - 1:
            continue
        j = win.rfind(" ", lo, max_len)
        if j >= lo:
            t = s0[:j].rstrip()
            if len(t) >= 20:
                return t
    return None


def _clip_at_subordinate_or_punct(
    s0: str, win: str, max_len: int
) -> str | None:
    best_e = 0
    for tok, min_j in _CLAUSE_CUT_TOKENS:
        j = win.rfind(tok)
        if j < min_j:
            continue
        end = j + len(tok)
        if 20 <= end <= max_len and end > best_e:
            best_e = end
    if best_e < 20:
        return None
    return s0[:best_e].rstrip()


def _finish_summary_point(
    text: str,
    *,
    max_len: int = SUMMARY_POINT_MAX_LEN,
) -> str:
    """
    각 요약 항목을 `max_len`자 이하로 맞추되, **가능하면 70자 안에서 문장이 끊기도록**(종결 어미) 맞춘다.
    모델이 70을 넘긴 경우, 앞에서부터 가장 긴 '완결' 접두를 택한다.
    """
    s0 = (text or "").strip()
    if not s0:
        return s0
    if len(s0) <= max_len and _korean_bullet_looks_ended(s0):
        return s0
    if len(s0) <= max_len:
        logger.info(
            "summary_point: %d자 이하이나 종결 어미가 약함(권장: 다/요/니다/습니다 등으로 끝냄): %r",
            len(s0),
            s0[:70],
        )
        return s0
    for i in range(max_len, 19, -1):
        t = s0[:i].rstrip()
        if 20 <= len(t) <= max_len and _korean_bullet_looks_ended(t):
            if len(s0) > max_len and len(s0) != len(t):
                logger.info(
                    "summary_point: %d자 → 완결 접두 %d자: 앞 45자=%r",
                    len(s0),
                    len(t),
                    t[:45],
                )
            return t
    # 종결 접두를 못 찾을 때: 70자 안의 마지막 뉴스식 구절끝(·다.·니다.·어요. …)에서 잘라 완성도 우선
    win = s0[:max_len]
    best_e = 0
    for tok, min_j in (
        (".\n", 0),
        ("다.", 12),
        ("니다.", 8),
        ("습니다.", 6),
        ("어요.", 8),
        ("에요.", 8),
        ("이에요.", 5),
        ("죠.", 6),
        ("요.", 4),
    ):
        j = win.rfind(tok)
        if j >= min_j and j + len(tok) > best_e:
            best_e = j + len(tok)
    if best_e >= 20:
        t = s0[:best_e].rstrip()
        if len(t) <= max_len:
            logger.info(
                "summary_point: %d자 → 구절끝 토막 %d자: %r",
                len(s0),
                len(t),
                t[:50],
            )
            return t
    t2 = _clip_at_subordinate_or_punct(s0, win, max_len)
    if t2 and 20 <= len(t2) <= max_len:
        logger.info(
            "summary_point: %d자 → 연결/쉼표·가운뎃점에서 %d자: %r",
            len(s0),
            len(t2),
            t2[:50],
        )
        return t2
    t3 = _clip_at_last_space_before_max(s0, max_len)
    if t3 and 20 <= len(t3) <= max_len:
        logger.info(
            "summary_point: %d자 → 어구 끝(공백)에서 %d자: %r",
            len(s0),
            len(t3),
            t3[:50],
        )
        return t3
    logger.info(
        "summary_point: %d자, 70자에서 구절·공백 없이 하드 절단(앞 55자=%r)",
        len(s0),
        s0[:55],
    )
    return s0[:max_len].rstrip()


# summarize_article JSON category — 국내·해외 동일. 앱 ChipColors·프롬프트와 동기화.
_CANONICAL_ARTICLE_CATEGORIES: frozenset[str] = frozenset(
    {
        "경제",
        "테크&반도체",
        "증시",
        "정치",
        "국제",
        "부동산",
        "산업",
        "사회",
        "빅테크",
        "암호화폐",
    }
)

# 모델이 구 라벨을 쓸 때 정규화(표기 통일).
_ARTICLE_CATEGORY_ALIASES: dict[str, str] = {
    "it": "테크&반도체",
    "it·테크": "테크&반도체",
    "it 테크": "테크&반도체",
    "테크·반도체": "테크&반도체",
    "금융": "증시",
    "증권": "증시",
    "원자재": "증시",
    "채권": "증시",
    "매크로": "경제",
}

# 미국증시·AI 이슈(해외 RSS) 요약 시에만 프롬프트에 삽입. 국내증시(domestic_market)는 제외.
_FOREIGN_RSS_NEWS_FEEDS: frozenset[str] = frozenset({"global_market", "ai_issue"})
_FOREIGN_RSS_KO_FORMAL_BLOCK = (
    "【해외 RSS】영문 출처 기사입니다. 회사·브랜드·제품·서비스·모델·인물·매체명은 "
    "**한국어 정식 명칭**이나 국내 금융·테크 뉴스에서 통용되는 한글 표기를 쓰세요 "
    "(예: 마이크로소프트, 엔비디아, 오픈AI, 앤스로픽, 구글, 애플, 메타, 스탠더드앤드푸어스 500). "
    "원문 영문 표기를 그대로 베끼지 마세요. 어색한 순수 음역(예: 젬나이)보다 통용 한글명을 택하세요. "
    "아래 구체성 규칙의 ‘원문 표기’는 이 【해외 RSS】 고유명 규칙보다 우선하지 않습니다. "
    "headline·summary_points 모두 자연스러운 한국어 문장으로 작성하세요.\n"
)

_SUMMARIZE_ARTICLE_CATEGORY_BLOCK = (
    "category는 반드시 다음 중 **정확히 표기된 문자열 하나**만 사용하세요: "
    "경제, 테크&반도체, 증시, 정치, 국제, 부동산, 산업, 사회, 빅테크, 암호화폐\n"
    "분류 가이드:\n"
    "- 금리, 환율, 매크로, 통화정책, 중앙은행, 물가·고용, 거시지표 등 → **경제**\n"
    "- IT, 소프트웨어, 반도체, AI, 데이터센터, 칩·파운드리 등(초대형 플랫폼 **기업 자체** 이슈가 아닐 때) → **테크&반도체**\n"
    "- 원자재, 채권, 증권, 주식시장, 지수, 거래소·종목 일반 → **증시**\n"
    "- 비트코인, 이더리움, 블록체인, 가상자산·코인 규제 등 → **암호화폐**\n"
    "- 애플, 구글(알파벳), 메타, 아마존, 마이크로소프트 등 **빅테크 기업** 중심 뉴스 → **빅테크**\n"
)


def _normalize_article_category(raw: str) -> str:
    s = (raw or "").strip()
    if s in _CANONICAL_ARTICLE_CATEGORIES:
        return s
    mapped = _ARTICLE_CATEGORY_ALIASES.get(s) or _ARTICLE_CATEGORY_ALIASES.get(s.replace(" ", ""))
    if mapped:
        return mapped
    if s:
        logger.warning("summarize_article: 허용 목록 밖 category %r — 경제로 대체", s)
    return "경제"


def _contains_hangul(text: str) -> bool:
    return bool(_HANGUL_RE.search(text or ""))


def _looks_english_headline(text: str) -> bool:
    s = (text or "").strip()
    if not s:
        return False
    return bool(_LATIN_RE.search(s)) and not _contains_hangul(s)


def _preprocess_llm_json_blob(text: str | None) -> str:
    """
    모든 json.loads 시도 전 공통 전처리.
    strip → ```json / ``` 마크다운 펜스 제거 → 첫 `{`~마지막 `}` 슬라이스.
    """
    if text is None:
        return ""
    t = str(text).strip()
    if not t:
        return ""
    t = re.sub(r"^```json\s*", "", t, flags=re.IGNORECASE)
    t = re.sub(r"^```\s*", "", t)
    t = re.sub(r"\s*```$", "", t)
    t = t.strip()
    start = t.find("{")
    end = t.rfind("}")
    if start != -1 and end != -1 and end >= start:
        t = t[start : end + 1]
    return t


def _json_object_slice(s: str) -> str | None:
    """본문 앞뒤 잡음 제거용: 첫 `{`~마지막 `}` 구간."""
    i = s.find("{")
    j = s.rfind("}")
    if 0 <= i < j:
        return s[i : j + 1]
    return None


# 비정상 입력(거대 한 줄)에 방어. 잘림 보완용 `]`·`}` 추가 상한.
_MAX_TRAILING_BRACKET_CLOSES = 128


def _count_json_brackets_outside_strings(text: str) -> tuple[int, int, int, int]:
    """
    문자열·이스케이프 밖에서만 `{` `}` `[` `]` 개수.
    반환: (open_curly, close_curly, open_square, close_square)
    """
    oc = cc = osq = csq = 0
    in_str = False
    esc = False
    for ch in text:
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if ch == '"':
            in_str = True
        elif ch == "{":
            oc += 1
        elif ch == "}":
            cc += 1
        elif ch == "[":
            osq += 1
        elif ch == "]":
            csq += 1
    return oc, cc, osq, csq


def _try_repair_truncated_json_dict(core: str) -> dict[str, Any] | None:
    """
    잘린 JSON 객체 복구: 문자열 밖에서 (열린 [ − 닫힌 ])만큼 `]` 추가,
    (열린 { − 닫힌 })만큼 `}` 추가 후 json.loads. 순서: ] 먼저, } 나중.
    """
    t = _preprocess_llm_json_blob(core)
    if not t.startswith("{"):
        return None
    try:
        parsed0: Any = json.loads(t)
        if isinstance(parsed0, dict):
            return parsed0
    except json.JSONDecodeError:
        pass
    oc, cc, osq, csq = _count_json_brackets_outside_strings(t)
    need_sq = min(max(0, osq - csq), _MAX_TRAILING_BRACKET_CLOSES)
    need_br = min(max(0, oc - cc), _MAX_TRAILING_BRACKET_CLOSES)
    if need_sq == 0 and need_br == 0:
        return None
    candidate = t + ("]" * need_sq) + ("}" * need_br)
    try:
        parsed: Any = json.loads(candidate)
        if isinstance(parsed, dict):
            logger.info("JSON 파싱: 잘림 보완 성공 — ]+%d }+%d", need_sq, need_br)
            return parsed
    except json.JSONDecodeError:
        pass
    return None


def _extract_json(text: str) -> dict[str, Any] | str:
    """
    JSON 객체 파싱 시도.
    1) 공통 전처리 후 전체 문자열 json.loads
    2) 첫 `{` 이후: `]`·`}` 균형 보완 후 json.loads
    3) 첫 `{`~마지막 `}` 슬라이스 후 동일 보완
    성공 시 dict. 실패 시 원문 처리 문자열 반환(호출부에서 dict 여부로 분기).
    """
    s = _preprocess_llm_json_blob(text)
    if not s:
        return ""
    try:
        parsed: Any = json.loads(s)
        if isinstance(parsed, dict):
            return parsed
    except json.JSONDecodeError:
        pass
    i = s.find("{")
    if i >= 0:
        from_first = s[i:].strip()
        fixed = _try_repair_truncated_json_dict(from_first)
        if fixed is not None:
            return fixed
    fixed = _try_repair_truncated_json_dict(s.strip())
    if fixed is not None:
        return fixed
    sub = _json_object_slice(s)
    if sub:
        try:
            parsed = json.loads(sub)
            if isinstance(parsed, dict):
                return parsed
        except json.JSONDecodeError:
            pass
        fixed_sub = _try_repair_truncated_json_dict(sub.strip())
        if fixed_sub is not None:
            return fixed_sub
    return s


def _require_json_dict(text: str) -> dict[str, Any]:
    """모델 응답에서 JSON 객체만 허용. 파싱 실패 시 로그 후 ValueError."""
    parsed = _extract_json(text)
    if isinstance(parsed, dict):
        return parsed
    if isinstance(parsed, str) and not parsed.strip():
        raise ValueError("empty model response text (JSON expected)")
    raw = parsed if isinstance(parsed, str) else str(parsed)
    logger.warning("JSON 파싱 실패 — 응답 앞 500자: %s", raw[:500])
    raise ValueError("model response was not valid JSON object")


def configure_ai(*, selected_model: str) -> None:
    """main.py에서 Firebase settings/ai_config 읽은 뒤 호출."""
    global _RUNTIME_SELECTED_MODEL
    s = (selected_model or "").strip().lower()
    if s == SELECTED_MODEL_CLAUDE:
        _RUNTIME_SELECTED_MODEL = SELECTED_MODEL_CLAUDE
    else:
        _RUNTIME_SELECTED_MODEL = SELECTED_MODEL_GEMINI
    logger.info(
        "summarizer AI runtime: selected_model=%s gemini_model=%s",
        _RUNTIME_SELECTED_MODEL,
        _gemini_model_id(),
    )


def reset_token_counters() -> None:
    global _token_input_total, _token_output_total, _token_model
    with _token_usage_lock:
        _token_input_total = 0
        _token_output_total = 0
        _token_model = ""


def get_token_usage() -> dict[str, Any]:
    with _token_usage_lock:
        return {
            "input": _token_input_total,
            "output": _token_output_total,
            "total": _token_input_total + _token_output_total,
            "model": _token_model,
        }


def _append_model_label(label: str) -> None:
    global _token_model
    s = (label or "").strip()
    if not s:
        return
    parts = [p.strip() for p in _token_model.split(",") if p.strip()] if _token_model else []
    if s not in parts:
        parts.append(s)
        _token_model = ",".join(parts)


def _usage_meta_get_int(obj: Any, *names: str) -> int:
    if obj is None:
        return 0
    if isinstance(obj, dict):
        for n in names:
            v = obj.get(n)
            if v is not None:
                try:
                    return int(v)
                except (TypeError, ValueError):
                    pass
        return 0
    for n in names:
        v = getattr(obj, n, None)
        if v is not None:
            try:
                return int(v)
            except (TypeError, ValueError):
                pass
    return 0


def _accumulate_gemini_usage(response: Any) -> None:
    global _token_input_total, _token_output_total
    um = getattr(response, "usage_metadata", None)
    if um is None:
        return
    inp = _usage_meta_get_int(um, "prompt_token_count", "prompt_tokens")
    out = _usage_meta_get_int(um, "candidates_token_count", "candidates_tokens", "output_token_count")
    with _token_usage_lock:
        _token_input_total += max(0, inp)
        _token_output_total += max(0, out)
        _append_model_label(_gemini_model_id())


def _ai_any_news_enabled() -> bool:
    return True


def _call_news_llm(user_content: str) -> str:
    """뉴스/시황/큐레이션 — Gemini."""
    if not _ai_any_news_enabled():
        return ""
    return str(_call_gemini(user_content) or "").strip()


def classify_kr_headline_bucket(
    row: dict[str, Any],
) -> tuple[Literal["domestic_stock", "domestic_economy", "none"], float]:
    """
    RSS 한 줄(제목·요약·가능하면 본문)만 보고, 국내 '증시' / '경제' 중 어디에 더 맞는지 판정.
    키워드 누락(표기/동음이의어 등) 보강용 — 호출 측에서 budget 을 둡니다.
    """
    if not _ai_any_news_enabled():
        return "none", 0.0

    title = str(row.get("title") or "").strip()
    summary = str(row.get("summary") or "").strip()
    body = str(row.get("body") or "").strip()
    if len(body) > 6000:
        body = body[:6000] + "…"

    if not (title or summary or body):
        return "none", 0.0

    user = (
        "아래 기사(제목+요약+가능한 본문)를 읽고, '국내 증시'와 '국내 경제(거시/정책/물가/고용/무역/환율/부동산정책/실물경제)'\n"
        "중에서 더 잘 맞는 한 가지로 분류하세요.\n"
        "- domestic_stock: 상장기업, 실적, 목표가, 밸류에이션, 국내 주식/지수/ETF, 증권·거래제도, 공매도/공시 등 '주식·증시' 중심\n"
        "- domestic_economy: 한은/금리, CPI/GDP, 고용, 재정, 세금, 대외/무역, 가계/기업/부채, 정책(부동산/규제 포함) 등 '경제' 중심\n"
        "둘 다에 해당해도, 더 강한 축을 하나만 고르세요. 뉴스가 둘 다에 해당 없으면 none.\n\n"
        f"title:\n{title}\n\nsummary:\n{summary}\n\nbody:\n{body}\n\n"
        "반드시 JSON 한 개만(코드펜스/주석/설명 없이) 출력:\n"
        '{"bucket":"domestic_stock"|"domestic_economy"|"none","confidence":0.0~1.0}'
    )

    raw = str(_call_gemini(user, temperature=0.15) or "").strip()
    if not raw:
        return "none", 0.0
    # 모델이 ```json``` 이나 잡담을 앞/뒤에 붙이는 경우 — 마지막 {..} 쪽을 우선 파싱
    try:
        data = _require_json_dict(raw)
    except Exception:
        m = re.search(r"\{[\s\S]*\}\s*$", raw)
        if not m:
            return "none", 0.0
        try:
            data = json.loads(m.group(0))
        except Exception:
            return "none", 0.0

    b = str(data.get("bucket", "")).strip().lower()
    if b in ("stock", "kr_stock", "korea_stock", "kospi", "kosdaq"):
        b = "domestic_stock"
    if b in ("economy", "macro", "korea_economy"):
        b = "domestic_economy"
    if b not in ("domestic_stock", "domestic_economy", "none"):
        b = "none"
    try:
        conf = float(data.get("confidence", 0.0))
    except (TypeError, ValueError):
        conf = 0.0
    if conf < 0.0:
        conf = 0.0
    if conf > 1.0:
        conf = 1.0
    return b, conf  # type: ignore[return-value]


def _get_gemini_client() -> Any:
    global _gemini_client
    if _gemini_client is not None:
        return _gemini_client
    with _gemini_client_init_lock:
        if _gemini_client is not None:
            return _gemini_client
        key = (os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY") or "").strip()
        if not key:
            raise RuntimeError("GEMINI_API_KEY 또는 GOOGLE_API_KEY 가 설정되어 있어야 합니다.")
        _gemini_client = genai.Client(api_key=key)
        return _gemini_client


def _call_gemini(user_content: str, *, temperature: float = 0.35) -> str:
    try:
        client = _get_gemini_client()
        model_id = _gemini_model_id()
        response = client.models.generate_content(
            model=model_id,
            contents=user_content,
            config=types.GenerateContentConfig(
                system_instruction=SYSTEM,
                max_output_tokens=8192,
                temperature=temperature,
            ),
        )
        try:
            _accumulate_gemini_usage(response)
        except Exception as e:
            logger.warning("Gemini usage 집계 실패(무시): %s", e)
        text = (getattr(response, "text", None) or "").strip()
        return text
    except Exception as e:
        logger.exception("_call_gemini 실패: %s", e)
        return ""


def _validate_summarize_article_shape(data: dict[str, Any]) -> None:
    if "headline" not in data or "category" not in data or "summary_points" not in data:
        raise ValueError(f"Invalid JSON keys: {list(data.keys())}")
    pts = data["summary_points"]
    if not isinstance(pts, list):
        raise ValueError("summary_points must be a list")


def _postprocess_summarize_article_dict(
    data: dict[str, Any],
    max_points_keep: int,
    title: str,
    source: str,
    summary: str,
    body_stripped: str,
    *,
    news_feed: str | None = None,
) -> dict[str, Any]:
    out = dict(data)
    out["headline"] = str(out.get("headline", "")).strip()
    foreign = news_feed in _FOREIGN_RSS_NEWS_FEEDS
    headline_has_latin = bool(_LATIN_RE.search(out["headline"]))
    if _looks_english_headline(out["headline"]) or (foreign and headline_has_latin):
        out["headline"] = translate_headline_to_korean(
            headline=out["headline"],
            source=source,
            summary=summary,
            body=body_stripped,
            formal_korean_names=foreign,
        )
    out["category"] = _normalize_article_category(str(out.get("category", "")))
    pts = out["summary_points"]
    if not isinstance(pts, list):
        raise ValueError("summary_points must be a list")
    cleaned: list[str] = []
    for p in pts:
        t = str(p).strip()
        if not t:
            continue
        t2 = _finish_summary_point(t)
        if t2:
            cleaned.append(t2)
    out["summary_points"] = cleaned[:max_points_keep]
    return out


def _summarize_article_claude_judge_once(
    user: str,
    *,
    title: str,
    source: str,
    summary: str,
    body_stripped: str,
    max_points_keep: int,
    news_feed: str | None = None,
) -> dict[str, Any]:
    """
    SAPIENS_ARTICLE_LLM_JUDGE=1: 단일 LLM 호출·JSON 파싱 후 반환(일반 2회 재시도 루프는 생략).
    JSON·스키마 오류 시 최대 2회까지 동일 프롬프트로 재시도.
    """
    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_gemini(user)
            if not (raw or "").strip():
                raise ValueError("empty Gemini response")
            data = _require_json_dict(raw)
            _validate_summarize_article_shape(data)
            out = _postprocess_summarize_article_dict(
                data,
                max_points_keep,
                title,
                source,
                summary,
                body_stripped,
                news_feed=news_feed,
            )
            out["_quality"] = {"path": "gemini_judge_once", "attempts": attempt + 1}
            return out
        except Exception as e:
            last_err = e
            logger.warning("Gemini(저지 1경로) 요약 재시도 (%s/2): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(0.5)
    raise last_err or RuntimeError("gemini_judge_once summarize failed")


NewsFeedId = Literal["domestic_market", "global_market", "ai_issue"]


def summarize_article(
    title: str,
    summary: str,
    source: str,
    body: str = "",
    *,
    news_feed: NewsFeedId | None = None,
) -> dict[str, Any]:
    """
    반환:
    headline, category, summary_points
    """
    body_stripped = (body or "").strip()
    summary_stripped = (summary or "").strip()

    if not _ai_any_news_enabled():
        logger.info("summarize_article: AI off — 원문 폴백용 스텁")
        return {"headline": "", "category": "", "summary_points": []}

    if body_stripped:
        article_block = (
            f"제목: {title}\n"
            f"기사 본문 (요약의 주된 근거):\n{body_stripped}\n"
        )
        if summary_stripped:
            article_block += f"목록/리드용 짧은 설명 (보조 참고): {summary_stripped}\n"
    else:
        article_block = f"제목: {title}\n목록/리드 요약문:\n{summary_stripped}\n"

    max_points_keep = 10
    foreign = news_feed in _FOREIGN_RSS_NEWS_FEEDS
    foreign_rss_extra = _FOREIGN_RSS_KO_FORMAL_BLOCK if foreign else ""
    proper_noun_rule = (
        "- 기업명·종목·브랜드·인물·매체명은 **한국어 정식 명칭** 또는 국내 금융·테크 뉴스 관례적 한글 표기로 쓸 것. "
        "원문 영문 표기를 그대로 베끼지 말 것.\n"
        if foreign
        else "- 기업명, 종목명, 브랜드명, 인물명은 원문에 나온 표기를 그대로 포함할 것.\n"
    )
    closing_foreign = (
        "**최종 확인(해외 RSS):** headline·summary_points 안의 고유명은 한글(정식·통용)만 쓰세요. "
        "영문 회사·제품명만 남기지 마세요.\n\n"
        if foreign
        else ""
    )
    user = (
        f"다음 뉴스를 분석해 JSON만 출력하세요.\n"
        f'키: "headline", "category", "summary_points"\n'
        f"headline·summary_points 각 문자열 안에 **큰따옴표(\")를 넣지 마세요**. "
        f"인용이 필요하면 작은따옴표(')나 「」를 쓰세요.\n"
        f"headline은 반드시 한국어로 작성하세요. 원문이 영어면 자연스러운 한국어 제목으로 번역하세요.\n"
        f"{foreign_rss_extra}"
        f"{_SUMMARIZE_ARTICLE_CATEGORY_BLOCK}"
        f"summary_points는 기사 핵심을 빠짐없이 담은 문자열 배열로 작성하세요.\n"
        f"반드시 2개 이상 4개 이하로 작성하세요. "
        f"각 summary_point는 **딱 한 문장**이며(절·콤마로 이어 쓰지 말 것), {SUMMARY_POINT_MIN_LEN}자 이상 {SUMMARY_POINT_MAX_LEN}자 이하(공백·구두점 포함)에서 "
        f"문장·어미(다/요/니다/습니다/어요 등)로 **반드시 끝맺을 것**. "
        f"{SUMMARY_POINT_MAX_LEN}자 중간이나 '…'로 끝내지 말 것. 쉼표 뒤에 절이 더 이어질 정도로 길게 쓰지 말 것(한 문장으로 끊을 것).\n"
        f"중요한 사실·수치·배경·영향을 누락하지 말고, 내용을 함축하거나 생략하지 마세요.\n"
        f"구체성 규칙(headline·summary_points 모두 적용):\n"
        f"{proper_noun_rule}"
        f"- 주가, 등락률·퍼센트, 금액 등 구체적 수치가 원문에 있으면 반드시 요약에 포함할 것.\n"
        f"- 「특정 종목」「해당 기업」「관련 주」처럼 추상적으로 바꾸어 고유명·수치를 대체하거나 감추지 말 것.\n"
        f"- 핵심 정보(누가·무엇을·얼마나)를 빠뜨리지 말고 구체적으로 서술할 것.\n"
        f"어려운 경제/금융/전문 용어는 뜻을 유지하되 쉬운 일상 언어로 풀어쓰세요.\n"
        f"문장은 짧고 자연스럽게 쓰고, 뉴스식 딱딱한 문체는 피하세요.\n\n"
        f"{closing_foreign}"
        f"{article_block}"
        f"출처: {source}\n"
    )

    if _article_llm_judge_enabled():
        try:
            return _summarize_article_claude_judge_once(
                user,
                title=title,
                source=source,
                summary=summary,
                body_stripped=body_stripped,
                max_points_keep=max_points_keep,
                news_feed=news_feed,
            )
        except Exception as e:
            logger.exception(
                "SAPIENS_ARTICLE_LLM_JUDGE(단일 경로) 실패, 일반 2회 재시도 경로로 폴백: %s",
                e,
            )

    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_news_llm(user)
            data = _require_json_dict(raw)
            _validate_summarize_article_shape(data)
            return _postprocess_summarize_article_dict(
                data,
                max_points_keep,
                title,
                source,
                summary,
                body_stripped,
                news_feed=news_feed,
            )
        except Exception as e:
            last_err = e
            logger.warning("summarize_article 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(1.0)
    raise last_err or RuntimeError("summarize_article failed")


def translate_headline_to_korean(
    headline: str,
    source: str,
    summary: str = "",
    body: str = "",
    *,
    formal_korean_names: bool = False,
) -> str:
    """헤드라인을 한국어 투자 뉴스 제목으로 정리·번역."""
    src = (headline or "").strip()
    if not src:
        return src
    ctx = (body or "").strip() or (summary or "")
    if formal_korean_names:
        proper = (
            "기업·브랜드·인물·매체·상품명은 국내 금융·테크 뉴스에 맞는 **한글 정식·통용 표기**로 쓰세요. "
            "원문 영어 철자만 남기지 마세요. 필요하면 한글명 뒤 괄호에 티커·약어만 병기할 수 있습니다."
        )
    else:
        proper = "브랜드명, 기업명, 인물명 등 고유명사는 원문 그대로 유지하세요."
    user = (
        "다음 뉴스 헤드라인을 한국어로 번역하거나 다듬으세요.\n"
        "의미를 보존하되 과장 없이 간결한 뉴스 제목 톤으로 작성하세요.\n"
        f"{proper}\n"
        '반드시 JSON 한 개만 출력: {"headline_ko":"..."}\n\n'
        f"source: {source}\n"
        f"headline_src: {src}\n"
        f"context: {ctx[:400]}\n"
    )
    if not _ai_any_news_enabled():
        return src

    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_news_llm(user)
            data = _require_json_dict(raw)
            ko = str(data.get("headline_ko", "")).strip()
            if not ko:
                raise ValueError("missing headline_ko")
            return ko
        except Exception as e:
            last_err = e
            logger.warning("translate_headline_to_korean 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(0.7)
    logger.warning("translate_headline_to_korean 실패, 원문 유지: %s", last_err)
    return src


def generate_market_report(
    indicators: list[dict[str, Any]],
    articles: list[dict[str, Any]],
) -> dict[str, Any]:
    """3줄 시황 요약. 반환: {\"report\": \"...\"}"""
    ind_lines = "\n".join(
        f"- {i.get('name','')}: {i.get('value','')} ({i.get('change','')})" for i in indicators[:12]
    )
    headlines = "\n".join(f"- {(a.get('headline') or a.get('title',''))[:120]}" for a in articles[:15])
    user = (
        "다음 지표와 해외 뉴스 헤드라인을 바탕으로 미국·글로벌 시황을 한국어로 정확히 3문장으로 요약하세요.\n"
        '반드시 JSON 한 개만: {"report": "..."}\n\n'
        f"[지표]\n{ind_lines or '(없음)'}\n\n[뉴스]\n{headlines or '(없음)'}\n"
    )
    if not _ai_any_news_enabled():
        return {"report": ""}

    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_news_llm(user)
            data = _require_json_dict(raw)
            if "report" not in data:
                raise ValueError("missing report")
            return {"report": str(data["report"]).strip()}
        except Exception as e:
            last_err = e
            logger.warning("generate_market_report 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(1.0)
    raise last_err or RuntimeError("generate_market_report failed")


def _summarize_batch_concurrency() -> int:
    """SAPIENS_SUMMARIZE_CONCURRENCY: 병렬 요약 동시 수(기본 4, 1~16)."""
    v = (os.environ.get("SAPIENS_SUMMARIZE_CONCURRENCY") or "4").strip()
    try:
        n = int(v, 10)
    except ValueError:
        n = 4
    return max(1, min(16, n))


def summarize_batch(
    items: list[dict[str, Any]],
    *,
    news_feed: NewsFeedId | None = None,
) -> list[dict[str, Any]]:
    """기사 요약. SAPIENS_SUMMARIZE_CONCURRENCY>1이면 ThreadPool(입력 순서 유지), 1이면 순차+항목 간 0.5s."""
    if not items:
        return []
    n = _summarize_batch_concurrency()
    if n <= 1:
        out: list[dict[str, Any]] = []
        for it in items:
            try:
                ai = summarize_article(
                    title=it.get("title", ""),
                    summary=it.get("summary", ""),
                    source=it.get("source", ""),
                    body=str(it.get("body") or ""),
                    news_feed=news_feed,
                )
                out.append({**it, "_ai": ai})
            except Exception as e:
                logger.error("배치 항목 스킵: %s — %s", it.get("title", "")[:40], e)
            time.sleep(0.5)
        return out

    def one(idx: int, it: dict[str, Any]) -> tuple[int, dict[str, Any] | None]:
        try:
            ai = summarize_article(
                title=it.get("title", ""),
                summary=it.get("summary", ""),
                source=it.get("source", ""),
                body=str(it.get("body") or ""),
                news_feed=news_feed,
            )
            return (idx, {**it, "_ai": ai})
        except Exception as e:
            logger.error("배치 항목 스킵: %s — %s", it.get("title", "")[:40], e)
            return (idx, None)

    out_pairs: list[tuple[int, dict[str, Any] | None]] = []
    with ThreadPoolExecutor(max_workers=n) as ex:
        futs = [ex.submit(one, i, it) for i, it in enumerate(items)]
        for f in futs:
            out_pairs.append(f.result())
    out_pairs.sort(key=lambda t: t[0])
    return [row for _i, row in out_pairs if row is not None]


def merge_to_firestore_article(raw: dict[str, Any], ai: dict[str, Any]) -> dict[str, Any]:
    """앱 Article 스키마에 맞춘 dict."""
    ko_headline = str(ai.get("headline", "")).strip() or str(raw.get("title", "")).strip()
    return {
        "source": raw.get("source", ""),
        "headline": ko_headline,
        "headline_ko": ko_headline,
        "headline_en": raw.get("title", ""),
        "imageUrl": raw.get("thumbnail_url", ""),
        "summary": (raw.get("summary") or "")[:500],
        "time": raw.get("published_at") or "",
        "category": ai.get("category", ""),
        "summaryPoints": ai.get("summary_points", []),
        "tag": ai.get("category", ""),
        "sourceColor": None,
        "url": str(raw.get("url") or "").strip(),
    }
