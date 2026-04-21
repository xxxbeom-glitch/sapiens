"""
기사 요약·시황·기업 분석: Firebase settings/ai_config 의 selected_model 에 따른 Claude / Gemini 분기.
"""
from __future__ import annotations

import json
import logging
import os
import re
import time
from typing import Any

import anthropic
from google import genai

logger = logging.getLogger(__name__)

GEMINI_MODEL = "gemini-2.5-flash"
CLAUDE_MODEL = "claude-haiku-4-5-20251001"

# 기사 요약: Claude(Haiku) 초안 → Gemini 검증 → 최대 3회 재작성 → 실패 시 Gemini 전체 요약.
# 켜기: 환경변수 SAPIENS_ARTICLE_LLM_JUDGE=1 (또는 true/yes). 미설정·0이면 기존 동작.
_ORIGINAL_EXCERPT_FOR_JUDGE_MAX = 14000

_JUDGE_SYSTEM = (
    "당신은 금융 뉴스 사실 검증자입니다.\n"
    "주어진 원문(또는 발췌)과 후보 JSON을 비교해 환각·오역·핵심 누락만 판정합니다.\n"
    "반드시 지정된 JSON 스키마로만 응답하세요. 설명 문장이나 마크다운을 붙이지 마세요."
)


def _article_llm_judge_enabled() -> bool:
    v = (os.environ.get("SAPIENS_ARTICLE_LLM_JUDGE") or "").strip().lower()
    return v in ("1", "true", "yes", "on")

SELECTED_MODEL_CLAUDE = "claude"
SELECTED_MODEL_GEMINI = "gemini"

# 파이프라인 시작 시 [configure_ai]로 설정 (기본: gemini).
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
    m = (selected_model or "").strip().lower()
    if m not in (SELECTED_MODEL_CLAUDE, SELECTED_MODEL_GEMINI):
        m = SELECTED_MODEL_GEMINI
    _RUNTIME_SELECTED_MODEL = m
    logger.info("summarizer AI runtime: selected_model=%s", _RUNTIME_SELECTED_MODEL)


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
    inp = _usage_meta_get_int(um, "prompt_token_count", "promptTokenCount")
    out = _usage_meta_get_int(um, "candidates_token_count", "candidatesTokenCount")
    _token_input_total += max(0, inp)
    _token_output_total += max(0, out)
    _append_model_label(GEMINI_MODEL)


def _accumulate_claude_usage(response: Any) -> None:
    global _token_input_total, _token_output_total
    usage = getattr(response, "usage", None)
    if usage is None:
        return
    if isinstance(usage, dict):
        inp = int(usage.get("input_tokens") or 0)
        out = int(usage.get("output_tokens") or 0)
    else:
        inp = int(getattr(usage, "input_tokens", None) or 0)
        out = int(getattr(usage, "output_tokens", None) or 0)
    _token_input_total += max(0, inp)
    _token_output_total += max(0, out)
    _append_model_label(CLAUDE_MODEL)


def _ai_any_news_enabled() -> bool:
    return True


def _ai_any_company_enabled() -> bool:
    return True


def _persist_selected_model_gemini() -> None:
    """Claude 실패 후 Gemini 폴백 시 원격·런타임을 gemini로 맞춤."""
    global _RUNTIME_SELECTED_MODEL
    _RUNTIME_SELECTED_MODEL = SELECTED_MODEL_GEMINI
    try:
        import firebase_client

        firebase_client.set_ai_config_selected_model(SELECTED_MODEL_GEMINI)
    except Exception as e:
        logger.warning("Firebase selected_model=gemini 반영 실패: %s", e)


def _call_claude(user_content: str) -> str:
    """Anthropic Messages API. 실패 시 예외, 빈 content 는 빈 문자열."""
    key = os.environ.get("ANTHROPIC_API_KEY", "").strip()
    if not key:
        raise RuntimeError("ANTHROPIC_API_KEY 가 설정되어 있어야 합니다.")
    prompt = f"{SYSTEM}\n\n{user_content}"
    client = anthropic.Anthropic(api_key=key)
    response = client.messages.create(
        model=CLAUDE_MODEL,
        max_tokens=4096,
        messages=[{"role": "user", "content": prompt}],
    )
    try:
        _accumulate_claude_usage(response)
    except Exception as e:
        logger.warning("Claude usage 집계 실패(무시): %s", e)
    blocks = getattr(response, "content", None) or []
    if not blocks:
        return ""
    first = blocks[0]
    raw = getattr(first, "text", None)
    if raw is None and isinstance(first, dict):
        raw = first.get("text")
    return str(raw or "").strip()


