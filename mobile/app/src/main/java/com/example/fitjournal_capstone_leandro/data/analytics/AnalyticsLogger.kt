package com.example.fitjournal_capstone_leandro.analytics

import android.util.Log

/**
 * Simple analytics logger for tracking key user events and errors.
 * No backend required — events are logged locally via Android Log.
 * Can be upgraded to Firebase Analytics in the future.
 */
object AnalyticsLogger {

    private const val TAG = "FitJournalAnalytics"

    // ── Auth Events ──
    fun logLoginSuccess() {
        Log.d(TAG, "EVENT: login_success")
    }

    fun logLoginFailure(reason: String) {
        Log.d(TAG, "EVENT: login_failure | reason=$reason")
    }

    fun logRegisterSuccess() {
        Log.d(TAG, "EVENT: register_success")
    }

    // ── Workout Events ──
    fun logWorkoutCreated(dayNumber: Int, exerciseCount: Int) {
        Log.d(TAG, "EVENT: workout_created | day=$dayNumber | exercises=$exerciseCount")
    }

    fun logWorkoutCompleted(dayNumber: Int, exercisesChecked: Int) {
        Log.d(TAG, "EVENT: workout_completed | day=$dayNumber | checked=$exercisesChecked")
    }

    fun logWorkoutError(reason: String) {
        Log.e(TAG, "ERROR: workout_error | reason=$reason")
    }

    // ── Exercise Events ──
    fun logExerciseAdded(muscleGroup: String) {
        Log.d(TAG, "EVENT: exercise_added | muscle_group=$muscleGroup")
    }

    fun logExerciseDeleted(exerciseId: Int) {
        Log.d(TAG, "EVENT: exercise_deleted | exercise_id=$exerciseId")
    }

    fun logExerciseWeightUpdated(exerciseId: Int, newWeight: Float?) {
        Log.d(TAG, "EVENT: exercise_weight_updated | exercise_id=$exerciseId | weight=$newWeight")
    }

    // ── Routine Events ──
    fun logRoutineSaved(daysPerWeek: Int) {
        Log.d(TAG, "EVENT: routine_saved | days_per_week=$daysPerWeek")
    }

    // ── Navigation Events ──
    fun logScreenView(screenName: String) {
        Log.d(TAG, "EVENT: screen_view | screen=$screenName")
    }

    // ── Error Events ──
    fun logError(screen: String, reason: String) {
        Log.e(TAG, "ERROR: app_error | screen=$screen | reason=$reason")
    }
}