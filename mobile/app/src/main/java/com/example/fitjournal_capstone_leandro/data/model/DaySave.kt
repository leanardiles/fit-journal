package com.example.fitjournal_capstone_leandro.data.model

// One day's save payload from the routine editor. A day is either per_muscle
// (muscle pools + counts) or manual (a flat exercise list).
data class DaySave(
    val day_type: String,                        // "per_muscle" | "manual"
    val name: String? = null,                    // optional day label, e.g. "Lower B"
    val pools: List<MusclePool> = emptyList(),   // per_muscle days
    val exercise_ids: List<Int> = emptyList()    // manual days
)