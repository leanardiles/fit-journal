package com.example.fitjournal_capstone_leandro.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.analytics.AnalyticsLogger
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.repository.IUserExercisesRepository
import com.example.fitjournal_capstone_leandro.data.repository.UserExercisesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UserExercisesUiState {
    object Loading : UserExercisesUiState()
    object Success : UserExercisesUiState()
    data class Error(val message: String) : UserExercisesUiState()
}

data class UserExercisesScreenState(
    val uiState: UserExercisesUiState = UserExercisesUiState.Loading,
    val muscleGroups: List<String> = emptyList(),
    val selectedMuscleGroup: String? = null,
    val exercises: List<UserExercise> = emptyList(),
    val errorMessage: String? = null,
    val updateTick: Int = 0
)

class UserExercisesViewModel(
    private val repository: IUserExercisesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(UserExercisesScreenState())
    val state: StateFlow<UserExercisesScreenState> = _state.asStateFlow()

    private val _exerciseAdded = MutableStateFlow(false)
    val exerciseAdded: StateFlow<Boolean> = _exerciseAdded

    // Cache all exercises to avoid repeated API calls
    private var allExercises: List<UserExercise> = emptyList()

    init {
        fetchMuscleGroups()
    }

    /**
     * Fetch muscle groups from backend
     */
    fun fetchMuscleGroups() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = UserExercisesUiState.Loading)
            val result = repository.getMuscleGroups()
            if (result.isSuccess) {
                allExercises = repository.getExercises().getOrNull() ?: emptyList()
                _state.value = _state.value.copy(
                    uiState = UserExercisesUiState.Success,
                    muscleGroups = result.getOrNull() ?: emptyList()
                )
            } else {
                _state.value = _state.value.copy(
                    uiState = UserExercisesUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to load"
                    )
                )
            }
        }
    }

    /**
     * Select a muscle group and filter exercises
     */
    fun selectMuscleGroup(muscleGroup: String) {
        val filtered = allExercises
            .filter { it.exercise_muscle_group == muscleGroup }
            .sortedBy { it.exercise_name }
        _state.value = _state.value.copy(
            selectedMuscleGroup = muscleGroup,
            exercises = filtered
        )
    }

    /**
     * Add a new exercise
     */
    fun addExercise(name: String, muscleGroup: String) {
        // Check for duplicate name (case-insensitive)
        val duplicate = allExercises.any {
            it.exercise_name.trim().lowercase() == name.trim().lowercase()
        }
        if (duplicate) {
            _state.value = _state.value.copy(
                uiState = UserExercisesUiState.Error("An exercise with this name already exists")
            )
            return
        }

        viewModelScope.launch {
            val result = repository.createExercise(name, muscleGroup)
            if (result.isSuccess) {
                // Refresh all exercises from backend
                val exercisesResult = repository.getExercises()
                if (exercisesResult.isSuccess) {
                    allExercises = exercisesResult.getOrNull() ?: emptyList()
                    // Rebuild muscle groups and re-select current group
                    val muscleGroups = allExercises
                        .map { it.exercise_muscle_group }
                        .distinct()
                        .sorted()
                    _state.value = _state.value.copy(
                        muscleGroups = muscleGroups,
                        exercises = allExercises
                            .filter { it.exercise_muscle_group == muscleGroup }
                            .sortedBy { it.exercise_name }
                    )
                    _exerciseAdded.value = true
                }
            } else {
                _state.value = _state.value.copy(
                    uiState = UserExercisesUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to add exercise"
                    )
                )
            }
        }
    }


    fun resetError() {
        _state.value = _state.value.copy(uiState = UserExercisesUiState.Success)
    }

    fun resetExerciseAdded() {
        _exerciseAdded.value = false
    }


    /**
     * Delete an exercise
     */
    fun deleteExercise(exerciseId: Int) {
        viewModelScope.launch {
            val result = repository.deleteExercise(exerciseId)
            if (result.isSuccess) {
                // Re-fetch from backend instead of updating local cache
                val exercisesResult = repository.getExercises()
                if (exercisesResult.isSuccess) {
                    allExercises = exercisesResult.getOrNull() ?: emptyList()
                    val currentMuscle = _state.value.selectedMuscleGroup
                    val muscleGroups = allExercises
                        .map { it.exercise_muscle_group }
                        .distinct()
                        .sorted()
                    _state.value = UserExercisesScreenState(
                        uiState = UserExercisesUiState.Success,
                        muscleGroups = muscleGroups,
                        selectedMuscleGroup = currentMuscle,
                        exercises = if (currentMuscle != null) {
                            allExercises
                                .filter { it.exercise_muscle_group == currentMuscle }
                                .sortedBy { it.exercise_name }
                        } else emptyList(),
                        updateTick = _state.value.updateTick + 1
                    )
                }
            } else {
                _state.value = _state.value.copy(
                    uiState = UserExercisesUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to delete exercise"
                    )
                )
            }
        }
    }


    /**
     * Update the weight of an exercise
     */
    fun updateExerciseWeight(exerciseId: Int, weight: Float?) {
        viewModelScope.launch {
            val result = repository.updateExerciseWeight(exerciseId, weight)
            if (result.isSuccess) {
                AnalyticsLogger.logExerciseWeightUpdated(exerciseId, weight)
                // Update local cache
                allExercises = allExercises.map {
                    if (it.exercise_id == exerciseId) it.copy(exercise_user_current_weight = weight)
                    else it
                }
                val currentMuscle = _state.value.selectedMuscleGroup
                _state.value = UserExercisesScreenState(
                    uiState = UserExercisesUiState.Success,
                    muscleGroups = _state.value.muscleGroups,
                    selectedMuscleGroup = currentMuscle,
                    exercises = if (currentMuscle != null) {
                        allExercises.filter { it.exercise_muscle_group == currentMuscle }
                            .sortedBy { it.exercise_name }
                    } else emptyList(),
                    updateTick = _state.value.updateTick + 1
                )
            } else {
                _state.value = _state.value.copy(
                    uiState = UserExercisesUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to update weight"
                    )
                )
            }
        }
    }
}

class UserExercisesViewModelFactory(
    private val repository: UserExercisesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserExercisesViewModel::class.java)) {
            return UserExercisesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}