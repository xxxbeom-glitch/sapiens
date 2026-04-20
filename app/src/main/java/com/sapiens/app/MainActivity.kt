package com.sapiens.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.sapiens.app.data.store.UserPreferencesRepository
import com.sapiens.app.messaging.FcmTopicSync
import com.sapiens.app.ui.main.MainScreen
import com.sapiens.app.ui.theme.SapiensTheme
import com.sapiens.app.ui.theme.applyThemePalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationSectionState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val enabled = UserPreferencesRepository(this@MainActivity)
                .pushNotificationsEnabledFlow.first()
            if (enabled) {
                maybeRequestNotificationPermission()
            }
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        mergeSectionFromIntent(intent)

        setContent {
            val pendingSection by notificationSectionState
            SapiensApp(
                notificationSection = pendingSection,
                onNotificationSectionConsumed = { notificationSectionState.value = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mergeSectionFromIntent(intent)
    }

    private fun mergeSectionFromIntent(intent: Intent?) {
        val section = intent?.getStringExtra("section")?.trim()?.takeIf { it.isNotEmpty() }
        if (section != null) {
            notificationSectionState.value = section
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIF_PERMISSION,
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        android.util.Log.d(
            "StatusBar",
            "hasFocus=$hasFocus, isLight=${controller.isAppearanceLightStatusBars}"
        )
    }

    companion object {
        private const val REQUEST_NOTIF_PERMISSION = 0x534e01
    }
}

@Composable
private fun SapiensApp(
    notificationSection: String? = null,
    onNotificationSectionConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val preferencesRepository = remember { UserPreferencesRepository(context = context) }
    val savedThemeMode by preferencesRepository.themeModeFlow.collectAsState(
        initial = UserPreferencesRepository.THEME_DARK
    )
    val pushNotificationsEnabled by preferencesRepository.pushNotificationsEnabledFlow.collectAsState(
        initial = true
    )
    var isDarkTheme by remember { mutableStateOf(true) }
    LaunchedEffect(savedThemeMode) {
        isDarkTheme = savedThemeMode != UserPreferencesRepository.THEME_LIGHT
    }

    LaunchedEffect(isDarkTheme) {
        applyThemePalette(isDarkTheme)
    }

    val scope = rememberCoroutineScope()
    LaunchedEffect(pushNotificationsEnabled) {
        FcmTopicSync.syncFromPreference(appContext, pushNotificationsEnabled)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, pushNotificationsEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    val enabled = preferencesRepository.pushNotificationsEnabledFlow.first()
                    FcmTopicSync.syncFromPreference(appContext, enabled)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SapiensTheme(darkTheme = isDarkTheme) {
        MainScreen(
            navigateToSectionKey = notificationSection,
            onNavigateToSectionConsumed = onNotificationSectionConsumed,
        )
    }
}
