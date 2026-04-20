package com.sapiens.app.ui.my

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.sapiens.app.BuildConfig
import com.sapiens.app.R
import com.sapiens.app.data.model.AiSelectedModel
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.stableId
import com.sapiens.app.data.repository.AiConfigFirestoreRepository
import com.sapiens.app.data.repository.FeedbackRepository
import com.sapiens.app.data.store.ArticleBookmarksRepository
import com.sapiens.app.data.store.BookmarkEntry
import com.sapiens.app.data.store.BookmarkToggleResult
import com.sapiens.app.data.store.UserPreferencesRepository
import com.sapiens.app.messaging.FcmTopicSync
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.common.ArticleBottomSheetKind
import com.sapiens.app.ui.common.transformNaverFinanceNewsReadUrlForMobile
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.CardPaddingBottom
import com.sapiens.app.ui.theme.OnPrimaryFixed
import com.sapiens.app.ui.theme.CardPaddingHorizontal
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.Primary
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class ExpandableSection {
    BOOKMARK,
    API_STATUS,
    SETTINGS,
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

    val apiSelectedModel by preferencesRepository.apiSelectedModelFlow.collectAsState(
        initial = AiSelectedModel.GEMINI
    )
    val pushNotificationsEnabled by preferencesRepository.pushNotificationsEnabledFlow.collectAsState(
        initial = true
    )

    val bookmarkEntries by bookmarksRepository.bookmarksFlow.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            coroutineScope.launch {
                preferencesRepository.setPushNotificationsEnabled(true)
                FcmTopicSync.subscribeAll()
            }
        } else {
            Toast.makeText(
                context,
                "알림을 받으려면 설정에서 알림 권한을 허용해 주세요.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    var expandedSection by remember { mutableStateOf<ExpandableSection?>(null) }
    var selectedBookmarkedArticle by remember { mutableStateOf<Article?>(null) }

    var draftAiModel by remember { mutableStateOf(AiSelectedModel.GEMINI) }

    LaunchedEffect(expandedSection == ExpandableSection.API_STATUS) {
        if (expandedSection == ExpandableSection.API_STATUS) {
            draftAiModel = preferencesRepository.apiSelectedModelFlow.first()
        }
    }

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

        val notifPermitted = FcmTopicSync.hasNotificationPermission(context)
        val pushSubtitle = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> "브리핑·뉴스·마켓 알림"
            pushNotificationsEnabled && !notifPermitted ->
                "설정에서 알림 권한을 허용해 주세요"
            else -> "브리핑·뉴스·마켓 알림"
        }
        PushNotificationMenuRow(
            subtitle = pushSubtitle,
            checked = pushNotificationsEnabled,
            onCheckedChange = { wantOn ->
                if (wantOn) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        when (
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                        ) {
                            PackageManager.PERMISSION_GRANTED -> {
                                coroutineScope.launch {
                                    preferencesRepository.setPushNotificationsEnabled(true)
                                    FcmTopicSync.subscribeAll()
                                }
                            }
                            else -> {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            preferencesRepository.setPushNotificationsEnabled(true)
                            FcmTopicSync.subscribeAll()
                        }
                    }
                } else {
                    coroutineScope.launch {
                        preferencesRepository.setPushNotificationsEnabled(false)
                        FcmTopicSync.unsubscribeAll()
                    }
                }
            },
        )

        ExpandableMenuCard(
            title = "설정",
            subtitle = "앱 정보",
            iconRes = R.drawable.ic_app_brand_0z,
            expanded = expandedSection == ExpandableSection.SETTINGS,
            onToggleExpand = {
                expandedSection =
                    if (expandedSection == ExpandableSection.SETTINGS) null else ExpandableSection.SETTINGS
            },
        ) {
            SettingsAppInfoPanel(versionName = BuildConfig.VERSION_NAME)
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
            AiModelSelectionPanel(
                selectedModel = draftAiModel,
                onSelectModel = { model -> draftAiModel = model }
            )
            Spacer(modifier = Modifier.height(Spacing.space12))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val normalized = AiSelectedModel.normalize(draftAiModel)
                        preferencesRepository.setApiSelectedModel(normalized)
                        aiConfigRepository.save(selectedModel = normalized)
                        Toast.makeText(context, "저장했습니다", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.button,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    contentColor = OnPrimaryFixed
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = Spacing.space0,
                    pressedElevation = Spacing.space0
                )
            ) {
                Text(
                    text = "저장",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
            },
            kind = ArticleBottomSheetKind.News,
            onOpenOriginalArticle = {
                val url = article.url.trim()
                if (url.isNotBlank()) {
                    val toOpen = transformNaverFinanceNewsReadUrlForMobile(url)
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(toOpen))
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun SettingsAppInfoPanel(versionName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space16),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_app_brand_0z),
            contentDescription = "앱 아이콘",
            modifier = Modifier.size(Spacing.space64),
            tint = Primary,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.space4),
        ) {
            Text(
                text = "Sapiens",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "버전 $versionName",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun PushNotificationMenuRow(
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
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
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "푸시 알림",
                    modifier = Modifier.size(Spacing.space28),
                    tint = Primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.space2)) {
                    Text(
                        text = "푸시 알림",
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
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnPrimaryFixed,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = TextSecondary.copy(alpha = 0.35f),
                ),
            )
        }
    }
}

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
private fun AiModelSelectionPanel(
    selectedModel: String,
    onSelectModel: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.space4)
    ) {
        AiModelRadioRow(
            title = "Claude (Haiku)",
            subtitle = null,
            selected = selectedModel == AiSelectedModel.CLAUDE,
            onSelect = { onSelectModel(AiSelectedModel.CLAUDE) }
        )
        AiModelRadioRow(
            title = "Gemini",
            subtitle = "기본값",
            selected = selectedModel == AiSelectedModel.GEMINI,
            onSelect = { onSelectModel(AiSelectedModel.GEMINI) }
        )
    }
}

@Composable
private fun AiModelRadioRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .clickable(onClick = onSelect)
            .padding(vertical = Spacing.space8, horizontal = Spacing.space4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.space10)
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = Primary,
                unselectedColor = TextSecondary
            )
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
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
