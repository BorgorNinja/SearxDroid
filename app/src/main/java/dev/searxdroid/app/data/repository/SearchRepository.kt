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
        ignoreUnknownKeys = true
        isLenient         = true
        coerceInputValues = true
    }

    /**
     * Execute a search against the given SearXNG instance.
     *
     * ## Per-candidate strategy
     *
     * 1. **Session warmup** — GET / to set cookies for bot-protection bypass.
     *    SearXNG's limiter checks for a `client_token` cookie that is only set
     *    after a real browser visits the homepage. Without it, cold requests
     *    receive an HTML challenge page instead of results.
     *
     * 2. **Try JSON** — request `?format=json`. If Content-Type is
     *    `application/json`, parse with kotlinx.serialization.
     *
     * 3. **HTML fallback** — if the instance has JSON disabled (Content-Type
     *    is `text/html`), re-request without a format parameter so SearXNG
     *    returns its full HTML results page, then parse with [SearxHtmlParser].
     *    Selectors are derived directly from SearXNG's simple-theme templates
     *    (macros.html, result_templates/images.html, result_templates/videos.html).
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
                // ── 0. Warm session ───────────────────────────────────────────
                // GET / to establish cookies before the first search request.
                // Cached per base URL so subsequent searches don't re-warm.
                SearxApiClient.warmupSession(url)

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

                // ── 2. HTML fallback ─────────────────────────────────────────
                // JSON is disabled on this instance (or the response was a
                // challenge page). Request without format= so SearXNG returns
                // its standard HTML search-results page.
                val htmlResp = service.search(
                    query      = query,
                    format     = null,   // omitted → SearXNG returns HTML
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
