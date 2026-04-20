package com.sapiens.app.ui.common

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.github.barteksc.pdfviewer.PDFView
import com.sapiens.app.ui.theme.Accent
import com.sapiens.app.ui.theme.Background
import com.sapiens.app.ui.theme.Card
import com.sapiens.app.ui.theme.ContentAlpha
import com.sapiens.app.ui.theme.Spacing
import com.sapiens.app.ui.theme.TextPrimary
import com.sapiens.app.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val pdfDownloadClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
}

/**
 * URL에서 PDF를 받아 인앱 [PDFView]로 표시. 다운로드 실패 시 외부 브라우저로 연 뒤 닫는다.
 */
@Composable
fun PdfViewerDialog(
    url: String,
    title: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var pdfBytes by remember(url) { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }

    fun openExternal(u: String) {
        val trimmed = u.trim()
        if (trimmed.isEmpty()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(trimmed)))
        }
    }

    LaunchedEffect(url) {
        pdfBytes = null
        isLoading = true
        val trimmed = url.trim()
        if (trimmed.isEmpty()) {
            isLoading = false
            openExternal(trimmed)
            onDismiss()
            return@LaunchedEffect
        }
        val bytes = withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder()
                    .url(trimmed)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
                    )
                    .build()
                pdfDownloadClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@runCatching null
                    resp.body?.bytes()
                }
            }.getOrNull()
        }
        if (bytes == null || bytes.isEmpty()) {
            isLoading = false
            openExternal(trimmed)
            onDismiss()
            return@LaunchedEffect
        }
        pdfBytes = bytes
        isLoading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Card),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.space16),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title.ifBlank { "리포트" },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = TextPrimary,
                    )
                }
            }
            HorizontalDivider(
                thickness = Spacing.hairline,
                color = TextSecondary.copy(alpha = ContentAlpha.hairlineOnSecondary),
            )
            when {
                pdfBytes != null -> {
                    val bytes = pdfBytes!!
                    key(url, bytes.size, bytes.contentHashCode()) {
                        AndroidView(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Background),
                            factory = { ctx ->
                                PDFView(ctx, null).apply {
                                    fromBytes(bytes)
                                        .enableSwipe(true)
                                        .swipeHorizontal(false)
                                        .enableDoubletap(true)
                                        .load()
                                }
                            },
                        )
                    }
                }
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Accent)
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "PDF를 불러올 수 없습니다",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
    }
}
