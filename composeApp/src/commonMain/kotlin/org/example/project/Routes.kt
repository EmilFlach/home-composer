package org.example.project

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey

@Serializable
data object Login : AppRoute

@Serializable
data object Dashboard : AppRoute
