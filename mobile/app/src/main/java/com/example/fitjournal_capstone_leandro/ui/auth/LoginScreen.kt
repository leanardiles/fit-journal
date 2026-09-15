package com.example.fitjournal_capstone_leandro.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.example.fitjournal_capstone_leandro.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.fitjournal_capstone_leandro.ui.theme.AccentYellow
import com.example.fitjournal_capstone_leandro.ui.theme.BackgroundDark
import com.example.fitjournal_capstone_leandro.ui.theme.TextGray
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont
import com.example.fitjournal_capstone_leandro.ui.shared.ChipToggle
import com.example.fitjournal_capstone_leandro.ui.shared.PrimaryButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Login Screen
 *
 * Handles both Login and Register modes via a tab toggle.
 * Navigates to home on success.
 *
 * @param viewModel  AuthViewModel
 * @param onAuthSuccess  Called when login/register succeeds → navigate to home
 */
@Composable
fun LoginScreen(
    viewModel: IAuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Form fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Toggle between Login and Register
    var isLoginMode by remember { mutableStateOf(true) }

    // Navigate on success
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onAuthSuccess()
            viewModel.resetState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // App title
        Image(
            painter = painterResource(id = R.drawable.logo_and_name),
            contentDescription = "FitJournal Logo",
            modifier = Modifier
                .fillMaxWidth(0.8f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isLoginMode) "Sign in and log your next session"
            else "Start your fitness journey",
            fontSize = 18.sp,
            color = Color.White,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Login / Register toggle — day-chip style (selected = yellow, other = outlined)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChipToggle(
                text = "Login",
                selected = isLoginMode,
                modifier = Modifier.weight(1f),
                onClick = {
                    isLoginMode = true
                    viewModel.resetState()
                }
            )
            ChipToggle(
                text = "Register",
                selected = !isLoginMode,
                modifier = Modifier.weight(1f),
                onClick = {
                    isLoginMode = false
                    viewModel.resetState()
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = TextGray) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedTextFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", color = TextGray) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
            colors = outlinedTextFieldColors(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Error message
        if (uiState is AuthUiState.Error) {
            Text(
                text = (uiState as AuthUiState.Error).message,
                color = Color(0xFFFF453A),
                fontSize = 13.sp,
                fontFamily = myCustomFont,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Submit button (shared PrimaryButton). While loading it disables and shows
        // a wait label — keeps the auth spinner behaviour without a custom button.
        val isLoading = uiState is AuthUiState.Loading
        PrimaryButton(
            text = if (isLoading) "Please wait…" else if (isLoginMode) "Login" else "Register",
            onClick = {
                if (isLoginMode) viewModel.login(email, password)
                else viewModel.register(email, password)
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(0.48f)
        )
    }
}

/**
 * Shared text field colors matching dark theme
 */
@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentYellow,
    unfocusedBorderColor = Color(0xFF3A3A3C),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = AccentYellow
)