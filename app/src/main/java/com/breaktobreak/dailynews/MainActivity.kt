package com.breaktobreak.dailynews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.breaktobreak.dailynews.data.store.UserPreferencesRepository
import com.breaktobreak.dailynews.ui.main.MainScreen
import com.breaktobreak.dailynews.ui.theme.DailyNewsTheme
import com.breaktobreak.dailynews.ui.theme.applyThemePalette
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyNewsApp()
        }
    }
}

@Composable
private fun DailyNewsApp() {
    val context = LocalContext.current
    val preferencesRepository = remember { UserPreferencesRepository(context = context) }
    val savedThemeMode by preferencesRepository.themeModeFlow.collectAsState(
        initial = UserPreferencesRepository.THEME_DARK
    )
    var isDarkTheme by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(savedThemeMode) {
        isDarkTheme = savedThemeMode != UserPreferencesRepository.THEME_LIGHT
    }

    LaunchedEffect(isDarkTheme) {
        applyThemePalette(isDarkTheme)
    }

    DailyNewsTheme(darkTheme = isDarkTheme) {
        MainScreen(
            isDarkTheme = isDarkTheme,
            onThemeChange = { darkMode ->
                isDarkTheme = darkMode
                coroutineScope.launch {
                    preferencesRepository.setThemeMode(
                        if (darkMode) UserPreferencesRepository.THEME_DARK
                        else UserPreferencesRepository.THEME_LIGHT
                    )
                }
            }
        )
    }
}