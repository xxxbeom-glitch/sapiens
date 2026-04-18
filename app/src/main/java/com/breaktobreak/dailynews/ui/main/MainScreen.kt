package com.breaktobreak.dailynews.ui.main

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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.breaktobreak.dailynews.R
import com.breaktobreak.dailynews.data.mock.MockData
import com.breaktobreak.dailynews.data.model.Company
import com.breaktobreak.dailynews.data.repository.NewsRepositoryImpl
import com.breaktobreak.dailynews.ui.briefing.BriefingScreen
import com.breaktobreak.dailynews.ui.briefing.BriefingViewModel
import com.breaktobreak.dailynews.ui.company.CompanySearchBottomSheet
import com.breaktobreak.dailynews.ui.company.CompanyScreen
import com.breaktobreak.dailynews.ui.my.MyScreen
import com.breaktobreak.dailynews.ui.news.NewsScreen
import com.breaktobreak.dailynews.ui.news.NewsViewModel
import com.breaktobreak.dailynews.ui.theme.Accent
import com.breaktobreak.dailynews.ui.theme.Background
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary

private data class BottomTab(
    val label: String,
    val iconResId: Int
)

private val tabs = listOf(
    BottomTab(label = "브리핑", iconResId = R.drawable.ico_brief),
    BottomTab(label = "뉴스", iconResId = R.drawable.ico_news),
    BottomTab(label = "기업정보", iconResId = R.drawable.ico_company),
    BottomTab(label = "마이", iconResId = R.drawable.ico_my)
)

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showCompanySearchSheet by remember { mutableStateOf(false) }
    val addedCompanies = remember { mutableStateListOf<Company>() }

    val newsRepository = remember { NewsRepositoryImpl() }
    val briefingViewModel: BriefingViewModel = viewModel(factory = BriefingViewModel.factory(newsRepository))
    val newsViewModel: NewsViewModel = viewModel(factory = NewsViewModel.factory(newsRepository))

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            MainTopAppBar(
                title = tabs[selectedTabIndex].label,
                showSearchAction = selectedTabIndex == 2,
                onClickSearch = { showCompanySearchSheet = true }
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
                0 -> BriefingScreen(viewModel = briefingViewModel)
                1 -> NewsScreen(viewModel = newsViewModel)
                2 -> CompanyScreen(
                    addedCompanies = addedCompanies
                )
                else -> MyScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange
                )
            }
        }
    }

    if (showCompanySearchSheet) {
        CompanySearchBottomSheet(
            allCompanies = MockData.companyList,
            addedTickers = addedCompanies.map { it.ticker }.toSet(),
            onAddCompany = { company ->
                if (addedCompanies.none { it.ticker == company.ticker }) {
                    addedCompanies.add(company)
                }
            },
            onDismissRequest = { showCompanySearchSheet = false }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainTopAppBar(
    title: String,
    showSearchAction: Boolean,
    onClickSearch: () -> Unit
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
            if (showSearchAction) {
                IconButton(
                    onClick = onClickSearch,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "기업 검색",
                        tint = TextPrimary
                    )
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

