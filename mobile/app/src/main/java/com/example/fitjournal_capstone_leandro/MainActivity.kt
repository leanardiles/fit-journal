package com.example.fitjournal_capstone_leandro

import com.example.fitjournal_capstone_leandro.ui.home.HomeViewModel
import com.example.fitjournal_capstone_leandro.ui.home.HomeViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.exercises.ExercisesViewModel
import com.example.fitjournal_capstone_leandro.ui.exercises.ExercisesViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsViewModel  // ← CHANGED
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsViewModelFactory  // ← CHANGED
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.fitjournal_capstone_leandro.data.local.FitJournalDatabase
import com.example.fitjournal_capstone_leandro.data.network.service
import com.example.fitjournal_capstone_leandro.data.repository.ExerciseRepository
import com.example.fitjournal_capstone_leandro.navigation.AppNavigation
import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.ui.auth.AuthViewModel
import com.example.fitjournal_capstone_leandro.ui.auth.AuthViewModelFactory
import com.example.fitjournal_capstone_leandro.ui.BottomNavBar
import com.example.fitjournal_capstone_leandro.ui.BottomNavItem
import com.example.fitjournal_capstone_leandro.ui.shared.ProfileTopBar
import com.example.fitjournal_capstone_leandro.ui.stopwatch.StopwatchBottomSheet
import com.example.fitjournal_capstone_leandro.ui.stopwatch.StopwatchViewModel
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.fitjournal_capstone_leandro.data.network.RetrofitClient
import com.example.fitjournal_capstone_leandro.ui.theme.FitJournal_Capstone_LeandroTheme

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

    private val exercisesViewModel: ExercisesViewModel by viewModels {
        Log.d("FitJournal", "Creating ExercisesViewModel with repository")
        ExercisesViewModelFactory(repository)
    }

    private val exerciseDetailsViewModel: ExerciseDetailsViewModel by viewModels {
        Log.d("FitJournal", "Creating ExerciseDetailsViewModel with repository")
        ExerciseDetailsViewModelFactory(repository)
    }

    private val stopwatchViewModel: StopwatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitJournal_Capstone_LeandroTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

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
                        if (currentRoute != "login") {
                            // Show back button on all screens except main tabs
                            val showBackButton = currentRoute !in listOf("home", "calendar", "exercises")

                            ProfileTopBar(
                                userName = "User Name",
                                showBackButton = showBackButton,
                                onBackClick = { navController.popBackStack() },
                                onAccountClick = { navController.navigate("account") },
                                onSettingsClick = { navController.navigate("settings") },
                                onLogoutClick = {
                                    navController.navigate("home") {
                                        popUpTo(navController.graph.startDestinationId) {
                                            inclusive = true
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        // Hide bottom nav on login screen
                        if (currentRoute != "login") {
                            BottomNavBar(
                                items = bottomNavItems,
                                currentRoute = currentRoute,
                                onItemClick = { item ->
                                    if (item.route == "timer") {
                                        showStopwatch = true
                                    } else {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = (item.route != "home")
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
                        exercisesViewModel = exercisesViewModel,
                        exerciseDetailsViewModel = exerciseDetailsViewModel,
                        authViewModel = authViewModel,
                        navController = navController,
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