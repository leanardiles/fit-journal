package com.example.fitjournal_capstone_leandro.ui.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

private val AccentYellow = Color(0xFFFFEB3B)
private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark = Color(0xFF2C2C2E)
private val TextGray = Color(0xFF8E8E93)

@Composable
fun RoutineScreen(viewModel: RoutineViewModel) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when (state.uiState) {
            is RoutineUiState.Loading -> {
                CircularProgressIndicator(
                    color = AccentYellow,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is RoutineUiState.NoRoutine -> {
                NoRoutineContent(
                    onSelectDays = { viewModel.selectDaysPerWeek(it) }
                )
            }

            is RoutineUiState.Editing -> {
                EditingContent(
                    state = state,
                    muscleGroups = viewModel.muscleGroups,
                    onSelectDays = { viewModel.selectDaysPerWeek(it) },
                    onToggleMuscle = { day, muscle -> viewModel.toggleMuscleGroup(day, muscle) },
                    onSave = { viewModel.saveRoutine() },
                    onCancel = { viewModel.cancelEditing() }
                )
            }

            is RoutineUiState.Success -> {
                ViewRoutineContent(
                    state = state,
                    onEdit = { viewModel.startEditing() }
                )
            }

            is RoutineUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = (state.uiState as RoutineUiState.Error).message,
                        color = Color(0xFFFF453A),
                        fontFamily = myCustomFont,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadRoutine() },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
                    ) {
                        Text("Retry", color = Color.Black, fontFamily = myCustomFont)
                    }
                }
            }
        }
    }
}

// ─── NO ROUTINE ───────────────────────────────────────────────────────────────

@Composable
private fun NoRoutineContent(onSelectDays: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Routine",
            fontSize = 32.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "How many days per week\ndo you want to train?",
            fontSize = 20.sp,
            color = Color.White,
            fontFamily = myCustomFont,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        DaySelector(selectedDays = 0, onSelectDays = onSelectDays)
    }
}

// ─── EDITING ──────────────────────────────────────────────────────────────────

@Composable
private fun EditingContent(
    state: RoutineScreenState,
    muscleGroups: List<String>,
    onSelectDays: (Int) -> Unit,
    onToggleMuscle: (Int, String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Your Routine",
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = myCustomFont
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "How many days per week\ndo you want to train?",
                fontSize = 18.sp,
                color = Color.White,
                fontFamily = myCustomFont,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            DaySelector(
                selectedDays = state.selectedDays,
                onSelectDays = onSelectDays
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Select muscle groups for each day:",
                fontSize = 18.sp,
                color = Color.White,
                fontFamily = myCustomFont
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // One card per day
        items((1..state.selectedDays).toList()) { day ->
            DayCard(
                day = day,
                muscleGroups = muscleGroups,
                selectedMuscles = state.editingDays[day] ?: emptyList(),
                onToggleMuscle = { muscle -> onToggleMuscle(day, muscle) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text("Cancel", color = TextGray, fontFamily = myCustomFont)
                }
                Button(
                    onClick = onSave,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
                ) {
                    Text(
                        "Save Routine",
                        color = Color.Black,
                        fontFamily = myCustomFont,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── VIEW ROUTINE ─────────────────────────────────────────────────────────────

@Composable
private fun ViewRoutineContent(
    state: RoutineScreenState,
    onEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Routine",
            fontSize = 32.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Training ${state.daysPerWeek} days per week",
            fontSize = 16.sp,
            color = TextGray,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(24.dp))

        (1..state.daysPerWeek).forEach { day ->
            val muscles = state.routineDays[day.toString()] ?: emptyList()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "Day $day:",
                    fontSize = 16.sp,
                    color = AccentYellow,
                    fontWeight = FontWeight.Bold,
                    fontFamily = myCustomFont,
                    modifier = Modifier.width(60.dp)
                )
                Text(
                    text = if (muscles.isEmpty()) "Rest day" else muscles.joinToString(", "),
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = myCustomFont
                )
            }
            Divider(color = SurfaceDark)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onEdit,
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)
        ) {
            Text("Edit Routine", color = Color.White, fontFamily = myCustomFont)
        }
    }
}

// ─── SHARED COMPOSABLES ───────────────────────────────────────────────────────

@Composable
private fun DaySelector(selectedDays: Int, onSelectDays: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (1..7).forEach { day ->
            val isSelected = day == selectedDays
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) AccentYellow else Color.White,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        color = if (isSelected) AccentYellow.copy(alpha = 0.2f)
                        else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelectDays(day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$day",
                    color = if (isSelected) AccentYellow else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = myCustomFont,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun DayCard(
    day: Int,
    muscleGroups: List<String>,
    selectedMuscles: List<String>,
    onToggleMuscle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Day $day",
            fontSize = 18.sp,
            color = AccentYellow,
            fontWeight = FontWeight.Bold,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Muscle group toggle buttons — wrap in rows of 3
        val rows = muscleGroups.chunked(3)
        rows.forEach { rowMuscles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowMuscles.forEach { muscle ->
                    val isSelected = selectedMuscles.contains(muscle)
                    OutlinedButton(
                        onClick = { onToggleMuscle(muscle) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) AccentYellow.copy(alpha = 0.2f)
                            else Color.Transparent
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.5.dp,
                            color = if (isSelected) AccentYellow else Color.Gray
                        ),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(
                            text = muscle,
                            color = if (isSelected) AccentYellow else Color.White,
                            fontFamily = myCustomFont,
                            fontSize = 11.sp
                        )
                    }
                }
                // Fill empty slots in last row
                repeat(3 - rowMuscles.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Selected summary
        if (selectedMuscles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Selected: ${selectedMuscles.joinToString(", ")}",
                fontSize = 12.sp,
                color = TextGray,
                fontFamily = myCustomFont
            )
        }
    }
}