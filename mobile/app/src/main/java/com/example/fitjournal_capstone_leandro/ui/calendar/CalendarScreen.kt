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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

private val AccentYellow   = Color(0xFFFFEB3B)
private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark    = Color(0xFF2C2C2E)
private val TextGray       = Color(0xFF8E8E93)
private val CurrentMarker  = Color(0xFFFF453A)
private val CellDark       = Color(0xFF1F1F1F)

// Table layout constants — tuned together; changing one may need the others.
private val ExerciseColWidth   = 130.dp
private val LogColWidth        = 60.dp
private val RowHeight          = 40.dp
private val HeaderHeight       = 32.dp
private val MuscleHeaderHeight = 36.dp

private val SelectionBorderWidth = 1.5.dp

/**
 * Calendar screen.
 *
 * Slice A: day tabs + header + actions. (done)
 * Slice B: the table — frozen exercise column + scrollable log columns. (done)
 * Slice C: tap-to-select + visual highlight on the name cell. (done)
 * Slice D (partial): muscle-group section headers; unit indicator above the table.
 *
 * `unitPreference` is the user's preference from their profile
 * ("metric" or "imperial"); anything else is treated as metric. Used here
 * only to label the table's unit, not to convert values.
 */
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    unitPreference: String = "metric"
) {
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
                    state              = state,
                    unitPreference     = unitPreference,
                    onSelectDay        = { viewModel.selectDay(it) },
                    onAutoSelect       = { viewModel.autoSelectForCurrentDay() },
                    onClearSelections  = { viewModel.clearSelectionsForCurrentDay() },
                    onToggleExercise   = { viewModel.toggleSelection(it) }
                )
            }
        }
    }
}

// ─── READY ────────────────────────────────────────────────────────────────────

@Composable
private fun ReadyContent(
    state: CalendarScreenState,
    unitPreference: String,
    onSelectDay: (Int) -> Unit,
    onAutoSelect: () -> Unit,
    onClearSelections: () -> Unit,
    onToggleExercise: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        DayTabsRow(
            daysPerWeek = state.daysPerWeek,
            currentDay  = state.currentDayNumber,
            selectedDay = state.selectedDayNumber,
            onSelectDay = onSelectDay
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

        Spacer(modifier = Modifier.height(12.dp))

        UnitIndicator(unitPreference = unitPreference)

        Spacer(modifier = Modifier.height(6.dp))

        ExerciseLogTable(
            rows             = state.exercisesForSelectedDay,
            columns          = state.sessionColumns,
            onToggleExercise = onToggleExercise
        )
    }
}

// ─── DAY TABS ─────────────────────────────────────────────────────────────────

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

@Composable
private fun TabsLegend() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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

// ─── UNIT INDICATOR ───────────────────────────────────────────────────────────

/**
 * Small right-aligned caption above the table telling the user which
 * weight unit the values are in. Matches the user's profile preference
 * — does NOT convert values, only labels them.
 *
 * Non-interactive in v1. P3 backlog: make tappable, either deep-linking
 * to ProfileSettings or offering an inline kg/lb toggle.
 */
@Composable
private fun UnitIndicator(unitPreference: String) {
    val label = if (unitPreference == "imperial") "LB" else "KG"
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = "Showing weights in $label",
            color      = TextGray,
            fontSize   = 11.sp,
            fontStyle  = FontStyle.Italic,
            fontFamily = myCustomFont,
            modifier   = Modifier.align(Alignment.CenterEnd)
        )
    }
}

// ─── TABLE ────────────────────────────────────────────────────────────────────

@Composable
private fun ExerciseLogTable(
    rows: List<CalendarExerciseRow>,
    columns: List<SessionColumn>,
    onToggleExercise: (Int) -> Unit
) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SurfaceDark)
    ) {
        // Top header row — frozen "Exercise" label + scrollable date headers
        Row(modifier = Modifier.fillMaxWidth()) {
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

        // Body — vertically scrollable as a unit
        Column(modifier = Modifier.verticalScroll(vScroll)) {
            var previousMuscle: String? = null

            rows.forEach { row ->
                if (row.muscleGroup != previousMuscle) {
                    MuscleGroupSectionRow(
                        muscleName  = row.muscleGroup,
                        columnCount = columns.size,
                        hScroll     = hScroll
                    )
                    previousMuscle = row.muscleGroup
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    val nameCellModifier = Modifier
                        .width(ExerciseColWidth)
                        .height(RowHeight)
                        .background(CellDark)
                        .clickable { onToggleExercise(row.exerciseId) }
                        .let { base ->
                            if (row.isSelected) {
                                base.border(SelectionBorderWidth, AccentYellow)
                            } else {
                                base
                            }
                        }
                        .padding(horizontal = 10.dp)

                    Box(
                        modifier = nameCellModifier,
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

@Composable
private fun MuscleGroupSectionRow(
    muscleName: String,
    columnCount: Int,
    hScroll: androidx.compose.foundation.ScrollState
) {
    val borderColor = TextGray.copy(alpha = 0.4f)

    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(ExerciseColWidth)
                .height(MuscleHeaderHeight)
                .background(CellDark)
                .drawBottomBorder(color = borderColor, widthDp = 1.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text       = muscleName.uppercase(),
                color      = Color.White,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = myCustomFont
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(hScroll)
        ) {
            repeat(columnCount) {
                Box(
                    modifier = Modifier
                        .width(LogColWidth)
                        .height(MuscleHeaderHeight)
                        .drawBottomBorder(color = borderColor, widthDp = 1.dp)
                )
            }
        }
    }
}

private fun Modifier.drawBottomBorder(color: Color, widthDp: Dp): Modifier =
    this.drawBehind {
        val strokePx = widthDp.toPx()
        drawLine(
            color = color,
            start = Offset(0f, size.height - strokePx / 2),
            end   = Offset(size.width, size.height - strokePx / 2),
            strokeWidth = strokePx
        )
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

private fun formatWeight(weight: Float?): String {
    if (weight == null || weight == 0f) return EMPTY_CELL
    return weight.toInt().toString()
}

private fun formatDate(raw: String): String {
    return if (raw.length >= 10 && raw[4] == '-' && raw[7] == '-') {
        raw.substring(5, 10)  // "04-20"
    } else {
        raw
    }
}