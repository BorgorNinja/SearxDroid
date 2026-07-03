package dev.searxdroid.app.data.repository

import dev.searxdroid.app.data.model.SearxSearchResponse
import dev.searxdroid.app.data.network.SearxApiClient
import dev.searxdroid.app.data.network.SearxHtmlParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

sealed class SearchState {
    object Idle    : SearchState()
    object Loading : SearchState()
    data class Success(
        val response: SearxSearchResponse,
        val query: String,
        val instanceUrl: String,
    ) : SearchState()
    data class Error(val message: String, val retryable: Boolean = true) : SearchState()
}

class SearchRepository {

    private val json = Json {
        ignoreUnknownKeys  = true
        isLenient          = true
        coerceInputValues  = true
    }

    /**
     * Execute a search against the given SearXNG instance.
     *
     * ## Parsing strategy (per candidate URL)
     *
     * 1. Request `?format=json`.
     *    - If the response Content-Type is `application/json` → parse as JSON.
     *    - If the response is HTML (JSON API disabled on the instance) →
     *      make a **second** request *without* a format parameter so SearXNG
     *      returns its full HTML search-results page, then parse with
     *      [SearxHtmlParser] (Jsoup).
     * 2. Falls back through [fallbackInstances] if the primary URL fails
     *    entirely (network error, HTTP error, no results).
     */
    suspend fun search(
        query: String,
        instanceUrl: String,
        categories: String = "general",
        language: String = "en-US",
        safeSearch: Int = 0,
        page: Int = 1,
        timeRange: String? = null,
        fallbackInstances: List<String> = emptyList(),
    ): SearchState = withContext(Dispatchers.IO) {
        val candidates = listOf(instanceUrl) + fallbackInstances
        var lastError  = ""

        for (url in candidates) {
            try {
                val service = SearxApiClient.buildServiceFor(url)

                // ── 1. Try JSON ───────────────────────────────────────────────
                val jsonResp = service.search(
                    query      = query,
                    format     = "json",
                    categories = categories,
                    language   = language,
                    safeSearch = safeSearch,
                    pageNo     = page,
                    timeRange  = timeRange,
                )
                val jsonCt = jsonResp.headers()["Content-Type"] ?: ""

                if (jsonResp.isSuccessful && "json" in jsonCt) {
                    val body     = jsonResp.body()?.string() ?: ""
                    val response = json.decodeFromString<SearxSearchResponse>(body)
                    if (response.results.isNotEmpty() || page > 1) {
                        return@withContext SearchState.Success(response, query, url)
                    }
                    lastError = "No results from $url"
                    continue
                }

                // ── 2. JSON unavailable — fall back to HTML parsing ───────────
                // The JSON request returned HTML (instance has JSON disabled).
                // We must make a fresh request *without* format=json so SearXNG
                // renders its full search-results HTML rather than an error page.
                val htmlResp = service.search(
                    query      = query,
                    format     = null,   // null → Retrofit omits the parameter
                    categories = categories,
                    language   = language,
                    safeSearch = safeSearch,
                    pageNo     = page,
                    timeRange  = timeRange,
                )

                if (htmlResp.isSuccessful) {
                    val body     = htmlResp.body()?.string() ?: ""
                    val response = SearxHtmlParser.parse(body)
                    if (response.results.isNotEmpty() || page > 1) {
                        return@withContext SearchState.Success(response, query, url)
                    }
                    lastError = "No results from $url (HTML mode)"
                    continue
                }

                lastError = "HTTP ${htmlResp.code()} from $url"

            } catch (e: Exception) {
                lastError = (e.localizedMessage ?: "Unknown error")
                    .lines().first().take(120)
            }
        }

        SearchState.Error(lastError.ifBlank { "All instances failed" })
    }

    companion object {
        @Volatile private var INSTANCE: SearchRepository? = null
        fun getInstance() = INSTANCE ?: synchronized(this) {
            INSTANCE ?: SearchRepository().also { INSTANCE = it }
        }
    }
}
