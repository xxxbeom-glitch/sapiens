package com.breaktobreak.dailynews.ui.my

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.breaktobreak.dailynews.data.mock.MockData
import com.breaktobreak.dailynews.data.model.Article
import com.breaktobreak.dailynews.data.store.UserPreferencesRepository
import com.breaktobreak.dailynews.ui.common.ArticleBottomSheet
import com.breaktobreak.dailynews.ui.theme.Accent
import com.breaktobreak.dailynews.ui.theme.Background
import com.breaktobreak.dailynews.ui.theme.RowVertical
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val sectorOptions = listOf(
    "반도체", "AI·빅테크", "2차전지", "바이오", "자동차", "금융", "에너지",
    "미국 빅테크", "나스닥 ETF", "S&P500 ETF", "원자재", "환율·매크로"
)

private val morningNewsOptions = listOf(
    "경제", "정치", "국제", "IT·테크", "부동산", "증권", "산업", "사회"
)

private enum class MyPage(val title: String) {
    MENU("마이"),
    ACCOUNT("계정"),
    THEME("디스플레이"),
    SECTOR("관심 섹터"),
    NEWS_CATEGORY("뉴스 수신 분야"),
    BOOKMARK("저장한 기사"),
    AI_STATUS("AI 연결 상태")
}

@Composable
fun MyScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val preferencesRepository = remember { UserPreferencesRepository(context) }
    val selectedSectors by preferencesRepository.selectedSectorsFlow.collectAsState(initial = emptySet())
    val selectedNewsCategories by preferencesRepository.selectedMorningCategoriesFlow.collectAsState(initial = emptySet())
    val coroutineScope = rememberCoroutineScope()

    val isLoggedIn = false
    val userName = "사용자"
    val userEmail = "user@example.com"

    var currentPage by remember { mutableStateOf(MyPage.MENU) }
    var selectedBookmarkedArticle by remember { mutableStateOf<Article?>(null) }

    when (currentPage) {
        MyPage.MENU -> {
            MyMenuScreen(
                accountStatus = if (isLoggedIn) "$userName · $userEmail" else "로그인이 필요합니다",
                themeStatus = if (isDarkTheme) "다크 모드" else "라이트 모드",
                onClickAccount = { currentPage = MyPage.ACCOUNT },
                onClickTheme = { currentPage = MyPage.THEME },
                onClickSector = { currentPage = MyPage.SECTOR },
                onClickNewsCategory = { currentPage = MyPage.NEWS_CATEGORY },
                onClickBookmark = { currentPage = MyPage.BOOKMARK },
                onClickAiStatus = { currentPage = MyPage.AI_STATUS }
            )
        }

        MyPage.ACCOUNT -> {
            DetailScaffold(
                title = MyPage.ACCOUNT.title,
                onBack = { currentPage = MyPage.MENU }
            ) {
                AccountDetailScreen(
                    isLoggedIn = isLoggedIn,
                    userName = userName,
                    userEmail = userEmail,
                    onGoogleSignInClick = {
                        android.widget.Toast.makeText(context, "준비 중입니다", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    onSignOutClick = { }
                )
            }
        }

        MyPage.SECTOR -> {
            DetailScaffold(
                title = MyPage.SECTOR.title,
                onBack = { currentPage = MyPage.MENU }
            ) {
                SectorDetailScreen(
                    chips = sectorOptions,
                    selectedValues = selectedSectors,
                    onToggle = { value ->
                        coroutineScope.launch { preferencesRepository.toggleSector(value) }
                    }
                )
            }
        }

        MyPage.THEME -> {
            DetailScaffold(
                title = MyPage.THEME.title,
                onBack = { currentPage = MyPage.MENU }
            ) {
                ThemeSettingDetailScreen(
                    isDarkTheme = isDarkTheme,
                    onSelectDarkTheme = { onThemeChange(true) },
                    onSelectLightTheme = { onThemeChange(false) }
                )
            }
        }

        MyPage.NEWS_CATEGORY -> {
            DetailScaffold(
                title = MyPage.NEWS_CATEGORY.title,
                onBack = { currentPage = MyPage.MENU }
            ) {
                NewsCategoryDetailScreen(
                    chips = morningNewsOptions,
                    selectedValues = selectedNewsCategories,
                    onToggle = { value ->
                        coroutineScope.launch { preferencesRepository.toggleMorningCategory(value) }
                    }
                )
            }
        }

        MyPage.BOOKMARK -> {
            DetailScaffold(
                title = MyPage.BOOKMARK.title,
                onBack = { currentPage = MyPage.MENU }
            ) {
                BookmarkDetailScreen(
                    items = MockData.bookmarkedArticles,
                    onClickItem = { selectedBookmarkedArticle = it }
                )
            }
        }

        MyPage.AI_STATUS -> {
            DetailScaffold(
                title = MyPage.AI_STATUS.title,
                onBack = { currentPage = MyPage.MENU }
            ) {
                AiStatusDetailScreen()
            }
        }
    }

    selectedBookmarkedArticle?.let { article ->
        ArticleBottomSheet(
            article = article,
            onDismissRequest = { selectedBookmarkedArticle = null }
        )
    }
}

@Composable
private fun MyMenuScreen(
    accountStatus: String,
    themeStatus: String,
    onClickAccount: () -> Unit,
    onClickTheme: () -> Unit,
    onClickSector: () -> Unit,
    onClickNewsCategory: () -> Unit,
    onClickBookmark: () -> Unit,
    onClickAiStatus: () -> Unit
) {
    val menuItems = listOf(
        MenuItem("계정", accountStatus, Icons.Filled.Person, onClickAccount),
        MenuItem("디스플레이", themeStatus, Icons.Filled.Settings, onClickTheme),
        MenuItem("관심 섹터", null, Icons.Filled.Topic, onClickSector),
        MenuItem("뉴스 수신 분야", null, Icons.Filled.Newspaper, onClickNewsCategory),
        MenuItem("저장한 기사", null, Icons.Filled.Bookmark, onClickBookmark),
        MenuItem("AI 연결 상태", null, Icons.Filled.Hub, onClickAiStatus)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = RowVertical)
    ) {
        item {
            menuItems.forEachIndexed { index, item ->
                MenuRow(item = item)
                if (index < menuItems.lastIndex) {
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                }
            }
        }
    }
}

