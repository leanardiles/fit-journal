package com.example.fitjournal_capstone_leandro.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class WorkoutUiState {
    object Idle : WorkoutUiState()
    object Loading : WorkoutUiState()
    object WorkoutReady : WorkoutUiState()
    object WorkoutComplete : WorkoutUiState()
    data class Error(val message: String) : WorkoutUiState()
}

data class WorkoutScreenState(
    val uiState: WorkoutUiState = WorkoutUiState.Idle,
    val exercises: List<UserExercise> = emptyList(),
    val checkedExerciseIds: Set<Int> = emptySet(),
    val currentDay: Int = 1,
    val todayDate: String = SimpleDateFormat(
        "EEEE, MMMM d", Locale.getDefault()
    ).format(Date())
)

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutScreenState())
    val state: StateFlow<WorkoutScreenState> = _state.asStateFlow()

    fun createWorkout() {
        android.util.Log.d("WorkoutVM", "createWorkout called")
        viewModelScope.launch {
            android.util.Log.d("WorkoutVM", "coroutine started")

            val stateResult = repository.getWorkoutState()
            android.util.Log.d("WorkoutVM", "stateResult: success=${stateResult.isSuccess}, error=${stateResult.exceptionOrNull()?.message}")

            if (stateResult.isFailure) {
                _state.value = _state.value.copy(
                    uiState = WorkoutUiState.Error("Failed to get workout state")
                )
                return@launch
            }
            val currentDay = stateResult.getOrNull()!!.current_day_number
            android.util.Log.d("WorkoutVM", "currentDay=$currentDay")

            repository.clearAllSelections()
            android.util.Log.d("WorkoutVM", "selections cleared")

            val generateResult = repository.generateWorkout(currentDay)
            android.util.Log.d("WorkoutVM", "generateResult: success=${generateResult.isSuccess}, error=${generateResult.exceptionOrNull()?.message}")

            if (generateResult.isFailure) {
                _state.value = _state.value.copy(
                    uiState = WorkoutUiState.Error("Failed to generate workout")
                )
                return@launch
            }

            // Fetch fresh selections
            val selections = repository.getSelections().getOrNull()
                ?.filter { it.is_selected }
                ?.map { it.exercise_id }
                ?: emptyList()

            loadExercisesForIds(selections, currentDay)
        }
    }

    private suspend fun loadExercisesForIds(ids: List<Int>, currentDay: Int) {
        val exercisesResult = repository.getExercises()
        if (exercisesResult.isFailure) {
            _state.value = _state.value.copy(
                uiState = WorkoutUiState.Error("Failed to load exercises")
            )
            return
        }
        val allExercises = exercisesResult.getOrNull() ?: emptyList()
        val workoutExercises = allExercises.filter { it.exercise_id in ids }

        _state.value = _state.value.copy(
            uiState = WorkoutUiState.WorkoutReady,
            exercises = workoutExercises,
            currentDay = currentDay,
            checkedExerciseIds = emptySet()
        )
    }

    fun toggleExerciseChecked(exerciseId: Int) {
        val current = _state.value.checkedExerciseIds.toMutableSet()
        if (current.contains(exerciseId)) {
            current.remove(exerciseId)
        } else {
            current.add(exerciseId)
        }
        _state.value = _state.value.copy(checkedExerciseIds = current)
    }

    fun completeWorkout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = WorkoutUiState.Loading)
            val result = repository.completeWorkout(
                _state.value.currentDay,
                _state.value.exercises
            )
            if (result.isSuccess) {
                _state.value = _state.value.copy(uiState = WorkoutUiState.WorkoutComplete)
            } else {
                _state.value = _state.value.copy(
                    uiState = WorkoutUiState.Error("Failed to complete workout")
                )
            }
        }
    }

    fun reset() {
        _state.value = WorkoutScreenState()
    }

    fun updateExerciseWeight(exerciseId: Int, weight: Float?) {
        viewModelScope.launch {
            repository.updateExerciseWeight(exerciseId, weight)
            // Update local state
            val updated = _state.value.exercises.map { ex ->
                if (ex.exercise_id == exerciseId) ex.copy(exercise_user_current_weight = weight)
                else ex
            }
            _state.value = _state.value.copy(exercises = updated)
        }
    }
}

class WorkoutViewModelFactory(
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            return WorkoutViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}