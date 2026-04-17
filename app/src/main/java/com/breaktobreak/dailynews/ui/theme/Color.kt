package com.breaktobreak.dailynews.ui.theme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

// Brand accent (shared)
val Accent = Color(0xFFF56E0F)

// Dark tokens
val BackgroundDark = Color(0xFF151419)
val CardDark = Color(0xFF1B1B1E)
val ElevatedDark = Color(0xFF262626)
val TextPrimaryDark = Color(0xFFFBFBFB)
val TextSecondaryDark = Color(0xFF878787)
val HairDark = Color(0x26FFFFFF)
val UpDark = Color(0xFFFF3B30)
val DownDark = Color(0xFF0064FF)

// Light tokens
val BackgroundLight = Color(0xFFFAFAF8)
val CardLight = Color(0xFFFFFFFF)
val ElevatedLight = Color(0xFFF2F1EE)
val TextPrimaryLight = Color(0xFF1A1A1A)
val TextSecondaryLight = Color(0xFF6B6B6B)
val HairLight = Color(0x14000000)
val UpLight = Color(0xFFE0291F)
val DownLight = Color(0xFF0052CC)

private val backgroundState = mutableStateOf(BackgroundDark)
private val cardState = mutableStateOf(CardDark)
private val elevatedState = mutableStateOf(ElevatedDark)
private val textPrimaryState = mutableStateOf(TextPrimaryDark)
private val textSecondaryState = mutableStateOf(TextSecondaryDark)
private val hairState = mutableStateOf(HairDark)
private val marketUpState = mutableStateOf(UpDark)
private val marketDownState = mutableStateOf(DownDark)

// Dynamic theme-aware aliases used by UI
val Background: Color get() = backgroundState.value
val Card: Color get() = cardState.value
val Elevated: Color get() = elevatedState.value
val TextPrimary: Color get() = textPrimaryState.value
val TextSecondary: Color get() = textSecondaryState.value
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
    hairState.value = if (darkTheme) HairDark else HairLight
    marketUpState.value = if (darkTheme) UpDark else UpLight
    marketDownState.value = if (darkTheme) DownDark else DownLight
}