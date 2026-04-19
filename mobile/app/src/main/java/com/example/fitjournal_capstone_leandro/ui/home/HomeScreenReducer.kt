package com.example.fitjournal_capstone_leandro.ui.home


fun homeScreenReducer(
    currentState: HomeScreenState,
    action: HomeScreenAction
): HomeScreenState {
    return when (action) {
        // User wants to fetch muscle groups -> set to Loading state
        is HomeScreenAction.FetchMuscleGroups -> {
            currentState.copy(
                uiState = HomeUiState.Loading,
                errorMessage = null
            )
        }

        // Successfully fetched muscle groups -> update to Success state
        is HomeScreenAction.FetchMuscleGroupsSuccess -> {
            if (action.muscleGroups.isEmpty()) {
                // No muscle groups returned -> Empty state
                currentState.copy(
                    uiState = HomeUiState.Empty,
                    muscleGroups = emptyList(),
                    errorMessage = null
                )
            } else {
                // We have muscle groups -> Success state
                currentState.copy(
                    uiState = HomeUiState.Success(action.muscleGroups),
                    muscleGroups = action.muscleGroups,
                    errorMessage = null
                )
            }
        }

        // Failed to fetch muscle groups -> Error state
        is HomeScreenAction.FetchMuscleGroupsFailure -> {
            currentState.copy(
                uiState = HomeUiState.Error(action.errorMessage),
                muscleGroups = emptyList(),
                errorMessage = action.errorMessage
            )
        }

        // User selected a muscle group
        is HomeScreenAction.SelectMuscleGroup -> {
            currentState.copy(
                selectedMuscleGroup = action.muscleGroup
            )
        }

        // User cleared the selection
        is HomeScreenAction.ClearSelection -> {
            currentState.copy(
                selectedMuscleGroup = null
            )
        }
    }
}