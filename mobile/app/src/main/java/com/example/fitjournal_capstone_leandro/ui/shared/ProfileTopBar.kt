package com.example.fitjournal_capstone_leandro.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.OutlinedButton

@Composable
fun ProfileTopBar(
    userName: String = "User Name",  // Default, will be replaced with actual user later
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1B1B1E))
            .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button (left side) - NEW
        if (showBackButton) {
            OutlinedButton(
                onClick = onBackClick,
                border = BorderStroke(2.dp, Color.White),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "<<",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        } else {
            // Empty spacer when no back button
            Spacer(modifier = Modifier.width(1.dp))
        }

        // User name and profile (right side) - existing code
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User name
            Text(
                text = userName,
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontFamily = myCustomFont,
                modifier = Modifier.padding(end = 8.dp)
            )

            // Profile icon button with dropdown
            Box {
                IconButton(
                    onClick = { expanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(Color(0xFF2A2A2E))
                        .width(180.dp)
                ) {
                    // Account option
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Account",
                                color = Color.White,
                                fontFamily = myCustomFont,
                                fontSize = 16.sp
                            )
                        },
                        onClick = {
                            expanded = false
                            onAccountClick()
                        },
                        modifier = Modifier.background(Color(0xFF2A2A2E))
                    )

                    Divider(color = Color.Gray.copy(alpha = 0.3f))

                    // Settings option
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Settings",
                                color = Color.White,
                                fontFamily = myCustomFont,
                                fontSize = 16.sp
                            )
                        },
                        onClick = {
                            expanded = false
                            onSettingsClick()
                        },
                        modifier = Modifier.background(Color(0xFF2A2A2E))
                    )

                    Divider(color = Color.Gray.copy(alpha = 0.3f))

                    // Logout option
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Log out",
                                color = Color(0xFFFF5252),  // Red color for logout
                                fontFamily = myCustomFont,
                                fontSize = 16.sp
                            )
                        },
                        onClick = {
                            expanded = false
                            onLogoutClick()
                        },
                        modifier = Modifier.background(Color(0xFF2A2A2E))
                    )
                }
            }
        }
    }
}