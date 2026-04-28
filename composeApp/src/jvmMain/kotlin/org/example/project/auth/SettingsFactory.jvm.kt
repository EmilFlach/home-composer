package org.example.project.auth

import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences

actual fun createSettings(): Settings {
    val node = Preferences.userRoot().node("org/example/project/auth")
    return PreferencesSettings(node)
}
