package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.CreateExerciseRequest
import com.example.fitjournal_capstone_leandro.data.model.UpdateExerciseRequest
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient

/**
 * Repository for user exercises from the FitJournal backend
 */
class UserExercisesRepository(
    private val tokenManager: TokenManager
) : IUserExercisesRepository {
    private val apiService = RetrofitClient.apiService

    /**
     * Get all exercises for the logged-in user
     */
    override suspend fun getExercises(): Result<List<UserExercise>> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val exercises = apiService.getExercises(userId)
            Result.success(exercises)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get distinct muscle groups from user's exercises
     */
    override suspend fun getMuscleGroups(): Result<List<String>> {
        return try {
            val result = getExercises()
            if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
            val muscleGroups = result.getOrNull()!!
                .map { it.exercise_muscle_group }
                .distinct()
                .sorted()
            Result.success(muscleGroups)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get exercises filtered by muscle group
     */
    override suspend fun getExercisesByMuscle(muscleGroup: String): Result<List<UserExercise>> {
        return try {
            val result = getExercises()
            if (result.isFailure) return Result.failure(result.exceptionOrNull()!!)
            val filtered = result.getOrNull()!!
                .filter { it.exercise_muscle_group == muscleGroup }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create a new exercise
     */
    override suspend fun createExercise(
        name: String,
        muscleGroup: String,
        weight: Float?,
        link: String?,
        comments: String?
    ): Result<UserExercise> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val request = CreateExerciseRequest(
                exercise_name = name,
                exercise_muscle_group = muscleGroup,
                exercise_user_current_weight = weight,
                exercise_link = link,
                comments = comments
            )
            val exercise = apiService.createExercise(userId, request)
            Result.success(exercise)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an exercise
     */
    override suspend fun deleteExercise(exerciseId: Int): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val response = apiService.deleteExercise(exerciseId, userId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Delete failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update the weight of an exercise
     */
    override suspend fun updateExerciseWeight(exerciseId: Int, weight: Float?): Result<UserExercise> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val result = apiService.updateExercise(
                exerciseId,
                userId,
                UpdateExerciseRequest(exercise_user_current_weight = weight)
            )
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}