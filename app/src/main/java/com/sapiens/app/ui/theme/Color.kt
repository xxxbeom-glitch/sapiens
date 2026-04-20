package com.sapiens.app.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

// --- Primary (주황 브랜드) ---
val Primary = Color(0xFFF56E0F)

/** @deprecated 호환용 — [Primary]와 동일. */
val Accent: Color get() = Primary

val OnPrimaryFixed = Color(0xFFFFFFFF)

// --- Dark palette (정적 참조) ---
val BackgroundDark = Color(0xFF151419)
val CardDark = Color(0xFF1B1B1E)
val ElevatedDark = Color(0xFF262626)
val TextPrimaryDark = Color(0xFFFBFBFB)
val TextSecondaryDark = Color(0xFF878787)
val HairDark = Color(0x26FFFFFF)
val UpDark = Color(0xFFFF3B30)
val DownDark = Color(0xFF0064FF)

// --- Light palette ---
val BackgroundLight = Color(0xFFFAFAF8)
val CardLight = Color(0xFFFFFFFF)
val ElevatedLight = Color(0xFFF2F1EE)
val TextPrimaryLight = Color(0xFF1A1A1A)
val TextSecondaryLight = Color(0xFF6B6B6B)
val HairLight = Color(0x14000000)
val UpLight = Color(0xFFE0291F)
val DownLight = Color(0xFF0052CC)

// --- Semantic (라이트/다크 공통 UI 조각) ---
val Success = Color(0xFF1D9E75)
val SuccessContainer = Color(0x261D9E75)
val Warning = Color(0xFFEF9F27)
val WarningContainer = Color(0x26EF9F27)
val Error = Color(0xFFE24B4A)
val ErrorContainer = Color(0x26E24B4A)
val SuccessBright = Color(0xFF22C55E)

val SurfaceMuted = Color(0xFF28282A)
val SurfaceChartInactive = Color(0xFF3C3C3C)
val SurfaceOpinion = Color(0xFF1F1D24)
val SurfaceHairlineOnDark = Color(0x14FFFFFF)

/** 어두운 카드 위 얇은 구분선(화이트 ~6%). */
val DividerOnMutedSurface = Color.White.copy(alpha = 0.06f)

/** 카드·시트 보조 채움·구분선·아이콘 고스트 배경용 알파 토큰. */
object ContentAlpha {
    const val onCardSurface = 0.08f
    const val hairlineOnSecondary = 0.2f
    const val iconGhost = 0.15f
    const val modalScrim = 0.6f
    const val sheetDragHandleOnSecondary = 0.5f
}

// --- 뉴스 카테고리 칩 (배경, 글자) ---
object CategoryChipPalette {
    val economyBg = Color(0xFF1A2F1A)
    val economyFg = Color(0xFF4CAF50)
    val itBg = Color(0xFF1A1F2F)
    val itFg = Color(0xFF5B8DEF)
    val politicsBg = Color(0xFF2F1A1A)
    val politicsFg = Color(0xFFEF5B5B)
    val societyBg = Color(0xFF2A2214)
    val societyFg = Color(0xFFD4A843)
    val worldBg = Color(0xFF142228)
    val worldFg = Color(0xFF43B8D4)
    val realestateBg = Color(0xFF2A1A2F)
    val realestateFg = Color(0xFFB05BEF)
    val industryBg = Color(0xFF2F1F1A)
    val industryFg = Color(0xFFEF8C5B)
    val financeBg = Color(0xFF1A2525)
    val financeFg = Color(0xFF5BCEBC)
    val macroBg = Color(0xFF252015)
    val macroFg = Color(0xFFD4943A)
    /** 암호화폐 카테고리 칩 */
    val cryptoBg = Color(0xFF221A0F)
    val cryptoFg = Color(0xFFFFB74D)
    val defaultBg = Color(0xFF222222)
    val defaultFg = Color(0xFF888888)
}

// --- 마켓 섹터 칩 ---
object SectorChipPalette {
    fun colors(sector: String): Pair<Color, Color> = when (sector.trim()) {
        "반도체" -> Color(0xFF1A1F2F) to Color(0xFF5B8DEF)
        "AI/반도체" -> Color(0xFF1A2035) to Color(0xFF6B9DF5)
        "인터넷" -> Color(0xFF1A2230) to Color(0xFF7BAAF5)
        "플랫폼/디바이스" -> Color(0xFF1B2135) to Color(0xFF5F95F0)
        "2차전지/화학" -> Color(0xFF1A2538) to Color(0xFF70A8F5)
        "전기차" -> Color(0xFF1C2338) to Color(0xFF6AA0F0)
        else -> Color(0xFF1A2030) to Color(0xFF7EB0F5)
    }
}

// --- 런타임 테마 (기존 패턴 유지) ---
private val backgroundState = mutableStateOf(BackgroundDark)
private val cardState = mutableStateOf(CardDark)
private val elevatedState = mutableStateOf(ElevatedDark)
private val textPrimaryState = mutableStateOf(TextPrimaryDark)
private val textSecondaryState = mutableStateOf(TextSecondaryDark)
private val textTertiaryState = mutableStateOf(TextSecondaryDark)
private val hairState = mutableStateOf(HairDark)
private val marketUpState = mutableStateOf(UpDark)
private val marketDownState = mutableStateOf(DownDark)

/** 앱 배경 (다크/라이트 전환). */
val Background: Color get() = backgroundState.value

/** 카드·리스트 서페이스. */
val Card: Color get() = cardState.value

/** 약간 올린 서페이스. */
val Elevated: Color get() = elevatedState.value

/** 본문·제목 1차 텍스트. */
val TextPrimary: Color get() = textPrimaryState.value

/** 부가 설명·캡션. */
val TextSecondary: Color get() = textSecondaryState.value

/** 비활·힌트·3차 계층. */
val TextTertiary: Color get() = textTertiaryState.value

val Hair: Color get() = hairState.value
val MarketUp: Color get() = marketUpState.value
val MarketDown: Color get() = marketDownState.value
val MarketFlat = Color(0xFF878787)

fun applyThemePalette(darkTheme: Boolean) {
    backgroundState.value = if (darkTheme) BackgroundDark else BackgroundLight
    cardState.value = if (darkTheme) CardDark else CardLight
    elevatedState.value = if (darkTheme) ElevatedDark else ElevatedLight
    textPrimaryState.value = if (darkTheme) TextPrimaryDark else TextPrimaryLight
    textSecondaryState.value = if (darkTheme) TextSecondaryDark else TextSecondaryLight
    textTertiaryState.value = if (darkTheme) TextSecondaryDark else TextSecondaryLight.copy(alpha = 0.85f)
    hairState.value = if (darkTheme) HairDark else HairLight
    marketUpState.value = if (darkTheme) UpDark else UpLight
    marketDownState.value = if (darkTheme) DownDark else DownLight
}
