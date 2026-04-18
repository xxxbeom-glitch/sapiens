import requests
from bs4 import BeautifulSoup
import firebase_admin
from firebase_admin import credentials, firestore
import json
import base64
import os
import time
from datetime import datetime

def init_firebase():
    if not firebase_admin._apps:
        b64 = os.environ.get("FIREBASE_SERVICE_ACCOUNT_B64")
        if b64:
            sa = json.loads(base64.b64decode(b64).decode())
            cred = credentials.Certificate(sa)
        else:
            cred = credentials.Certificate("/workspace/sapiens/pipeline/firebase_key.json")
        firebase_admin.initialize_app(cred)
    return firestore.client()

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
    "Referer": "https://finance.daum.net/",
}

COMPANIES = {
    "005930": "삼성전자",
    "000660": "SK하이닉스",
    "005380": "현대차",
}

def fetch_html(url):
    try:
        res = requests.get(url, headers=HEADERS, timeout=10)
        res.encoding = "utf-8"
        return BeautifulSoup(res.text, "html.parser")
    except Exception as e:
        print(f"  fetch 실패: {url} - {e}")
        return None

def crawl_company_info(code):
    url = f"https://wisefn.finance.daum.net/v1/company/c1020001.aspx?cmp_cd={code}"
    soup = fetch_html(url)
    if not soup:
        return {}
    info = {}
    try:
        rows = soup.select("table#cTB201 tr")
        for row in rows:
            ths = row.select("th")
            tds = row.select("td")
            for i, th in enumerate(ths):
                key = th.get_text(strip=True)
                val = tds[i].get_text(strip=True) if i < len(tds) else ""
                if key:
                    info[key] = val
    except Exception as e:
        print(f"  기업정보 파싱 오류: {e}")
    return info

def crawl_financials(code):
    url = f"https://wisefn.finance.daum.net/v1/company/c1030001_1.aspx?cmp_cd={code}"
    soup = fetch_html(url)
    if not soup:
        return {}
    result = {}
    try:
        tables = soup.select("table")
        for t in tables:
            headers = [th.get_text(strip=True) for th in t.select("thead th")]
            if "매출액" in str(headers) or "영업이익" in str(headers):
                rows = t.select("tbody tr")
                for row in rows:
                    th = row.find("th")
                    tds = row.select("td span.num")
                    if th and tds:
                        label = th.get_text(strip=True)
                        values = [td.get_text(strip=True).replace(",", "") for td in tds]
                        result[label] = values
                break
    except Exception as e:
        print(f"  재무분석 파싱 오류: {e}")
    return result

def crawl_reports(code):
    url = f"https://wisefn.finance.daum.net/v1/company/reports.aspx?cmp_cd={code}"
    soup = fetch_html(url)
    if not soup:
        return {}
    reports = []
    buy_count = 0
    neutral_count = 0
    sell_count = 0
    target_prices = []
    try:
        rows = soup.select("tr[onmouseover]")
        for row in rows:
            date_td = row.select_one("td.col1")
            price_td = row.select_one("td.col2")
            opinion_td = row.select_one("td.col4 div[title]")
            firm_td = row.select_one("td.col5")
            report_td = row.select_one("td.col6")
            date = date_td.get_text(strip=True) if date_td else ""
            price = price_td.get_text(strip=True).replace(",", "") if price_td else ""
            opinion = opinion_td.get("title", "").strip() if opinion_td else ""
            firm = firm_td.get_text(strip=True) if firm_td else ""
            title = report_td.get("data-title", "").strip() if report_td else ""
            comment = report_td.get("data-comment", "").strip() if report_td else ""
            if not date:
                continue
            op_lower = opinion.lower()
            if any(x in op_lower for x in ["buy", "매수", "강력매수"]):
                buy_count += 1
            elif any(x in op_lower for x in ["neutral", "중립", "hold"]):
                neutral_count += 1
            elif any(x in op_lower for x in ["sell", "매도"]):
                sell_count += 1
            if price and price.isdigit():
                target_prices.append(int(price))
            reports.append({
                "date": date,
                "targetPrice": int(price) if price and price.isdigit() else 0,
                "opinion": opinion,
                "firm": firm,
                "title": title,
                "comment": comment,
            })
    except Exception as e:
        print(f"  리포트 파싱 오류: {e}")
    total = buy_count + neutral_count + sell_count
    avg_target = int(sum(target_prices) / len(target_prices)) if target_prices else 0
    return {
        "reports": reports[:5],
        "consensus": {
            "buy": round(buy_count / total * 100) if total else 0,
            "neutral": round(neutral_count / total * 100) if total else 0,
            "sell": round(sell_count / total * 100) if total else 0,
            "targetPrice": avg_target,
            "total": total,
        }
    }

def save_to_firebase(db, code, name, data):
    ref = db.collection("stocks").document(code)
    ref.set({
        "code": code,
        "name": name,
        "updatedAt": datetime.now().isoformat(),
        **data
    }, merge=True)
    print(f"  Firebase 저장 완료: {name} ({code})")

def main():
    print("=== 기업정보 크롤러 시작 ===")
    db = init_firebase()
    for code, name in COMPANIES.items():
        print(f"\n[{name} ({code})] 크롤링 중...")
        info = crawl_company_info(code)
        print(f"  기업정보 {len(info)}개 항목")
        financials = crawl_financials(code)
        print(f"  재무분석 {len(financials)}개 항목")
        reports_data = crawl_reports(code)
        print(f"  리포트 {len(reports_data.get('reports', []))}개")
        print(f"  컨센서스: {reports_data.get('consensus', {})}")
        save_to_firebase(db, code, name, {
            "info": info,
            "financials": financials,
            "reports": reports_data.get("reports", []),
            "consensus": reports_data.get("consensus", {}),
        })
        time.sleep(1)
    print("\n=== 완료 ===")

if __name__ == "__main__":
    main()