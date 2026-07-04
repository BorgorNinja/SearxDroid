package dev.searxdroid.app.data.network

import dev.searxdroid.app.data.model.SearxResult
import dev.searxdroid.app.data.model.SearxSearchResponse
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

/**
 * Parses the HTML output of a SearXNG instance into a [SearxSearchResponse].
 *
 * Targets SearXNG's "simple" theme (default since ~2022).
 *
 * ## Key structural facts learned from SearXNG source templates
 *
 *   macros.html / result_header:
 *     <article class="result result-{template} category-{cat}">
 *       <a class="url_header" href="{url}">          ← result URL here
 *         <div class="url_wrapper">...</div>
 *       </a>
 *       <a class="thumbnail_link" href="{url}">      ← videos only
 *         <img class="thumbnail" src="{proxied_thumb}">
 *         <span class="thumbnail_length">{HH:MM:SS}</span>
 *       </a>
 *       <h3><a href="{url}">{title}</a></h3>
 *     </article>
 *     <p class="content">{snippet}</p>
 *     <time class="published_date" datetime="{iso}">{human}</time>
 *     <div class="engines"><span>{engine}</span>...</div>
 *     </article>
 *
 *   result_templates/images.html:
 *     <article class="result result-images">
 *       <a href="{img_src}">                         ← links to img_src, not page!
 *         <img class="image_thumbnail" src="{proxied_thumb}">
 *         <span class="title">{title}</span>
 *         <span class="source">{domain}</span>
 *       </a>
 *       <div class="detail ...">
 *         <p class="result-url"><a href="{result.url}">{result.url}</a></p>  ← page URL
 *       </div>
 *     </article>
 *
 *   elements/suggestions.html:
 *     <input type="submit" class="suggestion" value="{text}">  ← NOT <a> links!
 *
 * **Important**: SearXNG articles do NOT have a data-url attribute.
 * The result URL is always extracted from anchor hrefs inside the article.
 */
object SearxHtmlParser {

    fun parse(html: String): SearxSearchResponse {
        val doc = Jsoup.parse(html)

        // ── Results ───────────────────────────────────────────────────────────
        // SearXNG simple theme always uses <article class="result ...">
        // No data-url attribute exists — the previous implementation was
        // selecting zero elements on every real instance.
        val results = doc.select("article.result").mapNotNull { el ->
            runCatching { parseArticle(el) }.getOrNull()
        }

        // ── Direct answers ────────────────────────────────────────────────────
        val answers = doc.select("#answers .answer")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }

        // ── Suggestions ───────────────────────────────────────────────────────
        // Rendered as form submit inputs, NOT anchor links:
        //   <input type="submit" class="suggestion" value="related query">
        val suggestions = doc.select("#suggestions input.suggestion[value]")
            .map { it.attr("value").trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // ── Result count ──────────────────────────────────────────────────────
        // SearXNG's simple theme does not expose a standalone count element;
        // use the parsed result list size as the best available proxy.
        val count = results.size.toLong()

        return SearxSearchResponse(
            results         = results,
            answers         = answers,
            suggestions     = suggestions,
            numberOfResults = count,
        )
    }

    // ── Article dispatch ──────────────────────────────────────────────────────

    private fun parseArticle(el: Element): SearxResult? {
        // URL: from url_header link (present in all templates via result_header macro)
        // or from the h3 title link as fallback. There is NO data-url attribute.
        val url =
            el.selectFirst("a.url_header[href]")?.attr("href")?.takeIf { it.isNotBlank() }
                ?: el.selectFirst("h3 a[href]")?.attr("href")?.takeIf { it.isNotBlank() }
                ?: return null

        // Template detected from CSS class on the article element:
        //   result-default, result-images, result-videos, result-torrent, etc.
        val tpl = when {
            el.hasClass("result-images") -> "images"
            el.hasClass("result-videos") -> "videos"
            else                         -> "default"
        }

        // Engines are always in <div class="engines"><span>name</span>...</div>
        val engines = el.select("div.engines span")
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
        val title   = el.selectFirst("h3 a")?.text()?.trim() ?: ""
        val content = el.selectFirst("p.content")?.text()?.trim() ?: ""
        val date    = el.selectFirst("time.published_date[datetime]")
            ?.attr("datetime")?.takeIf { it.isNotBlank() }

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
        url: String,   // url_header href → img_src (direct image URL)
        engines: List<String>,
    ): SearxResult {
        // Title and thumbnail live inside the main anchor (which links to img_src)
        val title    = el.selectFirst("span.title")?.text()?.trim() ?: ""
        val thumbSrc = el.selectFirst("img.image_thumbnail[src]")
            ?.attr("src")?.takeIf { it.isNotBlank() }

        // The actual *page* URL that the image was found on is in the detail panel
        val pageUrl  = el.selectFirst(".result-url a[href]")
            ?.attr("href")?.takeIf { it.isNotBlank() }
            ?: url   // fallback: use img_src if page URL not present

        return SearxResult(
            url          = pageUrl,
            title        = title,
            content      = el.selectFirst("p.result-content")?.text()?.trim() ?: "",
            engine       = engines.firstOrNull() ?: "",
            engines      = engines,
            category     = "images",
            template     = "images.html",
            thumbnailSrc = thumbSrc,
            imgSrc       = url,   // direct image URL
        )
    }

    private fun parseVideoArticle(
        el: Element,
        url: String,
        engines: List<String>,
    ): SearxResult {
        val title = el.selectFirst("h3 a")?.text()?.trim() ?: ""

        // Thumbnail: result_header emits <a class="thumbnail_link"><img class="thumbnail">
        val thumbSrc = el.selectFirst("a.thumbnail_link img.thumbnail[src]")?.attr("src")
            ?: el.selectFirst("img.thumbnail[src]")?.attr("src")

        // Duration: overlaid in <span class="thumbnail_length"> when thumbnail exists,
        // otherwise in <div class="result_length">Length: HH:MM:SS</div>
        val length = el.selectFirst("span.thumbnail_length")?.text()?.trim()
            ?: el.selectFirst("div.result_length")?.text()
                ?.removePrefix("Length: ")?.removePrefix("Length:")?.trim()

        return SearxResult(
            url          = url,
            title        = title,
            content      = el.selectFirst("p.content")?.text()?.trim() ?: "",
            engine       = engines.firstOrNull() ?: "",
            engines      = engines,
            category     = "videos",
            template     = "videos.html",
            thumbnailSrc = thumbSrc?.takeIf { it.isNotBlank() },
            length       = length?.takeIf { it.isNotBlank() },
        )
    }
}
