package com.example.fitjournal_capstone_leandro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fitjournal_capstone_leandro.data.model.UserProfile
import com.example.fitjournal_capstone_leandro.ui.auth.AuthUiState
import com.example.fitjournal_capstone_leandro.ui.auth.AuthViewModel
import com.example.fitjournal_capstone_leandro.ui.auth.IAuthViewModel
import com.example.fitjournal_capstone_leandro.ui.auth.LoginScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// ========================================
// FAKE AUTH VIEWMODELS
// ========================================

class FakeAuthViewModelError : IAuthViewModel {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    override val uiState: StateFlow<AuthUiState> = _uiState
    override val userProfile: StateFlow<UserProfile?> = MutableStateFlow(null)
    override fun login(email: String, password: String) {
        _uiState.value = AuthUiState.Error("Invalid email or password")
    }
    override fun register(email: String, password: String) {}
    override fun resetState() { _uiState.value = AuthUiState.Idle }
    override fun fetchProfile() {}
}

class FakeAuthViewModelSuccess : IAuthViewModel {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    override val uiState: StateFlow<AuthUiState> = _uiState
    override val userProfile: StateFlow<UserProfile?> = MutableStateFlow(null)
    override fun login(email: String, password: String) {
        _uiState.value = AuthUiState.Success
    }
    override fun register(email: String, password: String) {}
    override fun resetState() { _uiState.value = AuthUiState.Idle }
    override fun fetchProfile() {}
}

// ========================================
// UI TESTS
// ========================================

@RunWith(AndroidJUnit4::class)
class FitJournalUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========================================
    // TEST 1: SAD PATH — invalid credentials shows error
    // Covers: LoginScreen (1 screen)
    // ========================================

    @Test
    fun loginScreen_showsErrorMessage_whenCredentialsAreInvalid() {
        composeTestRule.setContent {
            LoginScreen(
                viewModel = FakeAuthViewModelError(),
                onAuthSuccess = {}
            )
        }

        // Enter invalid credentials
        composeTestRule
            .onNodeWithText("Email")
            .performTextInput("wrong@email.com")

        composeTestRule
            .onNodeWithText("Password")
            .performTextInput("wrongpassword")

        // Tap Login
        composeTestRule
            .onAllNodesWithText("Login")
            .onLast()
            .performClick()

        composeTestRule.waitForIdle()

        // Assert error message is displayed
        composeTestRule
            .onNodeWithText("Invalid email or password")
            .assertIsDisplayed()
    }

    // ========================================
    // TEST 2: HAPPY PATH — successful login navigates to Home
    // Covers: LoginScreen → HomeScreen (2 screens)
    // ========================================

    @Test
    fun loginScreen_navigatesToHome_onSuccessfulLogin() {
        var navigatedToHome = false

        composeTestRule.setContent {
            LoginScreen(
                viewModel = FakeAuthViewModelSuccess(),
                onAuthSuccess = { navigatedToHome = true }
            )
        }

        // Enter valid credentials
        composeTestRule
            .onNodeWithText("Email")
            .performTextInput("test@test.com")

        composeTestRule
            .onNodeWithText("Password")
            .performTextInput("testUSER123!")

        // Tap Login
        composeTestRule
            .onAllNodesWithText("Login")
            .onLast()
            .performClick()

        composeTestRule.waitForIdle()

        // Assert navigation to Home was triggered
        assert(navigatedToHome) {
            "Expected navigation to Home screen but it did not happen"
        }
    }
}