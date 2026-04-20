"""
뉴스 크롤링 → Gemini 요약 → Firestore 저장.

RunPod 등에서 `from main import run` 시 import 단계에서 부작용·무거운 초기화가
돌아가지 않도록, 로깅 설정·환경 로드·하위 모듈 import는 모두 `run()` 안에서 수행합니다.
"""
from __future__ import annotations

import logging
import time
from pathlib import Path
from typing import Any

logger = logging.getLogger("pipeline")

BRIEFING_NEWSPAPER_N = 5


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

    try:
        firebase_client.clear_news_and_briefing_feeds()
    except Exception as e:
        logger.exception("뉴스·브리핑 피드 삭제 단계 실패, 크롤링 계속: %s", e)

    # 1) 국내
    domestic = crawler.crawl_domestic()
    domestic["realtime"] = crawler.dedupe_items(domestic["realtime"])
    domestic["popular"] = crawler.dedupe_items(domestic["popular"])
    domestic["main"] = crawler.dedupe_items(domestic["main"])

    # 2) 해외: Yahoo Finance(뉴스+테크 HTML) + NAVER 아침 신문(한경+매경)
    overseas_stocks, overseas_tech, yahoo_merged = crawler.crawl_yahoo_overseas_stocks_and_tech()
    newspaper_hankyung = crawler.crawl_hankyung_newspaper()
    newspaper_maeil = crawler.crawl_maeil_newspaper()
    pool_hankyung = _sorted_newspaper_top(newspaper_hankyung, BRIEFING_NEWSPAPER_N)
    pool_maeil = _sorted_newspaper_top(newspaper_maeil, BRIEFING_NEWSPAPER_N)

    # 3) 시장 지표 + 네이버 테마
    indicators = crawler.crawl_market_indicators()
    naver_themes = crawler.crawl_naver_stock_themes()
    naver_upjong = crawler.crawl_naver_stock_upjong()

    counts = {
        "realtime": len(domestic["realtime"]),
        "popular": len(domestic["popular"]),
        "main": len(domestic["main"]),
        "overseas_stocks": len(overseas_stocks),
        "overseas_tech": len(overseas_tech),
        "yahoo_merged": len(yahoo_merged),
        "briefing_hankyung_pool": len(pool_hankyung),
        "briefing_maeil_pool": len(pool_maeil),
        "indicators": len(indicators),
        "theme_count": len(naver_themes),
        "upjong_count": len(naver_upjong),
    }
    logger.info("크롤 완료: %s", counts)

    ai_cfg = firebase_client.get_ai_config()
    summarizer.configure_ai(selected_model=str(ai_cfg.get("selected_model", "gemini")))

    # 4) AI 요약 (국내 탭별 — Firebase settings/ai_config 분기)
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

    # 한경·매경 신문 브리핑 → briefing/hankyung, briefing/maeil (각 상위 5건, 언론사 간 중복 제거 없음)
    fs_hankyung = _summarize_newspaper_pool_for_briefing(summarizer, pool_hankyung)
    fs_maeil = _summarize_newspaper_pool_for_briefing(summarizer, pool_maeil)

    # Yahoo: 스톡/테크 각각 전체 요약 → news/overseas_* (기존과 동일)
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

    # 미국 시황 브리핑: 합친(raw) → Gemini 8개 엄선 → 요약 → briefing/us_market
    yahoo_picked: list[dict] = []
    if yahoo_merged:
        try:
            yahoo_picked = summarizer.curate_us_market_articles(yahoo_merged)
        except Exception as e:
            logger.warning("curate_us_market_articles 실패, 전체로 요약 시도: %s", e)
            yahoo_picked = yahoo_merged[:8]
    fs_us_market: list[dict] = []
    for row in summarizer.summarize_batch(yahoo_picked):
        ai = row.pop("_ai", None)
        if ai:
            fs_us_market.append(summarizer.merge_to_firestore_article(row, ai))

    # 시황 3문장 리포트: 국내 지표 + Yahoo 스톡·테크 전체 요약(피드)
    try:
        fs_for_report: list[dict] = list(fs_overseas_stocks) + list(fs_overseas_tech)
        report = summarizer.generate_market_report(indicators, fs_for_report)
        logger.info("시황 요약:\n%s", report.get("report", "")[:500])
    except Exception as e:
        logger.warning("시황 요약 생성 실패: %s", e)

    # 5) Firestore
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

    # 7–9) 종목 재무 크롤 → Gemini 분석 → companies 저장
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

    elapsed = time.perf_counter() - t0
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
        "완료 — 뉴스 저장: %s건, 지표: %s, 종목 저장: %s, 소요 %.1f초",
        total_fs,
        len(indicators),
        company_saved,
        elapsed,
    )


if __name__ == "__main__":
    run()
