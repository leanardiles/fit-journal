package com.example.fitjournal_capstone_leandro.ui.exercises

import com.example.fitjournal_capstone_leandro.ui.exercises.ExercisesUiState
import com.example.fitjournal_capstone_leandro.data.model.Exercise

data class ExercisesScreenState(
    // The UI state from Week 9 (Loading, Success, Error, Empty)
    val uiState: ExercisesUiState = ExercisesUiState.Loading,

    // The actual list of exercises fetched from API
    val exercises: List<Exercise> = emptyList(),

    // List of all muscle groups available
    val muscleGroups: List<String> = emptyList(),

    // Currently selected muscle group filter (null = show all)
    val selectedMuscleGroup: String? = null,

    // Currently selected exercise for details view
    val selectedExercise: Exercise? = null,

    // Error message (if any)
    val errorMessage: String? = null,

    // Is the data being refreshed?
    val isRefreshing: Boolean = false
)