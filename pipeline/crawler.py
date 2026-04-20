"""
네이버 금융 / Yahoo Finance(HTML) 뉴스 크롤러 + 종목 재무·증권가 크롤링.
(토스증권 Playwright `crawl_tossinvest_news` 는 보존. `crawl_domestic` 에서는 미사용.)
"""
from __future__ import annotations

import difflib
import json
import logging
from collections import Counter
import os
import re
import sys
import time
from datetime import datetime, timedelta, timezone
from typing import Any
from urllib.parse import parse_qs, urljoin, urlparse

import pytz
import requests
from bs4 import BeautifulSoup

logger = logging.getLogger(__name__)

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
}

NAVER_BASE = "https://finance.naver.com"
TOSS_INVEST_BASE = "https://www.tossinvest.com"
TOSS_INVEST_NEWS_URL = "https://www.tossinvest.com/news"
REQUEST_TIMEOUT = 20
MAX_PER_SECTION = 10
MAX_TOSS_NEWS = 20
SUMMARY_CHARS = 200
ARTICLE_BODY_MAX_CHARS = 2000
# 브리핑(한경·매경 신문) 저장 후보: 본문이 이 길이 미만이면 제외 (summarizer briefing_newspaper 기준과 동일)
BRIEFING_NEWSPAPER_MIN_BODY_CHARS = 200
NAVER_ARTICLE_BODY_SELECTORS = (
    "#dic_area",
    "div.article_body",
    "#articeBody",
    "#articleBody",
)
STOCK_NAVER_BASE = "https://stock.naver.com"
NAVER_STOCK_THEME_MAX = 20
NAVER_STOCK_THEME_LIST_API = (
    "https://stock.naver.com/api/domestic/market/theme/list"
    f"?startIdx=0&pageSize={NAVER_STOCK_THEME_MAX}&sortType=changeRate"
)
NAVER_STOCK_THEME_STOCKS_MAX = 6
NAVER_STOCK_THEME_FETCH_SLEEP = 0.3

NAVER_STOCK_UPJONG_MAX = 20
NAVER_STOCK_UPJONG_LIST_API = (
    "https://stock.naver.com/api/domestic/market/upjong/list"
    f"?startIdx=0&pageSize={NAVER_STOCK_UPJONG_MAX}&sortType=changeRate"
)
NAVER_STOCK_UPJONG_STOCKS_MAX = 6
NAVER_STOCK_UPJONG_FETCH_SLEEP = 0.3
_NAVER_ARTICLE_THUMB_CACHE: dict[str, str] = {}


_NAVER_BODY_NOISE_SELECTORS = (
    ".ad_area",
    ".promotion",
    ".related_news",
    ".article_related",
    ".vod_area",
    "#spiBundleArea",
    ".recommend",
    ".link_news",
    ".article_poll_area",
    ".media_end_smart_ann",
    ".end_photo_org",
    "._LAZY_LOADING_WRAP",
    ".poll_group",
    ".article_info_area",
    ".journalist_profile",
)


def _element_to_plain_article_text(root: Any) -> str:
    """본문 루트 노드에서 스크립트·광고·관련기사 등 제거 후 한 줄 위주 텍스트."""
    if root is None:
        return ""
    try:
        frag = BeautifulSoup(str(root), "lxml")
        top = frag.find()
        if not top:
            return ""
        for rem in top.find_all(["script", "style", "noscript", "iframe"]):
            rem.decompose()
        for sel in _NAVER_BODY_NOISE_SELECTORS:
            for rem in top.select(sel):
                rem.decompose()
        text = top.get_text(separator=" ", strip=True)
        text = re.sub(r"\s+", " ", text).strip()
        return text[:ARTICLE_BODY_MAX_CHARS]
    except Exception:
        return ""


def _naver_finance_news_read_to_n_news_article_url(url: str) -> str:
    """
    finance.naver.com/news/news_read.naver?... 쿼리의 office_id·article_id로
    n.news.naver.com/article/{office_id}/{article_id} 본문 페이지 URL을 만든다.
    해당 형식이 아니면 원본 URL을 그대로 반환한다.
    """
    raw = (url or "").strip()
    if not raw:
        return raw
    try:
        parsed = urlparse(raw)
    except Exception:
        return raw
    netloc = (parsed.netloc or "").lower()
    path = (parsed.path or "").lower()
    if "finance.naver.com" not in netloc:
        return raw
    if "news_read.naver" not in path and "news_read.nhn" not in path:
        return raw
    q = parse_qs(parsed.query, keep_blank_values=False)

    def _first(*keys: str) -> str:
        for k in keys:
            vals = q.get(k)
            if vals and str(vals[0]).strip():
                return str(vals[0]).strip()
        return ""

    article_id = _first(
        "article_id",
        "articleId",
        "ARTICLE_ID",
    )
    office_id = _first(
        "office_id",
        "officeId",
        "OFFICE_ID",
        "office_no",
        "officeNo",
    )
    if article_id and office_id:
        return f"https://n.news.naver.com/article/{office_id}/{article_id}"
    return raw


def _fetch_naver_article_body(article: dict[str, Any]) -> str:
    """
    네이버(금융/뉴스) 기사 본문 텍스트 추출. 실패 시 빈 문자열.
    기사 dict의 ``url``을 사용한다. finance ``news_read.naver`` 링크는
    쿼리의 office_id·article_id로 n.news 기사 URL로 바꾼 뒤 요청한다.
    """
    article_url = str(article.get("url") or "").strip()
    url = _naver_finance_news_read_to_n_news_article_url(article_url)
    if not url:
        return ""
    html = _fetch(url)
    if not html:
        logger.warning(
            "naver 기사 본문 비어 있음(HTML 미수신): article_url=%s fetch_url=%s 시도 셀렉터=%s",
            article_url,
            url,
            ", ".join(NAVER_ARTICLE_BODY_SELECTORS),
        )
        return ""
    try:
        soup = BeautifulSoup(html, "lxml")
        for sel in NAVER_ARTICLE_BODY_SELECTORS:
            el = soup.select_one(sel)
            if el:
                t = _element_to_plain_article_text(el)
                if t:
                    return t[:ARTICLE_BODY_MAX_CHARS]
        logger.warning(
            "naver 기사 본문 비어 있음(본문 노드·텍스트 없음): article_url=%s fetch_url=%s 시도 셀렉터=%s",
            article_url,
            url,
            ", ".join(NAVER_ARTICLE_BODY_SELECTORS),
        )
        return ""
    except Exception as e:
        logger.warning(
            "naver 기사 본문 비어 있음(파싱 예외): article_url=%s fetch_url=%s 시도 셀렉터=%s err=%s",
            article_url,
            url,
            ", ".join(NAVER_ARTICLE_BODY_SELECTORS),
            e,
        )
        return ""


def _fetch_yahoo_article_body(url: str) -> str:
    """Yahoo Finance 기사 페이지 본문 추출. 실패 시 빈 문자열."""
    u = (url or "").strip()
    if not u:
        return ""
    html = _fetch(u)
    if not html:
        return ""
    try:
        soup = BeautifulSoup(html, "lxml")
        for sel in (
            "div.caas-body",
            "div.caas-content",
            "[data-testid='article-body']",
            "article",
        ):
            el = soup.select_one(sel)
            if el:
                t = _element_to_plain_article_text(el)
                if len(t) >= 80:
                    return t[:ARTICLE_BODY_MAX_CHARS]
        return ""
    except Exception as e:
        logger.debug("yahoo article body 실패 %s: %s", u[:80], e)
        return ""


def _attach_naver_article_bodies(rows: list[dict[str, Any]], pause_sec: float = 0.06) -> None:
    """각 row에 `body` 키 채움. 본문 요청 URL은 row['url']만 사용."""
    for row in rows:
        u = str(row.get("url") or "").strip()
        row["body"] = _fetch_naver_article_body(row) if u else ""
        if pause_sec > 0:
            time.sleep(pause_sec)


def _filter_briefing_newspaper_rows_by_body(
    rows: list[dict[str, Any]],
    *,
    press_code: str = "",
) -> list[dict[str, Any]]:
    """
    브리핑 신문(한경·매경) 전용: 저장·요약 파이프라인에 넘기기 전 본문 검사.
    - body가 None이거나 공백만인 항목 제거
    - strip 이후 길이가 BRIEFING_NEWSPAPER_MIN_BODY_CHARS 미만 제거
    """
    kept: list[dict[str, Any]] = []
    for row in rows:
        raw = row.get("body")
        if raw is None:
            continue
        text = str(raw).strip()
        if not text or len(text) < BRIEFING_NEWSPAPER_MIN_BODY_CHARS:
            continue
        kept.append(row)
    dropped = len(rows) - len(kept)
    if dropped:
        logger.info(
            "브리핑 신문 본문 필터: 제거 %d건(본문 없음 또는 %d자 미만), 남음 %d건, press=%s",
            dropped,
            BRIEFING_NEWSPAPER_MIN_BODY_CHARS,
            len(kept),
            press_code or "?",
        )
    return kept


def _attach_yahoo_article_bodies(rows: list[dict[str, Any]], pause_sec: float = 0.06) -> None:
    """각 row에 `body` 키 채움 (Yahoo Finance 기사 URL)."""
    for row in rows:
        u = (row.get("url") or "").strip()
        row["body"] = _fetch_yahoo_article_body(u) if u else ""
        if pause_sec > 0:
            time.sleep(pause_sec)


def _normalize_url(url: str) -> str:
    if not url:
        return ""
    parsed = urlparse(url.strip())
    # 쿼리 중 불필요 파라미터 제거는 생략, scheme+netloc+path 기준
    return f"{parsed.scheme}://{parsed.netloc}{parsed.path}".rstrip("/")


def _fetch(url: str) -> str | None:
    try:
        r = requests.get(url, headers=HEADERS, timeout=REQUEST_TIMEOUT)
        r.raise_for_status()
        r.encoding = r.apparent_encoding or "utf-8"
        return r.text
    except Exception as e:
        logger.warning("요청 실패 %s: %s", url, e)
        return None


def _fetch_http_debug(url: str, *, log_label: str = "HTTP") -> tuple[str | None, int | None, str | None]:
    """
    Yahoo 디버그용: (본문, status_code, 에러문자열).
    workflow_dispatch 로그용으로 status·응답 크기를 항상 남김.
    """
    try:
        r = requests.get(url, headers=HEADERS, timeout=REQUEST_TIMEOUT)
        status = int(r.status_code)
        nbytes = len(r.content or b"")
        enc = r.encoding or r.apparent_encoding or "utf-8"
        logger.info(
            "%s: status=%s url=%s response_bytes=%s encoding=%s",
            log_label,
            status,
            url,
            nbytes,
            enc,
        )
        if status != 200:
            snippet = (r.text or "")[:800].replace("\n", " ")
            logger.warning(
                "%s: 비-200 응답 body_prefix=%r",
                log_label,
                snippet,
            )
        r.raise_for_status()
        r.encoding = r.apparent_encoding or "utf-8"
        return r.text, status, None
    except requests.HTTPError as e:
        resp = getattr(e, "response", None)
        status = int(resp.status_code) if resp is not None and resp.status_code is not None else None
        snippet = ((resp.text or "")[:800] if resp is not None else "").replace("\n", " ")
        logger.warning(
            "%s: HTTPError status=%s url=%s err=%s body_prefix=%r",
            log_label,
            status,
            url,
            e,
            snippet,
        )
        return None, status, str(e)
    except requests.RequestException as e:
        logger.warning("%s: RequestException url=%s: %s", log_label, url, e)
        return None, None, str(e)


def _fetch_json(url: str, headers: dict[str, str] | None = None) -> dict[str, Any] | None:
    req_headers = dict(HEADERS)
    if headers:
        req_headers.update(headers)
    try:
        r = requests.get(url, headers=req_headers, timeout=REQUEST_TIMEOUT)
        r.raise_for_status()
        return r.json()
    except Exception as e:
        logger.warning("JSON 요청 실패 %s: %s", url, e)
        return None


def _resolve_naver_news_href(href: str) -> str:
    """목록 href를 그대로 쓰되, hash 제거. 상대·// 는 절대 URL로."""
    h = (href or "").strip()
    if not h or h.startswith("#") or "javascript" in h.lower():
        return ""
    if h.startswith("http://") or h.startswith("https://"):
        return h.split("#")[0]
    if h.startswith("//"):
        return ("https:" + h).split("#")[0]
    # / 로 시작: 경로만 보고 n.news vs finance 구분 (쿼리의 article_id 등에 'article'이 들어가면 오탐하지 않음)
    if h.startswith("/"):
        path_only = h.split("?", 1)[0]
        pl = path_only.lower()
        if pl.startswith(("/mnp", "/include", "/article/option", "/m/article/")):
            return urljoin("https://n.news.naver.com", h).split("#")[0]
        if pl.startswith("/article/"):
            return urljoin("https://n.news.naver.com", h).split("#")[0]
        return urljoin(NAVER_BASE, h).split("#")[0]
    return urljoin(NAVER_BASE, h).split("#")[0]


def _absolutize_naver_list_image(src: str) -> str:
    t = (src or "").strip()
    if not t:
        return ""
    if t.startswith("//"):
        return "https:" + t
    if t.startswith("/"):
        return urljoin("https://mimgnews.pstatic.net", t)
    return t


