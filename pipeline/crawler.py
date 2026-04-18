"""
네이버 금융 / Google·Yahoo RSS 뉴스 크롤러 + 종목 재무·증권가 크롤링.
"""
from __future__ import annotations

import difflib
import json
import logging
import re
from typing import Any
from urllib.parse import urljoin, urlparse

import feedparser
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
REQUEST_TIMEOUT = 20
MAX_PER_SECTION = 10
SUMMARY_CHARS = 200


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


def _extract_naver_article_body(html: str) -> str:
    soup = BeautifulSoup(html, "lxml")
    for sel in (
        "#newsct_article",
        "#dic_area",
        "#articeBody",
        "div.newsct_article",
        "div.articleCont",
    ):
        node = soup.select_one(sel)
        if node:
            text = node.get_text(separator=" ", strip=True)
            if len(text) > 50:
                return text
    return ""


def _item_from_naver_article(
    title: str,
    article_url: str,
    source: str,
    published_hint: str = "",
    category: str = "",
    rank: int | None = None,
) -> dict[str, Any] | None:
    html = _fetch(article_url)
    if not html:
        return None
    body = _extract_naver_article_body(html)
    if not body and title:
        body = title
    summary = (body[:SUMMARY_CHARS] + ("…" if len(body) > SUMMARY_CHARS else "")).strip()
    t = title.strip()
    if rank is not None:
        t = f"[{rank}] {t}"
    return {
        "title": t,
        "summary": summary,
        "source": source,
        "url": _normalize_url(article_url),
        "published_at": published_hint,
        "category": category or "",
    }


def _collect_naver_links_from_list(html: str, max_n: int) -> list[tuple[str, str, str]]:
    """(title, href, date_hint) 리스트."""
    soup = BeautifulSoup(html, "lxml")
    out: list[tuple[str, str, str]] = []
    seen: set[str] = set()

    for a in soup.find_all("a", href=True):
        href = a.get("href", "")
        full = urljoin(NAVER_BASE, href)
        if "news_read.naver" not in full:
            continue
        norm = _normalize_url(full)
        if norm in seen:
            continue
        title = a.get_text(strip=True)
        if not title or len(title) < 4:
            continue
        # 날짜: 인접 td 등 (있으면)
        date_hint = ""
        parent_tr = a.find_parent("tr")
        if parent_tr:
            for td in parent_tr.find_all("td"):
                tx = td.get_text(strip=True)
                if re.match(r"^\d{4}\.\d{2}\.\d{2}", tx) or re.match(r"^\d{2}:\d{2}$", tx):
                    date_hint = tx
                    break
        seen.add(norm)
        out.append((title, full, date_hint))
        if len(out) >= max_n:
            break
    return out


def crawl_naver_realtime() -> list[dict[str, Any]]:
    url = (
        "https://finance.naver.com/news/news_list.naver?"
        "mode=LSS2D&section_id=101&section_id2=258"
    )
    items: list[dict[str, Any]] = []
    try:
        html = _fetch(url)
        if not html:
            return items
        for title, href, date_hint in _collect_naver_links_from_list(html, MAX_PER_SECTION):
            row = _item_from_naver_article(
                title, href, source="네이버금융", published_hint=date_hint
            )
            if row:
                items.append(row)
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
        links = _collect_naver_links_from_list(html, MAX_PER_SECTION * 2)
        for rank, (title, href, date_hint) in enumerate(links[:MAX_PER_SECTION], start=1):
            row = _item_from_naver_article(
                title,
                href,
                source="네이버금융",
                published_hint=date_hint,
                rank=rank,
            )
            if row:
                items.append(row)
    except Exception as e:
        logger.exception("crawl_naver_ranked 실패: %s", e)
    return items


