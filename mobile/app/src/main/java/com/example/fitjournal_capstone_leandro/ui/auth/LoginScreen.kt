package com.example.fitjournal_capstone_leandro.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Accent yellow used throughout the app
private val AccentYellow = Color(0xFFFFFFFF)
private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark = Color(0xFF2C2C2E)
private val TextGray = Color(0xFF8E8E93)

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
            painter = painterResource(id = R.drawable.logo_alone),
            contentDescription = "FitJournal Logo",
            modifier = Modifier
                .size(100.dp)
                .padding(bottom = 8.dp)
        )

        Text(
            text = "FitJournal",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = AccentYellow
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "The easiest way to log your workouts.",
            fontSize = 14.sp,
            color = TextGray
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Login / Register toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            TabButton(
                text = "Login",
                isSelected = isLoginMode,
                modifier = Modifier.weight(1f),
                onClick = {
                    isLoginMode = true
                    viewModel.resetState()
                }
            )
            TabButton(
                text = "Register",
                isSelected = !isLoginMode,
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
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Submit button
        Button(
            onClick = {
                if (isLoginMode) {
                    viewModel.login(email, password)
                } else {
                    viewModel.register(email, password)
                }
            },
            enabled = uiState !is AuthUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentYellow,
                contentColor = Color.Black,
                disabledContainerColor = AccentYellow.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    color = Color.Black,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isLoginMode) "Login" else "Register",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * Tab toggle button (Login / Register)
 */
@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) AccentYellow else Color.Transparent,
            contentColor = if (isSelected) Color.Black else TextGray
        ),
        elevation = ButtonDefaults.buttonElevation(0.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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