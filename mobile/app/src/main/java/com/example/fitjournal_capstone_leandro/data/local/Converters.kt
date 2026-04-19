package com.example.fitjournal_capstone_leandro.data.local

import com.example.fitjournal_capstone_leandro.data.model.Exercise
import com.google.gson.Gson

// Convert Exercise (from API) → ExerciseEntity (for database)
fun Exercise.toExerciseEntity(): ExerciseEntity {
    return ExerciseEntity(
        id = this.id,
        name = this.name,
        bodyPart = this.bodyPart,
        equipment = this.equipment,
        gifUrl = this.gifUrl,
        target = this.target,
        instructions = Gson().toJson(this.instructions),
        secondaryMuscles = Gson().toJson(this.secondaryMuscles)
    )
}

// Convert ExerciseEntity (from database) → Exercise (for UI)
fun ExerciseEntity.toExercise(): Exercise {
    return Exercise(
        id = this.id,
        name = this.name,
        bodyPart = this.bodyPart,
        equipment = this.equipment,
        gifUrl = this.gifUrl ?: "",
        target = this.target,
        instructions = Gson().fromJson(this.instructions, Array<String>::class.java).toList(),
        secondaryMuscles = Gson().fromJson(this.secondaryMuscles, Array<String>::class.java).toList()
    )
}

// Convert muscle String → MuscleEntity
fun String.toMuscleEntity(): MuscleEntity {
    return MuscleEntity(name = this)
}

// Convert MuscleEntity → String
fun MuscleEntity.toMuscle(): String {
    return this.name
}