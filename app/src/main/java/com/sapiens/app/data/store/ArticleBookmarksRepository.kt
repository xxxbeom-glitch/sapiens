package com.sapiens.app.data.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.stableId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val BOOKMARKS_STORE = "sapiens_bookmarks"
private val Context.bookmarksDataStore by preferencesDataStore(name = BOOKMARKS_STORE)

private val bookmarksJsonKey = stringPreferencesKey("bookmarks_json")

data class BookmarkEntry(
    val article: Article,
    /** 뉴스 탭에서 북마크해 `feedback` 문서를 함께 쓴 경우만 true */
    val withFeedbackSync: Boolean,
    /** 저장 시각(epoch ms). 마이 탭 정렬·표시용. */
    val savedAtMillis: Long = System.currentTimeMillis()
)

sealed class BookmarkToggleResult {
    data class Added(val withFeedbackSync: Boolean) : BookmarkToggleResult()
    data class Removed(val hadFeedbackSync: Boolean) : BookmarkToggleResult()
}

class ArticleBookmarksRepository(
    private val context: Context,
) {

    val bookmarksFlow: Flow<List<BookmarkEntry>> = context.bookmarksDataStore.data.map { prefs ->
        parseBookmarksJson(prefs[bookmarksJsonKey].orEmpty())
    }

    suspend fun isBookmarked(articleId: String): Boolean {
        val list = bookmarksFlow.first()
        return list.any { it.article.stableId() == articleId }
    }

    suspend fun toggleBookmark(
        article: Article,
        withFeedbackWhenAdding: Boolean
    ): BookmarkToggleResult {
        val id = article.stableId()
        val raw = context.bookmarksDataStore.data.map { it[bookmarksJsonKey].orEmpty() }.first()
        val list = parseBookmarksJson(raw).toMutableList()
        val idx = list.indexOfFirst { it.article.stableId() == id }
        return if (idx >= 0) {
            val had = list[idx].withFeedbackSync
            list.removeAt(idx)
            persist(list)
            BookmarkToggleResult.Removed(hadFeedbackSync = had)
        } else {
            list.add(
                0,
                BookmarkEntry(
                    article = article,
                    withFeedbackSync = withFeedbackWhenAdding,
                    savedAtMillis = System.currentTimeMillis()
                )
            )
            persist(list)
            BookmarkToggleResult.Added(withFeedbackSync = withFeedbackWhenAdding)
        }
    }

    private suspend fun persist(entries: List<BookmarkEntry>) {
        context.bookmarksDataStore.edit { prefs ->
            prefs[bookmarksJsonKey] = serializeBookmarks(entries)
        }
    }

    suspend fun getRawBookmarksJson(): String =
        context.bookmarksDataStore.data.first()[bookmarksJsonKey].orEmpty()

    suspend fun setRawBookmarksJson(json: String) {
        context.bookmarksDataStore.edit { prefs ->
            prefs[bookmarksJsonKey] = json
        }
    }

    suspend fun hasBookmarks(): Boolean = bookmarksFlow.first().isNotEmpty()

    private companion object {
        private const val TAG = "ArticleBookmarks"
    }
}

private fun articleToJson(a: Article): JSONObject =
    JSONObject().apply {
        put("source", a.source)
        put("headline", a.headline)
        put("summary", a.summary)
        put("time", a.time)
        put("category", a.category)
        put("tag", a.tag)
        a.sourceColor?.let { put("sourceColor", it) }
        put("imageUrl", a.imageUrl)
        put("thumbnailUrl", a.thumbnailUrl)
        put("url", a.url)
        put("summaryPoints", JSONArray(a.summaryPoints))
    }

private fun jsonToArticle(o: JSONObject): Article? {
    return try {
        val headline = o.optString("headline", "").trim()
        if (headline.isBlank()) return null
        val spArr = o.optJSONArray("summaryPoints")
        val points = buildList {
            if (spArr != null) {
                for (i in 0 until spArr.length()) {
                    spArr.optString(i).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }
        Article(
            source = o.optString("source", ""),
            headline = headline,
            summary = o.optString("summary", ""),
            time = o.optString("time", ""),
            category = o.optString("category", ""),
            summaryPoints = points,
            tag = o.optString("tag", ""),
            sourceColor = o.optString("sourceColor", "").trim().ifBlank { null },
            imageUrl = o.optString("imageUrl", ""),
            thumbnailUrl = o.optString("thumbnailUrl", ""),
            url = o.optString("url", "").trim()
        )
    } catch (_: Exception) {
        null
    }
}

private fun serializeBookmarks(entries: List<BookmarkEntry>): String {
    val arr = JSONArray()
    for (e in entries) {
        arr.put(
            JSONObject().apply {
                put("withFeedbackSync", e.withFeedbackSync)
                put("savedAtMillis", e.savedAtMillis)
                put("article", articleToJson(e.article))
            }
        )
    }
    return arr.toString()
}

private fun parseBookmarksJson(raw: String): List<BookmarkEntry> {
    if (raw.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(raw)
        val now = System.currentTimeMillis()
        buildList {
            for (i in 0 until arr.length()) {
                val wrap = arr.optJSONObject(i) ?: continue
                val art = wrap.optJSONObject("article")?.let(::jsonToArticle) ?: continue
                val sync = wrap.optBoolean("withFeedbackSync", false)
                val saved = wrap.optLong("savedAtMillis", 0L)
                val savedAtMillis = if (saved > 0L) saved else now - i * 1000L
                add(BookmarkEntry(article = art, withFeedbackSync = sync, savedAtMillis = savedAtMillis))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
