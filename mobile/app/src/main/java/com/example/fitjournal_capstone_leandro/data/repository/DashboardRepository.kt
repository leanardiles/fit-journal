package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
import com.example.fitjournal_capstone_leandro.data.model.LogsBySessionsRequest
import com.example.fitjournal_capstone_leandro.data.model.WorkoutSession
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient


class DashboardRepository(private val tokenManager: TokenManager) {

    private val apiService = RetrofitClient.apiService

    suspend fun getRoutine(): Result<RoutineResponse> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val routine = apiService.getRoutine(userId)
            Result.success(routine)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkoutsThisWeek(): Result<Int> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val sessions = apiService.getWorkoutSessions(userId, 20)

            // Get start of current week (Monday) using Calendar
            val calendar = java.util.Calendar.getInstance()
            calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val startOfWeek = calendar.time

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val today = sdf.format(java.util.Date())
            val startOfWeekStr = sdf.format(startOfWeek)

            // Count sessions this week
            val count = sessions.count { session ->
                session.workout_date >= startOfWeekStr && session.workout_date <= today
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentDay(): Result<Int> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val state = apiService.getWorkoutState(userId)
            Result.success(state.current_day_number)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sets logged per muscle group, computed client-side (like web): recent
     * sessions -> their logs -> map each log's exercise to its muscle -> sum
     * sets_completed within two windows (this week Monday-based, and last 7 days).
     */
    suspend fun getSetsPerMuscle(): Result<SetsPerMuscle> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))

            // Full canonical muscle list — untrained muscles show as 0.
            val allMuscles = listOf(
                "Abs", "Back", "Biceps", "Calves", "Chest",
                "Forearms", "Glutes", "Legs", "Shoulders", "Triceps"
            )
            val allZero: Map<String, Int> = allMuscles.associateWith { 0 }

            val sessions = apiService.getWorkoutSessions(userId, 50)
            if (sessions.isEmpty()) return Result.success(SetsPerMuscle(allZero, allZero))

            val sessionIds = sessions.map { it.session_id }
            val logs = apiService.getWorkoutLogsBySessions(userId, LogsBySessionsRequest(sessionIds))
            val exercises = apiService.getExercises(userId)
            val muscleByExercise = exercises.associate { it.exercise_id to it.exercise_muscle_group }

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val todayStr = sdf.format(java.util.Date())

            val weekCal = java.util.Calendar.getInstance()
            weekCal.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
            weekCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            weekCal.set(java.util.Calendar.MINUTE, 0)
            weekCal.set(java.util.Calendar.SECOND, 0)
            weekCal.set(java.util.Calendar.MILLISECOND, 0)
            val weekStartStr = sdf.format(weekCal.time)

            val last7Cal = java.util.Calendar.getInstance()
            last7Cal.add(java.util.Calendar.DAY_OF_YEAR, -6)
            val last7StartStr = sdf.format(last7Cal.time)

            fun sumInWindow(startStr: String): Map<String, Int> {
                val result = LinkedHashMap<String, Int>()
                for (m in allMuscles) result[m] = 0
                for (log in logs) {
                    if (log.workout_date in startStr..todayStr) {
                        val muscle = muscleByExercise[log.exercise_id] ?: continue
                        result[muscle] = (result[muscle] ?: 0) + log.sets_completed
                    }
                }
                return result
            }

            Result.success(SetsPerMuscle(thisWeek = sumInWindow(weekStartStr), last7Days = sumInWindow(last7StartStr)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SetsPerMuscle(
    val thisWeek: Map<String, Int>,
    val last7Days: Map<String, Int>
)