package org.example.project.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings

actual fun createCacheSettings(): Settings = StorageSettings()
