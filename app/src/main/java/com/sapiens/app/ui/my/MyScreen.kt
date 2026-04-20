package com.sapiens.app.ui.my

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.sapiens.app.R
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.stableId
import com.sapiens.app.data.repository.AiConfigFirestoreRepository
import com.sapiens.app.data.repository.FeedbackRepository
import com.sapiens.app.data.store.ArticleBookmarksRepository
import com.sapiens.app.data.store.BookmarkEntry
import com.sapiens.app.data.store.BookmarkToggleResult
import com.sapiens.app.data.store.UserPreferencesRepository
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.CardPaddingBottom
import com.sapiens.app.ui.theme.CardPaddingHorizontal
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.Primary
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val domesticNewsOptions = listOf(
    "경제", "정치", "국제", "IT·테크", "부동산", "증권", "산업", "사회"
)

private val overseasNewsOptions = listOf(
    "증시", "테크·반도체", "매크로", "환율", "금리", "원자재", "채권", "암호화폐"
)

private enum class ExpandableSection {
    NEWS_CATEGORY,
    BOOKMARK,
    API_STATUS
}

@Composable
fun MyScreen(
    authViewModel: AuthViewModel,
    bookmarksRepository: ArticleBookmarksRepository,
    feedbackRepository: FeedbackRepository
) {
    val context = LocalContext.current
    val preferencesRepository = remember { UserPreferencesRepository(context) }
    val aiConfigRepository = remember { AiConfigFirestoreRepository() }
    val authUser by authViewModel.authUser.collectAsState()
    val signInError by authViewModel.signInError.collectAsState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        authViewModel.onGoogleSignInActivityResult(result.data)
    }

    LaunchedEffect(signInError) {
        val msg = signInError ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        authViewModel.clearSignInError()
    }

    val selectedDomestic by preferencesRepository.selectedDomesticNewsCategoriesFlow.collectAsState(initial = emptySet())
    val selectedOverseas by preferencesRepository.selectedOverseasNewsCategoriesFlow.collectAsState(initial = emptySet())
    val apiClaudeEnabled by preferencesRepository.apiClaudeEnabledFlow.collectAsState(initial = true)
    val apiGeminiEnabled by preferencesRepository.apiGeminiEnabledFlow.collectAsState(initial = true)

    val bookmarkEntries by bookmarksRepository.bookmarksFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    var expandedSection by remember { mutableStateOf<ExpandableSection?>(null) }
    var selectedBookmarkedArticle by remember { mutableStateOf<Article?>(null) }

    val sortedBookmarks = remember(bookmarkEntries) {
        bookmarkEntries.sortedByDescending { it.savedAtMillis }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(vertical = RowVertical)
    ) {
        AccountMenuRow(
            subtitle = authUser?.email?.let { email -> "$email 로그인 됨" } ?: "로그인이 필요합니다",
            canOpenGoogleSignIn = authUser == null,
            onClick = {
                googleSignInLauncher.launch(authViewModel.googleSignInIntent())
            }
        )

        if (authUser != null) {
            TextButton(
                onClick = { authViewModel.signOut() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.space16)
            ) {
                Text("로그아웃", color = TextSecondary)
            }
        }

        ExpandableMenuCard(
            title = "뉴스 수신 분야",
            subtitle = domesticSubtitle(selectedDomestic) + " · " + overseasSubtitle(selectedOverseas),
            iconRes = R.drawable.ico_my_news_category,
            expanded = expandedSection == ExpandableSection.NEWS_CATEGORY,
            onToggleExpand = {
                expandedSection =
                    if (expandedSection == ExpandableSection.NEWS_CATEGORY) null else ExpandableSection.NEWS_CATEGORY
            }
        ) {
            NewsReceiveCategoriesPanel(
                domesticSelected = selectedDomestic,
                overseasSelected = selectedOverseas,
                onToggleDomestic = { v ->
                    coroutineScope.launch { preferencesRepository.toggleDomesticNewsCategory(v) }
                },
                onToggleOverseas = { v ->
                    coroutineScope.launch { preferencesRepository.toggleOverseasNewsCategory(v) }
                }
            )
        }

        ExpandableMenuCard(
            title = "저장한 기사",
            subtitle = if (sortedBookmarks.isEmpty()) "없음" else "${sortedBookmarks.size}건",
            iconRes = R.drawable.ico_my_bookmark,
            expanded = expandedSection == ExpandableSection.BOOKMARK,
            onToggleExpand = {
                expandedSection =
                    if (expandedSection == ExpandableSection.BOOKMARK) null else ExpandableSection.BOOKMARK
            }
        ) {
            BookmarkPagerPanel(
                entries = sortedBookmarks,
                onClickItem = { selectedBookmarkedArticle = it.article }
            )
        }

        ExpandableMenuCard(
            title = "API 연결 상태",
            subtitle = null,
            iconRes = R.drawable.ico_my_ai_status,
            expanded = expandedSection == ExpandableSection.API_STATUS,
            onToggleExpand = {
                expandedSection =
                    if (expandedSection == ExpandableSection.API_STATUS) null else ExpandableSection.API_STATUS
            }
        ) {
            ApiConnectionPanel(
                claudeEnabled = apiClaudeEnabled,
                geminiEnabled = apiGeminiEnabled,
                onClaudeChange = { v ->
                    coroutineScope.launch {
                        preferencesRepository.setApiClaudeEnabled(v)
                        aiConfigRepository.save(claudeEnabled = v, geminiEnabled = apiGeminiEnabled)
                    }
                },
                onGeminiChange = { v ->
                    coroutineScope.launch {
                        preferencesRepository.setApiGeminiEnabled(v)
                        aiConfigRepository.save(claudeEnabled = apiClaudeEnabled, geminiEnabled = v)
                    }
                }
            )
        }
    }

    selectedBookmarkedArticle?.let { article ->
        val bookmarked = bookmarkEntries.any { it.article.stableId() == article.stableId() }
        ArticleBottomSheet(
            article = article,
            onDismissRequest = { selectedBookmarkedArticle = null },
            isBookmarked = bookmarked,
            onBookmarkToggle = {
                coroutineScope.launch {
                    val id = article.stableId()
                    try {
                        when (
                            val r = bookmarksRepository.toggleBookmark(
                                article,
                                withFeedbackWhenAdding = false
                            )
                        ) {
                            is BookmarkToggleResult.Added ->
                                if (r.withFeedbackSync) {
                                    feedbackRepository.saveArticleLike(id, article.category)
                                }
                            is BookmarkToggleResult.Removed ->
                                if (r.hadFeedbackSync) {
                                    feedbackRepository.deleteFeedback(id)
                                }
                        }
                    } catch (e: Exception) {
                        Log.w("MyScreen", "bookmark/feedback", e)
                    }
                }
            }
        )
    }
}

