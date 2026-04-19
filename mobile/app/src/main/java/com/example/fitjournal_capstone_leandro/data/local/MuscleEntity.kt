package com.example.fitjournal_capstone_leandro.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "muscles")
data class MuscleEntity(
    @PrimaryKey
    val name: String
)