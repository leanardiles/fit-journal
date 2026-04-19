package com.example.fitjournal_capstone_leandro.ui.exercises

import com.example.fitjournal_capstone_leandro.data.model.Exercise

sealed class ExercisesUiState {
    object Loading : ExercisesUiState()
    data class Success(val exercises: List<Exercise>) : ExercisesUiState()
    data class Error(val message: String) : ExercisesUiState()
    object Empty : ExercisesUiState()
}