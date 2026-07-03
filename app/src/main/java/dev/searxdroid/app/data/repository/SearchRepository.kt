package dev.searxdroid.app.data.repository

import dev.searxdroid.app.data.model.SearxResult
import dev.searxdroid.app.data.model.SearxSearchResponse
import dev.searxdroid.app.data.network.SearxApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    /**
     * Execute a search against the given SearXNG instance.
     * Falls back through [fallbackInstances] if the primary fails.
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
        var lastError = ""

        for (url in candidates) {
            try {
                val service = SearxApiClient.buildServiceFor(url)
                val response = service.search(
                    query       = query,
                    format      = "json",
                    categories  = categories,
                    language    = language,
                    safeSearch  = safeSearch,
                    pageNo      = page,
                    timeRange   = timeRange,
                )
                if (response.results.isNotEmpty() || page > 1) {
                    return@withContext SearchState.Success(response, query, url)
                }
                // Empty but no error — try next instance
                lastError = "No results from $url"
            } catch (e: Exception) {
                lastError = (e.localizedMessage ?: "Unknown error").lines().first().take(120)
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
