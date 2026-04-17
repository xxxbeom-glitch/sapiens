package com.breaktobreak.dailynews.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.breaktobreak.dailynews.ui.briefing.BriefingScreen
import com.breaktobreak.dailynews.ui.company.CompanyScreen
import com.breaktobreak.dailynews.ui.my.MyScreen
import com.breaktobreak.dailynews.ui.news.NewsScreen
import com.breaktobreak.dailynews.ui.theme.Accent
import com.breaktobreak.dailynews.ui.theme.Background
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary

private data class BottomTab(
    val label: String,
    val iconResId: Int
)

private val tabs = listOf(
    BottomTab(label = "브리핑", iconResId = android.R.drawable.ic_menu_agenda),
    BottomTab(label = "뉴스", iconResId = android.R.drawable.ic_menu_info_details),
    BottomTab(label = "기업정보", iconResId = android.R.drawable.ic_menu_manage),
    BottomTab(label = "마이", iconResId = android.R.drawable.ic_menu_myplaces)
)

@Composable
fun MainScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            MainTopAppBar(
                title = tabs[selectedTabIndex].label
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
                0 -> BriefingScreen()
                1 -> NewsScreen()
                2 -> CompanyScreen()
                else -> MyScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainTopAppBar(
    title: String
) {
    TopAppBar(
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

