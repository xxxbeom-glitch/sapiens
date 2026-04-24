"""
뉴스 **국내 탭** 3분류용 LLM 지시문 및 도우미.

- 앱 / Firestore: `domestic_market` · `global_market` · `ai_issue` ( `NewsRepository.kt` 의 `NewsFeedType` 과 동일 )
- 매경·한경·조선 풀: `classify_article_domestic_tab` → `finalize_kr_overseas_tab_label`. **해외 RSS(`feed_fallback=global`)** 는 LLM이 `해외증시`로 준 건 키워드 없어도 유지(짧은 RSS 요약에 나스닥 등이 없어 미국 탭이 비는 문제 방지). **국내 RSS** 에서만 `해외증시`+미시장 키워드 없으면 `국내증시`로 내림.
- CNBC 풀: **`ai_issue` Firestore 문서에만** 적재; 동일 규칙으로 **「AI 이슈」만** 남기고 국내증시·해외증시 판정은 제외(실패 시 해당 건은 유지). 매경·한경·조선 풀에서 「AI 이슈」로 나온 건은 `crawl_domestic`에서 국내/해외 탭으로만 보냄.
"""

from __future__ import annotations

import logging
import re
import time
from typing import Any

logger = logging.getLogger(__name__)

# --- [AI 분류 시스템 지시문] (요청 시 원문 유지) ---
NEWS_TAB_CLASSIFICATION_INSTRUCTION_KO = """[AI 분류 시스템 지시문]
당신은 경제 및 테크 뉴스 큐레이터입니다. 수집된 기사의 제목과 요약본을 읽고 다음 4가지 중 하나로 분류하십시오.

국내증시: 한국 주식 시장, 한국 상장사, 국내 경제·정책·지표가 주된 내용일 때. (코스피, 코스닥, 한국 투자자·상장·거래소)

해외증시(미국 증시·자본시장): **나스닥·S&P500·다우 등 미국 증권시장, 미국 상장사 주가·실적·ETF, 연준·금리·미 국채·달러(투자·시장에 초점), 뉴욕 시장** 등 **미국 자본시장**과 직접 연관될 때만. (해외=미국시장이 아님. 일반 국제 뉴스를 해외증시로 넣지 말 것.)

AI 이슈: 주가·지수보다 **AI·반도체·빅테크 기술·제품·규제**가 주제일 때. (모델, GPU, OpenAI, 선단 공정 등)

제외: 한국·미국 증시·경제·금융·테크와 **무관한** 기사. 아래 중 하나라도 해당하면 반드시 제외.
- 국내외 정치 단독 보도 (경제·시장 영향이 전혀 없는 순수 정치 뉴스)
- 사건·사고·범죄·사회 뉴스 (경제·시장과 무관한 것)
- 스포츠·연예·문화·날씨·생활
- 특정 국가 내정·선거 (한국·미국 금융시장과 직접 무관할 때)

※ 전쟁·분쟁·지정학 리스크는 증시·원자재·환율에 영향을 주므로 **제외하지 말 것** (해외증시 또는 국내증시로 분류)
※ 관세·무역·외교 마찰도 경제 영향이 있으면 **제외하지 말 것**

[해외증시에 넣지 말 것 — 아주 중요]
- 사회, 사건, 범죄, 재난, 날씨, 정치(일반), 문화, 스포츠·연예 **단독** 보도이고 **나스닥·S&P·다우·연준·미 상장사 실적/주가·FX(투자)**가 본문 주제가 **아닐** 때
- EU·일본·중국 등 **다른 나라** 이슈만이고 **미국 증시·미 기업(상장) 투자**와 **무관**할 때
- 그런 기사는 '해외증시'가 **아님** → 한국 투자자/시장과 맞닿으면 '국내증시', 기술·AI 중심이면 'AI 이슈', 나머지는 '제외'로

[판단 주의사항]
해외 기업 기사라도 **한국 시장·투자자**와 엮이면 '국내증시'로.
같은 기업(엔비디아 등)도 **주가·실적·시총**이면 '해외증시', **제품·기술**이면 'AI 이슈'.
애매하면 **제외**보다 관련 탭으로 넣는 것을 우선하되, 명백히 증시·경제·테크와 무관한 정치·사회·연예·스포츠면 반드시 '제외'로."""

