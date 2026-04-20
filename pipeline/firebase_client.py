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
from google.cloud.firestore import DELETE_FIELD, SERVER_TIMESTAMP

logger = logging.getLogger(__name__)

_db: firestore.Client | None = None

# 뉴스·브리핑 피드 문서(단일 문서 + articles 배열). briefing: 한경·매경 = 국내/해외 신문 브리핑 슬롯.
_NEWS_FEED_DOC_IDS = ("realtime", "popular", "main", "overseas_stocks", "overseas_tech")
_BRIEFING_FEED_DOC_IDS = ("hankyung", "maeil")  # 제품 상 domestic/overseas 신문 풀과 대응

_SAVED_ARTICLES = "saved_articles"
_MARKET = "market"
_MARKET_THEMES_PARENT_DOC = "themes"
_MARKET_THEMES_SUBCOL = "by_no"
_MARKET_INDUSTRIES_PARENT_DOC = "industries"
_MARKET_INDUSTRIES_SUBCOL = "by_no"
_MARKET_INDICATORS_DOC = "indicators"


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


def _list_saved_article_ids(db: firestore.Client) -> set[str]:
    """saved_articles 컬렉션의 문서 ID(article_id) 집합. 실패 시 빈 집합."""
    out: set[str] = set()
    try:
        for doc in db.collection(_SAVED_ARTICLES).stream():
            did = (doc.id or "").strip()
            if did:
                out.add(did)
    except Exception as e:
        logger.exception("saved_articles 문서 ID 조회 실패, 보호 목록 없이 진행: %s", e)
    return out


def _recursive_delete_subcollections(
    doc_ref: firestore.DocumentReference, protected_ids: set[str]
) -> None:
    """문서 하위 모든 서브컬렉션의 문서를 재귀 삭제(문서 ID가 protected_ids인 것은 스킵)."""
    try:
        subcols = doc_ref.collections()
    except Exception as e:
        logger.warning("서브컬렉션 목록 조회 실패 %s: %s", doc_ref.path, e)
        return
    for sub in subcols:
        try:
            for child in sub.stream():
                if child.id in protected_ids:
                    continue
                try:
                    _recursive_delete_subcollections(child.reference, protected_ids)
                    child.reference.delete()
                except Exception as e:
                    logger.exception("하위 문서 삭제 실패 %s: %s", child.reference.path, e)
        except Exception as e:
            logger.exception("서브컬렉션 %s/%s 순회 실패: %s", doc_ref.path, sub.id, e)


def _delete_feed_document(
    db: firestore.Client, collection: str, doc_id: str, protected_ids: set[str]
) -> None:
    """피드 문서 및(있을 경우) 하위 문서 삭제. 루트 문서 ID는 보호 목록과 무관하게 삭제."""
    ref = db.collection(collection).document(doc_id)
    try:
        snap = ref.get()
        if not snap.exists:
            return
    except Exception as e:
        logger.exception("피드 문서 조회 실패 %s/%s: %s", collection, doc_id, e)
        return
    try:
        _recursive_delete_subcollections(ref, protected_ids)
    except Exception as e:
        logger.exception("피드 하위 삭제 중 오류 %s/%s: %s", collection, doc_id, e)
    try:
        ref.delete()
    except Exception as e:
        logger.exception("피드 문서 삭제 실패 %s/%s: %s", collection, doc_id, e)


def clear_news_and_briefing_feeds() -> None:
    """
    크롤링 전 뉴스·브리핑 피드 문서를 비움.
    saved_articles 에 등록된 article_id와 동일한 ID의 하위 문서는 삭제하지 않음.
    """
    db = _get_db()
    protected = _list_saved_article_ids(db)
    for doc_id in _NEWS_FEED_DOC_IDS:
        _delete_feed_document(db, "news", doc_id, protected)
    for doc_id in _BRIEFING_FEED_DOC_IDS:
        _delete_feed_document(db, "briefing", doc_id, protected)


def save_briefing_hankyung_articles(articles: List[dict[str, Any]]) -> None:
    """briefing/hankyung 문서에 articles + updated_at 저장."""
    try:
        db = _get_db()
        db.collection("briefing").document("hankyung").set(
            {
                "articles": articles,
                "updated_at": SERVER_TIMESTAMP,
            },
            merge=False,
        )
    except Exception as e:
        logger.exception("save_briefing_hankyung_articles 실패: %s", e)


def save_briefing_maeil_articles(articles: List[dict[str, Any]]) -> None:
    """briefing/maeil 문서에 articles + updated_at 저장."""
    try:
        db = _get_db()
        db.collection("briefing").document("maeil").set(
            {
                "articles": articles,
                "updated_at": SERVER_TIMESTAMP,
            },
            merge=False,
        )
    except Exception as e:
        logger.exception("save_briefing_maeil_articles 실패: %s", e)


def save_us_market_articles(articles: List[dict[str, Any]]) -> None:
    """briefing/us_market 문서에 articles (Yahoo market 뉴스 요약 등)."""
    try:
        db = _get_db()
        db.collection("briefing").document("us_market").set(
            {
                "articles": articles,
                "updated_at": SERVER_TIMESTAMP,
            },
            merge=True,
        )
    except Exception as e:
        logger.exception("save_us_market_articles 실패: %s", e)


