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
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Accent yellow used throughout the app
private val AccentYellow = Color(0xFFFFEB3B)
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
                fontFamily = myCustomFont,
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
                .fillMaxWidth(0.48f)
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
                    fontSize = 16.sp,
                    fontFamily = myCustomFont
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
    // Mirrors the workout screen day chips: selected = yellow-tinted fill +
    // yellow border + yellow text; unselected = transparent + white border + white text.
    val borderColor = if (isSelected) AccentYellow else Color.White
    val fillColor   = if (isSelected) AccentYellow.copy(alpha = 0.15f) else Color.Transparent
    val textColor   = if (isSelected) AccentYellow else Color.White
    Box(
        modifier = modifier
            .height(40.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .background(fillColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            fontFamily = myCustomFont
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