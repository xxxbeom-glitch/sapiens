package com.breaktobreak.dailynews.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.breaktobreak.dailynews.ui.theme.Accent
import com.breaktobreak.dailynews.ui.theme.Background
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class BottomTab(
    val label: String,
    val iconResId: Int
)

private val tabs = listOf(
    BottomTab(label = "브리핑", iconResId = android.R.drawable.ic_menu_agenda),
    BottomTab(label = "뉴스", iconResId = android.R.drawable.ic_menu_info_details),
    BottomTab(label = "기업정보", iconResId = android.R.drawable.ic_menu_manage)
)

@Composable
fun MainScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val todayLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            MainTopAppBar(
                title = tabs[selectedTabIndex].label,
                dateText = todayLabel
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
                0 -> BriefingPlaceholder()
                1 -> NewsPlaceholder()
                else -> CompanyInfoPlaceholder()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MainTopAppBar(
    title: String,
    dateText: String
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = dateText,
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_preferences),
                    contentDescription = "설정",
                    tint = Accent
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Background,
            titleContentColor = TextPrimary,
            actionIconContentColor = Accent
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

@Composable
private fun BriefingPlaceholder() {
    PlaceholderText(text = "브리핑 화면 Placeholder")
}

@Composable
private fun NewsPlaceholder() {
    PlaceholderText(text = "뉴스 화면 Placeholder")
}

@Composable
private fun CompanyInfoPlaceholder() {
    PlaceholderText(text = "기업정보 화면 Placeholder")
}

@Composable
private fun PlaceholderText(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
    }
}
