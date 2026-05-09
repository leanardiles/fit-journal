package com.example.fitjournal_capstone_leandro.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.data.repository.IUserRoutineRepository
import com.example.fitjournal_capstone_leandro.data.repository.UserRoutineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RoutineUiState {
    object Loading : RoutineUiState()
    object NoRoutine : RoutineUiState()
    object Editing : RoutineUiState()
    object Success : RoutineUiState()
    data class Error(val message: String) : RoutineUiState()
}

data class RoutineScreenState(
    val uiState: RoutineUiState = RoutineUiState.Loading,
    val daysPerWeek: Int = 0,
    val selectedDays: Int = 0,               // days selected in editor
    val routineDays: Map<String, List<String>> = emptyMap(),  // existing routine from backend
    val editingDays: Map<Int, List<String>> = emptyMap(),     // being built in editor
    val savedMessage: String? = null
)

class RoutineViewModel(
    private val repository: IUserRoutineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoutineScreenState())
    val state: StateFlow<RoutineScreenState> = _state.asStateFlow()

    val muscleGroups = listOf(
        "Legs", "Shoulders", "Chest", "Glutes",
        "Biceps", "Triceps", "Back", "Calves", "Abs"
    )

    init {
        loadRoutine()
    }

    /**
     * Load existing routine from backend
     */
    fun loadRoutine() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = RoutineUiState.Loading)
            val result = repository.getRoutine()
            if (result.isSuccess) {
                val routine = result.getOrNull()!!
                if (routine.days_per_week == 0) {
                    _state.value = _state.value.copy(uiState = RoutineUiState.NoRoutine)
                } else {
                    _state.value = _state.value.copy(
                        uiState = RoutineUiState.Success,
                        daysPerWeek = routine.days_per_week,
                        routineDays = routine.routine_days
                    )
                }
            } else {
                _state.value = _state.value.copy(
                    uiState = RoutineUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to load routine"
                    )
                )
            }
        }
    }

    /**
     * User selected number of training days — move to editing mode
     */
    fun selectDaysPerWeek(days: Int) {
        val editing = (1..days).associateWith { emptyList<String>() }
        _state.value = _state.value.copy(
            uiState = RoutineUiState.Editing,
            selectedDays = days,
            editingDays = editing
        )
    }

    /**
     * Toggle a muscle group for a specific day
     */
    fun toggleMuscleGroup(day: Int, muscle: String) {
        val current = _state.value.editingDays.toMutableMap()
        val dayMuscles = current[day]?.toMutableList() ?: mutableListOf()
        if (dayMuscles.contains(muscle)) {
            dayMuscles.remove(muscle)
        } else {
            dayMuscles.add(muscle)
        }
        current[day] = dayMuscles
        _state.value = _state.value.copy(editingDays = current)
    }

    /**
     * Save routine to backend
     */
    fun saveRoutine() {
        viewModelScope.launch {
            val days = _state.value.selectedDays
            val editingDays = _state.value.editingDays

            // Validate at least one day has a muscle group
            val hasSelection = editingDays.values.any { it.isNotEmpty() }
            if (!hasSelection) {
                _state.value = _state.value.copy(
                    uiState = RoutineUiState.Error("Please select at least one muscle group")
                )
                return@launch
            }

            val result = repository.saveRoutine(days, editingDays)
            if (result.isSuccess) {
                loadRoutine()
            } else {
                _state.value = _state.value.copy(
                    uiState = RoutineUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to save routine"
                    )
                )
            }
        }
    }

    /**
     * Switch back to editing mode from view mode
     */
    fun startEditing() {
        // Pre-populate editor with existing routine
        val existing = _state.value.routineDays
        val days = _state.value.daysPerWeek
        val editing = (1..days).associateWith { day ->
            existing[day.toString()] ?: emptyList()
        }
        _state.value = _state.value.copy(
            uiState = RoutineUiState.Editing,
            selectedDays = days,
            editingDays = editing
        )
    }

    /**
     * Cancel editing — go back to view if routine exists, NoRoutine if not
     */
    fun cancelEditing() {
        if (_state.value.daysPerWeek > 0) {
            _state.value = _state.value.copy(uiState = RoutineUiState.Success)
        } else {
            _state.value = _state.value.copy(uiState = RoutineUiState.NoRoutine)
        }
    }
}

class RoutineViewModelFactory(
    private val repository: IUserRoutineRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutineViewModel::class.java)) {
            return RoutineViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}