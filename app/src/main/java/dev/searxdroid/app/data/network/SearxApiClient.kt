package dev.searxdroid.app.data.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Singleton OkHttp + Retrofit client for SearXNG API calls.
 *
 * Handles cookies, User-Agent spoofing, timeouts, and session warming.
 * Response parsing (JSON vs HTML) is delegated entirely to [SearchRepository]
 * and [SearxHtmlParser].
 */
object SearxApiClient {

    private lateinit var appContext: Context

    /**
     * In-memory cookie jar keyed by host.
     *
     * Persists SearXNG session tokens across requests. Required when the
     * instance has the bot-protection limiter enabled — without persisted
     * cookies every request arrives "cold" and triggers an HTML challenge
     * page instead of search results.
     */
    private val cookieJar: CookieJar = object : CookieJar {
        private val store = mutableMapOf<String, List<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return store[url.host] ?: emptyList()
        }
    }

    /** Tracks which base URLs have already been session-warmed this process. */
    private val warmedInstances: MutableSet<String> = ConcurrentHashMap.newKeySet()

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                // Masquerade as desktop Firefox so bot-detection heuristics
                // on any SearXNG instance do not block our requests.
                val req = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0",
                    )
                    .header("Accept", "text/html,application/json;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
            .build()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /**
     * Fetches the instance root page (GET /) to establish a session cookie
     * before the first search request.
     *
     * SearXNG's built-in limiter validates a `client_token` cookie that is
     * set when any page on the instance is first visited. Without it, cold
     * search requests are treated as bot traffic and receive an HTML challenge
     * page instead of results. Warming the session before searching bypasses
     * this check.
     *
     * Results are cached in [warmedInstances] so each URL is warmed at most
     * once per process lifetime. The call is blocking and must be made from
     * a background thread (Dispatchers.IO).
     */
    fun warmupSession(baseUrl: String) {
        val normalised = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        if (warmedInstances.contains(normalised)) return
        try {
            val req = Request.Builder().url(normalised).build()
            okHttpClient.newCall(req).execute().close()
            warmedInstances.add(normalised)
        } catch (_: Exception) {
            // Best-effort — proceed with the search even if warmup fails.
        }
    }

    /**
     * Build a [SearxApiService] pointed at [baseUrl].
     * A trailing slash is added if absent (required by Retrofit).
     */
    fun buildServiceFor(baseUrl: String): SearxApiService {
        val normalised = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalised)
            .client(okHttpClient)
            .build()
            .create(SearxApiService::class.java)
    }
}
