package com.example.fitjournal_capstone_leandro.data.repository

import com.example.fitjournal_capstone_leandro.data.model.UserExercise

interface IUserExercisesRepository {
    suspend fun getExercises(): Result<List<UserExercise>>
    suspend fun getMuscleGroups(): Result<List<String>>
    suspend fun getExercisesByMuscle(muscleGroup: String): Result<List<UserExercise>>
    suspend fun createExercise(
        name: String,
        muscleGroup: String,
        weight: Float? = null,
        link: String? = null,
        comments: String? = null
    ): Result<UserExercise>
    suspend fun deleteExercise(exerciseId: Int): Result<Unit>
    suspend fun updateExerciseWeight(exerciseId: Int, weight: Float?): Result<UserExercise>
}