NEWS_TAB_LABELS: tuple[str, ...] = ("국내증시", "해외증시", "AI 이슈", "제외")

TAB_LABEL_TO_FIRESTORE_DOCUMENT: dict[str, str] = {
    "국내증시": "domestic_market",
    "해외증시": "global_market",
    "AI 이슈": "ai_issue",
    "제외": "excluded",
}

_JSON_TAB_CLASSIFIER_FOOTER = (
    '반드시 **JSON 한 개만** 출력하세요: {"tab":"국내증시" | "해외증시" | "AI 이슈" | "제외"}\n'
    '키 "tab" 값은 위 넷 중 **정확한 문자열** 하나이어야 합니다.'
)

_TAB_ALIASES: dict[str, str] = {
    "국내 증시": "국내증시",
    "해외 증시": "해외증시",
    "ai 이슈": "AI 이슈",
    "aiissue": "AI 이슈",
    "excluded": "제외",
    "제 외": "제외",
}

# 제목+요약에 **미국 자본시장** 힌트 (LLM '해외증시'·해외 RSS 폴백 검증). "미국" 단독·일반 사회 뉴스는 제외.
_US_CAPITAL_MARKET_SIGNALS = re.compile(
    (
        r"나스닥|NASDAQ|"
        r"에스앤피|\bS\s*＆\s*P\s*500|\bS&P\s*500|\bS\&P\b|"
        r"다우\s*지?수?|Dow\s*Jones|\bDJI\b|"
        r"뉴욕\s*증[시권]|뉴욓\s*증[시권]|뉴욕\s*장|뉴욓\s*장|"
        r"\bNYSE\b|뉴욕\s*거래소|"
        r"필라델피아|"
        r"나스닥\s*100|"
        r"라셀|Russell|\bIWM\b|"
        r"FOMC|"
        r"FRB|연\s*준|Federal\s*Reserve|파월|"
        r"기준\s*금리.?(?:인하|인상|동결|결정|발표|회의|선언)"
        r"|미국채|미국\s*국채|10\s*년.?(?:\s*미국|물).{0,6}채|"
        r"미국\s*증[시권]|\b미시장|미\s*증[시권]"
        r"|\bNVDA\b|\bTSLA\b|\bAAPL\b|\bMSFT\b|\bAMZN\b|\bGOOGL|\bGOOG\b|\bMETA\b|"
        r"엔비디아.?(?:주[가]|시총|실적|어닝|급등|급락|발표)"
        r"|테슬라.?(?:주[가]|시총|실적)"
        r"|빅테크.?(?:주[가]|\s*기술주\)|\s*실적)"
        r"|\bCPI\b.{0,50}미국|미국.{0,50}\bCPI\b|ADP|"
        r"장\s*마감.?(?:\s*뉴욓|\s*뉴욕|나스|다우|미국)"
        r"|\bWTI\b|원/달러|\bDXY\b"
        r"|월가"
    ),
    re.IGNORECASE,
)


def _text_for_tab_signals(title: str, summary: str) -> str:
    t = f"{(title or '').strip()} {(summary or '').strip()}"
    t = re.sub(r"\s+", " ", t).strip()
    return t


def looks_us_capital_markets_centric(title: str, summary: str) -> bool:
    """제목+요약에 **미국 증시/연준/주요 티커·뉴욕 시장** 힌트가 있는지."""
    text = _text_for_tab_signals(title, summary)
    if not text:
        return False
    if _US_CAPITAL_MARKET_SIGNALS.search(text):
        return True
    return False


