package com.example.fitjournal_capstone_leandro.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

private val AccentYellow = Color(0xFFFFEB3B)
private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark = Color(0xFF2C2C2E)
private val TextGray = Color(0xFF8E8E93)
private val DangerRed = Color(0xFFFF453A)

private val timezones = listOf(
    // Americas
    "America/New_York" to "New York (UTC-5)",
    "America/Chicago" to "Chicago (UTC-6)",
    "America/Denver" to "Denver (UTC-7)",
    "America/Los_Angeles" to "Los Angeles (UTC-8)",
    "America/Toronto" to "Toronto (UTC-5)",
    "America/Vancouver" to "Vancouver (UTC-8)",
    "America/Sao_Paulo" to "São Paulo (UTC-3)",
    "America/Argentina/Buenos_Aires" to "Buenos Aires (UTC-3)",
    "America/Bogota" to "Bogotá (UTC-5)",
    "America/Mexico_City" to "Mexico City (UTC-6)",
    // Europe
    "Europe/London" to "London (UTC+0)",
    "Europe/Amsterdam" to "Amsterdam (UTC+1)",
    "Europe/Paris" to "Paris (UTC+1)",
    "Europe/Berlin" to "Berlin (UTC+1)",
    "Europe/Madrid" to "Madrid (UTC+1)",
    "Europe/Rome" to "Rome (UTC+1)",
    "Europe/Moscow" to "Moscow (UTC+3)",
    // Asia / Pacific
    "Asia/Dubai" to "Dubai (UTC+4)",
    "Asia/Kolkata" to "Kolkata (UTC+5:30)",
    "Asia/Singapore" to "Singapore (UTC+8)",
    "Asia/Tokyo" to "Tokyo (UTC+9)",
    "Asia/Shanghai" to "Shanghai (UTC+8)",
    "Australia/Sydney" to "Sydney (UTC+11)",
    "Pacific/Auckland" to "Auckland (UTC+13)",
    // Africa
    "Africa/Cairo" to "Cairo (UTC+2)",
    "Africa/Johannesburg" to "Johannesburg (UTC+2)",
    "Africa/Lagos" to "Lagos (UTC+1)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    viewModel: ProfileSettingsViewModel,
    onSaved: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigate back after save
    LaunchedEffect(state.uiState) {
        if (state.uiState is ProfileSettingsUiState.Saved) {
            snackbarHostState.showSnackbar("Profile saved!")
            viewModel.resetState()
            onSaved()
        }
    }

    // Account deleted -> leave for login (backstack cleared by the caller)
    LaunchedEffect(state.accountDeleted) {
        if (state.accountDeleted) {
            onAccountDeleted()
            viewModel.acknowledgeAccountDeleted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when (state.uiState) {
            is ProfileSettingsUiState.Loading -> {
                CircularProgressIndicator(
                    color = AccentYellow,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is ProfileSettingsUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = (state.uiState as ProfileSettingsUiState.Error).message,
                        color = Color(0xFFFF453A),
                        fontFamily = myCustomFont
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
                    ) {
                        Text("Retry", color = Color.Black, fontFamily = myCustomFont)
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Title
                    Text(
                        text = "Profile Settings",
                        fontSize = 32.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = myCustomFont
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email (read-only)
                    ProfileLabel("Email")
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name
                    ProfileLabel("Name")
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { viewModel.updateName(it) },
                        placeholder = { Text("Your name", color = TextGray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sex
                    ProfileLabel("Sex")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("M" to "M", "F" to "W", "NB" to "NB").forEach { (value, label) ->
                            ToggleButton(
                                label = label,
                                isSelected = state.sex == value,
                                onClick = { viewModel.updateSex(value) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Age
                    ProfileLabel("Age")
                    OutlinedTextField(
                        value = state.age,
                        onValueChange = { viewModel.updateAge(it) },
                        placeholder = { Text("e.g. 25", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

// Timezone
                    var timezoneExpanded by remember { mutableStateOf(false) }
                    val selectedTimezoneLabel = timezones.find { it.first == state.timezone }?.second ?: state.timezone

                    ProfileLabel("Timezone")
                    ExposedDropdownMenuBox(
                        expanded = timezoneExpanded,
                        onExpandedChange = { timezoneExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedTimezoneLabel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = timezoneExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = timezoneExpanded,
                            onDismissRequest = { timezoneExpanded = false },
                            modifier = Modifier.background(SurfaceDark)
                        ) {
                            timezones.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = label,
                                            color = Color.White,
                                            fontFamily = myCustomFont,
                                            fontSize = 14.sp
                                        )
                                    },
                                    onClick = {
                                        viewModel.updateTimezone(value)
                                        timezoneExpanded = false
                                    },
                                    modifier = Modifier.background(SurfaceDark)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Preferred Unit
                    ProfileLabel("Preferred Unit")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("metric" to "Metric", "imperial" to "Imperial").forEach { (value, label) ->
                            ToggleButton(
                                label = label,
                                isSelected = state.unitPreference == value,
                                onClick = { viewModel.updateUnitPreference(value) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Height — label changes based on unit
                    val heightLabel = if (state.unitPreference == "metric") "Height (cm)" else "Height (ft'in\")"
                    // placeholder:
                    Text(if (state.unitPreference == "metric") "170" else "5'11\"", color = TextGray)

                    ProfileLabel(heightLabel)
                    OutlinedTextField(
                        value = state.height,
                        onValueChange = { viewModel.updateHeight(it) },
                        placeholder = { Text(if (state.unitPreference == "metric") "170" else "5.9", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weight — label changes based on unit
                    val weightLabel = if (state.unitPreference == "metric") "Weight (kg)" else "Weight (lbs)"
                    ProfileLabel(weightLabel)
                    OutlinedTextField(
                        value = state.weight,
                        onValueChange = { viewModel.updateWeight(it) },
                        placeholder = { Text(if (state.unitPreference == "metric") "70.5" else "155", color = TextGray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Save button
                    Button(
                        onClick = { viewModel.saveProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Save",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = myCustomFont
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // ---- Danger zone ----
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF3A3A3C))
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Danger zone",
                        fontSize = 14.sp,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold,
                        fontFamily = myCustomFont
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Permanently delete your account and all associated data. This can't be undone.",
                        fontSize = 13.sp,
                        color = TextGray,
                        fontFamily = myCustomFont
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.showDeleteAccountDialog() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        border = BorderStroke(2.dp, DangerRed),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Text(
                            text = "Delete account",
                            color = DangerRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = myCustomFont
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // ---- Delete account confirmation dialog ----
        if (state.showDeleteDialog) {
            var deletePassword by remember { mutableStateOf("") }
            var showPassword by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    if (!state.deleteInProgress) viewModel.dismissDeleteAccountDialog()
                },
                containerColor = SurfaceDark,
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = {
                    Text(
                        text = "Delete account?",
                        fontFamily = myCustomFont,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "This permanently deletes your account and all associated data: " +
                                    "profile, exercises, routine, workout logs, session history, and saved " +
                                    "selections. This can't be undone.",
                            fontFamily = myCustomFont,
                            fontSize = 14.sp,
                            color = TextGray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = deletePassword,
                            onValueChange = { deletePassword = it },
                            placeholder = { Text("Password", color = TextGray) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                Text(
                                    text = if (showPassword) "Hide" else "Show",
                                    color = AccentYellow,
                                    fontFamily = myCustomFont,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clickable { showPassword = !showPassword }
                                        .padding(horizontal = 12.dp)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = outlinedColors(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        if (state.deleteError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.deleteError!!,
                                color = DangerRed,
                                fontFamily = myCustomFont,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.deleteAccount(deletePassword) },
                        enabled = deletePassword.isNotBlank() && !state.deleteInProgress
                    ) {
                        Text(
                            text = if (state.deleteInProgress) "Deleting\u2026" else "Delete",
                            color = if (deletePassword.isNotBlank() && !state.deleteInProgress) DangerRed else TextGray,
                            fontFamily = myCustomFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissDeleteAccountDialog() },
                        enabled = !state.deleteInProgress
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontFamily = myCustomFont
                        )
                    }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ProfileLabel(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = TextGray,
        fontFamily = myCustomFont,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun ToggleButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(
                width = 2.dp,
                color = if (isSelected) AccentYellow else Color.Gray,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = if (isSelected) AccentYellow.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) AccentYellow else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = myCustomFont,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentYellow,
    unfocusedBorderColor = Color(0xFF3A3A3C),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = AccentYellow,
    focusedContainerColor = SurfaceDark,
    unfocusedContainerColor = SurfaceDark
)