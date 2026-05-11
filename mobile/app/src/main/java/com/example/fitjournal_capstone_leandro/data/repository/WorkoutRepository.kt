package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.model.ExerciseLog
import com.example.fitjournal_capstone_leandro.data.model.NextWorkoutSelection
import com.example.fitjournal_capstone_leandro.data.model.UpdateExerciseRequest
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.model.WorkoutCompleteRequest
import com.example.fitjournal_capstone_leandro.data.model.WorkoutState
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient

class WorkoutRepository(private val tokenManager: TokenManager) {

    private val apiService = RetrofitClient.apiService

    suspend fun getWorkoutState(): Result<WorkoutState> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val state = apiService.getWorkoutState(userId)
            Result.success(state)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSelections(): Result<List<NextWorkoutSelection>> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val selections = apiService.getNextWorkoutSelections(userId)
            Result.success(selections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateWorkout(dayNumber: Int): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            apiService.generateNextWorkout(userId, dayNumber)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExercises(): Result<List<UserExercise>> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val exercises = apiService.getExercises(userId)
            Result.success(exercises)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeWorkout(
        dayNumber: Int,
        exercises: List<UserExercise>
    ): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            val logs = exercises.map { ex ->
                ExerciseLog(
                    exercise_id = ex.exercise_id,
                    sets_completed = 3,
                    reps_completed = 10,
                    weight_used = ex.exercise_user_current_weight ?: 0f
                )
            }
            apiService.completeWorkout(
                userId,
                WorkoutCompleteRequest(day_number = dayNumber, exercises = logs)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("WorkoutRepo", "completeWorkout error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun clearAllSelections(): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            apiService.clearAllSelections(userId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateExerciseWeight(exerciseId: Int, weight: Float?): Result<Unit> {
        return try {
            val userId = tokenManager.getUserId()
            if (userId == -1) return Result.failure(Exception("No user logged in"))
            apiService.updateExercise(
                exerciseId,
                userId,
                UpdateExerciseRequest(exercise_user_current_weight = weight)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}