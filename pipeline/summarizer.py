"""
Claude API 기사 요약·시황 생성.
"""
from __future__ import annotations

import json
import logging
import re
import time
from typing import Any

import anthropic

logger = logging.getLogger(__name__)

MODEL = "claude-sonnet-4-20250514"
SYSTEM = (
    "당신은 한국 투자자를 위한 금융 뉴스 에디터입니다.\n"
    "뉴스를 분석해서 핵심 내용을 명확하고 구체적으로 요약하세요.\n"
    "반드시 JSON 형식으로만 응답하세요."
)


_HANGUL_RE = re.compile(r"[가-힣]")
_LATIN_RE = re.compile(r"[A-Za-z]")


def _contains_hangul(text: str) -> bool:
    return bool(_HANGUL_RE.search(text or ""))


def _looks_english_headline(text: str) -> bool:
    s = (text or "").strip()
    if not s:
        return False
    return bool(_LATIN_RE.search(s)) and not _contains_hangul(s)


def _extract_json(text: str) -> dict[str, Any]:
    text = text.strip()
    m = re.search(r"```(?:json)?\s*([\s\S]*?)\s*```", text)
    if m:
        text = m.group(1).strip()
    return json.loads(text)


def _call_claude(client: anthropic.Anthropic, user_content: str) -> str:
    msg = client.messages.create(
        model=MODEL,
        max_tokens=2048,
        system=SYSTEM,
        messages=[{"role": "user", "content": user_content}],
    )
    parts = []
    for block in msg.content:
        if block.type == "text":
            parts.append(block.text)
    return "".join(parts).strip()


