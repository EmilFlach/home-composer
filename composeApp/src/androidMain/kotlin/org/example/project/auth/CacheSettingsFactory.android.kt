package org.example.project.auth

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.example.project.ProjectApplication

actual fun createCacheSettings(): Settings {
    val prefs = ProjectApplication.appContext.getSharedPreferences(
        "home_composer_cache",
        Context.MODE_PRIVATE,
    )
    return SharedPreferencesSettings(prefs)
}
