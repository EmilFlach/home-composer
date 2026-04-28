package org.example.project.auth

import androidx.compose.runtime.staticCompositionLocalOf

val LocalHomeAssistantConfig = staticCompositionLocalOf<HomeAssistantConfig?> { null }

fun resolveHaUrl(baseUrl: String?, path: String): String {
    if (path.isBlank()) return path
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    if (baseUrl.isNullOrBlank()) return path
    val trimmedBase = baseUrl.trimEnd('/')
    val normalizedPath = if (path.startsWith("/")) path else "/$path"
    return "$trimmedBase$normalizedPath"
}

fun isHaApiUrl(url: String, baseUrl: String?): Boolean {
    if (baseUrl.isNullOrBlank()) return false
    val trimmedBase = baseUrl.trimEnd('/')
    if (!url.startsWith(trimmedBase)) return false
    val tail = url.removePrefix(trimmedBase)
    return tail.startsWith("/api/")
}