def save_us_articles(articles: List[dict[str, Any]]) -> None:
    """briefing/us_market — `save_us_market_articles` 별칭 (하위 호환)."""
    save_us_market_articles(articles)


def save_market_indicators(indicators: List[dict[str, Any]]) -> None:
    """market/indicators 단일 문서에 indicators + updatedAt 덮어쓰기(merge)."""
    try:
        db = _get_db()
        db.collection(_MARKET).document(_MARKET_INDICATORS_DOC).set(
            {
                "indicators": indicators,
                "updatedAt": SERVER_TIMESTAMP,
            },
            merge=True,
        )
    except Exception as e:
        logger.exception("save_market_indicators 실패: %s", e)


def save_market_themes(themes: List[dict[str, Any]]) -> None:
    """
    market/themes/by_no/{theme_no} 문서마다 name, change_rate, stocks, description,
    updatedAt, rank 를 set(merge=True). description: 네이버 테마 info categoryInfo.
    이번 크롤에 없는 theme_no 문서는 삭제(saved_articles 보호는 테마 경로와 무관).
    """
    db = _get_db()
    parent = (
        db.collection(_MARKET)
        .document(_MARKET_THEMES_PARENT_DOC)
        .collection(_MARKET_THEMES_SUBCOL)
    )
    current_ids: set[str] = set()
    for rank, theme in enumerate(themes):
        tid = str(theme.get("theme_id") or theme.get("theme_no") or "").strip()
        if not tid:
            logger.warning("save_market_themes: theme_id 없음, 스킵: %s", theme.get("theme_name", "")[:40])
            continue
        current_ids.add(tid)
        name = str(theme.get("theme_name") or "").strip()
        change_rate = str(theme.get("change_rate") or "")
        stocks = theme.get("stocks") or []
        description = str(theme.get("description") or "").strip()
        payload: dict[str, Any] = {
            "name": name,
            "change_rate": change_rate,
            "stocks": stocks,
            "description": description,
            "updatedAt": SERVER_TIMESTAMP,
            "rank": rank,
        }
        try:
            parent.document(tid).set(payload, merge=True)
        except Exception as e:
            logger.exception("테마 문서 저장 실패 theme_no=%s: %s", tid, e)

    try:
        for doc in parent.stream():
            if doc.id not in current_ids:
                try:
                    doc.reference.delete()
                except Exception as e:
                    logger.exception("구 테마 문서 삭제 실패 %s: %s", doc.reference.path, e)
    except Exception as e:
        logger.exception("테마 목록 조회(정리) 실패: %s", e)

    # 구버전 단일 문서 market/themes 의 themes[] 배열 필드 제거(서브컬렉션 by_no 는 유지).
    try:
        leg_ref = db.collection(_MARKET).document(_MARKET_THEMES_PARENT_DOC)
        legacy = leg_ref.get()
        if legacy.exists and "themes" in (legacy.to_dict() or {}):
            leg_ref.update({"themes": DELETE_FIELD})
    except Exception as e:
        logger.warning("구 market/themes 배열 필드 정리 실패(무시): %s", e)


def save_market_industries(industries: List[dict[str, Any]]) -> None:
    """
    market/industries/by_no/{no} 문서마다 name, change_rate, stocks, updatedAt, rank 를 set(merge=True).
    이번 크롤에 없는 no 문서는 삭제.
    """
    db = _get_db()
    parent = (
        db.collection(_MARKET)
        .document(_MARKET_INDUSTRIES_PARENT_DOC)
        .collection(_MARKET_INDUSTRIES_SUBCOL)
    )
    current_ids: set[str] = set()
    for rank, row in enumerate(industries):
        iid = str(
            row.get("industry_id")
            or row.get("industry_no")
            or row.get("no")
            or ""
        ).strip()
        if not iid:
            logger.warning(
                "save_market_industries: industry_id 없음, 스킵: %s",
                str(row.get("industry_name", ""))[:40],
            )
            continue
        current_ids.add(iid)
        name = str(row.get("industry_name") or row.get("name") or "").strip()
        change_rate = str(row.get("change_rate") or "")
        stocks = row.get("stocks") or []
        payload: dict[str, Any] = {
            "name": name,
            "change_rate": change_rate,
            "stocks": stocks,
            "updatedAt": SERVER_TIMESTAMP,
            "rank": rank,
        }
        try:
            parent.document(iid).set(payload, merge=True)
        except Exception as e:
            logger.exception("업종 문서 저장 실패 no=%s: %s", iid, e)

    try:
        for doc in parent.stream():
            if doc.id not in current_ids:
                try:
                    doc.reference.delete()
                except Exception as e:
                    logger.exception("구 업종 문서 삭제 실패 %s: %s", doc.reference.path, e)
    except Exception as e:
        logger.exception("업종 목록 조회(정리) 실패: %s", e)


