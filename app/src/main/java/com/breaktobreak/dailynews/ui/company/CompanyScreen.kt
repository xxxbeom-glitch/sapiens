package com.breaktobreak.dailynews.ui.company

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.breaktobreak.dailynews.data.mock.MockData
import com.breaktobreak.dailynews.data.model.Company
import com.breaktobreak.dailynews.ui.common.CompanyBottomSheet
import com.breaktobreak.dailynews.ui.theme.Background
import com.breaktobreak.dailynews.ui.theme.Card
import com.breaktobreak.dailynews.ui.theme.CardPaddingHorizontal
import com.breaktobreak.dailynews.ui.theme.RowVertical
import com.breaktobreak.dailynews.ui.theme.TextPrimary
import com.breaktobreak.dailynews.ui.theme.TextSecondary

@Composable
fun CompanyScreen() {
    var selectedCompany by remember { mutableStateOf<Company?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = RowVertical)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(Card, RoundedCornerShape(18.dp))
            ) {
                MockData.companyList.forEachIndexed { index, company ->
                    CompanyRow(
                        company = company,
                        onClick = { selectedCompany = company }
                    )
                    if (index < MockData.companyList.lastIndex) {
                        HorizontalDivider(
                            color = TextSecondary.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 16.dp)
                        )
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
private fun CompanyRow(
    company: Company,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
