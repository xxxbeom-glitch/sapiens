package com.sapiens.app.data.model

/**
 * Firestore `briefing_cards/{card_id}` 문서 스키마.
 * 파이프라인 generate_briefing_cards() 출력과 1:1 매핑.
 */
data class BriefingCard(
    val cardId: String = "",
    val category: String = "",
    /** "상승" / "하락" / "관망" */
    val moneyFlow: String = "",
    /** 현재 시장 상태 한 줄 (명사형 종결) */
    val marketStatus: String = "",
    /** 서술형 본문 (결론 → 이유 → 영향, 2~3문장) */
    val body: String = "",
    /** 투자 관찰 포인트 한 줄 */
    val investPoint: String = "",
    /** 관련 시장 태그 1~2개 (예: 미국증시, 국내증시, AI) */
    val tags: List<String> = emptyList(),
    /** 카드 생성 시각 (ISO 8601) */
    val generatedAt: String = "",
)

fun BriefingCard.moneyFlowDirection(): MarketDirection = when (moneyFlow) {
    "상승" -> MarketDirection.UP
    "하락" -> MarketDirection.DOWN
    else  -> MarketDirection.FLAT
}
