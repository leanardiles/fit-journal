package com.example.fitjournal_capstone_leandro.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.analytics.AnalyticsLogger
import com.example.fitjournal_capstone_leandro.data.model.ExerciseLog
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class WorkoutUiState {
    object Idle : WorkoutUiState()
    object Loading : WorkoutUiState()
    object WorkoutReady : WorkoutUiState()
    object WorkoutComplete : WorkoutUiState()
    data class Error(val message: String) : WorkoutUiState()
}

data class WorkoutDay(val dayNumber: Int, val name: String?)

data class WorkoutScreenState(
    val uiState: WorkoutUiState = WorkoutUiState.Idle,
    val exercises: List<UserExercise> = emptyList(),
    val checkedExerciseIds: Set<Int> = emptySet(),
    val setsById: Map<Int, String> = emptyMap(),   // per-exercise input, keyed by id (survives reorder)
    val repsById: Map<Int, String> = emptyMap(),
    val weightById: Map<Int, String> = emptyMap(),   // inline weight input, keyed by id
    val currentDay: Int = 1,
    val days: List<WorkoutDay> = emptyList(),   // routine days for the picker (Idle screen)
    val pendingGenerateDay: Int? = null,   // set when a day has no selections and we're asking to auto-generate
    val pendingDayPick: Int? = null,       // a day tapped in the picker, awaiting confirm
    val pendingDiscard: Boolean = false,   // discard confirm (only when a workout is in progress)
    val todayDate: String = SimpleDateFormat(
        "EEEE, MMMM d", Locale.getDefault()
    ).format(Date())
)

