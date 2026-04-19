package com.example.fitjournal_capstone_leandro.ui.stopwatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchBottomSheet(
    viewModel: StopwatchViewModel,
    onDismiss: () -> Unit
) {
    val timer by viewModel.timer.collectAsState()
    val isActive by viewModel.isActive.collectAsState()

    val sheetState = rememberModalBottomSheetState()

    // Format time as HH:MM:SS
    val hours = timer / 3600
    val minutes = (timer % 3600) / 60
    val seconds = timer % 60
    val timerDisplay = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1B1B1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Workout Timer",
                fontSize = 24.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = myCustomFont
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Timer Display
            Text(
                text = timerDisplay,
                fontSize = 56.sp,
                color = Color(0xFFFFEB3B),
                fontWeight = FontWeight.Bold,
                fontFamily = myCustomFont
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Start Button
                OutlinedButton(
                    onClick = { viewModel.startTimer() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Start")
                }

                // Pause/Resume Button
                if (isActive) {
                    OutlinedButton(
                        onClick = { viewModel.pauseTimer() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Pause")
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.startTimer() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Resume")
                    }
                }

                // Reset Button
                OutlinedButton(
                    onClick = { viewModel.resetTimer() },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    )
                ) {
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}