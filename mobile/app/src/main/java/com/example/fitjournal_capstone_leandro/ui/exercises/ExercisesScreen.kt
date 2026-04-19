package com.example.fitjournal_capstone_leandro.ui.exercises

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.exercises.ExercisesViewModel
import com.example.fitjournal_capstone_leandro.data.model.Exercise
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisesScreen(
    viewModel: ExercisesViewModel,
    onExerciseClick: (Exercise) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // Observe the MVI state
    val state by viewModel.state.collectAsState()

    // Fetch muscle groups on load using MVI action
    LaunchedEffect(Unit) {
        viewModel.processAction(ExercisesScreenAction.FetchMuscleGroups)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1E))
            .padding(16.dp)
    ) {
        Text(
            text = "Exercises",
            fontSize = 32.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Muscle Group:",
            fontSize = 18.sp,
            color = Color.White,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Dropdown
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = state.selectedMuscleGroup?.replaceFirstChar { it.uppercase() } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text(
                        text = "Select muscle group",
                        color = Color.Gray,
                        fontFamily = myCustomFont
                    )
                },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF2A2A2E),
                    unfocusedContainerColor = Color(0xFF2A2A2E),
                    disabledContainerColor = Color(0xFF2A2A2E),
                    focusedIndicatorColor = Color.White,
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = Color.White
                ),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                textStyle = TextStyle(
                    fontFamily = myCustomFont,
                    fontSize = 16.sp,
                    color = Color.White
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color(0xFF2A2A2E))
                    .exposedDropdownSize()
            ) {

                val muscleGroups = state.muscleGroups

                muscleGroups.forEach { muscle ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = muscle.replaceFirstChar { it.uppercase() },
                                color = Color.White,
                                fontFamily = myCustomFont,
                                fontSize = 16.sp
                            )
                        },
                        onClick = {
                            viewModel.processAction(
                                ExercisesScreenAction.SelectMuscleGroup(muscle)
                            )
                            expanded = false
                        },
                        modifier = Modifier.background(Color(0xFF2A2A2E))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        state.selectedMuscleGroup?.let { muscle ->
            Text(
                text = "Exercises for ${muscle.replaceFirstChar { it.uppercase() }}:",
                fontSize = 20.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = myCustomFont
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (state.uiState) {
            is ExercisesUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFEB3B))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading exercises...", color = Color.White)
                }
            }

            is ExercisesUiState.Success -> {
                val exercises = state.exercises
                LazyColumn {
                    items(exercises) { exercise ->
                        Text(
                            text = exercise.name.replaceFirstChar { it.uppercase() },
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = myCustomFont,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onExerciseClick(exercise) }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }

            is ExercisesUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        state.errorMessage ?: "Unknown error",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    state.selectedMuscleGroup?.let { muscle ->
                        Button(onClick = {
                            viewModel.processAction(
                                ExercisesScreenAction.FetchExercises(muscle)
                            )
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is ExercisesUiState.Empty -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📭 No Exercises", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No exercises found for this muscle group", color = Color.Gray)
                    Spacer(modifier = Modifier.height(24.dp))
                    state.selectedMuscleGroup?.let { muscle ->
                        Button(onClick = {
                            viewModel.processAction(
                                ExercisesScreenAction.RefreshExercises
                            )
                        }) {
                            Text("Refresh")
                        }
                    }
                }
            }
        }
    }
}