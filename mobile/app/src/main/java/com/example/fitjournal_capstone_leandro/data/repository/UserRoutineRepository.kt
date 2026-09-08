package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.model.DaySave
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
import com.example.fitjournal_capstone_leandro.data.model.RoutineSetupRequest
import com.example.fitjournal_capstone_leandro.data.model.TrainingDayMuscleRequest
import com.example.fitjournal_capstone_leandro.data.model.TrainingDayRequest
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient

class UserRoutineRepository(
    private val tokenManager: TokenManager
) : IUserRoutineRepository {
    private val apiService = RetrofitClient.apiService

    override suspend fun getRoutine(): Result<RoutineResponse> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            Result.success(apiService.getRoutine(userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExercises(): Result<List<UserExercise>> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            Result.success(apiService.getExercises(userId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save the user's routine. Each day is either per_muscle (muscle pools +
     * counts) or manual (a flat exercise list); map each into the training-day
     * request shape accordingly.
     *
     * @param days day_number -> that day's save payload.
     */
    override suspend fun saveRoutine(days: Map<Int, DaySave>): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))

            val dayRequests = days.toSortedMap().map { (dayNumber, d) ->
                if (d.day_type == "manual") {
                    TrainingDayRequest(
                        day_number = dayNumber,
                        day_type = "manual",
                        name = d.name,
                        muscles = emptyList(),
                        exercise_ids = d.exercise_ids.distinct()
                    )
                } else {
                    TrainingDayRequest(
                        day_number = dayNumber,
                        day_type = "per_muscle",
                        name = d.name,
                        muscles = d.pools.map {
                            TrainingDayMuscleRequest(
                                muscle_group = it.muscle_group,
                                exercise_count = it.exercise_count
                            )
                        },
                        exercise_ids = d.pools.flatMap { it.exercise_ids }.distinct()
                    )
                }
            }

            apiService.saveRoutine(userId, RoutineSetupRequest(days = dayRequests))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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