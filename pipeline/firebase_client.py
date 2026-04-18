"""
Firestore (database: sapiens) 저장.
"""
from __future__ import annotations

import json
import logging
import os
from typing import Any, List

import firebase_admin
from firebase_admin import credentials, firestore
from google.cloud.firestore import SERVER_TIMESTAMP

logger = logging.getLogger(__name__)

_db: firestore.Client | None = None


def _ensure_firebase_app() -> None:
    """기본 Firebase 앱이 없을 때만 서비스 계정으로 초기화. 중복 initialize_app 방지."""
    try:
        firebase_admin.get_app()
        return
    except ValueError:
        # 기본 앱이 아직 없음
        pass

    path = os.environ.get("FIREBASE_SERVICE_ACCOUNT_PATH", "").strip()
    if not path or not os.path.isfile(path):
        raise FileNotFoundError(
            "FIREBASE_SERVICE_ACCOUNT_PATH 가 유효한 서비스 계정 JSON 파일을 가리켜야 합니다."
        )

    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    # strict=False: PEM 등에 포함된 제어 문자로 인한 JSONDecodeError 완화
    json_data = json.loads(content, strict=False)
    cred = credentials.Certificate(json_data)
    try:
        firebase_admin.initialize_app(cred)
    except ValueError as e:
        msg = str(e).lower()
        if "already exists" in msg:
            return
        raise


def _get_db() -> firestore.Client:
    global _db
    if _db is not None:
        return _db

    _ensure_firebase_app()

    database_id = os.environ.get("DATABASE_ID", "sapiens").strip() or "sapiens"

    try:
        _db = firestore.client(database_id=database_id)
    except TypeError:
        logger.warning(
            "firestore.client(database_id=) 미지원 — 기본 DB 사용. firebase-admin 업그레이드를 권장합니다."
        )
        _db = firestore.client()
    return _db


def save_morning_articles(articles: List[dict[str, Any]]) -> None:
    """briefing/morning 문서에 articles + updated_at 저장."""
    db = _get_db()
    db.collection("briefing").document("morning").set(
        {
            "articles": articles,
            "updated_at": SERVER_TIMESTAMP,
        },
        merge=True,
    )


def save_us_articles(articles: List[dict[str, Any]]) -> None:
    """briefing/us_market 문서에 articles 병합 저장."""
    db = _get_db()
    db.collection("briefing").document("us_market").set(
        {
            "articles": articles,
            "updated_at": SERVER_TIMESTAMP,
        },
        merge=True,
    )


def save_market_indicators(indicators: List[dict[str, Any]]) -> None:
    """briefing/us_market 문서에 indicators 병합 저장."""
    db = _get_db()
    db.collection("briefing").document("us_market").set(
        {
            "indicators": indicators,
            "updated_at": SERVER_TIMESTAMP,
        },
        merge=True,
    )


def save_company_data(ticker: str, data: dict[str, Any]) -> None:
    """companies/{ticker} 문서에 병합 저장 + updated_at."""
    db = _get_db()
    db.collection("companies").document(ticker).set(
        {**data, "updated_at": SERVER_TIMESTAMP},
        merge=True,
    )


def save_news_feed(articles: List[dict[str, Any]], feed_type: str) -> None:
    """
    news/{feed_type} 문서에 저장.
    feed_type: realtime | popular | main
    """
    if feed_type not in ("realtime", "popular", "main"):
        raise ValueError(f"Invalid feed_type: {feed_type}")
    db = _get_db()
    db.collection("news").document(feed_type).set(
        {
            "articles": articles,
            "updated_at": SERVER_TIMESTAMP,
        },
        merge=True,
    )
