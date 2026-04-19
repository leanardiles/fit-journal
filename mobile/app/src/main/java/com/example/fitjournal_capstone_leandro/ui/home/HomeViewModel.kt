package com.example.fitjournal_capstone_leandro.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.data.repository.ExerciseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: ExerciseRepository) : ViewModel() {

    // Single StateFlow for the entire screen state
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()


    fun processAction(action: HomeScreenAction) {
        when (action) {
            // User wants to fetch muscle groups
            is HomeScreenAction.FetchMuscleGroups -> {
                // First, update state to Loading using reducer
                _state.value = homeScreenReducer(_state.value, action)

                // Then, perform side effect (network/database call)
                viewModelScope.launch {
                    try {
                        val muscleGroups = repository.getMuscleList()

                        // On success, trigger success action
                        processAction(
                            HomeScreenAction.FetchMuscleGroupsSuccess(muscleGroups)
                        )
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error fetching muscle groups", e)

                        // On failure, trigger failure action
                        processAction(
                            HomeScreenAction.FetchMuscleGroupsFailure(
                                e.message ?: "Failed to load muscle groups"
                            )
                        )
                    }
                }
            }

            // All other actions just update state through reducer
            is HomeScreenAction.FetchMuscleGroupsSuccess,
            is HomeScreenAction.FetchMuscleGroupsFailure,
            is HomeScreenAction.SelectMuscleGroup,
            is HomeScreenAction.ClearSelection -> {
                _state.value = homeScreenReducer(_state.value, action)
            }
        }
    }
}


 // Factory for creating HomeViewModel with dependencies
class HomeViewModelFactory(
    private val repository: ExerciseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}