def _absolutize_with_base(base_url: str, src: str) -> str:
    t = (src or "").strip()
    if not t or t.startswith("data:"):
        return ""
    if t.startswith("//"):
        return "https:" + t
    if t.startswith("http://") or t.startswith("https://"):
        return t.split("#")[0]
    return urljoin(base_url, t).split("#")[0]


def _extract_naver_article_first_image(article_url: str) -> str:
    """기사 본문에서 대표 이미지 후보(#img1, .nbd_im_w img) 추출."""
    u = (article_url or "").strip()
    if not u:
        return ""
    if u in _NAVER_ARTICLE_THUMB_CACHE:
        return _NAVER_ARTICLE_THUMB_CACHE[u]

    html = _fetch(u)
    if not html:
        _NAVER_ARTICLE_THUMB_CACHE[u] = ""
        return ""

    try:
        soup = BeautifulSoup(html, "lxml")
        cand = soup.select_one("#img1") or soup.select_one(".nbd_im_w img")
        src = (cand.get("src") or "").strip() if cand else ""
        img = _absolutize_with_base(u, src)
        _NAVER_ARTICLE_THUMB_CACHE[u] = img
        return img
    except Exception:
        _NAVER_ARTICLE_THUMB_CACHE[u] = ""
        return ""


def _article_summary_text_excluding_spans(article_summary_dd) -> str:
    """dd.articleSummary에서 span.press, span.bar, span.wdate 제거 후 텍스트."""
    if not article_summary_dd:
        return ""
    frag = BeautifulSoup(str(article_summary_dd), "lxml")
    root = frag.find("dd") or frag
    for sp in root.select("span.press, span.bar, span.wdate"):
        sp.decompose()
    raw = root.get_text(separator=" ", strip=True)
    return re.sub(r"\s+", " ", raw).strip()


def _normalize_press_name(raw: str) -> str:
    s = re.sub(r"\s+", " ", (raw or "").strip())
    if not s:
        return ""
    s = re.sub(r"^(입력|제공)\s*", "", s)
    s = re.sub(r"\s*기자$", "", s)
    return s.strip(" ·|")


def _extract_naver_source_and_published(asum, container=None) -> tuple[str, str]:
    """네이버 금융 목록 HTML에서 언론사/시간을 최대한 보수적으로 추출."""
    source = ""
    published = ""

    if asum:
        pr = asum.select_one("span.press")
        if pr:
            source = _normalize_press_name(pr.get_text(strip=True))
        wd = asum.select_one("span.wdate")
        if wd:
            published = (wd.get_text(strip=True) or "").strip()

    root = container or asum
    if root:
        # 제목 옆/메타에 span.press가 아닌 클래스명으로 들어오는 경우 대응
        if not source:
            for node in root.select("span[class], em[class], a[class], strong[class]"):
                classes = " ".join(node.get("class") or []).lower()
                if not any(k in classes for k in ("press", "media", "office", "source")):
                    continue
                txt = _normalize_press_name(node.get_text(" ", strip=True))
                if txt and len(txt) <= 24 and not re.search(r"\d{2,4}[./-]\d{1,2}", txt):
                    source = txt
                    break
        if not published:
            wnode = root.select_one("span.wdate, span.date, time")
            if wnode:
                published = (wnode.get_text(strip=True) or "").strip()

    return source or "네이버금융", published


def _parse_naver_pc_article_block(dd_subj) -> dict[str, Any] | None:
    """
    PC finance 뉴스 목록:
    - 링크: dd.articleSubject > a
    - 제목: a[title] 또는 a 텍스트
    - 요약: dd.articleSummary (press/bar/wdate span 제외)
    - 언론: span.press, 날짜: span.wdate, 썸네일: dt.thumb > a > img
    """
    a = dd_subj.select_one("a[href]")
    if not a:
        return None
    url = _resolve_naver_news_href((a.get("href") or "").strip())
    if not url:
        return None
    title = ((a.get("title") or "").strip() or a.get_text(strip=True) or "").strip()
    if len(title) < 2:
        return None

    dl = dd_subj.find_parent("dl")
    asum = None
    if dl:
        asum = dl.select_one("dd.articleSummary")
    if not asum:
        sib: Any = dd_subj
        for _ in range(20):
            sib = sib.find_next_sibling()
            if sib is None:
                break
            if not getattr(sib, "name", None):
                continue
            cls = sib.get("class") or []
            if sib.name == "dd" and "articleSummary" in cls:
                asum = sib
                break

    source, published = _extract_naver_source_and_published(asum=asum, container=dl or dd_subj)

    raw = _article_summary_text_excluding_spans(asum)
    summary = (raw[:SUMMARY_CHARS] + ("…" if len(raw) > SUMMARY_CHARS else "")).strip() or title[:SUMMARY_CHARS]

    thumb = ""
    if dl:
        img = dl.select_one("dt.thumb > a > img") or dl.select_one("dt.thumb a img")
        if img and img.get("src"):
            thumb = _absolutize_naver_list_image(img.get("src", ""))
    if not thumb:
        thumb = _extract_naver_article_first_image(url)

    return {
        "title": title,
        "url": url,
        "source": source,
        "published_at": published,
        "summary": summary,
        "category": "",
        "thumbnail_url": thumb,
    }


def _collect_naver_links_from_list(html: str, max_n: int) -> list[dict[str, Any]]:
    """dd.articleSubject 블록 단위로 PC 목록 HTML 파싱 (기사 URL 별도 요청 없음)."""
    soup = BeautifulSoup(html, "lxml")
    out: list[dict[str, Any]] = []
    seen: set[str] = set()
    debug_lines: list[str] = []
    debug = os.environ.get("SAPIENS_NAVER_CRAWL_DEBUG", "").lower() in ("1", "true", "yes")

    for dd in soup.select("dd.articleSubject"):
        row = _parse_naver_pc_article_block(dd)
        if not row:
            continue
        u = row["url"]
        if u in seen:
            continue
        seen.add(u)
        if debug:
            debug_lines.append(f"{u} | {row['title']!r} | {row.get('source')!r}")
        out.append(row)
        if len(out) >= max_n:
            break
    if debug and debug_lines:
        logger.info(
            "NAVER CRAWL DEBUG: articleSubject %d (max %d):\n%s",
            len(debug_lines),
            max_n,
            "\n".join(debug_lines),
        )
    return out


def _parse_naver_ranked_simple_news_li(li) -> dict[str, Any] | None:
    """
    많이 본 뉴스(RANK) 페이지: div.hotNewsList ul.simpleNewsList li
    - 링크/제목: a[href], title 속성(제목), a 텍스트(요약용)
    - span.press, span.wdate; 썸네일 없음
    """
    a = li.select_one("a[href]")
    if not a:
        return None
    url = _resolve_naver_news_href((a.get("href") or "").strip())
    if not url:
        return None
    title = ((a.get("title") or "").strip() or a.get_text(strip=True) or "").strip()
    if len(title) < 2:
        return None
    source, published = _extract_naver_source_and_published(asum=None, container=li)
    raw = a.get_text(separator=" ", strip=True)
    summary = (raw[:SUMMARY_CHARS] + ("…" if len(raw) > SUMMARY_CHARS else "")).strip() or title[:SUMMARY_CHARS]
    thumb = _extract_naver_article_first_image(url)
    return {
        "title": title,
        "url": url,
        "source": source,
        "published_at": published,
        "summary": summary,
        "category": "",
        "thumbnail_url": thumb,
    }


def _collect_naver_ranked_from_list(html: str, max_n: int) -> list[dict[str, Any]]:
    """많이 본 뉴스 전용 목록 (simpleNewsList)."""
    soup = BeautifulSoup(html, "lxml")
    out: list[dict[str, Any]] = []
    seen: set[str] = set()
    debug_lines: list[str] = []
    debug = os.environ.get("SAPIENS_NAVER_CRAWL_DEBUG", "").lower() in ("1", "true", "yes")

    for li in soup.select("div.hotNewsList ul.simpleNewsList li"):
        row = _parse_naver_ranked_simple_news_li(li)
        if not row:
            continue
        u = row["url"]
        if u in seen:
            continue
        seen.add(u)
        if debug:
            debug_lines.append(f"{u} | {row['title']!r} | {row.get('source')!r}")
        out.append(row)
        if len(out) >= max_n:
            break
    if debug and debug_lines:
        logger.info(
            "NAVER CRAWL DEBUG (RANK): simpleNewsList li %d (max %d):\n%s",
            len(debug_lines),
            max_n,
            "\n".join(debug_lines),
        )
    return out


def crawl_naver_realtime() -> list[dict[str, Any]]:
    url = "https://finance.naver.com/news/news_list.naver?mode=LSS2D&section_id=101&section_id2=258"
    items: list[dict[str, Any]] = []
    try:
        html = _fetch(url)
        if not html:
            return items
        for row in _collect_naver_links_from_list(html, MAX_PER_SECTION):
            items.append(row)
        _attach_naver_article_bodies(items)
    except Exception as e:
        logger.exception("crawl_naver_realtime 실패: %s", e)
    return items


def crawl_naver_ranked() -> list[dict[str, Any]]:
    url = "https://finance.naver.com/news/news_list.naver?mode=RANK"
    items: list[dict[str, Any]] = []
    try:
        html = _fetch(url)
        if not html:
            return items
        links = _collect_naver_ranked_from_list(html, MAX_PER_SECTION * 2)
        for rank, row in enumerate(links[:MAX_PER_SECTION], start=1):
            d = dict(row)
            d["title"] = f"[{rank}] {d['title']}"
            items.append(d)
        _attach_naver_article_bodies(items)
    except Exception as e:
        logger.exception("crawl_naver_ranked 실패: %s", e)
    return items


def crawl_naver_main() -> list[dict[str, Any]]:
    url = f"{NAVER_BASE}/news/mainnews.naver"
    items: list[dict[str, Any]] = []
    try:
        html = _fetch(url)
        if not html:
            return items
        for row in _collect_naver_links_from_list(html, MAX_PER_SECTION):
            items.append(row)
        _attach_naver_article_bodies(items)
    except Exception as e:
        logger.exception("crawl_naver_main 실패: %s", e)
    return items


def _debug_naver_mainnews_list_all_links() -> None:
    """
    mainnews.naver PC HTML: dd.articleSubject 파싱 + _collect_naver_links_from_list(max 15) 출력.
    사용:  python pipeline/crawler.py debug-naver-main  (pipeline 폴더 기준)
    """
    u = f"{NAVER_BASE}/news/mainnews.naver"
    print("GET", u, file=sys.stderr, flush=True)
    html = _fetch(u)
    if not html:
        print("empty response", file=sys.stderr, flush=True)
        return
    print("html length:", len(html), file=sys.stderr, flush=True)
    soup = BeautifulSoup(html, "lxml")
    print("--- dd.articleSubject (parsed) ---", flush=True)
    for dd in soup.select("dd.articleSubject"):
        row = _parse_naver_pc_article_block(dd)
        if row:
            s = (row.get("summary") or "")[:80]
            print(
                f"  {row['url']!r} | {row['title']!r} | summary={s!r} | src={row.get('source')!r}",
                flush=True,
            )
    print("--- _collect_naver_links_from_list (max 15) ---", flush=True)
    for row in _collect_naver_links_from_list(html, 15):
        print(f"  {row!r}", flush=True)
    print("--- end ---", flush=True)


def _toss_click_tab(page: Any, label: str) -> None:
    """토스증권 뉴스 텍스트 탭 클릭 (get_by_text → button/tab/a → 전체 노드 순)."""
    errors: list[Exception] = []

    try:
        page.get_by_text(label, exact=True).click(timeout=8000)
        return
    except Exception as e:
        errors.append(e)

    try:
        page.locator("button, [role='tab'], a").filter(has_text=label).first.click(timeout=8000)
        return
    except Exception as e:
        errors.append(e)

    try:
        page.locator("*").filter(has_text=label).first.click(timeout=8000)
        return
    except Exception as e:
        errors.append(e)

    msg = f"탭 클릭 실패: {label!r}"
    if errors:
        raise RuntimeError(msg) from errors[-1]
    raise RuntimeError(msg)


