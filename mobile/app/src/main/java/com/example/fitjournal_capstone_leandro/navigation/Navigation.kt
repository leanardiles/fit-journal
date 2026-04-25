package com.example.fitjournal_capstone_leandro.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.fitjournal_capstone_leandro.data.local.TokenManager
import com.example.fitjournal_capstone_leandro.ui.auth.AuthViewModel
import com.example.fitjournal_capstone_leandro.ui.auth.LoginScreen
import com.example.fitjournal_capstone_leandro.ui.calendar.CalendarScreen
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsViewModel
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsScreen
import com.example.fitjournal_capstone_leandro.ui.exercises.ExercisesScreen
import com.example.fitjournal_capstone_leandro.ui.exercises.ExercisesViewModel
import com.example.fitjournal_capstone_leandro.ui.home.HomeScreen
import com.example.fitjournal_capstone_leandro.ui.home.HomeViewModel



@Composable
fun AppNavigation(
    homeViewModel: HomeViewModel,
    exercisesViewModel: ExercisesViewModel,
    exerciseDetailsViewModel: ExerciseDetailsViewModel,
    authViewModel: AuthViewModel,
    navController: NavHostController,
    tokenManager: TokenManager,
    modifier: Modifier = Modifier
) {
    val startDestination = if (tokenManager.isLoggedIn()) Routes.HOME else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Login / Register screen
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true } // Can't go back to login
                    }
                }
            )
        }
        // Profile tab (placeholder)
        composable(Routes.PROFILE) {
            PlaceholderScreen(title = "Profile")
        }

        // Calendar tab (placeholder)
        composable(Routes.CALENDAR) {
            CalendarScreen()
        }

        // Home tab
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onMuscleGroupClick = { muscle ->
                }
            )
        }

        // Exercises tab
        composable(Routes.EXERCISES) {
            ExercisesScreen(
                viewModel = exercisesViewModel,
                onExerciseClick = { exercise ->
                    exerciseDetailsViewModel.selectExercise(exercise)
                    navController.navigate(Routes.EXERCISE_DETAILS)
                },
            )
        }

        // Exercise details
        composable(Routes.EXERCISE_DETAILS) {
            ExerciseDetailsScreen(
                viewModel = exerciseDetailsViewModel
            )
        }

        // Workout tab (placeholder)
        composable(Routes.WORKOUT) {
            PlaceholderScreen(title = "Workout")
        }

        // Settings tab (placeholder)
        composable(Routes.SETTINGS) {
            PlaceholderScreen(title = "Settings")
        }
    }
}

// Placeholder for future screens
@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1E)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 32.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Coming Soon",
            fontSize = 18.sp,
            color = Color.Gray
        )
    }
}