package com.example.fitjournal_capstone_leandro.ui.workout

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import androidx.navigation.NavHostController
import com.example.fitjournal_capstone_leandro.navigation.Routes
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
    navController: NavHostController,
    onWorkoutComplete: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.uiState) {
        if (state.uiState is WorkoutUiState.WorkoutComplete) {
            onWorkoutComplete()
            viewModel.reset()
        }
        if (state.uiState is WorkoutUiState.Idle) {
            viewModel.loadDayPicker()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
    ) {
        // Ask before auto-generating when a day has no selections.
        if (state.pendingGenerateDay != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelGenerate() },
                containerColor = Color(0xFF2C2C2E),
                title = { Text("No exercises selected", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont) },
                text = { Text("This day has no exercises selected. Generate one automatically?", color = Color.Gray, fontFamily = myCustomFont) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmGenerate() }) {
                        Text("Generate", color = AccentYellow, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelGenerate() }) {
                        Text("Cancel", color = Color.Gray, fontFamily = myCustomFont)
                    }
                }
            )
        }

        // Confirm setting a different day as current
        if (state.pendingDayPick != null) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelSetCurrentDay() },
                containerColor = Color(0xFF2C2C2E),
                title = { Text("Set as current day?", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont) },
                text = { Text("Make Day ${state.pendingDayPick} your current training day?", color = Color.Gray, fontFamily = myCustomFont) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmSetCurrentDay() }) {
                        Text("Set", color = AccentYellow, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelSetCurrentDay() }) {
                        Text("Cancel", color = Color.Gray, fontFamily = myCustomFont)
                    }
                }
            )
        }

        // Confirm discarding an in-progress workout
        if (state.pendingDiscard) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDiscard() },
                containerColor = Color(0xFF2C2C2E),
                title = { Text("Discard workout?", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont) },
                text = { Text("Your entered sets will be lost.", color = Color.Gray, fontFamily = myCustomFont) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDiscard() }) {
                        Text("Discard", color = AccentYellow, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDiscard() }) {
                        Text("Keep", color = Color.Gray, fontFamily = myCustomFont)
                    }
                }
            )
        }

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
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.days.isNotEmpty()) {
                        Text(text = "Current training day:", color = Color.Gray, fontSize = 13.sp, fontFamily = myCustomFont)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            state.days.forEach { d ->
                                DayChip(
                                    dayNumber = d.dayNumber,
                                    name = d.name,
                                    current = d.dayNumber == state.currentDay,
                                    onClick = { viewModel.requestSetCurrentDay(d.dayNumber) }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.createWorkout() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow, contentColor = Color.Black),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text("Generate Workout", fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = myCustomFont)
                        }
                        OutlinedButton(
                            onClick = { navController.navigate(Routes.MANUAL_LOG) },
                            border = BorderStroke(1.5.dp, AccentYellow),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(52.dp)
                        ) {
                            Text("Log Manually", color = AccentYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = myCustomFont)
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
                                setsValue = state.setsById[exercise.exercise_id] ?: "",
                                repsValue = state.repsById[exercise.exercise_id] ?: "",
                                weightValue = state.weightById[exercise.exercise_id] ?: "",
                                onSetsChange = { viewModel.setSets(exercise.exercise_id, it) },
                                onRepsChange = { viewModel.setReps(exercise.exercise_id, it) },
                                onWeightChange = { viewModel.setWeight(exercise.exercise_id, it) },
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

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { viewModel.requestDiscard() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Discard / New workout", color = Color.Gray, fontFamily = myCustomFont, fontSize = 14.sp)
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
    setsValue: String,
    repsValue: String,
    weightValue: String,
    onSetsChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    isDragging: Boolean = false,
    reorderableScope: ReorderableCollectionItemScope? = null
) {
    val nameColor = if (isChecked) Color.Gray else Color.White
    val nameDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None

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
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle — vertically centered over the whole card
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

            // Center: two lines (header + inputs)
            Column(modifier = Modifier.weight(1f)) {
                // Line 1: muscle group (inline) + exercise name
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = exercise.exercise_muscle_group,
                        color = AccentYellow.copy(alpha = if (isChecked) 0.4f else 1f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = myCustomFont
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = exercise.exercise_name,
                        color = nameColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = myCustomFont,
                        textDecoration = nameDecoration,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Line 2: Weight / Sets / Reps inline
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallNumberField(
                        label = "Weight (kg)",
                        value = weightValue,
                        onChange = onWeightChange,
                        decimal = true,
                        modifier = Modifier.weight(1f)
                    )
                    SmallNumberField(
                        label = "Sets",
                        value = setsValue,
                        onChange = onSetsChange,
                        modifier = Modifier.weight(1f)
                    )
                    SmallNumberField(
                        label = "Reps",
                        value = repsValue,
                        onChange = onRepsChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Check — vertically centered over the whole card
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
private fun SmallNumberField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    decimal: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, color = Color.Gray, fontSize = 11.sp, fontFamily = myCustomFont)
        Spacer(modifier = Modifier.height(2.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontFamily = myCustomFont, fontSize = 15.sp),
            cursorBrush = SolidColor(AccentYellow),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("–", color = Color(0xFF666666), fontFamily = myCustomFont, fontSize = 15.sp)
                }
                inner()
            }
        )
    }
}

@Composable
fun DayChip(
    dayNumber: Int,
    name: String?,
    current: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .border(1.5.dp, if (current) AccentYellow else Color.Gray, RoundedCornerShape(10.dp))
            .background(if (current) AccentYellow.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Day $dayNumber",
            color = if (current) AccentYellow else Color.White,
            fontFamily = myCustomFont,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        if (!name.isNullOrBlank()) {
            Text(
                text = name,
                color = if (current) AccentYellow.copy(alpha = 0.9f) else Color.Gray,
                fontFamily = myCustomFont,
                fontSize = 12.sp
            )
        }
    }
}