package com.example.fitjournal_capstone_leandro

import com.example.fitjournal_capstone_leandro.ui.home.HomeViewModel
import com.example.fitjournal_capstone_leandro.ui.home.HomeViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsViewModel
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsViewModelFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitjournal_capstone_leandro.data.local.FitJournalDatabase
import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.data.network.service
import com.example.fitjournal_capstone_leandro.data.repository.CalendarRepository
import com.example.fitjournal_capstone_leandro.data.repository.DashboardRepository
import com.example.fitjournal_capstone_leandro.data.repository.ExerciseRepository
import com.example.fitjournal_capstone_leandro.data.repository.UserExercisesRepository
import com.example.fitjournal_capstone_leandro.data.repository.UserRoutineRepository
import com.example.fitjournal_capstone_leandro.data.repository.WorkoutRepository
import com.example.fitjournal_capstone_leandro.navigation.AppNavigation
import com.example.fitjournal_capstone_leandro.ui.auth.AuthViewModel
import com.example.fitjournal_capstone_leandro.ui.auth.AuthViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.calendar.CalendarViewModel
import com.example.fitjournal_capstone_leandro.ui.calendar.CalendarViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.exercises.UserExercisesViewModel
import com.example.fitjournal_capstone_leandro.ui.exercises.UserExercisesViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.home.DashboardViewModel
import com.example.fitjournal_capstone_leandro.ui.home.DashboardViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.routine.RoutineViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.shared.BottomNavBar
import com.example.fitjournal_capstone_leandro.ui.shared.BottomNavItem
import com.example.fitjournal_capstone_leandro.ui.shared.ProfileTopBar
import com.example.fitjournal_capstone_leandro.ui.stopwatch.StopwatchBottomSheet
import com.example.fitjournal_capstone_leandro.ui.stopwatch.StopwatchViewModel
import com.example.fitjournal_capstone_leandro.ui.workout.WorkoutViewModel
import com.example.fitjournal_capstone_leandro.ui.workout.WorkoutViewModelFactory
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient
import com.example.fitjournal_capstone_leandro.navigation.Routes
import com.example.fitjournal_capstone_leandro.ui.profile.ProfileSettingsViewModel
import com.example.fitjournal_capstone_leandro.ui.profile.ProfileSettingsViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.routine.RoutineViewModel
import com.example.fitjournal_capstone_leandro.ui.theme.fitJournalCapstoneLeandroTheme
import kotlin.getValue



class MainActivity : ComponentActivity() {

    private val database by lazy {
        Log.d("FitJournal", "Creating database instance")
        FitJournalDatabase.getDatabase(applicationContext)
    }

    // Create one repository shared by all ViewModels
    private val repository by lazy {
        Log.d("FitJournal", "Creating shared repository")
        ExerciseRepository(
            service,
            database.muscleDao(),
            database.exerciseDao()
        )
    }

    private val tokenManager by lazy {
        TokenManager(applicationContext).also {
            RetrofitClient.initialize(it)
        }
    }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(tokenManager)
    }

    private val homeViewModel: HomeViewModel by viewModels {
        Log.d("FitJournal", "Creating HomeViewModel with repository")
        HomeViewModelFactory(repository)
    }

    private val profileSettingsViewModel: ProfileSettingsViewModel by viewModels {
        ProfileSettingsViewModelFactory(tokenManager)
    }

    private val exerciseDetailsViewModel: ExerciseDetailsViewModel by viewModels {
        Log.d("FitJournal", "Creating ExerciseDetailsViewModel with repository")
        ExerciseDetailsViewModelFactory(repository)
    }

    private val userExercisesRepository by lazy {
        UserExercisesRepository(tokenManager)
    }

    private val userExercisesViewModel: UserExercisesViewModel by viewModels {
        UserExercisesViewModelFactory(userExercisesRepository)
    }

    private val userRoutineRepository by lazy {
        UserRoutineRepository(tokenManager)
    }

    private val routineViewModel: RoutineViewModel by viewModels {
        RoutineViewModelFactory(userRoutineRepository)
    }

    private val dashboardRepository by lazy {
        DashboardRepository(tokenManager)
    }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(dashboardRepository)
    }

    private val workoutRepository by lazy {
        WorkoutRepository(tokenManager)
    }

    private val workoutViewModel: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(workoutRepository)
    }

    private val calendarRepository by lazy {
        CalendarRepository(tokenManager)
    }

    private val calendarViewModel: CalendarViewModel by viewModels {
        CalendarViewModelFactory(calendarRepository)
    }

    private val stopwatchViewModel: StopwatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            fitJournalCapstoneLeandroTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val userProfile by authViewModel.userProfile.collectAsState()

                LaunchedEffect(currentRoute) {
                    if (currentRoute != Routes.LOGIN) {
                        authViewModel.fetchProfile()
                    }
                }

                var showStopwatch by remember { mutableStateOf(false) }

                val bottomNavItems = listOf(
                    BottomNavItem.Calendar,
                    BottomNavItem.Exercises,
                    BottomNavItem.Home,
                    BottomNavItem.Timer,
                    BottomNavItem.Workout
                )

                Scaffold(
                    topBar = {
                        // Hide top bar entirely on login screen
                        if (currentRoute != Routes.LOGIN) {
                            // Show back button on all screens except main tabs
                            val showBackButton = currentRoute !in listOf(Routes.HOME, Routes.CALENDAR, Routes.EXERCISES)

                            ProfileTopBar(
                                userName = userProfile?.user_first_name ?: "User",
                                showBackButton = showBackButton,
                                onBackClick = { navController.popBackStack() },
                                onRoutineClick = { navController.navigate(Routes.ROUTINE) },
                                onSettingsClick = { navController.navigate(Routes.PROFILE_SETTINGS) },
                                onLogoutClick = {
                                    tokenManager.clearAll()
                                    navController.navigate(Routes.LOGIN) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        // Hide bottom nav on login screen
                        if (currentRoute != Routes.LOGIN) {
                            BottomNavBar(
                                items = bottomNavItems,
                                currentRoute = currentRoute,
                                onItemClick = { item ->
                                    if (item.route == Routes.TIMER) {
                                        showStopwatch = true
                                    } else {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = (item.route != Routes.HOME)
                                        }
                                    }
                                },
                                modifier = Modifier.navigationBarsPadding()
                            )
                        }
                    }
                ) { innerPadding ->
                    AppNavigation(
                        homeViewModel = homeViewModel,
                        dashboardViewModel = dashboardViewModel,
                        profileSettingsViewModel = profileSettingsViewModel,
                        exerciseDetailsViewModel = exerciseDetailsViewModel,
                        userExercisesViewModel = userExercisesViewModel,
                        routineViewModel = routineViewModel,
                        authViewModel = authViewModel,
                        workoutViewModel = workoutViewModel,
                        calendarViewModel = calendarViewModel,
                        navController = navController,
                        tokenManager = tokenManager,
                        modifier = Modifier.padding(innerPadding)
                    )

                    if (showStopwatch) {
                        StopwatchBottomSheet(
                            viewModel = stopwatchViewModel,
                            onDismiss = { showStopwatch = false }
                        )
                    }
                }
            }
        }
        Log.d("FitJournal", "MainActivity onCreate completed")
    }
}