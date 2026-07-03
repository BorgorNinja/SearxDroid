package dev.searxdroid.app.data.network

import dev.searxdroid.app.data.model.SearxResult
import dev.searxdroid.app.data.model.SearxSearchResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Parses the HTML output of a SearXNG instance into a [SearxSearchResponse].
 *
 * Targets the SearXNG "simple" theme (the default since ~2022).
 * Key selectors are tried with multiple fallback variants so the parser
 * remains useful across minor SearXNG version differences.
 *
 * Used automatically by [SearchRepository] when an instance has the JSON
 * API disabled and returns HTML instead.
 */
object SearxHtmlParser {

    fun parse(html: String): SearxSearchResponse {
        val doc = Jsoup.parse(html)

        // ── Results ──────────────────────────────────────────────────────────
        val results = doc.select("article.result[data-url]").mapNotNull { el ->
            runCatching { parseArticle(el) }.getOrNull()
        }

        // ── Direct answers ────────────────────────────────────────────────────
        val answers = doc.select("#answers .answer, #answers li")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }

        // ── Suggestions ───────────────────────────────────────────────────────
        val suggestions = doc
            .select(".suggestion a, #suggestions a, #sidebar .suggestion a")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // ── Result count ──────────────────────────────────────────────────────
        val countRaw = doc
            .select("#result_count, #number_of_results, .result-count")
            .firstOrNull()?.text() ?: ""
        val count = Regex("[0-9][0-9,.]*")
            .find(countRaw)
            ?.value
            ?.replace(Regex("[,.]"), "")
            ?.toLongOrNull()
            ?: results.size.toLong()

        return SearxSearchResponse(
            results          = results,
            answers          = answers,
            suggestions      = suggestions,
            numberOfResults  = count,
        )
    }

    // ── Article dispatch ──────────────────────────────────────────────────────

    private fun parseArticle(el: Element): SearxResult? {
        val url = el.attr("data-url").takeIf { it.isNotBlank() } ?: return null

        // data-tpl is "default" / "images" / "videos"; fall back to CSS class
        val tpl = el.attr("data-tpl").ifBlank {
            when {
                el.hasClass("result-images") -> "images"
                el.hasClass("result-videos") -> "videos"
                else                         -> "default"
            }
        }

        val engines = el
            .select("p.engines span, .engines span, .result-engines span")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }

        return when (tpl) {
            "images" -> parseImageArticle(el, url, engines)
            "videos" -> parseVideoArticle(el, url, engines)
            else     -> parseDefaultArticle(el, url, engines)
        }
    }

    // ── Template parsers ──────────────────────────────────────────────────────

    private fun parseDefaultArticle(
        el: Element,
        url: String,
        engines: List<String>,
    ): SearxResult {
        val title = el
            .select("h3.result_title a, h3 a, .result_header h3 a, h3")
            .firstOrNull()?.text()?.trim() ?: ""

        val content = el
            .select("p.content, .result-content")
            .firstOrNull()?.text()?.trim() ?: ""

        val date = el.select("time[datetime]")
            .attr("datetime")
            .takeIf { it.isNotBlank() }

        return SearxResult(
            url           = url,
            title         = title,
            content       = content,
            engine        = engines.firstOrNull() ?: "",
            engines       = engines,
            category      = "general",
            template      = "default.html",
            publishedDate = date,
        )
    }

    private fun parseImageArticle(
        el: Element,
        url: String,
        engines: List<String>,
    ): SearxResult {
        val title = el
            .select(
                "span.title, .result_images-detail span.title, " +
                ".result_header a, h3 a",
            )
            .firstOrNull()?.text()?.trim() ?: ""

        // Try the dedicated image-container img first, then any img in the article
        val thumbSrc = el
            .select(
                "div.result_images-img img[src], " +
                ".image_thumbnail img[src], " +
                "a.image_thumbnail img[src], " +
                "img[src]",
            )
            .attr("src")
            .takeIf { it.isNotBlank() }

        return SearxResult(
            url          = url,
            title        = title,
            content      = el.select("p.content").firstOrNull()?.text()?.trim() ?: "",
            engine       = engines.firstOrNull() ?: "",
            engines      = engines,
            category     = "images",
            template     = "images.html",
            thumbnailSrc = thumbSrc,
            imgSrc       = thumbSrc,
        )
    }

    private fun parseVideoArticle(
        el: Element,
        url: String,
        engines: List<String>,
    ): SearxResult {
        val title = el
            .select(".result_header h3, h3, .result_header a, a.url_wrapper h3")
            .firstOrNull()?.text()?.trim() ?: ""

        val thumbSrc = el
            .select(
                "div.result_videos-img img[src], " +
                "img.thumbnail[src], " +
                "img[src]",
            )
            .attr("src")
            .takeIf { it.isNotBlank() }

        // Duration looks like "14:32" or "1:02:15" — pick the first match
        val length = el
            .select(".text-muted, span.length, .result_header span")
            .map { it.text().trim() }
            .firstOrNull { it.matches(Regex("\\d+:\\d+(:\\d+)?")) }

        return SearxResult(
            url          = url,
            title        = title,
            content      = el.select("p.content").firstOrNull()?.text()?.trim() ?: "",
            engine       = engines.firstOrNull() ?: "",
            engines      = engines,
            category     = "videos",
            template     = "videos.html",
            thumbnailSrc = thumbSrc,
            length       = length,
        )
    }
}
