package com.sapiens.app.data.stock

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sapiens.app.data.stock.dto.FinanceSummaryDto
import com.sapiens.app.data.stock.dto.NaverStockBasicDto
import com.sapiens.app.data.stock.dto.ResearchItemDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

interface StockDetailRepository {
    suspend fun loadStockDetail(code: String): StockDetailUi
}

class StockDetailRepositoryImpl(
    private val publicDataApiKey: String,
) : StockDetailRepository {

    override suspend fun loadStockDetail(code: String): StockDetailUi = withContext(Dispatchers.IO) {
        val normalized = code.trim().padStart(6, '0')
        val m = StockRetrofitProvider.mStock
        val researchApi = StockRetrofitProvider.stockNaverResearch
        val newsApi = StockRetrofitProvider.stockNaverNews
        val publicApi = StockRetrofitProvider.publicData

        val basic: NaverStockBasicDto = m.getBasic(normalized)
        val integrationMetrics = runCatching {
            parseIntegrationTotalInfos(m.getIntegration(normalized).string())
        }.getOrNull()

        val finance: FinanceSummaryDto = runCatching { m.getFinanceSummary(normalized) }
            .getOrElse { FinanceSummaryDto(null) }

        val publicMetrics = runCatching {
            if (publicDataApiKey.isBlank()) null
            else parsePublicPriceInfo(
                publicApi.getStockPriceInfo(
                    serviceKey = publicDataApiKey,
                    numOfRows = 1,
                    pageNo = 1,
                    resultType = "json",
                    likeSrtnCd = normalized
                ).string()
            )
        }.getOrNull()

        val marketCap = coalesceMetric(
            formatMarketSum(basic.marketSum),
            publicMetrics?.marketCap
        )
        val per = coalesceMetric(formatPer(basic.per), publicMetrics?.per)
        val pbr = coalesceMetric(formatPbr(basic.pbr), publicMetrics?.pbr)
        val eps = coalesceMetric(formatEps(basic.eps), publicMetrics?.eps)
        val dividendYield = coalesceMetric(
            formatDividendRate(basic.dividendRate),
            publicMetrics?.dividendYield
        )
        val dps = coalesceMetric(formatDividendWon(basic.dividend), publicMetrics?.dps)

        val reports: List<StockReportItem> = runCatching {
            researchApi.getResearch(normalized, page = 0, size = 16)
                .mapNotNull { it.toReportItem(normalized) }
                .take(3)
        }.getOrElse { emptyList() }

        val news: List<StockNewsItem> = loadNews(normalized, newsApi, m)

        val (financialYears, financialUnit) = parseFinancialYears(finance)

        StockDetailUi(
            code = normalized,
            stockName = basic.stockName.orEmpty().ifBlank { normalized },
            exchangeLabel = basic.stockExchangeType?.nameKor
                ?: basic.stockExchangeName
                ?: "—",
            closePrice = basic.closePrice.orEmpty().ifBlank { "—" },
            fluctuationsRatio = formatChangePercent(basic.fluctuationsRatio),
            isRising = when (basic.compareToPreviousPrice?.name?.uppercase()) {
                "RISING" -> true
                "FALLING" -> false
                else -> inferRisingFromRatio(basic.fluctuationsRatio)
            },
            marketCap = marketCap,
            per = per,
            pbr = pbr,
            eps = eps,
            dividendYield = dividendYield,
            dps = dps,
            financialValueUnit = financialUnit,
            financialYears = financialYears,
            reports = reports,
            news = news,
        )
    }

    private fun ResearchItemDto.toReportItem(code: String): StockReportItem? {
        val url = attachUrl?.trim().orEmpty()
        if (title.isNullOrBlank()) return null
        return StockReportItem(
            brokerName = brokerName.orEmpty().ifBlank { "—" },
            title = title.trim(),
            date = formatResearchDate(writeDate),
            goalPrice = goalPrice?.trim().orEmpty().ifBlank { "—" },
            opinion = opinion?.trim().orEmpty().ifBlank { "—" },
            url = url.ifBlank { "https://finance.naver.com/item/main.naver?code=$code" }
        )
    }

    private suspend fun loadNews(
        code: String,
        newsApi: com.sapiens.app.data.stock.api.StockNaverNewsApi,
        m: com.sapiens.app.data.stock.api.MStockApi
    ): List<StockNewsItem> {
        val fromStockNaver = runCatching {
            val body = newsApi.getDomesticNews(code, page = 0, size = 16).string()
            parseStockNaverNewsJson(body)
        }.getOrNull()
        if (!fromStockNaver.isNullOrEmpty()) return fromStockNaver.take(3)

        return runCatching {
            val body = m.getNewsByStock(code, page = 0, pageSize = 16).string()
            parseMStockNewsJson(body)
        }.getOrElse { emptyList() }.take(3)
    }

    private fun parseStockNaverNewsJson(json: String): List<StockNewsItem> {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return emptyList()
        val arr = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> root.asJsonObject.getAsJsonArray("items")
                ?: root.asJsonObject.getAsJsonArray("content")
            else -> return emptyList()
        } ?: return emptyList()
        return arr.mapNotNull { el ->
            if (!el.isJsonObject) return@mapNotNull null
            val o = el.asJsonObject
            val title = o.stringOr("title", "titl", "headline") ?: return@mapNotNull null
            StockNewsItem(
                officeName = o.stringOr("officeName", "pressNm", "source") ?: "—",
                title = title,
                date = formatNewsDatetime(o.stringOr("datetime", "dt", "pubDate")),
                thumbnailUrl = o.stringOr("imageOriginLink", "imageUrl", "thumbnailUrl").orEmpty(),
                url = o.stringOr("mobileNewsUrl", "link", "url").orEmpty()
            )
        }.filter { it.url.isNotBlank() }
            .take(3)
    }

    private fun parseMStockNewsJson(json: String): List<StockNewsItem> {
        val root = runCatching { JsonParser.parseString(json) }.getOrNull() ?: return emptyList()
        if (!root.isJsonArray) return emptyList()
        val out = mutableListOf<StockNewsItem>()
        for (group in root.asJsonArray) {
            if (!group.isJsonObject) continue
            val items = group.asJsonObject.getAsJsonArray("items") ?: continue
            for (el in items) {
                if (!el.isJsonObject) continue
                val o = el.asJsonObject
                val url = o.stringOr("mobileNewsUrl", "url").orEmpty()
                if (url.isBlank()) continue
                val title = o.stringOr("title", "titleFull").orEmpty()
                if (title.isBlank()) continue
                out.add(
                    StockNewsItem(
                        officeName = o.stringOr("officeName").orEmpty().ifBlank { "—" },
                        title = title,
                        date = formatNewsDatetime(o.stringOr("datetime")),
                        thumbnailUrl = o.stringOr("imageOriginLink").orEmpty(),
                        url = url
                    )
                )
            }
        }
        return out.take(3)
    }

    private data class PublicMetrics(
        val marketCap: String,
        val per: String,
        val pbr: String,
        val eps: String,
        val dividendYield: String,
        val dps: String,
    )

    /**
     * `m.stock.naver.com/api/stock/{code}/integration` 의 [totalInfos]에서 투자 지표 추출.
     * (basic JSON 루트에는 per 등이 없는 경우가 많음)
     */
    private data class IntegrationTotalInfos(
        val perValue: String?,
        val pbrValue: String?,
        val epsValue: String?,
        val dividendYieldValue: String?,
        val dividendValue: String?,
        val marketCapDisplay: String?,
    )

    private fun parseIntegrationTotalInfos(json: String): IntegrationTotalInfos? {
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: return null
        val arr = root.getAsJsonArray("totalInfos") ?: return null
        val byCode = mutableMapOf<String, String>()
        for (el in arr) {
            if (!el.isJsonObject) continue
            val o = el.asJsonObject
            val code = o.get("code")?.takeIf { !it.isJsonNull }?.asString?.trim().orEmpty()
            val value = o.get("value")?.takeIf { !it.isJsonNull }?.asString?.trim().orEmpty()
            if (code.isNotEmpty() && value.isNotEmpty()) {
                byCode[code] = value
            }
        }
        if (byCode.isEmpty()) return null
        return IntegrationTotalInfos(
            perValue = byCode["per"],
            pbrValue = byCode["pbr"],
            epsValue = byCode["eps"],
            dividendYieldValue = byCode["dividendYieldRatio"] ?: byCode["dividendRate"],
            dividendValue = byCode["dividend"],
            marketCapDisplay = formatMarketCapFromIntegrationValue(byCode["marketValue"]),
        )
    }

    /** 이미 "1,254조 268억" 형태면 끝에 ` 원`만 보강. */
    private fun formatMarketCapFromIntegrationValue(raw: String?): String? {
        val s = raw?.trim().orEmpty()
        if (s.isBlank()) return null
        return when {
            s.endsWith("원") -> s
            else -> "${s.trimEnd()} 원"
        }
    }

    private fun coalesceMetric(primary: String, secondary: String?): String {
        val p = primary.trim()
        if (p.isNotEmpty() && p != "—") return primary
        val s = secondary?.trim().orEmpty()
        return if (s.isNotEmpty() && s != "—") secondary!!.trim() else "—"
    }

    private fun coalesceMetricThree(primary: String, secondary: String?, tertiary: String?): String {
        val p = primary.trim()
        if (p.isNotEmpty() && p != "—") return primary
        val s = secondary?.trim().orEmpty()
        if (s.isNotEmpty() && s != "—") return secondary!!.trim()
        val t = tertiary?.trim().orEmpty()
        return if (t.isNotEmpty() && t != "—") tertiary!!.trim() else "—"
    }

    private fun normalizedNumericToken(raw: String?): String =
        raw?.trim().orEmpty()
            .removeSuffix("%")
            .replace(",", "")
            .replace(" ", "")
            .replace("원", "")
            .replace("배", "")
            .trim()

    /** null, 빈 문자열, "0", "0.00", "0.0" 등 → true (음수는 유효값으로 둠) */
    private fun isMissingOrZeroNumber(raw: String?): Boolean {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty()) return true
        if (t == "—" || t == "-") return true
        if (t.equals("null", ignoreCase = true)) return true
        if (t.equals("n/a", ignoreCase = true)) return true
        val n = normalizedNumericToken(raw)
        if (n.isEmpty()) return true
        val d = n.toDoubleOrNull() ?: return true
        if (d == 0.0) return true
        return kotlin.math.abs(d) < 1e-12
    }

    private fun parseFiniteDouble(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val n = normalizedNumericToken(raw)
        if (n.isEmpty()) return null
        return n.toDoubleOrNull()
    }

    /** PER — 소수 둘째자리 */
    private fun formatPer(raw: String?): String {
        if (isMissingOrZeroNumber(raw)) return "—"
        val d = parseFiniteDouble(raw) ?: return "—"
        return String.format(Locale.US, "%.2f", d)
    }

    /** PBR — 소수 둘째자리 */
    private fun formatPbr(raw: String?): String {
        if (isMissingOrZeroNumber(raw)) return "—"
        val d = parseFiniteDouble(raw) ?: return "—"
        return String.format(Locale.US, "%.2f", d)
    }

    /** EPS — 천단위 콤마 (소수 있으면 불필요한 0 제거) */
    private fun formatEps(raw: String?): String {
        if (isMissingOrZeroNumber(raw)) return "—"
        val d = parseFiniteDouble(raw) ?: return "—"
        val symbols = DecimalFormatSymbols(Locale.US)
        return if (kotlin.math.abs(d - d.toLong()) < 1e-6) {
            DecimalFormat("#,###", symbols).format(d.toLong())
        } else {
            DecimalFormat("#,##0.##", symbols).format(d).trimEnd('0').trimEnd('.')
        }
    }

    /** 배당수익률 — % (끝자리 0 정리) */
    private fun formatDividendRate(raw: String?): String {
        if (isMissingOrZeroNumber(raw)) return "—"
        val d = parseFiniteDouble(raw) ?: return "—"
        val s = String.format(Locale.US, "%.2f", d).trimEnd('0').trimEnd('.')
        return "${s}%"
    }

    /** 주당배당금 — 천단위 + "원" */
    private fun formatDividendWon(raw: String?): String {
        if (isMissingOrZeroNumber(raw)) return "—"
        val n = parseFiniteDouble(raw)?.toLong() ?: return "—"
        if (n <= 0L) return "—"
        val symbols = DecimalFormatSymbols(Locale.US)
        return "${DecimalFormat("#,###", symbols).format(n)}원"
    }

    /** 시가총액(marketSum) — 조·억 원 표기 */
    private fun formatMarketSum(raw: String?): String {
        if (raw.isNullOrBlank()) return "—"
        val normalized = raw.trim().replace(",", "").replace(" ", "")
        if (normalized.isEmpty()) return "—"
        val n = normalized.toLongOrNull()
        if (n != null) {
            if (n <= 0L) return "—"
            return formatMarketCapKoreanWon(raw.trim())
        }
        val d = normalized.toDoubleOrNull() ?: return "—"
        if (d <= 0.0) return "—"
        return formatMarketCapKoreanWon(d.toLong().toString())
    }

    private fun parsePublicPriceInfo(body: String): PublicMetrics? {
        val root = runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull() ?: return null
        val item = extractFirstItem(root) ?: return null
        return PublicMetrics(
            marketCap = formatMarketSum(pickRawString(item, "mrktTotAmt", "mrkttotamt", "hts_avls", "htsAvls")),
            per = formatPer(pickRawString(item, "per", "PER")),
            pbr = formatPbr(pickRawString(item, "pbr", "PBR")),
            eps = formatEps(pickRawString(item, "eps", "EPS")),
            dividendYield = formatDividendRate(
                pickRawString(item, "dvdYld", "dvd_yld", "divYld", "dividendRate")
            ),
            dps = formatDividendWon(
                pickRawString(item, "dps", "DPS", "stckDps", "dvdn", "stck_dvd_amt", "dividend")
            )
        )
    }

    private fun extractFirstItem(root: JsonObject): JsonObject? {
        val response = root.getAsJsonObject("response") ?: return null
        val body = response.getAsJsonObject("body") ?: return null
        val items = body.get("items") ?: return null
        if (items.isJsonNull) return null
        val itemsObj = items.asJsonObject
        val itemEl = itemsObj.get("item") ?: return null
        return when {
            itemEl.isJsonArray -> itemEl.asJsonArray.firstOrNull()?.asJsonObject
            itemEl.isJsonObject -> itemEl.asJsonObject
            else -> null
        }
    }

    /**
     * 네이버 모바일 재무 차트 `columns` — 0행이 헤더(매출·영업이익 라벨에 단위 포함 가능), 이후 연도별 값.
     * 값은 통상 **억원**; 헤더에 조·백만 등이 있으면 그에 맞춘다.
     */
    private fun parseFinancialYears(finance: FinanceSummaryDto): Pair<List<FinancialYearRow>, String> {
        val cols = finance.chartIncomeStatement?.annual?.columns ?: return emptyList<FinancialYearRow>() to ""
        if (cols.size < 3) return emptyList<FinancialYearRow>() to ""
        val unit = inferIncomeStatementUnitFromHeaders(cols)
        val periods = cols[0].drop(1)
        val revenues = cols[1].drop(1)
        val ops = cols[2].drop(1)
        val len = minOf(periods.size, revenues.size, ops.size)
        if (len == 0) return emptyList<FinancialYearRow>() to unit
        val excludeLastForecast = len >= 4
        val end = if (excludeLastForecast) len - 1 else len
        val start = maxOf(0, end - 3)
        val rows = (start until end).map { i ->
            FinancialYearRow(
                yearLabel = periods[i],
                revenue = formatIncomeStatementFigure(revenues[i], unit),
                operatingProfit = formatIncomeStatementFigure(ops[i], unit),
            )
        }
        return rows to unit
    }

    private fun inferIncomeStatementUnitFromHeaders(cols: List<List<String>>): String {
        val h1 = cols.getOrNull(1)?.firstOrNull().orEmpty()
        val h2 = cols.getOrNull(2)?.firstOrNull().orEmpty()
        val h = "$h1 $h2"
        return when {
            "조원" in h || "(조원)" in h || "조)" in h -> "조원"
            "백만원" in h || "백만" in h -> "백만원"
            "천원" in h -> "천원"
            "억원" in h || "(억원)" in h || "억)" in h -> "억원"
            else -> "억원"
        }
    }

    /** 정수 셀 + 단위 접미(예: `337,455억원`). 파싱 실패 시 원문 유지. */
    private fun formatIncomeStatementFigure(raw: String, unit: String): String {
        val base = formatBigNumber(raw)
        if (unit.isBlank() || base == raw.trim()) return base
        return "${base}${unit}"
    }

    private fun formatChangePercent(raw: String?): String {
        val s = raw?.trim().orEmpty()
        if (s.isBlank()) return "—"
        if (s.contains("%")) return s
        val n = s.replace(",", "").toDoubleOrNull() ?: return s
        val sign = when {
            n > 0 -> "+"
            n < 0 -> ""
            else -> ""
        }
        return "${sign}${String.format(java.util.Locale.US, "%.2f", kotlin.math.abs(n))}%"
    }

    private fun inferRisingFromRatio(raw: String?): Boolean? {
        val s = raw?.trim().orEmpty().replace("%", "").replace(",", "")
        val n = s.toDoubleOrNull() ?: return null
        return when {
            n > 0 -> true
            n < 0 -> false
            else -> null
        }
    }

    private fun formatResearchDate(raw: String?): String {
        val s = raw?.trim().orEmpty()
        if (s.length >= 10 && s[4] == '-' && s[7] == '-') return s.take(10)
        return s.ifBlank { "—" }
    }

    private fun formatNewsDatetime(raw: String?): String {
        val s = raw?.trim().orEmpty()
        if (s.length >= 12 && s.all { it.isDigit() }) {
            return "${s.substring(0, 4)}-${s.substring(4, 6)}-${s.substring(6, 8)} ${s.substring(8, 10)}:${s.substring(10, 12)}"
        }
        return s.ifBlank { "—" }
    }

    private fun JsonObject.stringOr(vararg keys: String): String? {
        for (k in keys) {
            if (!has(k)) continue
            val e = get(k)
            if (e == null || e.isJsonNull) continue
            if (e.isJsonPrimitive) {
                val p = e.asJsonPrimitive
                if (p.isString) return p.asString
                return p.toString().trim('"')
            }
        }
        return null
    }

    private fun pickRawString(item: JsonObject, vararg keys: String): String? {
        for (k in keys) {
            val v = item.stringOr(k)
            if (!v.isNullOrBlank()) return v
        }
        return null
    }

    /**
     * 원 단위 시가총액을 "9조 7,812억 원" 형태로 표시.
     * 1조=10^12, 1억=10^8 (정수 억만 표기, 나머지는 1억 미만이면 콤마+원).
     */
    private fun formatMarketCapKoreanWon(raw: String): String {
        val normalized = raw.trim().replace(",", "").replace(" ", "")
        if (normalized.isEmpty()) return "—"
        val n = normalized.toLongOrNull() ?: return "—"
        if (n <= 0L) return "—"
        val jo = n / 1_000_000_000_000L
        val afterJo = n % 1_000_000_000_000L
        val eok = afterJo / 100_000_000L
        val symbols = DecimalFormatSymbols(Locale.US)
        val fmt = DecimalFormat("#,###", symbols)
        val parts = mutableListOf<String>()
        if (jo > 0) parts.add("${fmt.format(jo)}조")
        if (eok > 0) parts.add("${fmt.format(eok)}억")
        return when {
            parts.isNotEmpty() -> parts.joinToString(" ") + " 원"
            else -> "${fmt.format(n)}원"
        }
    }

    private fun formatBigNumber(raw: String): String {
        val s = raw.trim().replace(",", "")
        val n = s.toLongOrNull() ?: return raw
        return DecimalFormat("#,###").format(n)
    }
}
