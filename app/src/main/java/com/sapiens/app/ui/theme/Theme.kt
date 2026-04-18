package com.sapiens.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimaryDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = BackgroundDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainer = CardDark,
    surfaceContainerHigh = ElevatedDark,
    outline = HairDark
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = TextPrimaryDark,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = BackgroundLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainer = CardLight,
    surfaceContainerHigh = ElevatedLight,
    outline = HairLight
)

@Composable
fun SapiensTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    CompositionLocalProvider(
        LocalRippleConfiguration provides null
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SapiensTypography,
            content = content
        )
    }
}