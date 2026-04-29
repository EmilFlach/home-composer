package org.example.project.auth

import com.russhwolf.settings.Settings

class AppPreferences(private val settings: Settings) {
    var defaultDashboardKey: String?
        get() = settings.getStringOrNull(KEY_DEFAULT_DASHBOARD)
        set(value) {
            if (value != null) settings.putString(KEY_DEFAULT_DASHBOARD, value)
            else settings.remove(KEY_DEFAULT_DASHBOARD)
        }

    companion object {
        private const val KEY_DEFAULT_DASHBOARD = "default_dashboard_key"
    }
}
