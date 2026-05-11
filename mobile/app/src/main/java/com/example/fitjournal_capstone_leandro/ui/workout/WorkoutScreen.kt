package com.example.fitjournal_capstone_leandro.ui.workout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.rememberReorderableLazyListState


private val BackgroundDark = Color(0xFF1B1B1E)
private val AccentYellow = Color(0xFFFFEB3B)

@OptIn(ExperimentalFoundationApi::class)

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onWorkoutComplete: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

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

            is WorkoutUiState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Ready to train?", color = Color.Gray, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.createWorkout() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentYellow,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(0.7f).height(52.dp)
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

            is WorkoutUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentYellow)
                }
            }

            is WorkoutUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (state.uiState as WorkoutUiState.Error).message,
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.reset() }) { Text("Try Again") }
                    }
                }
            }

            is WorkoutUiState.WorkoutReady -> {
                Text(
                    text = state.todayDate,
                    color = AccentYellow,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(text = "Day ${state.currentDay}", color = Color.Gray, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Reorderable list state — must be inside the composable
                val lazyListState = rememberLazyListState()
                val haptic = LocalHapticFeedback.current
                val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    viewModel.reorderExercises(from.index, to.index)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.exercises, key = { it.exercise_id }) { exercise ->
                        ReorderableItem(reorderableLazyListState, key = exercise.exercise_id) { isDragging ->
                            ExerciseRow(
                                exercise = exercise,
                                isChecked = exercise.exercise_id in state.checkedExerciseIds,
                                onToggle = { viewModel.toggleExerciseChecked(exercise.exercise_id) },
                                onWeightUpdate = { id, weight ->
                                    viewModel.updateExerciseWeight(id, weight)
                                },
                                isDragging = isDragging,
                                reorderableScope = this
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.completeWorkout() },
                    enabled = state.checkedExerciseIds.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
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

@Composable
fun ExerciseRow(
    exercise: UserExercise,
    isChecked: Boolean,
    onToggle: () -> Unit,
    onWeightUpdate: (Int, Float?) -> Unit,
    isDragging: Boolean = false,
    reorderableScope: ReorderableCollectionItemScope? = null
) {
    val textColor = if (isChecked) Color.Gray else Color.White
    val textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
    var showWeightDialog by remember { mutableStateOf(false) }

    if (showWeightDialog) {
        WeightEditDialog(
            exerciseId = exercise.exercise_id,
            currentWeight = exercise.exercise_user_current_weight,
            onConfirm = { newWeight ->
                onWeightUpdate(exercise.exercise_id, newWeight)
                showWeightDialog = false
            },
            onDismiss = { showWeightDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isDragging) Modifier.shadow(8.dp, RoundedCornerShape(10.dp)) else Modifier),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) Color(0xFF3A3A3C)
            else if (isChecked) Color(0xFF1E1E1E)
            else Color(0xFF2C2C2E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            if (reorderableScope != null) {
                with(reorderableScope) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = "Drag to reorder",
                        tint = Color.Gray,
                        modifier = Modifier
                            .draggableHandle()
                            .padding(end = 8.dp)
                            .size(20.dp)
                    )
                }
            }

            // Left: muscle group + exercise name
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

            // Weight
            Text(
                text = if (exercise.exercise_user_current_weight != null &&
                    exercise.exercise_user_current_weight > 0)
                    "${exercise.exercise_user_current_weight} kg"
                else "— kg",
                color = AccentYellow.copy(alpha = if (isChecked) 0.4f else 1f),
                fontSize = 13.sp,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .clickable(enabled = !isChecked) { showWeightDialog = true },
                textDecoration = textDecoration
            )

            // Check button
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isChecked) Icons.Filled.CheckCircle
                    else Icons.Outlined.Circle,
                    contentDescription = if (isChecked) "Done" else "Mark as done",
                    tint = if (isChecked) AccentYellow else Color.Gray
                )
            }
        }
    }
}

@Composable
fun WeightEditDialog(
    exerciseId: Int,
    currentWeight: Float?,
    onConfirm: (Float?) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember {
        mutableStateOf(
            if (currentWeight != null && currentWeight > 0) currentWeight.toString() else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2C2C2E),
        title = {
            Text("Update Weight", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = weightText,
                onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Weight (kg)", color = Color.Gray) },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentYellow,
                    unfocusedBorderColor = Color(0xFF3A3A3C),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val newWeight = weightText.toFloatOrNull()
                onConfirm(newWeight)
            }) {
                Text("Save", color = AccentYellow, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}