"""
기사 요약·시황·기업 분석: Google Gemini API (google-genai).
"""
from __future__ import annotations

import json
import logging
import os
import re
import time
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

_gemini_client: Any = None
SYSTEM = (
    "당신은 한국 투자자를 위한 금융 뉴스 에디터입니다.\n"
    "뉴스를 분석해서 핵심 내용을 명확하고 구체적으로 요약하세요.\n"
    "반드시 JSON 형식으로만 응답하세요."
)


_HANGUL_RE = re.compile(r"[가-힣]")
_LATIN_RE = re.compile(r"[A-Za-z]")

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
    _token_input_total = 0
    _token_output_total = 0
    _token_model = ""


def get_token_usage() -> dict[str, Any]:
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


def _get_gemini_client() -> Any:
    global _gemini_client
    if _gemini_client is not None:
        return _gemini_client
    key = (os.environ.get("GEMINI_API_KEY") or os.environ.get("GOOGLE_API_KEY") or "").strip()
    if not key:
        raise RuntimeError("GEMINI_API_KEY 또는 GOOGLE_API_KEY 가 설정되어 있어야 합니다.")
    _gemini_client = genai.Client(api_key=key)
    return _gemini_client


def _call_gemini(user_content: str) -> str:
    try:
        client = _get_gemini_client()
        model_id = _gemini_model_id()
        response = client.models.generate_content(
            model=model_id,
            contents=user_content,
            config=types.GenerateContentConfig(
                system_instruction=SYSTEM,
                max_output_tokens=8192,
                temperature=0.35,
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
) -> dict[str, Any]:
    out = dict(data)
    out["headline"] = str(out.get("headline", "")).strip()
    if _looks_english_headline(out["headline"]):
        out["headline"] = translate_headline_to_korean(
            headline=out["headline"],
            source=source,
            summary=summary,
            body=body_stripped,
        )
    out["category"] = _normalize_article_category(str(out.get("category", "")))
    pts = out["summary_points"]
    if not isinstance(pts, list):
        raise ValueError("summary_points must be a list")
    out["summary_points"] = [
        str(p).strip() for p in pts if str(p).strip()
    ][:max_points_keep]
    return out


def _summarize_article_claude_judge_once(
    user: str,
    *,
    title: str,
    source: str,
    summary: str,
    body_stripped: str,
    max_points_keep: int,
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
                data, max_points_keep, title, source, summary, body_stripped
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
    foreign_rss_extra = (
        _FOREIGN_RSS_KO_FORMAL_BLOCK if (news_feed in _FOREIGN_RSS_NEWS_FEEDS) else ""
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
        f"반드시 2개 이상 4개 이하로 작성하세요. 각 포인트는 30자 이상 70자 이내로 작성하세요.\n"
        f"중요한 사실·수치·배경·영향을 누락하지 말고, 내용을 함축하거나 생략하지 마세요.\n"
        f"구체성 규칙(headline·summary_points 모두 적용):\n"
        f"- 기업명, 종목명, 브랜드명, 인물명은 원문에 나온 표기를 그대로 포함할 것.\n"
        f"- 주가, 등락률·퍼센트, 금액 등 구체적 수치가 원문에 있으면 반드시 요약에 포함할 것.\n"
        f"- 「특정 종목」「해당 기업」「관련 주」처럼 추상적으로 바꾸어 고유명·수치를 대체하거나 감추지 말 것.\n"
        f"- 핵심 정보(누가·무엇을·얼마나)를 빠뜨리지 말고 구체적으로 서술할 것.\n"
        f"어려운 경제/금융/전문 용어는 뜻을 유지하되 쉬운 일상 언어로 풀어쓰세요.\n"
        f"문장은 짧고 자연스럽게 쓰고, 뉴스식 딱딱한 문체는 피하세요.\n\n"
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
                data, max_points_keep, title, source, summary, body_stripped
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
) -> str:
    """영문 헤드라인을 한국어 투자 뉴스 제목으로 번역."""
    src = (headline or "").strip()
    if not src:
        return src
    ctx = (body or "").strip() or (summary or "")
    user = (
        "다음 뉴스 헤드라인을 한국어로 번역하세요.\n"
        "의미를 보존하되 과장 없이 간결한 뉴스 제목 톤으로 작성하세요.\n"
        "브랜드명, 기업명, 인물명 등 고유명사는 원문 그대로 유지하세요.\n"
        '반드시 JSON 한 개만 출력: {"headline_ko":"..."}\n\n'
        f"source: {source}\n"
        f"headline_en: {src}\n"
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


def summarize_batch(
    items: list[dict[str, Any]],
    *,
    news_feed: NewsFeedId | None = None,
) -> list[dict[str, Any]]:
    """기사 목록 순차 요약. 항목 사이 0.5초."""
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
