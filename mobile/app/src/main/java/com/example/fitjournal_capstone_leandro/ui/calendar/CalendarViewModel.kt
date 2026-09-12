package com.example.fitjournal_capstone_leandro.ui.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.fitjournal_capstone_leandro.data.repository.CalendarBundle
import com.example.fitjournal_capstone_leandro.data.repository.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// UI state types
// ---------------------------------------------------------------------------

sealed class CalendarUiState {
    object Idle : CalendarUiState()
    object Loading : CalendarUiState()
    object Ready : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

/** A row in the calendar table — one exercise. */
data class CalendarExerciseRow(
    val exerciseId: Int,
    val exerciseName: String,
    val muscleGroup: String,
    val isSelected: Boolean,
    val selectable: Boolean,                    // per_muscle pool → togglable
    val inPool: Boolean,                        // in the day's pool (manual → locked)
    val logsBySessionId: Map<Int, LogCell>      // sparse: missing → "—" in UI
)

/** A column header for the log table — one session. */
data class SessionColumn(
    val sessionId: Int,
    val workoutDate: String
)

/** What goes in a single table cell when a log exists. */
data class LogCell(
    val weight: Float?,
    val sets: Int?,
    val reps: Int?
)

data class CalendarScreenState(
    val uiState: CalendarUiState = CalendarUiState.Idle,
    val daysPerWeek: Int = 0,
    val currentDayNumber: Int = 1,
    val selectedDayNumber: Int = 1,
    val isAllView: Boolean = false,             // true = View All (muscle-first, no select)
    val muscleGroupsForSelectedDay: List<String> = emptyList(),
    val exercisesForSelectedDay: List<CalendarExerciseRow> = emptyList(),
    val sessionColumns: List<SessionColumn> = emptyList()
)

/** Internal working row before conversion to CalendarExerciseRow. */
private data class RowInfo(
    val exerciseId: Int,
    val name: String,
    val muscle: String,
    val inPool: Boolean,
    val selectable: Boolean
)


// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

class CalendarViewModel(
    private val repository: CalendarRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarScreenState())
    val state: StateFlow<CalendarScreenState> = _state.asStateFlow()

    init {
        loadCalendar()
    }