def finalize_kr_overseas_tab_label(
    label: str | None,
    title: str,
    summary: str,
    feed_fallback: str,
) -> str:
    """
    매경·한경·조선 KR/해외 풀: LLM 결과 + 출처 RSS로 탭을 확정.

    - **해외 RSS (`global_market`)**: LLM이 `해외증시`이면 그대로 둠. 분류 실패(None)도 `해외증시`
      (짧은 영문 스니펫에 나스닥·S&P 키워드가 없어 전량이 국내로 가며 미국 탭이 비는 것을 막음).
    - **국내 RSS (`domestic_market`)**: `해외증시`인데 제목·요약에 미시장 신호가 없으면 `국내증시`로 내림
      (사회면 등이 미국 탭으로 가는 완충).
    - 국내 RSS + LLM 실패(None): 미시장 힌트 있을 때만 `해외증시`, 아니면 `국내증시`.
    """
    fb = (feed_fallback or "domestic_market").strip()
    if fb not in ("domestic_market", "global_market"):
        fb = "domestic_market"
    t_raw = (title or "").strip()
    s_raw = (summary or "").strip()
    us = looks_us_capital_markets_centric(t_raw, s_raw)
    t_norm = _normalize_tab_label((label or "").strip()) if (label and str(label).strip()) else None
    if fb == "global_market":
        if t_norm is None or t_norm == "제외":
            return "해외증시"
        if t_norm == "해외증시":
            return "해외증시"
        return t_norm
    # --- 아래는 국내 RSS 출신만 ---
    if t_norm is None:
        return "해외증시" if us else "국내증시"
    if t_norm == "해외증시" and not us:
        logger.info(
            "뉴스탭: 해외증시→국내증시(미시장 키워드 없음·국내RSS) title=%.100s",
            t_raw,
        )
        return "국내증시"
    return t_norm


def _normalize_tab_label(raw: str) -> str | None:
    s = re.sub(r"\s+", " ", (raw or "").strip())
    if not s:
        return None
    if s in TAB_LABEL_TO_FIRESTORE_DOCUMENT:
        return s
    al = _TAB_ALIASES.get(s) or _TAB_ALIASES.get(s.replace(" ", ""))
    if al in TAB_LABEL_TO_FIRESTORE_DOCUMENT:
        return al
    low = s.lower()
    if "국내" in s and "증시" in s:
        return "국내증시"
    if "해외" in s and "증시" in s:
        return "해외증시"
    if "ai" in low and "이슈" in s:
        return "AI 이슈"
    if "제외" in s or "excluded" in low:
        return "제외"
    return None


def build_classify_domestic_tab_user_prompt(*, title: str, summary: str) -> str:
    t = (title or "").strip()
    s = (summary or "").strip()
    return (
        f"{NEWS_TAB_CLASSIFICATION_INSTRUCTION_KO}\n\n"
        f"[제목]\n{t}\n\n"
        f"[요약]\n{s}\n\n"
        f"{_JSON_TAB_CLASSIFIER_FOOTER}"
    )


def parse_tab_from_classifier_json(data: dict[str, Any]) -> str | None:
    tab = data.get("tab", data.get("category", data.get("label")))
    if not isinstance(tab, str):
        return None
    return _normalize_tab_label(tab)


def classify_article_domestic_tab(
    title: str,
    summary: str,
    *,
    retries: int = 2,
) -> str | None:
    """
    제목+요약만으로 국내 탭 3분류. 반환: '국내증시' | '해외증시' | 'AI 이슈' | None
    (`crawl_domestic` 3탭 배정. `configure_ai` 이후·요약 이전에 호출.)
    """
    # GitHub Actions 등: `cd pipeline && python main.py` → 패키지 루트가 `pipeline`이라 `summarizer`만 씀.
    try:
        from summarizer import _ai_any_news_enabled, _call_news_llm, _require_json_dict
    except ImportError:
        from pipeline.summarizer import _ai_any_news_enabled, _call_news_llm, _require_json_dict

    if not _ai_any_news_enabled():
        return None
    user = build_classify_domestic_tab_user_prompt(title=title, summary=summary)
    last: Exception | None = None
    for attempt in range(retries):
        try:
            raw = _call_news_llm(user)
            d = _require_json_dict(raw)
            tab = parse_tab_from_classifier_json(d)
            if tab:
                return tab
            last = ValueError("unknown tab in classifier JSON")
        except Exception as e:
            last = e
            logger.warning("classify_article_domestic_tab 재시도 (%s/2): %s", attempt + 1, e)
            if attempt < retries - 1:
                time.sleep(0.5)
    logger.warning("classify_article_domestic_tab 실패: %s", last)
    return None


def tab_to_firestore_document_id(tab: str | None) -> str | None:
    """'국내증시' 등 → 'domestic_market'. `tab`이 None·빈 값이면 None."""
    if not tab or not str(tab).strip():
        return None
    n = _normalize_tab_label(str(tab).strip())
    if n is None:
        return None
    return TAB_LABEL_TO_FIRESTORE_DOCUMENT.get(n)
