package com.example.fitjournal_capstone_leandro.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.AccentYellow
import com.example.fitjournal_capstone_leandro.ui.theme.BackgroundDark
import com.example.fitjournal_capstone_leandro.ui.theme.SurfaceDark
import com.example.fitjournal_capstone_leandro.ui.theme.TextGray
import com.example.fitjournal_capstone_leandro.ui.theme.ErrorRed
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont
import com.example.fitjournal_capstone_leandro.ui.shared.PrimaryButton
import com.example.fitjournal_capstone_leandro.ui.shared.SecondaryButton
import com.example.fitjournal_capstone_leandro.ui.shared.MuscleTab
import java.util.Calendar
import java.util.TimeZone


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualLogScreen(
    viewModel: ManualLogViewModel,
    unitPreference: String,
    onLogged: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadLibrary() }
    var showDatePicker by remember { mutableStateOf(false) }
    val picked = viewModel.pickedExercises()
    val weightUnit = if (unitPreference == "imperial") "lb" else "kg"

    Column(
        modifier = Modifier.fillMaxSize().background(BackgroundDark).padding(16.dp)
    ) {
        // ---- Fixed header: title, date, muscle chips ----
        Text("Log a workout", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
        Spacer(modifier = Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Date", color = TextGray, fontFamily = myCustomFont, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                modifier = Modifier
                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(displayDate(state.dateMillis), color = AccentYellow, fontFamily = myCustomFont, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Future: a search bar would sit here, above the chips.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.muscles.forEach { m ->
                val n = state.exercisesByMuscle[m].orEmpty().count { it.exercise_id in state.pickedIds }
                MuscleTab(label = m, count = n, active = m in state.activeMuscles) { viewModel.toggleMuscleTab(m) }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---- Load-error / retry (shown when the exercise library failed to load) ----
        val loadError = state.uiState as? ManualLogUiState.Error
        if (loadError != null && state.muscles.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    loadError.message,
                    color = ErrorRed,
                    fontFamily = myCustomFont,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                SecondaryButton(text = "Try again", onClick = { viewModel.loadLibrary() })
            }
        }

        // ---- Top pane: selection (scrolls on its own) ----
        Text("Pick exercises", fontSize = 13.sp, color = TextGray, fontFamily = myCustomFont)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.activeMuscles.isEmpty()) {
                item {
                    Text("Tap muscle groups above to show their exercises.", color = TextGray, fontFamily = myCustomFont, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                state.muscles.filter { it in state.activeMuscles }.forEach { m ->
                    item(key = "hdr_$m") {
                        Text(m, fontSize = 15.sp, color = AccentYellow, fontWeight = FontWeight.Bold, fontFamily = myCustomFont, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
                    }
                    items(state.exercisesByMuscle[m].orEmpty(), key = { "pick_${m}_${it.exercise_id}" }) { ex ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleExercise(ex.exercise_id) }.padding(vertical = 6.dp)
                        ) {
                            CheckboxBox(checked = ex.exercise_id in state.pickedIds)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(ex.exercise_name, color = Color.White, fontFamily = myCustomFont, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        Divider(color = Color(0xFF333333), modifier = Modifier.padding(vertical = 8.dp))

        // ---- Bottom pane: your workout + inputs (scrolls on its own) ----
        Text("Your workout (${picked.size})", fontSize = 13.sp, color = TextGray, fontFamily = myCustomFont)
        Spacer(modifier = Modifier.height(4.dp))
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (picked.isEmpty()) {
                item {
                    Text("No exercises selected yet.", color = TextGray, fontFamily = myCustomFont, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
            } else {
                items(picked, key = { "log_${it.exercise_id}" }) { ex ->
                    Column(
                        modifier = Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(10.dp)).padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ex.exercise_muscle_group, color = AccentYellow, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = myCustomFont)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(ex.exercise_name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = myCustomFont, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InlineField("Weight", state.weightById[ex.exercise_id] ?: "", { viewModel.setWeight(ex.exercise_id, it) }, decimal = true, unit = weightUnit, modifier = Modifier.weight(1.3f))
                            InlineField("Sets", state.setsById[ex.exercise_id] ?: "", { viewModel.setSets(ex.exercise_id, it) }, modifier = Modifier.weight(1f))
                            InlineField("Reps", state.repsById[ex.exercise_id] ?: "", { viewModel.setReps(ex.exercise_id, it) }, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ---- Fixed footer: error + submit ----
        val ui = state.uiState
        if (ui is ManualLogUiState.Error) {
            Text(ui.message, color = ErrorRed, fontFamily = myCustomFont, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryButton(
            text = if (ui == ManualLogUiState.Submitting) "Logging…" else "Log workout",
            onClick = { viewModel.submit(onLogged) },
            enabled = ui != ManualLogUiState.Submitting,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= state.maxDateMillis
            }
        )
        val dpColors = DatePickerDefaults.colors(
            containerColor = SurfaceDark,
            titleContentColor = Color.White,
            headlineContentColor = Color.White,
            weekdayContentColor = TextGray,
            yearContentColor = Color.White,
            currentYearContentColor = AccentYellow,
            selectedYearContainerColor = AccentYellow,
            selectedYearContentColor = Color.Black,
            dayContentColor = Color.White,
            selectedDayContainerColor = AccentYellow,
            selectedDayContentColor = Color.Black,
            todayContentColor = AccentYellow,
            todayDateBorderColor = AccentYellow
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            colors = dpColors,
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { viewModel.setDate(it) }
                    showDatePicker = false
                }) { Text("OK", color = AccentYellow, fontFamily = myCustomFont) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = TextGray, fontFamily = myCustomFont) }
            }
        ) {
            DatePicker(state = dpState, colors = dpColors)
        }
    }
}

private fun displayDate(millis: Long): String {
    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    c.timeInMillis = millis
    val d = c.get(Calendar.DAY_OF_MONTH)
    val m = c.get(Calendar.MONTH) + 1
    val y = c.get(Calendar.YEAR)
    return "%02d-%02d-%04d".format(d, m, y)
}


@Composable
private fun CheckboxBox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .border(1.5.dp, if (checked) AccentYellow else Color.Gray, RoundedCornerShape(4.dp))
            .background(if (checked) AccentYellow else Color.Transparent, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (checked) Text("✓", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

// Single-line field: label, a compact box, and an optional trailing unit (kg/lb).
@Composable
private fun InlineField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    decimal: Boolean = false,
    unit: String? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontFamily = myCustomFont)
        Spacer(modifier = Modifier.width(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontFamily = myCustomFont, fontSize = 15.sp),
            cursorBrush = SolidColor(AccentYellow),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            modifier = Modifier.weight(1f).border(1.dp, Color(0xFF444444), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 7.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text("-", color = Color(0xFF666666), fontFamily = myCustomFont, fontSize = 15.sp)
                inner()
            }
        )
        if (unit != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(unit, color = Color.Gray, fontSize = 12.sp, fontFamily = myCustomFont)
        }
    }
}