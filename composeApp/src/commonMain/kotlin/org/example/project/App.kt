package org.example.project

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.example.project.auth.HomeAssistantClient
import org.example.project.auth.HomeAssistantWebSocketClient
import org.example.project.auth.LocalHomeAssistantConfig
import org.example.project.auth.LoginEffect
import org.example.project.auth.LoginViewModel
import org.example.project.auth.TokenStorage
import org.example.project.auth.createHttpClient
import org.example.project.auth.createSettings
import androidx.compose.runtime.CompositionLocalProvider

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

    val tokenStorage = remember { TokenStorage(createSettings()) }
    val httpClient = remember { createHttpClient() }
    val haClient = remember { HomeAssistantClient(httpClient) }
    val haWsClient = remember { HomeAssistantWebSocketClient(httpClient) }

    @OptIn(coil3.annotation.ExperimentalCoilApi::class)
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = httpClient)) }
            .crossfade(true)
            .build()
    }

    val startRoute = remember { if (tokenStorage.load() != null) Dashboard else Login }

    AppTheme(darkTheme = darkTheme) {
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
                                darkTheme = darkTheme,
                                onToggleDarkMode = { darkTheme = !darkTheme },
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
