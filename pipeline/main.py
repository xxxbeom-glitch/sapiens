"""
뉴스 크롤링 → Gemini 요약 → Firestore 저장.

PIPELINE_SECTION 환경변수:
  all (기본) | briefing | domestic_news | overseas_news | market | full

  - all: 아래 네 블록을 순서대로 실행(각 블록은 해당 섹션의 휴장 규칙으로 개별 스킵).
  - full: 레거시 전체 파이프라인(피드 초기화 + 종목 루프 포함).

RunPod 등에서 `from main import run` 시 import 단계에서 부작용·무거운 초기화가
돌아가지 않도록, 로깅 설정·환경 로드·하위 모듈 import는 모두 `run()` 안에서 수행합니다.
"""
from __future__ import annotations

import logging
import os
import time
from pathlib import Path
from typing import Any, Tuple

logger = logging.getLogger("pipeline")

BRIEFING_NEWSPAPER_N = 5

PIPELINE_SECTION_ALL = "all"
PIPELINE_SECTION_FULL = "full"
PIPELINE_SECTION_BRIEFING = "briefing"
PIPELINE_SECTION_DOMESTIC_NEWS = "domestic_news"
PIPELINE_SECTION_MARKET = "market"
PIPELINE_SECTION_OVERSEAS_NEWS = "overseas_news"


def is_skip_day(section: str) -> Tuple[bool, str]:
    """
    (스킵 여부, 사유).
    section: briefing | domestic_news | market | overseas_news | full | all
    """
    from datetime import datetime

    import holidays
    import pytz
    from pandas_market_calendars import get_calendar

    s = (section or "").strip().lower()
    if s in ("all", "full"):
        return False, ""

    kst = pytz.timezone("Asia/Seoul")
    now = datetime.now(kst)

    if now.weekday() == 6:
        return True, "일요일"

    if s in ("briefing", "market"):
        kr_holidays = holidays.KR(years=now.year)
        if now.date() in kr_holidays:
            return True, f"한국 공휴일: {kr_holidays[now.date()]}"

    if s in ("briefing", "overseas_news"):
        nyse = get_calendar("NYSE")
        schedule = nyse.schedule(
            start_date=now.strftime("%Y-%m-%d"),
            end_date=now.strftime("%Y-%m-%d"),
        )
        if schedule.empty:
            return True, "NYSE 휴장일"

    return False, ""


def _sorted_newspaper_top(rows: list[dict[str, Any]], n: int) -> list[dict[str, Any]]:
    if not rows:
        return []
    return sorted(
        rows,
        key=lambda r: (int(r.get("paper_number", 999)), int(r.get("detail_position", 99))),
    )[:n]


def _summarize_newspaper_pool_for_briefing(
    summarizer: Any, pool: list[dict[str, Any]]
) -> list[dict[str, Any]]:
    """요약 실패 시 스텁으로 merge. pool 길이만큼(최대 5) 순서 유지."""
    stub = {"headline": "", "category": "", "summary_points": []}
    summarized = summarizer.summarize_batch(pool, briefing_newspaper=True)
    by_url: dict[str, dict[str, Any]] = {}
    for row in summarized:
        r = dict(row)
        ai = r.pop("_ai", None)
        if ai:
            u = (r.get("url") or "").strip()
            if u:
                by_url[u] = ai
    out: list[dict[str, Any]] = []
    seen_url: set[str] = set()
    for row in pool:
        u = (row.get("url") or "").strip()
        if u and u in seen_url:
            continue
        ai = by_url.get(u) if u else None
        if ai is None:
            ai = stub
            logger.warning(
                "briefing 신문 요약 폴백: %s",
                (row.get("title") or "")[:80],
            )
        out.append(summarizer.merge_to_firestore_article(dict(row), ai))
        if u:
            seen_url.add(u)
    return out


