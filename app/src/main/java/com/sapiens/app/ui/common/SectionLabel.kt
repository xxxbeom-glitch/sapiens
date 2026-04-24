package com.sapiens.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary

@Composable
fun SectionLabel(
    title: String,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier.padding(horizontal = Spacing.space20, vertical = Spacing.space12)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.padding(top = Spacing.space4)
            )
        }
    }
}
