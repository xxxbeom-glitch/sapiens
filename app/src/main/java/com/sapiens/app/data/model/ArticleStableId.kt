package com.sapiens.app.data.model

import java.security.MessageDigest

/** Firestore `feedback/{id}` 등 클라이언트 측 안정 키용 (파이프라인 `id` 필드 없을 때). */
fun Article.stableId(): String {
    val raw = "${source.trim()}|${time.trim()}|${headline.trim()}"
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(raw.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { b -> "%02x".format(b) }
}
