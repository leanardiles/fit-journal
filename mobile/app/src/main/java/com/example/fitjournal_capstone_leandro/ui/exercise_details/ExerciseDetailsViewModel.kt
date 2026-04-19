package com.example.fitjournal_capstone_leandro.ui.exercise_details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fitjournal_capstone_leandro.data.model.Exercise
import com.example.fitjournal_capstone_leandro.data.repository.ExerciseRepository

class ExerciseDetailsViewModel(private val repo: ExerciseRepository) : ViewModel() {

    var selectedExercise by mutableStateOf<Exercise?>(null)
        private set

    fun selectExercise(exercise: Exercise) {
        selectedExercise = exercise
    }
}

class ExerciseDetailsViewModelFactory(
    private val repository: ExerciseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExerciseDetailsViewModel::class.java)) {
            return ExerciseDetailsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}