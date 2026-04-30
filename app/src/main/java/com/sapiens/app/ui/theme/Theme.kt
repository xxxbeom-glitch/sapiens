package com.sapiens.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimaryFixed,
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
    primary = Primary,
    onPrimary = OnPrimaryFixed,
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

    val figmaWidthScale = rememberFigmaFrameWidthScale()
    val density = LocalDensity.current
    val densityFixedFontScale =
        remember(density.density) {
            Density(density = density.density, fontScale = 1f)
        }
    val scaledTypography =
        remember(figmaWidthScale) {
            SapiensTypography.scaleForFigmaFrame(figmaWidthScale)
        }

    CompositionLocalProvider(
        LocalDensity provides densityFixedFontScale,
        LocalFigmaFrameWidthScale provides figmaWidthScale,
    ) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides null,
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                typography = scaledTypography,
                content = content,
            )
        }
    }
}