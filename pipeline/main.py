"""
뉴스 크롤링 → LLM 요약 → Firestore 저장.

PIPELINE_SECTION 환경변수:
  all (기본) | news | market | full
  (구) domestic_news 는 news 와 동일하게 처리

  - all: 아래 두 블록을 순서대로 실행(각 블록은 해당 섹션의 휴장 규칙으로 개별 스킵).
  - full: 레거시 전체 파이프라인(피드 초기화 등).

마켓(지표·네이버 테마/업종·시황 요약·Firestore `market/*`)은 기본 비활성.
켜려면 환경변수 `SAPIENS_MARKET_PIPELINE=1`(또는 true/yes/on).

`from main import run` 을 쓰는 러너(CI, Docker 등)는 import 시점에 부작용·무거운 초기화가
돌아가지 않도록, 로깅 설정·환경 로드·하위 모듈 import는 모두 `run()` 안에서 수행합니다.
"""
from __future__ import annotations

import logging
import os
import time
from pathlib import Path
from typing import Any, Tuple

logger = logging.getLogger("pipeline")

PIPELINE_SECTION_ALL = "all"
PIPELINE_SECTION_FULL = "full"
PIPELINE_SECTION_NEWS = "news"
PIPELINE_SECTION_MARKET = "market"
# 하위 호환: 예전 Run/CI의 PIPELINE_SECTION=domestic_news
_LEGACY_PIPELINE_SECTION_NEWS = "domestic_news"


def _market_pipeline_enabled() -> bool:
    """False 기본. `SAPIENS_MARKET_PIPELINE=1|true|yes|on` 일 때만 마켓 크롤·저장·시황 요약."""
    v = (os.environ.get("SAPIENS_MARKET_PIPELINE") or "").strip().lower()
    if v in ("1", "true", "yes", "on"):
        return True
    if v in ("0", "false", "no", "off"):
        return False
    return False


def is_skip_day(section: str) -> Tuple[bool, str]:
    """
    (스킵 여부, 사유).
    - news: 휴일/주말 없이 매일 수행.
    - market: 월~금(한국주말)만, 한국 공휴일 제외.
    - full, all: 여기서는 스킵 없음(개별 단계는 news/market 규칙).
    """
    from datetime import datetime

    import holidays
    import pytz

    s = (section or "").strip().lower()
    if s in ("all", "full"):
        return False, ""

    if s == "news":
        return False, ""

    if s == "market":
        kst = pytz.timezone("Asia/Seoul")
        now = datetime.now(kst)
        if now.weekday() >= 5:  # 토(5) · 일(6)
            return True, "주말"
        kr_holidays = holidays.KR(years=now.year)
        if now.date() in kr_holidays:
            return True, f"한국 공휴일: {kr_holidays[now.date()]}"
        return False, ""

    return False, ""


def _schedule_unified_feed_push(firebase_client: Any) -> None:
    """뉴스 등 반영 후 FCM 예약 1건(토픽 `sapiens_feed`)."""
    import push_schedule_util as psu

    when_k = psu.resolve_push_target_kst()
    title, body = psu.unified_feed_push_content()
    firebase_client.write_push_schedule_entry(
        doc_id=psu.push_doc_id("feed", when_k),
        section="news",
        scheduled_at_utc_iso=psu.kst_to_utc_iso_z(when_k),
        title=title,
        body=body,
        topic=psu.UNIFIED_FEED_TOPIC,
    )


