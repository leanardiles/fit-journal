package com.example.fitjournal_capstone_leandro.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.analytics.AnalyticsLogger
import com.example.fitjournal_capstone_leandro.data.model.DaySave
import com.example.fitjournal_capstone_leandro.data.model.MusclePool
import com.example.fitjournal_capstone_leandro.data.model.TrainingDayResponse
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.repository.IUserRoutineRepository
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

/**
 * A muscle's selection within a per_muscle day: which exercises are in the
 * pool, and how many to draw per session.
 */
data class MuscleSelection(
    val exerciseIds: Set<Int> = emptySet(),
    val count: Int = 3
)

/**
 * A single day being edited. type is "per_muscle" or "manual".
 * - per_muscle uses `perMuscle` (muscle -> pool + count).
 * - manual uses `manualExerciseIds` (the flat list) and `manualActiveMuscles`
 *   (which muscle sections are currently revealed in the manual picker; a UI
 *   concern, independent of which exercises are checked).
 */
data class DayEdit(
    val type: String = "per_muscle",
    val name: String = "",
    val perMuscle: Map<String, MuscleSelection> = emptyMap(),
    val manualExerciseIds: Set<Int> = emptySet(),
    val manualActiveMuscles: Set<String> = emptySet()
)

data class RoutineScreenState(
    val uiState: RoutineUiState = RoutineUiState.Loading,
    val daysPerWeek: Int = 0,
    val selectedDays: Int = 0,                                   // days selected in editor
    val routineDays: Map<String, List<String>> = emptyMap(),    // existing routine (view mode display)
    val routineDayNames: Map<String, String?> = emptyMap(),     // day_number -> optional name (view mode)
    val routineDayTypes: Map<String, String> = emptyMap(),      // day_number -> "per_muscle" | "manual" (view mode)
    val editingDays: Map<Int, DayEdit> = emptyMap(),            // day -> edit state
    val exercisesByMuscle: Map<String, List<UserExercise>> = emptyMap(),   // library, for the picker
    val savedMessage: String? = null
)

