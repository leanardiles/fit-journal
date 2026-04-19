package com.example.fitjournal_capstone_leandro.ui.home

sealed class HomeScreenAction {

    // User wants to fetch/refresh muscle groups
    object FetchMuscleGroups : HomeScreenAction()

    // System successfully fetched muscle groups from the data source
    data class FetchMuscleGroupsSuccess(val muscleGroups: List<String>) : HomeScreenAction()

    // System failed to fetch muscle groups
    data class FetchMuscleGroupsFailure(val errorMessage: String) : HomeScreenAction()

    // User selected a muscle group from the list
    data class SelectMuscleGroup(val muscleGroup: String) : HomeScreenAction()

    // User cleared/deselected the muscle group
    object ClearSelection : HomeScreenAction()
}