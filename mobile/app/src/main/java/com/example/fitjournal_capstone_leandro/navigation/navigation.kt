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
import com.example.fitjournal_capstone_leandro.ui.auth.AuthViewModel
import com.example.fitjournal_capstone_leandro.ui.auth.LoginScreen
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsViewModel
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsScreen
import com.example.fitjournal_capstone_leandro.ui.exercises.ExercisesScreen
import com.example.fitjournal_capstone_leandro.ui.exercises.ExercisesViewModel
import com.example.fitjournal_capstone_leandro.ui.home.HomeScreen
import com.example.fitjournal_capstone_leandro.ui.home.HomeViewModel
import com.example.fitjournal_capstone_leandro.ui.calendar.CalendarScreen


@Composable
fun AppNavigation(
    homeViewModel: HomeViewModel,
    exercisesViewModel: ExercisesViewModel,
    exerciseDetailsViewModel: ExerciseDetailsViewModel,
    authViewModel: AuthViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier
    ) {
        // Login / Register screen
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true } // Can't go back to login
                    }
                }
            )
        }
        // Profile tab (placeholder)
        composable("profile") {
            PlaceholderScreen(title = "Profile")
        }

        // Calendar tab (placeholder)
        composable("calendar") {
            CalendarScreen()
        }

        // Home tab
        composable("home") {
            HomeScreen(
                viewModel = homeViewModel,
                onMuscleGroupClick = { muscle ->
                }
            )
        }

        // Exercises tab
        composable("exercises") {
            ExercisesScreen(
                viewModel = exercisesViewModel,
                onExerciseClick = { exercise ->
                    exerciseDetailsViewModel.selectExercise(exercise)
                    navController.navigate("exerciseDetails")
                },
            )
        }

        // Exercise details
        composable("exerciseDetails") {
            ExerciseDetailsScreen(
                viewModel = exerciseDetailsViewModel
            )
        }

        // Settings tab (placeholder)
        composable("settings") {
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