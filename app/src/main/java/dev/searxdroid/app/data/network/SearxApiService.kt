package dev.searxdroid.app.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for the SearXNG /search endpoint.
 *
 * Returns [Response<ResponseBody>] so the caller can inspect the Content-Type
 * header and decide whether to parse the body as JSON or as HTML.
 */
interface SearxApiService {

    /**
     * Performs a search against the SearXNG `/search` endpoint.
     *
     * @param query      The search query string.
     * @param format     "json" for JSON API output; **null** to omit the parameter
     *                   entirely so SearXNG returns its standard HTML page.
     *                   Retrofit automatically drops null @Query parameters.
     * @param categories Comma-separated category list (e.g. "general", "images").
     * @param language   Language code e.g. "en-US" or "all".
     * @param safeSearch 0 = off, 1 = moderate, 2 = strict.
     * @param pageNo     1-based result page number.
     * @param timeRange  "day", "week", "month", "year", or null for no filter.
     */
    @GET("search")
    suspend fun search(
        @Query("q")          query: String,
        @Query("format")     format: String? = "json",
        @Query("categories") categories: String = "general",
        @Query("language")   language: String = "en-US",
        @Query("safesearch") safeSearch: Int = 0,
        @Query("pageno")     pageNo: Int = 1,
        @Query("time_range") timeRange: String? = null,
    ): Response<ResponseBody>
}
