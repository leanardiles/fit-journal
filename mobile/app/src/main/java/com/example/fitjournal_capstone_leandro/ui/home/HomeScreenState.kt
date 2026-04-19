package com.example.fitjournal_capstone_leandro.ui.home


data class HomeScreenState(
    // The UI state from Week 9 (Loading, Success, Error, Empty)
    val uiState: HomeUiState = HomeUiState.Loading,

    // The actual list of muscle groups
    val muscleGroups: List<String> = emptyList(),

    // Currently selected muscle group (if any)
    val selectedMuscleGroup: String? = null,

    // Error message (if any)
    val errorMessage: String? = null
)