package org.example.project.auth

sealed interface ConnectionStatus {
    data object Checking : ConnectionStatus
    data object Connected : ConnectionStatus
    data class Disconnected(val message: String) : ConnectionStatus
}
