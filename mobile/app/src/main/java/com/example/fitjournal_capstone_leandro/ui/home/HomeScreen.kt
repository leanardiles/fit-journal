package com.example.fitjournal_capstone_leandro.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.data.model.RoutineResponse
import com.example.fitjournal_capstone_leandro.ui.theme.AccentYellow
import com.example.fitjournal_capstone_leandro.ui.theme.BackgroundDark
import com.example.fitjournal_capstone_leandro.ui.theme.NoteYellow
import com.example.fitjournal_capstone_leandro.ui.theme.NoteText
import com.example.fitjournal_capstone_leandro.ui.theme.NoteTextSoft
import com.example.fitjournal_capstone_leandro.ui.theme.NoteTape
import com.example.fitjournal_capstone_leandro.ui.theme.ErrorRed
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont
import com.example.fitjournal_capstone_leandro.ui.shared.PrimaryButton


@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    dashboardViewModel: DashboardViewModel,
    onMuscleGroupClick: ((String) -> Unit)? = null,
    onEditRoutineClick: () -> Unit = {}
) {
    val dashboardState by dashboardViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        dashboardViewModel.loadDashboard()
    }

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

        Spacer(modifier = Modifier.height(24.dp))

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
                    color = ErrorRed,
                    fontFamily = myCustomFont
                )
                Spacer(modifier = Modifier.height(8.dp))
                PrimaryButton(
                    text = "Retry",
                    onClick = { dashboardViewModel.loadDashboard() }
                )
            }

            is DashboardUiState.Success -> {
                QuickStatsCard(workoutsThisWeek = dashboardState.workoutsThisWeek)

                Spacer(modifier = Modifier.height(30.dp))

                CurrentRoutineCard(
                    routine = dashboardState.routine,
                    currentDay = dashboardState.currentDay,
                    onEditClick = onEditRoutineClick
                )

                Spacer(modifier = Modifier.height(30.dp))

                SetsPerMuscleCard(
                    setsThisWeek = dashboardState.setsThisWeek,
                    setsLast7 = dashboardState.setsLast7,
                    window = dashboardState.setsWindow,
                    onWindowChange = { dashboardViewModel.setSetsWindow(it) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ========================================
// Sticky-note wrapper
// ========================================

@Composable
fun StickyNote(
    tiltDegrees: Float,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .rotate(tiltDegrees)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, RoundedCornerShape(4.dp))
                .background(NoteYellow, RoundedCornerShape(4.dp))
                .padding(16.dp),
            content = content
        )
        // Tape strip, sitting slightly above the note's top edge
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-8).dp)
                .rotate(-2.5f)
                .size(width = 74.dp, height = 20.dp)
                .background(NoteTape)
        )
    }
}

// ========================================
// MODULE 1: Quick Stats
// ========================================

