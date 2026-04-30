"""
기존 summarizer.py 하단에 추가할 카드 생성 함수들.
기존 코드는 그대로 유지하고 이 부분만 append.
"""

# ── 카드 생성 상수 ─────────────────────────────────────────
CARD_CATEGORIES: list[str] = [
    "시장전체",
    "금리·연준",
    "섹터",
    "대장주",
    "매크로",
    "이벤트",
    "AI·테크",
]

CARD_MONEY_FLOW_VALUES = ("상승", "하락", "관망")

# 각 필드 글자 수 범위
CARD_MARKET_STATUS_MIN = 10
CARD_MARKET_STATUS_MAX = 30
CARD_KEY_REASON_MIN = 15
CARD_KEY_REASON_MAX = 50
CARD_INVEST_POINT_MIN = 10
CARD_INVEST_POINT_MAX = 40


def _call_gemini_card(user_content: str) -> str:
    """카드 생성 전용 Gemini 호출 (temperature 낮게 — 할루시네이션 억제)."""
    return str(_call_gemini(user_content, temperature=0.20) or "").strip()


# ── 후처리 헬퍼 ───────────────────────────────────────────

def _clamp_text(text: str, min_len: int, max_len: int) -> str:
    t = (text or "").strip()
    if len(t) < min_len:
        return ""
    return t[:max_len]


def _validate_card_dict(data: dict) -> None:
    for key in ("money_flow", "market_status", "key_reasons", "invest_point", "tags"):
        if key not in data:
            raise ValueError(f"필수 필드 누락: {key}")
    if data["money_flow"] not in CARD_MONEY_FLOW_VALUES:
        raise ValueError(f"money_flow 허용값 외: {data['money_flow']!r}")
    if not isinstance(data["key_reasons"], list) or len(data["key_reasons"]) < 1:
        raise ValueError("key_reasons 최소 1개 필요")
    if not str(data.get("market_status", "")).strip():
        raise ValueError("market_status 비어 있음")


def _postprocess_card_dict(data: dict, category: str) -> dict:
    from datetime import datetime, timezone

    money_flow = str(data.get("money_flow", "관망")).strip()
    if money_flow not in CARD_MONEY_FLOW_VALUES:
        money_flow = "관망"

    market_status = _clamp_text(
        str(data.get("market_status", "")),
        CARD_MARKET_STATUS_MIN,
        CARD_MARKET_STATUS_MAX,
    )

    raw_reasons = data.get("key_reasons", [])
    if not isinstance(raw_reasons, list):
        raw_reasons = []
    key_reasons = [
        t for r in raw_reasons
        if (t := _clamp_text(str(r), CARD_KEY_REASON_MIN, CARD_KEY_REASON_MAX))
    ][:3]

    invest_point = _clamp_text(
        str(data.get("invest_point", "")),
        CARD_INVEST_POINT_MIN,
        CARD_INVEST_POINT_MAX,
    )

    raw_tags = data.get("tags", [])
    tags = [str(t).strip() for t in (raw_tags if isinstance(raw_tags, list) else []) if str(t).strip()][:2]

    if not market_status:
        raise ValueError("market_status 후처리 후 비어 있음")
    if not key_reasons:
        raise ValueError("key_reasons 후처리 후 비어 있음")

    now = datetime.now(timezone.utc)
    return {
        "card_id": f"{category}_{now.strftime('%Y%m%d_%H%M')}",
        "category": category,
        "money_flow": money_flow,
        "market_status": market_status,
        "key_reasons": key_reasons,
        "invest_point": invest_point,
        "tags": tags,
        "generated_at": now.isoformat(),
    }


# ── 1단계: 필터링 ──────────────────────────────────────────

