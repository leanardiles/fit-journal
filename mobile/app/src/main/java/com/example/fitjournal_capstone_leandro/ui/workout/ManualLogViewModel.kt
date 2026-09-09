package com.example.fitjournal_capstone_leandro.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.data.model.ManualLogEntry
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

sealed class ManualLogUiState {
    object Editing : ManualLogUiState()
    object Submitting : ManualLogUiState()
    object Done : ManualLogUiState()
    data class Error(val message: String) : ManualLogUiState()
}

data class ManualLogState(
    val uiState: ManualLogUiState = ManualLogUiState.Editing,
    // Chosen date as epoch millis (UTC midnight, from the date picker). null = today.
    val dateMillis: Long = todayMillis(),
    val maxDateMillis: Long = todayMillis(),   // cap the picker at today (no future logs)
    val exercisesByMuscle: Map<String, List<UserExercise>> = emptyMap(),
    val muscles: List<String> = emptyList(),           // muscles with exercises, alphabetized
    val activeMuscles: Set<String> = emptySet(),       // which muscle sections are revealed (picker)
    val pickedIds: Set<Int> = emptySet(),              // exercises chosen for this log
    val setsById: Map<Int, String> = emptyMap(),
    val repsById: Map<Int, String> = emptyMap(),
    val weightById: Map<Int, String> = emptyMap()
) {
    companion object
}

private fun todayMillis(): Long {
    // Material3's DatePicker works in UTC-midnight millis. Build UTC midnight
    // for the LOCAL current date so the default and the wire value agree.
    val local = Calendar.getInstance()
    val y = local.get(Calendar.YEAR)
    val m = local.get(Calendar.MONTH)
    val d = local.get(Calendar.DAY_OF_MONTH)
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(y, m, d, 0, 0, 0)
    utc.set(Calendar.MILLISECOND, 0)
    return utc.timeInMillis
}

class ManualLogViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ManualLogState())
    val state: StateFlow<ManualLogState> = _state.asStateFlow()

    private val muscleOrder = listOf(
        "Abs", "Back", "Biceps", "Calves", "Chest", "Glutes", "Legs", "Shoulders", "Triceps"
    )

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            val byMuscle = repository.getExercises().getOrNull().orEmpty()
                .groupBy { it.exercise_muscle_group }
            val muscles = byMuscle.keys.sorted()
            _state.value = _state.value.copy(exercisesByMuscle = byMuscle, muscles = muscles)
        }
    }

    fun setDate(millis: Long) {
        _state.value = _state.value.copy(dateMillis = millis)
    }

    // ---- picker (mirrors the routine editor's manual picker) ----

    fun toggleMuscleTab(muscle: String) {
        val active = _state.value.activeMuscles.toMutableSet()
        if (active.contains(muscle)) active.remove(muscle) else active.add(muscle)
        _state.value = _state.value.copy(activeMuscles = active)
    }

    fun toggleExercise(exerciseId: Int) {
        val picked = _state.value.pickedIds.toMutableSet()
        if (picked.contains(exerciseId)) picked.remove(exerciseId) else picked.add(exerciseId)
        _state.value = _state.value.copy(pickedIds = picked)
    }

    // ---- per-exercise inputs ----

    fun setSets(exerciseId: Int, value: String) {
        val cleaned = value.filter { it.isDigit() }.take(2)
        _state.value = _state.value.copy(setsById = _state.value.setsById + (exerciseId to cleaned))
    }

    fun setReps(exerciseId: Int, value: String) {
        val cleaned = value.filter { it.isDigit() }.take(3)
        _state.value = _state.value.copy(repsById = _state.value.repsById + (exerciseId to cleaned))
    }

    fun setWeight(exerciseId: Int, value: String) {
        val cleaned = buildString {
            var dot = false
            for (c in value) {
                if (c.isDigit()) append(c)
                else if (c == '.' && !dot) { append(c); dot = true }
            }
        }.take(6)
        _state.value = _state.value.copy(weightById = _state.value.weightById + (exerciseId to cleaned))
    }

    /** All picked exercises as objects, in muscle-then-name order. */
    fun pickedExercises(): List<UserExercise> {
        val st = _state.value
        val all = st.exercisesByMuscle.values.flatten()
        return all.filter { it.exercise_id in st.pickedIds }
    }

    fun submit(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val st = _state.value
            val picked = pickedExercises()
            if (picked.isEmpty()) {
                _state.value = st.copy(uiState = ManualLogUiState.Error("Pick at least one exercise"))
                return@launch
            }
            val entries = ArrayList<ManualLogEntry>()
            for (ex in picked) {
                val sets = st.setsById[ex.exercise_id]?.toIntOrNull()
                if (sets == null || sets < 1) {
                    _state.value = st.copy(uiState = ManualLogUiState.Error("Enter sets for ${ex.exercise_name}"))
                    return@launch
                }
                val reps = st.repsById[ex.exercise_id]?.toIntOrNull() ?: 0
                val weight = st.weightById[ex.exercise_id]?.toFloatOrNull()
                entries.add(
                    ManualLogEntry(
                        exercise_id = ex.exercise_id,
                        sets_completed = sets,
                        reps_completed = reps,
                        weight_used = weight
                    )
                )
            }

            _state.value = st.copy(uiState = ManualLogUiState.Submitting)
            val iso = isoDate(st.dateMillis)
            val result = repository.logManual(iso, entries)
            if (result.isSuccess) {
                _state.value = _state.value.copy(uiState = ManualLogUiState.Done)
                onSuccess()
            } else {
                _state.value = _state.value.copy(
                    uiState = ManualLogUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to log workout"
                    )
                )
            }
        }
    }

    fun clearError() {
        if (_state.value.uiState is ManualLogUiState.Error) {
            _state.value = _state.value.copy(uiState = ManualLogUiState.Editing)
        }
    }

    private fun isoDate(millis: Long): String {
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        c.timeInMillis = millis
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }
}

class ManualLogViewModelFactory(
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ManualLogViewModel::class.java)) {
            return ManualLogViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}