def _call_news_llm(user_content: str) -> str:
    """
    뉴스/브리핑/시황/큐레이션.
    - selected_model == gemini → Gemini만.
    - selected_model == claude → Claude 우선, 실패·빈 응답 시 Gemini 폴백 + Firebase selected_model=gemini.
    """
    if not _ai_any_news_enabled():
        return ""
    if _RUNTIME_SELECTED_MODEL == SELECTED_MODEL_GEMINI:
        return str(_call_gemini(user_content) or "").strip()
    try:
        raw = _call_claude(user_content)
        if raw:
            return raw
    except Exception as e:
        logger.warning("Claude 뉴스/요약 호출 실패: %s", e)
    logger.info("뉴스 요약: Claude 실패 또는 빈 응답 → Gemini 폴백, settings/ai_config selected_model=gemini")
    _persist_selected_model_gemini()
    return str(_call_gemini(user_content) or "").strip()


def _call_company_llm(user_content: str) -> str:
    """종목 분석. 분기 규칙은 [_call_news_llm] 과 동일."""
    if not _ai_any_company_enabled():
        return ""
    if _RUNTIME_SELECTED_MODEL == SELECTED_MODEL_GEMINI:
        return str(_call_gemini(user_content) or "").strip()
    try:
        raw = _call_claude(user_content)
        if raw:
            return raw
    except Exception as e:
        logger.warning("Claude 기업 분석 호출 실패: %s", e)
    logger.info("기업 분석: Claude 실패 또는 빈 응답 → Gemini 폴백, settings/ai_config selected_model=gemini")
    _persist_selected_model_gemini()
    return str(_call_gemini(user_content) or "").strip()


def _get_gemini_client() -> Any:
    global _gemini_client
    if _gemini_client is not None:
        return _gemini_client
    key = os.environ.get("GEMINI_API_KEY", "").strip()
    if not key:
        raise RuntimeError("GEMINI_API_KEY 가 설정되어 있어야 합니다.")
    _gemini_client = genai.Client(api_key=key)
    return _gemini_client


def _stub_company_ai_result() -> dict[str, Any]:
    """종목 분석 API 미사용 시 Firestore 호환 기본값."""
    hs = {
        "debt_ratio_status": "안정",
        "current_ratio_status": "안정",
        "equity_ratio_status": "안정",
        "cash_status": "안정",
        "interest_status": "안정",
    }
    return {
        "business": "",
        "revenue_model": "",
        "outlook_2026": "",
        "risk_factors": "",
        "capital": "",
        "health_summary": "",
        "ai_analyst_summary": "",
        "health_status": hs,
        "health_score": 3,
    }


def _finalize_company_ai_dict(data: dict[str, Any]) -> dict[str, Any]:
    required_str_keys = (
        "business",
        "revenue_model",
        "outlook_2026",
        "risk_factors",
        "capital",
        "health_summary",
        "ai_analyst_summary",
    )
    for k in required_str_keys:
        if k not in data or data[k] is None:
            data[k] = ""
        else:
            data[k] = str(data[k])
    hs = data.get("health_status")
    if not isinstance(hs, dict):
        hs = {}
        data["health_status"] = hs
    for hk in (
        "debt_ratio_status",
        "current_ratio_status",
        "equity_ratio_status",
        "cash_status",
        "interest_status",
    ):
        hs.setdefault(hk, "안정")
    try:
        sc = int(data.get("health_score"))
    except (TypeError, ValueError):
        sc = 3
    data["health_score"] = max(0, min(5, sc))
    return data


def _call_gemini(user_content: str) -> str:
    try:
        client = _get_gemini_client()
        prompt = f"{SYSTEM}\n\n{user_content}"
        response = client.models.generate_content(
            model=GEMINI_MODEL,
            contents=prompt,
            config={"max_output_tokens": 4096},
        )
        try:
            _accumulate_gemini_usage(response)
        except Exception as e:
            logger.warning("Gemini usage 집계 실패(무시): %s", e)
        raw = getattr(response, "text", None)
        if raw is None:
            return ""
        return str(raw).strip()
    except Exception as e:
        logger.exception("_call_gemini 실패: %s", e)
        return ""


