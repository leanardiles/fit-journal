package com.example.fitjournal_capstone_leandro.data.repository

import android.util.Log
import com.example.fitjournal_capstone_leandro.data.local.ExerciseDao
import com.example.fitjournal_capstone_leandro.data.local.MuscleDao
import com.example.fitjournal_capstone_leandro.data.local.toExercise
import com.example.fitjournal_capstone_leandro.data.local.toExerciseEntity
import com.example.fitjournal_capstone_leandro.data.local.toMuscle
import com.example.fitjournal_capstone_leandro.data.local.toMuscleEntity
import com.example.fitjournal_capstone_leandro.data.model.Exercise
import com.example.fitjournal_capstone_leandro.data.network.ApiService

class ExerciseRepository(
    private val apiService: ApiService,
    private val muscleDao: MuscleDao,
    private val exerciseDao: ExerciseDao
) {

    suspend fun getMuscleList(): List<String> {
        val cachedMuscles = muscleDao.getAllMuscles()

        if (cachedMuscles.isNotEmpty()) {
            return cachedMuscles.map { it.toMuscle() }
        }

        val muscles = apiService.getMuscleList()
        val muscleEntities = muscles.map { it.toMuscleEntity() }
        muscleDao.insertMuscles(muscleEntities)

        return muscles
    }

    suspend fun getExercisesByMuscle(muscle: String): List<Exercise> {
        val cachedExercises = exerciseDao.getExercisesByMuscle(muscle)

        if (cachedExercises.isNotEmpty()) {
            Log.d("Repository", "Returning ${cachedExercises.size} cached exercises")
            Log.d("Repository", "First cached exercise gifUrl: ${cachedExercises.first().gifUrl}")
            return cachedExercises.map { it.toExercise() }
        }

        val exercises = apiService.getExercisesByMuscle(muscle)
        Log.d("Repository", "Fetched ${exercises.size} exercises from API")
        Log.d("Repository", "First API exercise gifUrl: ${exercises.first().gifUrl}")

        val exerciseEntities = exercises.map { it.toExerciseEntity() }
        Log.d("Repository", "First entity gifUrl before insert: ${exerciseEntities.first().gifUrl}")

        exerciseDao.insertExercises(exerciseEntities)

        return exercises
    }
}