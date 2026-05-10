package com.example.fitjournal_capstone_leandro.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
import com.example.fitjournal_capstone_leandro.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DashboardUiState {
    object Loading : DashboardUiState()
    object Success : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}

data class DashboardScreenState(
    val uiState: DashboardUiState = DashboardUiState.Loading,
    val routine: RoutineResponse? = null,
    val workoutsThisWeek: Int = 0,
    val currentDay: Int = 1
)

class DashboardViewModel(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardScreenState())
    val state: StateFlow<DashboardScreenState> = _state.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = DashboardUiState.Loading)

            val routineResult = repository.getRoutine()
            val workoutsResult = repository.getWorkoutsThisWeek()
            val currentDayResult = repository.getCurrentDay()

            if (routineResult.isSuccess && workoutsResult.isSuccess) {
                _state.value = DashboardScreenState(
                    uiState = DashboardUiState.Success,
                    routine = routineResult.getOrNull(),
                    workoutsThisWeek = workoutsResult.getOrNull() ?: 0,
                    currentDay = currentDayResult.getOrNull() ?: 1
                )
            } else {
                _state.value = _state.value.copy(
                    uiState = DashboardUiState.Error("Failed to load dashboard")
                )
            }
        }
    }
}

class DashboardViewModelFactory(
    private val repository: DashboardRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