def _call_gemini_judge(user_content: str) -> str:
    """Gemini 판정 전용. temperature 낮춤."""
    try:
        client = _get_gemini_client()
        prompt = f"{_JUDGE_SYSTEM}\n\n{user_content}"
        response = client.models.generate_content(
            model=GEMINI_MODEL,
            contents=prompt,
            config={"max_output_tokens": 2048, "temperature": 0.15},
        )
        try:
            _accumulate_gemini_usage(response)
        except Exception as e:
            logger.warning("Gemini judge usage 집계 실패(무시): %s", e)
        raw = getattr(response, "text", None)
        if raw is None:
            return ""
        return str(raw).strip()
    except Exception as e:
        logger.exception("_call_gemini_judge 실패: %s", e)
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


def _gemini_judge_verdict(
    original_excerpt: str,
    draft: dict[str, Any],
    *,
    title: str,
    source: str,
) -> tuple[bool, list[dict[str, Any]]]:
    """(통과 여부, 이슈 목록). 판정 파싱 실패 시 통과로 간주(파이프라인 정지 방지)."""
    candidate = json.dumps(
        {
            "headline": draft.get("headline"),
            "category": draft.get("category"),
            "summary_points": draft.get("summary_points"),
        },
        ensure_ascii=False,
    )
    judge_user = (
        "원문(또는 발췌)과 후보 요약 JSON을 비교해 사실성을 판정하세요.\n\n"
        f"[출처] {source}\n[제목] {title}\n\n"
        f"[원문·블록 발췌]\n{original_excerpt}\n\n"
        f"[후보 JSON]\n{candidate}\n\n"
        "판정 규칙:\n"
        "- 후보 headline·summary_points에 원문에 없는 사실·수치·인과가 있으면 FAIL(환각).\n"
        "- 원문의 핵심 부정·조건·예외가 빠져 의미가 바뀌면 FAIL(누락).\n"
        "- 고유명사·수치의 명백한 오역이 있으면 FAIL.\n"
        "- 경미한 문장 표현 차이만 있으면 PASS.\n\n"
        '반드시 JSON 한 개만 출력:\n'
        '{"verdict":"PASS" 또는 "FAIL","issues":[{"type":"hallucination|omission|translation|format|other","detail":"한국어로 구체적 설명"}]}\n'
        "PASS인 경우 issues는 빈 배열 []이어야 합니다."
    )
    raw = _call_gemini_judge(judge_user)
    if not (raw or "").strip():
        logger.warning("Gemini judge 빈 응답 — 통과 처리")
        return True, []
    try:
        d = _require_json_dict(raw)
    except ValueError:
        logger.warning("Gemini judge JSON 파싱 실패 — 통과 처리")
        return True, []
    verdict = str(d.get("verdict", "")).strip().upper()
    issues_raw = d.get("issues", [])
    if not isinstance(issues_raw, list):
        issues_raw = []
    issues: list[dict[str, Any]] = []
    for x in issues_raw:
        if isinstance(x, dict):
            issues.append(x)
    if verdict == "PASS":
        return True, []
    return False, issues


