package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.LogsBySessionsRequest
import com.example.fitjournal_capstone_leandro.data.model.NextWorkoutSelection
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
import com.example.fitjournal_capstone_leandro.data.model.ToggleSelectionRequest
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.model.WorkoutLog
import com.example.fitjournal_capstone_leandro.data.model.WorkoutSession
import com.example.fitjournal_capstone_leandro.data.model.WorkoutState
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Fetches all data needed by the Calendar screen and exposes mutating actions.
 *
 * Returns Result<T> per the codebase convention (matches WorkoutRepository).
 *
 * The headline method `loadCalendarBundle` fans out the 5 read endpoints in
 * parallel and then fetches per-session logs once it knows which sessions
 * are relevant. The ViewModel takes the resulting CalendarBundle and combines
 * it into a CalendarScreenState for the UI.
 */
class CalendarRepository(private val tokenManager: TokenManager) {

    private val apiService = RetrofitClient.apiService

    /**
     * One trip to the network: pulls routine, workout state, exercises,
     * selections, sessions; then pulls logs for the sessions of the
     * given day (or the current day if none specified).
     */
    suspend fun loadCalendarBundle(selectedDayNumber: Int? = null): Result<CalendarBundle> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))

            // Fan out the 5 independent reads in parallel
            val bundle = coroutineScope {
                val routineD     = async { apiService.getRoutine(userId) }
                val stateD       = async { apiService.getWorkoutState(userId) }
                val exercisesD   = async { apiService.getExercises(userId) }
                val selectionsD  = async { apiService.getNextWorkoutSelections(userId) }
                val sessionsD    = async {
                    apiService.getWorkoutSessions(userId, limit = SESSION_FETCH_LIMIT)
                }

                val routine     = routineD.await()
                val workoutState = stateD.await()
                val exercises   = exercisesD.await()
                val selections  = selectionsD.await()
                val sessions    = sessionsD.await()

                // Resolve which day's logs to fetch
                val viewingDay = selectedDayNumber ?: workoutState.current_day_number

                // Filter sessions to viewing day, newest first, take last MAX_LOG_COLUMNS.
                // workout_date is an ISO String, so lexical sort matches chronological.
                val sessionsForDay = sessions
                    .filter { it.routine_day_number == viewingDay }
                    .sortedByDescending { it.workout_date }
                    .take(MAX_LOG_COLUMNS)

                // Fetch logs for just those sessions
                val logs: List<WorkoutLog> = if (sessionsForDay.isNotEmpty()) {
                    apiService.getWorkoutLogsBySessions(
                        userId,
                        LogsBySessionsRequest(session_ids = sessionsForDay.map { it.session_id })
                    )
                } else {
                    emptyList()
                }

                CalendarBundle(
                    routine = routine,
                    workoutState = workoutState,
                    exercises = exercises,
                    selections = selections,
                    sessionsForDay = sessionsForDay,
                    logs = logs,
                    viewingDay = viewingDay
                )
            }

            Result.success(bundle)
        } catch (e: Exception) {
            android.util.Log.e("CalendarRepo", "loadCalendarBundle error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Auto-generate selections for the given day.
     */
    suspend fun generateForDay(dayNumber: Int): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            apiService.generateNextWorkout(userId, dayNumber)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clear selections for the given day only (uses the day_number query
     * filter added to clearAllSelections).
     */
    suspend fun clearForDay(dayNumber: Int): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            apiService.clearAllSelections(userId, dayNumber)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle a single exercise's selection for the next workout.
     *
     * POST /v1/next-workout/toggle
     *
     * Used by the Calendar's tap-to-select interaction.
     */
    suspend fun toggleSelection(exerciseId: Int, isSelected: Boolean): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            apiService.toggleNextWorkoutSelection(
                ToggleSelectionRequest(
                    user_id = userId,
                    exercise_id = exerciseId,
                    is_selected = isSelected
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("CalendarRepo", "toggleSelection error: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        /**
         * Sessions to pull when populating the Calendar.
         *
         * Worst case (a 7-day routine trained daily) still yields ~10
         * sessions per day within 70 fetched sessions. Most users have
         * 3–5 day routines, so this gives a comfortable margin.
         */
        private const val SESSION_FETCH_LIMIT = 70

        /** Max log columns to display for a given day. */
        private const val MAX_LOG_COLUMNS = 10
    }
}

/**
 * Raw bundle of data returned by the network. The ViewModel turns this
 * into the UI-shaped CalendarScreenState.
 */
data class CalendarBundle(
    val routine: RoutineResponse,
    val workoutState: WorkoutState,
    val exercises: List<UserExercise>,
    val selections: List<NextWorkoutSelection>,
    val sessionsForDay: List<WorkoutSession>,
    val logs: List<WorkoutLog>,
    val viewingDay: Int
)