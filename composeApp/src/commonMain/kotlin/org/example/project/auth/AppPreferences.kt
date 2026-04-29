package org.example.project.auth

import com.russhwolf.settings.Settings

class AppPreferences(private val settings: Settings) {
    var defaultDashboardKey: String?
        get() = settings.getStringOrNull(KEY_DEFAULT_DASHBOARD)
        set(value) {
            if (value != null) settings.putString(KEY_DEFAULT_DASHBOARD, value)
            else settings.remove(KEY_DEFAULT_DASHBOARD)
        }

    var themeSeedColor: Int?
        get() = settings.getIntOrNull(KEY_THEME_SEED_COLOR)
        set(value) {
            if (value != null) settings.putInt(KEY_THEME_SEED_COLOR, value)
            else settings.remove(KEY_THEME_SEED_COLOR)
        }

    companion object {
        private const val KEY_DEFAULT_DASHBOARD = "default_dashboard_key"
        private const val KEY_THEME_SEED_COLOR = "theme_seed_color"
    }
}
