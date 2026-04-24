"""
뉴스 **국내 탭** 3분류용 LLM 지시문 및 도우미.

- 앱 / Firestore: `domestic_market` · `global_market` · `ai_issue` ( `NewsRepository.kt` 의 `NewsFeedType` 과 동일 )
- 매경·한경·조선 풀: `NEWS_TAB_CLASSIFICATION_INSTRUCTION_KO` + `classify_article_domestic_tab` 으로 3탭 라우팅 (`crawler.crawl_domestic`). 실패·AI off 시 `feed_fallback`.
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
당신은 경제 및 테크 뉴스 큐레이터입니다. 수집된 기사의 제목과 요약본을 읽고 다음 3가지 중 하나로 분류하십시오.

국내증시: 한국 주식 시장, 한국 상장사, 국내 경제 지표가 주된 내용일 때. (코스피, 코스닥)

해외증시: 미국 주식 시장, 미국 기업의 '실적, 주가, 투자 지표, 거시경제(금리 등)'가 주된 내용일 때. (나스닥, S&P500)

AI 이슈: 주식이나 실적보다는 AI 모델 출시, 기술 발전, 반도체 성능 향상, 규제 등 '기술과 산업 트렌드' 자체가 주된 내용일 때.

[판단 주의사항]

해외 기업 기사라도 한국 시장과 엮여 있다면 '국내증시'로 분류할 것.

같은 기업(예: 오픈AI, 엔비디아)의 기사라도 주가/실적 위주면 '해외증시', 신제품이나 기술 설명 위주면 'AI 이슈'로 분류할 것."""

NEWS_TAB_LABELS: tuple[str, ...] = ("국내증시", "해외증시", "AI 이슈")

TAB_LABEL_TO_FIRESTORE_DOCUMENT: dict[str, str] = {
    "국내증시": "domestic_market",
    "해외증시": "global_market",
    "AI 이슈": "ai_issue",
}

_JSON_TAB_CLASSIFIER_FOOTER = (
    '반드시 **JSON 한 개만** 출력하세요: {"tab":"국내증시" | "해외증시" | "AI 이슈"}\n'
    '키 "tab" 값은 위 셋 중 **정확한 문자열** 하나이어야 합니다.'
)

_TAB_ALIASES: dict[str, str] = {
    "국내 증시": "국내증시",
    "해외 증시": "해외증시",
    "ai 이슈": "AI 이슈",
    "aiissue": "AI 이슈",
}


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
