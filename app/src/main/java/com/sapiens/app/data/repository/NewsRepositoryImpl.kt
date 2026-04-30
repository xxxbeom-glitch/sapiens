package com.sapiens.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.sapiens.app.data.model.Article
import com.sapiens.app.data.model.BriefingCard
import com.sapiens.app.data.model.MarketDirection
import com.sapiens.app.data.model.MarketIndex
import com.sapiens.app.data.model.MarketIndexSnapshot
import com.sapiens.app.data.model.MarketTheme
import com.sapiens.app.data.mock.MockData
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.DecimalFormat
import java.util.Locale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject

/** Firestore `news` 문서 구독 시 탭당(국내·미국·AI) UI에 올릴 엄선 기사 최대 개수. */
private const val NEWS_FEED_MAX_ARTICLES = 15

class NewsRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val appContext: Context
) : NewsRepository {
    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_MARKET_CACHE, Context.MODE_PRIVATE)
    }

    constructor() : this(
        firestore = FirebaseFirestore.getInstance(FirebaseApp.getInstance(), DATABASE_ID),
        appContext = FirebaseApp.getInstance().applicationContext
    )

    override fun getRepresentativeIndices(): Flow<MarketIndexSnapshot> = flow {
        emit(loadRepresentativeIndices())
    }.flowOn(Dispatchers.IO)

    override fun getNewsFeed(type: NewsFeedType): Flow<List<Article>> = callbackFlow {
        val reg = firestore.collection(COLLECTION_NEWS).document(type.documentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(
                    snapshot.parseArticles("articles").take(NEWS_FEED_MAX_ARTICLES)
                )
            }
        awaitClose { reg.remove() }
    }

    override fun getNewsFeedDocument(documentId: String): Flow<List<Article>> = callbackFlow {
        val docId = documentId.trim()
        if (docId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg = firestore.collection(COLLECTION_NEWS).document(docId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snapshot.parseArticles("articles").take(NEWS_FEED_MAX_ARTICLES))
            }
        awaitClose { reg.remove() }
    }

    override fun getMarketThemes(): Flow<List<MarketTheme>> = callbackFlow {
        val coll = firestore.collection(COLLECTION_MARKET)
            .document(DOC_MARKET_THEMES_ROOT)
            .collection(SUB_MARKET_THEMES_BY_NO)
        val reg = coll.addSnapshotListener { qs, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            if (qs == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val sorted = qs.documents.sortedWith(
                compareBy<DocumentSnapshot> { it.getLong("rank") ?: Long.MAX_VALUE }
                    .thenBy { it.id }
            )
            trySend(sorted.mapNotNull { it.toMarketThemeDoc() })
        }
        awaitClose { reg.remove() }
    }

    override fun getBriefingCards(): Flow<List<BriefingCard>> = callbackFlow {
        val reg = firestore.collection(COLLECTION_BRIEFING_CARDS)
            .addSnapshotListener { qs, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                if (qs == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val cards = qs.documents
                    .mapNotNull { it.toBriefingCard() }
                    .sortedByDescending { it.generatedAt }
                trySend(cards)
            }
        awaitClose { reg.remove() }
    }

    private fun loadRepresentativeIndices(): MarketIndexSnapshot {
        val now = System.currentTimeMillis()
        val lastFetch = prefs.getLong(PREF_LAST_FETCH_MILLIS, 0L)
        val cached = readCachedIndices()
        val shouldFetch = lastFetch <= 0L || now - lastFetch >= FETCH_INTERVAL_MILLIS || cached.isEmpty()

        if (!shouldFetch && cached.isNotEmpty()) {
            return MarketIndexSnapshot(indices = cached, updatedAtMillis = lastFetch)
        }

        val fresh = fetchRepresentativeIndices()
        if (fresh.isNotEmpty()) {
            saveCachedIndices(fresh, now)
            return MarketIndexSnapshot(indices = fresh, updatedAtMillis = now)
        }

        if (cached.isNotEmpty()) {
            return MarketIndexSnapshot(indices = cached, updatedAtMillis = lastFetch)
        }
        return MarketIndexSnapshot(indices = MockData.MARKET_INDEX_LIST, updatedAtMillis = 0L)
    }

    private fun fetchRepresentativeIndices(): List<MarketIndex> {
        val result = mutableListOf<MarketIndex>()
        fetchDaumQuote(
            code = "K001",
            group = "국내",
            name = "코스피"
        )?.let(result::add)
        fetchDaumQuote(
            code = "K301",
            group = "국내",
            name = "코스닥"
        )?.let(result::add)

        val yahooSymbols = listOf("^GSPC", "^IXIC", "^DJI", "GC=F", "CL=F", "KRW=X")
        val symbolMeta = mapOf(
            "^GSPC" to ("미국" to "S&P 500"),
            "^IXIC" to ("미국" to "나스닥"),
            "^DJI" to ("미국" to "다우존스"),
            "GC=F" to ("원자재" to "금"),
            "CL=F" to ("원자재" to "WTI 유가"),
            "KRW=X" to ("환율" to "달러/원")
        )
        result += fetchYahooQuotes(yahooSymbols, symbolMeta)
        return result
    }

    private fun fetchDaumQuote(code: String, group: String, name: String): MarketIndex? {
        val json = requestJson(
            url = "https://finance.daum.net/api/quotes/$code?summary=false&changeRatioSign=false",
            headers = mapOf("Referer" to "https://finance.daum.net")
        ) ?: return null

        val tradePrice = json.optDouble("tradePrice", Double.NaN)
            .takeIf { !it.isNaN() }
            ?: json.optDouble("closePrice", Double.NaN)
                .takeIf { !it.isNaN() }
            ?: return null
        val changePrice = json.optDouble("changePrice", 0.0)
        val changeRate = normalizePercent(json.optDouble("changeRate", 0.0))
        val prevClose = json.optDouble("prevClosingPrice", Double.NaN)
            .takeIf { !it.isNaN() }
            ?: (tradePrice - changePrice)

        return MarketIndex(
            group = group,
            name = name,
            value = formatValue(tradePrice),
            change = formatPercent(changeRate),
            prevValue = formatValue(prevClose),
            direction = toDirection(changePrice)
        )
    }

    private fun fetchYahooQuotes(
        symbols: List<String>,
        symbolMeta: Map<String, Pair<String, String>>
    ): List<MarketIndex> {
        if (symbols.isEmpty()) return emptyList()
        val joined = URLEncoder.encode(symbols.joinToString(","), "UTF-8")
        val json = requestJson("https://query1.finance.yahoo.com/v7/finance/quote?symbols=$joined")
            ?: return emptyList()
        val arr = json.optJSONObject("quoteResponse")?.optJSONArray("result") ?: JSONArray()
        val out = mutableListOf<MarketIndex>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val symbol = obj.optString("symbol").orEmpty()
            val (group, name) = symbolMeta[symbol] ?: continue
            val price = obj.optDouble("regularMarketPrice", Double.NaN)
            if (price.isNaN()) continue
            val change = obj.optDouble("regularMarketChange", 0.0)
            val changePercent = normalizePercent(obj.optDouble("regularMarketChangePercent", 0.0))
            val prevClose = obj.optDouble("regularMarketPreviousClose", Double.NaN)
                .takeIf { !it.isNaN() }
                ?: (price - change)
            out += MarketIndex(
                group = group,
                name = name,
                value = formatValue(price),
                change = formatPercent(changePercent),
                prevValue = formatValue(prevClose),
                direction = toDirection(change)
            )
        }
        return out
    }

    private fun requestJson(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): JSONObject? {
        val conn = (URL(url).openConnection() as? HttpURLConnection) ?: return null
        return try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.inputStream.bufferedReader().use { reader ->
                JSONObject(reader.readText())
            }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private fun saveCachedIndices(indices: List<MarketIndex>, fetchedAt: Long) {
        val arr = JSONArray()
        indices.forEach { index ->
            arr.put(
                JSONObject().apply {
                    put("group", index.group)
                    put("name", index.name)
                    put("value", index.value)
                    put("change", index.change)
                    put("prevValue", index.prevValue)
                    put("direction", index.direction.name)
                }
            )
        }
        prefs.edit()
            .putLong(PREF_LAST_FETCH_MILLIS, fetchedAt)
            .putString(PREF_CACHED_INDICES, arr.toString())
            .apply()
    }

    private fun readCachedIndices(): List<MarketIndex> {
        val raw = prefs.getString(PREF_CACHED_INDICES, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val group = obj.optString("group")
                    val name = obj.optString("name")
                    val value = obj.optString("value")
                    val change = obj.optString("change")
                    val prev = obj.optString("prevValue")
                    if (group.isBlank() || name.isBlank()) continue
                    val direction = when (obj.optString("direction").uppercase(Locale.US)) {
                        MarketDirection.UP.name -> MarketDirection.UP
                        MarketDirection.DOWN.name -> MarketDirection.DOWN
                        else -> MarketDirection.FLAT
                    }
                    add(
                        MarketIndex(
                            group = group,
                            name = name,
                            value = value,
                            change = change,
                            prevValue = prev,
                            direction = direction
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun normalizePercent(raw: Double): Double {
        val abs = kotlin.math.abs(raw)
        return if (abs <= 1.0) raw * 100.0 else raw
    }

    private fun toDirection(delta: Double): MarketDirection = when {
        delta > 0 -> MarketDirection.UP
        delta < 0 -> MarketDirection.DOWN
        else -> MarketDirection.FLAT
    }

    private fun formatPercent(percent: Double): String {
        val symbol = when {
            percent > 0 -> "+"
            percent < 0 -> "-"
            else -> ""
        }
        val formatted = DecimalFormat("#,##0.00").format(kotlin.math.abs(percent))
        return "$symbol$formatted%"
    }

    private fun formatValue(value: Double): String {
        val pattern = if (kotlin.math.abs(value) >= 1000) "#,##0.00" else "0.00"
        return DecimalFormat(pattern).format(value)
    }

    companion object {
        private const val DATABASE_ID = "sapiens"
        private const val COLLECTION_NEWS = "news"
        private const val COLLECTION_MARKET = "market"
        /** `market/themes/by_no/{theme_no}` 부모 문서. */
        private const val DOC_MARKET_THEMES_ROOT = "themes"
        private const val SUB_MARKET_THEMES_BY_NO = "by_no"
        private const val PREFS_MARKET_CACHE = "market_index_cache"
        private const val PREF_LAST_FETCH_MILLIS = "last_fetch_millis"
        private const val PREF_CACHED_INDICES = "cached_indices_json"
        private const val COLLECTION_BRIEFING_CARDS = "briefing_cards"
        private const val FETCH_INTERVAL_MILLIS = 3 * 60 * 60 * 1000L
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}

// ── Firestore 파서 ────────────────────────────────────────────────────────────

@Suppress("UNCHECKED_CAST")
internal fun DocumentSnapshot.toBriefingCard(): BriefingCard? {
    return try {
        BriefingCard(
            cardId      = id,
            category    = getString("category").orEmpty(),
            moneyFlow   = getString("money_flow").orEmpty(),
            marketStatus = getString("market_status").orEmpty(),
            keyReasons  = (get("key_reasons") as? List<*>)
                ?.mapNotNull { it as? String } ?: emptyList(),
            investPoint = getString("invest_point").orEmpty(),
            tags        = (get("tags") as? List<*>)
                ?.mapNotNull { it as? String } ?: emptyList(),
            generatedAt = getString("generated_at").orEmpty(),
        )
    } catch (e: Exception) {
        null
    }
}
