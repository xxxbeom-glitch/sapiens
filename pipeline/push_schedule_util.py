"""
FCM 푸시 예약 시각·문구 (KST) 및 Firestore `push_schedule` 저장 헬퍼.

CI/로컬: 환경변수 `PUSH_KST_HOUR` (0–23)로 당일 정각(분 0)에 FCM을 보내고자 하는 시(한국)를 맞춘다.
(작업은 그보다 20분 앞, GitHub Actions `cron`으로 맞춤)

뉴스·마켓 갱신 후 **토픽 `sapiens_feed`로 예약 1건**만 쓴다(구 `news_update` / `market_update` 이중 푸시 없음).
"""
from __future__ import annotations

import os
import re
from datetime import datetime, timedelta
from typing import Any

import pytz
from google.cloud import firestore

# FCM 토픽 `sapiens_feed` — 앱 [FcmTopicSync]와 동일 문자열.
UNIFIED_FEED_TOPIC = "sapiens_feed"
UNIFIED_FEED_TITLE = "Sapiens"
UNIFIED_FEED_BODY = "새로운 뉴스가 업데이트 됐어요"


def unified_feed_push_content() -> tuple[str, str]:
    """뉴스·마켓 공통 예약 알림 문구 (토픽은 [UNIFIED_FEED_TOPIC])."""
    return UNIFIED_FEED_TITLE, UNIFIED_FEED_BODY


def get_today_6am_kst() -> str:
    kst = pytz.timezone("Asia/Seoul")
    today = datetime.now(kst).replace(hour=6, minute=0, second=0, microsecond=0)
    return today.isoformat()


def get_next_hour_kst() -> str:
    kst = pytz.timezone("Asia/Seoul")
    now = datetime.now(kst)
    next_hour = now.replace(minute=0, second=0, microsecond=0) + timedelta(hours=1)
    return next_hour.isoformat()


def schedule_push(
    db: Any,
    section: str,
    title: str,
    body: str,
    topic: str,
    scheduled_at: str,
) -> None:
    kst = pytz.timezone("Asia/Seoul")
    doc_id = f"{section}_{datetime.now(kst).strftime('%Y%m%d_%H%M')}"
    db.collection("push_schedule").document(doc_id).set(
        {
            "section": section,
            "title": title,
            "body": body,
            "topic": topic,
            "scheduled_at": scheduled_at,
            "status": "pending",
            "created_at": firestore.SERVER_TIMESTAMP,
        }
    )


def now_kst() -> datetime:
    return datetime.now(pytz.timezone("Asia/Seoul"))


def kst_today_at_hour(hour: int) -> datetime:
    """당일 KST `hour:00:00` (0–23)."""
    n = now_kst()
    if not 0 <= hour <= 23:
        raise ValueError("hour must be 0..23")
    return n.replace(hour=hour, minute=0, second=0, microsecond=0)


def next_kst_top_of_hour(after: datetime) -> datetime:
    """크롤 시각 이후의 다음 정각(KST). (PUSH_KST_HOUR 미지정·로컬 백업용)"""
    base = after.replace(minute=0, second=0, microsecond=0)
    return base + timedelta(hours=1)


def _parse_push_kst_hour() -> int | None:
    raw = (os.environ.get("PUSH_KST_HOUR") or "").strip()
    if not raw or not re.fullmatch(r"\d{1,2}", raw):
        return None
    h = int(raw, 10)
    if 0 <= h <= 23:
        return h
    return None


def resolve_push_target_kst() -> datetime:
    """PUSH_KST_HOUR가 있으면 당일 그 정각, 없으면 다음 정각(레거시)."""
    ph = _parse_push_kst_hour()
    if ph is not None:
        return kst_today_at_hour(ph)
    return next_kst_top_of_hour(now_kst())


def kst_to_utc_iso_z(dt_kst: datetime) -> str:
    """Cloud Functions 스케줄 비교용 UTC ISO 문자열 (밀리초 0, Z)."""
    if dt_kst.tzinfo is None:
        dt_kst = pytz.timezone("Asia/Seoul").localize(dt_kst)
    return dt_kst.astimezone(pytz.UTC).strftime("%Y-%m-%dT%H:%M:%S.000Z")


def push_doc_id(section: str, scheduled_kst: datetime) -> str:
    return f"{section}_{scheduled_kst.strftime('%Y%m%d_%H%M')}"
