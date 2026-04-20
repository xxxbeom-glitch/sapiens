"""
FCM 푸시 예약 시각·문구 (KST) 및 Firestore `push_schedule` 저장 헬퍼.
"""
from __future__ import annotations

from datetime import datetime, timedelta
from typing import Any

import pytz
from google.cloud import firestore


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


def next_kst_top_of_hour(after: datetime) -> datetime:
    """크롤 시각 이후의 다음 정각(KST)."""
    base = after.replace(minute=0, second=0, microsecond=0)
    return base + timedelta(hours=1)


def next_briefing_push_kst(now_kst: datetime) -> datetime:
    """당일 06:00 KST. 이미 지났으면 익일 06:00."""
    target = now_kst.replace(hour=6, minute=0, second=0, microsecond=0)
    if now_kst < target:
        return target
    return target + timedelta(days=1)


def domestic_news_title_body(scheduled_kst: datetime) -> tuple[str, str]:
    h = scheduled_kst.hour
    if h <= 8:
        return ("📰 오전 뉴스", "장 시작 전 주요 경제 뉴스")
    if h <= 11:
        return ("📊 오전장 뉴스", "장중 주요 뉴스 업데이트")
    return ("📋 오후 뉴스", "오늘의 주요 경제 뉴스 정리")


def market_title_body(scheduled_kst: datetime) -> tuple[str, str]:
    if scheduled_kst.hour <= 10:
        return ("📈 마켓 현황", "오전장 테마·업종 동향")
    return ("📉 장 마감 마켓", "오늘 증시 테마·업종 결산")


def kst_to_utc_iso_z(dt_kst: datetime) -> str:
    """Cloud Functions 스케줄 비교용 UTC ISO 문자열 (밀리초 0, Z)."""
    if dt_kst.tzinfo is None:
        dt_kst = pytz.timezone("Asia/Seoul").localize(dt_kst)
    return dt_kst.astimezone(pytz.UTC).strftime("%Y-%m-%dT%H:%M:%S.000Z")


def push_doc_id(section: str, scheduled_kst: datetime) -> str:
    return f"{section}_{scheduled_kst.strftime('%Y%m%d_%H%M')}"
