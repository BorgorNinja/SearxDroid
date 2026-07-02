package dev.searxdroid.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SearxInstance(
    val name: String,
    val url: String,
    val country: String = "",
    val isCustom: Boolean = false,
)

/** Curated list of public SearXNG instances used as built-in defaults.
 *  User can add/remove custom instances in Settings.
 */
val DEFAULT_SEARX_INSTANCES = listOf(
    SearxInstance("searx.be",            "https://searx.be",                      "🇧🇪 BE"),
    SearxInstance("SearXNG World",        "https://searxng.world",                 "🌍 Global"),
    SearxInstance("search.disroot.org",  "https://search.disroot.org",            "🇳🇱 NL"),
    SearxInstance("paulgo.io",           "https://paulgo.io",                     "🇩🇪 DE"),
    SearxInstance("tiekoetter.com",      "https://searx.tiekoetter.com",          "🇩🇪 DE"),
    SearxInstance("priv.au",             "https://priv.au",                       "🇦🇺 AU"),
    SearxInstance("baresearch.org",      "https://baresearch.org",                "🌍 Global"),
    SearxInstance("search.sapti.me",     "https://search.sapti.me",               "🇩🇪 DE"),
)
