package com.sapiens.app.ui.main

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sapiens.app.ui.my.AuthViewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sapiens.app.R
import com.sapiens.app.data.repository.NewsRepositoryImpl
import com.sapiens.app.data.sync.UserCloudBackupRepository
import com.sapiens.app.data.sync.UserCloudBackupScheduler
import com.sapiens.app.ui.market.MarketScreen
import com.sapiens.app.ui.market.MarketViewModel
import com.sapiens.app.ui.my.MyScreen
import com.sapiens.app.ui.news.NewsScreen
import com.sapiens.app.ui.news.NewsViewModel
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class BottomTab(
    val label: String,
    val iconResId: Int
)

/** false: 하단 마켓 탭·[MarketViewModel] 비활성. 뉴스(국내/미국 증시 기사)는 유지. */
private const val MARKET_TAB_ENABLED = false

private fun mainBottomTabs(): List<BottomTab> =
    if (MARKET_TAB_ENABLED) {
        listOf(
            BottomTab(label = "뉴스", iconResId = R.drawable.ico_news),
            BottomTab(label = "마켓", iconResId = R.drawable.ico_market),
            BottomTab(label = "마이", iconResId = R.drawable.ico_my),
        )
    } else {
        listOf(
            BottomTab(label = "뉴스", iconResId = R.drawable.ico_news),
            BottomTab(label = "마이", iconResId = R.drawable.ico_my),
        )
    }

@Composable
fun MainScreen(
    navigateToSectionKey: String? = null,
    onNavigateToSectionConsumed: () -> Unit = {},
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = remember { mainBottomTabs() }

    val context = LocalContext.current
    val newsRepository = remember { NewsRepositoryImpl() }
    val newsViewModel: NewsViewModel = viewModel(
        factory = NewsViewModel.factory(newsRepository)
    )
    val marketViewModel: MarketViewModel? =
        if (MARKET_TAB_ENABLED) {
            viewModel(factory = MarketViewModel.factory(newsRepository))
        } else {
            null
        }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.factory(context.applicationContext as Application)
    )
    val application = context.applicationContext as Application
    val cloudBackupRepository = remember { UserCloudBackupRepository.create(application) }
    val authUser by authViewModel.authUser.collectAsState(initial = null)

    LaunchedEffect(authUser?.uid) {
        val uid = authUser?.uid
        if (uid.isNullOrBlank()) {
            UserCloudBackupScheduler.cancel(application)
        } else {
            UserCloudBackupScheduler.schedule(application)
            withContext(Dispatchers.IO) {
                cloudBackupRepository.restoreFromCloudIfNeeded(uid)
            }
        }
    }

    LaunchedEffect(navigateToSectionKey) {
        val key = navigateToSectionKey?.trim()?.takeIf { it.isNotEmpty() } ?: return@LaunchedEffect
        when (key) {
            "briefing", "domestic_news", "news" -> selectedTabIndex = 0
            "market" -> selectedTabIndex = if (MARKET_TAB_ENABLED) 1 else 0
        }
        onNavigateToSectionConsumed()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            MainTopAppBar(
                title = tabs[selectedTabIndex].label,
            )
        },
        bottomBar = {
            BottomNavigationBar(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { selectedTabIndex = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> NewsScreen(viewModel = newsViewModel)
                1 -> {
                    val mv = marketViewModel
                    if (MARKET_TAB_ENABLED && mv != null) {
                        MarketScreen(viewModel = mv)
                    } else {
                        MyScreen(authViewModel = authViewModel)
                    }
                }
                else -> MyScreen(authViewModel = authViewModel)
            }
        }
    }

}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainTopAppBar(
    title: String,
) {
    TopAppBar(
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = Spacing.space6),
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge.copy(
                    fontSize = (androidx.compose.material3.MaterialTheme.typography.titleLarge.fontSize.value + 6f).sp
                ),
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background,
            titleContentColor = TextPrimary
        )
    )
}

@Composable
private fun BottomNavigationBar(
    tabs: List<BottomTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Background
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedTabIndex == index
            val tint = if (selected) Accent else TextSecondary

            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        painter = painterResource(id = tab.iconResId),
                        contentDescription = tab.label,
                        tint = tint
                    )
                },
                interactionSource = remember { MutableInteractionSource() },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                label = {
                    Text(
                        text = tab.label,
                        color = tint
                    )
                }
            )
        }
    }
}

