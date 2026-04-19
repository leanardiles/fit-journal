package com.example.fitjournal_capstone_leandro.ui.exercises

import com.example.fitjournal_capstone_leandro.data.model.Exercise

sealed class ExercisesScreenAction {

    // ========== Fetching Muscle Groups ==========

    // User wants to fetch muscle groups
    object FetchMuscleGroups : ExercisesScreenAction()

    // System successfully fetched muscle groups
    data class FetchMuscleGroupsSuccess(val muscleGroups: List<String>) : ExercisesScreenAction()

    // System failed to fetch muscle groups
    data class FetchMuscleGroupsFailure(val errorMessage: String) : ExercisesScreenAction()

    // ========== Fetching Exercises ==========

    // User wants to fetch exercises (initial load or refresh)
    data class FetchExercises(val muscleGroup: String? = null) : ExercisesScreenAction()

    // System successfully fetched exercises from API
    data class FetchExercisesSuccess(val exercises: List<Exercise>) : ExercisesScreenAction()

    // System failed to fetch exercises
    data class FetchExercisesFailure(val errorMessage: String) : ExercisesScreenAction()

    // ========== Filtering ==========

    // User selected a muscle group to filter exercises
    data class SelectMuscleGroup(val muscleGroup: String) : ExercisesScreenAction()

    // User cleared the muscle group filter (show all exercises)
    object ClearMuscleGroupFilter : ExercisesScreenAction()

    // ========== Exercise Selection ==========

    // User tapped on an exercise to view details
    data class SelectExercise(val exercise: Exercise) : ExercisesScreenAction()

    // User closed the exercise details view
    object ClearExerciseSelection : ExercisesScreenAction()

    // ========== Refresh ==========

    // User pulled to refresh the exercises list
    object RefreshExercises : ExercisesScreenAction()

    // Refresh completed
    object RefreshComplete : ExercisesScreenAction()
}