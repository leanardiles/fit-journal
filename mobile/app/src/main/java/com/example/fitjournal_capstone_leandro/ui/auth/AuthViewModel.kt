package com.example.fitjournal_capstone_leandro.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Login/Register screen
 */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

/**
 * ViewModel for authentication screens
 *
 * Handles login and register actions,
 * exposes UI state to the Compose screen
 */
class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    /**
     * Login with email and password
     */
    fun login(email: String, password: String) {
        // Basic validation before hitting the network
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.login(email, password)

            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    /**
     * Register with email and password
     */
    fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.register(email, password)

            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success
            } else {
                AuthUiState.Error(
                    result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }

    /**
     * Reset state back to Idle
     * Call this when user dismisses an error
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

/**
 * Factory to create AuthViewModel with its dependency (AuthRepository)
 */
class AuthViewModelFactory(
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return AuthViewModel(
            AuthRepository(tokenManager)
        ) as T
    }
}
