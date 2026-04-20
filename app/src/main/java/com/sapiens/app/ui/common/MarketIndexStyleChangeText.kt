package com.sapiens.app.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.sapiens.app.data.model.MarketDirection
import com.sapiens.app.data.model.toSignedChangePercent
import com.sapiens.app.ui.theme.MarketDown
import com.sapiens.app.ui.theme.MarketFlat
import com.sapiens.app.ui.theme.MarketUp

private fun directionFromChangePercent(change: String): MarketDirection {
    val v = change.toSignedChangePercent()
    return when {
        v > 0.0 -> MarketDirection.UP
        v < 0.0 -> MarketDirection.DOWN
        else -> MarketDirection.FLAT
    }
}

/**
 * 브리핑「대표 지수」카드(`MarketIndexCard`)의 등락 텍스트와 동일한 타이포·색 규칙.
 * [direction]이 null이면 [change] 문자열을 `toSignedChangePercent()`로 파싱해 방향을 추정한다.
 */
@Composable
fun MarketIndexStyleChangeText(
    change: String,
    modifier: Modifier = Modifier,
    direction: MarketDirection? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val resolved = direction ?: directionFromChangePercent(change)
    val directionColor = when (resolved) {
        MarketDirection.UP -> MarketUp
        MarketDirection.DOWN -> MarketDown
        MarketDirection.FLAT -> MarketFlat
    }
    Text(
        text = change,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = directionColor,
        maxLines = maxLines,
        overflow = overflow
    )
}
