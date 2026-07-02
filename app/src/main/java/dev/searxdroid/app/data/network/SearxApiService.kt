package dev.searxdroid.app.data.network

import dev.searxdroid.app.data.model.SearxSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** Retrofit interface matching the SearXNG JSON search API. */
interface SearxApiService {
    /**
     * Performs a search against the SearXNG `/search` endpoint.
     *
     * @param query      The search query string.
     * @param format     Must be "json" for machine-readable output.
     * @param categories Comma-separated category list (e.g. "general,news").
     * @param language   Language code e.g. "en-US" or "all".
     * @param safeSearch 0 = off, 1 = moderate, 2 = strict.
     * @param pageNo     1-based result page number.
     * @param timeRange  "day","week","month","year", or null for no filter.
     */
    @GET("search")
    suspend fun search(
        @Query("q")          query: String,
        @Query("format")     format: String = "json",
        @Query("categories") categories: String = "general",
        @Query("language")   language: String = "en-US",
        @Query("safesearch") safeSearch: Int = 0,
        @Query("pageno")     pageNo: Int = 1,
        @Query("time_range") timeRange: String? = null,
    ): SearxSearchResponse
}
