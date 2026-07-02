package dev.searxdroid.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Maps to the SearXNG JSON API `/search?format=json` response. */
@Serializable
data class SearxSearchResponse(
    val results: List<SearxResult> = emptyList(),
    val answers: List<String> = emptyList(),
    val corrections: List<String> = emptyList(),
    val infoboxes: List<SearxInfobox> = emptyList(),
    val suggestions: List<String> = emptyList(),
    @SerialName("number_of_results") val numberOfResults: Long = 0,
    val query: String = "",
)

@Serializable
data class SearxResult(
    val url: String,
    val title: String,
    val content: String = "",
    val engine: String = "",
    val engines: List<String> = emptyList(),
    val category: String = "general",
    val score: Float = 0f,
    val template: String = "default.html",
    @SerialName("parsed_url") val parsedUrl: List<String>? = null,
    @SerialName("img_src")       val imgSrc: String? = null,
    @SerialName("thumbnail_src") val thumbnailSrc: String? = null,
    @SerialName("publishedDate") val publishedDate: String? = null,
    val author: String? = null,
    val length: String? = null,
) {
    /** Friendly domain extracted from parsedUrl or url. */
    val displayDomain: String get() {
        parsedUrl?.getOrNull(1)?.let { return it }
        return try {
            val h = url.removePrefix("https://").removePrefix("http://")
            h.substringBefore("/")
        } catch (_: Exception) { url }
    }
}

@Serializable
data class SearxInfobox(
    val infobox: String = "",
    val content: String = "",
    val urls: List<SearxInfoboxUrl> = emptyList(),
)

@Serializable
data class SearxInfoboxUrl(
    val title: String = "",
    val url: String = "",
)
