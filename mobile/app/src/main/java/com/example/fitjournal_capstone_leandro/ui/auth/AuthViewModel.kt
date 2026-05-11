package com.example.fitjournal_capstone_leandro.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.analytics.AnalyticsLogger
import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.UserProfile
import com.example.fitjournal_capstone_leandro.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

interface IAuthViewModel {
    val uiState: StateFlow<AuthUiState>
    val userProfile: StateFlow<UserProfile?>
    fun login(email: String, password: String)
    fun register(email: String, password: String)
    fun resetState()
    fun fetchProfile()
}

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
) : ViewModel(), IAuthViewModel {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    override val uiState: StateFlow<AuthUiState> = _uiState

    /**
     * Login with email and password
     */
    override fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.login(email, password)

            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
                AnalyticsLogger.logLoginSuccess()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Login failed"
                _uiState.value = AuthUiState.Error(error)
                AnalyticsLogger.logLoginFailure(error)
            }
        }
    }

    /**
     * Register with email and password
     */
    override fun register(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Email and password are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val result = authRepository.register(email, password)

            if (result.isSuccess) {
                _uiState.value = AuthUiState.Success
                AnalyticsLogger.logRegisterSuccess()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Registration failed"
                _uiState.value = AuthUiState.Error(error)
            }
        }
    }


    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    override val userProfile: StateFlow<UserProfile?> = _userProfile

    override fun fetchProfile() {
        viewModelScope.launch {
            val result = authRepository.getProfile()
            if (result.isSuccess) {
                _userProfile.value = result.getOrNull()
            }
        }
    }


    /**
     * Reset state back to Idle
     * Call this when user dismisses an error
     */
     override fun resetState() {
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
