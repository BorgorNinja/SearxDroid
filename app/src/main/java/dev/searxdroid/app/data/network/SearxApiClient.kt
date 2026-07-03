package dev.searxdroid.app.data.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Singleton HTTP client for SearXNG API calls.
 *
 * The base URL is mutable — switching instances only requires calling
 * [buildServiceFor] with the new URL; no restart needed.
 */
object SearxApiClient {

    private lateinit var appContext: Context

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Simple in-memory CookieJar keyed by host.
     *
     * Persists SearXNG session tokens across requests so bot-protection
     * limiter challenges are passed on the first response and subsequent
     * searches go through cleanly — without needing the external
     * okhttp-urlconnection artifact.
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
                // Masquerade as a desktop Firefox browser so instances don't
                // block the app with bot-detection heuristics.
                val req = chain.request().newBuilder()
                    .header("User-Agent",
                        "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0")
                    .header("Accept",
                        "application/json,text/html;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .build()
                chain.proceed(req)
            }
            .addInterceptor { chain ->
                val resp = chain.proceed(chain.request())
                val ct = resp.header("Content-Type") ?: ""

                if ("html" in ct) {
                    val statusCode = resp.code
                    resp.close()
                    // Distinguish between two distinct failure modes:
                    //  * 4xx  -> JSON format is actually disabled server-side
                    //  * 2xx  -> Bot-protection / limiter returned an HTML challenge
                    //            page; the instance may still have JSON enabled but
                    //            rejected the cold request without a session cookie.
                    val message = if (statusCode in 400..499) {
                        "Instance has JSON API disabled (HTTP $statusCode). " +
                        "Add 'json' to the formats list in your instance's settings.yaml, " +
                        "or open Settings to switch instance."
                    } else {
                        "Instance returned an HTML page instead of JSON (HTTP $statusCode). " +
                        "If this is a self-hosted instance, set 'limiter: false' in settings.yaml " +
                        "to disable bot-detection, or open Settings to switch instance."
                    }
                    throw IOException(message)
                }

                if ("json" !in ct && resp.isSuccessful && ct.isNotEmpty()) {
                    resp.close()
                    throw IOException(
                        "Unexpected response type '$ct' -- expected application/json. " +
                        "Open Settings to switch instance."
                    )
                }

                resp
            }
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** Build a [SearxApiService] pointing at the given base URL. */
    fun buildServiceFor(baseUrl: String): SearxApiService {
        val normalised = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalised)
            .client(okHttpClient)
            .addConverterFactory(
                json.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(SearxApiService::class.java)
    }
}
