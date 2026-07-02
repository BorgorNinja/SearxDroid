package dev.searxdroid.app.data.model

/** SearXNG search category filter chips. */
enum class SearchCategory(val id: String, val label: String, val icon: String) {
    GENERAL("general",      "General",     "public"),
    IT("it",                "IT",          "code"),
    NEWS("news",            "News",        "newspaper"),
    IMAGES("images",        "Images",      "image"),
    VIDEOS("videos",        "Videos",      "play_circle"),
    MUSIC("music",          "Music",       "music_note"),
    FILES("files",          "Files",       "folder"),
    SOCIAL("social media",  "Social",      "forum"),
    SCIENCE("science",      "Science",     "science"),
    MAP("map",              "Maps",        "map"),
}
