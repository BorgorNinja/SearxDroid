package dev.searxdroid.app.data.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Singleton OkHttp + Retrofit client for SearXNG API calls.
 *
 * This layer is a pure transport layer: it handles cookies, User-Agent
 * spoofing, and timeouts. Response parsing (JSON vs HTML) is delegated
 * entirely to [SearchRepository] and [SearxHtmlParser].
 */
object SearxApiClient {

    private lateinit var appContext: Context

    /**
     * In-memory cookie jar so SearXNG session tokens persist across requests.
     * Required when the instance has the bot-protection limiter enabled —
     * without persisted cookies every request arrives "cold" and triggers an
     * HTML challenge page instead of search results.
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

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor { chain ->
                // Masquerade as desktop Firefox so bot-detection heuristics
                // on any SearXNG instance don't block requests.
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