def _extract_toss_items_from_page(page: Any, max_n: int) -> list[dict[str, Any]]:
    """현재 탭에서 a[href*='news'] 링크 기준 수집 (텍스트 10자 이상)."""
    raw = page.evaluate(
        """(maxN) => {
      const seen = new Set();
      const rows = [];
      const anchors = document.querySelectorAll('a[href]');
      for (const a of anchors) {
        if (rows.length >= maxN) break;
        const href = a.getAttribute('href') || '';
        if (!href.includes('/news')) continue;
        const pathOnly = href.split('?')[0].replace(/\\/$/, '');
        if (pathOnly.endsWith('/news')) continue;
        let abs;
        try { abs = new URL(href, location.origin).href; } catch (e) { continue; }
        if (seen.has(abs)) continue;
        let title = (a.innerText || '').trim().replace(/\\s+/g, ' ');
        const lines = title.split(/\\n+/).map(s => s.trim()).filter(Boolean);
        title = (lines[0] || title).trim();
        if (title.length < 10) continue;
        let thumb = '';
        let press = '';
        let ptime = '';
        let el = a.closest('li') || a.closest('article') || a.closest('div');
        for (let depth = 0; depth < 8 && el; depth++) {
          const img = el.querySelector('img[src]');
          if (img && !thumb) thumb = img.getAttribute('src') || '';
          const smalls = el.querySelectorAll('span, p, em, time, div');
          for (const s of smalls) {
            const tx = (s.innerText || '').trim();
            if (!tx || tx.length > 40) continue;
            if (/분 전|시간 전|일 전|주 전|\\d{4}\\.\\d{2}\\.\\d{2}|\\d{1,2}:\\d{2}/.test(tx)) {
              if (!ptime) ptime = tx;
            } else if (tx.length >= 2 && tx.length <= 20 && tx !== title && !title.startsWith(tx)) {
              if (!press) press = tx;
            }
          }
          el = el.parentElement;
        }
        seen.add(abs);
        rows.push({ url: abs, title: title.slice(0, 500), thumb, press, time: ptime });
      }
      return rows;
    }""",
        max_n,
    )
    items: list[dict[str, Any]] = []
    for row in raw or []:
        url = _normalize_url(str(row.get("url") or ""))
        title = (str(row.get("title") or "")).strip()
        if not url or not title:
            continue
        press = (str(row.get("press") or "")).strip()
        time_s = (str(row.get("time") or "")).strip()
        thumb = (str(row.get("thumb") or "")).strip()
        if thumb.startswith("//"):
            thumb = "https:" + thumb
        elif thumb.startswith("/"):
            thumb = urljoin(TOSS_INVEST_BASE, thumb)
        summary_src = title
        summary = (summary_src[:SUMMARY_CHARS] + ("…" if len(summary_src) > SUMMARY_CHARS else "")).strip()
        items.append(
            {
                "title": title,
                "summary": summary or title[:SUMMARY_CHARS],
                "source": press or "토스증권",
                "url": url,
                "published_at": time_s,
                "category": "토스증권",
                "thumbnail_url": thumb,
            }
        )
    return items


def crawl_tossinvest_news() -> dict[str, list[dict[str, Any]]]:
    """
    토스증권 뉴스(https://www.tossinvest.com/news) 탭별 Playwright 크롤.
    반환 키: many_viewed(인기), main_news(주요), realtime(최신) — 각 최대 MAX_TOSS_NEWS건.
    """
    empty: dict[str, list[dict[str, Any]]] = {
        "many_viewed": [],
        "main_news": [],
        "realtime": [],
    }
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        logger.warning("playwright 미설치 — 토스증권 뉴스 크롤 스킵")
        return empty

    tab_jobs: list[tuple[str, tuple[str, ...]]] = [
        ("many_viewed", ("인기뉴스", "많이 본 뉴스", "많이 본")),
        ("main_news", ("주요뉴스", "주요 뉴스")),
        ("realtime", ("최신뉴스", "실시간 속보", "실시간")),
    ]

    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            try:
                context = browser.new_context(
                    user_agent=HEADERS["User-Agent"],
                    locale="ko-KR",
                    viewport={"width": 1400, "height": 2200},
                )
                page = context.new_page()
                page.goto(TOSS_INVEST_NEWS_URL, wait_until="domcontentloaded", timeout=90000)
                page.wait_for_timeout(5000)
                # '지원하지 않는 브라우저' / 크롬 다운로드 안내 등 팝업이 있으면 ESC로 닫기
                try:
                    page.keyboard.press("Escape")
                except Exception:
                    pass
                page.wait_for_timeout(3000)
                # 탭(인기뉴스 등)이 실제로 그려질 때까지 대기
                page.wait_for_selector("text=인기뉴스", timeout=10000)

                for key, aliases in tab_jobs:
                    clicked = False
                    for label in aliases:
                        try:
                            _toss_click_tab(page, label)
                            clicked = True
                            break
                        except Exception:
                            continue
                    if not clicked:
                        logger.warning("토스증권 탭 클릭 실패 (%s), 시도 라벨: %s", key, aliases)
                        continue
                    page.wait_for_timeout(1500)
                    empty[key] = _extract_toss_items_from_page(page, MAX_TOSS_NEWS)
            finally:
                browser.close()
    except Exception as e:
        logger.exception("crawl_tossinvest_news 실패: %s", e)
        return {"many_viewed": [], "main_news": [], "realtime": []}

    return empty


YAHOO_FINANCE = "https://finance.yahoo.com"
YAHOO_MAX_LIST_ITEMS = 10
# 해외(stocks+tech) 합산 시간 필터·목표량용: 목록에서 충분히 긁은 뒤 절대시간 파싱·창 확장(6h→12h→24h→48h)
YAHOO_OVERSEAS_FETCH_CAP = 100
YAHOO_OVERSEAS_MERGE_TARGET = 30
YAHOO_OVERSEAS_TIME_WINDOWS_HOURS: tuple[float, ...] = (6.0, 12.0, 24.0, 48.0)
# js-stream-content 등 넓은 셀렉터가 과다 매칭될 때 상한(성능·로그 가독).
YAHOO_LIST_MAX_ROOTS_SCAN = 500

# 직전 `crawl_yahoo_stocks` / `crawl_yahoo_tech` 가 사용한 해외 파이프라인 진단(한 프로세스·마지막 실행).
_yahoo_overseas_last_pipeline_diag: dict[str, Any] = {}

_yahoo_overseas_bundle_cache: (
    tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]] | None
) = None


def _resolve_yahoo_href(href: str) -> str:
    h = (href or "").strip()
    if not h or h.startswith("#") or "javascript" in h.lower():
        return ""
    if h.startswith("http://") or h.startswith("https://"):
        return h.split("#")[0]
    if h.startswith("//"):
        return ("https:" + h).split("#")[0]
    return urljoin(YAHOO_FINANCE, h).split("#")[0]


def _parse_yahoo_publishing_line(text: str) -> tuple[str, str]:
    """
    div.publishing 한 줄: 언론사 • 날짜 형태(• 또는 ·) 앞/뒤.
    """
    s = re.sub(r"\s+", " ", (text or "").strip())
    for sep in ("•", "·", "|"):
        if sep in s:
            a, b = s.split(sep, 1)
            return a.strip(), b.strip()
    return s, ""


def _parse_yahoo_published_at_utc(raw: str, now: datetime) -> datetime | None:
    """
    Yahoo 목록의 published 문자열 → UTC aware datetime (UTC).
    약어: '5h ago', '1d ago', '4mo ago', '30m ago', '2y ago' (mo=30일, y=365일).
    서술형: '2 hours ago', '1 day ago', … 절대일자 등.
    파싱 실패 시 None.
    """
    if now.tzinfo is None:
        now = now.replace(tzinfo=timezone.utc)
    s = re.sub(r"\s+", " ", (raw or "").strip())
    if not s:
        return None
    sl = s.lower()
    if sl == "just now":
        return now
    # Yahoo 단축 상대시간 (예: 4mo ago, 13h ago) — 'mo'를 'm'보다 먼저 매칭
    m_short = re.match(r"(?is)^(\d+)\s*(mo|h|d|y|m|s|w)\s+ago\s*$", s)
    if m_short:
        n = int(m_short.group(1))
        if n < 0:
            return None
        u = m_short.group(2).lower()
        if u == "mo":
            return now - timedelta(days=30 * n)
        if u == "h":
            return now - timedelta(hours=n)
        if u == "d":
            return now - timedelta(days=n)
        if u == "y":
            return now - timedelta(days=365 * n)
        if u == "m":
            return now - timedelta(minutes=n)
        if u == "s":
            return now - timedelta(seconds=n)
        if u == "w":
            return now - timedelta(weeks=n)
        return None
    if sl in ("an hour ago", "a hour ago"):
        return now - timedelta(hours=1)
    if sl == "a minute ago":
        return now - timedelta(minutes=1)
    if sl == "a second ago":
        return now - timedelta(seconds=1)
    if sl == "a day ago":
        return now - timedelta(days=1)
    if sl == "a week ago":
        return now - timedelta(weeks=1)
    m = re.match(
        r"(?is)^(\d+)\s*(second|seconds|minute|minutes|hour|hours|day|days|week|weeks|month|months|year|years)\s+ago\s*$",
        s,
    )
    if m:
        n = int(m.group(1))
        u = m.group(2).lower()
        if u.startswith("second"):
            return now - timedelta(seconds=n)
        if u.startswith("minute"):
            return now - timedelta(minutes=n)
        if u.startswith("hour"):
            return now - timedelta(hours=n)
        if u.startswith("day"):
            return now - timedelta(days=n)
        if u.startswith("week"):
            return now - timedelta(weeks=n)
        if u.startswith("month"):
            return now - timedelta(days=30 * n)
        if u.startswith("year"):
            return now - timedelta(days=365 * n)
        return None
    if " at " in s:
        for fmt in ("%b %d, %Y at %I:%M %p", "%b %d, %Y at %I:%M%p", "%B %d, %Y at %I:%M %p"):
            try:
                return datetime.strptime(s, fmt).replace(tzinfo=timezone.utc)
            except ValueError:
                continue
    for fmt in ("%b %d, %Y", "%B %d, %Y"):
        try:
            base = datetime.strptime(s, fmt).replace(tzinfo=timezone.utc)
            return base.replace(hour=12, minute=0, second=0, microsecond=0)
        except ValueError:
            continue
    m3 = re.match(r"(?is)^(\d{4})-(\d{2})-(\d{2})\s*$", s)
    if m3:
        y, mo, d = int(m3.group(1)), int(m3.group(2)), int(m3.group(3))
        try:
            return datetime(y, mo, d, 12, 0, 0, tzinfo=timezone.utc)
        except ValueError:
            return None
    return None


def _merge_yahoo_stocks_tech_with_origin(
    stocks: list[dict[str, Any]],
    tech: list[dict[str, Any]],
) -> list[tuple[dict[str, Any], str]]:
    """stocks 우선, URL 기준 중복 제거 후 (row, 'stocks'|'tech')."""
    seen: set[str] = set()
    merged: list[tuple[dict[str, Any], str]] = []
    for row in stocks:
        u = (row.get("url") or "").strip()
        if not u or u in seen:
            continue
        seen.add(u)
        merged.append((row, "stocks"))
    for row in tech:
        u = (row.get("url") or "").strip()
        if not u or u in seen:
            continue
        seen.add(u)
        merged.append((row, "tech"))
    return merged


def _yahoo_rows_with_published_utc(
    merged: list[tuple[dict[str, Any], str]],
    now: datetime,
) -> tuple[list[tuple[dict[str, Any], str, datetime]], int]:
    """published_at 파싱 성공한 행만 (ISO 문자열로 갱신). 실패 행은 제외. 반환: (rows, parse_fail_count)."""
    out: list[tuple[dict[str, Any], str, datetime]] = []
    parse_fail = 0
    for row, origin in merged:
        hint = (row.get("published_at") or "").strip()
        parsed = _parse_yahoo_published_at_utc(hint, now)
        if parsed is None:
            parse_fail += 1
            continue
        r2 = dict(row)
        r2["published_at"] = parsed.isoformat()
        out.append((r2, origin, parsed))
    return out, parse_fail


def _yahoo_curate_by_time_windows(
    pre: list[tuple[dict[str, Any], str, datetime]],
    now: datetime,
    target: int,
) -> tuple[list[tuple[dict[str, Any], str]], list[tuple[float, int]]]:
    """
    6h→12h→24h→48h 윈도우 확장. target 이상이면 그 시점에서 상한 target.
    마지막 창까지도 부족하면 그때까지 통과한 기사만.
    반환: (curated, [(hours, eligible_count), ...])
    """
    if now.tzinfo is None:
        now = now.replace(tzinfo=timezone.utc)
    last: list[tuple[dict[str, Any], str]] = []
    step_counts: list[tuple[float, int]] = []
    for h in YAHOO_OVERSEAS_TIME_WINDOWS_HOURS:
        cutoff = now - timedelta(hours=h)
        eligible = [(r, o) for (r, o, p) in pre if p >= cutoff]
        last = eligible
        step_counts.append((h, len(eligible)))
        logger.info(
            "Yahoo 시간필터: 최근 %.0f시간 이내 %d건 (시각 파싱 통과 전체 %d건, 목표 %d건)",
            h,
            len(eligible),
            len(pre),
            target,
        )
        if len(eligible) >= target:
            return eligible[:target], step_counts
    return last, step_counts


def _yahoo_split_stocks_tech(curated: list[tuple[dict[str, Any], str]]) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    stocks: list[dict[str, Any]] = []
    tech: list[dict[str, Any]] = []
    for row, origin in curated:
        if origin == "stocks":
            stocks.append(row)
        else:
            tech.append(row)
    return stocks, tech