    /** Initial load / refresh — preserves the current view (All or a specific day). */
    fun loadCalendar() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = CalendarUiState.Loading)

            val isAll = _state.value.isAllView
            val keepSelected = if (_state.value.daysPerWeek == 0) null
            else _state.value.selectedDayNumber

            val result = repository.loadCalendarBundle(keepSelected)
            if (result.isFailure) {
                Log.e(TAG, "loadCalendar failed", result.exceptionOrNull())
                _state.value = _state.value.copy(
                    uiState = CalendarUiState.Error("Couldn't load the calendar.")
                )
                return@launch
            }
            _state.value = composeStateFrom(result.getOrNull()!!, isAll)
        }
    }

    /** Tap a day tab → switch to that day (leaving All view if active). */
    fun selectDay(dayNumber: Int) {
        if (!_state.value.isAllView && dayNumber == _state.value.selectedDayNumber) return
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = CalendarUiState.Loading)
            val result = repository.loadCalendarBundle(dayNumber)
            if (result.isFailure) {
                Log.e(TAG, "selectDay failed", result.exceptionOrNull())
                _state.value = _state.value.copy(
                    uiState = CalendarUiState.Error("Couldn't switch days.")
                )
                return@launch
            }
            _state.value = composeStateFrom(result.getOrNull()!!, isAllView = false)
        }
    }

    /** Tap "All" → muscle-first, read-only view across all days. */
    fun selectAllView() {
        if (_state.value.isAllView) return
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = CalendarUiState.Loading)
            val result = repository.loadCalendarBundle(_state.value.selectedDayNumber)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    uiState = CalendarUiState.Error("Couldn't load the calendar.")
                )
                return@launch
            }
            _state.value = composeStateFrom(result.getOrNull()!!, isAllView = true)
        }
    }

    /** Tap-to-select toggle (day views only). Optimistic local update. */
    fun toggleSelection(exerciseId: Int) {
        val row = _state.value.exercisesForSelectedDay
            .firstOrNull { it.exerciseId == exerciseId } ?: return
        val newIsSelected = !row.isSelected
        viewModelScope.launch {
            val result = repository.toggleSelection(exerciseId, newIsSelected)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    uiState = CalendarUiState.Error("Couldn't update selection.")
                )
                return@launch
            }
            _state.value = _state.value.copy(
                exercisesForSelectedDay = _state.value.exercisesForSelectedDay.map {
                    if (it.exerciseId == exerciseId) it.copy(isSelected = newIsSelected) else it
                }
            )
        }
    }

    fun autoSelectForCurrentDay() {
        val day = _state.value.selectedDayNumber
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = CalendarUiState.Loading)
            val result = repository.generateForDay(day)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    uiState = CalendarUiState.Error("Couldn't auto-select.")
                )
                return@launch
            }
            loadCalendar()
        }
    }

    fun clearSelectionsForCurrentDay() {
        val day = _state.value.selectedDayNumber
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = CalendarUiState.Loading)
            val result = repository.clearForDay(day)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    uiState = CalendarUiState.Error("Couldn't clear selections.")
                )
                return@launch
            }
            loadCalendar()
        }
    }

    // -------------------------------------------------------------------
    // Row builders (ported from the web calendar's union/muscle logic)
    // -------------------------------------------------------------------

    private fun dayObj(bundle: CalendarBundle, dayNumber: Int) =
        bundle.routine.days.find { it.day_number == dayNumber }

    /** Muscles a day trains: its pool muscles (per_muscle) or the distinct
    muscles of its exercises (manual). */
    private fun dayMuscles(bundle: CalendarBundle, dayNumber: Int): List<String> {
        val day = dayObj(bundle, dayNumber) ?: return emptyList()
        return if (day.day_type == "manual") {
            day.exercises.map { it.muscle_group }.distinct()
        } else {
            day.muscles.map { it.muscle_group }
        }
    }

    /** A day's pool exercises, flagged: per_muscle → selectable; manual → locked. */
    private fun dayPoolRows(bundle: CalendarBundle, dayNumber: Int): List<RowInfo> {
        val day = dayObj(bundle, dayNumber) ?: return emptyList()
        val perMuscle = day.day_type != "manual"
        return day.exercises.map {
            RowInfo(it.exercise_id, it.exercise_name, it.muscle_group, inPool = true, selectable = perMuscle)
        }
    }

    /** Day-view rows: the pool PLUS any logged exercise whose muscle the day
    trains (so manual/off-routine logs surface under the right day). */
    private fun rowsForDay(bundle: CalendarBundle, dayNumber: Int): List<RowInfo> {
        val byId = LinkedHashMap<Int, RowInfo>()
        dayPoolRows(bundle, dayNumber).forEach { byId[it.exerciseId] = it }

        val dayMus = dayMuscles(bundle, dayNumber).toSet()
        val libMuscle = bundle.exercises.associate { it.exercise_id to it.exercise_muscle_group }
        val libName = bundle.exercises.associate { it.exercise_id to it.exercise_name }

        bundle.logs.map { it.exercise_id }.toSet().forEach { id ->
            if (byId.containsKey(id)) return@forEach
            val mus = libMuscle[id] ?: return@forEach
            if (mus !in dayMus) return@forEach
            byId[id] = RowInfo(id, libName[id] ?: "", mus, inPool = false, selectable = false)
        }
        return byId.values.toList()
    }

    /** View All rows: every routine exercise across all days + any logged
    off-routine exercise (under its library muscle), each once. */
    private fun allViewRows(bundle: CalendarBundle): List<RowInfo> {
        val byId = LinkedHashMap<Int, RowInfo>()
        bundle.routine.days.forEach { day ->
            day.exercises.forEach { e ->
                if (!byId.containsKey(e.exercise_id)) {
                    byId[e.exercise_id] = RowInfo(e.exercise_id, e.exercise_name, e.muscle_group, inPool = false, selectable = false)
                }
            }
        }
        val libMuscle = bundle.exercises.associate { it.exercise_id to it.exercise_muscle_group }
        val libName = bundle.exercises.associate { it.exercise_id to it.exercise_name }
        bundle.logs.map { it.exercise_id }.toSet().forEach { id ->
            if (byId.containsKey(id)) return@forEach
            val mus = libMuscle[id] ?: return@forEach
            byId[id] = RowInfo(id, libName[id] ?: "", mus, inPool = false, selectable = false)
        }
        return byId.values.toList()
    }

    // -------------------------------------------------------------------

    private fun composeStateFrom(bundle: CalendarBundle, isAllView: Boolean): CalendarScreenState {
        val rowInfos = if (isAllView) allViewRows(bundle) else rowsForDay(bundle, bundle.viewingDay)

        // Muscle alphabetical, then exercise name — matches the web calendar.
        val sorted = rowInfos.sortedWith(
            compareBy({ it.muscle.lowercase() }, { it.name.lowercase() })
        )

        val rowIds = sorted.map { it.exerciseId }.toSet()

        // Columns = sessions that logged any visible row (so manual/day-less
        // sessions surface under the days whose muscles they trained), newest first.
        val columns: List<SessionColumn> = bundle.sessions
            .filter { session ->
                bundle.logs.any { it.session_id == session.session_id && it.exercise_id in rowIds }
            }
            .sortedByDescending { it.workout_date }
            .map { SessionColumn(it.session_id, it.workout_date) }

        val logIndex: Map<Pair<Int, Int>, LogCell> = bundle.logs.associate { log ->
            (log.exercise_id to log.session_id) to LogCell(
                weight = log.weight_used,
                sets   = log.sets_completed,
                reps   = log.reps_completed
            )
        }
        val selectedIds = bundle.selections.map { it.exercise_id }.toSet()

        val rows: List<CalendarExerciseRow> = sorted.map { ri ->
            CalendarExerciseRow(
                exerciseId   = ri.exerciseId,
                exerciseName = ri.name,
                muscleGroup  = ri.muscle,
                isSelected   = ri.exerciseId in selectedIds,
                selectable   = ri.selectable,
                inPool       = ri.inPool,
                logsBySessionId = columns.mapNotNull { col ->
                    logIndex[ri.exerciseId to col.sessionId]?.let { col.sessionId to it }
                }.toMap()
            )
        }

        return CalendarScreenState(
            uiState                    = CalendarUiState.Ready,
            daysPerWeek                = bundle.routine.days_per_week,
            currentDayNumber           = bundle.workoutState.current_day_number,
            selectedDayNumber          = bundle.viewingDay,
            isAllView                  = isAllView,
            muscleGroupsForSelectedDay = sorted.map { it.muscle }.distinct(),
            exercisesForSelectedDay    = rows,
            sessionColumns             = columns
        )
    }

    companion object {
        private const val TAG = "FitJournalCalendar"
    }
}


// ---------------------------------------------------------------------------
// Factory
// ---------------------------------------------------------------------------

class CalendarViewModelFactory(
    private val repository: CalendarRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            return CalendarViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}