def _schedule_briefing_push(firebase_client: Any) -> None:
    import push_schedule_util as psu

    now_k = psu.now_kst()
    when_k = psu.next_briefing_push_kst(now_k)
    firebase_client.write_push_schedule_entry(
        doc_id=psu.push_doc_id("briefing", when_k),
        section="briefing",
        scheduled_at_utc_iso=psu.kst_to_utc_iso_z(when_k),
        title="☀️ 모닝 브리핑",
        body="오늘 꼭 알아야 할 국내외 주요 뉴스",
        topic="briefing_update",
    )


def _schedule_domestic_news_push(firebase_client: Any) -> None:
    import push_schedule_util as psu

    now_k = psu.now_kst()
    when_k = psu.next_kst_top_of_hour(now_k)
    title, body = psu.domestic_news_title_body(when_k)
    firebase_client.write_push_schedule_entry(
        doc_id=psu.push_doc_id("domestic_news", when_k),
        section="domestic_news",
        scheduled_at_utc_iso=psu.kst_to_utc_iso_z(when_k),
        title=title,
        body=body,
        topic="news_update",
    )


def _schedule_market_push(firebase_client: Any) -> None:
    import push_schedule_util as psu

    now_k = psu.now_kst()
    when_k = psu.next_kst_top_of_hour(now_k)
    title, body = psu.market_title_body(when_k)
    firebase_client.write_push_schedule_entry(
        doc_id=psu.push_doc_id("market", when_k),
        section="market",
        scheduled_at_utc_iso=psu.kst_to_utc_iso_z(when_k),
        title=title,
        body=body,
        topic="market_update",
    )