@Composable
fun QuickStatsCard(workoutsThisWeek: Int) {
    StickyNote(tiltDegrees = -1.4f) {
        Text(
            text = "Quick Stats",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NoteText,
            fontFamily = myCustomFont
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Workouts completed this week: $workoutsThisWeek",
            fontSize = 15.sp,
            color = NoteTextSoft,
            fontFamily = myCustomFont
        )
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
    StickyNote(tiltDegrees = 1.1f) {
        Text(
            text = "Your Current Routine",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NoteText,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (routine == null || routine.days_per_week == 0) {
            Text(
                text = "No routine set up yet.",
                color = NoteTextSoft,
                fontSize = 14.sp,
                fontFamily = myCustomFont
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onEditClick,
                border = BorderStroke(1.5.dp, NoteText),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NoteText)
            ) {
                Text("Create Routine", color = NoteText, fontFamily = myCustomFont)
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            routine.days.sortedBy { it.day_number }.forEach { dayObj ->
                val isCurrentDay = dayObj.day_number == currentDay
                val muscles = if (dayObj.day_type == "manual") {
                    dayObj.exercises.map { it.muscle_group }.distinct()
                } else {
                    dayObj.muscles.map { it.muscle_group }
                }
                val name = dayObj.name
                val label = if (!name.isNullOrBlank()) "Day ${dayObj.day_number} — $name: " else "Day ${dayObj.day_number}: "
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(
                            Color.Black.copy(alpha = if (isCurrentDay) 0.10f else 0.06f),
                            RoundedCornerShape(8.dp)
                        )
                        .then(
                            if (isCurrentDay) Modifier.border(
                                width = 2.dp,
                                color = NoteText,
                                shape = RoundedCornerShape(8.dp)
                            ) else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isCurrentDay) NoteText else NoteTextSoft,
                        fontWeight = if (isCurrentDay) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        fontFamily = myCustomFont
                    )
                    Text(
                        text = muscles.joinToString(", "),
                        color = if (isCurrentDay) NoteText else NoteTextSoft,
                        fontSize = 14.sp,
                        fontFamily = myCustomFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onEditClick,
                border = BorderStroke(1.5.dp, NoteText),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = NoteText)
            ) {
                Text("Edit Routine", color = NoteText, fontFamily = myCustomFont)
            }
        }
    }
}

// ========================================
// MODULE 3: Sets per muscle
// ========================================

@Composable
fun SetsPerMuscleCard(
    setsThisWeek: Map<String, Int>,
    setsLast7: Map<String, Int>,
    window: String,
    onWindowChange: (String) -> Unit
) {
    StickyNote(tiltDegrees = -0.8f) {
        Text(
            text = "Sets per Muscle",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NoteText,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(8.dp))

        // This week / Last 7 days toggle (text toggle, on-note styling)
        Row(verticalAlignment = Alignment.CenterVertically) {
            val thisWeekSel = window == "week"
            Text(
                text = "This week",
                color = if (thisWeekSel) NoteText else NoteTextSoft,
                fontWeight = if (thisWeekSel) FontWeight.Bold else FontWeight.Normal,
                fontFamily = myCustomFont,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onWindowChange("week") }.padding(vertical = 2.dp, horizontal = 2.dp)
            )
            Text("  /  ", color = NoteTextSoft, fontFamily = myCustomFont, fontSize = 14.sp)
            val last7Sel = window == "last7"
            Text(
                text = "Last 7 days",
                color = if (last7Sel) NoteText else NoteTextSoft,
                fontWeight = if (last7Sel) FontWeight.Bold else FontWeight.Normal,
                fontFamily = myCustomFont,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onWindowChange("last7") }.padding(vertical = 2.dp, horizontal = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val sets = if (window == "last7") setsLast7 else setsThisWeek
        // Most-trained first; ties (incl. all the 0s) fall back to alphabetical.
        val sorted = sets.entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }
        )
        if (sorted.isEmpty()) {
            Text(
                text = "No sets logged in this window yet.",
                color = NoteTextSoft,
                fontSize = 14.sp,
                fontFamily = myCustomFont
            )
        } else {
            // Column-major: left column is the top half of the ranking (most sets),
            // right column continues with the bottom half — each row pairs rank i
            // with rank i+half, so reading DOWN the left column is strict descending.
            val half = (sorted.size + 1) / 2
            for (i in 0 until half) {
                val left = sorted[i]
                val right = sorted.getOrNull(i + half)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    SetsCell(left.key, left.value, Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    if (right != null) {
                        SetsCell(right.key, right.value, Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SetsCell(muscle: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Fixed-width muscle name so the count sits right beside it (counts still
        // line up in a column); the cell's leftover width falls to the RIGHT of the
        // count, forming the gap to the next column.
        Text(
            muscle,
            color = NoteText,
            fontSize = 14.sp,
            fontFamily = myCustomFont,
            maxLines = 1,
            modifier = Modifier.width(84.dp)
        )
        Text(
            "$count",
            color = NoteTextSoft,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = myCustomFont
        )
    }
}