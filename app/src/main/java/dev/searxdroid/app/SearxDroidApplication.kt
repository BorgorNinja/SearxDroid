package dev.searxdroid.app

import android.app.Application
import dev.searxdroid.app.data.network.SearxApiClient

class SearxDroidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialise the singleton HTTP client once at startup.
        SearxApiClient.init(this)
    }
}
