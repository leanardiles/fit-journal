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

/** High-level state of the screen. Matches the pattern used by WorkoutUiState. */
sealed class CalendarUiState {
    object Idle : CalendarUiState()
    object Loading : CalendarUiState()
    object Ready : CalendarUiState()
    data class Error(val message: String) : CalendarUiState()
}

/** A row in the calendar table — one exercise in the selected day. */
data class CalendarExerciseRow(
    val exerciseId: Int,
    val exerciseName: String,
    val muscleGroup: String,
    val isSelected: Boolean,
    val logsBySessionId: Map<Int, LogCell>     // sparse: missing → "—" in UI
)

/** A column header for the log table — one session for the selected day. */
data class SessionColumn(
    val sessionId: Int,
    val workoutDate: String                     // raw ISO date; UI formats it
)

/** What goes in a single table cell when a log exists. */
data class LogCell(
    val weight: Float?,
    val sets: Int?,
    val reps: Int?
)

/**
 * Composite state for the Calendar screen — the single thing the UI observes.
 *
 * Note on `exercisesForSelectedDay` ordering: rows are sorted by muscle group
 * in the order defined by `muscleGroupsForSelectedDay` (which mirrors the
 * routine setup), then alphabetically within each muscle group. The UI
 * relies on this contiguous-by-muscle ordering to insert muscle-group
 * headers between groups in the table.
 */
data class CalendarScreenState(
    val uiState: CalendarUiState = CalendarUiState.Idle,
    val daysPerWeek: Int = 0,                                 // 0 = no routine
    val currentDayNumber: Int = 1,                            // from workout_state
    val selectedDayNumber: Int = 1,                           // what user is viewing
    val muscleGroupsForSelectedDay: List<String> = emptyList(),
    val exercisesForSelectedDay: List<CalendarExerciseRow> = emptyList(),
    val sessionColumns: List<SessionColumn> = emptyList()
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

    /**
     * Initial load / pull-to-refresh. Preserves the user's selected day
     * across reloads; on first load (daysPerWeek == 0) defers to the repo,
     * which picks the workout_state's current day.
     */
    fun loadCalendar() {
        viewModelScope.launch {
            _state.value = _state.value.copy(uiState = CalendarUiState.Loading)

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

            val bundle = result.getOrNull()!!
            _state.value = composeStateFrom(bundle)
            Log.d(TAG, "Loaded: ${_state.value}")
        }
    }

    /**
     * User tapped a day tab. Re-fetches that day's content.
     */
    fun selectDay(dayNumber: Int) {
        if (dayNumber == _state.value.selectedDayNumber) return

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

            val bundle = result.getOrNull()!!
            _state.value = composeStateFrom(bundle)
        }
    }

    /**
     * Tap-to-select toggle. Calls the backend; on success applies an
     * optimistic local update (no full reload) so the UI feels responsive.
     */
    fun toggleSelection(exerciseId: Int) {
        val row = _state.value.exercisesForSelectedDay
            .firstOrNull { it.exerciseId == exerciseId } ?: return
        val newIsSelected = !row.isSelected

        viewModelScope.launch {
            val result = repository.toggleSelection(exerciseId, newIsSelected)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    uiState = CalendarUiState.Error("Selection toggling isn't wired up yet.")
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

    /**
     * Auto-generate selections for the currently-viewed day, then reload.
     */
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

    /**
     * Clear selections for the currently-viewed day, then reload.
     */
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
    // Helpers
    // -------------------------------------------------------------------

    /**
     * Builds the UI-shaped state from the raw CalendarBundle returned by
     * the repository. All the "combine" logic lives here so the repo just
     * deals with the network.
     *
     * Sorting: rows are grouped by muscle (in the order the muscles appear
     * in the routine's day definition) and alphabetized within each group.
     * The UI relies on this for inserting muscle-group headers in the table.
     */
    private fun composeStateFrom(bundle: CalendarBundle): CalendarScreenState {
        val muscleGroupsForDay: List<String> =
            bundle.routine.routine_days[bundle.viewingDay.toString()].orEmpty()

        // Map muscle name → its order index from the routine. Anything not
        // in the routine (defensive: shouldn't happen since we filter exercises
        // to the day's muscles) is parked at the end.
        val muscleOrder: Map<String, Int> =
            muscleGroupsForDay.withIndex().associate { (i, name) -> name to i }
        val muscleOrderComparator = compareBy<CalendarExerciseRow>(
            { muscleOrder[it.muscleGroup] ?: Int.MAX_VALUE },
            { it.exerciseName.lowercase() }
        )

        // Index logs by (exercise_id, session_id) for O(1) cell lookup
        val logIndex: Map<Pair<Int, Int>, LogCell> = bundle.logs.associate { log ->
            (log.exercise_id to log.session_id) to LogCell(
                weight = log.weight_used,
                sets   = log.sets_completed,
                reps   = log.reps_completed
            )
        }

        val selectedExerciseIds = bundle.selections.map { it.exercise_id }.toSet()

        val rows: List<CalendarExerciseRow> = bundle.exercises
            .filter { it.exercise_muscle_group in muscleGroupsForDay }
            .map { ex ->
                CalendarExerciseRow(
                    exerciseId      = ex.exercise_id,
                    exerciseName    = ex.exercise_name,
                    muscleGroup     = ex.exercise_muscle_group,
                    isSelected      = ex.exercise_id in selectedExerciseIds,
                    logsBySessionId = bundle.sessionsForDay
                        .mapNotNull { session ->
                            logIndex[ex.exercise_id to session.session_id]
                                ?.let { session.session_id to it }
                        }
                        .toMap()
                )
            }
            .sortedWith(muscleOrderComparator)

        val columns: List<SessionColumn> = bundle.sessionsForDay.map {
            SessionColumn(sessionId = it.session_id, workoutDate = it.workout_date)
        }

        return CalendarScreenState(
            uiState                    = CalendarUiState.Ready,
            daysPerWeek                = bundle.routine.days_per_week,
            currentDayNumber           = bundle.workoutState.current_day_number,
            selectedDayNumber          = bundle.viewingDay,
            muscleGroupsForSelectedDay = muscleGroupsForDay,
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