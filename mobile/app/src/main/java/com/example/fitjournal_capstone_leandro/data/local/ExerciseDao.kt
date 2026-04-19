package com.example.fitjournal_capstone_leandro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExerciseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("SELECT * FROM exercises WHERE target = :muscle")
    suspend fun getExercisesByMuscle(muscle: String): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun getExerciseById(exerciseId: String): ExerciseEntity?

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercises(): List<ExerciseEntity>

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()
}