package com.example.fitjournal_capstone_leandro

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.fitjournal_capstone_leandro.data.model.UserExercise
import com.example.fitjournal_capstone_leandro.data.repository.IUserExercisesRepository
import com.example.fitjournal_capstone_leandro.ui.exercises.UserExercisesUiState
import com.example.fitjournal_capstone_leandro.ui.exercises.UserExercisesViewModel
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
// FAKE DATA
// ========================================

val fakeUserExercises = listOf(
    UserExercise(
        exercise_id = 1,
        user_id = 2,
        exercise_name = "Barbell Curl",
        exercise_muscle_group = "Biceps",
        exercise_user_current_weight = 20.0f,
        exercise_is_in_routine = true,
        exercise_times_performed = 3,
        exercise_link = null,
        comments = null
    ),
    UserExercise(
        exercise_id = 2,
        user_id = 2,
        exercise_name = "Squat",
        exercise_muscle_group = "Legs",
        exercise_user_current_weight = 60.0f,
        exercise_is_in_routine = true,
        exercise_times_performed = 5,
        exercise_link = null,
        comments = null
    )
)

// ========================================
// FAKE REPOSITORY
// ========================================

class FakeUserExercisesRepository : IUserExercisesRepository {
    var exercises = fakeUserExercises.toMutableList()
    var shouldFail = false

    override suspend fun getExercises(): Result<List<UserExercise>> {
        return if (shouldFail) Result.failure(Exception("Network error"))
        else Result.success(exercises.toList())
    }

    override suspend fun getMuscleGroups(): Result<List<String>> {
        return if (shouldFail) Result.failure(Exception("Network error"))
        else Result.success(exercises.map { it.exercise_muscle_group }.distinct().sorted())
    }

    override suspend fun getExercisesByMuscle(muscleGroup: String): Result<List<UserExercise>> {
        return if (shouldFail) Result.failure(Exception("Network error"))
        else Result.success(exercises.filter { it.exercise_muscle_group == muscleGroup })
    }

    override suspend fun createExercise(
        name: String,
        muscleGroup: String,
        weight: Float?,
        link: String?,
        comments: String?
    ): Result<UserExercise> {
        if (shouldFail) return Result.failure(Exception("Network error"))
        val new = UserExercise(
            exercise_id = exercises.size + 1,
            user_id = 2,
            exercise_name = name,
            exercise_muscle_group = muscleGroup,
            exercise_user_current_weight = weight,
            exercise_is_in_routine = true,
            exercise_times_performed = 0,
            exercise_link = link,
            comments = comments
        )
        exercises.add(new)
        return Result.success(new)
    }

    override suspend fun deleteExercise(exerciseId: Int): Result<Unit> {
        if (shouldFail) return Result.failure(Exception("Network error"))
        exercises.removeIf { it.exercise_id == exerciseId }
        return Result.success(Unit)
    }

    override suspend fun updateExerciseWeight(exerciseId: Int, weight: Float?): Result<UserExercise> {
        if (shouldFail) return Result.failure(Exception("Network error"))
        val exercise = exercises.find { it.exercise_id == exerciseId }
            ?: return Result.failure(Exception("Not found"))
        val updated = exercise.copy(exercise_user_current_weight = weight)
        exercises[exercises.indexOf(exercise)] = updated
        return Result.success(updated)
    }
}

// ========================================
// TEST CLASS
// ========================================

@OptIn(ExperimentalCoroutinesApi::class)
class UserExercisesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: UserExercisesViewModel
    private lateinit var fakeRepo: FakeUserExercisesRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeUserExercisesRepository()
        viewModel = UserExercisesViewModel(fakeRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchMuscleGroups sets muscle groups from repository`() = runTest {
        advanceUntilIdle()
        assertEquals(listOf("Biceps", "Legs"), viewModel.state.value.muscleGroups)
    }

    @Test
    fun `fetchMuscleGroups sets uiState to Success`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.state.value.uiState is UserExercisesUiState.Success)
    }

    @Test
    fun `fetchMuscleGroups sets uiState to Error on failure`() = runTest {
        fakeRepo.shouldFail = true
        viewModel.fetchMuscleGroups()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.uiState is UserExercisesUiState.Error)
    }

    @Test
    fun `selectMuscleGroup filters exercises correctly`() = runTest {
        advanceUntilIdle()
        viewModel.selectMuscleGroup("Biceps")
        val exercises = viewModel.state.value.exercises
        assertTrue(exercises.all { it.exercise_muscle_group == "Biceps" })
    }

    @Test
    fun `selectMuscleGroup sorts exercises alphabetically`() = runTest {
        advanceUntilIdle()
        viewModel.selectMuscleGroup("Biceps")
        val names = viewModel.state.value.exercises.map { it.exercise_name }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `addExercise sets Error state for duplicate name`() = runTest {
        advanceUntilIdle()
        viewModel.addExercise("Barbell Curl", "Biceps")
        assertTrue(viewModel.state.value.uiState is UserExercisesUiState.Error)
    }

    @Test
    fun `addExercise succeeds for new exercise name`() = runTest {
        advanceUntilIdle()
        viewModel.addExercise("Cable Curl", "Biceps")
        advanceUntilIdle()
        assertTrue(viewModel.exerciseAdded.value)
    }

    @Test
    fun `deleteExercise removes exercise and refreshes list`() = runTest {
        advanceUntilIdle()
        val initialCount = fakeRepo.exercises.size
        viewModel.deleteExercise(1)
        advanceUntilIdle()
        assertEquals(initialCount - 1, fakeRepo.exercises.size)
    }

    @Test
    fun `updateExerciseWeight updates weight in cache`() = runTest {
        advanceUntilIdle()
        viewModel.selectMuscleGroup("Biceps")
        viewModel.updateExerciseWeight(1, 30.0f)
        advanceUntilIdle()
        val exercise = viewModel.state.value.exercises.find { it.exercise_id == 1 }
        assertEquals(30.0f, exercise?.exercise_user_current_weight)
    }

    @Test
    fun `resetError sets uiState back to Success`() = runTest {
        advanceUntilIdle()
        viewModel.addExercise("Barbell Curl", "Biceps")
        viewModel.resetError()
        assertTrue(viewModel.state.value.uiState is UserExercisesUiState.Success)
    }

    @Test
    fun `resetExerciseAdded sets exerciseAdded to false`() = runTest {
        advanceUntilIdle()
        viewModel.addExercise("Cable Curl", "Biceps")
        advanceUntilIdle()
        viewModel.resetExerciseAdded()
        assertEquals(false, viewModel.exerciseAdded.value)
    }
}