package dev.searxdroid.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class SearxInstance(
    val name: String,
    val url: String,
    val country: String = "",
    val isCustom: Boolean = false,
)