def save_company_data(ticker: str, data: dict[str, Any]) -> None:
    """companies/{ticker} 문서에 병합 저장 + updated_at."""
    try:
        db = _get_db()
        db.collection("companies").document(ticker).set(
            {**data, "updated_at": SERVER_TIMESTAMP},
            merge=True,
        )
    except Exception as e:
        logger.exception("save_company_data 실패 [%s]: %s", ticker, e)


def save_news_feed(articles: List[dict[str, Any]], feed_type: str) -> None:
    """
    news/{feed_type} 문서에 저장.
    feed_type: realtime | popular | main
    """
    if feed_type not in ("realtime", "popular", "main"):
        raise ValueError(f"Invalid feed_type: {feed_type}")
    try:
        db = _get_db()
        db.collection("news").document(feed_type).set(
            {
                "articles": articles,
                "updated_at": SERVER_TIMESTAMP,
            },
            merge=False,
        )
    except Exception as e:
        logger.exception("save_news_feed 실패 [%s]: %s", feed_type, e)


def save_overseas_stocks_articles(articles: List[dict[str, Any]]) -> None:
    """news/overseas_stocks 문서에 articles 배열 + updated_at (CNBC·Guardian 등 RSS)."""
    try:
        db = _get_db()
        db.collection("news").document("overseas_stocks").set(
            {
                "articles": articles,
                "updated_at": SERVER_TIMESTAMP,
            },
            merge=False,
        )
    except Exception as e:
        logger.exception("save_overseas_stocks_articles 실패: %s", e)


def save_overseas_tech_articles(articles: List[dict[str, Any]]) -> None:
    """news/overseas_tech 문서에 articles 배열 + updated_at (CNBC·Verge·Ars 등 RSS)."""
    try:
        db = _get_db()
        db.collection("news").document("overseas_tech").set(
            {
                "articles": articles,
                "updated_at": SERVER_TIMESTAMP,
            },
            merge=False,
        )
    except Exception as e:
        logger.exception("save_overseas_tech_articles 실패: %s", e)


_SETTINGS = "settings"
_AI_CONFIG_DOC = "ai_config"


_AI_MODEL_CLAUDE = "claude"
_AI_MODEL_GEMINI = "gemini"


def _normalize_ai_selected_model(raw: object | None) -> str | None:
    if not isinstance(raw, str):
        return None
    s = raw.strip().lower()
    if s in (_AI_MODEL_CLAUDE, _AI_MODEL_GEMINI):
        return s
    return None


def get_ai_config() -> dict[str, str]:
    """
    settings/ai_config — 파이프라인 AI 분기용.
    [selected_model]: \"claude\" | \"gemini\". 없으면 gemini 기본.
    레거시 [claude_enabled]/[gemini_enabled]만 있으면 이전 의미로 유도.
    """
    defaults: dict[str, str] = {"selected_model": _AI_MODEL_GEMINI}
    try:
        db = _get_db()
        snap = db.collection(_SETTINGS).document(_AI_CONFIG_DOC).get()
        if not snap.exists:
            return dict(defaults)
        d = snap.to_dict() or {}
        sm = _normalize_ai_selected_model(d.get("selected_model"))
        if sm:
            return {"selected_model": sm}
        # 레거시 bool 필드
        if "claude_enabled" in d or "gemini_enabled" in d:
            c = bool(d.get("claude_enabled", True))
            g = bool(d.get("gemini_enabled", True))
            if c and not g:
                return {"selected_model": _AI_MODEL_CLAUDE}
            return {"selected_model": _AI_MODEL_GEMINI}
        return dict(defaults)
    except Exception as e:
        logger.exception("get_ai_config 실패, 기본값 사용: %s", e)
        return dict(defaults)


_PUSH_SCHEDULE = "push_schedule"


def write_push_schedule_entry(
    *,
    doc_id: str,
    section: str,
    scheduled_at_utc_iso: str,
    title: str,
    body: str,
    topic: str,
) -> None:
    """
    FCM 푸시 예약 문서 (Cloud Functions 스케줄러가 발송).
    scheduled_at: UTC ISO 문자열 (Z), Cloud Function에서 new Date() 비교용.
    """
    try:
        db = _get_db()
        db.collection(_PUSH_SCHEDULE).document(doc_id).set(
            {
                "section": section,
                "scheduled_at": scheduled_at_utc_iso,
                "title": title,
                "body": body,
                "topic": topic,
                "status": "pending",
                "created_at": SERVER_TIMESTAMP,
            },
            merge=False,
        )
        logger.info("push_schedule 저장: %s (%s)", doc_id, section)
    except Exception as e:
        logger.exception("push_schedule 저장 실패 doc_id=%s: %s", doc_id, e)


def set_ai_config_selected_model(model: str) -> None:
    """Claude 실패 후 Gemini 폴백 등에서 원격 selected_model 갱신."""
    m = _normalize_ai_selected_model(model) or _AI_MODEL_GEMINI
    try:
        db = _get_db()
        db.collection(_SETTINGS).document(_AI_CONFIG_DOC).set(
            {
                "selected_model": m,
                "updatedAt": SERVER_TIMESTAMP,
            },
            merge=True,
        )
    except Exception as e:
        logger.exception("set_ai_config_selected_model(%s) 실패: %s", m, e)