def _run_news_only(crawler: Any, firebase_client: Any, summarizer: Any) -> None:
    ai_cfg = firebase_client.get_ai_config()
    summarizer.configure_ai(selected_model=str(ai_cfg.get("selected_model", "gemini")))

    # TTL 보조 정리(48시간 만료). Firestore TTL이 켜져 있어도 지연될 수 있어 선제 정리.
    try:
        firebase_client.cleanup_expired_news_docs()
    except Exception:
        pass

    domestic = crawler.crawl_domestic()
    domestic["domestic_market"] = crawler.dedupe_items(domestic["domestic_market"])
    domestic["global_market"] = crawler.dedupe_items(domestic["global_market"])
    domestic["ai_issue"] = crawler.dedupe_items(domestic["ai_issue"])

    fs_domestic_market: list[dict] = []
    for row in summarizer.summarize_batch(domestic["domestic_market"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_domestic_market.append(summarizer.merge_to_firestore_article(row, ai))
    fs_global_market: list[dict] = []
    for row in summarizer.summarize_batch(domestic["global_market"], news_feed="global_market"):
        ai = row.pop("_ai", None)
        if ai:
            fs_global_market.append(summarizer.merge_to_firestore_article(row, ai))
    fs_ai_issue: list[dict] = []
    for row in summarizer.summarize_batch(domestic["ai_issue"], news_feed="ai_issue"):
        ai = row.pop("_ai", None)
        if ai:
            fs_ai_issue.append(summarizer.merge_to_firestore_article(row, ai))

    firebase_client.save_news_feed(fs_domestic_market, "domestic_market")
    firebase_client.save_news_feed(fs_global_market, "global_market")
    firebase_client.save_news_feed(fs_ai_issue, "ai_issue")

    # --- 헤드라인 화면(한국/미국)용: 카테고리별 RSS → 요약/번역 → 저장 ---
    # Firestore 문서 ID는 앱에서 그대로 구독한다.
    # kr_domestic_* : 키워드·LLM보강 없이, `crawl_domestic` domestic_market 과 동일(48h·피드당 15·대괄호/중복·최신 15).
    for doc_id, url_list in (
        ("kr_domestic_stock", crawler.RSS_FEEDS_KR_HEADLINE_STOCK),
        ("kr_domestic_economy", crawler.RSS_FEEDS_KR_HEADLINE_ECONOMY),
    ):
        picked = crawler.crawl_rss_kr_headline_leg(url_list)
        fs_kr: list[dict] = []
        for row in summarizer.summarize_batch(picked, news_feed="domestic_market"):
            ai = row.pop("_ai", None)
            if ai:
                fs_kr.append(summarizer.merge_to_firestore_article(row, ai))
        firebase_client.save_news_feed(fs_kr, doc_id)

    # 미국 헤드라인: 백필(기간 확장) + 광고/의견 필터 (기존)
    TARGET_ARTICLE_COUNT = 15
    BACKFILL_STEP_HOURS = 168.0  # 1주
    BACKFILL_MAX_AGE_HOURS = 168.0 * 4  # 최대 4주까지 확장

    def _looks_like_ad_or_opinion(row: dict) -> bool:
        text = (
            f"{row.get('title','')}\n{row.get('summary','')}\n{row.get('body','')}"
        ).lower()
        ad_markers = (
            "광고",
            "협찬",
            "제휴",
            "후원",
            "sponsored",
            "sponsor",
            "프로모션",
            "promotion",
            "이벤트",
            "event",
            "할인",
            "특가",
            "쿠폰",
            "보도자료",
            "자료제공",
            "press release",
            "문의",
            "상담",
            "예약",
            "신청",
            "구매",
            "구독",
        )
        if any(m in text for m in ad_markers):
            return True
        opinion_markers = (
            "칼럼",
            "기고",
            "사설",
            "논설",
            "오피니언",
            "opinion",
            "기자수첩",
            "데스크칼럼",
            "기자의 시선",
            "기자 생각",
            "에디터",
            "editorial",
        )
        if any(m in text for m in opinion_markers):
            return True
        clickbait_markers = (
            "지금 사야",
            "무조건",
            "대박",
            "초대박",
            "충격",
            "반전",
            "비밀",
            "단독",
            "!!",
            "!!!",
        )
        if any(m in text for m in clickbait_markers):
            return True
        return False

    def _sort_newest_first(rows: list[dict]) -> list[dict]:
        # published_at은 crawler에서 UTC ISO 문자열로 들어간다(없으면 빈 문자열).
        return sorted(rows, key=lambda r: str(r.get("published_at") or ""), reverse=True)

    def _backfill_pick(
        *,
        urls: list[str],
        max_items_per_feed: int,
        attach_mk_hankyung_body: bool,
    ) -> list[dict]:
        """
        1주 → 2주 → ... 로 max_age_hours 를 늘리며 수집, 광고/의견/클릭베이트 제외 후
        TARGET_ARTICLE_COUNT (미국 헤드라인) 채우기. 국내 kr_domestic_* 는 사용하지 않는다.
        """
        collected: list[dict] = []
        age = BACKFILL_STEP_HOURS
        while True:
            rows = crawler.crawl_rss_headline_urls(
                urls,
                max_items_per_feed=max_items_per_feed,
                max_age_hours=age,
                attach_mk_hankyung_body=attach_mk_hankyung_body,
            )
            collected = crawler.dedupe_items(collected + rows)
            picked = [r for r in collected if not _looks_like_ad_or_opinion(r)]
            picked = _sort_newest_first(picked)
            if len(picked) >= TARGET_ARTICLE_COUNT:
                return picked[:TARGET_ARTICLE_COUNT]
            if age >= BACKFILL_MAX_AGE_HOURS:
                return picked
            age += BACKFILL_STEP_HOURS

    us_categories: list[tuple[str, str]] = [
        (
            "us_software_internet",
            "https://finance.yahoo.com/rss/headline?s=AAPL,MSFT,GOOGL,META,AMZN,CRM,NOW,ADBE,ORCL,FIG,PLTR,SNOW",
        ),
        (
            "us_semiconductor_hw",
            "https://finance.yahoo.com/rss/headline?s=NVDA,AMD,TSM,INTC,ASML,LRCX,ARM,QCOM,AVGO,ANET,MU,WDC,STX,SNDK",
        ),
        (
            "us_aerospace_mobility",
            "https://finance.yahoo.com/rss/headline?s=LMT,BA,NOC,RKLB,ASTS,LUNR,PL,BKSY,IRDM,SPCE,TSLA",
        ),
        (
            "us_finance_capital",
            "https://finance.yahoo.com/rss/headline?s=JPM,GS,MS,BLK,BRK-B,BX,KKR",
        ),
    ]

    for doc_id, url in us_categories:
        rows = _backfill_pick(
            urls=[url],
            max_items_per_feed=45,
            attach_mk_hankyung_body=False,
        )
        fs: list[dict] = []
        for row in summarizer.summarize_batch(rows, news_feed="global_market"):
            ai = row.pop("_ai", None)
            if ai:
                fs.append(summarizer.merge_to_firestore_article(row, ai))
        firebase_client.save_news_feed(fs, doc_id)


def _run_market_only(crawler: Any, firebase_client: Any) -> None:
    if not _market_pipeline_enabled():
        logger.info("마켓 파이프라인 비활성 — _run_market_only 스킵")
        return
    naver_themes = crawler.crawl_naver_stock_themes()
    naver_upjong = crawler.crawl_naver_stock_upjong()
    firebase_client.save_market_themes(naver_themes)
    firebase_client.save_market_industries(naver_upjong)


def _run_pipeline_full(crawler: Any, firebase_client: Any, summarizer: Any) -> None:
    try:
        firebase_client.clear_news_feeds()
    except Exception as e:
        logger.exception("뉴스 피드 삭제 단계 실패, 크롤링 계속: %s", e)

    ai_cfg = firebase_client.get_ai_config()
    summarizer.configure_ai(selected_model=str(ai_cfg.get("selected_model", "gemini")))

    domestic = crawler.crawl_domestic()
    domestic["domestic_market"] = crawler.dedupe_items(domestic["domestic_market"])
    domestic["global_market"] = crawler.dedupe_items(domestic["global_market"])
    domestic["ai_issue"] = crawler.dedupe_items(domestic["ai_issue"])

    if _market_pipeline_enabled():
        indicators = crawler.crawl_market_indicators()
        naver_themes = crawler.crawl_naver_stock_themes()
        naver_upjong = crawler.crawl_naver_stock_upjong()
    else:
        logger.info("마켓 파이프라인 비활성: 지표·테마·업종 크롤·시황 요약·Firestore 마켓 저장 스킵")
        indicators = []
        naver_themes = []
        naver_upjong = []

    counts = {
        "domestic_market": len(domestic["domestic_market"]),
        "global_market": len(domestic["global_market"]),
        "ai_issue": len(domestic["ai_issue"]),
        "indicators": len(indicators),
        "theme_count": len(naver_themes),
        "upjong_count": len(naver_upjong),
    }
    logger.info("크롤 완료: %s", counts)

    fs_domestic_market: list[dict] = []
    for row in summarizer.summarize_batch(domestic["domestic_market"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_domestic_market.append(summarizer.merge_to_firestore_article(row, ai))

    fs_global_market: list[dict] = []
    for row in summarizer.summarize_batch(domestic["global_market"], news_feed="global_market"):
        ai = row.pop("_ai", None)
        if ai:
            fs_global_market.append(summarizer.merge_to_firestore_article(row, ai))

    fs_ai_issue: list[dict] = []
    for row in summarizer.summarize_batch(domestic["ai_issue"], news_feed="ai_issue"):
        ai = row.pop("_ai", None)
        if ai:
            fs_ai_issue.append(summarizer.merge_to_firestore_article(row, ai))

    if _market_pipeline_enabled():
        firebase_client.save_market_indicators(indicators)
        firebase_client.save_market_themes(naver_themes)
        firebase_client.save_market_industries(naver_upjong)

    firebase_client.save_news_feed(fs_domestic_market, "domestic_market")
    firebase_client.save_news_feed(fs_global_market, "global_market")
    firebase_client.save_news_feed(fs_ai_issue, "ai_issue")

    _schedule_unified_feed_push(firebase_client)

    total_fs = (
        len(fs_domestic_market)
        + len(fs_global_market)
        + len(fs_ai_issue)
    )
    logger.info(
        "완료 — 뉴스 저장: %s건, 지표: %s",
        total_fs,
        len(indicators),
    )


def run() -> None:
    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    )
    from dotenv import load_dotenv

    load_dotenv(Path(__file__).resolve().parent / ".env")

    import crawler
    import firebase_client
    import summarizer
    from datetime import datetime

    import pytz

    section = (os.environ.get("PIPELINE_SECTION") or PIPELINE_SECTION_ALL).strip().lower()
    if section == _LEGACY_PIPELINE_SECTION_NEWS:
        section = PIPELINE_SECTION_NEWS
    allowed = (
        PIPELINE_SECTION_ALL,
        PIPELINE_SECTION_FULL,
        PIPELINE_SECTION_NEWS,
        PIPELINE_SECTION_MARKET,
    )
    if section not in allowed:
        logger.warning("알 수 없는 PIPELINE_SECTION=%r — all 로 실행", section)
        section = PIPELINE_SECTION_ALL

    summarizer.reset_token_counters()
    kst = pytz.timezone("Asia/Seoul")
    started_at = datetime.now(kst)

    t0 = time.perf_counter()
    status = "success"
    error_message: str | None = None

    def _maybe_news() -> None:
        sk, rs = is_skip_day(PIPELINE_SECTION_NEWS)
        if sk:
            logger.info(
                "파이프라인 스킵: section=%s reason=%s",
                PIPELINE_SECTION_NEWS,
                rs,
            )
            return
        _run_news_only(crawler, firebase_client, summarizer)

    def _maybe_market() -> None:
        if not _market_pipeline_enabled():
            logger.info("마켓 파이프라인 비활성 — _maybe_market 스킵")
            return
        sk, rs = is_skip_day(PIPELINE_SECTION_MARKET)
        if sk:
            logger.info(
                "파이프라인 스킵: section=%s reason=%s",
                PIPELINE_SECTION_MARKET,
                rs,
            )
            return
        _run_market_only(crawler, firebase_client)

    try:
        if section == PIPELINE_SECTION_FULL:
            _run_pipeline_full(crawler, firebase_client, summarizer)
            elapsed = time.perf_counter() - t0
            logger.info("파이프라인 종료 section=%s, 소요 %.1f초", section, elapsed)
            return

        if section != PIPELINE_SECTION_ALL:
            skip, reason = is_skip_day(section)
            if skip:
                logger.info("파이프라인 스킵: section=%s reason=%s", section, reason)
                return

        if section == PIPELINE_SECTION_ALL:
            _maybe_news()
            _maybe_market()
            _schedule_unified_feed_push(firebase_client)
        elif section == PIPELINE_SECTION_NEWS:
            _run_news_only(crawler, firebase_client, summarizer)
            _schedule_unified_feed_push(firebase_client)
        elif section == PIPELINE_SECTION_MARKET:
            if not _market_pipeline_enabled():
                logger.info("마켓 파이프라인 비활성 — section=market 전체 스킵(푸시 없음)")
            else:
                _run_market_only(crawler, firebase_client)
                _schedule_unified_feed_push(firebase_client)

        elapsed = time.perf_counter() - t0
        logger.info("파이프라인 종료 section=%s, 소요 %.1f초", section, elapsed)
    except Exception as e:
        status = "error"
        error_message = str(e)
        logger.exception("파이프라인 오류 section=%s: %s", section, e)
        raise
    finally:
        finished_at = datetime.now(kst)
        usage = summarizer.get_token_usage()
        duration_seconds = int((finished_at - started_at).total_seconds())
        doc_id = f"{section}_{started_at.strftime('%Y%m%d_%H%M')}"
        try:
            firebase_client.write_pipeline_log(
                doc_id=doc_id,
                section=section,
                started_at_iso_kst=started_at.isoformat(),
                finished_at_iso_kst=finished_at.isoformat(),
                duration_seconds=duration_seconds,
                input_tokens=int(usage["input"]),
                output_tokens=int(usage["output"]),
                total_tokens=int(usage["total"]),
                model=str(usage.get("model") or ""),
                status=status,
                error_message=error_message,
            )
        except Exception:
            logger.exception("pipeline_logs 저장 실패 doc_id=%s", doc_id)


if __name__ == "__main__":
    run()
