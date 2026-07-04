package dev.searxdroid.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.searxdroid.app.data.model.SearxInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("searxdroid_prefs")

class SettingsRepository private constructor(private val context: Context) {

    private val ds   = context.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val INSTANCE_URL     = stringPreferencesKey("instance_url")
        val DARK_MODE        = booleanPreferencesKey("dark_mode")
        val SAFE_SEARCH      = intPreferencesKey("safe_search")
        val LANGUAGE         = stringPreferencesKey("language")
        val CUSTOM_INSTANCES = stringSetPreferencesKey("custom_instances")
        val ACTIVE_ENGINES   = stringSetPreferencesKey("active_engines")
    }

    /** Empty string means no instance has been configured yet. */
    val instanceUrl: Flow<String> = ds.data.map { it[Keys.INSTANCE_URL] ?: "" }

    val darkMode: Flow<Boolean> = ds.data.map { it[Keys.DARK_MODE] ?: false }

    val safeSearch: Flow<Int> = ds.data.map { it[Keys.SAFE_SEARCH] ?: 0 }

    val language: Flow<String> = ds.data.map { it[Keys.LANGUAGE] ?: "en-US" }

    val customInstances: Flow<List<SearxInstance>> = ds.data.map { prefs ->
        prefs[Keys.CUSTOM_INSTANCES]
            ?.mapNotNull { runCatching { json.decodeFromString<SearxInstance>(it) }.getOrNull() }
            ?: emptyList()
    }

    val activeEngines: Flow<Set<String>> = ds.data.map { prefs ->
        prefs[Keys.ACTIVE_ENGINES] ?: setOf("google", "bing", "duckduckgo", "brave", "wikipedia")
    }

    suspend fun setInstanceUrl(url: String) { ds.edit { it[Keys.INSTANCE_URL] = url } }

    suspend fun setDarkMode(enabled: Boolean) { ds.edit { it[Keys.DARK_MODE] = enabled } }

    suspend fun setSafeSearch(level: Int) {
        ds.edit { it[Keys.SAFE_SEARCH] = level.coerceIn(0, 2) }
    }

    suspend fun setLanguage(lang: String) { ds.edit { it[Keys.LANGUAGE] = lang } }

    suspend fun addCustomInstance(instance: SearxInstance) {
        ds.edit { prefs ->
            val current = prefs[Keys.CUSTOM_INSTANCES]?.toMutableSet() ?: mutableSetOf()
            current.add(json.encodeToString(instance))
            prefs[Keys.CUSTOM_INSTANCES] = current
        }
    }

    suspend fun removeCustomInstance(url: String) {
        ds.edit { prefs ->
            val current = prefs[Keys.CUSTOM_INSTANCES]?.toMutableSet() ?: return@edit
            current.removeAll { encoded ->
                runCatching { json.decodeFromString<SearxInstance>(encoded).url == url }
                    .getOrDefault(false)
            }
            prefs[Keys.CUSTOM_INSTANCES] = current
        }
    }

    suspend fun setActiveEngines(engines: Set<String>) {
        ds.edit { it[Keys.ACTIVE_ENGINES] = engines }
    }

    companion object {
        @Volatile private var INSTANCE: SettingsRepository? = null
        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
        }
    }
}
