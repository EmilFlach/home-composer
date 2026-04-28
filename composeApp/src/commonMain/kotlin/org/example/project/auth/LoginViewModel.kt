package org.example.project.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val serverUrl: String = "",
    val token: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface LoginEvent {
    data class UrlChanged(val value: String) : LoginEvent
    data class TokenChanged(val value: String) : LoginEvent
    data object Submit : LoginEvent
}

sealed interface LoginEffect {
    data object NavigateToDashboard : LoginEffect
}

class LoginViewModel(
    private val client: HomeAssistantClient,
    private val tokenStorage: TokenStorage,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effect = effects.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.UrlChanged -> _state.update { it.copy(serverUrl = event.value, errorMessage = null) }
            is LoginEvent.TokenChanged -> _state.update { it.copy(token = event.value, errorMessage = null) }
            LoginEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        val baseUrl = normalizeUrl(current.serverUrl)
        val token = current.token.trim()
        if (baseUrl.isEmpty()) {
            _state.update { it.copy(errorMessage = "Server URL is required") }
            return
        }
        if (token.isEmpty()) {
            _state.update { it.copy(errorMessage = "Access token is required") }
            return
        }
        _state.update { it.copy(isSubmitting = true, errorMessage = null) }
        viewModelScope.launch {
            val config = HomeAssistantConfig(baseUrl = baseUrl, token = token)
            client.verify(config)
                .onSuccess {
                    tokenStorage.save(config)
                    _state.update { it.copy(isSubmitting = false) }
                    effects.trySend(LoginEffect.NavigateToDashboard)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Connection failed",
                        )
                    }
                }
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ""
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
    }
}
