package org.example.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

private val navConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Login::class, Login.serializer())
            subclass(Dashboard::class, Dashboard.serializer())
        }
    }
}

@Composable
@Preview
fun App() {
    val systemDark = isSystemInDarkTheme()
    var darkTheme by remember { mutableStateOf(systemDark) }
    AppTheme(darkTheme = darkTheme) {
        val backStack = rememberNavBackStack(navConfig, Login)

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Login> {
                    LoginScreen(
                        onLogin = { _, _ ->
                            backStack.add(Dashboard)
                        },
                    )
                }
                entry<Dashboard> {
                    DashboardScreen(
                        darkTheme = darkTheme,
                        onToggleDarkMode = { darkTheme = !darkTheme },
                    )
                }
            },
        )
    }
}