def _gemini_fallback_full_summarize(
    user: str,
    max_points_keep: int,
    title: str,
    source: str,
    summary: str,
    body_stripped: str,
    *,
    quality_meta: dict[str, Any],
) -> dict[str, Any]:
    """Judge 실패·Claude 실패 시 동일 user 프롬프트로 Gemini 전체 요약."""
    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_gemini(user)
            data = _require_json_dict(raw)
            _validate_summarize_article_shape(data)
            out = _postprocess_summarize_article_dict(
                data, max_points_keep, title, source, summary, body_stripped
            )
            meta = dict(quality_meta)
            meta["fallback_model"] = GEMINI_MODEL
            out["_quality"] = meta
            return out
        except Exception as e:
            last_err = e
            logger.warning("Gemini fallback summarize 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(1.0)
    raise last_err or RuntimeError("gemini fallback summarize failed")


def _summarize_article_llm_judge_path(
    user: str,
    *,
    article_block: str,
    title: str,
    source: str,
    summary: str,
    body_stripped: str,
    max_points_keep: int,
) -> dict[str, Any]:
    """
    Claude Haiku 최대 3회(피드백 반영) → 매회 Gemini 검증.
    3회 끝까지 미통과·Claude 실패 시 Gemini로 동일 프롬프트 전체 요약.
    """
    excerpt = (article_block or "").strip()
    if len(excerpt) > _ORIGINAL_EXCERPT_FOR_JUDGE_MAX:
        original_excerpt = excerpt[:_ORIGINAL_EXCERPT_FOR_JUDGE_MAX] + "\n\n...(원문 일부만 표시)"
    else:
        original_excerpt = excerpt

    feedback_extra = ""
    notes: list[dict[str, Any]] = []
    haiku_round = 0

    for round_ix in range(3):
        user_haiku = user + feedback_extra
        haiku_round += 1
        try:
            raw_haiku = _call_claude(user_haiku)
        except Exception as e:
            logger.warning(
                "LLM judge: Haiku 호출 실패(라운드 %d) → Gemini 전체 요약: %s",
                haiku_round,
                e,
            )
            return _gemini_fallback_full_summarize(
                user,
                max_points_keep,
                title,
                source,
                summary,
                body_stripped,
                quality_meta={
                    "path": "gemini_fallback",
                    "reason": "claude_error",
                    "haiku_rounds": haiku_round - 1,
                    "notes": notes,
                },
            )

        if not (raw_haiku or "").strip():
            logger.warning("LLM judge: Haiku 빈 응답 → Gemini 전체 요약")
            return _gemini_fallback_full_summarize(
                user,
                max_points_keep,
                title,
                source,
                summary,
                body_stripped,
                quality_meta={
                    "path": "gemini_fallback",
                    "reason": "empty_claude",
                    "haiku_rounds": haiku_round - 1,
                    "notes": notes,
                },
            )

        try:
            data = _require_json_dict(raw_haiku)
            _validate_summarize_article_shape(data)
        except Exception as e:
            notes.append({"round": round_ix + 1, "parse_error": str(e)})
            feedback_extra = (
                "\n\n## 이전 출력 오류\n"
                "유효한 JSON 객체 **한 개만** 출력해야 합니다. "
                '키는 반드시 "headline", "category", "summary_points" 세 가지입니다.\n'
                f"오류: {e}\n"
            )
            if round_ix == 2:
                return _gemini_fallback_full_summarize(
                    user,
                    max_points_keep,
                    title,
                    source,
                    summary,
                    body_stripped,
                    quality_meta={
                        "path": "gemini_fallback",
                        "reason": "haiku_json_fail_3x",
                        "haiku_rounds": haiku_round,
                        "notes": notes,
                    },
                )
            continue

        try:
            passed, issues = _gemini_judge_verdict(
                original_excerpt, data, title=title, source=source
            )
        except Exception as e:
            logger.warning("LLM judge: 판정 예외, 통과 처리: %s", e)
            passed, issues = True, []

        if passed:
            out = _postprocess_summarize_article_dict(
                data, max_points_keep, title, source, summary, body_stripped
            )
            out["_quality"] = {
                "path": "haiku_judge_pass",
                "haiku_rounds": haiku_round,
                "notes": notes,
            }
            return out

        notes.append({"round": round_ix + 1, "issues": issues})
        feedback_extra = (
            "\n\n## 품질 검증 피드백(반드시 반영)\n"
            "아래 지적을 모두 반영한 뒤, 동일 스키마의 JSON **한 개만** 다시 출력하세요.\n"
            f"{json.dumps(issues, ensure_ascii=False)}\n"
        )
        if round_ix == 2:
            return _gemini_fallback_full_summarize(
                user,
                max_points_keep,
                title,
                source,
                summary,
                body_stripped,
                quality_meta={
                    "path": "gemini_fallback",
                    "reason": "judge_fail_after_3_haiku",
                    "haiku_rounds": haiku_round,
                    "notes": notes,
                },
            )

    raise RuntimeError("LLM judge path: internal logic error (should not reach)")


def summarize_article(
    title: str,
    summary: str,
    source: str,
    body: str = "",
    *,
    briefing_newspaper: bool = False,
) -> dict[str, Any]:
    """
    반환:
    headline, category, summary_points
    """
    body_stripped = (body or "").strip()
    summary_stripped = (summary or "").strip()

    if briefing_newspaper and len(body_stripped) < 200:
        logger.info(
            "briefing 신문: 본문 %d자(200자 미만) — 요약 API 스킵, summary_points=[]",
            len(body_stripped),
        )
        return {"headline": "", "category": "", "summary_points": []}

    if not _ai_any_news_enabled():
        logger.info("summarize_article: Claude/Gemini 모두 off — 원문 폴백용 스텁")
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
    if briefing_newspaper and len(body_stripped) >= 200:
        blen = len(body_stripped)
        if blen < 500:
            max_points_keep = 2
            pts_count_rule = (
                "summary_points 배열 길이: 원문 분량에 맞게 **1개 또는 2개**만 사용하세요. "
                "짧은 기사는 **1개**만으로 충분할 수 있습니다. 억지로 bullet을 늘리거나 한 문장을 잘게 쪼개지 마세요.\n"
            )
        else:
            max_points_keep = 4
            pts_count_rule = (
                "summary_points 배열 길이: **3개 또는 4개**만 사용하세요(5개 이상 금지). "
                "핵심이 3개로 충분하면 3개만 써도 됩니다.\n"
            )
        user = (
            "다음 뉴스를 분석해 JSON만 출력하세요.\n"
            f'키: "headline", "category", "summary_points"\n'
            "headline은 반드시 한국어로 작성하세요. 원문이 영어면 자연스러운 한국어 제목으로 번역하세요.\n"
            f"{_SUMMARIZE_ARTICLE_CATEGORY_BLOCK}"
            f"{pts_count_rule}"
            "각 summary_points 항목은 **하나의 완전한 문장**으로 끝내세요. "
            "항목별 글자 수·길이 상한을 두지 말고, 읽기 자연스러운 분량으로 쓰세요.\n"
            "중요한 사실·수치·배경·영향을 누락하지 마세요.\n"
            "구체성 규칙(headline·summary_points 모두 적용):\n"
            "- 기업명, 종목명, 브랜드명, 인물명은 원문에 나온 표기를 그대로 포함할 것.\n"
            "- 주가, 등락률·퍼센트, 금액 등 구체적 수치가 원문에 있으면 반드시 요약에 포함할 것.\n"
            "- 「특정 종목」「해당 기업」「관련 주」처럼 추상적으로 바꾸어 고유명·수치를 대체하거나 감추지 말 것.\n"
            "- 핵심 정보(누가·무엇을·얼마나)를 빠뜨리지 말고 구체적으로 서술할 것.\n"
            "어려운 경제/금융/전문 용어는 뜻을 유지하되 쉬운 일상 언어로 풀어쓰세요.\n"
            "뉴스식 딱딱한 문체는 피하세요.\n\n"
            f"{article_block}"
            f"출처: {source}\n"
        )
    else:
        user = (
            f"다음 뉴스를 분석해 JSON만 출력하세요.\n"
            f'키: "headline", "category", "summary_points"\n'
            f"headline은 반드시 한국어로 작성하세요. 원문이 영어면 자연스러운 한국어 제목으로 번역하세요.\n"
            f"{_SUMMARIZE_ARTICLE_CATEGORY_BLOCK}"
            f"summary_points는 기사 핵심을 빠짐없이 담은 문자열 배열로 작성하세요.\n"
            f"중요한 사실·수치·배경·영향을 누락하지 말고, 내용을 함축하거나 생략하지 마세요.\n"
            f"구체성 규칙(headline·summary_points 모두 적용):\n"
            f"- 기업명, 종목명, 브랜드명, 인물명은 원문에 나온 표기를 그대로 포함할 것.\n"
            f"- 주가, 등락률·퍼센트, 금액 등 구체적 수치가 원문에 있으면 반드시 요약에 포함할 것.\n"
            f"- 「특정 종목」「해당 기업」「관련 주」처럼 추상적으로 바꾸어 고유명·수치를 대체하거나 감추지 말 것.\n"
            f"- 핵심 정보(누가·무엇을·얼마나)를 빠뜨리지 말고 구체적으로 서술할 것.\n"
            f"어려운 경제/금융/전문 용어는 뜻을 유지하되 쉬운 일상 언어로 풀어쓰세요.\n"
            f"문장은 짧고 자연스럽게 쓰고, 뉴스식 딱딱한 문체는 피하세요.\n"
            f"분량은 고정하지 말고 내용량에 맞춰 유동적으로 작성하세요.\n"
            f"(중요 내용이 많으면 길게, 단순한 내용이면 짧게)\n\n"
            f"{article_block}"
            f"출처: {source}\n"
        )

    if _article_llm_judge_enabled():
        try:
            return _summarize_article_llm_judge_path(
                user,
                article_block=article_block,
                title=title,
                source=source,
                summary=summary,
                body_stripped=body_stripped,
                max_points_keep=max_points_keep,
            )
        except Exception as e:
            logger.exception(
                "SAPIENS_ARTICLE_LLM_JUDGE 파이프라인 실패, 기존 _call_news_llm 경로로 폴백: %s",
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


def curate_us_market_articles(
    items: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """
    stocks+tech 합친 리스트에서 Gemini로 최대 8개 엄선 (중복·광고성 제외).
    8개 미만이면 전부 반환; 8개 이하면 API 생략하고 전부 반환.
    """
    if not items:
        return []
    n = len(items)
    if n <= 8:
        return [dict(x) for x in items]

    if not _ai_any_news_enabled():
        return [dict(items[i]) for i in range(8)]

    lines: list[str] = []
    for i, it in enumerate(items):
        t = str(it.get("title", ""))[:220]
        s = str(it.get("summary", ""))[:400]
        src = str(it.get("source", ""))[:80]
        u = str(it.get("url", ""))[:300]
        lines.append(f'[{i}] title: {t}\n  source: {src}\n  url: {u}\n  lead: {s}')
    body = "\n\n".join(lines)
    user = (
        "당신은 글로벌 금융 뉴스 큐레이터입니다.\n"
        "투자자 관점에서 가장 중요하고, 서로 **다른 주제**를 넓게 커버하도록 **정확히 8개** 기사를 고르세요. "
        "제목·내용이 **중복/유사**한 기사는 **하나만** 남기세요. **광고성·낚시성(클릭베이트)** 느낌이 강한 기사는 제외하세요.\n"
        f"아래 [0]~[{n-1}] 인덱스로 식별됩니다. **서로 다른 정수 8개**의 인덱스만 JSON으로 출력하세요.\n"
        '형식(배열만, 길이 8): {{"selected_indices": [0,1,2,3,4,5,6,7]}}\n\n'
        f"{body}"
    )
    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_news_llm(user)
            data = _require_json_dict(raw)
            idxs = data.get("selected_indices", [])
            if not isinstance(idxs, list) or not idxs:
                raise ValueError("missing or invalid selected_indices")
            picked: list[dict[str, Any]] = []
            seen: set[int] = set()
            for j in idxs:
                if not isinstance(j, int) or j < 0 or j >= n or j in seen:
                    continue
                seen.add(j)
                picked.append(dict(items[j]))
            if not picked:
                return [dict(items[i]) for i in range(min(8, n))]
            for i in range(n):
                if len(picked) >= 8:
                    break
                if i not in seen:
                    seen.add(i)
                    picked.append(dict(items[i]))
            return picked[:8]
        except Exception as e:
            last_err = e
            logger.warning("curate_us_market_articles 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(1.0)
    logger.warning("curate_us_market_articles 실패, 상위 8개 사용: %s", last_err)
    return [dict(items[i]) for i in range(min(8, n))]


def summarize_batch(
    items: list[dict[str, Any]],
    *,
    briefing_newspaper: bool = False,
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
                briefing_newspaper=briefing_newspaper,
            )
            out.append({**it, "_ai": ai})
        except Exception as e:
            logger.error("배치 항목 스킵: %s — %s", it.get("title", "")[:40], e)
        time.sleep(0.5)
    return out


def analyze_company(
    ticker: str,
    name: str,
    financials: dict[str, Any],
    health: dict[str, Any],
    analyst: dict[str, Any],
    profile: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """
    기업 서술·건전성 라벨·점수·AI 증권 의견 (Firebase ai_config 에 따른 Claude / Gemini).
    """
    profile = profile or {}
    payload = {
        "ticker": ticker,
        "name": name,
        "profile": profile,
        "financials": financials,
        "health": health,
        "analyst": analyst,
    }
    user = (
        "다음 기업 데이터를 바탕으로 한국 투자자용 분석 JSON만 출력하세요.\n"
        '필수 키: "business", "revenue_model", "outlook_2026", "risk_factors", "capital", '
        '"health_status", "health_summary", "health_score", "ai_analyst_summary"\n'
        "health_status는 객체이며 반드시 다음 키 포함:\n"
        "debt_ratio_status, current_ratio_status, equity_ratio_status, "
        "cash_status, interest_status\n"
        "일반 지표 값은 우량/안정/주의/위험 중 하나.\n"
        "cash_status만 풍부/안정/주의/부족 중 하나.\n"
        "health_score: 0~5 정수.\n\n"
        + json.dumps(payload, ensure_ascii=False)[:45000]
    )
    if not _ai_any_company_enabled():
        logger.info("analyze_company: AI 모두 off — 스텁 저장")
        return _stub_company_ai_result()

    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_company_llm(user)
            data = _require_json_dict(raw)
            return _finalize_company_ai_dict(data)
        except Exception as e:
            last_err = e
            logger.warning("analyze_company 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(1.0)
    raise last_err or RuntimeError("analyze_company failed")


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
