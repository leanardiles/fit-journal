package com.example.fitjournal_capstone_leandro.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.fitjournal_capstone_leandro.ui.calendar.CalendarViewModel
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsViewModel
import com.example.fitjournal_capstone_leandro.ui.exercise_details.ExerciseDetailsScreen
import com.example.fitjournal_capstone_leandro.ui.exercises.UserExercisesViewModel
import com.example.fitjournal_capstone_leandro.ui.exercises.UserExercisesScreen
import com.example.fitjournal_capstone_leandro.ui.home.DashboardViewModel
import com.example.fitjournal_capstone_leandro.ui.home.HomeScreen
import com.example.fitjournal_capstone_leandro.ui.home.HomeViewModel
import com.example.fitjournal_capstone_leandro.ui.profile.ProfileSettingsScreen
import com.example.fitjournal_capstone_leandro.ui.profile.ProfileSettingsViewModel
import com.example.fitjournal_capstone_leandro.ui.routine.RoutineScreen
import com.example.fitjournal_capstone_leandro.ui.routine.RoutineViewModel
import com.example.fitjournal_capstone_leandro.ui.workout.WorkoutViewModel
import com.example.fitjournal_capstone_leandro.ui.workout.WorkoutScreen



@Composable
fun AppNavigation(
    homeViewModel: HomeViewModel,
    dashboardViewModel: DashboardViewModel,
    profileSettingsViewModel: ProfileSettingsViewModel,
    userExercisesViewModel: UserExercisesViewModel,
    exerciseDetailsViewModel: ExerciseDetailsViewModel,
    routineViewModel: RoutineViewModel,
    authViewModel: AuthViewModel,
    workoutViewModel: WorkoutViewModel,
    calendarViewModel: CalendarViewModel,
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

        // Home tab
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                dashboardViewModel = dashboardViewModel,
                onEditRoutineClick = { navController.navigate(Routes.ROUTINE) }
            )
        }

        // Exercises tab
        composable(Routes.EXERCISES) {
            val unitPreference = authViewModel.userProfile.collectAsState().value?.user_unit_preference ?: "metric"
            UserExercisesScreen(
                viewModel = userExercisesViewModel,
                unitPreference = unitPreference
            )
        }

        // Exercise details
        composable(Routes.EXERCISE_DETAILS) {
            ExerciseDetailsScreen(
                viewModel = exerciseDetailsViewModel
            )
        }

        // Routine
        composable(Routes.ROUTINE) {
            RoutineScreen(viewModel = routineViewModel)
        }

        composable(Routes.PROFILE_SETTINGS) {
            ProfileSettingsScreen(
                viewModel = profileSettingsViewModel,
                onSaved = {
                    authViewModel.fetchProfile()
                    navController.popBackStack()
                }
            )
        }

        // Workout tab
        composable(Routes.WORKOUT) {
            WorkoutScreen(
                viewModel = workoutViewModel,
                onWorkoutComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        // Calendar tab
        composable(Routes.CALENDAR) {
            val unitPreference = authViewModel.userProfile.collectAsState().value?.user_unit_preference ?: "metric"
            CalendarScreen(
                viewModel = calendarViewModel,
                unitPreference = unitPreference
            )
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