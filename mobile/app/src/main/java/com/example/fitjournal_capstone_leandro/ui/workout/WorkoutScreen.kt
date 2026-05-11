package com.example.fitjournal_capstone_leandro.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark = Color(0xFF2C2C2E)
private val AccentYellow = Color(0xFFFFEB3B)

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onWorkoutComplete: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    // Navigate away on completion
    LaunchedEffect(state.uiState) {
        if (state.uiState is WorkoutUiState.WorkoutComplete) {
            onWorkoutComplete()
            viewModel.reset()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Workout 🏋️",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (state.uiState) {

            // ── Idle: show Create Workout button ──
            is WorkoutUiState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Ready to train?",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.createWorkout() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentYellow,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(52.dp)
                        ) {
                            Text(
                                text = "Create Workout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // ── Loading ──
            is WorkoutUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentYellow)
                }
            }

            // ── Error ──
            is WorkoutUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (state.uiState as WorkoutUiState.Error).message,
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.reset() }) {
                            Text("Try Again")
                        }
                    }
                }
            }

            // ── Workout Ready: show exercise list ──
            is WorkoutUiState.WorkoutReady -> {
                // Date header
                Text(
                    text = state.todayDate,
                    color = AccentYellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Day ${state.currentDay}",
                    color = Color.Gray,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.exercises) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            isChecked = exercise.exercise_id in state.checkedExerciseIds,
                            onToggle = { viewModel.toggleExerciseChecked(exercise.exercise_id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mark as Complete button
                Button(
                    onClick = { viewModel.completeWorkout() },
                    enabled = state.checkedExerciseIds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentYellow,
                        contentColor = Color.Black,
                        disabledContainerColor = AccentYellow.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Mark Workout as Complete",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            is WorkoutUiState.WorkoutComplete -> {
                // handled by LaunchedEffect
            }
        }
    }
}

// ── Exercise Row ──
@Composable
fun ExerciseRow(
    exercise: UserExercise,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    val textColor = if (isChecked) Color.Gray else Color.White
    val textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) Color(0xFF1E1E1E) else Color(0xFF2C2C2E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: muscle group tag + exercise name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.exercise_muscle_group,
                    color = Color(0xFFFFEB3B).copy(alpha = if (isChecked) 0.4f else 1f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = exercise.exercise_name,
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = textDecoration
                )
            }

            // Center: weight
            Text(
                text = if (exercise.exercise_user_current_weight != null &&
                    exercise.exercise_user_current_weight > 0)
                    "${exercise.exercise_user_current_weight} kg"
                else "—",
                color = textColor.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
                textDecoration = textDecoration
            )

            // Right: check button
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isChecked) Icons.Filled.CheckCircle
                    else Icons.Outlined.Circle,
                    contentDescription = if (isChecked) "Done" else "Mark as done",
                    tint = if (isChecked) Color(0xFFFFEB3B) else Color.Gray
                )
            }
        }
    }
}