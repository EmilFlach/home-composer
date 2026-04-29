package org.example.project.auth

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createCacheSettings(): Settings =
    NSUserDefaultsSettings(delegate = NSUserDefaults.standardUserDefaults)