class RoutineViewModel(
    private val repository: IUserRoutineRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RoutineScreenState())
    val state: StateFlow<RoutineScreenState> = _state.asStateFlow()

    // Raw routine days from the last load — used to prefill the editor.
    private var rawDays: List<TrainingDayResponse> = emptyList()

    // A day's edit snapshot taken when its picker opens, so Cancel / back can revert.
    private var pickerSnapshot: Pair<Int, DayEdit>? = null

    val muscleGroups = listOf(
        "Legs", "Shoulders", "Chest", "Glutes",
        "Biceps", "Triceps", "Back", "Calves", "Abs"
    )

    init {
        loadRoutine()
    }

    fun loadRoutine() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = RoutineUiState.Loading)

            val exByMuscle = repository.getExercises().getOrNull().orEmpty()
                .groupBy { it.exercise_muscle_group }

            val result = repository.getRoutine()
            if (result.isSuccess) {
                val routine = result.getOrNull()!!
                rawDays = routine.days
                if (routine.days_per_week == 0 || routine.days.isEmpty()) {
                    _state.value = _state.value.copy(
                        uiState = RoutineUiState.NoRoutine,
                        exercisesByMuscle = exByMuscle
                    )
                } else {
                    val displayMap = routine.days
                        .sortedBy { it.day_number }
                        .associate { it.day_number.toString() to musclesForDisplay(it) }
                    val nameMap = routine.days
                        .associate { it.day_number.toString() to it.name }
                    val typeMap = routine.days
                        .associate { it.day_number.toString() to it.day_type }
                    _state.value = _state.value.copy(
                        uiState = RoutineUiState.Success,
                        daysPerWeek = routine.days_per_week,
                        routineDays = displayMap,
                        routineDayNames = nameMap,
                        routineDayTypes = typeMap,
                        exercisesByMuscle = exByMuscle
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

    private fun musclesForDisplay(day: TrainingDayResponse): List<String> =
        if (day.day_type == "manual") {
            day.exercises.map { it.muscle_group }.distinct()
        } else {
            day.muscles.map { it.muscle_group }
        }

    /**
     * User selected number of training days — move to editing mode.
     * Existing days' content is preserved when the count changes.
     */
    fun selectDaysPerWeek(days: Int) {
        val existing = _state.value.editingDays
        val editing = (1..days).associateWith { existing[it] ?: DayEdit() }
        _state.value = _state.value.copy(
            uiState = RoutineUiState.Editing,
            selectedDays = days,
            editingDays = editing
        )
    }

    /** Switch a day between per_muscle and manual, clearing its content. */
    fun setDayType(day: Int, type: String) {
        val current = _state.value.editingDays.toMutableMap()
        val existing = current[day] ?: DayEdit()
        if (existing.type == type) return
        current[day] = DayEdit(type = type, name = existing.name)   // keep the name; clear content
        _state.value = _state.value.copy(editingDays = current)
    }

    /** Set a day's optional label (capped short). */
    fun setDayName(day: Int, name: String) {
        val current = _state.value.editingDays.toMutableMap()
        val de = current[day] ?: DayEdit()
        current[day] = de.copy(name = name.take(25))
        _state.value = _state.value.copy(editingDays = current)
    }

    // ---- per_muscle day: muscle pills + pool picker ----

    fun toggleMuscleGroup(day: Int, muscle: String) {
        val current = _state.value.editingDays.toMutableMap()
        val de = current[day] ?: DayEdit()
        val pm = de.perMuscle.toMutableMap()
        if (pm.containsKey(muscle)) {
            pm.remove(muscle)
        } else {
            val allIds = _state.value.exercisesByMuscle[muscle].orEmpty()
                .map { it.exercise_id }.toSet()
            val count = if (allIds.isEmpty()) 1 else minOf(3, allIds.size)
            pm[muscle] = MuscleSelection(exerciseIds = allIds, count = count)
        }
        current[day] = de.copy(perMuscle = pm)
        _state.value = _state.value.copy(editingDays = current)
    }

    fun toggleExercise(day: Int, muscle: String, exerciseId: Int) {
        val current = _state.value.editingDays.toMutableMap()
        val de = current[day] ?: return
        val pm = de.perMuscle.toMutableMap()
        val sel = pm[muscle] ?: return
        val ids = sel.exerciseIds.toMutableSet()
        if (ids.contains(exerciseId)) ids.remove(exerciseId) else ids.add(exerciseId)
        val newCount = if (ids.isEmpty()) 1 else minOf(sel.count, ids.size).coerceAtLeast(1)
        pm[muscle] = sel.copy(exerciseIds = ids, count = newCount)
        current[day] = de.copy(perMuscle = pm)
        _state.value = _state.value.copy(editingDays = current)
    }

    fun changeCount(day: Int, muscle: String, delta: Int) {
        val current = _state.value.editingDays.toMutableMap()
        val de = current[day] ?: return
        val pm = de.perMuscle.toMutableMap()
        val sel = pm[muscle] ?: return
        val maxCount = maxOf(1, sel.exerciseIds.size)
        val newCount = (sel.count + delta).coerceIn(1, maxCount)
        pm[muscle] = sel.copy(count = newCount)
        current[day] = de.copy(perMuscle = pm)
        _state.value = _state.value.copy(editingDays = current)
    }

    // ---- manual day: browse muscles + flat exercise list ----

    /** Show/hide a muscle's section in the manual picker (does not select anything). */
    fun toggleManualMuscleTab(day: Int, muscle: String) {
        val current = _state.value.editingDays.toMutableMap()
        val de = current[day] ?: return
        val active = de.manualActiveMuscles.toMutableSet()
        if (active.contains(muscle)) active.remove(muscle) else active.add(muscle)
        current[day] = de.copy(manualActiveMuscles = active)
        _state.value = _state.value.copy(editingDays = current)
    }

    /** Check/uncheck an exercise in the manual day's list. */
    fun toggleManualExercise(day: Int, exerciseId: Int) {
        val current = _state.value.editingDays.toMutableMap()
        val de = current[day] ?: return
        val ids = de.manualExerciseIds.toMutableSet()
        if (ids.contains(exerciseId)) ids.remove(exerciseId) else ids.add(exerciseId)
        current[day] = de.copy(manualExerciseIds = ids)
        _state.value = _state.value.copy(editingDays = current)
    }

    // ---- picker snapshot / revert (shared by both picker types) ----

    fun beginPickerEdit(day: Int) {
        pickerSnapshot = day to (_state.value.editingDays[day] ?: DayEdit())
    }

    fun revertPickerEdit(day: Int) {
        val snap = pickerSnapshot ?: return
        if (snap.first != day) return
        val current = _state.value.editingDays.toMutableMap()
        current[day] = snap.second
        _state.value = _state.value.copy(editingDays = current)
        pickerSnapshot = null
    }

    fun saveRoutine() {
        viewModelScope.launch {
            val editingDays = _state.value.editingDays

            // Every day must have content, validated per type.
            for ((day, de) in editingDays.toSortedMap()) {
                if (de.type == "manual") {
                    if (de.manualExerciseIds.isEmpty()) {
                        _state.value = _state.value.copy(
                            uiState = RoutineUiState.Error("Day $day: pick at least one exercise")
                        )
                        return@launch
                    }
                } else {
                    if (de.perMuscle.isEmpty()) {
                        _state.value = _state.value.copy(
                            uiState = RoutineUiState.Error("Day $day needs at least one muscle group")
                        )
                        return@launch
                    }
                    for ((muscle, sel) in de.perMuscle) {
                        if (sel.exerciseIds.isEmpty()) {
                            _state.value = _state.value.copy(
                                uiState = RoutineUiState.Error("Day $day: pick at least one $muscle exercise")
                            )
                            return@launch
                        }
                    }
                }
            }

            val days: Map<Int, DaySave> = editingDays.mapValues { (_, de) ->
                val dayName = de.name.trim().ifBlank { null }
                if (de.type == "manual") {
                    DaySave(day_type = "manual", name = dayName, exercise_ids = de.manualExerciseIds.toList())
                } else {
                    DaySave(
                        day_type = "per_muscle",
                        name = dayName,
                        pools = de.perMuscle.map { (muscle, sel) ->
                            MusclePool(
                                muscle_group = muscle,
                                exercise_ids = sel.exerciseIds.toList(),
                                exercise_count = sel.count
                            )
                        }
                    )
                }
            }

            val result = repository.saveRoutine(days)
            if (result.isSuccess) {
                AnalyticsLogger.logRoutineSaved(_state.value.selectedDays)
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
     * Switch to editing an existing routine — prefill each day by its type.
     */
    fun startEditing() {
        val editing = rawDays.sortedBy { it.day_number }.associate { day ->
            val de = if (day.day_type == "manual") {
                val ids = day.exercises.map { it.exercise_id }.toSet()
                val active = day.exercises.map { it.muscle_group }.distinct().toSet()
                DayEdit(type = "manual", name = day.name ?: "", manualExerciseIds = ids, manualActiveMuscles = active)
            } else {
                val pm = day.muscles.associate { m ->
                    val ids = day.exercises
                        .filter { it.muscle_group == m.muscle_group }
                        .map { it.exercise_id }
                        .toSet()
                    m.muscle_group to MuscleSelection(exerciseIds = ids, count = m.exercise_count)
                }
                DayEdit(type = "per_muscle", name = day.name ?: "", perMuscle = pm)
            }
            day.day_number to de
        }
        _state.value = _state.value.copy(
            uiState = RoutineUiState.Editing,
            selectedDays = _state.value.daysPerWeek,
            editingDays = editing
        )
    }

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