package com.example.fitjournal_capstone_leandro

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
import com.example.fitjournal_capstone_leandro.data.repository.IUserRoutineRepository
import com.example.fitjournal_capstone_leandro.ui.routine.RoutineUiState
import com.example.fitjournal_capstone_leandro.ui.routine.RoutineViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// ========================================
// FAKE REPOSITORY
// ========================================

class FakeUserRoutineRepository : IUserRoutineRepository {
    var routineResponse = RoutineResponse(
        days_per_week = 3,
        routine_days = mapOf(
            "1" to listOf("Legs", "Abs"),
            "2" to listOf("Chest", "Triceps"),
            "3" to listOf("Back", "Biceps")
        )
    )
    var shouldFail = false
    var saveCalled = false

    override suspend fun getRoutine(): Result<RoutineResponse> {
        return if (shouldFail) Result.failure(Exception("Network error"))
        else Result.success(routineResponse)
    }

    override suspend fun saveRoutine(
        daysPerWeek: Int,
        routineDays: Map<Int, List<String>>
    ): Result<Unit> {
        saveCalled = true
        return if (shouldFail) Result.failure(Exception("Network error"))
        else Result.success(Unit)
    }

    override suspend fun deleteRoutine(): Result<Unit> {
        return if (shouldFail) Result.failure(Exception("Network error"))
        else Result.success(Unit)
    }
}

// ========================================
// TEST CLASS
// ========================================

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: RoutineViewModel
    private lateinit var fakeRepo: FakeUserRoutineRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeUserRoutineRepository()
        viewModel = RoutineViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadRoutine sets uiState to Success when routine exists`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.state.value.uiState is RoutineUiState.Success)
    }

    @Test
    fun `loadRoutine sets daysPerWeek correctly`() = runTest {
        advanceUntilIdle()
        assertEquals(3, viewModel.state.value.daysPerWeek)
    }

    @Test
    fun `loadRoutine sets uiState to NoRoutine when days_per_week is 0`() = runTest {
        fakeRepo.routineResponse = RoutineResponse(
            days_per_week = 0,
            routine_days = emptyMap()
        )
        viewModel.loadRoutine()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.uiState is RoutineUiState.NoRoutine)
    }

    @Test
    fun `loadRoutine sets uiState to Error on failure`() = runTest {
        fakeRepo.shouldFail = true
        viewModel.loadRoutine()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.uiState is RoutineUiState.Error)
    }

    @Test
    fun `selectDaysPerWeek sets editing state with correct days`() = runTest {
        advanceUntilIdle()
        viewModel.selectDaysPerWeek(4)
        assertEquals(RoutineUiState.Editing, viewModel.state.value.uiState)
        assertEquals(4, viewModel.state.value.selectedDays)
    }

    @Test
    fun `selectDaysPerWeek creates empty editing days map`() = runTest {
        advanceUntilIdle()
        viewModel.selectDaysPerWeek(3)
        val editingDays = viewModel.state.value.editingDays
        assertEquals(3, editingDays.size)
        assertTrue(editingDays.values.all { it.isEmpty() })
    }

    @Test
    fun `toggleMuscleGroup adds muscle to day`() = runTest {
        advanceUntilIdle()
        viewModel.selectDaysPerWeek(3)
        viewModel.toggleMuscleGroup(1, "Legs")
        assertTrue(viewModel.state.value.editingDays[1]!!.contains("Legs"))
    }

    @Test
    fun `toggleMuscleGroup removes muscle if already selected`() = runTest {
        advanceUntilIdle()
        viewModel.selectDaysPerWeek(3)
        viewModel.toggleMuscleGroup(1, "Legs")
        viewModel.toggleMuscleGroup(1, "Legs")
        assertTrue(viewModel.state.value.editingDays[1]!!.isEmpty())
    }

    @Test
    fun `saveRoutine calls repository when selection is valid`() = runTest {
        advanceUntilIdle()
        viewModel.selectDaysPerWeek(3)
        viewModel.toggleMuscleGroup(1, "Legs")
        viewModel.saveRoutine()
        advanceUntilIdle()
        assertTrue(fakeRepo.saveCalled)
    }

    @Test
    fun `saveRoutine sets Error when no muscle groups selected`() = runTest {
        advanceUntilIdle()
        viewModel.selectDaysPerWeek(3)
        viewModel.saveRoutine()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.uiState is RoutineUiState.Error)
    }

    @Test
    fun `startEditing pre-populates editing days from existing routine`() = runTest {
        advanceUntilIdle()
        viewModel.startEditing()
        val editingDays = viewModel.state.value.editingDays
        assertTrue(editingDays[1]!!.contains("Legs"))
    }

    @Test
    fun `cancelEditing returns to Success when routine exists`() = runTest {
        advanceUntilIdle()
        viewModel.selectDaysPerWeek(3)
        viewModel.cancelEditing()
        assertTrue(viewModel.state.value.uiState is RoutineUiState.Success)
    }

    @Test
    fun `cancelEditing returns to NoRoutine when no routine exists`() = runTest {
        // Set up repo to return no routine BEFORE creating the ViewModel
        fakeRepo.routineResponse = RoutineResponse(
            days_per_week = 0,
            routine_days = emptyMap()
        )
        // Create a fresh ViewModel so init runs with the empty routine
        val freshViewModel = RoutineViewModel(fakeRepo)
        advanceUntilIdle()
        freshViewModel.selectDaysPerWeek(3)
        freshViewModel.cancelEditing()
        assertTrue(freshViewModel.state.value.uiState is RoutineUiState.NoRoutine)
    }
}