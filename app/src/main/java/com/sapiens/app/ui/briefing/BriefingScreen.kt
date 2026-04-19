package com.sapiens.app.ui.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.CardSpacing
import com.sapiens.app.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun BriefingScreen(viewModel: BriefingViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val morningArticles by viewModel.morningArticles.collectAsState()
    val usArticles by viewModel.usArticles.collectAsState()
    val marketIndices by viewModel.marketIndices.collectAsState()
    val marketUpdatedLabel by viewModel.marketUpdatedLabel.collectAsState()

    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    val nowMillis = remember { System.currentTimeMillis() }
    val sortedMorningArticles = remember(morningArticles, nowMillis) {
        morningArticles
            .mapIndexed { index, article ->
                val normalized = if (article.source.isBlank()) article.copy(source = "기타") else article
                normalized to parsePublishedAtToMillis(normalized.time, nowMillis, index)
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(8)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                item {
                    SectionLabel(
                        title = "국내 주요뉴스"
                    )
                }

                item {
                    MorningTopArticlesCard(
                        articles = sortedMorningArticles,
                        onClickArticle = { selectedArticle = it },
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    SectionLabel(title = "해외 주요뉴스")
                }

                item {
                    USMajorArticlesCard(
                        articles = usArticles,
                        onClickArticle = { selectedArticle = it }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(CardSpacing))
                    SectionLabel(title = "대표 지수")
                }

                item {
                    MarketIndexGrid(
                        indices = marketIndices
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = marketUpdatedLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        selectedArticle?.let { article ->
            ArticleBottomSheet(
                article = article,
                onDismissRequest = { selectedArticle = null }
            )
        }
    }
}

private fun parsePublishedAtToMillis(timeRaw: String, nowMillis: Long, index: Int): Long {
    val raw = timeRaw.trim()
    if (raw.isEmpty()) return Long.MIN_VALUE + index

    val minutesAgo = Regex("""(\d+)\s*분\s*전""").find(raw)?.groupValues?.get(1)?.toLongOrNull()
    if (minutesAgo != null) return nowMillis - minutesAgo * 60_000L

    val hoursAgo = Regex("""(\d+)\s*시간\s*전""").find(raw)?.groupValues?.get(1)?.toLongOrNull()
    if (hoursAgo != null) return nowMillis - hoursAgo * 3_600_000L

    if (raw.contains("방금")) return nowMillis
    if (raw == "오늘") return nowMillis - 1L

    val kst = ZoneId.of("Asia/Seoul")
    val timeMatch = Regex("""(오전|오후)\s*(\d{1,2}):(\d{2})""").find(raw)
    if (timeMatch != null) {
        val period = timeMatch.groupValues[1]
        val hourBase = timeMatch.groupValues[2].toIntOrNull() ?: 0
        val minute = timeMatch.groupValues[3].toIntOrNull() ?: 0
        val hour = when {
            period == "오전" && hourBase == 12 -> 0
            period == "오전" -> hourBase
            period == "오후" && hourBase == 12 -> 12
            else -> hourBase + 12
        }
        val date = LocalDate.now(kst)
        return LocalDateTime.of(date, LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)))
            .atZone(kst)
            .toInstant()
            .toEpochMilli()
    }

    val dateMatch = Regex("""(20\d{2})[./-](\d{1,2})[./-](\d{1,2})""").find(raw)
    if (dateMatch != null) {
        val y = dateMatch.groupValues[1].toIntOrNull()
        val m = dateMatch.groupValues[2].toIntOrNull()
        val d = dateMatch.groupValues[3].toIntOrNull()
        if (y != null && m != null && d != null) {
            return LocalDate.of(y, m.coerceIn(1, 12), d.coerceIn(1, 28))
                .atStartOfDay(kst)
                .toInstant()
                .toEpochMilli()
        }
    }

    return Long.MIN_VALUE + index
}
