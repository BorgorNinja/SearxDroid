# SearxDroid ProGuard rules

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class dev.searxdroid.app.**$$serializer { *; }
-keepclassmembers class dev.searxdroid.app.** {
    *** Companion;
}
-keepclasseswithmembers class dev.searxdroid.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Coil
-dontwarn coil.**

# Keep data models for serialization
-keep class dev.searxdroid.app.data.model.** { *; }

# Jsoup HTML parser (used by SearxHtmlParser for HTML-mode fallback)
-keep class org.jsoup.** { *; }
-dontwarn org.jsoup.**