def _compute_yahoo_overseas_curated() -> tuple[
    list[dict[str, Any]],
    list[dict[str, Any]],
    list[dict[str, Any]],
]:
    """뉴스+테크 HTML 수집 → URL 병합 → 절대시간 파싱 → 6h/12h/24h 목표 30건 → 본문 첨부."""
    global _yahoo_overseas_last_pipeline_diag
    u_news = f"{YAHOO_FINANCE}/news/"
    u_tech = f"{YAHOO_FINANCE}/topic/tech/"
    diag_stocks: dict[str, Any] = {}
    diag_tech: dict[str, Any] = {}
    try:
        stocks_raw = _crawl_yahoo_finance_list_url(
            u_news, YAHOO_OVERSEAS_FETCH_CAP, attach_bodies=False, diag=diag_stocks
        )
        tech_raw = _crawl_yahoo_finance_list_url(
            u_tech, YAHOO_OVERSEAS_FETCH_CAP, attach_bodies=False, diag=diag_tech
        )
    except Exception as e:
        logger.exception("Yahoo overseas raw fetch 실패: %s", e)
        _yahoo_overseas_last_pipeline_diag = {
            "error": str(e),
            "stocks_diag": diag_stocks,
            "tech_diag": diag_tech,
        }
        return [], [], []
    merged = _merge_yahoo_stocks_tech_with_origin(stocks_raw, tech_raw)
    logger.info(
        "Yahoo [시간필터 전] raw 기사(목록 파싱·URL 병합 후 unique)=%d건 "
        "(stocks_raw=%d + tech_raw=%d → 중복 제거 후)",
        len(merged),
        len(stocks_raw),
        len(tech_raw),
    )
    now = datetime.now(timezone.utc)
    pre, parse_fail_merged = _yahoo_rows_with_published_utc(merged, now)
    logger.info(
        "Yahoo [시간필터 직전] published_at 시각 파싱 성공=%d건 실패(스킵)=%d건",
        len(pre),
        parse_fail_merged,
    )
    if merged and not pre:
        hints = [str((row.get("published_at") or ""))[:80] for row, _ in merged[:8]]
        logger.warning(
            "Yahoo: 병합 URL %d건 중 published_at 시각 파싱 성공 0건(전부 스킵). "
            "날짜 문자열 형식 변경 가능. 샘플: %s",
            len(merged),
            hints,
        )
    curated, time_steps = _yahoo_curate_by_time_windows(pre, now, YAHOO_OVERSEAS_MERGE_TARGET)
    stocks_f, tech_f = _yahoo_split_stocks_tech(curated)
    merged_ordered = [r for r, _ in curated]
    logger.info(
        "Yahoo [시간필터 후] 남은 기사=%d건 (stocks=%d tech=%d) 단계별후보=%s",
        len(merged_ordered),
        len(stocks_f),
        len(tech_f),
        time_steps,
    )
    _attach_yahoo_article_bodies(stocks_f)
    _attach_yahoo_article_bodies(tech_f)
    _yahoo_overseas_last_pipeline_diag = {
        "stocks_raw_rows": len(stocks_raw),
        "tech_raw_rows": len(tech_raw),
        "stocks_html_diag": diag_stocks,
        "tech_html_diag": diag_tech,
        "merged_unique_before_time_filter": len(merged),
        "published_parse_ok": len(pre),
        "published_parse_dropped": parse_fail_merged,
        "time_window_hours": list(YAHOO_OVERSEAS_TIME_WINDOWS_HOURS),
        "time_filter_steps_eligible": time_steps,
        "after_time_filter_merged": len(merged_ordered),
        "final_stocks": len(stocks_f),
        "final_tech": len(tech_f),
        "target": YAHOO_OVERSEAS_MERGE_TARGET,
        # 하위 호환 키
        "merged_unique_urls": len(merged),
        "final_merged": len(merged_ordered),
    }
    logger.info(
        "Yahoo overseas: stocks_raw=%d tech_raw=%d merged_unique=%d parsed_ok=%d parse_drop=%d -> "
        "time_windows %s -> merged=%d stocks=%d tech=%d (target=%d) | HTML li stocks=%s tech=%s",
        len(stocks_raw),
        len(tech_raw),
        len(merged),
        len(pre),
        parse_fail_merged,
        time_steps,
        len(merged_ordered),
        len(stocks_f),
        len(tech_f),
        YAHOO_OVERSEAS_MERGE_TARGET,
        diag_stocks.get("li_stream_story"),
        diag_tech.get("li_stream_story"),
    )
    if not merged_ordered:
        logger.warning(
            "Yahoo overseas 최종 0건: HTTP stocks=%s tech=%s | roots stocks=%s tech=%s | "
            "rows stocks=%d tech=%d | 병합 unique=%d | 시각 OK=%d 실패=%d | 시간창=%s | "
            "필드실패 stocks=%s tech=%s",
            diag_stocks.get("http_status"),
            diag_tech.get("http_status"),
            diag_stocks.get("li_stream_story"),
            diag_tech.get("li_stream_story"),
            len(stocks_raw),
            len(tech_raw),
            len(merged),
            len(pre),
            parse_fail_merged,
            time_steps,
            diag_stocks.get("parse_fail_reasons"),
            diag_tech.get("parse_fail_reasons"),
        )
    return stocks_f, tech_f, merged_ordered


def _ensure_yahoo_overseas_bundle_cached() -> tuple[
    list[dict[str, Any]],
    list[dict[str, Any]],
    list[dict[str, Any]],
]:
    global _yahoo_overseas_bundle_cache
    if _yahoo_overseas_bundle_cache is None:
        _yahoo_overseas_bundle_cache = _compute_yahoo_overseas_curated()
    return _yahoo_overseas_bundle_cache


def _first_img_src_in(root: Any) -> str:
    for img in root.find_all("img", src=True):
        src = (img.get("src") or "").strip()
        if not src or src.startswith("data:"):
            continue
        if src.startswith("//"):
            return "https:" + src
        if src.startswith("/"):
            return urljoin(YAHOO_FINANCE, src)
        if src.startswith("http"):
            return src.split("#")[0]
    return ""


def _yahoo_selector_probe_counts(soup: BeautifulSoup) -> dict[str, int]:
    """디버그: 주요 후보 셀렉터별 매칭 개수(실제 HTML과의 대조용)."""
    probes: list[tuple[str, str]] = [
        ("li.stream-item.story-item", "li.stream-item.story-item"),
        ("li.stream-item", "li.stream-item"),
        ("li.js-stream-content", "li.js-stream-content"),
        ("section.storyitem", 'section[data-testid="storyitem"]'),
        ("section.StoryItem", 'section[data-testid="StoryItem"]'),
        ("a.titles", "a.titles"),
        ("h3.clamp", "h3.clamp"),
    ]
    return {label: len(soup.select(sel)) for label, sel in probes}


def _yahoo_story_roots_from_sections(soup: BeautifulSoup) -> list[Any]:
    """section[data-testid=storyitem] 기준으로 상위 li|article|div 를 스토리 루트로 수집."""
    roots: list[Any] = []
    seen: set[int] = set()
    for sec in soup.select(
        'section[data-testid="storyitem"], section[data-testid="StoryItem"]',
    ):
        p = sec.find_parent("li") or sec.find_parent("article") or sec.find_parent("div")
        if p is None:
            p = sec
        pid = id(p)
        if pid not in seen:
            seen.add(pid)
            roots.append(p)
    return roots


def _yahoo_collect_story_root_nodes(soup: BeautifulSoup) -> tuple[list[Any], str]:
    """
    스토리 카드 루트 노드 목록과 사용한 전략 라벨.
    우선순위: stream-item.story-item → js-stream-content → stream-item → section 기반.
    """
    strategies: list[tuple[str, str]] = [
        ("li.stream-item.story-item", "li.stream-item.story-item"),
        ("li.js-stream-content", "li.js-stream-content"),
        ("li.stream-item", "li.stream-item"),
    ]
    for label, sel in strategies:
        els = soup.select(sel)
        if els:
            return list(els), label
    roots = _yahoo_story_roots_from_sections(soup)
    if roots:
        return roots, "section[storyitem|StoryItem]→ancestor(li|article|div)"
    return [], "(none)"


def _yahoo_fallback_title_anchor(section: Any) -> Any:
    """a.titles 없을 때 제목 링크 추정."""
    for cand in section.select("a[href]"):
        href = (cand.get("href") or "").strip()
        if not href or href.startswith("#"):
            continue
        text = cand.get_text(strip=True) or ""
        if len(text) < 4:
            continue
        hl = href.lower()
        if "/news/" in hl or "/videos/" in hl or "/quote/" in hl or "finance.yahoo.com" in hl:
            return cand
    h3 = section.select_one("h3")
    if h3 is not None:
        pa = h3.find_parent("a")
        if pa is not None and (pa.get("href") or "").strip():
            return pa
    return None


def _parse_yahoo_finance_story_item_detailed(root: Any) -> tuple[dict[str, Any] | None, str]:
    """
    Yahoo 뉴스 스트림 한 건 파싱. 실패 시 (None, 사유코드).
    root: li.stream-item… 또는 section의 조상 래퍼.
    """
    section = root.select_one('section[data-testid="storyitem"]') or root.select_one(
        'section[data-testid="StoryItem"]',
    )
    if section is None and getattr(root, "name", None) == "section":
        tid = (root.get("data-testid") or "").strip()
        if tid.lower() == "storyitem":
            section = root
    if section is None:
        return None, "no_section"

    a = None
    for cand in section.select("a[href]"):
        classes = cand.get("class") or []
        if isinstance(classes, str):
            classes = classes.split()
        if "titles" in classes:
            a = cand
            break
    if a is None:
        a = _yahoo_fallback_title_anchor(section)
    if a is None:
        return None, "no_title_anchor"

    href = (a.get("href") or "").strip()
    url = _resolve_yahoo_href(href)
    if not url:
        return None, "no_url"

    h3 = (
        section.select_one("h3.clamp")
        or section.select_one("h3")
        or root.select_one("h3.clamp")
        or root.select_one("h3")
    )
    title = ""
    if h3 is not None and h3.get_text(strip=True):
        title = h3.get_text(strip=True)
    if not title and a.get("title"):
        title = str(a.get("title", "")).strip()
    if not title:
        title = a.get_text(strip=True) or ""
    if len(title) < 2:
        return None, "title_too_short"

    pub = section.select_one("div.publishing") or root.select_one("div.publishing")
    pub_t = pub.get_text(" ", strip=True) if pub else ""
    source, date_hint = _parse_yahoo_publishing_line(pub_t)
    if not source and pub_t:
        source = pub_t
    if not source:
        source = "Yahoo Finance"

    thumb = _first_img_src_in(root)
    t_for_sum = title
    summary = (t_for_sum[:SUMMARY_CHARS] + ("…" if len(t_for_sum) > SUMMARY_CHARS else "")).strip()
    return {
        "title": title,
        "summary": summary,
        "source": source,
        "url": _normalize_url(url),
        "published_at": date_hint,
        "category": "",
        "thumbnail_url": thumb,
    }, "ok"


def _parse_yahoo_finance_story_item(root: Any) -> dict[str, Any] | None:
    """section[data-testid=storyitem], a.titles(또는 폴백 링크), h3, div.publishing."""
    row, _reason = _parse_yahoo_finance_story_item_detailed(root)
    return row


