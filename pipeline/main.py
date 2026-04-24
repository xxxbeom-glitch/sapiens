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
        try:
            fs_for_report: list[dict] = list(fs_global_market) + list(fs_ai_issue)
            report = summarizer.generate_market_report(indicators, fs_for_report)
            logger.info("시황 요약:\n%s", report.get("report", "")[:500])
        except Exception as e:
            logger.warning("시황 요약 생성 실패: %s", e)
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
