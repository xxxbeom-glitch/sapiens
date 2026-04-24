package com.sapiens.app.ui.common

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Seoul: ZoneId = ZoneId.of("Asia/Seoul")
private val DisplayFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MM.dd HH:mm", Locale.ROOT)

/**
 * Firestore·파이프라인의 ISO-8601(`2026-04-24T02:31:27+00:00` 등)을 KST `MM.dd HH:mm`로 표시.
 * 상대시각·이미 짧은 문자열 등 파싱 불가 시 원문을 그대로 둔다.
 */
fun articleTimeForDisplay(raw: String): String {
    val t = raw.trim()
    if (t.isBlank()) return t
    return runCatching {
        val instant = try {
            OffsetDateTime.parse(t).toInstant()
        } catch (_: Exception) {
            try {
                ZonedDateTime.parse(t).toInstant()
            } catch (_: Exception) {
                Instant.parse(t)
            }
        }
        instant.atZone(Seoul).format(DisplayFmt)
    }.getOrDefault(raw)
}
