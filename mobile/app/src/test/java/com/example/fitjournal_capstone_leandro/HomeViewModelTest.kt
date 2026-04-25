package com.example.fitjournal_capstone_leandro

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.fitjournal_capstone_leandro.data.local.ExerciseDao
import com.example.fitjournal_capstone_leandro.data.local.ExerciseEntity
import com.example.fitjournal_capstone_leandro.data.local.MuscleDao
import com.example.fitjournal_capstone_leandro.data.model.Exercise
import com.example.fitjournal_capstone_leandro.data.network.ApiService
import com.example.fitjournal_capstone_leandro.data.repository.ExerciseRepository
import com.example.fitjournal_capstone_leandro.ui.home.HomeScreenAction
import com.example.fitjournal_capstone_leandro.ui.home.HomeScreenState
import com.example.fitjournal_capstone_leandro.ui.home.HomeUiState
import com.example.fitjournal_capstone_leandro.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// ========================================
// FAKE DATA
// ========================================

val fakeMuscleList = listOf("biceps", "triceps", "quads", "hamstrings")

val fakeExercises = listOf(
    Exercise(
        id = "1",
        name = "Barbell Curl",
        target = "biceps",
        equipment = "barbell",
        gifUrl = "",
        bodyPart = "upper arms",
        instructions = emptyList(),
        secondaryMuscles = emptyList()
    )
)

// ========================================
// FAKE API SERVICE
// ========================================

class FakeApiService : ApiService {
    override suspend fun getMuscleList(): List<String> {
        return fakeMuscleList
    }

    override suspend fun getExercisesByMuscle(muscle: String): List<Exercise> {
        return fakeExercises
    }
}

// ========================================
// FAKE DAOs (return empty so repository hits the API)
// ========================================

class FakeMuscleDao : MuscleDao {
    override suspend fun getAllMuscles() = emptyList<com.example.fitjournal_capstone_leandro.data.local.MuscleEntity>()
    override suspend fun insertMuscles(muscles: List<com.example.fitjournal_capstone_leandro.data.local.MuscleEntity>) {}
    override suspend fun deleteAllMuscles() {}
}

class FakeExerciseDao : ExerciseDao {
    override suspend fun getExercisesByMuscle(muscle: String) = emptyList<com.example.fitjournal_capstone_leandro.data.local.ExerciseEntity>()
    override suspend fun insertExercises(exercises: List<com.example.fitjournal_capstone_leandro.data.local.ExerciseEntity>) {}
    override suspend fun getExerciseById(exerciseId: String) = null
    override suspend fun getAllExercises() = emptyList<ExerciseEntity>()
    override suspend fun deleteAllExercises() {}
}

// ========================================
// TEST CLASS
// ========================================

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Test dispatcher controls coroutine execution in tests
    private val testDispatcher = StandardTestDispatcher()

    // Components under test
    private lateinit var viewModel: HomeViewModel
    private lateinit var repository: ExerciseRepository
    private lateinit var fakeApiService: FakeApiService

    @Before
    fun setUp() {
        // Replace Main dispatcher with test dispatcher
        Dispatchers.setMain(testDispatcher)

        fakeApiService = FakeApiService()
        repository = ExerciseRepository(
            apiService = fakeApiService,
            muscleDao = FakeMuscleDao(),
            exerciseDao = FakeExerciseDao()
        )
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        // Restore the original Main dispatcher
        Dispatchers.resetMain()
    }

    // ========================================
    // TESTS
    // ========================================

    @Test
    fun `fetchMuscleGroups sets muscle groups to what the API returns`() = runTest {
        // Arrange — initial state should be empty
        assertEquals(emptyList<String>(), viewModel.state.value.muscleGroups)

        // Act — trigger the fetch action (equivalent to refreshHomePage)
        viewModel.processAction(HomeScreenAction.FetchMuscleGroups)

        // Wait for all coroutines to complete
        advanceUntilIdle()

        // Assert — muscle groups should match what the fake API returned
        assertEquals(fakeMuscleList, viewModel.state.value.muscleGroups)
    }

    @Test
    fun `fetchMuscleGroups sets uiState to Success on success`() = runTest {
        // Act
        viewModel.processAction(HomeScreenAction.FetchMuscleGroups)
        advanceUntilIdle()

        // Assert
        assertEquals(HomeUiState.Success(fakeMuscleList), viewModel.state.value.uiState)
    }

    @Test
    fun `fetchMuscleGroups sets uiState to Error on failure`() = runTest {
        // Arrange — create a repository that throws
        val failingRepository = ExerciseRepository(
            apiService = object : ApiService {
                override suspend fun getMuscleList(): List<String> {
                    throw RuntimeException("Network error")
                }
                override suspend fun getExercisesByMuscle(muscle: String) = emptyList<Exercise>()
            },
            muscleDao = FakeMuscleDao(),
            exerciseDao = FakeExerciseDao()
        )

        // Create viewModel AFTER setting main dispatcher (already done in @Before)
        val failingViewModel = HomeViewModel(failingRepository)

        // Act
        failingViewModel.processAction(HomeScreenAction.FetchMuscleGroups)
        advanceUntilIdle()

        // Assert
        val currentState = failingViewModel.state.value.uiState
        println("DEBUG state: $currentState")
        assert(currentState is HomeUiState.Error) {
            "Expected Error state but got: $currentState"
        }
    }
}