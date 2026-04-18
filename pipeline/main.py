"""
뉴스 크롤링 → Claude 요약 → Firestore 저장.

RunPod 등에서 `from main import run` 시 import 단계에서 부작용·무거운 초기화가
돌아가지 않도록, 로깅 설정·환경 로드·하위 모듈 import는 모두 `run()` 안에서 수행합니다.
"""
from __future__ import annotations

import logging
import os
import time
from pathlib import Path
from typing import Any

logger = logging.getLogger("pipeline")


def _build_client() -> Any:
    import anthropic

    key = os.environ.get("ANTHROPIC_API_KEY", "").strip()
    if not key:
        raise RuntimeError("ANTHROPIC_API_KEY 가 설정되어 있어야 합니다.")
    return anthropic.Anthropic(api_key=key)


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

    client = _build_client()

    # 1) 국내
    domestic = crawler.crawl_domestic()
    domestic["realtime"] = crawler.dedupe_items(domestic["realtime"])
    domestic["popular"] = crawler.dedupe_items(domestic["popular"])
    domestic["main"] = crawler.dedupe_items(domestic["main"])

    # 2) 해외: Yahoo Finance HTML(뉴스 / 테크 / morning-brief) + NAVER 아침 신문(한경+매경)
    overseas_stocks = crawler.crawl_yahoo_stocks()
    overseas_tech = crawler.crawl_yahoo_tech()
    yahoo_morning_brief = crawler.crawl_yahoo_morning_brief()
    newspaper_hankyung = crawler.crawl_hankyung_newspaper()
    newspaper_maeil = crawler.crawl_maeil_newspaper()
    newspaper_merged: list[dict] = list(newspaper_hankyung) + list(newspaper_maeil)
    newspaper_merged = crawler.dedupe_items(newspaper_merged)

    # 3) 시장 지표
    indicators = crawler.crawl_market_indicators()

    counts = {
        "realtime": len(domestic["realtime"]),
        "popular": len(domestic["popular"]),
        "main": len(domestic["main"]),
        "overseas_stocks": len(overseas_stocks),
        "overseas_tech": len(overseas_tech),
        "yahoo_morning_brief": len(yahoo_morning_brief),
        "newspaper_merged": len(newspaper_merged),
        "indicators": len(indicators),
    }
    logger.info("크롤 완료: %s", counts)

    # 4) Claude 요약 (국내 탭별)
    fs_realtime: list[dict] = []
    for row in summarizer.summarize_batch(client, domestic["realtime"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_realtime.append(summarizer.merge_to_firestore_article(row, ai))

    fs_popular: list[dict] = []
    for row in summarizer.summarize_batch(client, domestic["popular"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_popular.append(summarizer.merge_to_firestore_article(row, ai))

    fs_main: list[dict] = []
    for row in summarizer.summarize_batch(client, domestic["main"]):
        ai = row.pop("_ai", None)
        if ai:
            fs_main.append(summarizer.merge_to_firestore_article(row, ai))

    # 아침 브리핑(한국경제+매일경제 신문) → briefing/morning/articles
    fs_morning: list[dict] = []
    for row in summarizer.summarize_batch(client, newspaper_merged):
        ai = row.pop("_ai", None)
        if ai:
            fs_morning.append(summarizer.merge_to_firestore_article(row, ai))

    # Yahoo Finance HTML: 뉴스(스톡) / 테크 + morning-brief(→ briefing/us_market/articles)
    fs_overseas_stocks: list[dict] = []
    for row in summarizer.summarize_batch(client, overseas_stocks):
        ai = row.pop("_ai", None)
        if ai:
            fs_overseas_stocks.append(summarizer.merge_to_firestore_article(row, ai))

    fs_overseas_tech: list[dict] = []
    for row in summarizer.summarize_batch(client, overseas_tech):
        ai = row.pop("_ai", None)
        if ai:
            fs_overseas_tech.append(summarizer.merge_to_firestore_article(row, ai))

    fs_yahoo_morning_brief: list[dict] = []
    for row in summarizer.summarize_batch(client, yahoo_morning_brief):
        ai = row.pop("_ai", None)
        if ai:
            fs_yahoo_morning_brief.append(summarizer.merge_to_firestore_article(row, ai))

    # 시황 리포트: 해외(스톡/테크/morning-brief) 요약 + 국내 지표
    try:
        fs_for_report: list[dict] = (
            list(fs_overseas_stocks) + list(fs_overseas_tech) + list(fs_yahoo_morning_brief)
        )
        report = summarizer.generate_market_report(client, indicators, fs_for_report)
        logger.info("시황 요약:\n%s", report.get("report", "")[:500])
    except Exception as e:
        logger.warning("시황 요약 생성 실패: %s", e)

    # 5) Firestore
    firebase_client.save_morning_articles(fs_morning)
    firebase_client.save_overseas_stocks_articles(fs_overseas_stocks)
    firebase_client.save_overseas_tech_articles(fs_overseas_tech)
    firebase_client.save_us_market_articles(fs_yahoo_morning_brief)
    firebase_client.save_market_indicators(indicators)
    firebase_client.save_news_feed(fs_realtime, "realtime")
    firebase_client.save_news_feed(fs_popular, "popular")
    firebase_client.save_news_feed(fs_main, "main")

    # 7–9) 종목 재무 크롤 → Claude 분석 → companies 저장
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
            ai_company = summarizer.analyze_company(client, ticker, name, fin, he, ana, prof)
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
        + len(fs_morning)
        + len(fs_overseas_stocks)
        + len(fs_overseas_tech)
        + len(fs_yahoo_morning_brief)
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
