package dev.searxdroid.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.searxdroid.app.ui.navigation.SearxNavHost
import dev.searxdroid.app.ui.theme.ObsidianFluxTheme
import dev.searxdroid.app.data.repository.SettingsRepository

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Resolve deep-link query from ACTION_WEB_SEARCH or ACTION_SEARCH intents.
        val initialQuery = intent.getStringExtra("query")
            ?: intent.getStringExtra(android.app.SearchManager.QUERY)
            ?: ""

        setContent {
            val settings = SettingsRepository.getInstance(applicationContext)
            val isDark by settings.darkMode.collectAsStateWithLifecycle(initialValue = false)

            ObsidianFluxTheme(darkTheme = isDark) {
                SearxNavHost(initialQuery = initialQuery)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
