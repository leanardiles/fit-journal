package com.example.fitjournal_capstone_leandro.navigation

/**
 * Centralized navigation routes for the FitJournal app.
 *
 * All composable destinations and navigate() calls should reference
 * these constants rather than string literals.
 */
object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val PROFILE_SETTINGS = "profile_settings"
    const val CALENDAR = "calendar"
    const val EXERCISES = "exercises"
    const val EXERCISE_DETAILS = "exerciseDetails"
    const val ROUTINE = "routine"
    const val EXERCISE_PICKER = "exercise_picker/{day}"
    const val WORKOUT = "workout"
    const val MANUAL_LOG = "manual_log"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val TIMER = "timer"
    const val ACCOUNT = "account"

    fun exercisePicker(day: Int) = "exercise_picker/$day"
}