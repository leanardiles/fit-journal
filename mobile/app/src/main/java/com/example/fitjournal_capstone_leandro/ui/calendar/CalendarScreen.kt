package com.example.fitjournal_capstone_leandro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

private val AccentYellow   = Color(0xFFFFEB3B)
private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark    = Color(0xFF2C2C2E)
private val TextGray       = Color(0xFF8E8E93)
private val CurrentMarker  = Color(0xFFFF453A)  // small red dot for "current day"

// Table layout constants — tuned together; changing one may need the others.
private val ExerciseColWidth = 140.dp           // left frozen column width
private val LogColWidth      = 60.dp            // each session column on the right
private val RowHeight        = 40.dp            // shared row height across both halves
private val HeaderHeight     = 32.dp

/**
 * Calendar screen.
 *
 * Slice A: day tabs + header + actions. (done)
 * Slice B (current): the table — frozen exercise column + scrollable log columns.
 * Slice C: tap-to-select + visual highlight (after toggle endpoint is wired).
 * Slice D: polish (empty states, unit indicator, swipe affordance, date format).
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

        ExerciseLogTable(
            rows    = state.exercisesForSelectedDay,
            columns = state.sessionColumns
        )
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

// ─── TABLE ────────────────────────────────────────────────────────────────────

/**
 * The table: a frozen left column (exercise names) + a horizontally scrollable
 * right region (one column per session, showing the weight or "—").
 *
 * Both halves render the same exercises in the same order with the same row
 * heights so rows align visually across the seam.
 *
 * Vertical scrolling wraps the whole thing — both halves scroll together.
 */
@Composable
private fun ExerciseLogTable(
    rows: List<CalendarExerciseRow>,
    columns: List<SessionColumn>
) {
    // Single vertical scroll wraps both halves so they always stay aligned
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
    ) {
        // Header row — frozen "Exercise" label + scrollable date headers
        Row(modifier = Modifier.fillMaxWidth()) {
            // Frozen header cell
            Box(
                modifier = Modifier
                    .width(ExerciseColWidth)
                    .height(HeaderHeight)
                    .background(Color(0xFF232323))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text       = "Exercise",
                    color      = TextGray,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = myCustomFont
                )
            }
            // Scrollable date headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(hScroll)
                    .background(Color(0xFF232323))
            ) {
                columns.forEach { col ->
                    Box(
                        modifier = Modifier
                            .width(LogColWidth)
                            .height(HeaderHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text       = formatDate(col.workoutDate),
                            color      = TextGray,
                            fontSize   = 10.sp,
                            fontFamily = myCustomFont,
                            textAlign  = TextAlign.Center,
                            maxLines   = 1
                        )
                    }
                }
            }
        }

        // Body rows — vertically scrollable as a unit
        Column(modifier = Modifier.verticalScroll(vScroll)) {
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Frozen exercise-name cell
                    Box(
                        modifier = Modifier
                            .width(ExerciseColWidth)
                            .height(RowHeight)
                            .background(Color(0xFF1F1F1F))
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text       = row.exerciseName,
                            color      = Color.White,
                            fontSize   = 12.sp,
                            fontFamily = myCustomFont,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis
                        )
                    }
                    // Scrollable cells — must share the SAME hScroll as the header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(hScroll)
                    ) {
                        columns.forEach { col ->
                            val log = row.logsBySessionId[col.sessionId]
                            val display = formatWeight(log?.weight)
                            Box(
                                modifier = Modifier
                                    .width(LogColWidth)
                                    .height(RowHeight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text       = display,
                                    color      = if (display == EMPTY_CELL) TextGray else Color.White,
                                    fontSize   = 13.sp,
                                    fontFamily = myCustomFont,
                                    textAlign  = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
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

// ─── HELPERS ──────────────────────────────────────────────────────────────────

private const val EMPTY_CELL = "—"

/**
 * Render the weight for a single cell.
 *
 * Treats null and 0f the same — both become "—" — because the data has both
 * "no log entry for this exercise that session" (null) and "log entry exists
 * but weight is 0/missing" (0f). Both are non-informative to the user.
 *
 * Returned as a plain integer string ("60", not "60.0") since users enter
 * whole numbers most of the time. When per-set weights ship later
 * (e.g. "60-70-80"), only the data shape needs to change; this function
 * already returns a String.
 */
private fun formatWeight(weight: Float?): String {
    if (weight == null || weight == 0f) return EMPTY_CELL
    return weight.toInt().toString()
}

/**
 * Format a workout_date String for the column header.
 *
 * The backend sends ISO dates like "2026-04-20". For now we show just
 * "04-20" (month-day, no year), which is compact enough to fit in a 60dp
 * column. Slice D could upgrade this to localized "20 Apr" once the trade-
 * offs (parsing cost, locale handling) are worth the effort.
 *
 * Falls back to the raw String if it doesn't look like ISO.
 */
private fun formatDate(raw: String): String {
    // Expecting "YYYY-MM-DD". Show "MM-DD".
    return if (raw.length >= 10 && raw[4] == '-' && raw[7] == '-') {
        raw.substring(5, 10)  // "04-20"
    } else {
        raw
    }
}