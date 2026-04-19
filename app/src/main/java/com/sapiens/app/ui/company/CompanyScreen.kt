package com.sapiens.app.ui.company

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.sapiens.app.data.mock.MockData
import com.sapiens.app.data.model.Company
import com.sapiens.app.data.model.MarketTheme
import com.sapiens.app.data.model.ThemeStock
import com.sapiens.app.data.model.logoUrl
import com.sapiens.app.data.model.stocksForDisplay
import com.sapiens.app.ui.common.CompanyBottomSheet
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.CardPaddingHorizontal
import com.sapiens.app.ui.theme.RowVertical
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

private val companyTabs = listOf("국내", "해외")
private val domesticTickers = setOf("005930", "000660", "035420", "051910")
private val foreignTickers = setOf("NVDA", "AAPL", "TSLA")

private val rateDownBlue = Color(0xFF007AFF)

@Composable
fun CompanyScreen(
    viewModel: CompanyViewModel,
    addedCompanies: List<Company>
) {
    val marketThemes by viewModel.marketThemes.collectAsState()
    var selectedCompany by remember { mutableStateOf<Company?>(null) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val baseForeign = MockData.companyList.filter { it.ticker in foreignTickers }
    val addedForeign = addedCompanies.filter { !isDomesticCompany(it) }
    val foreignItems = (baseForeign + addedForeign).distinctBy { it.ticker }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Background,
            contentColor = Accent
        ) {
            companyTabs.forEachIndexed { index, tab ->
                val selected = selectedTabIndex == index
                Tab(
                    selected = selected,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = tab,
                            color = if (selected) TextPrimary else TextSecondary
                        )
                    }
                )
            }
        }

        if (selectedTabIndex == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = RowVertical),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = marketThemes,
                    key = { it.themeName }
                ) { theme ->
                    DomesticThemeCard(
                        theme = theme,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = RowVertical)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .background(Card, RoundedCornerShape(18.dp))
                    ) {
                        foreignItems.forEachIndexed { index, company ->
                            CompanyRow(
                                company = company,
                                onClick = { selectedCompany = company }
                            )
                            if (index < foreignItems.lastIndex) {
                                HorizontalDivider(
                                    color = TextSecondary.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedCompany?.let { company ->
        CompanyBottomSheet(
            company = company,
            onDismissRequest = { selectedCompany = null }
        )
    }
}

@Composable
private fun rememberSvgImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
}

@Composable
private fun DomesticThemeCard(
    theme: MarketTheme,
    modifier: Modifier = Modifier
) {
    val svgLoader = rememberSvgImageLoader()
    val displayStocks = theme.stocksForDisplay()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Card,
        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CardPaddingHorizontal, vertical = RowVertical)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${theme.themeName}",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = theme.changeRate,
                    style = MaterialTheme.typography.titleSmall,
                    color = rateTextColor(theme.changeRate),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                )
            }

            if (displayStocks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                displayStocks.forEachIndexed { index, stock ->
                    ThemeStockRow(stock = stock, imageLoader = svgLoader)
                    if (index < displayStocks.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = TextSecondary.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeStockRow(
    stock: ThemeStock,
    imageLoader: ImageLoader
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            val logo = stock.logoUrl()
            if (logo.isNotEmpty()) {
                AsyncImage(
                    model = logo,
                    contentDescription = stock.name,
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TextSecondary.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stock.name.firstOrNull()?.toString().orEmpty().ifBlank { "·" },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = stock.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stock.price,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = stock.change,
                style = MaterialTheme.typography.labelMedium,
                color = rateTextColor(stock.change)
            )
        }
    }
}

@Composable
private fun rateTextColor(rate: String): Color {
    val t = rate.trim()
    return when {
        t.startsWith("+") -> MaterialTheme.colorScheme.error
        t.startsWith("-") || t.startsWith("−") || t.startsWith("–") -> rateDownBlue
        else -> TextSecondary
    }
}

private fun isDomesticCompany(company: Company): Boolean {
    return company.ticker in domesticTickers || company.ticker.all(Char::isDigit)
}

@Composable
private fun CompanyRow(
    company: Company,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(
                horizontal = CardPaddingHorizontal,
                vertical = RowVertical
            ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = company.name,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            SectorChip(label = company.sector)
        }

        Text(
            text = company.description,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SectorChip(label: String) {
    val (backgroundColor, textColor) = companySectorChipColors(label)
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

private fun companySectorChipColors(sector: String): Pair<Color, Color> = when (sector.trim()) {
    "반도체" -> Color(0xFF1A1F2F) to Color(0xFF5B8DEF)
    "AI/반도체" -> Color(0xFF1A2035) to Color(0xFF6B9DF5)
    "인터넷" -> Color(0xFF1A2230) to Color(0xFF7BAAF5)
    "플랫폼/디바이스" -> Color(0xFF1B2135) to Color(0xFF5F95F0)
    "2차전지/화학" -> Color(0xFF1A2538) to Color(0xFF70A8F5)
    "전기차" -> Color(0xFF1C2338) to Color(0xFF6AA0F0)
    else -> Color(0xFF1A2030) to Color(0xFF7EB0F5)
}
