package com.example.fitjournal_capstone_leandro.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MuscleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuscles(muscles: List<MuscleEntity>)

    @Query("SELECT * FROM muscles")
    suspend fun getAllMuscles(): List<MuscleEntity>

    @Query("DELETE FROM muscles")
    suspend fun deleteAllMuscles()
}