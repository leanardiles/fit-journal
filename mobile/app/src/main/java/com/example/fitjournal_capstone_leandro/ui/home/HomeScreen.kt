package com.example.fitjournal_capstone_leandro.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark = Color(0xFF2C2C2E)
private val AccentBlue = Color(0xFF5595CE)
private val AccentYellow = Color(0xFFFFEB3B)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    dashboardViewModel: DashboardViewModel,
    onMuscleGroupClick: ((String) -> Unit)? = null,
    onEditRoutineClick: () -> Unit = {}
) {
    val dashboardState by dashboardViewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Dashboard 💪",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(16.dp))

        when (dashboardState.uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentYellow)
                }
            }

            is DashboardUiState.Error -> {
                Text(
                    text = (dashboardState.uiState as DashboardUiState.Error).message,
                    color = Color.Red
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { dashboardViewModel.loadDashboard() }) {
                    Text("Retry")
                }
            }

            is DashboardUiState.Success -> {
                // Module 1: Quick Stats
                QuickStatsCard(workoutsThisWeek = dashboardState.workoutsThisWeek)

                Spacer(modifier = Modifier.height(16.dp))

                // Module 2: Current Routine
                CurrentRoutineCard(
                    routine = dashboardState.routine,
                    currentDay = dashboardState.currentDay,
                    onEditClick = onEditRoutineClick
                )
            }
        }
    }
}

// ========================================
// MODULE 1: Quick Stats
// ========================================

@Composable
fun QuickStatsCard(workoutsThisWeek: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick Stats",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = myCustomFont
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Workouts completed this week: $workoutsThisWeek",
                fontSize = 14.sp,
                color = Color.LightGray
            )
        }
    }
}

// ========================================
// MODULE 2: Current Routine
// ========================================

@Composable
fun CurrentRoutineCard(
    routine: RoutineResponse?,
    currentDay: Int,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Your Current Routine",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = myCustomFont
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (routine == null || routine.days_per_week == 0) {
                Text(
                    text = "No routine set up yet.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Create Routine", color = Color.White)
                }
            } else {
                Text(
                    text = "Training ${routine.days_per_week} ${if (routine.days_per_week == 1) "day" else "days"} per week",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Day rows
                routine.routine_days.entries.sortedBy { it.key }.forEach { (day, muscles) ->
                    val isCurrentDay = day.toIntOrNull() == currentDay
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color(0xFF3A3A3C), RoundedCornerShape(8.dp))
                            .then(
                                if (isCurrentDay) Modifier.border(
                                    width = 2.dp,
                                    color = Color(0xFFFFEB3B),
                                    shape = RoundedCornerShape(8.dp)
                                ) else Modifier
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Day $day: ",
                            color = if (isCurrentDay) Color.White else Color.LightGray,
                            fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        Text(
                            text = muscles.joinToString(", "),
                            color = if (isCurrentDay) Color.White else Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onEditClick,
                    border = ButtonDefaults.outlinedButtonBorder,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Edit Routine", color = Color.White)
                }
            }
        }
    }
}