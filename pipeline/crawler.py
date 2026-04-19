"""
네이버 금융 / Yahoo Finance(HTML) 뉴스 크롤러 + 종목 재무·증권가 크롤링.
(토스증권 Playwright `crawl_tossinvest_news` 는 보존. `crawl_domestic` 에서는 미사용.)
"""
from __future__ import annotations

import difflib
import json
import logging
import os
import re
import sys
import time
from datetime import datetime, timedelta
from typing import Any
from urllib.parse import urljoin, urlparse

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


def _fetch_naver_article_body(article: dict[str, Any]) -> str:
    """
    네이버(금융/뉴스) 기사 본문 텍스트 추출. 실패 시 빈 문자열.
    기사 dict의 ``url`` 값만 그대로 HTTP 요청에 사용한다(재조합·정규화 없음).
    """
    u = str(article.get("url") or "").strip()
    if not u:
        return ""
    html = _fetch(u)
    if not html:
        return ""
    try:
        soup = BeautifulSoup(html, "lxml")
        for sel in ("#dic_area", "div.article_body", "#articeBody", "#articleBody"):
            el = soup.select_one(sel)
            if el:
                t = _element_to_plain_article_text(el)
                if t:
                    return t[:ARTICLE_BODY_MAX_CHARS]
        return ""
    except Exception as e:
        logger.debug("naver article body 실패 %s: %s", u[:80], e)
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


def _first_img_src_in(li) -> str:
    for img in li.find_all("img", src=True):
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


def _parse_yahoo_finance_story_item(li) -> dict[str, Any] | None:
    """
    li.stream-item.story-item — section[data-testid=storyitem], a.titles, h3.clamp, div.publishing, img
    """
    section = li.select_one('section[data-testid="storyitem"]') or li.select_one(
        'section[data-testid="StoryItem"]',
    )
    if not section:
        return None
    a = None
    for cand in section.select("a[href]"):
        classes = cand.get("class") or []
        if isinstance(classes, str):
            classes = classes.split()
        if "titles" in classes:
            a = cand
            break
    if a is None:
        return None
    href = (a.get("href") or "").strip()
    url = _resolve_yahoo_href(href)
    if not url:
        return None
    h3 = section.select_one("h3.clamp") or li.select_one("h3.clamp")
    title = ""
    if h3 and h3.get_text(strip=True):
        title = h3.get_text(strip=True)
    if not title and a.get("title"):
        title = a.get("title", "").strip()
    if not title:
        title = a.get_text(strip=True) or ""
    if len(title) < 2:
        return None

    pub = section.select_one("div.publishing") or li.select_one("div.publishing")
    pub_t = pub.get_text(" ", strip=True) if pub else ""
    source, date_hint = _parse_yahoo_publishing_line(pub_t)
    if not source and pub_t:
        source = pub_t
    if not source:
        source = "Yahoo Finance"

    thumb = _first_img_src_in(li)
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
    }


def _crawl_yahoo_finance_list_url(page_url: str, max_n: int) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    try:
        html = _fetch(page_url)
        if not html:
            return out
        soup = BeautifulSoup(html, "lxml")
        seen: set[str] = set()
        for li in soup.select("li.stream-item.story-item"):
            row = _parse_yahoo_finance_story_item(li)
            if not row:
                continue
            u = (row.get("url") or "").strip()
            if not u or u in seen:
                continue
            seen.add(u)
            out.append(row)
            if len(out) >= max_n:
                break
        _attach_yahoo_article_bodies(out)
    except Exception as e:
        logger.warning("_crawl_yahoo_finance_list_url %s: %s", page_url, e)
    return out


def crawl_yahoo_stocks() -> list[dict[str, Any]]:
    """
    Yahoo Finance 뉴스 메인 스트림 (요청이 차단될 수 있어 빈 리스트일 수 있음).
    https://finance.yahoo.com/news/
    """
    u = f"{YAHOO_FINANCE}/news/"
    try:
        return _crawl_yahoo_finance_list_url(u, YAHOO_MAX_LIST_ITEMS)
    except Exception as e:
        logger.exception("crawl_yahoo_stocks 실패: %s", e)
        return []


def crawl_yahoo_tech() -> list[dict[str, Any]]:
    """
    Tech 토픽 뉴스 스트림.
    https://finance.yahoo.com/topic/tech/
    """
    u = f"{YAHOO_FINANCE}/topic/tech/"
    try:
        return _crawl_yahoo_finance_list_url(u, YAHOO_MAX_LIST_ITEMS)
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
        API 최상위: [{ paperNumber, newspaperOfficeMain: [ { articleId, title, ... } ] }, ...]
        기사 URL: https://n.news.naver.com/article/{officeId}/{articleId}
        """
        if not isinstance(payload, list):
            return []

        out: list[dict[str, Any]] = []
        for section in payload:
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
                office_id = str(art.get("officeId") or "").strip()
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
            return rows
    return []


def crawl_hankyung_newspaper() -> list[dict[str, Any]]:
    """NAVER 뉴스 — 한국경제 신문 스탠드 (최대 30). 파이프라인은 면·면 내 순 정렬 후 상위 5건만 briefing/hankyung에 저장."""
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
    """NAVER 뉴스 — 매일경제 신문 스탠드 (최대 30). 파이프라인은 면·면 내 순 정렬 후 상위 5건만 briefing/maeil에 저장."""
    try:
        return _crawl_naver_newspaper(
            "009",
            "매일경제",
            MAX_NEWSPAPER_ITEMS,
        )
    except Exception as e:
        logger.exception("crawl_maeil_newspaper 실패: %s", e)
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
