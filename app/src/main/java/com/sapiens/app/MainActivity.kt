package com.sapiens.app

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.sapiens.app.data.store.UserPreferencesRepository
import com.sapiens.app.ui.main.MainScreen
import com.sapiens.app.ui.theme.SapiensTheme
import com.sapiens.app.ui.theme.applyThemePalette
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        setContent {
            SapiensApp()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        android.util.Log.d(
            "StatusBar",
            "hasFocus=$hasFocus, isLight=${controller.isAppearanceLightStatusBars}"
        )
    }
}

@Composable
private fun SapiensApp() {
    val context = LocalContext.current
    val preferencesRepository = remember { UserPreferencesRepository(context = context) }
    val savedThemeMode by preferencesRepository.themeModeFlow.collectAsState(
        initial = UserPreferencesRepository.THEME_DARK
    )
    var isDarkTheme by remember { mutableStateOf(true) }
    LaunchedEffect(savedThemeMode) {
        isDarkTheme = savedThemeMode != UserPreferencesRepository.THEME_LIGHT
    }

    LaunchedEffect(isDarkTheme) {
        applyThemePalette(isDarkTheme)
    }

    SapiensTheme(darkTheme = isDarkTheme) {
        MainScreen()
    }
}