def crawl_naver_main() -> list[dict[str, Any]]:
    url = "https://finance.naver.com/news/mainnews.naver"
    items: list[dict[str, Any]] = []
    try:
        html = _fetch(url)
        if not html:
            return items
        soup = BeautifulSoup(html, "lxml")
        seen: set[str] = set()
        for a in soup.find_all("a", href=True):
            if len(items) >= MAX_PER_SECTION:
                break
            href = a.get("href", "")
            full = urljoin(NAVER_BASE, href)
            if "news_read.naver" not in full:
                continue
            norm = _normalize_url(full)
            if norm in seen:
                continue
            title = a.get_text(strip=True)
            if not title or len(title) < 4:
                continue
            seen.add(norm)
            row = _item_from_naver_article(title, full, source="네이버금융")
            if row:
                items.append(row)
    except Exception as e:
        logger.exception("crawl_naver_main 실패: %s", e)
    return items


GOOGLE_FEEDS = [
    ("Google Finance", "https://news.google.com/rss/search?q=stock+market&hl=en-US&gl=US&ceid=US:en"),
    ("Google Finance", "https://news.google.com/rss/search?q=nvidia+apple+meta+google&hl=en-US&gl=US&ceid=US:en"),
    ("Google Finance", "https://news.google.com/rss/search?q=fed+interest+rate+inflation&hl=en-US&gl=US&ceid=US:en"),
]

YAHOO_FEED = "https://feeds.finance.yahoo.com/rss/2.0/headline"


def _published_str(entry: Any) -> str:
    if getattr(entry, "published", None):
        return str(entry.published)
    if getattr(entry, "updated", None):
        return str(entry.updated)
    return ""


def _feed_to_items(feed_url: str, source_label: str, max_n: int) -> list[dict[str, Any]]:
    items: list[dict[str, Any]] = []
    try:
        raw = _fetch(feed_url)
        if not raw:
            return items
        parsed = feedparser.parse(raw)
        for entry in parsed.entries[:max_n]:
            title = getattr(entry, "title", "") or ""
            link = getattr(entry, "link", "") or ""
            if not title or not link:
                continue
            summary_src = ""
            if getattr(entry, "summary", None):
                soup = BeautifulSoup(entry.summary, "lxml")
                summary_src = soup.get_text(separator=" ", strip=True)
            if not summary_src and getattr(entry, "description", None):
                soup = BeautifulSoup(entry.description, "lxml")
                summary_src = soup.get_text(separator=" ", strip=True)
            summary = (summary_src[:SUMMARY_CHARS] + ("…" if len(summary_src) > SUMMARY_CHARS else "")).strip()
            items.append(
                {
                    "title": title.strip(),
                    "summary": summary or title[:SUMMARY_CHARS],
                    "source": source_label,
                    "url": _normalize_url(link),
                    "published_at": _published_str(entry),
                    "category": "",
                }
            )
    except Exception as e:
        logger.warning("_feed_to_items 실패 %s: %s", feed_url, e)
    return items


def crawl_google_finance_rss() -> list[dict[str, Any]]:
    all_items: list[dict[str, Any]] = []
    try:
        for label, feed_url in GOOGLE_FEEDS:
            all_items.extend(_feed_to_items(feed_url, label, MAX_PER_SECTION))
    except Exception as e:
        logger.exception("crawl_google_finance_rss 실패: %s", e)
    return all_items


def crawl_yahoo_rss() -> list[dict[str, Any]]:
    try:
        return _feed_to_items(YAHOO_FEED, "Yahoo Finance", MAX_PER_SECTION)
    except Exception as e:
        logger.exception("crawl_yahoo_rss 실패: %s", e)
        return []


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
    """실시간 / 많이 본 / 주요 네이버 뉴스."""
    return {
        "realtime": crawl_naver_realtime(),
        "popular": crawl_naver_ranked(),
        "main": crawl_naver_main(),
    }


def crawl_international() -> list[dict[str, Any]]:
    """Google + Yahoo RSS 병합 (dedupe 전)."""
    merged: list[dict[str, Any]] = []
    merged.extend(crawl_google_finance_rss())
    merged.extend(crawl_yahoo_rss())
    return dedupe_items(merged)


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
