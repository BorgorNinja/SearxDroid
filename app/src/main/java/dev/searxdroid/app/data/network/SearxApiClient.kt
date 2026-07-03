package dev.searxdroid.app.data.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.IOException
import java.net.CookieManager
import java.net.CookiePolicy
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
     * Persistent cookie jar so session tokens from SearXNG's bot-protection
     * limiter (if enabled on the instance) survive across requests.  Without
     * this every search hits the instance "cold" with no session cookie and
     * triggers an HTML challenge page instead of a JSON response.
     */
    private val cookieJar: CookieJar by lazy {
        JavaNetCookieJar(CookieManager().also { it.setCookiePolicy(CookiePolicy.ACCEPT_ALL) })
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
