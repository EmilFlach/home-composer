package org.example.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.example.project.auth.AppPreferences
import org.example.project.auth.HomeAssistantClient
import org.example.project.auth.HomeAssistantWebSocketClient
import org.example.project.auth.LocalHomeAssistantConfig
import org.example.project.auth.LoginEffect
import org.example.project.auth.LoginViewModel
import org.example.project.auth.TokenStorage
import org.example.project.auth.DashboardCache
import org.example.project.auth.createCacheSettings
import org.example.project.auth.createHttpClient
import org.example.project.auth.createSettings
import androidx.compose.runtime.CompositionLocalProvider

private fun colorToRgbInt(color: Color): Int =
    ((color.red * 255 + 0.5f).toInt() shl 16) or
    ((color.green * 255 + 0.5f).toInt() shl 8) or
    (color.blue * 255 + 0.5f).toInt()

private fun colorFromRgbInt(rgb: Int): Color = Color(
    red   = ((rgb ushr 16) and 0xFF) / 255f,
    green = ((rgb ushr 8) and 0xFF) / 255f,
    blue  = (rgb and 0xFF) / 255f,
)

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

    val settings = remember { createSettings() }
    val tokenStorage = remember { TokenStorage(settings) }
    val appPreferences = remember { AppPreferences(settings) }
    var themeSeedColor by remember { mutableStateOf(appPreferences.themeSeedColor?.let { colorFromRgbInt(it) }) }
    val httpClient = remember { createHttpClient() }
    val haClient = remember { HomeAssistantClient(httpClient) }
    val dashboardCache = remember { DashboardCache(createCacheSettings()) }
    val haWsClient = remember { HomeAssistantWebSocketClient(httpClient, dashboardCache) }

    @OptIn(coil3.annotation.ExperimentalCoilApi::class)
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = httpClient)) }
            .crossfade(true)
            .applyDiskCache()
            .build()
    }

    val startRoute = remember { if (tokenStorage.load() != null) Dashboard else Login }

    AppTheme(darkTheme = darkTheme, seedColor = themeSeedColor) {
        val backStack = rememberNavBackStack(navConfig, startRoute)

        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Login> {
                    val viewModel = viewModel { LoginViewModel(haClient, tokenStorage) }
                    val state by viewModel.state.collectAsStateWithLifecycle()

                    LaunchedEffect(viewModel) {
                        viewModel.effect.collect { effect ->
                            when (effect) {
                                LoginEffect.NavigateToDashboard -> {
                                    backStack.clear()
                                    backStack.add(Dashboard)
                                }
                            }
                        }
                    }

                    LoginScreen(
                        state = state,
                        onEvent = viewModel::onEvent,
                        darkTheme = darkTheme,
                        onToggleDarkMode = { darkTheme = !darkTheme },
                    )
                }
                entry<Dashboard> {
                    val config = remember { tokenStorage.load() }
                    if (config == null) {
                        LaunchedEffect(Unit) {
                            backStack.clear()
                            backStack.add(Login)
                        }
                    } else {
                        CompositionLocalProvider(LocalHomeAssistantConfig provides config) {
                            DashboardScreen(
                                config = config,
                                client = haClient,
                                wsClient = haWsClient,
                                appPreferences = appPreferences,
                                darkTheme = darkTheme,
                                onToggleDarkMode = { darkTheme = !darkTheme },
                                currentSeedColor = themeSeedColor,
                                onThemeChange = { color ->
                                    themeSeedColor = color
                                    appPreferences.themeSeedColor = colorToRgbInt(color)
                                },
                                onLogout = {
                                    tokenStorage.clear()
                                    backStack.clear()
                                    backStack.add(Login)
                                },
                            )
                        }
                    }
                }
            },
        )
    }
}
