package com.example.fitjournal_capstone_leandro.ui.exercises

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExercisesViewModel(private val repository: ExerciseRepository) : ViewModel() {

        // Single StateFlow for the entire screen state
    private val _state = MutableStateFlow(ExercisesScreenState())
    val state: StateFlow<ExercisesScreenState> = _state.asStateFlow()

    fun processAction(action: ExercisesScreenAction) {
        when (action) {
            // User wants to fetch muscle groups
            is ExercisesScreenAction.FetchMuscleGroups -> {
                viewModelScope.launch {
                    try {
                        val muscleGroups = repository.getMuscleList()
                        processAction(
                            ExercisesScreenAction.FetchMuscleGroupsSuccess(muscleGroups)
                        )
                    } catch (e: Exception) {
                        Log.e("ExercisesViewModel", "Error fetching muscle groups", e)
                        processAction(
                            ExercisesScreenAction.FetchMuscleGroupsFailure(
                                e.message ?: "Failed to load muscle groups"
                            )
                        )
                    }
                }
            }

            // User wants to fetch exercises
            is ExercisesScreenAction.FetchExercises -> {
                // First, update state to Loading using reducer
                _state.value = exercisesScreenReducer(_state.value, action)

                // Then, perform side effect (network/database call)
                viewModelScope.launch {
                    try {
                        val exercises = action.muscleGroup?.let {
                            repository.getExercisesByMuscle(it)
                        } ?: emptyList()

                        // On success, trigger success action
                        processAction(
                            ExercisesScreenAction.FetchExercisesSuccess(exercises)
                        )
                    } catch (e: Exception) {
                        Log.e("ExercisesViewModel", "Error fetching exercises", e)

                        // On failure, trigger failure action
                        processAction(
                            ExercisesScreenAction.FetchExercisesFailure(
                                e.message ?: "Failed to load exercises"
                            )
                        )
                    }
                }
            }

            // User wants to refresh exercises
            is ExercisesScreenAction.RefreshExercises -> {
                // Mark as refreshing
                _state.value = exercisesScreenReducer(_state.value, action)

                // Fetch exercises again with current muscle group
                val currentMuscleGroup = _state.value.selectedMuscleGroup
                if (currentMuscleGroup != null) {
                    processAction(ExercisesScreenAction.FetchExercises(currentMuscleGroup))
                } else {
                    // No muscle group selected, just stop refreshing
                    processAction(ExercisesScreenAction.RefreshComplete)
                }
            }

            // User selected a muscle group filter
            is ExercisesScreenAction.SelectMuscleGroup -> {
                // Update state with selected muscle group
                _state.value = exercisesScreenReducer(_state.value, action)

                // Trigger fetch with the new muscle group
                processAction(ExercisesScreenAction.FetchExercises(action.muscleGroup))
            }

            // User cleared the filter
            is ExercisesScreenAction.ClearMuscleGroupFilter -> {
                // Update state
                _state.value = exercisesScreenReducer(_state.value, action)

                // Clear exercises (could optionally fetch all exercises here)
                processAction(ExercisesScreenAction.FetchExercisesSuccess(emptyList()))
            }

            // All other actions just update state through reducer
            is ExercisesScreenAction.FetchMuscleGroupsSuccess,
            is ExercisesScreenAction.FetchMuscleGroupsFailure,
            is ExercisesScreenAction.FetchExercisesSuccess,
            is ExercisesScreenAction.FetchExercisesFailure,
            is ExercisesScreenAction.SelectExercise,
            is ExercisesScreenAction.ClearExerciseSelection,
            is ExercisesScreenAction.RefreshComplete -> {
                _state.value = exercisesScreenReducer(_state.value, action)
            }
        }
    }
}



class ExercisesViewModelFactory(
    private val repository: ExerciseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExercisesViewModel::class.java)) {
            return ExercisesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}