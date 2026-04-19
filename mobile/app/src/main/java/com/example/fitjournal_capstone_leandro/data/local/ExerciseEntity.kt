package com.example.fitjournal_capstone_leandro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val bodyPart: String,
    val equipment: String,
    val gifUrl: String?,
    val target: String,
    val instructions: String,
    val secondaryMuscles: String
)