package com.example.fitjournal_capstone_leandro.data.model

// Off-routine ("manual") workout logging.
// Mirrors the backend ManualLogRequest / ManualLogExercise shapes.
// workout_date is ISO (YYYY-MM-DD); the UI displays it as DD-MM-YYYY.

data class ManualLogEntry(
    val exercise_id: Int,
    val sets_completed: Int,
    val reps_completed: Int = 0,
    val weight_used: Float? = null
)

data class ManualLogRequest(
    val workout_date: String,            // ISO: YYYY-MM-DD
    val exercises: List<ManualLogEntry>
)