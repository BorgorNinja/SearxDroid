# SearxDroid 🔍🛡️

**A privacy-first SearXNG GUI wrapper for Android — written in Kotlin with Jetpack Compose.**

> There is no official SearXNG mobile app. SearxDroid is a native Android frontend that connects to any SearXNG instance (public or self-hosted), giving you a fast, beautiful, tracker-free search experience without opening a browser.

---

## Features

| Feature | Detail |
|---|---|
| 🔍 **Multi-engine search** | Aggregates results from 70+ search engines via SearXNG |
| 🛡️ **Privacy-first** | No tracking, no analytics, no ads |
| 🌐 **Configurable instance** | Point to any public or self-hosted SearXNG instance |
| 🔄 **Auto-fallback** | Cycles through backup instances if the primary fails |
| 📂 **Categories** | General, IT, News, Images, Videos, Music, Files, Social, Science, Maps |
| 🌙 **Dark / Light mode** | Follows system theme or manual toggle |
| 🔒 **Safe search** | Off / Moderate / Strict |
| 📱 **Material You** | Full Jetpack Compose UI with Obsidian Flux design system |

---

## Architecture

```
SearxDroid/
├── data/
│   ├── model/          # Kotlin @Serializable data classes (SearXNG JSON API)
│   ├── network/        # Retrofit + OkHttp client (SearxApiService)
│   └── repository/     # SearchRepository, SettingsRepository (DataStore)
└── ui/
    ├── theme/          # ObsidianFluxTheme — full M3 color scheme from design spec
    ├── home/           # Dashboard / landing screen
    ├── search/         # Results screen + SearchViewModel
    ├── settings/       # Instance picker, engines, privacy controls
    ├── components/     # SearxSearchBar, ResultCard, CategoryChip, PrivacyChip
    └── navigation/     # NavHost + route definitions
```

### Self-Contained Design

SearxDroid ships with a curated list of **8 public SearXNG instances** as defaults. No server setup needed out of the box. To point at your own instance:

**Settings → Instance URL → Add custom instance**

Enter any `https://your-searxng-server.example.com` URL. The app validates the URL and immediately switches to it. Your instance URL is persisted in DataStore (no unencrypted SQLite).

---

## Building

```bash
# Clone
git clone https://github.com/BorgorNinja/SearxDroid.git
cd SearxDroid

# Debug build (no keystore needed)
./gradlew assembleDebug

# Install directly to connected device
./gradlew installDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### CI / GitHub Actions

Every push to `main` triggers a build that produces both a **Debug APK** and a **Release APK**, attached as artifacts and published as a GitHub pre-release.

#### First-run signing setup
1. Run the workflow once (it generates a fresh keystore).
2. Download the `SAVE-AS-KEYSTORE_BASE64-SECRET` artifact.
3. Base64-encode it: `base64 -w 0 searxdroid.jks`
4. Save the output as repo secrets:
   - `KEYSTORE_BASE64` — the base64 string
   - `KEYSTORE_PASSWORD` — `searx_debug` (or your own)
   - `KEY_ALIAS` — `searxdroid`
   - `KEY_PASSWORD` — `searx_debug` (or your own)

All future builds will reuse the same signing key, so installs update seamlessly.

---

## SearXNG API

SearxDroid uses the **SearXNG JSON API**:

```
GET https://<instance>/search?q=<query>&format=json&categories=general&language=en-US&safesearch=0&pageno=1
```

Response fields used: `results[].url`, `results[].title`, `results[].content`, `results[].engine`, `results[].category`, `results[].score`, `answers`, `suggestions`, `number_of_results`.

---

## License

MIT © BorgorNinja