def _run_domestic_news_only(crawler: Any, firebase_client: Any, summarizer: Any) -> None:
    domestic = crawler.crawl_domestic()
    domestic["realtime"] = crawler.dedupe_items(domestic["realtime"])
    domestic["popular"] = crawler.dedupe_items(domestic["popular"])
    domestic["main"] = crawler.dedupe_items(domestic["main"])

    ai_cfg = firebase_client.get_ai_config()
    summarizer.configure_ai(selected_model=str(ai_cfg.get("selected_model", "gemini")))

    fs_realtime: list[dict] = []
    for row in summarizer.summarize_batch(domestic["realtime"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_realtime.append(summarizer.merge_to_firestore_article(row, ai))
    fs_popular: list[dict] = []
    for row in summarizer.summarize_batch(domestic["popular"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_popular.append(summarizer.merge_to_firestore_article(row, ai))
    fs_main: list[dict] = []
    for row in summarizer.summarize_batch(domestic["main"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_main.append(summarizer.merge_to_firestore_article(row, ai))

    firebase_client.save_news_feed(fs_realtime, "realtime")
    firebase_client.save_news_feed(fs_popular, "popular")
    firebase_client.save_news_feed(fs_main, "main")
    _schedule_domestic_news_push(firebase_client)


def _run_overseas_news_only(crawler: Any, firebase_client: Any, summarizer: Any) -> None:
    overseas_stocks = crawler.crawl_rss_overseas_stocks()
    overseas_tech = crawler.crawl_rss_overseas_tech()

    ai_cfg = firebase_client.get_ai_config()
    summarizer.configure_ai(selected_model=str(ai_cfg.get("selected_model", "gemini")))

    fs_overseas_stocks: list[dict] = []
    for row in summarizer.summarize_batch(overseas_stocks):
        ai = row.pop("_ai", None)
        if ai:
            fs_overseas_stocks.append(summarizer.merge_to_firestore_article(row, ai))

    fs_overseas_tech: list[dict] = []
    for row in summarizer.summarize_batch(overseas_tech):
        ai = row.pop("_ai", None)
        if ai:
            fs_overseas_tech.append(summarizer.merge_to_firestore_article(row, ai))

    firebase_client.save_overseas_stocks_articles(fs_overseas_stocks)
    firebase_client.save_overseas_tech_articles(fs_overseas_tech)


def _run_market_only(crawler: Any, firebase_client: Any) -> None:
    naver_themes = crawler.crawl_naver_stock_themes()
    naver_upjong = crawler.crawl_naver_stock_upjong()
    firebase_client.save_market_themes(naver_themes)
    firebase_client.save_market_industries(naver_upjong)
    _schedule_market_push(firebase_client)


def _run_briefing_only(crawler: Any, firebase_client: Any, summarizer: Any) -> None:
    briefing_overseas_raw = crawler.crawl_rss_briefing_overseas()
    newspaper_hankyung = crawler.crawl_hankyung_newspaper()
    newspaper_maeil = crawler.crawl_maeil_newspaper()
    pool_hankyung = _sorted_newspaper_top(newspaper_hankyung, BRIEFING_NEWSPAPER_N)
    pool_maeil = _sorted_newspaper_top(newspaper_maeil, BRIEFING_NEWSPAPER_N)
    indicators = crawler.crawl_market_indicators()

    ai_cfg = firebase_client.get_ai_config()
    summarizer.configure_ai(selected_model=str(ai_cfg.get("selected_model", "gemini")))

    fs_hankyung = _summarize_newspaper_pool_for_briefing(summarizer, pool_hankyung)
    fs_maeil = _summarize_newspaper_pool_for_briefing(summarizer, pool_maeil)

    us_pool: list[dict] = []
    if briefing_overseas_raw:
        try:
            us_pool = summarizer.curate_us_market_articles(briefing_overseas_raw)
        except Exception as e:
            logger.warning("curate_us_market_articles 실패, 전체로 요약 시도: %s", e)
            us_pool = briefing_overseas_raw[:8]
    fs_us_market: list[dict] = []
    for row in summarizer.summarize_batch(us_pool):
        ai = row.pop("_ai", None)
        if ai:
            fs_us_market.append(summarizer.merge_to_firestore_article(row, ai))

    firebase_client.save_briefing_hankyung_articles(fs_hankyung)
    firebase_client.save_briefing_maeil_articles(fs_maeil)
    firebase_client.save_us_market_articles(fs_us_market)
    firebase_client.save_market_indicators(indicators)
    _schedule_briefing_push(firebase_client)


def _run_pipeline_full(crawler: Any, firebase_client: Any, summarizer: Any) -> None:
    try:
        firebase_client.clear_news_and_briefing_feeds()
    except Exception as e:
        logger.exception("뉴스·브리핑 피드 삭제 단계 실패, 크롤링 계속: %s", e)

    domestic = crawler.crawl_domestic()
    domestic["realtime"] = crawler.dedupe_items(domestic["realtime"])
    domestic["popular"] = crawler.dedupe_items(domestic["popular"])
    domestic["main"] = crawler.dedupe_items(domestic["main"])

    overseas_stocks = crawler.crawl_rss_overseas_stocks()
    overseas_tech = crawler.crawl_rss_overseas_tech()
    briefing_overseas_raw = crawler.crawl_rss_briefing_overseas()
    newspaper_hankyung = crawler.crawl_hankyung_newspaper()
    newspaper_maeil = crawler.crawl_maeil_newspaper()
    pool_hankyung = _sorted_newspaper_top(newspaper_hankyung, BRIEFING_NEWSPAPER_N)
    pool_maeil = _sorted_newspaper_top(newspaper_maeil, BRIEFING_NEWSPAPER_N)

    indicators = crawler.crawl_market_indicators()
    naver_themes = crawler.crawl_naver_stock_themes()
    naver_upjong = crawler.crawl_naver_stock_upjong()

    counts = {
        "realtime": len(domestic["realtime"]),
        "popular": len(domestic["popular"]),
        "main": len(domestic["main"]),
        "overseas_stocks": len(overseas_stocks),
        "overseas_tech": len(overseas_tech),
        "briefing_overseas_rss": len(briefing_overseas_raw),
        "briefing_hankyung_pool": len(pool_hankyung),
        "briefing_maeil_pool": len(pool_maeil),
        "indicators": len(indicators),
        "theme_count": len(naver_themes),
        "upjong_count": len(naver_upjong),
    }
    logger.info("크롤 완료: %s", counts)

    ai_cfg = firebase_client.get_ai_config()
    summarizer.configure_ai(selected_model=str(ai_cfg.get("selected_model", "gemini")))

    fs_realtime: list[dict] = []
    for row in summarizer.summarize_batch(domestic["realtime"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_realtime.append(summarizer.merge_to_firestore_article(row, ai))

    fs_popular: list[dict] = []
    for row in summarizer.summarize_batch(domestic["popular"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_popular.append(summarizer.merge_to_firestore_article(row, ai))

    fs_main: list[dict] = []
    for row in summarizer.summarize_batch(domestic["main"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_main.append(summarizer.merge_to_firestore_article(row, ai))

    fs_hankyung = _summarize_newspaper_pool_for_briefing(summarizer, pool_hankyung)
    fs_maeil = _summarize_newspaper_pool_for_briefing(summarizer, pool_maeil)

    fs_overseas_stocks: list[dict] = []
    for row in summarizer.summarize_batch(overseas_stocks):
        ai = row.pop("_ai", None)
        if ai:
            fs_overseas_stocks.append(summarizer.merge_to_firestore_article(row, ai))

    fs_overseas_tech: list[dict] = []
    for row in summarizer.summarize_batch(overseas_tech):
        ai = row.pop("_ai", None)
        if ai:
            fs_overseas_tech.append(summarizer.merge_to_firestore_article(row, ai))

    us_pool: list[dict] = []
    if briefing_overseas_raw:
        try:
            us_pool = summarizer.curate_us_market_articles(briefing_overseas_raw)
        except Exception as e:
            logger.warning("curate_us_market_articles 실패, 전체로 요약 시도: %s", e)
            us_pool = briefing_overseas_raw[:8]
    fs_us_market: list[dict] = []
    for row in summarizer.summarize_batch(us_pool):
        ai = row.pop("_ai", None)
        if ai:
            fs_us_market.append(summarizer.merge_to_firestore_article(row, ai))

    try:
        fs_for_report: list[dict] = list(fs_overseas_stocks) + list(fs_overseas_tech)
        report = summarizer.generate_market_report(indicators, fs_for_report)
        logger.info("시황 요약:\n%s", report.get("report", "")[:500])
    except Exception as e:
        logger.warning("시황 요약 생성 실패: %s", e)

    firebase_client.save_briefing_hankyung_articles(fs_hankyung)
    firebase_client.save_briefing_maeil_articles(fs_maeil)
    firebase_client.save_overseas_stocks_articles(fs_overseas_stocks)
    firebase_client.save_overseas_tech_articles(fs_overseas_tech)
    firebase_client.save_us_market_articles(fs_us_market)
    firebase_client.save_market_indicators(indicators)
    firebase_client.save_market_themes(naver_themes)
    firebase_client.save_market_industries(naver_upjong)
    firebase_client.save_news_feed(fs_realtime, "realtime")
    firebase_client.save_news_feed(fs_popular, "popular")
    firebase_client.save_news_feed(fs_main, "main")

    _schedule_domestic_news_push(firebase_client)
    _schedule_briefing_push(firebase_client)
    _schedule_market_push(firebase_client)

    company_saved = 0
    for bundle in crawler.crawl_all_company_bundles():
        ticker = str(bundle.get("ticker", "")).strip()
        if not ticker:
            continue
        try:
            name = str(bundle.get("name") or ticker)
            fin = bundle.get("financials") or {}
            he = bundle.get("health") or {}
            ana = bundle.get("analyst") or {}
            prof = bundle.get("profile") or {}
            ai_company = summarizer.analyze_company(ticker, name, fin, he, ana, prof)
            doc: dict = {
                "ticker": ticker,
                "name": name,
                "profile": prof,
                "financials": fin,
                "health": he,
                "analyst": ana,
            }
            doc.update(ai_company)
            firebase_client.save_company_data(ticker, doc)
            company_saved += 1
        except Exception as e:
            logger.exception("종목 파이프라인 실패 [%s]: %s", ticker, e)
        time.sleep(0.5)

    total_fs = (
        len(fs_realtime)
        + len(fs_popular)
        + len(fs_main)
        + len(fs_hankyung)
        + len(fs_maeil)
        + len(fs_overseas_stocks)
        + len(fs_overseas_tech)
        + len(fs_us_market)
    )
    logger.info(
        "완료 — 뉴스 저장: %s건, 지표: %s, 종목 저장: %s",
        total_fs,
        len(indicators),
        company_saved,
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

    t0 = time.perf_counter()

    section = (os.environ.get("PIPELINE_SECTION") or PIPELINE_SECTION_ALL).strip().lower()
    allowed = (
        PIPELINE_SECTION_ALL,
        PIPELINE_SECTION_FULL,
        PIPELINE_SECTION_BRIEFING,
        PIPELINE_SECTION_DOMESTIC_NEWS,
        PIPELINE_SECTION_OVERSEAS_NEWS,
        PIPELINE_SECTION_MARKET,
    )
    if section not in allowed:
        logger.warning("알 수 없는 PIPELINE_SECTION=%r — all 로 실행", section)
        section = PIPELINE_SECTION_ALL

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

    def _maybe_briefing() -> None:
        sk, rs = is_skip_day(PIPELINE_SECTION_BRIEFING)
        if sk:
            logger.info(
                "파이프라인 스킵: section=%s reason=%s",
                PIPELINE_SECTION_BRIEFING,
                rs,
            )
            return
        _run_briefing_only(crawler, firebase_client, summarizer)

    def _maybe_domestic() -> None:
        sk, rs = is_skip_day(PIPELINE_SECTION_DOMESTIC_NEWS)
        if sk:
            logger.info(
                "파이프라인 스킵: section=%s reason=%s",
                PIPELINE_SECTION_DOMESTIC_NEWS,
                rs,
            )
            return
        _run_domestic_news_only(crawler, firebase_client, summarizer)

    def _maybe_overseas() -> None:
        sk, rs = is_skip_day(PIPELINE_SECTION_OVERSEAS_NEWS)
        if sk:
            logger.info(
                "파이프라인 스킵: section=%s reason=%s",
                PIPELINE_SECTION_OVERSEAS_NEWS,
                rs,
            )
            return
        _run_overseas_news_only(crawler, firebase_client, summarizer)

    def _maybe_market() -> None:
        sk, rs = is_skip_day(PIPELINE_SECTION_MARKET)
        if sk:
            logger.info(
                "파이프라인 스킵: section=%s reason=%s",
                PIPELINE_SECTION_MARKET,
                rs,
            )
            return
        _run_market_only(crawler, firebase_client)

    if section == PIPELINE_SECTION_ALL:
        _maybe_briefing()
        _maybe_domestic()
        _maybe_overseas()
        _maybe_market()
    elif section == PIPELINE_SECTION_BRIEFING:
        _run_briefing_only(crawler, firebase_client, summarizer)
    elif section == PIPELINE_SECTION_DOMESTIC_NEWS:
        _run_domestic_news_only(crawler, firebase_client, summarizer)
    elif section == PIPELINE_SECTION_OVERSEAS_NEWS:
        _run_overseas_news_only(crawler, firebase_client, summarizer)
    elif section == PIPELINE_SECTION_MARKET:
        _run_market_only(crawler, firebase_client)

    elapsed = time.perf_counter() - t0
    logger.info("파이프라인 종료 section=%s, 소요 %.1f초", section, elapsed)


if __name__ == "__main__":
    run()