class WorkoutViewModel(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutScreenState())
    val state: StateFlow<WorkoutScreenState> = _state.asStateFlow()

    /** Load the routine days + current day for the Idle-screen picker. */
    fun loadDayPicker() {
        viewModelScope.launch {
            val current = repository.getWorkoutState().getOrNull()?.current_day_number ?: 1
            val days = repository.getRoutine().getOrNull()?.days
                ?.sortedBy { it.day_number }
                ?.map { WorkoutDay(it.day_number, it.name) }
                ?: emptyList()
            _state.value = _state.value.copy(currentDay = current, days = days)
        }
    }

    /** Tapped a day chip — confirm before making it the current day. */
    fun requestSetCurrentDay(day: Int) {
        if (day == _state.value.currentDay) return
        _state.value = _state.value.copy(pendingDayPick = day)
    }

    fun confirmSetCurrentDay() {
        val day = _state.value.pendingDayPick ?: return
        viewModelScope.launch {
            val res = repository.setCurrentDay(day)
            if (res.isSuccess) {
                _state.value = _state.value.copy(currentDay = day, pendingDayPick = null)
            } else {
                _state.value = _state.value.copy(
                    pendingDayPick = null,
                    uiState = WorkoutUiState.Error("Failed to set current day")
                )
            }
        }
    }

    fun cancelSetCurrentDay() {
        _state.value = _state.value.copy(pendingDayPick = null)
    }

    // ---- Discard (Option C): the WOD persists until explicitly discarded ----
    private fun hasProgress(): Boolean {
        val st = _state.value
        return st.checkedExerciseIds.isNotEmpty() ||
                st.setsById.values.any { it.isNotBlank() } ||
                st.repsById.values.any { it.isNotBlank() }
    }

    fun requestDiscard() {
        if (hasProgress()) _state.value = _state.value.copy(pendingDiscard = true) else reset()
    }

    fun confirmDiscard() { reset() }

    fun cancelDiscard() {
        _state.value = _state.value.copy(pendingDiscard = false)
    }

    fun createWorkout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = WorkoutUiState.Loading)

            val stateResult = repository.getWorkoutState()
            if (stateResult.isFailure) {
                _state.value = _state.value.copy(uiState = WorkoutUiState.Error("Failed to get workout state"))
                return@launch
            }
            val currentDay = stateResult.getOrNull()!!.current_day_number

            // The current day's exercises (its pool for per_muscle, exact list for manual).
            val dayExerciseIds = repository.getRoutine().getOrNull()
                ?.days?.find { it.day_number == currentDay }
                ?.exercises?.map { it.exercise_id }?.toSet()
                ?: emptySet()

            val selectedIds = repository.getSelections().getOrNull()
                ?.filter { it.is_selected }
                ?.map { it.exercise_id }
                ?: emptyList()

            // Selections are day-less (one global set). Keep only the selected
            // exercises that belong to TODAY's day — same as web. If any remain,
            // that's today's workout; if none match today, prompt to generate.
            val todaysSelected = selectedIds.filter { it in dayExerciseIds }

            if (todaysSelected.isNotEmpty()) {
                loadExercisesForIds(todaysSelected, currentDay)
            } else {
                _state.value = _state.value.copy(
                    uiState = WorkoutUiState.Idle,
                    pendingGenerateDay = currentDay
                )
            }
        }
    }

    /** User confirmed auto-generation for a day with no selections. */
    fun confirmGenerate() {
        val day = _state.value.pendingGenerateDay ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = WorkoutUiState.Loading, pendingGenerateDay = null)
            val generateResult = repository.generateWorkout(day)
            if (generateResult.isFailure) {
                _state.value = _state.value.copy(uiState = WorkoutUiState.Error("Failed to generate workout"))
                return@launch
            }
            val selections = repository.getSelections().getOrNull()
                ?.filter { it.is_selected }
                ?.map { it.exercise_id }
                ?: emptyList()
            loadExercisesForIds(selections, day)
        }
    }

    /** User declined auto-generation. */
    fun cancelGenerate() {
        _state.value = _state.value.copy(uiState = WorkoutUiState.Idle, pendingGenerateDay = null)
    }

    private suspend fun loadExercisesForIds(ids: List<Int>, currentDay: Int) {
        val exercisesResult = repository.getExercises()
        if (exercisesResult.isFailure) {
            _state.value = _state.value.copy(
                uiState = WorkoutUiState.Error("Failed to load exercises")
            )
            return
        }
        val allExercises = exercisesResult.getOrNull() ?: emptyList()
        val workoutExercises = allExercises.filter { it.exercise_id in ids }

        val weights = workoutExercises.associate { ex ->
            val w = ex.exercise_user_current_weight
            val text = if (w != null && w > 0f) {
                if (w % 1f == 0f) w.toInt().toString() else w.toString()
            } else ""
            ex.exercise_id to text
        }
        _state.value = _state.value.copy(
            uiState = WorkoutUiState.WorkoutReady,
            exercises = workoutExercises,
            currentDay = currentDay,
            checkedExerciseIds = emptySet(),
            setsById = emptyMap(),
            repsById = emptyMap(),
            weightById = weights
        )
    }

    fun toggleExerciseChecked(exerciseId: Int) {
        val current = _state.value.checkedExerciseIds.toMutableSet()
        if (current.contains(exerciseId)) {
            current.remove(exerciseId)
        } else {
            current.add(exerciseId)
        }
        _state.value = _state.value.copy(checkedExerciseIds = current)
    }

    fun setSets(exerciseId: Int, value: String) {
        val cleaned = value.filter { it.isDigit() }.take(2)
        _state.value = _state.value.copy(setsById = _state.value.setsById + (exerciseId to cleaned))
    }

    fun setReps(exerciseId: Int, value: String) {
        val cleaned = value.filter { it.isDigit() }.take(3)
        _state.value = _state.value.copy(repsById = _state.value.repsById + (exerciseId to cleaned))
    }

    fun setWeight(exerciseId: Int, value: String) {
        // digits + at most one decimal point
        val cleaned = buildString {
            var dot = false
            for (c in value) {
                if (c.isDigit()) append(c)
                else if (c == '.' && !dot) { append(c); dot = true }
            }
        }.take(6)
        _state.value = _state.value.copy(weightById = _state.value.weightById + (exerciseId to cleaned))
    }

    fun completeWorkout() {
        viewModelScope.launch {
            val st = _state.value
            val checked = st.exercises.filter { it.exercise_id in st.checkedExerciseIds }
            if (checked.isEmpty()) {
                _state.value = st.copy(uiState = WorkoutUiState.Error("Check the exercises you completed"))
                return@launch
            }
            // Each checked exercise needs a sets value (reps optional).
            val logs = ArrayList<ExerciseLog>()
            for (ex in checked) {
                val sets = st.setsById[ex.exercise_id]?.toIntOrNull()
                if (sets == null || sets < 1) {
                    _state.value = st.copy(
                        uiState = WorkoutUiState.Error("Enter sets for ${ex.exercise_name}")
                    )
                    return@launch
                }
                val reps = st.repsById[ex.exercise_id]?.toIntOrNull() ?: 0
                val weight = st.weightById[ex.exercise_id]?.toFloatOrNull()
                    ?: ex.exercise_user_current_weight ?: 0f
                logs.add(
                    ExerciseLog(
                        exercise_id = ex.exercise_id,
                        sets_completed = sets,
                        reps_completed = reps,
                        weight_used = weight
                    )
                )
            }

            _state.value = st.copy(uiState = WorkoutUiState.Loading)
            val result = repository.completeWorkout(st.currentDay, logs)
            if (result.isSuccess) {
                AnalyticsLogger.logWorkoutCompleted(st.currentDay, checked.size)
                _state.value = _state.value.copy(uiState = WorkoutUiState.WorkoutComplete)
            } else {
                AnalyticsLogger.logWorkoutError("Failed to complete workout")
                _state.value = _state.value.copy(
                    uiState = WorkoutUiState.Error("Failed to complete workout")
                )
            }
        }
    }

    fun reset() {
        _state.value = WorkoutScreenState()
    }

    fun reorderExercises(fromIndex: Int, toIndex: Int) {
        val list = _state.value.exercises.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _state.value = _state.value.copy(exercises = list)
    }

    fun updateExerciseWeight(exerciseId: Int, weight: Float?) {
        viewModelScope.launch {
            repository.updateExerciseWeight(exerciseId, weight)
            // Update local state
            val updated = _state.value.exercises.map { ex ->
                if (ex.exercise_id == exerciseId) ex.copy(exercise_user_current_weight = weight)
                else ex
            }
            _state.value = _state.value.copy(exercises = updated)
        }
    }
}

class WorkoutViewModelFactory(
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            return WorkoutViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}