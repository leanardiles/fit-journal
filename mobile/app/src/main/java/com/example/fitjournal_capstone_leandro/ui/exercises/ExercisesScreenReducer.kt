package com.example.fitjournal_capstone_leandro.ui.exercises

import com.example.fitjournal_capstone_leandro.ui.exercises.ExercisesUiState

fun exercisesScreenReducer(
    currentState: ExercisesScreenState,
    action: ExercisesScreenAction
): ExercisesScreenState {
    return when (action) {

        // ========== Fetching Muscle Groups ==========

        // User wants to fetch muscle groups -> keep current state, just mark as loading
        is ExercisesScreenAction.FetchMuscleGroups -> {
            currentState.copy(
                // Keep current state, fetching in background
            )
        }

        // Successfully fetched muscle groups
        is ExercisesScreenAction.FetchMuscleGroupsSuccess -> {
            currentState.copy(
                muscleGroups = action.muscleGroups
            )
        }

        // Failed to fetch muscle groups
        is ExercisesScreenAction.FetchMuscleGroupsFailure -> {
            currentState.copy(
                errorMessage = action.errorMessage
            )
        }

        // ========== Fetching Exercises ==========

        // User wants to fetch exercises -> set to Loading state
        is ExercisesScreenAction.FetchExercises -> {
            currentState.copy(
                uiState = ExercisesUiState.Loading,
                selectedMuscleGroup = action.muscleGroup,
                errorMessage = null
            )
        }

        // Successfully fetched exercises -> update to Success state
        is ExercisesScreenAction.FetchExercisesSuccess -> {
            if (action.exercises.isEmpty()) {
                // No exercises returned -> Empty state
                currentState.copy(
                    uiState = ExercisesUiState.Empty,
                    exercises = emptyList(),
                    errorMessage = null,
                    isRefreshing = false
                )
            } else {
                // We have exercises -> Success state
                currentState.copy(
                    uiState = ExercisesUiState.Success(action.exercises),
                    exercises = action.exercises,
                    errorMessage = null,
                    isRefreshing = false
                )
            }
        }

        // Failed to fetch exercises -> Error state
        is ExercisesScreenAction.FetchExercisesFailure -> {
            currentState.copy(
                uiState = ExercisesUiState.Error(action.errorMessage),
                exercises = emptyList(),
                errorMessage = action.errorMessage,
                isRefreshing = false
            )
        }

        // ========== Filtering ==========

        // User selected a muscle group filter
        is ExercisesScreenAction.SelectMuscleGroup -> {
            currentState.copy(
                selectedMuscleGroup = action.muscleGroup
                // Note: The actual filtering will trigger a new FetchExercises action
                // in the ViewModel, which will then update the UI state
            )
        }

        // User cleared the muscle group filter
        is ExercisesScreenAction.ClearMuscleGroupFilter -> {
            currentState.copy(
                selectedMuscleGroup = null
                // Note: This will trigger a new FetchExercises action in the ViewModel
            )
        }

        // ========== Exercise Selection ==========

        // User selected an exercise to view details
        is ExercisesScreenAction.SelectExercise -> {
            currentState.copy(
                selectedExercise = action.exercise
            )
        }

        // User closed exercise details
        is ExercisesScreenAction.ClearExerciseSelection -> {
            currentState.copy(
                selectedExercise = null
            )
        }

        // ========== Refresh ==========

        // User triggered pull-to-refresh
        is ExercisesScreenAction.RefreshExercises -> {
            currentState.copy(
                isRefreshing = true,
                errorMessage = null
            )
        }

        // Refresh completed
        is ExercisesScreenAction.RefreshComplete -> {
            currentState.copy(
                isRefreshing = false
            )
        }
    }
}