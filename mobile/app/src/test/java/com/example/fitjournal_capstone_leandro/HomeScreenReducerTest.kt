package com.example.fitjournal_capstone_leandro

import com.example.fitjournal_capstone_leandro.ui.home.HomeScreenAction
import com.example.fitjournal_capstone_leandro.ui.home.HomeScreenState
import com.example.fitjournal_capstone_leandro.ui.home.HomeUiState
import com.example.fitjournal_capstone_leandro.ui.home.homeScreenReducer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeScreenReducerTest {

    private val initialState = HomeScreenState()

    @Test
    fun `FetchMuscleGroups action sets uiState to Loading`() {
        val result = homeScreenReducer(initialState, HomeScreenAction.FetchMuscleGroups)
        assertEquals(HomeUiState.Loading, result.uiState)
    }

    @Test
    fun `FetchMuscleGroups action clears errorMessage`() {
        val stateWithError = initialState.copy(errorMessage = "some error")
        val result = homeScreenReducer(stateWithError, HomeScreenAction.FetchMuscleGroups)
        assertNull(result.errorMessage)
    }

    @Test
    fun `FetchMuscleGroupsSuccess sets uiState to Success with muscle groups`() {
        val muscles = listOf("Biceps", "Legs")
        val result = homeScreenReducer(
            initialState,
            HomeScreenAction.FetchMuscleGroupsSuccess(muscles)
        )
        assertEquals(HomeUiState.Success(muscles), result.uiState)
        assertEquals(muscles, result.muscleGroups)
    }

    @Test
    fun `FetchMuscleGroupsSuccess with empty list sets uiState to Empty`() {
        val result = homeScreenReducer(
            initialState,
            HomeScreenAction.FetchMuscleGroupsSuccess(emptyList())
        )
        assertEquals(HomeUiState.Empty, result.uiState)
        assertTrue(result.muscleGroups.isEmpty())
    }

    @Test
    fun `FetchMuscleGroupsFailure sets uiState to Error`() {
        val result = homeScreenReducer(
            initialState,
            HomeScreenAction.FetchMuscleGroupsFailure("Network error")
        )
        assertTrue(result.uiState is HomeUiState.Error)
        assertEquals("Network error", result.errorMessage)
        assertTrue(result.muscleGroups.isEmpty())
    }

    @Test
    fun `SelectMuscleGroup updates selectedMuscleGroup`() {
        val result = homeScreenReducer(
            initialState,
            HomeScreenAction.SelectMuscleGroup("Biceps")
        )
        assertEquals("Biceps", result.selectedMuscleGroup)
    }

    @Test
    fun `ClearSelection sets selectedMuscleGroup to null`() {
        val stateWithSelection = initialState.copy(selectedMuscleGroup = "Biceps")
        val result = homeScreenReducer(stateWithSelection, HomeScreenAction.ClearSelection)
        assertNull(result.selectedMuscleGroup)
    }
}