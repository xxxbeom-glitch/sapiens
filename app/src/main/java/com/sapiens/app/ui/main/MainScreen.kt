package com.sapiens.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sapiens.app.R
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Company
import com.sapiens.app.data.repository.FeedbackRepositoryImpl
import com.sapiens.app.data.repository.NewsRepositoryImpl
import com.sapiens.app.data.store.ArticleBookmarksRepository
import com.sapiens.app.ui.briefing.BriefingScreen
import com.sapiens.app.ui.briefing.BriefingViewModel
import com.sapiens.app.ui.market.MarketSearchBottomSheet
import com.sapiens.app.ui.market.MarketScreen
import com.sapiens.app.ui.market.MarketViewModel
import com.sapiens.app.ui.my.MyScreen
import com.sapiens.app.ui.news.NewsRegionToggle
import com.sapiens.app.ui.news.NewsScreen
import com.sapiens.app.ui.news.NewsViewModel
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

private data class BottomTab(
    val label: String,
    val iconResId: Int
)

private val tabs = listOf(
    BottomTab(label = "브리핑", iconResId = R.drawable.ico_brief),
    BottomTab(label = "뉴스", iconResId = R.drawable.ico_news),
    BottomTab(label = "마켓", iconResId = R.drawable.ico_company),
    BottomTab(label = "마이", iconResId = R.drawable.ico_my)
)

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showMarketSearchSheet by remember { mutableStateOf(false) }
    var isOverseasNews by remember { mutableStateOf(false) }
    val addedCompanies = remember { mutableStateListOf<Company>() }

    val context = LocalContext.current
    val newsRepository = remember { NewsRepositoryImpl() }
    val bookmarksRepository = remember { ArticleBookmarksRepository(context.applicationContext) }
    val feedbackRepository = remember { FeedbackRepositoryImpl() }
    val briefingViewModel: BriefingViewModel = viewModel(factory = BriefingViewModel.factory(newsRepository))
    val newsViewModel: NewsViewModel = viewModel(factory = NewsViewModel.factory(newsRepository))
    val marketViewModel: MarketViewModel = viewModel(factory = MarketViewModel.factory(newsRepository))

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            MainTopAppBar(
                title = tabs[selectedTabIndex].label,
                showSearchAction = selectedTabIndex == 2,
                onClickSearch = { showMarketSearchSheet = true },
                showNewsRegionToggle = selectedTabIndex == 1,
                isOverseasNews = isOverseasNews,
                onOverseasNewsChange = { isOverseasNews = it }
            )
        },
        bottomBar = {
            BottomNavigationBar(
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
                0 -> BriefingScreen(
                    viewModel = briefingViewModel,
                    bookmarksRepository = bookmarksRepository
                )
                1 -> NewsScreen(
                    viewModel = newsViewModel,
                    isOverseas = isOverseasNews,
                    bookmarksRepository = bookmarksRepository,
                    feedbackRepository = feedbackRepository
                )
                2 -> MarketScreen(
                    viewModel = marketViewModel,
                    addedCompanies = addedCompanies
                )
                else -> MyScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    bookmarksRepository = bookmarksRepository,
                    feedbackRepository = feedbackRepository
                )
            }
        }
    }

    if (showMarketSearchSheet) {
        MarketSearchBottomSheet(
            allCompanies = MockData.companyList,
            addedTickers = addedCompanies.map { it.ticker }.toSet(),
            onAddCompany = { company ->
                if (addedCompanies.none { it.ticker == company.ticker }) {
                    addedCompanies.add(company)
                }
            },
            onDismissRequest = { showMarketSearchSheet = false }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainTopAppBar(
    title: String,
    showSearchAction: Boolean,
    onClickSearch: () -> Unit,
    showNewsRegionToggle: Boolean = false,
    isOverseasNews: Boolean = false,
    onOverseasNewsChange: (Boolean) -> Unit = {}
) {
    TopAppBar(
        modifier = Modifier
            .statusBarsPadding()
            .padding(top = 6.dp),
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
        actions = {
            when {
                showNewsRegionToggle -> {
                    Box(Modifier.padding(end = 4.dp)) {
                        NewsRegionToggle(
                            isOverseas = isOverseasNews,
                            onIsOverseasChange = onOverseasNewsChange
                        )
                    }
                }
                showSearchAction -> {
                    IconButton(
                        onClick = onClickSearch,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "마켓 검색",
                            tint = TextPrimary
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background,
            titleContentColor = TextPrimary
        )
    )
}

@Composable
private fun BottomNavigationBar(
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

