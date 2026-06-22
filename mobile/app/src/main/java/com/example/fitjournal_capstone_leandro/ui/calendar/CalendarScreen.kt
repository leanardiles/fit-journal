package com.example.fitjournal_capstone_leandro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

private val AccentYellow   = Color(0xFFFFEB3B)
private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark    = Color(0xFF2C2C2E)
private val TextGray       = Color(0xFF8E8E93)
private val CurrentMarker  = Color(0xFFFF453A)  // small red dot for "current day"

/**
 * Calendar screen.
 *
 * Slice A (current): day tabs + header + actions.
 * Slice B (next): the table (frozen exercise column + scrollable log columns).
 * Slice C: selection toggling + table interactions.
 * Slice D: polish (empty states, weight-0 rendering, edge cues).
 */
@Composable
fun CalendarScreen(viewModel: CalendarViewModel) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when (state.uiState) {
            is CalendarUiState.Idle,
            is CalendarUiState.Loading -> {
                CircularProgressIndicator(
                    color = AccentYellow,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is CalendarUiState.Error -> {
                ErrorContent(
                    message = (state.uiState as CalendarUiState.Error).message,
                    onRetry = { viewModel.loadCalendar() }
                )
            }

            is CalendarUiState.Ready -> {
                ReadyContent(
                    state = state,
                    onSelectDay        = { viewModel.selectDay(it) },
                    onAutoSelect       = { viewModel.autoSelectForCurrentDay() },
                    onClearSelections  = { viewModel.clearSelectionsForCurrentDay() }
                )
            }
        }
    }
}

// ─── READY ────────────────────────────────────────────────────────────────────

@Composable
private fun ReadyContent(
    state: CalendarScreenState,
    onSelectDay: (Int) -> Unit,
    onAutoSelect: () -> Unit,
    onClearSelections: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        DayTabsRow(
            daysPerWeek      = state.daysPerWeek,
            currentDay       = state.currentDayNumber,
            selectedDay      = state.selectedDayNumber,
            onSelectDay      = onSelectDay
        )

        Spacer(modifier = Modifier.height(8.dp))

        TabsLegend()

        Spacer(modifier = Modifier.height(16.dp))

        DayHeader(
            dayNumber    = state.selectedDayNumber,
            muscleGroups = state.muscleGroupsForSelectedDay
        )

        Spacer(modifier = Modifier.height(12.dp))

        ActionButtonsRow(
            onAutoSelect      = onAutoSelect,
            onClearSelections = onClearSelections
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Slice B placeholder — the table goes here next session
        TablePlaceholder()
    }
}

// ─── DAY TABS ─────────────────────────────────────────────────────────────────

/**
 * Horizontally-scrollable row of day chips. Each chip:
 *  - shows "Day N"
 *  - is filled yellow when it's the SELECTED day
 *  - has a small red dot when it's the CURRENT day (from workout_state)
 *
 * On first load currentDay == selectedDay, so both signals appear on the
 * same chip simultaneously (yellow fill + red dot).
 */
@Composable
private fun DayTabsRow(
    daysPerWeek: Int,
    currentDay: Int,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // If daysPerWeek == 0 (no routine yet) this loop produces nothing —
        // an empty-state message would be a Slice D refinement.
        (1..daysPerWeek).forEach { day ->
            DayChip(
                day        = day,
                isSelected = day == selectedDay,
                isCurrent  = day == currentDay,
                onClick    = { onSelectDay(day) }
            )
        }
    }
}

@Composable
private fun DayChip(
    day: Int,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) AccentYellow else SurfaceDark
    val textColor       = if (isSelected) Color.Black else Color.White

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(backgroundColor)
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = "Day $day",
                color      = textColor,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = myCustomFont
            )
        }

        // Current-day marker (small red dot, top-right of the chip)
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(CurrentMarker)
                    .border(width = 2.dp, color = BackgroundDark, shape = CircleShape)
            )
        }
    }
}

/**
 * Tiny legend under the tabs row, since the dual-marker convention
 * (red dot = current, yellow fill = viewing) is not obvious at first.
 * Can be removed in Slice D if it ever feels redundant.
 */
@Composable
private fun TabsLegend() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // current
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(CurrentMarker)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text       = "current",
                color      = TextGray,
                fontSize   = 11.sp,
                fontFamily = myCustomFont
            )
        }

        Text(text = "·", color = TextGray, fontSize = 11.sp)

        // viewing
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(AccentYellow)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text       = "viewing",
                color      = TextGray,
                fontSize   = 11.sp,
                fontFamily = myCustomFont
            )
        }
    }
}

// ─── HEADER ───────────────────────────────────────────────────────────────────

@Composable
private fun DayHeader(
    dayNumber: Int,
    muscleGroups: List<String>
) {
    val muscleLine =
        if (muscleGroups.isNotEmpty()) muscleGroups.joinToString(", ") else "—"

    Column {
        Text(
            text       = "Day $dayNumber — $muscleLine",
            color      = Color.White,
            fontSize   = 16.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text       = "Tap an exercise to select it for the next workout.",
            color      = TextGray,
            fontSize   = 12.sp,
            fontStyle  = FontStyle.Italic,
            fontFamily = myCustomFont
        )
    }
}

// ─── BUTTONS ──────────────────────────────────────────────────────────────────

@Composable
private fun ActionButtonsRow(
    onAutoSelect: () -> Unit,
    onClearSelections: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onAutoSelect,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(
                text       = "Auto-select",
                fontSize   = 13.sp,
                fontFamily = myCustomFont
            )
        }

        OutlinedButton(
            onClick = onClearSelections,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceDark),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(
                text       = "Clear",
                fontSize   = 13.sp,
                fontFamily = myCustomFont
            )
        }
    }
}

// ─── ERROR ────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text       = message,
            color      = Color(0xFFFF453A),
            fontSize   = 16.sp,
            fontFamily = myCustomFont
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)
        ) {
            Text(
                text       = "Retry",
                color      = Color.Black,
                fontFamily = myCustomFont
            )
        }
    }
}

// ─── PLACEHOLDER FOR SLICE B (the table) ──────────────────────────────────────

@Composable
private fun TablePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = "Exercises + logs table coming next.",
            color      = TextGray,
            fontSize   = 12.sp,
            fontStyle  = FontStyle.Italic,
            fontFamily = myCustomFont
        )
    }
}