def _crawl_yahoo_finance_list_url(
    page_url: str,
    max_n: int,
    *,
    attach_bodies: bool = True,
    diag: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    d = diag if diag is not None else {}
    d.clear()
    d["page_url"] = page_url
    log_h = "YahooList"
    try:
        html, status, fetch_err = _fetch_http_debug(page_url, log_label=log_h)
        d["http_status"] = status
        d["http_fetch_error"] = fetch_err
        if not html:
            d["html_len"] = 0
            logger.warning(
                "Yahoo 목록 HTML 없음: page=%s http_status=%s err=%s",
                page_url,
                status,
                fetch_err,
            )
            return out
        d["html_len"] = len(html)
        hl = html[:2000].lower()
        if any(
            k in hl
            for k in (
                "consent",
                "captcha",
                "robot check",
                "are you human",
                "unusual traffic",
                "enable javascript",
                "noscript",
            )
        ):
            logger.warning(
                "Yahoo 목록 HTML에 동의/봇·차단·스크립트 필요 징후: page=%s html_len=%d http=%s",
                page_url,
                len(html),
                status,
            )
        soup = BeautifulSoup(html, "lxml")
        probe = _yahoo_selector_probe_counts(soup)
        d["selector_probe_counts"] = probe
        logger.info(
            "%s: 셀렉터 프로브 counts=%s page=%s http=%s",
            log_h,
            probe,
            page_url,
            status,
        )

        li_els, strat = _yahoo_collect_story_root_nodes(soup)
        d["selector_strategy"] = strat
        d["li_stream_story"] = len(li_els)
        scan_roots = li_els
        if len(li_els) > YAHOO_LIST_MAX_ROOTS_SCAN:
            logger.warning(
                "%s: 스토리 루트 후보 %d건 → 상위 %d건만 필드 파싱 스캔",
                log_h,
                len(li_els),
                YAHOO_LIST_MAX_ROOTS_SCAN,
            )
            scan_roots = li_els[:YAHOO_LIST_MAX_ROOTS_SCAN]
        logger.info(
            "%s: 스토리 루트 %d건 (전략=%s) page=%s",
            log_h,
            len(li_els),
            strat,
            page_url,
        )

        seen: set[str] = set()
        reason_counter: Counter[str] = Counter()
        dup_skip = 0
        for node in scan_roots:
            row, reason = _parse_yahoo_finance_story_item_detailed(node)
            if not row:
                reason_counter[reason] += 1
                continue
            u = (row.get("url") or "").strip()
            if not u:
                reason_counter["empty_url_after_row"] += 1
                continue
            if u in seen:
                dup_skip += 1
                continue
            seen.add(u)
            out.append(row)
            if len(out) >= max_n:
                break

        parse_fail = int(sum(reason_counter.values()))
        d["parse_fail_reasons"] = dict(reason_counter)
        d["dup_url_skipped"] = dup_skip
        d["rows_out"] = len(out)
        d["parse_fail_li"] = parse_fail
        d["roots_scanned"] = len(scan_roots)

        logger.info(
            "%s: 필드 파싱 요약 roots=%d rows_out=%d dup_skip=%d reasons=%s page=%s",
            log_h,
            len(li_els),
            len(out),
            dup_skip,
            dict(reason_counter),
            page_url,
        )

        if not li_els:
            logger.error(
                "%s: 스토리 루트 0건 — 마크업 변경 가능. probe=%s page=%s html_len=%d http=%s",
                log_h,
                probe,
                page_url,
                len(html),
                status,
            )
        elif not out:
            logger.warning(
                "%s: 루트는 %d건이나 유효 기사 0건 reasons=%s dup=%d page=%s",
                log_h,
                len(li_els),
                dict(reason_counter),
                dup_skip,
                page_url,
            )
        else:
            logger.info(
                "%s: 목록 파싱 OK page=%s rows=%d roots=%d",
                log_h,
                page_url,
                len(out),
                len(li_els),
            )
        if attach_bodies:
            _attach_yahoo_article_bodies(out)
    except Exception as e:
        logger.exception(
            "Yahoo 목록 HTML 파싱·처리 중 예외 page=%s: %s",
            page_url,
            e,
        )
        d["exception"] = repr(e)
    return out


def crawl_yahoo_overseas_stocks_and_tech() -> tuple[
    list[dict[str, Any]],
    list[dict[str, Any]],
    list[dict[str, Any]],
]:
    """
    Yahoo 뉴스(/news/) + 테크(/topic/tech/)를 한 번에 수집.
    URL 중복 제거 후 published_at 파싱 성공분만 사용, 6h→12h→24h→48h 시간 창으로 최대 30건.
    반환: (overseas_stocks, overseas_tech, yahoo_merged_curated_order)
    """
    try:
        s, t, m = _ensure_yahoo_overseas_bundle_cached()
        ns, nt, nm = len(s), len(t), len(m)
        _log_yahoo_pipeline_diag_common(
            _yahoo_overseas_last_pipeline_diag,
            "crawl_yahoo_overseas_stocks_and_tech",
            max(ns, nt, nm),
        )
        return list(s), list(t), list(m)
    except Exception as e:
        logger.exception("crawl_yahoo_overseas_stocks_and_tech 실패: %s", e)
        return [], [], []


def _log_yahoo_pipeline_diag_common(d: dict[str, Any], label: str, returned: int) -> None:
    """해외 파이프라인 직후 진단: HTTP·셀렉터·필드 파싱 vs 시각 파싱 vs 시간창."""
    if not d:
        logger.info("%s: 반환 %d건 (진단 dict 비어 있음 — 예외 직후 등)", label, returned)
        return
    sd = d.get("stocks_html_diag") or {}
    td = d.get("tech_html_diag") or {}
    logger.info(
        "%s: 반환 %d건 | HTTP stocks=%s tech=%s | roots stocks=%s(%s) tech=%s(%s) | "
        "probe_stocks=%s probe_tech=%s | 필드실패 stocks=%s tech=%s | "
        "시간필터전unique=%d 시각파싱OK=%d 시각파싱제외=%d | 시간창(시간h,후보)=%s | 시간필터후=%d",
        label,
        returned,
        sd.get("http_status"),
        td.get("http_status"),
        sd.get("li_stream_story"),
        sd.get("selector_strategy"),
        td.get("li_stream_story"),
        td.get("selector_strategy"),
        sd.get("selector_probe_counts"),
        td.get("selector_probe_counts"),
        sd.get("parse_fail_reasons"),
        td.get("parse_fail_reasons"),
        d.get("merged_unique_before_time_filter", d.get("merged_unique_urls", -1)),
        d.get("published_parse_ok", -1),
        d.get("published_parse_dropped", -1),
        d.get("time_filter_steps_eligible"),
        d.get("after_time_filter_merged", d.get("final_merged", -1)),
    )
    if returned == 0:
        logger.warning(
            "%s: 0건 — 원인 분기: (1) http_status≠200 또는 roots=0 → 요청/차단/HTML변경. "
            "(2) parse_fail_reasons 집중 → 필드 셀렉터 불일치. "
            "(3) 시각파싱OK=0 → published_at 문자열 형식. "
            "(4) 시각파싱OK>0 & 시간필터후=0 → 시간창(time_filter_steps).",
            label,
        )


def _log_yahoo_crawl_entrypoint(name: str, returned: int) -> None:
    _log_yahoo_pipeline_diag_common(_yahoo_overseas_last_pipeline_diag, name, returned)


def _log_yahoo_feed_workflow_dispatch(
    fn_label: str,
    *,
    feed_diag: dict[str, Any],
    returned_count: int,
) -> None:
    """workflow_dispatch 로그용: 요청 1)~5) 한 피드(stocks 또는 tech) 기준."""
    pipe = _yahoo_overseas_last_pipeline_diag
    logger.info(
        "%s [확인] (1) HTTP status=%s url=%s | "
        "(2) 셀렉터 루트=%d strategy=%r probe=%s | "
        "(3) 필드파싱 실패 reasons=%s rows_out=%s | "
        "(4) 시간필터 전 파이프 unique URL=%s | (5) 시간필터 후 이 목록 반환=%d",
        fn_label,
        feed_diag.get("http_status"),
        feed_diag.get("page_url"),
        int(feed_diag.get("li_stream_story") or 0),
        feed_diag.get("selector_strategy"),
        feed_diag.get("selector_probe_counts"),
        feed_diag.get("parse_fail_reasons"),
        feed_diag.get("rows_out"),
        pipe.get("merged_unique_before_time_filter", pipe.get("merged_unique_urls")),
        returned_count,
    )


def crawl_yahoo_stocks() -> list[dict[str, Any]]:
    """
    Yahoo Finance 뉴스 메인 스트림 (요청이 차단될 수 있어 빈 리스트일 수 있음).
    https://finance.yahoo.com/news/
    해외 파이프라인은 `crawl_yahoo_overseas_stocks_and_tech`와 동일 캐시를 사용한다.
    """
    try:
        s, _, _ = _ensure_yahoo_overseas_bundle_cached()
        _log_yahoo_crawl_entrypoint("crawl_yahoo_stocks", len(s))
        _log_yahoo_feed_workflow_dispatch(
            "crawl_yahoo_stocks",
            feed_diag=_yahoo_overseas_last_pipeline_diag.get("stocks_html_diag") or {},
            returned_count=len(s),
        )
        return list(s)
    except Exception as e:
        logger.exception("crawl_yahoo_stocks 실패: %s", e)
        return []


def crawl_yahoo_tech() -> list[dict[str, Any]]:
    """
    Tech 토픽 뉴스 스트림.
    https://finance.yahoo.com/topic/tech/
    해외 파이프라인은 `crawl_yahoo_overseas_stocks_and_tech`와 동일 캐시를 사용한다.
    """
    try:
        _, t, _ = _ensure_yahoo_overseas_bundle_cached()
        _log_yahoo_crawl_entrypoint("crawl_yahoo_tech", len(t))
        _log_yahoo_feed_workflow_dispatch(
            "crawl_yahoo_tech",
            feed_diag=_yahoo_overseas_last_pipeline_diag.get("tech_html_diag") or {},
            returned_count=len(t),
        )
        return list(t)
    except Exception as e:
        logger.exception("crawl_yahoo_tech 실패: %s", e)
        return []


NAVER_MEDIA = "https://media.naver.com"
MAX_NEWSPAPER_ITEMS = 30


def _absolutize_media_naver_url(href: str) -> str:
    h = (href or "").strip()
    if not h or h.startswith("#") or "javascript" in h.lower():
        return ""
    if h.startswith("http://") or h.startswith("https://"):
        return h.split("#")[0]
    if h.startswith("//"):
        return ("https:" + h).split("#")[0]
    return urljoin(NAVER_MEDIA, h).split("#")[0]


def _absolutize_naver_newspaper_img(src: str) -> str:
    t = (src or "").strip()
    if not t or t.startswith("data:"):
        return ""
    if t.startswith("//"):
        return "https:" + t
    if t.startswith("http://") or t.startswith("https://"):
        return t.split("#")[0]
    if t.startswith("/"):
        return urljoin(NAVER_MEDIA, t)
    return t


def _parse_naver_press_newspaper_li(li, source_label: str) -> dict[str, Any] | None:
    """ul.newspaper_article_lst li — a[href], strong 제목, img[ src ]."""
    a = li.select_one("a[href]")
    if not a:
        return None
    url = _absolutize_media_naver_url(a.get("href", ""))
    if not url:
        return None
    st = a.select_one("strong") or li.select_one("strong")
    title = (st.get_text(strip=True) if st else a.get_text(strip=True) or "").strip()
    if len(title) < 2:
        return None
    img = li.select_one("img[src]")
    thumb = _absolutize_naver_newspaper_img((img.get("src") or "").strip()) if img else ""
    summ = (title[:SUMMARY_CHARS] + ("…" if len(title) > SUMMARY_CHARS else "")).strip()
    return {
        "title": title,
        "summary": summ,
        "source": source_label,
        "url": _normalize_url(url),
        "published_at": "",
        "category": "",
        "thumbnail_url": thumb,
    }


def _newspaper_paper_number_int(node: dict[str, Any]) -> int:
    """paperNumber(예: A1)에서 숫자만 추출. 없거나 숫자 없으면 999."""
    raw = node.get("paperNumber") or node.get("paper_number")
    if raw is None:
        return 999
    s = str(raw).strip()
    if not s:
        return 999
    m = re.search(r"\d+", s)
    return int(m.group()) if m else 999


def _newspaper_detail_position_int(node: dict[str, Any]) -> int:
    """detailPosition — 면 내 기사 순서. 없거나 비정상이면 99."""
    v = node.get("detailPosition")
    if v is None:
        return 99
    try:
        return int(v)
    except (TypeError, ValueError):
        return 99


def _crawl_naver_newspaper(press_code: str, source_label: str, max_n: int) -> list[dict[str, Any]]:
    def _newspaper_service_time_published(val: Any) -> str:
        """serviceTime(밀리초 epoch 문자열/숫자) → KST 표시 문자열. 실패 시 빈 문자열."""
        if val is None:
            return ""
        s = str(val).strip()
        if not s or not s.isdigit():
            return ""
        try:
            ms = int(s)
            dt_utc = datetime.fromtimestamp(ms / 1000.0, tz=pytz.UTC)
            dt_seoul = dt_utc.astimezone(pytz.timezone("Asia/Seoul"))
            return dt_seoul.strftime("%Y-%m-%d %H:%M")
        except (OSError, ValueError, OverflowError):
            return ""

    def _parse_payload(payload: Any, fallback_source: str, limit: int) -> list[dict[str, Any]]:
        """
        API dict: { officeId, newspaperOfficeMainPerPaper: [{ paperNumber, newspaperOfficeMain }, ...] }
        레거시 list: [{ paperNumber, newspaperOfficeMain }, ...]
        기사 URL: https://n.news.naver.com/article/{officeId}/{articleId}
        """
        root_office_id = ""
        if isinstance(payload, dict):
            sections = payload.get("newspaperOfficeMainPerPaper") or []
            root_office_id = str(payload.get("officeId") or "").strip()
        elif isinstance(payload, list):
            sections = payload
        else:
            return []

        if not isinstance(sections, list):
            return []

        out: list[dict[str, Any]] = []
        for section in sections:
            if not isinstance(section, dict):
                continue
            paper_number = _newspaper_paper_number_int(section)
            articles = section.get("newspaperOfficeMain")
            if not isinstance(articles, list):
                continue
            for art in articles:
                if not isinstance(art, dict):
                    continue
                article_id = str(art.get("articleId") or "").strip()
                office_id = str(art.get("officeId") or "").strip() or root_office_id
                if not article_id or not office_id:
                    continue
                url = _normalize_url(f"https://n.news.naver.com/article/{office_id}/{article_id}")
                if not url:
                    continue
                title = str(art.get("title") or "").strip()
                if len(title) < 2:
                    continue
                thumb_raw = str(
                    art.get("thumbnailImgUrl")
                    or art.get("thumnailImgUrl")
                    or art.get("thumbnailUrl")
                    or ""
                ).strip()
                thumb = _absolutize_naver_newspaper_img(thumb_raw)
                if not thumb:
                    thumb = _extract_naver_article_first_image(url)
                detail_position = _newspaper_detail_position_int(art)
                source = str(art.get("officeName") or "").strip() or fallback_source
                summary_raw = str(art.get("summary") or art.get("lede") or "").strip()
                summary = (
                    (summary_raw[:SUMMARY_CHARS] + ("…" if len(summary_raw) > SUMMARY_CHARS else ""))
                    if summary_raw
                    else (title[:SUMMARY_CHARS] + ("…" if len(title) > SUMMARY_CHARS else ""))
                ).strip()
                published = _newspaper_service_time_published(art.get("serviceTime"))
                if not published:
                    published = str(
                        art.get("date") or art.get("publishedAt") or art.get("articleDate") or ""
                    ).strip()

                out.append(
                    {
                        "title": title,
                        "summary": summary,
                        "source": source,
                        "url": url,
                        "published_at": published,
                        "category": "",
                        "thumbnail_url": thumb,
                        "paper_number": paper_number,
                        "detail_position": detail_position,
                    }
                )

        out.sort(key=lambda r: (r["paper_number"], r["detail_position"]))
        seen_url: set[str] = set()
        deduped: list[dict[str, Any]] = []
        for row in out:
            u = (row.get("url") or "").strip()
            if not u or u in seen_url:
                continue
            seen_url.add(u)
            deduped.append(row)
        return deduped[:limit]

    seoul = pytz.timezone("Asia/Seoul")
    today = datetime.now(seoul)
    date_try_labels = ("오늘", "어제", "그제", "3일 전")
    # 오늘 → 어제 → 그제 → 3일 전까지 (주말·공휴일 등으로 당일 API가 빈 배열일 때 대비)
    for days_back in range(4):
        target_day = today - timedelta(days=days_back)
        date_str = target_day.strftime("%Y%m%d")
        api_url = f"{NAVER_MEDIA}/api/press/{press_code}/newspaper?date={date_str}"
        date_try_label = date_try_labels[days_back]

        req_headers = dict(HEADERS)
        req_headers["Referer"] = f"https://media.naver.com/press/{press_code}/newspaper"
        req_headers["User-Agent"] = (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        )

        status_code: int | None = None
        raw_json: Any = None
        try:
            r = requests.get(api_url, headers=req_headers, timeout=REQUEST_TIMEOUT)
            status_code = r.status_code
            r.raise_for_status()
            raw_json = r.json()
        except requests.HTTPError as e:
            if e.response is not None:
                status_code = e.response.status_code
            logger.warning("_crawl_naver_newspaper HTTP 오류 press=%s url=%s: %s", press_code, api_url, e)
        except Exception as e:
            logger.warning("_crawl_naver_newspaper 요청 실패 press=%s url=%s: %s", press_code, api_url, e)

        payload = raw_json
        logger.info("RAW PAYLOAD 앞 500자: %s", str(payload)[:500])

        rows = _parse_payload(payload, source_label, max_n) if payload is not None else []
        logger.info(
            "_crawl_naver_newspaper press=%s source=%s 날짜시도=%s(%s) url=%s status=%s articles=%d",
            press_code,
            source_label,
            date_try_label,
            date_str,
            api_url,
            status_code if status_code is not None else "-",
            len(rows),
        )
        if rows:
            rows.sort(key=lambda r: (r.get("paper_number", 999), r.get("detail_position", 99)))
            # 뉴스탭(crawl_naver_realtime 등)과 동일: 각 row에 `body` 채움 → 브리핑 요약에 본문 전달
            _attach_naver_article_bodies(rows)
            # 한경·매경 공통: 본문 없음·짧은 기사는 풀에 넣지 않음 (briefing_hankyung_pool / briefing_maeil_pool 동일 기준)
            filtered = _filter_briefing_newspaper_rows_by_body(rows, press_code=press_code)
            if filtered:
                return filtered
            logger.info(
                "브리핑 신문: 본문 필터로 당일 후보 전부 제거 → 다음 날짜 시도 press=%s %s(%s)",
                press_code,
                date_try_label,
                date_str,
            )
    return []


def crawl_hankyung_newspaper() -> list[dict[str, Any]]:
    """NAVER 뉴스 — 한국경제 신문 스탠드 (최대 30). 본문 200자 미만·빈 본문은 제외. 파이프라인은 면·면 내 순 정렬 후 상위 5건만 briefing/hankyung에 저장."""
    try:
        return _crawl_naver_newspaper(
            "015",
            "한국경제",
            MAX_NEWSPAPER_ITEMS,
        )
    except Exception as e:
        logger.exception("crawl_hankyung_newspaper 실패: %s", e)
        return []


def crawl_maeil_newspaper() -> list[dict[str, Any]]:
    """NAVER 뉴스 — 매일경제 신문 스탠드 (최대 30). 본문 200자 미만·빈 본문은 제외. 파이프라인은 면·면 내 순 정렬 후 상위 5건만 briefing/maeil에 저장."""
    try:
        return _crawl_naver_newspaper(
            "009",
            "매일경제",
            MAX_NEWSPAPER_ITEMS,
        )
    except Exception as e:
        logger.exception("crawl_maeil_newspaper 실패: %s", e)
        return []


def _fetch_stock_naver_json(url: str) -> Any | None:
    """stock.naver.com JSON API. 실패 시 None."""
    headers = dict(HEADERS)
    headers["Referer"] = "https://stock.naver.com/"
    headers.setdefault("Accept", "application/json")
    try:
        r = requests.get(url, headers=headers, timeout=REQUEST_TIMEOUT)
        body_200 = (r.text or "")[:200]
        logger.info(
            "stock.naver API 응답: status=%s url=%s body_200=%s",
            r.status_code,
            url,
            body_200,
        )
        r.raise_for_status()
        return r.json()
    except Exception as e:
        logger.warning("stock.naver JSON 요청 실패 %s: %s", url, e)
        return None


def _unwrap_json_array(payload: Any) -> list[Any]:
    """API 최상위 또는 result/data/items 등 안의 리스트를 꺼낸다."""
    if isinstance(payload, list):
        return payload
    if not isinstance(payload, dict):
        return []
    for k in (
        "result",
        "data",
        "body",
        "themes",
        "upjong",
        "upjongs",
        "items",
        "categories",
        "categoryList",
        "list",
        "content",
        "stocks",
        "stockList",
        "stockInfos",
    ):
        v = payload.get(k)
        if isinstance(v, list):
            return v
        if isinstance(v, dict):
            for k2 in ("themes", "items", "list", "categories", "stocks", "stockList", "content", "data"):
                inner = v.get(k2)
                if isinstance(inner, list):
                    return inner
    return []


def _naver_theme_format_change_rate(val: Any) -> str:
    if val is None or val == "":
        return ""
    s = str(val).strip()
    if "%" in s:
        return s
    try:
        x = float(val)  # type: ignore[arg-type]
        sign = "+" if x > 0 else ""
        if x == 0.0:
            return "0.00%"
        return f"{sign}{x:.2f}%"
    except (TypeError, ValueError):
        return s


def _naver_theme_format_price(val: Any) -> str:
    if val is None or val == "":
        return ""
    s = str(val).strip()
    if "," in s and re.search(r"\d", s):
        return s
    try:
        n = int(float(str(val).replace(",", "")))  # type: ignore[arg-type]
        return f"{n:,}"
    except (TypeError, ValueError):
        return s


def _naver_theme_category_info_from_payload(payload: Any) -> str:
    """theme/.../info 응답에서 categoryInfo 문자열 추출. 실패·누락 시 빈 문자열."""
    if not isinstance(payload, dict):
        return ""
    val = payload.get("categoryInfo")
    if val is None:
        val = payload.get("categoryinfo")
    if val is None:
        return ""
    return str(val).strip()


def _naver_theme_list_from_api(payload: Any) -> list[dict[str, str]]:
    """
    테마 목록 JSON → [{"theme_id", "theme_name", "change_rate"}, ...]
    최상위 리스트 각 항목: no → theme_id, name → theme_name, changeRate → 등락률 문자열.
    changeRate는 숫자·문자열 모두 `+8.38%` 형태로 맞춘다.
    """
    out: list[dict[str, str]] = []
    for item in _unwrap_json_array(payload):
        if not isinstance(item, dict):
            continue
        tid = str(item.get("no") or item.get("themeId") or item.get("id") or "").strip()
        name = str(item.get("name") or item.get("themeName") or "").strip()
        if not tid or not name:
            continue
        chg = item.get("changeRate")
        if chg is None or (isinstance(chg, str) and not chg.strip()):
            chg = (
                item.get("fluctuationsRatio")
                or item.get("priceChangeRate")
                or item.get("rtn")
                or item.get("rate")
                or item.get("chgRate")
            )
        out.append(
            {
                "theme_id": tid,
                "theme_name": name,
                "change_rate": _naver_theme_format_change_rate(chg),
            }
        )
    return out


def _naver_theme_format_signed_amount(val: Any) -> str:
    """전일대비 금액 등 부호 있는 숫자(원 단위 등)."""
    if val is None or val == "":
        return ""
    s = str(val).strip()
    if "%" in s:
        return s
    try:
        x = float(val)  # type: ignore[arg-type]
        sign = "+" if x > 0 else ""
        if abs(x - round(x)) < 1e-6:
            return f"{sign}{format(int(round(x)), ',')}"
        return f"{sign}{x:,.2f}"
    except (TypeError, ValueError):
        return s


def _naver_stocklist_now_price_display(now_price: Any) -> str:
    """stocklist `nowPrice` → 천단위 콤마 현재가 (예: 12890 → \"12,890\")."""
    if now_price is None:
        return ""
    s = str(now_price).strip().replace(",", "")
    if not s:
        return ""
    try:
        x = float(s)
        if x != x:  # NaN
            return ""
        if abs(x - round(x)) < 1e-9:
            return f"{int(round(x)):,}"
        return f"{x:,.2f}"
    except (TypeError, ValueError):
        return ""


def _naver_stocklist_prev_change_display(prev_change_rate: Any, up_down_gb: Any) -> str | None:
    """
    stocklist `prevChangeRate` + `upDownGb` → 등락률 문자열.
    1=하락, 2=상승, 3=보합, 4=하한가, 5=상한가.
    1·2인데 등락률 값이 없으면 None (호출측에서 기존 파서로 폴백).
    """
    gb = str(up_down_gb or "").strip()
    if gb == "5":
        return "상한가"
    if gb == "4":
        return "하한가"
    if gb == "3":
        return "0%"
    if gb not in ("1", "2"):
        return None
    raw = (
        str(prev_change_rate).strip().rstrip("%").replace(",", "")
        if prev_change_rate is not None
        else ""
    )
    if raw == "":
        return None
    try:
        mag = abs(float(raw))
    except (TypeError, ValueError):
        return None
    s = f"{mag:.2f}"
    if gb == "2":
        return f"+{s}%"
    return f"-{s}%"


def _naver_stocklist_stock_price_and_change(item: dict) -> tuple[str, str]:
    """테마·업종 stocklist 공통: nowPrice / prevChangeRate+upDownGb 우선, 없으면 기존 필드 폴백."""
    now_p = item.get("nowPrice") or item.get("nowprice")
    price = _naver_stocklist_now_price_display(now_p)
    chg = _naver_stocklist_prev_change_display(
        item.get("prevChangeRate"),
        item.get("upDownGb"),
    )
    if not price:
        price_raw = (
            item.get("closePrice")
            or item.get("closeprice")
            or item.get("now")
            or item.get("dealPrice")
            or item.get("price")
            or item.get("prpr")
            or item.get("currentPrice")
            or item.get("stockPrice")
        )
        price = _naver_theme_format_price(price_raw)
    if chg is None:
        chg = _naver_theme_stock_change_display(item)
    return price, chg


def _naver_theme_stock_change_display(item: dict) -> str:
    """
    stock.naver 테마 종목 1건 기준 등락률 문자열.
    upDownGb: 1=하락, 2=상승, 3=보합, 4=하한가, 5=상한가 (API 문서·실응답 기준).
    fluctuationsRatio는 보통 절대값(%)이며, 1·2에서는 부호를 upDownGb에 맞춘다.
    """
    gb = str(item.get("upDownGb") or "").strip()
    if gb == "5":
        return "상한가"
    if gb == "4":
        return "하한가"
    if gb == "3":
        return "0%"

    fr = item.get("fluctuationsRatio")
    if fr is None or (isinstance(fr, str) and not str(fr).strip()):
        cp = item.get("compareToPreviousClosePrice")
        if cp is not None and str(cp).strip() != "":
            return _naver_theme_format_signed_amount(cp)
        return _naver_theme_format_change_rate(
            item.get("prdyCtrt") or item.get("changeRate") or item.get("rate") or item.get("chgRate")
        )

    s = str(fr).strip()
    if not s:
        return ""
    if "%" in s:
        return s

    raw = s.rstrip("%").replace(",", "")
    try:
        signed_val = float(raw)
    except (TypeError, ValueError):
        return s
    mag = abs(signed_val)

    if gb == "1":
        sign = "-"
    elif gb == "2":
        sign = "+"
    else:
        if signed_val > 0:
            sign = "+"
        elif signed_val < 0:
            sign = "-"
        else:
            sign = ""

    if mag == 0.0:
        return "0.00%"
    return f"{sign}{mag:.2f}%"


def _naver_theme_stocks_from_api(payload: Any, limit: int) -> list[dict[str, str]]:
    """
    테마 stocklist JSON → [{"name", "price", "change", "code"(있으면)}, ...]
    우선 nowPrice·prevChangeRate·upDownGb ([_naver_stocklist_stock_price_and_change]), 없으면 기존 필드 폴백.
    """
    rows: list[dict[str, str]] = []
    for item in _unwrap_json_array(payload):
        if not isinstance(item, dict):
            continue
        name = str(
            item.get("itemname")
            or item.get("itemName")
            or item.get("stockName")
            or item.get("stockNm")
            or item.get("name")
            or item.get("nm")
            or ""
        ).strip()
        if not name:
            continue
        price, change = _naver_stocklist_stock_price_and_change(item)
        code_raw = (
            item.get("itemCode")
            or item.get("itemcode")
            or item.get("stockCd")
            or item.get("stockCode")
            or item.get("isuCd")
            or item.get("isuSrtCd")
            or item.get("code")
            or item.get("stkCd")
            or item.get("itemCd")
        )
        code_str = ""
        if code_raw is not None and str(code_raw).strip() != "":
            digits = "".join(ch for ch in str(code_raw).strip() if ch.isdigit())
            if digits:
                code_str = digits.zfill(6) if len(digits) <= 6 else digits
        row: dict[str, str] = {
            "name": name,
            "price": price,
            "change": change,
        }
        if code_str:
            row["code"] = code_str
        rows.append(row)
        if len(rows) >= limit:
            break
    return rows


def crawl_naver_stock_themes() -> list[dict[str, Any]]:
    """
    네이버 증권 stock.naver.com API — 국내 테마 상위 NAVER_STOCK_THEME_MAX개 및 테마별 종목 최대 NAVER_STOCK_THEME_STOCKS_MAX개.
    반환: [{"theme_id", "theme_name", "change_rate", "description", "stocks": [...]}, ...]
    description: theme/{{no}}/info 의 categoryInfo (실패 시 빈 문자열, 파이프라인 계속).
    """
    try:
        raw_list = _fetch_stock_naver_json(NAVER_STOCK_THEME_LIST_API)
        if raw_list is None:
            return []
        logger.info("테마 목록 API 응답 앞 500자: %s", str(raw_list)[:500])

        metas = _naver_theme_list_from_api(raw_list)
        if not metas:
            logger.warning("crawl_naver_stock_themes: 테마 목록 파싱 결과 없음")
            return []
        metas = metas[:NAVER_STOCK_THEME_MAX]

        out: list[dict[str, Any]] = []
        for meta in metas:
            time.sleep(NAVER_STOCK_THEME_FETCH_SLEEP)
            theme_id = meta["theme_id"]
            theme_name = meta["theme_name"]
            change_rate = meta["change_rate"]
            stocks_url = (
                "https://stock.naver.com/api/domestic/market/theme/"
                f"{theme_id}/stocklist?marketType=ALL&orderType=priceTop&startIdx=0"
                f"&pageSize={NAVER_STOCK_THEME_STOCKS_MAX}"
            )
            raw_stocks = _fetch_stock_naver_json(stocks_url)
            stocks = (
                _naver_theme_stocks_from_api(raw_stocks, NAVER_STOCK_THEME_STOCKS_MAX)
                if raw_stocks is not None
                else []
            )
            time.sleep(NAVER_STOCK_THEME_FETCH_SLEEP)
            info_url = (
                "https://stock.naver.com/api/domestic/market/theme/"
                f"{theme_id}/info?marketType=ALL"
            )
            raw_info = _fetch_stock_naver_json(info_url)
            description = _naver_theme_category_info_from_payload(raw_info)
            out.append(
                {
                    "theme_id": theme_id,
                    "theme_name": theme_name,
                    "change_rate": change_rate,
                    "description": description,
                    "stocks": stocks[:NAVER_STOCK_THEME_STOCKS_MAX],
                }
            )
        return out
    except Exception as e:
        logger.exception("crawl_naver_stock_themes 실패: %s", e)
        return []


def _naver_upjong_stocks_from_api(payload: Any, limit: int) -> list[dict[str, str]]:
    """
    업종 종목 JSON → [{"name", "price", "change", "code"(있으면)}, ...]
    nowPrice(현재가), prevChangeRate(등락률), upDownGb(부호), itemname, itemcode
    """
    rows: list[dict[str, str]] = []
    for item in _unwrap_json_array(payload):
        if not isinstance(item, dict):
            continue
        name = str(
            item.get("itemname")
            or item.get("itemName")
            or item.get("stockName")
            or item.get("name")
            or ""
        ).strip()
        if not name:
            continue
        price, change = _naver_stocklist_stock_price_and_change(item)
        code_raw = (
            item.get("itemcode")
            or item.get("itemCode")
            or item.get("stockCd")
            or item.get("stockCode")
            or item.get("isuCd")
            or item.get("code")
        )
        code_str = ""
        if code_raw is not None and str(code_raw).strip() != "":
            digits = "".join(ch for ch in str(code_raw).strip() if ch.isdigit())
            if digits:
                code_str = digits.zfill(6) if len(digits) <= 6 else digits
        row: dict[str, str] = {
            "name": name,
            "price": price,
            "change": change,
        }
        if code_str:
            row["code"] = code_str
        rows.append(row)
        if len(rows) >= limit:
            break
    return rows


def crawl_naver_stock_upjong() -> list[dict[str, Any]]:
    """
    네이버 증권 국내 업종 상위 NAVER_STOCK_UPJONG_MAX개 및 업종별 종목 최대 NAVER_STOCK_UPJONG_STOCKS_MAX개.
    반환: [{"industry_id", "industry_name", "change_rate", "stocks": [...]}, ...]
    """
    try:
        raw_list = _fetch_stock_naver_json(NAVER_STOCK_UPJONG_LIST_API)
        if raw_list is None:
            return []
        logger.info("업종 목록 API 응답 앞 500자: %s", str(raw_list)[:500])

        metas = _naver_theme_list_from_api(raw_list)
        if not metas:
            logger.warning("crawl_naver_stock_upjong: 업종 목록 파싱 결과 없음")
            return []
        metas = metas[:NAVER_STOCK_UPJONG_MAX]

        out: list[dict[str, Any]] = []
        for meta in metas:
            time.sleep(NAVER_STOCK_UPJONG_FETCH_SLEEP)
            industry_id = meta["theme_id"]
            industry_name = meta["theme_name"]
            change_rate = meta["change_rate"]
            stocks_url = (
                "https://stock.naver.com/api/domestic/market/upjong/"
                f"{industry_id}/stocklist?marketType=ALL&orderType=priceTop&startIdx=0"
                f"&pageSize={NAVER_STOCK_UPJONG_STOCKS_MAX}"
            )
            raw_stocks = _fetch_stock_naver_json(stocks_url)
            stocks = (
                _naver_upjong_stocks_from_api(raw_stocks, NAVER_STOCK_UPJONG_STOCKS_MAX)
                if raw_stocks is not None
                else []
            )
            out.append(
                {
                    "industry_id": industry_id,
                    "industry_name": industry_name,
                    "change_rate": change_rate,
                    "stocks": stocks[:NAVER_STOCK_UPJONG_STOCKS_MAX],
                }
            )
        return out
    except Exception as e:
        logger.exception("crawl_naver_stock_upjong 실패: %s", e)
        return []


def dedupe_similar_titles_ordered(
    items: list[dict[str, Any]], min_ratio: float = 0.8
) -> list[dict[str, Any]]:
    """입력 순서를 유지하며, 이미 남긴 기사와 제목 유사도 min_ratio 이상이면 스킵."""
    kept: list[dict[str, Any]] = []
    for it in items:
        title = it.get("title") or ""
        is_dup = False
        for other in kept:
            otitle = other.get("title") or ""
            if not title or not otitle:
                continue
            ratio = difflib.SequenceMatcher(None, title.lower(), otitle.lower()).ratio()
            if ratio >= min_ratio:
                is_dup = True
                break
        if not is_dup:
            kept.append(it)
    return kept


def dedupe_items(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """URL 기준 중복 제거 후, 제목 유사도 80% 이상 difflib 중복 제거."""
    by_url: dict[str, dict[str, Any]] = {}
    for it in items:
        u = (it.get("url") or "").strip()
        if not u:
            continue
        by_url[u] = it
    unique = list(by_url.values())
    kept: list[dict[str, Any]] = []
    for it in unique:
        title = it.get("title") or ""
        is_dup = False
        for other in kept:
            otitle = other.get("title") or ""
            if not title or not otitle:
                continue
            ratio = difflib.SequenceMatcher(None, title.lower(), otitle.lower()).ratio()
            if ratio >= 0.8:
                is_dup = True
                break
        if not is_dup:
            kept.append(it)
    return kept


def crawl_domestic() -> dict[str, list[dict[str, Any]]]:
    """실시간 / 많이 본 / 주요: 네이버 금융만 (토스증권 `crawl_tossinvest_news` 는 미사용)."""
    return {
        "realtime": crawl_naver_realtime(),
        "popular": crawl_naver_ranked(),
        "main": crawl_naver_main(),
    }


def crawl_market_indicators() -> list[dict[str, Any]]:
    """국내 증시 요약 페이지에서 지표 일부 파싱 (실패 시 빈 리스트)."""
    url = "https://finance.naver.com/sise/sise_index.naver?code=KOSPI"
    out: list[dict[str, Any]] = []
    try:
        html = _fetch(url)
        if not html:
            return out
        soup = BeautifulSoup(html, "lxml")
        # 코스피 현재가 영역 (마크업 변경 시 빈 값)
        now = soup.select_one("#now_value")
        change = soup.select_one("#change_value_and_rate")
        if now:
            name = "코스피"
            val = now.get_text(strip=True)
            ch = change.get_text(" ", strip=True) if change else ""
            direction = "FLAT"
            if "▲" in ch or "상승" in ch or (ch and ch.strip().startswith("+")):
                direction = "UP"
            elif "▼" in ch or "하락" in ch or (ch and ch.strip().startswith("-")):
                direction = "DOWN"
            out.append(
                {
                    "name": name,
                    "value": val,
                    "change": ch or "-",
                    "direction": direction,
                }
            )
    except Exception as e:
        logger.warning("crawl_market_indicators: %s", e)
    return out


# ---------------------------------------------------------------------------
# 종목별 재무·증권가 크롤링 (네이버 국내 / Yahoo 해외)
# ---------------------------------------------------------------------------

DOMESTIC_STOCKS: list[tuple[str, str]] = [
    ("005930", "삼성전자"),
    ("000660", "SK하이닉스"),
    ("035420", "NAVER"),
    ("051910", "LG화학"),
]

OVERSEAS_TICKERS: list[str] = ["NVDA", "AAPL", "TSLA"]


def _yahoo_symbol(code: str) -> str:
    if code.isdigit() and len(code) == 6:
        return f"{code.zfill(6)}.KS"
    return code.upper()


def _raw_field(node: Any) -> tuple[Any, str]:
    """Yahoo 숫자 노드 {raw, fmt} → (raw, fmt 문자열)."""
    if node is None:
        return None, ""
    if isinstance(node, dict):
        r = node.get("raw")
        f = node.get("fmt") or (str(r) if r is not None else "")
        return r, f
    return node, str(node)


def _yahoo_quote_summary(ticker: str) -> dict[str, Any] | None:
    modules = (
        "incomeStatementHistory,balanceSheetHistory,summaryProfile,"
        "defaultKeyStatistics,financialData,recommendationTrend"
    )
    url = f"https://query1.finance.yahoo.com/v10/finance/quoteSummary/{ticker}?modules={modules}"
    raw = _fetch(url)
    if not raw:
        return None
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return None
    qs = data.get("quoteSummary") or {}
    if qs.get("error"):
        logger.warning("Yahoo error %s: %s", ticker, qs["error"])
        return None
    res = qs.get("result") or []
    return res[0] if res else None


def _empty_financials() -> dict[str, Any]:
    return {
        "revenue": [],
        "operating_income": [],
        "net_income": [],
        "eps": [],
        "per": [],
        "roe": [],
    }


def _year_from_stmt(stmt: dict[str, Any]) -> int | None:
    ed = stmt.get("endDate") or {}
    if isinstance(ed, dict):
        fmt = ed.get("fmt") or ""
        m = re.search(r"(20\d{2})", fmt)
        if m:
            return int(m.group(1))
    return None


def _stmt_series_field(
    statements: list[dict[str, Any]], field: str, max_n: int = 4
) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for stmt in statements[:max_n]:
        y = _year_from_stmt(stmt)
        if y is None:
            continue
        node = stmt.get(field)
        raw_v, fmt_v = _raw_field(node)
        out.append({"year": y, "value": fmt_v or (str(raw_v) if raw_v is not None else ""), "raw": raw_v})
    return out


def crawl_yahoo_financial_bundle(ticker: str) -> dict[str, Any]:
    """해외 티커 또는 .KS — 재무·건전성·증권가 요약."""
    sym = _yahoo_symbol(ticker)
    r0 = _yahoo_quote_summary(sym)
    financials = _empty_financials()
    health: dict[str, Any] = {}
    analyst: dict[str, Any] = {
        "target_price": "",
        "buy_ratio": 0,
        "neutral_ratio": 0,
        "sell_ratio": 0,
        "comments": [],
    }
    name = ticker

    if not r0:
        return {
            "ticker": ticker,
            "name": name,
            "financials": financials,
            "health": health,
            "analyst": analyst,
            "profile": {"name": name, "sector": "", "ceo": "", "market_cap": ""},
        }

    sp = r0.get("summaryProfile") or {}
    sector = ""
    if isinstance(sp, dict):
        name = sp.get("longName") or sp.get("shortName") or name
        sector = sp.get("sector", "") or ""

    ish = (r0.get("incomeStatementHistory") or {}).get("incomeStatementHistory") or []
    ish = sorted(ish, key=lambda s: _year_from_stmt(s) or 0, reverse=True)

    financials["revenue"] = _stmt_series_field(ish, "totalRevenue")
    financials["operating_income"] = _stmt_series_field(ish, "operatingIncome")
    if not financials["operating_income"]:
        financials["operating_income"] = _stmt_series_field(ish, "totalOperatingIncome")
    financials["net_income"] = _stmt_series_field(ish, "netIncome")
    financials["eps"] = _stmt_series_field(ish, "dilutedEPS")
    if not financials["eps"]:
        financials["eps"] = _stmt_series_field(ish, "basicEPS")

    dks = r0.get("defaultKeyStatistics") or {}
    fd = r0.get("financialData") or {}
    pe_fmt, pe_raw = _raw_field(dks.get("trailingPE"))
    roe_fmt, roe_raw = _raw_field(fd.get("returnOnEquity"))

    years = [x["year"] for x in financials["revenue"][:4]] if financials["revenue"] else []
    if not years and ish:
        years = [y for y in (_year_from_stmt(s) for s in ish[:4]) if y]
    for y in years[:4]:
        financials["per"].append({"year": y, "value": str(pe_fmt or ""), "raw": pe_raw})
        financials["roe"].append({"year": y, "value": str(roe_fmt or ""), "raw": roe_raw})

    bsh = (r0.get("balanceSheetHistory") or {}).get("balanceSheetHistory") or []
    latest_bs = bsh[0] if bsh else {}
    td, _ = _raw_field(latest_bs.get("totalDebt"))
    eq, _ = _raw_field(latest_bs.get("totalStockholderEquity"))
    ta, _ = _raw_field(latest_bs.get("totalAssets"))
    if eq and float(eq) != 0 and td is not None:
        health["debt_ratio"] = round(float(td) / float(eq) * 100, 2)
    cr, _ = _raw_field(fd.get("currentRatio"))
    if cr is not None:
        health["current_ratio"] = round(float(cr), 2)

    tc, tc_fmt = _raw_field(fd.get("totalCash"))
    health["cash_holding"] = tc_fmt or (str(tc) if tc is not None else "")
    if ta and eq and float(ta) != 0:
        health["equity_ratio"] = round(float(eq or 0) / float(ta) * 100, 2)
    else:
        health["equity_ratio"] = 0.0
    ebitda, _ = _raw_field(fd.get("ebitda"))
    interest_exp, _ = _raw_field(fd.get("interestExpense"))
    if interest_exp and float(interest_exp) != 0 and ebitda is not None:
        health["interest_coverage"] = round(abs(float(ebitda)) / abs(float(interest_exp)), 2)
    else:
        health["interest_coverage"] = 0.0

    rt = (r0.get("recommendationTrend") or {}).get("trend") or []
    if rt:
        t0 = rt[0]
        sb = int(t0.get("strongBuy") or 0)
        b = int(t0.get("buy") or 0)
        h = int(t0.get("hold") or 0)
        s = int(t0.get("sell") or 0)
        ss = int(t0.get("strongSell") or 0)
        tot = sb + b + h + s + ss
        if tot > 0:
            analyst["buy_ratio"] = round((sb + b) / tot * 100)
            analyst["neutral_ratio"] = round(h / tot * 100)
            analyst["sell_ratio"] = round((s + ss) / tot * 100)
    tp_node = fd.get("targetMeanPrice") or fd.get("targetMedianPrice")
    tp, tp_fmt = _raw_field(tp_node)
    if tp_fmt:
        analyst["target_price"] = tp_fmt
    elif isinstance(tp, (int, float)):
        analyst["target_price"] = str(int(tp)) if float(tp) == int(tp) else str(tp)
    else:
        analyst["target_price"] = ""

    return {
        "ticker": ticker,
        "name": name,
        "financials": financials,
        "health": health,
        "analyst": analyst,
        "profile": {
            "name": name,
            "sector": sector,
            "ceo": "",
            "market_cap": "",
        },
    }


def _naver_find_row_values(soup: BeautifulSoup, label_keywords: tuple[str, ...]) -> list[str]:
    for tr in soup.find_all("tr"):
        cells = tr.find_all(["th", "td"])
        if not cells:
            continue
        first = cells[0].get_text(" ", strip=True).replace("\n", "")
        for kw in label_keywords:
            if kw in first.replace(" ", ""):
                return [c.get_text(" ", strip=True) for c in cells[1:]]
    return []


def _naver_financials_from_main(html: str) -> dict[str, Any]:
    soup = BeautifulSoup(html, "lxml")
    financials = _empty_financials()
    label_map = [
        ("revenue", ("매출액",)),
        ("operating_income", ("영업이익",)),
        ("net_income", ("당기순이익", "순이익")),
        ("eps", ("EPS", "주당순이익")),
        ("per", ("PER",)),
        ("roe", ("ROE",)),
    ]
    for table in soup.select("table.tb_type1"):
        thead = table.find("thead")
        years: list[int] = []
        if thead:
            for th in thead.find_all("th")[1:]:
                m = re.search(r"(20\d{2})", th.get_text(strip=True))
                if m:
                    years.append(int(m.group(1)))
        if len(years) < 2:
            continue
        years = years[:4]
        table_soup = BeautifulSoup(str(table), "lxml")
        for key, kws in label_map:
            vals = _naver_find_row_values(table_soup, kws)
            if not vals:
                continue
            n = min(len(vals), len(years), 4)
            for i in range(n):
                y = years[i]
                raw_txt = vals[i]
                raw_num: Any = raw_txt
                try:
                    raw_num = float(re.sub(r"[^\d.\-]", "", raw_txt.replace(",", "")))
                except ValueError:
                    pass
                financials[key].append({"year": y, "value": raw_txt, "raw": raw_num})
        if financials["revenue"]:
            break
    return financials


def _naver_health_from_coinfo(html: str) -> dict[str, Any]:
    soup = BeautifulSoup(html, "lxml")
    health: dict[str, Any] = {}

    def pick_float(labels: tuple[str, ...]) -> float | None:
        vals = _naver_find_row_values(soup, labels)
        if not vals:
            return None
        txt = vals[0]
        m = re.search(r"([\d,\.]+)", txt.replace(",", ""))
        if not m:
            return None
        try:
            return float(m.group(1).replace(",", ""))
        except ValueError:
            return None

    dr = pick_float(("부채비율",))
    if dr is not None:
        health["debt_ratio"] = dr
    cr = pick_float(("유동비율",))
    if cr is not None:
        health["current_ratio"] = cr
    er = pick_float(("자기자본비율",))
    if er is not None:
        health["equity_ratio"] = er
    vals = _naver_find_row_values(soup, ("현금및현금성자산", "현금", "보유현금"))
    if vals:
        health["cash_holding"] = vals[0]
    ic = pick_float(("이자보상배율",))
    if ic is not None:
        health["interest_coverage"] = ic
    return health


def _naver_company_basic_from_main(html: str) -> dict[str, Any]:
    soup = BeautifulSoup(html, "lxml")
    profile: dict[str, Any] = {"name": "", "sector": "", "ceo": "", "market_cap": ""}
    name_el = soup.select_one("div.wrap_company h2 a") or soup.select_one("div.wrap_company h2")
    if name_el:
        profile["name"] = name_el.get_text(strip=True)
    for dl in soup.find_all("dl"):
        dts = dl.find_all("dt")
        dds = dl.find_all("dd")
        for dt, dd in zip(dts, dds):
            k = dt.get_text(" ", strip=True)
            v = dd.get_text(" ", strip=True)
            if "업종" in k:
                profile["sector"] = v
            if "대표" in k or "CEO" in k.upper():
                profile["ceo"] = v
            if "시가총액" in k or "시총" in k:
                profile["market_cap"] = v
    return profile


def _naver_analyst_page(code: str) -> dict[str, Any]:
    out: dict[str, Any] = {
        "target_price": "",
        "buy_ratio": 0,
        "neutral_ratio": 0,
        "sell_ratio": 0,
        "comments": [],
    }
    url = f"{NAVER_BASE}/item/analyst.naver?code={code}"
    html = _fetch(url)
    if not html:
        return out
    soup = BeautifulSoup(html, "lxml")
    for em in soup.find_all("em"):
        t = em.get_text(strip=True)
        if "목표" in t or "컨센서스" in t or "평균" in t:
            parent = em.find_parent()
            if parent:
                tx = parent.get_text(" ", strip=True)
                m = re.search(r"([\d,]+)\s*원", tx)
                if m:
                    out["target_price"] = m.group(1)
                    break
    txt_blob = soup.get_text("\n", strip=True)
    m_buy = re.search(r"매수\s*[:\s]*(\d+)\s*%", txt_blob)
    m_hold = re.search(r"중립\s*[:\s]*(\d+)\s*%", txt_blob)
    m_sell = re.search(r"매도\s*[:\s]*(\d+)\s*%", txt_blob)
    if m_buy and m_hold and m_sell:
        br, nr, sr = int(m_buy.group(1)), int(m_hold.group(1)), int(m_sell.group(1))
        tot = br + nr + sr
        if tot > 0:
            out["buy_ratio"] = round(br / tot * 100)
            out["neutral_ratio"] = round(nr / tot * 100)
            out["sell_ratio"] = round(sr / tot * 100)
    comments: list[dict[str, str]] = []
    for table in soup.select("table.type5, table.tb_type1"):
        for tr in table.find_all("tr"):
            cells = tr.find_all("td")
            if len(cells) < 3:
                continue
            src = cells[0].get_text(strip=True)
            date = cells[1].get_text(strip=True) if len(cells) > 1 else ""
            body = cells[-1].get_text(" ", strip=True)
            if len(body) > 30 and src:
                comments.append({"source": src, "date": date, "comment": body[:800]})
            if len(comments) >= 3:
                break
        if len(comments) >= 3:
            break
    out["comments"] = comments[:3]
    return out


def crawl_domestic_stock(code: str, default_name: str = "") -> dict[str, Any]:
    """국내 종목: 네이버 + 실패 시 Yahoo .KS 보조."""
    financials = _empty_financials()
    health: dict[str, Any] = {}
    analyst: dict[str, Any] = {
        "target_price": "",
        "buy_ratio": 0,
        "neutral_ratio": 0,
        "sell_ratio": 0,
        "comments": [],
    }
    profile = {"name": default_name, "sector": "", "ceo": "", "market_cap": ""}

    try:
        main_html = _fetch(f"{NAVER_BASE}/item/main.naver?code={code}")
        if main_html:
            financials = _naver_financials_from_main(main_html)
            profile.update(_naver_company_basic_from_main(main_html))
    except Exception as e:
        logger.warning("네이버 main 재무 %s: %s", code, e)

    try:
        coinfo_html = _fetch(f"{NAVER_BASE}/item/coinfo.naver?code={code}")
        if coinfo_html:
            h2 = _naver_health_from_coinfo(coinfo_html)
            health.update(h2)
    except Exception as e:
        logger.warning("네이버 coinfo %s: %s", code, e)

    try:
        analyst = _naver_analyst_page(code)
    except Exception as e:
        logger.warning("네이버 analyst %s: %s", code, e)

    if not financials["revenue"]:
        yb = crawl_yahoo_financial_bundle(code)
        financials = yb.get("financials") or financials
        if not health:
            health = yb.get("health") or health
        if profile.get("name") in ("", default_name):
            profile["name"] = yb.get("name", profile.get("name", default_name))
        yp = yb.get("profile") or {}
        for k, v in yp.items():
            if v and not profile.get(k):
                profile[k] = str(v)
        if not analyst.get("target_price") and yb.get("analyst"):
            for k in ("target_price", "buy_ratio", "neutral_ratio", "sell_ratio"):
                if k in yb["analyst"] and yb["analyst"][k]:
                    analyst[k] = yb["analyst"][k]

    name = profile.get("name") or default_name or code
    return {
        "ticker": code,
        "name": name,
        "profile": profile,
        "financials": financials,
        "health": health,
        "analyst": analyst,
    }


def crawl_overseas_stock(ticker: str) -> dict[str, Any]:
    """해외 종목 Yahoo quoteSummary."""
    bundle = crawl_yahoo_financial_bundle(ticker)
    bundle["profile"] = bundle.get("profile") or {
        "name": bundle.get("name", ticker),
        "sector": "",
        "ceo": "",
        "market_cap": "",
    }
    return bundle


def crawl_all_company_bundles() -> list[dict[str, Any]]:
    """국내 4 + 해외 3 종목 전체 묶음."""
    out: list[dict[str, Any]] = []
    for code, default_name in DOMESTIC_STOCKS:
        try:
            out.append(crawl_domestic_stock(code, default_name))
        except Exception as e:
            logger.exception("국내 종목 크롤 실패 %s: %s", code, e)
    for t in OVERSEAS_TICKERS:
        try:
            out.append(crawl_overseas_stock(t))
        except Exception as e:
            logger.exception("해외 종목 크롤 실패 %s: %s", t, e)
    return out


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "debug-naver-main":
        _debug_naver_mainnews_list_all_links()
    else:
        print(
            "Usage: python crawler.py debug-naver-main",
            file=sys.stderr,
        )
