"""
뉴스 크롤링 → 카드 생성 → Firestore 저장.

PIPELINE_SECTION 환경변수:
  all (기본) | news | market

마켓(지표·네이버 테마/업종)은 기본 비활성.
켜려면 환경변수 `SAPIENS_MARKET_PIPELINE=1`.
"""
from __future__ import annotations

import logging
import os
import time
from pathlib import Path
from typing import Any, Tuple

logger = logging.getLogger("pipeline")

PIPELINE_SECTION_ALL = "all"
PIPELINE_SECTION_NEWS = "news"
PIPELINE_SECTION_MARKET = "market"


def _market_pipeline_enabled() -> bool:
    v = (os.environ.get("SAPIENS_MARKET_PIPELINE") or "").strip().lower()
    return v in ("1", "true", "yes", "on")


def is_skip_day(section: str) -> Tuple[bool, str]:
    from datetime import datetime
    import holidays
    import pytz

    s = (section or "").strip().lower()
    if s in ("all", "news"):
        return False, ""

    if s == "market":
        kst = pytz.timezone("Asia/Seoul")
        now = datetime.now(kst)
        if now.weekday() >= 5:
            return True, "주말"
        kr_holidays = holidays.KR(years=now.year)
        if now.date() in kr_holidays:
            return True, f"한국 공휴일: {kr_holidays[now.date()]}"
        return False, ""

    return False, ""


def _schedule_unified_feed_push(firebase_client: Any) -> None:
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


def _collect_all_articles(crawler: Any) -> list[dict]:
    """
    모든 RSS 피드에서 기사 수집 후 합산.
    domestic_market + global_market + ai_issue + naver(2종) 합침.
    """
    domestic = crawler.crawl_domestic()
    naver_realtime = crawler.crawl_naver_realtime()
    naver_main = crawler.crawl_naver_main()
    all_articles: list[dict] = []
    all_articles.extend(domestic.get("domestic_market", []))
    all_articles.extend(domestic.get("global_market", []))
    all_articles.extend(domestic.get("ai_issue", []))
    all_articles.extend(naver_realtime)
    all_articles.extend(naver_main)

    # URL 기준 중복 제거
    seen: set[str] = set()
    deduped: list[dict] = []
    for a in all_articles:
        u = (a.get("url") or "").strip()
        if u and u not in seen:
            seen.add(u)
            deduped.append(a)

    logger.info(
        "기사 수집: domestic=%d global=%d ai=%d naver_realtime=%d naver_main=%d → 중복제거 후 %d개",
        len(domestic.get("domestic_market", [])),
        len(domestic.get("global_market", [])),
        len(domestic.get("ai_issue", [])),
        len(naver_realtime),
        len(naver_main),
        len(deduped),
    )
    return deduped


def _run_news_only(crawler: Any, firebase_client: Any, summarizer: Any) -> None:
    """
    핵심 파이프라인:
    1) 전체 RSS 수집
    2) 필터링 → 분류 → 카드 6~7장 생성
    3) Firestore 저장
    """
    ai_cfg = firebase_client.get_ai_config()
    summarizer.configure_ai(selected_model=str(ai_cfg.get("selected_model", "gemini")))

    # TTL 보조 정리
    try:
        firebase_client.cleanup_expired_news_docs()
    except Exception:
        pass

    # 1) 기사 수집
    all_articles = _collect_all_articles(crawler)
    if not all_articles:
        logger.warning("수집된 기사 없음 — 파이프라인 종료")
        return

    # 2) 카드 생성
    cards = summarizer.generate_briefing_cards(all_articles)
    if not cards:
        logger.warning("생성된 카드 없음")
        return

    # 3) Firestore 저장
    firebase_client.save_briefing_cards(cards)
    logger.info("카드 저장 완료: %d장", len(cards))

    # 헤드라인(국내주식/경제) 별도 저장 — 기존 유지
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


def _run_market_only(crawler: Any, firebase_client: Any) -> None:
    if not _market_pipeline_enabled():
        logger.info("마켓 파이프라인 비활성 — 스킵")
        return
    naver_themes = crawler.crawl_naver_stock_themes()
    naver_upjong = crawler.crawl_naver_stock_upjong()
    firebase_client.save_market_themes(naver_themes)
    firebase_client.save_market_industries(naver_upjong)


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
    if section not in (PIPELINE_SECTION_ALL, PIPELINE_SECTION_NEWS, PIPELINE_SECTION_MARKET):
        logger.warning("알 수 없는 PIPELINE_SECTION=%r — all 로 실행", section)
        section = PIPELINE_SECTION_ALL

    summarizer.reset_token_counters()
    kst = pytz.timezone("Asia/Seoul")
    started_at = datetime.now(kst)
    t0 = time.perf_counter()
    status = "success"
    error_message: str | None = None

    try:
        if section == PIPELINE_SECTION_ALL:
            sk, rs = is_skip_day(PIPELINE_SECTION_NEWS)
            if not sk:
                _run_news_only(crawler, firebase_client, summarizer)
            else:
                logger.info("뉴스 스킵: %s", rs)

            if _market_pipeline_enabled():
                sk, rs = is_skip_day(PIPELINE_SECTION_MARKET)
                if not sk:
                    _run_market_only(crawler, firebase_client)
                else:
                    logger.info("마켓 스킵: %s", rs)

            _schedule_unified_feed_push(firebase_client)

        elif section == PIPELINE_SECTION_NEWS:
            sk, rs = is_skip_day(PIPELINE_SECTION_NEWS)
            if sk:
                logger.info("파이프라인 스킵: %s", rs)
            else:
                _run_news_only(crawler, firebase_client, summarizer)
                _schedule_unified_feed_push(firebase_client)

        elif section == PIPELINE_SECTION_MARKET:
            if not _market_pipeline_enabled():
                logger.info("마켓 파이프라인 비활성")
            else:
                sk, rs = is_skip_day(PIPELINE_SECTION_MARKET)
                if sk:
                    logger.info("파이프라인 스킵: %s", rs)
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