def filter_important_articles(
    articles: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """
    수집된 기사 전체 제목+요약을 AI에 한 번에 던져
    투자 판단에 중요한 기사만 추려낸다. API 1회 호출.
    필터링 실패 시 전체 반환(파이프라인 중단 방지).
    """
    if not articles:
        return []

    lines = []
    for i, a in enumerate(articles):
        title = (a.get("title") or "").strip()[:120]
        summary = (a.get("summary") or "").strip()[:80]
        source = (a.get("source") or "").strip()
        lines.append(f"{i}. [{source}] {title} — {summary}")

    user = (
        "아래 뉴스 기사 목록을 읽고, 한국 개인 투자자의 투자 판단에 실질적으로 영향을 주는 기사 번호만 골라줘.\n\n"
        "【제거 기준 — 하나라도 해당하면 제거】\n"
        "- 거의 동일한 내용의 중복 기사\n"
        "- PR성·보도자료·광고성·협찬 기사\n"
        "- 수치만 나열하고 시장 해석이 없는 단순 시세 기사\n"
        "- 연예·스포츠·사건사고·날씨 등 금융시장과 무관한 뉴스\n"
        "- '급등' '폭락' '대박' '반드시' 등 근거 없는 어그로성 제목\n"
        "- 특정 종목 매수/매도를 직접 권유하는 기사\n\n"
        "【유지 기준 — 하나라도 해당하면 유지】\n"
        "- 금리·환율·지수·실적·정책 등 시장에 직접 영향을 주는 내용\n"
        "- 주요 기업 실적 발표·가이던스·M&A·경영 변화\n"
        "- 연준·한국은행·정부 정책 발표 및 위원 발언\n"
        "- 글로벌 매크로 지표 (CPI·GDP·고용 등)\n"
        "- 지정학 리스크 (전쟁·제재·무역분쟁 등 시장 영향)\n\n"
        f"기사 목록:\n{chr(10).join(lines)}\n\n"
        "반드시 JSON 한 개만 출력 (코드펜스·설명 없이):\n"
        '{"keep": [0, 3, 5, ...]}'
    )

    try:
        raw = _call_gemini_card(user)
        data = _require_json_dict(raw)
        keep_indices = data.get("keep", [])
        if not isinstance(keep_indices, list):
            return articles
        kept = [
            articles[i] for i in keep_indices
            if isinstance(i, int) and 0 <= i < len(articles)
        ]
        logger.info("필터링: %d개 → %d개", len(articles), len(kept))
        return kept if kept else articles
    except Exception as e:
        logger.warning("filter_important_articles 실패, 전체 사용: %s", e)
        return articles


# ── 2단계: 카테고리 분류 ───────────────────────────────────

def classify_articles_to_cards(
    articles: list[dict[str, Any]],
) -> dict[str, list[dict[str, Any]]]:
    """
    필터링된 기사들을 카드 카테고리별로 분류. API 1회 호출.
    한 기사는 반드시 하나의 카테고리에만 배정.
    """
    if not articles:
        return {cat: [] for cat in CARD_CATEGORIES}

    lines = [
        f"{i}. [{(a.get('source') or '').strip()}] {(a.get('title') or '').strip()[:120]}"
        for i, a in enumerate(articles)
    ]

    user = (
        "아래 기사들을 각각 다음 카테고리 중 정확히 하나에만 배정해줘.\n"
        "카테고리: 시장전체 / 금리·연준 / 섹터 / 대장주 / 매크로 / 이벤트 / AI·테크\n\n"
        "【카테고리 기준】\n"
        "- 시장전체: 코스피·코스닥·나스닥·S&P500 등 전체 지수 흐름, 외국인/기관 수급\n"
        "- 금리·연준: 기준금리 결정·예상, 연준 발언, 한국은행 통화정책, FOMC\n"
        "- 섹터: 반도체·바이오·에너지·2차전지 등 특정 산업 섹터 전체 흐름\n"
        "- 대장주: 삼성전자·SK하이닉스·엔비디아·애플 등 개별 대형 종목 이슈\n"
        "- 매크로: 환율·물가(CPI·PPI)·고용·GDP·무역수지·관세 등 거시경제 지표\n"
        "- 이벤트: 실적발표·IPO·M&A·정부 규제·지정학 리스크 등 단발성 이벤트\n"
        "- AI·테크: AI 모델·GPU·빅테크 전략·반도체 신제품·데이터센터 투자\n\n"
        "【주의】 기사 하나는 반드시 한 카테고리에만 배정. 중복 배정 금지.\n\n"
        f"기사 목록:\n{chr(10).join(lines)}\n\n"
        "반드시 JSON 한 개만 출력 (코드펜스·설명 없이):\n"
        '{"시장전체":[0,2],"금리·연준":[1],"섹터":[],"대장주":[3,4],"매크로":[],"이벤트":[5],"AI·테크":[6,7]}'
    )

    result: dict[str, list[dict[str, Any]]] = {cat: [] for cat in CARD_CATEGORIES}
    assigned: set[int] = set()

    try:
        raw = _call_gemini_card(user)
        data = _require_json_dict(raw)
        for cat in CARD_CATEGORIES:
            for i in (data.get(cat) or []):
                if isinstance(i, int) and 0 <= i < len(articles) and i not in assigned:
                    assigned.add(i)
                    result[cat].append(articles[i])
        logger.info("카테고리 분류 완료: %s", {k: len(v) for k, v in result.items() if v})
    except Exception as e:
        logger.warning("classify_articles_to_cards 실패: %s", e)

    return result


# ── 3단계: 카드 1장 생성 ───────────────────────────────────

_CARD_PROMPT = """\
카테고리: {category}

아래 기사들을 읽고 한국 개인 투자자를 위한 시장 해석 카드를 만들어줘.
"뉴스 요약"이 아닌 "시장 결론과 돈의 흐름"을 도출하는 것이 목적이야.

【출력 필드】

money_flow (필수)
· "상승" / "하락" / "관망" 중 하나만
· 여러 기사가 같은 방향 → 상승 또는 하락
· 방향 엇갈림·근거 부족 → 반드시 "관망"
· 억지 결론 금지

market_status (필수)
· {status_min}~{status_max}자 이내, 명사형 종결
· 예: "관망세 지속", "금리 인하 기대 약화", "반도체 수급 개선 기대"
· 서술형·해요체 금지 / 기사에 없는 내용 지어내기 금지

key_reasons (필수, 2~3개)
· 각 항목 {reason_min}~{reason_max}자 이내, 명사형 종결
· 판단의 근거가 되는 이유만 — 단순 뉴스 제목 복사 금지
· 기사에 수치(등락률·금리·환율·실적 등)가 있으면 반드시 포함
· 기사에 없는 수치·주장·기업명 지어내기 절대 금지
· 예: "나스닥 -1.2%, 기술주 전반 매도세 확대"
· 예: "연준 위원 매파 발언, 금리 인하 기대 후퇴"

invest_point (필수)
· {point_min}~{point_max}자 이내
· 매수/매도 권유 금지 — "지금 무엇을 봐야 하는가" 관찰 포인트
· 예: "CPI 발표 전 변동성 주의"
· 예: "AI 반도체 대장주 수급 지속 여부 확인"
· 예: "금리 방향 확인 후 포지션 조정 고려"

tags (1~2개)
· 이 카드와 직접 관련된 시장 키워드
· 예: 미국증시, 국내증시, AI, 반도체, 금리, 환율, 빅테크

【반드시 준수】
1. 기사에 없는 내용(기업명·수치·전망) 지어내기 절대 금지
2. money_flow 근거 수치는 key_reasons에 반드시 포함
3. 기사 방향 엇갈리면 money_flow는 반드시 "관망"
4. 모든 필드 내부에 큰따옴표(") 사용 금지 — 필요시 작은따옴표(') 사용
5. 서술형·해요체 종결 금지 ("~습니다" "~에요" "~있다" 금지)

기사 목록:
{articles_text}

반드시 JSON 한 개만 출력 (코드펜스·설명 없이):
{{"money_flow":"관망","market_status":"...","key_reasons":["...","..."],"invest_point":"...","tags":["미국증시"]}}
"""


def generate_single_card(
    category: str,
    articles: list[dict[str, Any]],
) -> dict[str, Any] | None:
    """
    카테고리별 기사 묶음 → 카드 1장 생성. API 1회 (최대 2회 재시도).
    본문(body) 있으면 본문 우선, 없으면 제목+요약 사용.
    """
    if not articles:
        return None

    article_blocks = []
    for i, a in enumerate(articles, start=1):
        title = (a.get("title") or "").strip()
        body = (a.get("body") or "").strip()
        summary = (a.get("summary") or "").strip()
        source = (a.get("source") or "").strip()
        published = (a.get("published_at") or "").strip()[:16]
        content = body[:1800] if body else summary[:300]
        date_str = f" ({published})" if published else ""
        article_blocks.append(
            f"[기사 {i}] 출처: {source}{date_str}\n"
            f"제목: {title}\n"
            f"내용: {content}"
        )

    user = _CARD_PROMPT.format(
        category=category,
        status_min=CARD_MARKET_STATUS_MIN,
        status_max=CARD_MARKET_STATUS_MAX,
        reason_min=CARD_KEY_REASON_MIN,
        reason_max=CARD_KEY_REASON_MAX,
        point_min=CARD_INVEST_POINT_MIN,
        point_max=CARD_INVEST_POINT_MAX,
        articles_text="\n\n".join(article_blocks),
    )

    last_err: Exception | None = None
    for attempt in range(2):
        try:
            raw = _call_gemini_card(user)
            if not raw:
                raise ValueError("Gemini 응답 비어 있음")
            data = _require_json_dict(raw)
            _validate_card_dict(data)
            card = _postprocess_card_dict(data, category)
            logger.info(
                "카드 생성 완료: %s | money_flow=%s | reasons=%d개",
                category, card["money_flow"], len(card["key_reasons"]),
            )
            return card
        except Exception as e:
            last_err = e
            logger.warning(
                "generate_single_card 재시도 (%d/2) category=%s: %s",
                attempt + 1, category, e,
            )
            if attempt == 0:
                time.sleep(1.0)

    logger.error("generate_single_card 최종 실패: %s — %s", category, last_err)
    return None


# ── 전체 카드 생성 파이프라인 ──────────────────────────────

def generate_briefing_cards(
    all_articles: list[dict[str, Any]],
) -> list[dict[str, Any]]:
    """
    RSS 기사 전체 → 필터링 → 분류 → 카드 6~7장 생성.

    1) filter_important_articles  : 중요 기사 추출  (API 1회)
    2) classify_articles_to_cards : 카테고리 분류   (API 1회)
    3) generate_single_card × N   : 카드 생성       (API 최대 7회)

    기사 없는 카테고리는 스킵. 총 API 호출 8~9회.
    """
    if not all_articles:
        logger.warning("generate_briefing_cards: 입력 기사 없음")
        return []

    logger.info("브리핑 카드 생성 시작: 입력 기사 %d개", len(all_articles))

    # 1단계: 필터링
    filtered = filter_important_articles(all_articles)
    if not filtered:
        logger.warning("필터링 후 기사 없음 — 원본 사용")
        filtered = all_articles
    logger.info("필터링 완료: %d개", len(filtered))

    # 2단계: 카테고리 분류
    categorized = classify_articles_to_cards(filtered)

    # 3단계: 카드 생성
    cards: list[dict[str, Any]] = []
    for category in CARD_CATEGORIES:
        articles_for_cat = categorized.get(category, [])
        if not articles_for_cat:
            logger.info("카드 스킵 (기사 없음): %s", category)
            continue
        logger.info("카드 생성 중: %s (%d개 기사)", category, len(articles_for_cat))
        card = generate_single_card(category, articles_for_cat)
        if card:
            cards.append(card)
        time.sleep(0.5)

    logger.info(
        "브리핑 카드 생성 완료: %d장 | 카테고리: %s",
        len(cards),
        [c["category"] for c in cards],
    )
    return cards
