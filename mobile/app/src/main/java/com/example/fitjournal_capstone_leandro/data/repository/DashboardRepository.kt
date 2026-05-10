package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
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
}