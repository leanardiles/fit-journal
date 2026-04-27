package com.example.fitjournal_capstone_leandro.ui.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont
import kotlinx.coroutines.launch

private val AccentYellow = Color(0xFFFFFFFF)
private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark = Color(0xFF2C2C2E)
private val TextGray = Color(0xFF8E8E93)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserExercisesScreen(
    viewModel: UserExercisesViewModel,
    unitPreference: String = "metric"
) {
    val state by viewModel.state.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var exerciseToEdit by remember { mutableStateOf<UserExercise?>(null) }
    var exerciseToDelete by remember { mutableStateOf<Int?>(null) }

    val errorMessage = if (state.uiState is UserExercisesUiState.Error) {
        (state.uiState as UserExercisesUiState.Error).message
    } else null

    val exerciseAdded by viewModel.exerciseAdded.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(exerciseAdded) {
        if (exerciseAdded && showAddDialog) {
            showAddDialog = false
            viewModel.resetExerciseAdded()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "Exercises",
                fontSize = 32.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = myCustomFont
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Muscle group dropdown label
            Text(
                text = "Muscle Group:",
                fontSize = 16.sp,
                color = Color.White,
                fontFamily = myCustomFont
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Muscle group dropdown
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.selectedMuscleGroup?.replaceFirstChar { it.uppercase() } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    placeholder = {
                        Text("Select muscle group", color = TextGray, fontFamily = myCustomFont)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        focusedBorderColor = AccentYellow,
                        unfocusedBorderColor = TextGray,
                    ),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    textStyle = TextStyle(fontFamily = myCustomFont, fontSize = 16.sp)
                )

                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    state.muscleGroups.forEach { muscle ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = muscle.replaceFirstChar { it.uppercase() },
                                    color = Color.White,
                                    fontFamily = myCustomFont
                                )
                            },
                            onClick = {
                                viewModel.selectMuscleGroup(muscle)
                                dropdownExpanded = false
                            },
                            modifier = Modifier.background(SurfaceDark)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Exercise list header + Add button
            state.selectedMuscleGroup?.let { muscle ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = muscle.replaceFirstChar { it.uppercase() },
                        fontSize = 20.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = myCustomFont
                    )
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add exercise",
                            tint = AccentYellow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Exercise list
                if (state.exercises.isEmpty()) {
                    Text(
                        text = "No exercises for this muscle group.",
                        color = TextGray,
                        fontFamily = myCustomFont
                    )
                } else {
                    LazyColumn {
                        items(state.exercises) { exercise ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Name + weight
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.exercise_name,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontFamily = myCustomFont
                                    )
                                    // Weight display
                                    val weightDisplay = if (exercise.exercise_user_current_weight != null) {
                                        if (unitPreference == "imperial") {
                                            val lbs = exercise.exercise_user_current_weight * 2.20462f
                                            "${"%.1f".format(lbs)} lbs"
                                        } else {
                                            "${exercise.exercise_user_current_weight} kg"
                                        }
                                    } else {
                                        if (unitPreference == "imperial") "0.0 lbs" else "0.0 kg"
                                    }
                                    Text(
                                        text = weightDisplay,
                                        fontSize = 12.sp,
                                        color = TextGray,
                                        fontFamily = myCustomFont
                                    )
                                }

                                // Edit icon
                                IconButton(onClick = { exerciseToEdit = exercise }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit weight",
                                        tint = AccentYellow,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Delete icon
                                IconButton(onClick = { exerciseToDelete = exercise.exercise_id }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFFFFFFF)
                                    )
                                }
                            }
                            Divider(color = SurfaceDark)
                        }
                    }
                }
            }

            // Loading / Error states
            when (val uiState = state.uiState) {
                is UserExercisesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AccentYellow)
                    }
                }
                is UserExercisesUiState.Error -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.message,
                            color = Color(0xFFFF453A),
                            fontFamily = myCustomFont
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.fetchMuscleGroups() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
                        ) {
                            Text("Retry", color = Color.Black, fontFamily = myCustomFont)
                        }
                    }
                }
                else -> {}
            }

        } // Column closes here

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

    } // Box closes here

    // Add exercise dialog
    if (showAddDialog) {
        AddExerciseDialog(
            currentMuscleGroup = state.selectedMuscleGroup ?: "",
            muscleGroups = state.muscleGroups,
            errorMessage = errorMessage,
            onConfirm = { name, muscle ->
                viewModel.addExercise(name, muscle)
                scope.launch {
                    snackbarHostState.showSnackbar("Exercise added")
                }
            },
            onDismiss = {
                showAddDialog = false
                viewModel.resetError()
            }
        )
    }

    // Delete confirmation dialog
    exerciseToDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            containerColor = SurfaceDark,
            title = {
                Text("Delete Exercise", color = Color.White, fontFamily = myCustomFont)
            },
            text = {
                Text(
                    "Are you sure you want to delete this exercise?",
                    color = TextGray,
                    fontFamily = myCustomFont
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExercise(id)
                    exerciseToDelete = null
                    scope.launch {
                        snackbarHostState.showSnackbar("Exercise deleted")
                    }
                }) {
                    Text("Delete", color = Color(0xFFFF453A), fontFamily = myCustomFont)
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseToDelete = null }) {
                    Text("Cancel", color = Color.White, fontFamily = myCustomFont)
                }
            }
        )
    }
    // Edit weight dialog
    exerciseToEdit?.let { exercise ->
        EditWeightDialog(
            exerciseName = exercise.exercise_name,
            currentWeight = exercise.exercise_user_current_weight,
            unitPreference = unitPreference,
            onConfirm = { weightKg ->
                viewModel.updateExerciseWeight(exercise.exercise_id, weightKg)
                exerciseToEdit = null
                scope.launch {
                    snackbarHostState.showSnackbar("Weight updated")
                }
            },
            onDismiss = { exerciseToEdit = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExerciseDialog(
    currentMuscleGroup: String,
    muscleGroups: List<String>,
    errorMessage: String? = null,
    onConfirm: (name: String, muscleGroup: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf(currentMuscleGroup) }
    var muscleDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add Exercise",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = myCustomFont
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise name", color = TextGray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentYellow,
                        unfocusedBorderColor = TextGray,
                        focusedContainerColor = Color(0xFF1B1B1E),
                        unfocusedContainerColor = Color(0xFF1B1B1E),
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Muscle group dropdown
                ExposedDropdownMenuBox(
                    expanded = muscleDropdownExpanded,
                    onExpandedChange = { muscleDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedMuscle.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Muscle group", color = TextGray) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = muscleDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentYellow,
                            unfocusedBorderColor = TextGray,
                            focusedContainerColor = Color(0xFF1B1B1E),
                            unfocusedContainerColor = Color(0xFF1B1B1E),
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = muscleDropdownExpanded,
                        onDismissRequest = { muscleDropdownExpanded = false },
                        modifier = Modifier.background(SurfaceDark)
                    ) {
                        muscleGroups.forEach { muscle ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        muscle.replaceFirstChar { it.uppercase() },
                                        color = Color.White,
                                        fontFamily = myCustomFont
                                    )
                                },
                                onClick = {
                                    selectedMuscle = muscle
                                    muscleDropdownExpanded = false
                                },
                                modifier = Modifier.background(SurfaceDark)
                            )
                        }
                    }
                }

                // Show error if any
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = Color(0xFFFF453A),
                        fontSize = 13.sp,
                        fontFamily = myCustomFont
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextGray, fontFamily = myCustomFont)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) onConfirm(name.trim(), selectedMuscle)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
                    ) {
                        Text("Add", color = Color.Black, fontFamily = myCustomFont)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditWeightDialog(
    exerciseName: String,
    currentWeight: Float?,
    unitPreference: String,
    onConfirm: (Float?) -> Unit,
    onDismiss: () -> Unit
) {
    // Convert current weight for display
    val displayWeight = if (currentWeight != null && currentWeight > 0) {
        if (unitPreference == "imperial") {
            "%.1f".format(currentWeight * 2.20462f)
        } else {
            "$currentWeight"
        }
    } else ""

    var weightInput by remember { mutableStateOf(displayWeight) }
    val unit = if (unitPreference == "imperial") "lbs" else "kg"

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Weight",
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = myCustomFont
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exerciseName,
                    fontSize = 13.sp,
                    color = TextGray,
                    fontFamily = myCustomFont
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight ($unit)", color = TextGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = AccentYellow,
                        unfocusedBorderColor = TextGray,
                        focusedContainerColor = BackgroundDark,
                        unfocusedContainerColor = BackgroundDark,
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextGray, fontFamily = myCustomFont)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val input = weightInput.toFloatOrNull()
                            // Convert back to kg if imperial
                            val weightKg = if (unitPreference == "imperial" && input != null) {
                                input / 2.20462f
                            } else input
                            onConfirm(weightKg)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
                    ) {
                        Text("Save", color = Color.Black, fontFamily = myCustomFont)
                    }
                }
            }
        }
    }
}