private fun domesticSubtitle(set: Set<String>): String =
    if (set.isEmpty()) "국내 선택 없음" else "국내 ${set.size}개"

private fun overseasSubtitle(set: Set<String>): String =
    if (set.isEmpty()) "해외 선택 없음" else "해외 ${set.size}개"

@Composable
private fun AccountMenuRow(
    subtitle: String,
    canOpenGoogleSignIn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space16, vertical = Spacing.space6),
        shape = AppShapes.card,
        color = Card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = canOpenGoogleSignIn,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
                .padding(horizontal = CardPaddingHorizontal, vertical = RowVertical),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space12),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ico_my_account),
                    contentDescription = "계정",
                    modifier = Modifier.size(Spacing.space28),
                    tint = Primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(
                        text = "계정",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Google 간편 로그인",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun NewsReceiveCategoriesPanel(
    domesticSelected: Set<String>,
    overseasSelected: Set<String>,
    onToggleDomestic: (String) -> Unit,
    onToggleOverseas: (String) -> Unit
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxWidth()) {
        PrimaryTabRow(selectedTabIndex = tabIndex) {
            Tab(
                selected = tabIndex == 0,
                onClick = { tabIndex = 0 },
                text = { Text("국내 뉴스", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = { tabIndex = 1 },
                text = { Text("해외 뉴스", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
        Spacer(modifier = Modifier.height(Spacing.space12))
        if (tabIndex == 0) {
            CheckboxCategoryList(
                labels = domesticNewsOptions,
                selected = domesticSelected,
                onToggle = onToggleDomestic
            )
        } else {
            CheckboxCategoryList(
                labels = overseasNewsOptions,
                selected = overseasSelected,
                onToggle = onToggleOverseas
            )
        }
    }
}

@Composable
private fun CheckboxCategoryList(
    labels: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4)
    ) {
        labels.forEach { label ->
            val checked = label in selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onToggle(label) }
                    .padding(vertical = Spacing.space6),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.space8)
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onToggle(label) }
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BookmarkPagerPanel(
    entries: List<BookmarkEntry>,
    onClickItem: (BookmarkEntry) -> Unit
) {
    if (entries.isEmpty()) {
        Text(
            text = "저장한 기사가 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        return
    }
    val chunks = remember(entries) { entries.chunked(5) }
    val pagerState = rememberPagerState(pageCount = { chunks.size })
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) { page ->
            Column(modifier = Modifier.fillMaxWidth()) {
                chunks[page].forEachIndexed { index, entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onClickItem(entry) }
                            .padding(vertical = Spacing.space10)
                    ) {
                        Text(
                            text = entry.article.headline,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${entry.article.source} · ${entry.article.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (index < chunks[page].lastIndex) {
                        HorizontalDivider(
                            thickness = Spacing.hairline,
                            color = TextSecondary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
        if (chunks.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.space12),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(chunks.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = Spacing.space3)
                            .height(Spacing.space6)
                            .width(if (selected) Spacing.space20 else Spacing.space6)
                            .clip(CircleShape)
                            .background(
                                if (selected) Primary else TextSecondary.copy(alpha = 0.4f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiConnectionPanel(
    claudeEnabled: Boolean,
    geminiEnabled: Boolean,
    onClaudeChange: (Boolean) -> Unit,
    onGeminiChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.space12)
    ) {
        ApiProviderRow(
            label = "Claude",
            enabled = claudeEnabled,
            onEnabledChange = onClaudeChange
        )
        ApiProviderRow(
            label = "Gemini",
            enabled = geminiEnabled,
            onEnabledChange = onGeminiChange
        )
    }
}

@Composable
private fun ApiProviderRow(
    label: String,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.space10)
        ) {
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
            ConnectionStatusChip(connected = enabled)
        }
    }
}

@Composable
private fun ConnectionStatusChip(connected: Boolean) {
    val (text, bg, fg) = if (connected) {
        Triple(
            "연결",
            Accent.copy(alpha = 0.14f),
            Accent
        )
    } else {
        Triple(
            "오류",
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.error
        )
    }
    Surface(
        color = bg,
        shape = AppShapes.chipTight
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = Spacing.space8, vertical = Spacing.space4)
        )
    }
}

@Composable
private fun ExpandableMenuCard(
    title: String,
    subtitle: String?,
    @DrawableRes iconRes: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.space16, vertical = Spacing.space6),
        shape = AppShapes.card,
        color = Card
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onToggleExpand() }
                    .padding(horizontal = CardPaddingHorizontal, vertical = RowVertical),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.space12),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = title,
                        modifier = Modifier.size(Spacing.space28),
                        tint = Primary
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        subtitle?.let { sub ->
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "접기" else "펼치기",
                    tint = TextSecondary
                )
            }
            if (expanded) {
                HorizontalDivider(
                    thickness = Spacing.hairline,
                    color = TextSecondary.copy(alpha = 0.2f)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = CardPaddingHorizontal,
                            end = CardPaddingHorizontal,
                            top = Spacing.space12,
                            bottom = CardPaddingBottom
                        )
                ) {
                    content()
                }
            }
        }
    }
}
