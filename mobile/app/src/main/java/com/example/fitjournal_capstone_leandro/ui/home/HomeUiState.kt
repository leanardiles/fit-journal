package com.example.fitjournal_capstone_leandro.ui.home

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(val muscleGroups: List<String>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
    object Empty : HomeUiState()
}