def summarize_article(
    client: anthropic.Anthropic,
    title: str,
    summary: str,
    source: str,
    body: str = "",
) -> dict[str, Any]:
    """
    반환:
    headline, category, summary_points
    """
    body_stripped = (body or "").strip()
    summary_stripped = (summary or "").strip()
    if body_stripped:
        article_block = (
            f"제목: {title}\n"
            f"기사 본문 (요약의 주된 근거):\n{body_stripped}\n"
        )
        if summary_stripped:
            article_block += f"목록/리드용 짧은 설명 (보조 참고): {summary_stripped}\n"
    else:
        article_block = f"제목: {title}\n목록/리드 요약문:\n{summary_stripped}\n"

    user = (
        f"다음 뉴스를 분석해 JSON만 출력하세요.\n"
        f'키: "headline", "category", "summary_points"\n'
        f"headline은 반드시 한국어로 작성하세요. 원문이 영어면 자연스러운 한국어 제목으로 번역하세요.\n"
        f'category는 반드시 다음 중 하나: '
        f"경제, IT, 정치, 사회, 국제, 부동산, 산업, 금융, 매크로, 빅테크\n"
        f"summary_points는 기사 핵심을 빠짐없이 담은 문자열 배열로 작성하세요.\n"
        f"중요한 사실·수치·배경·영향을 누락하지 말고, 내용을 함축하거나 생략하지 마세요.\n"
        f"구체성 규칙(headline·summary_points 모두 적용):\n"
        f"- 기업명, 종목명, 브랜드명, 인물명은 원문에 나온 표기를 그대로 포함할 것.\n"
        f"- 주가, 등락률·퍼센트, 금액 등 구체적 수치가 원문에 있으면 반드시 요약에 포함할 것.\n"
        f"- 「특정 종목」「해당 기업」「관련 주」처럼 추상적으로 바꾸어 고유명·수치를 대체하거나 감추지 말 것.\n"
        f"- 핵심 정보(누가·무엇을·얼마나)를 빠뜨리지 말고 구체적으로 서술할 것.\n"
        f"어려운 경제/금융/전문 용어는 뜻을 유지하되 쉬운 일상 언어로 풀어쓰세요.\n"
        f"문장은 짧고 자연스럽게 쓰고, 뉴스식 딱딱한 문체는 피하세요.\n"
        f"분량은 고정하지 말고 내용량에 맞춰 유동적으로 작성하세요.\n"
        f"(중요 내용이 많으면 길게, 단순한 내용이면 짧게)\n\n"
        f"{article_block}"
        f"출처: {source}\n"
    )
    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_claude(client, user)
            data = _extract_json(raw)
            if "headline" not in data or "category" not in data or "summary_points" not in data:
                raise ValueError(f"Invalid JSON keys: {data.keys()}")
            data["headline"] = str(data.get("headline", "")).strip()
            if _looks_english_headline(data["headline"]):
                data["headline"] = translate_headline_to_korean(
                    client=client,
                    headline=data["headline"],
                    source=source,
                    summary=summary,
                    body=body_stripped,
                )
            pts = data["summary_points"]
            if not isinstance(pts, list):
                raise ValueError("summary_points must be a list")
            data["summary_points"] = [str(p).strip() for p in pts if str(p).strip()][:10]
            return data
        except Exception as e:
            last_err = e
            logger.warning("summarize_article 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(1.0)
    raise last_err or RuntimeError("summarize_article failed")


def translate_headline_to_korean(
    client: anthropic.Anthropic,
    headline: str,
    source: str,
    summary: str = "",
    body: str = "",
) -> str:
    """영문 헤드라인을 한국어 투자 뉴스 제목으로 번역."""
    src = (headline or "").strip()
    if not src:
        return src
    ctx = (body or "").strip() or (summary or "")
    user = (
        "다음 뉴스 헤드라인을 한국어로 번역하세요.\n"
        "의미를 보존하되 과장 없이 간결한 뉴스 제목 톤으로 작성하세요.\n"
        "브랜드명, 기업명, 인물명 등 고유명사는 원문 그대로 유지하세요.\n"
        '반드시 JSON 한 개만 출력: {"headline_ko":"..."}\n\n'
        f"source: {source}\n"
        f"headline_en: {src}\n"
        f"context: {ctx[:400]}\n"
    )
    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_claude(client, user)
            data = _extract_json(raw)
            ko = str(data.get("headline_ko", "")).strip()
            if not ko:
                raise ValueError("missing headline_ko")
            return ko
        except Exception as e:
            last_err = e
            logger.warning("translate_headline_to_korean 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(0.7)
    logger.warning("translate_headline_to_korean 실패, 원문 유지: %s", last_err)
    return src


def generate_market_report(
    client: anthropic.Anthropic,
    indicators: list[dict[str, Any]],
    articles: list[dict[str, Any]],
) -> dict[str, Any]:
    """3줄 시황 요약. 반환: {\"report\": \"...\"}"""
    ind_lines = "\n".join(
        f"- {i.get('name','')}: {i.get('value','')} ({i.get('change','')})" for i in indicators[:12]
    )
    headlines = "\n".join(f"- {(a.get('headline') or a.get('title',''))[:120]}" for a in articles[:15])
    user = (
        "다음 지표와 해외 뉴스 헤드라인을 바탕으로 미국·글로벌 시황을 한국어로 정확히 3문장으로 요약하세요.\n"
        '반드시 JSON 한 개만: {"report": "..."}\n\n'
        f"[지표]\n{ind_lines or '(없음)'}\n\n[뉴스]\n{headlines or '(없음)'}\n"
    )
    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_claude(client, user)
            data = _extract_json(raw)
            if "report" not in data:
                raise ValueError("missing report")
            return {"report": str(data["report"]).strip()}
        except Exception as e:
            last_err = e
            logger.warning("generate_market_report 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(1.0)
    raise last_err or RuntimeError("generate_market_report failed")


def curate_us_market_articles(
    client: anthropic.Anthropic,
    items: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """
    stocks+tech 합친 리스트에서 Claude로 최대 8개 엄선 (중복·광고성 제외).
    8개 미만이면 전부 반환; 8개 이하면 API 생략하고 전부 반환.
    """
    if not items:
        return []
    n = len(items)
    if n <= 8:
        return [dict(x) for x in items]

    lines: list[str] = []
    for i, it in enumerate(items):
        t = str(it.get("title", ""))[:220]
        s = str(it.get("summary", ""))[:400]
        src = str(it.get("source", ""))[:80]
        u = str(it.get("url", ""))[:300]
        lines.append(f'[{i}] title: {t}\n  source: {src}\n  url: {u}\n  lead: {s}')
    body = "\n\n".join(lines)
    user = (
        "당신은 글로벌 금융 뉴스 큐레이터입니다.\n"
        "투자자 관점에서 가장 중요하고, 서로 **다른 주제**를 넓게 커버하도록 **정확히 8개** 기사를 고르세요. "
        "제목·내용이 **중복/유사**한 기사는 **하나만** 남기세요. **광고성·낚시성(클릭베이트)** 느낌이 강한 기사는 제외하세요.\n"
        f"아래 [0]~[{n-1}] 인덱스로 식별됩니다. **서로 다른 정수 8개**의 인덱스만 JSON으로 출력하세요.\n"
        '형식(배열만, 길이 8): {{"selected_indices": [0,1,2,3,4,5,6,7]}}\n\n'
        f"{body}"
    )
    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_claude(client, user)
            data = _extract_json(raw)
            idxs = data.get("selected_indices", [])
            if not isinstance(idxs, list) or not idxs:
                raise ValueError("missing or invalid selected_indices")
            picked: list[dict[str, Any]] = []
            seen: set[int] = set()
            for j in idxs:
                if not isinstance(j, int) or j < 0 or j >= n or j in seen:
                    continue
                seen.add(j)
                picked.append(dict(items[j]))
            if not picked:
                return [dict(items[i]) for i in range(min(8, n))]
            for i in range(n):
                if len(picked) >= 8:
                    break
                if i not in seen:
                    seen.add(i)
                    picked.append(dict(items[i]))
            return picked[:8]
        except Exception as e:
            last_err = e
            logger.warning("curate_us_market_articles 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(1.0)
    logger.warning("curate_us_market_articles 실패, 상위 8개 사용: %s", last_err)
    return [dict(items[i]) for i in range(min(8, n))]


def summarize_batch(
    client: anthropic.Anthropic,
    items: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """기사 목록 순차 요약. 항목 사이 0.5초."""
    out: list[dict[str, Any]] = []
    for it in items:
        try:
            ai = summarize_article(
                client,
                title=it.get("title", ""),
                summary=it.get("summary", ""),
                source=it.get("source", ""),
                body=str(it.get("body") or ""),
            )
            out.append({**it, "_ai": ai})
        except Exception as e:
            logger.error("배치 항목 스킵: %s — %s", it.get("title", "")[:40], e)
        time.sleep(0.5)
    return out


def analyze_company(
    client: anthropic.Anthropic,
    ticker: str,
    name: str,
    financials: dict[str, Any],
    health: dict[str, Any],
    analyst: dict[str, Any],
    profile: dict[str, Any] | None = None,
) -> dict[str, Any]:
    """
    Claude 기반 기업 서술·건전성 라벨·점수·AI 증권 의견.
    """
    profile = profile or {}
    payload = {
        "ticker": ticker,
        "name": name,
        "profile": profile,
        "financials": financials,
        "health": health,
        "analyst": analyst,
    }
    user = (
        "다음 기업 데이터를 바탕으로 한국 투자자용 분석 JSON만 출력하세요.\n"
        '필수 키: "business", "revenue_model", "outlook_2026", "risk_factors", "capital", '
        '"health_status", "health_summary", "health_score", "ai_analyst_summary"\n'
        "health_status는 객체이며 반드시 다음 키 포함:\n"
        "debt_ratio_status, current_ratio_status, equity_ratio_status, "
        "cash_status, interest_status\n"
        "일반 지표 값은 우량/안정/주의/위험 중 하나.\n"
        "cash_status만 풍부/안정/주의/부족 중 하나.\n"
        "health_score: 0~5 정수.\n\n"
        + json.dumps(payload, ensure_ascii=False)[:45000]
    )
    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_claude(client, user)
            data = _extract_json(raw)
            required = (
                "business",
                "revenue_model",
                "outlook_2026",
                "risk_factors",
                "capital",
                "health_status",
                "health_summary",
                "health_score",
                "ai_analyst_summary",
            )
            for k in required:
                if k not in data:
                    raise ValueError(f"missing key: {k}")
            hs = data["health_status"]
            if not isinstance(hs, dict):
                raise ValueError("health_status must be object")
            for hk in (
                "debt_ratio_status",
                "current_ratio_status",
                "equity_ratio_status",
                "cash_status",
                "interest_status",
            ):
                hs.setdefault(hk, "안정")
            try:
                sc = int(data["health_score"])
            except (TypeError, ValueError):
                sc = 3
            data["health_score"] = max(0, min(5, sc))
            return data
        except Exception as e:
            last_err = e
            logger.warning("analyze_company 재시도 (%s): %s", attempt + 1, e)
            if attempt == 0:
                time.sleep(1.0)
    raise last_err or RuntimeError("analyze_company failed")


def merge_to_firestore_article(raw: dict[str, Any], ai: dict[str, Any]) -> dict[str, Any]:
    """앱 Article 스키마에 맞춘 dict."""
    ko_headline = str(ai.get("headline", "")).strip() or str(raw.get("title", "")).strip()
    return {
        "source": raw.get("source", ""),
        "headline": ko_headline,
        "headline_ko": ko_headline,
        "headline_en": raw.get("title", ""),
        "imageUrl": raw.get("thumbnail_url", ""),
        "summary": (raw.get("summary") or "")[:500],
        "time": raw.get("published_at") or "",
        "category": ai.get("category", ""),
        "summaryPoints": ai.get("summary_points", []),
        "tag": ai.get("category", ""),
        "sourceColor": None,
    }