private data class MenuItem(
    val title: String,
    val subtitle: String?,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
private fun MenuRow(item: MenuItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { item.onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Accent
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                item.subtitle?.let { subtitle ->
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextSecondary
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DetailScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로가기",
                        tint = TextPrimary
                    )
                }
            }
        )
        content()
    }
}

@Composable
private fun AccountDetailScreen(
    isLoggedIn: Boolean,
    userName: String,
    userEmail: String,
    onGoogleSignInClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isLoggedIn) {
            Button(
                onClick = onGoogleSignInClick,
                interactionSource = remember { MutableInteractionSource() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Google로 로그인")
            }
            Text(
                text = "로그인 기능은 현재 준비 중입니다.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Accent.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.toString() ?: "U",
                            color = Accent,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Column {
                        Text(userName, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Text(userEmail, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                OutlinedButton(
                    onClick = onSignOutClick,
                    interactionSource = remember { MutableInteractionSource() }
                ) { Text("로그아웃") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SectorDetailScreen(
    chips: List<String>,
    selectedValues: Set<String>,
    onToggle: (String) -> Unit
) {
    ChipGrid(chips = chips, selectedValues = selectedValues, onToggle = onToggle)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewsCategoryDetailScreen(
    chips: List<String>,
    selectedValues: Set<String>,
    onToggle: (String) -> Unit
) {
    ChipGrid(chips = chips, selectedValues = selectedValues, onToggle = onToggle)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGrid(
    chips: List<String>,
    selectedValues: Set<String>,
    onToggle: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { label ->
            val selected = label in selectedValues
            Surface(
                color = if (selected) Accent.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggle(label) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) Accent else TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BookmarkDetailScreen(
    items: List<Article>,
    onClickItem: (Article) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = RowVertical)
    ) {
        item {
            items.forEachIndexed { index, article ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onClickItem(article) }
                        .padding(vertical = RowVertical)
                ) {
                    Text(
                        text = article.headline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${article.source} · ${article.time}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                }
            }
        }
    }
}

@Composable
private fun AiStatusDetailScreen() {
    Column(modifier = Modifier.padding(16.dp)) {
        StatusRow(label = "뉴스 요약 AI", healthy = true)
        StatusRow(label = "시황 리포트 AI", healthy = true)
        StatusRow(label = "데이터 수집", healthy = true)
        Text(
            text = "오전 6:00 업데이트",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ThemeSettingDetailScreen(
    isDarkTheme: Boolean,
    onSelectDarkTheme: () -> Unit,
    onSelectLightTheme: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeOptionRow(
            label = "다크 모드",
            selected = isDarkTheme,
            onClick = onSelectDarkTheme
        )
        ThemeOptionRow(
            label = "라이트 모드",
            selected = !isDarkTheme,
            onClick = onSelectLightTheme
        )
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary
        )
        RadioButton(
            selected = selected,
            onClick = onClick,
            interactionSource = remember { MutableInteractionSource() }
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    healthy: Boolean
) {
    val dotColor = if (healthy) Color(0xFF22C55E) else Color(0xFFFF3B30)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RowVertical),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape)
            )
            Text(
                text = if (healthy) "정상" else "오류",
                style = MaterialTheme.typography.bodySmall,
                color = dotColor
            )
        }
    }
}
