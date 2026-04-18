"""
Firestore (database: sapiens) 저장.
"""
from __future__ import annotations

import base64
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

    b64 = os.environ.get("FIREBASE_SERVICE_ACCOUNT_B64", "").strip()
    if b64:
        cred_dict = json.loads(base64.b64decode(b64).decode("utf-8"), strict=False)
        cred = credentials.Certificate(cred_dict)
    else:
        cred_json = os.environ.get("FIREBASE_SERVICE_ACCOUNT", "").strip()
        if cred_json:
            cred_dict = json.loads(cred_json, strict=False)
            cred = credentials.Certificate(cred_dict)
        else:
            path = os.environ.get("FIREBASE_SERVICE_ACCOUNT_PATH", "").strip()
            if not path or not os.path.isfile(path):
                raise FileNotFoundError(
                    "Firebase 자격 증명이 필요합니다. 우선순위: "
                    "FIREBASE_SERVICE_ACCOUNT_B64 (base64 인코딩 JSON), "
                    "FIREBASE_SERVICE_ACCOUNT (평문 JSON 문자열), "
                    "FIREBASE_SERVICE_ACCOUNT_PATH (JSON 파일 경로)."
                )
            with open(path, encoding="utf-8") as f:
                content = f.read()
            cred_dict = json.loads(content, strict=False)
            cred = credentials.Certificate(cred_dict)
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


def save_us_market_articles(articles: List[dict[str, Any]]) -> None:
    """briefing/us_market 문서에 articles (Yahoo market 뉴스 요약 등)."""
    db = _get_db()
    db.collection("briefing").document("us_market").set(
        {
            "articles": articles,
            "updated_at": SERVER_TIMESTAMP,
        },
        merge=True,
    )


def save_us_articles(articles: List[dict[str, Any]]) -> None:
    """briefing/us_market — `save_us_market_articles` 별칭 (하위 호환)."""
    save_us_market_articles(articles)


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


def save_overseas_stocks_articles(articles: List[dict[str, Any]]) -> None:
    """news/overseas_stocks 문서에 articles 배열 + updated_at (Yahoo 뉴스 HTML 피드)."""
    db = _get_db()
    db.collection("news").document("overseas_stocks").set(
        {
            "articles": articles,
            "updated_at": SERVER_TIMESTAMP,
        },
        merge=True,
    )


def save_overseas_tech_articles(articles: List[dict[str, Any]]) -> None:
    """news/overseas_tech 문서에 articles 배열 + updated_at (Yahoo Tech HTML 피드)."""
    db = _get_db()
    db.collection("news").document("overseas_tech").set(
        {
            "articles": articles,
            "updated_at": SERVER_TIMESTAMP,
        },
        merge=True,
    )
