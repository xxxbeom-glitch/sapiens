package com.sapiens.app.ui.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.sapiens.app.data.model.Article
import com.sapiens.app.ui.common.ArticleBottomSheet
import com.sapiens.app.ui.common.ArticleMixedFeedCard
import com.sapiens.app.ui.common.transformNaverFinanceNewsReadUrlForMobile
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.SapiensTextStyles
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary

private data class UsCategory(
    val label: String,
    val rssUrl: String,
)

private val usCategories = listOf(
    UsCategory(
        label = "소프트웨어 및 인터넷",
        rssUrl = "https://finance.yahoo.com/rss/headline?s=AAPL,MSFT,GOOGL,META,AMZN,CRM,NOW,ADBE,ORCL,FIG,PLTR,SNOW"
    ),
    UsCategory(
        label = "반도체 및 하드웨어",
        rssUrl = "https://finance.yahoo.com/rss/headline?s=NVDA,AMD,TSM,INTC,ASML,LRCX,ARM,QCOM,AVGO,ANET,MU,WDC,STX,SNDK"
    ),
    UsCategory(
        label = "항공우주 및 모빌리티",
        rssUrl = "https://finance.yahoo.com/rss/headline?s=LMT,BA,NOC,RKLB,ASTS,LUNR,PL,BKSY,IRDM,SPCE,TSLA"
    ),
    UsCategory(
        label = "금융 및 자본 시장",
        rssUrl = "https://finance.yahoo.com/rss/headline?s=JPM,GS,MS,BLK,BRK-B,BX,KKR"
    ),
)

private data class KrCategory(
    val label: String,
    val keywords: List<String>,
)

private val krCategories = listOf(
    KrCategory(
        label = "국내증시",
        keywords = listOf(
            "코스닥", "코스피", "삼성전자", "SK하이닉스", "국내 증시", "오전장", "오후장",
            "사이드카", "서킷브레이크", "현대차",
        )
    ),
    KrCategory(
        label = "반도체 및 하드웨어",
        keywords = listOf(
            "삼성전자(005930)", "SK하이닉스(000660)", "한미반도체(042700)", "HPSP(403870)",
            "이오테크닉스(039200)", "리노공업(058470)", "파운드리", "HBM", "D램", "낸드",
            "웨이퍼", "패키징", "유리기판",
        )
    ),
    KrCategory(
        label = "금융",
        keywords = listOf(
            "KB금융(105560)", "신한지주(055550)", "하나금융지주(086790)", "우리금융지주(316140)",
            "메리츠금융지주(138040)", "미래에셋증권(006800)",
        )
    ),
    KrCategory(
        label = "조선 및 해양",
        keywords = listOf(
            "HD한국조선해양(009540)", "삼성중공업(010140)", "한화오션(042660)", "HD현대미포(010620)",
            "HD현대중공업(329180)", "NG선", "암모니아 운반선", "친환경 선박", "해양 플랜트",
        )
    ),
    KrCategory(
        label = "에너지 및 전력 인프라",
        keywords = listOf(
            "HD현대일렉트릭(267260)", "LS일렉트릭(010120)", "두산에너빌리티(034020)", "씨에스윈드(112610)",
            "한국전력(015760)", "SMR(소형모듈원전)", "원전", "원자력", "데이터센터 전력",
            "신재생 에너지", "초고압 변압기",
        )
    ),
    KrCategory(
        label = "AI 및 로봇",
        keywords = listOf(
            "네이버(035420)", "카카오(035720)", "두산로보틱스(454910)", "레인보우로보틱스(277810)",
            "엔젤로보틱스(455900)",
        )
    ),
)

@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val krRssNews by viewModel.krRssNews.collectAsState()
    val usRssNews by viewModel.usRssNews.collectAsState()

    /** (기사, 뉴스 탭 페이지 0·1·2) — 바텀시트 등에서 탭 컨텍스트가 필요할 때 사용 */
    var sheetArticleAndPage by remember { mutableStateOf<Pair<Article, Int>?>(null) }
    val context = LocalContext.current
    var regionChipIndex by remember { mutableIntStateOf(0) }
    var secondDepthTabIndex by remember { mutableIntStateOf(0) }
    val secondDepthLabels = remember(regionChipIndex) {
        if (regionChipIndex == 1) {
            usCategories.map { it.label }
        } else {
            krCategories.map { it.label }
        }
    }

    androidx.compose.runtime.LaunchedEffect(regionChipIndex) {
        secondDepthTabIndex = 0
    }

    androidx.compose.runtime.LaunchedEffect(regionChipIndex, secondDepthTabIndex) {
        if (regionChipIndex == 1) {
            val idx = secondDepthTabIndex.coerceIn(0, usCategories.lastIndex)
            viewModel.setUsRssUrl(usCategories[idx].rssUrl)
        } else {
            val idx = secondDepthTabIndex.coerceIn(0, krCategories.lastIndex)
            viewModel.setKrKeywords(krCategories[idx].keywords)
        }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(top = Spacing.space36)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Spacing.space20,
                            end = Spacing.space20,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "#오늘의 헤드라인",
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = Spacing.space12),
                        color = TextPrimary,
                        style = SapiensTextStyles.todayHeadlineTitle,
                        maxLines = 2,
                    )
                    KoreaUsRegionChips(
                        selectedIndex = regionChipIndex,
                        onSelect = { regionChipIndex = it },
                    )
                }

                Spacer(Modifier.height(Spacing.space28))

                NewsSecondDepthTabRow(
                    labels = secondDepthLabels,
                    selectedIndex = secondDepthTabIndex,
                    onSelect = { secondDepthTabIndex = it },
                    modifier = Modifier.padding(bottom = Spacing.space18),
                )
                val pageItems = if (regionChipIndex == 1) usRssNews else krRssNews
                ArticleMixedFeedCard(
                    articles = pageItems,
                    onClickArticle = { sheetArticleAndPage = it to 0 },
                    modifier = Modifier
                        .fillMaxSize()
                        // 하단 앱바(Scaffold bottomBar) 위에서 16dp 정도 여유.
                        .padding(bottom = Spacing.space16),
                    topChipForArticle = { article ->
                        val chip = newsPublisherChipText(article.source)
                        when {
                            chip.isNotBlank() -> chip
                            regionChipIndex == 1 -> "해외"
                            else -> "국내"
                        }
                    },
                    emptyStateText = "불러온 기사가 없습니다."
                )
            }
        }

        sheetArticleAndPage?.let { (article, _) ->
            ArticleBottomSheet(
                article = article,
                onDismissRequest = { sheetArticleAndPage = null },
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
}
