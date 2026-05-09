package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.RoutineDay
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
import com.example.fitjournal_capstone_leandro.data.model.RoutineSetupRequest
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient

class UserRoutineRepository(
    private val tokenManager: TokenManager
) : IUserRoutineRepository {
    private val apiService = RetrofitClient.apiService

    /**
     * Get user's current routine
     */
    override suspend fun getRoutine(): Result<RoutineResponse> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val routine = apiService.getRoutine(userId)
            Result.success(routine)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save user's routine
     */
    override suspend fun saveRoutine(daysPerWeek: Int, routineDays: Map<Int, List<String>>): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))

            val days = routineDays.map { (dayNumber, muscles) ->
                RoutineDay(day_number = dayNumber, muscle_groups = muscles)
            }

            val request = RoutineSetupRequest(
                days_per_week = daysPerWeek,
                routine_days = days
            )

            apiService.saveRoutine(userId, request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete user's routine
     */
    override suspend fun deleteRoutine(): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            apiService.deleteRoutine(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}