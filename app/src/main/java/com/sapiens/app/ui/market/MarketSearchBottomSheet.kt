package com.sapiens.app.ui.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import com.sapiens.app.data.model.Company
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.AppShapes
import com.sapiens.app.ui.theme.BottomSheetBottomPadding
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.Elevated
import com.sapiens.app.ui.theme.SheetHorizontal
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketSearchBottomSheet(
    allCompanies: List<Company>,
    addedTickers: Set<String> = emptySet(),
    onAddCompany: ((Company) -> Unit)? = null,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            newValue != SheetValue.PartiallyExpanded &&
                newValue != SheetValue.Hidden
        }
    )
    val focusRequester = remember { FocusRequester() }
    var query by remember { mutableStateOf("") }

    val filteredCompanies = remember(query, allCompanies) {
        val keyword = query.trim()
        if (keyword.isBlank()) {
            allCompanies
        } else {
            allCompanies.filter { company ->
                company.name.contains(keyword, ignoreCase = true) ||
                    company.ticker.contains(keyword, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        containerColor = Card,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = BottomSheetBottomPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SheetHorizontal, vertical = Spacing.space16),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "마켓 검색",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(Spacing.space24)
                        .clip(CircleShape)
                        .background(TextSecondary.copy(alpha = 0.15f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onDismissRequest() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = TextPrimary,
                        modifier = Modifier.size(Spacing.space14)
                    )
                }
            }

            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SheetHorizontal)
                    .background(
                        color = Elevated,
                        shape = AppShapes.searchField
                    )
                    .padding(horizontal = Spacing.space16, vertical = Spacing.space12)
                    .focusRequester(focusRequester),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            "기업명 또는 티커 검색",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    innerTextField()
                },
                singleLine = true,
                cursorBrush = SolidColor(Accent)
            )

            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }

            val showEmptyResult = query.isNotBlank() && filteredCompanies.isEmpty()

            if (showEmptyResult) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "일치하는 정보가 없습니다",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = Spacing.space12, start = SheetHorizontal, end = SheetHorizontal)
                ) {
                    items(
                        items = filteredCompanies,
                        key = { it.ticker }
                    ) { company ->
                        SearchResultRow(
                            company = company,
                            isAdded = company.ticker in addedTickers,
                            showAdd = onAddCompany != null,
                            onClickAdd = { onAddCompany?.invoke(company) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    company: Company,
    isAdded: Boolean,
    showAdd: Boolean,
    onClickAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.space10),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.space2)
        ) {
            Text(
                text = company.name,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = company.ticker,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }

        if (showAdd) {
            TextButton(
                onClick = onClickAdd,
                interactionSource = remember { MutableInteractionSource() },
                enabled = !isAdded
            ) {
                Text(
                    text = if (isAdded) "추가됨" else "추가",
                    color = if (isAdded) TextSecondary else Accent
                )
            }
        }
    }
}
