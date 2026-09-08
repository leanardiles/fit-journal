package com.example.fitjournal_capstone_leandro.ui.routine

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.fitjournal_capstone_leandro.navigation.Routes
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

private val AccentYellow = Color(0xFFFFEB3B)
private val BackgroundDark = Color(0xFF1B1B1E)
private val SurfaceDark = Color(0xFF2C2C2E)
private val TextGray = Color(0xFF8E8E93)

@Composable
fun RoutineScreen(viewModel: RoutineViewModel, navController: NavHostController) {
    val state by viewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        when (state.uiState) {
            is RoutineUiState.Loading -> {
                CircularProgressIndicator(color = AccentYellow, modifier = Modifier.align(Alignment.Center))
            }

            is RoutineUiState.NoRoutine -> {
                NoRoutineContent(onSelectDays = { viewModel.selectDaysPerWeek(it) })
            }

            is RoutineUiState.Editing -> {
                EditingContent(
                    state = state,
                    muscleGroups = viewModel.muscleGroups,
                    onSelectDays = { viewModel.selectDaysPerWeek(it) },
                    onSetType = { day, type -> viewModel.setDayType(day, type) },
                    onSetName = { day, name -> viewModel.setDayName(day, name) },
                    onToggleMuscle = { day, muscle -> viewModel.toggleMuscleGroup(day, muscle) },
                    onOpenPicker = { day -> navController.navigate(Routes.exercisePicker(day)) },
                    onSave = { viewModel.saveRoutine() },
                    onCancel = { viewModel.cancelEditing() }
                )
            }

            is RoutineUiState.Success -> {
                ViewRoutineContent(state = state, onEdit = { viewModel.startEditing() })
            }

            is RoutineUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = (state.uiState as RoutineUiState.Error).message,
                        color = Color(0xFFFF453A),
                        fontFamily = myCustomFont,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadRoutine() }, colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)) {
                        Text("Retry", color = Color.Black, fontFamily = myCustomFont)
                    }
                }
            }
        }
    }
}

// ─── NO ROUTINE ───────────────────────────────────────────────────────────────

@Composable
private fun NoRoutineContent(onSelectDays: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Your Routine", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "How many days per week\ndo you want to train?",
            fontSize = 20.sp, color = Color.White, fontFamily = myCustomFont,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        DaySelector(selectedDays = 0, onSelectDays = onSelectDays)
    }
}

// ─── EDITING ──────────────────────────────────────────────────────────────────

@Composable
private fun EditingContent(
    state: RoutineScreenState,
    muscleGroups: List<String>,
    onSelectDays: (Int) -> Unit,
    onSetType: (Int, String) -> Unit,
    onSetName: (Int, String) -> Unit,
    onToggleMuscle: (Int, String) -> Unit,
    onOpenPicker: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Your Routine", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "How many days per week\ndo you want to train?",
                fontSize = 18.sp, color = Color.White, fontFamily = myCustomFont,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            DaySelector(selectedDays = state.selectedDays, onSelectDays = onSelectDays)
            Spacer(modifier = Modifier.height(32.dp))
            Text("Set up each day:", fontSize = 18.sp, color = Color.White, fontFamily = myCustomFont)
            Spacer(modifier = Modifier.height(16.dp))
        }

        items((1..state.selectedDays).toList()) { day ->
            DayCard(
                day = day,
                dayEdit = state.editingDays[day] ?: DayEdit(),
                muscleGroups = muscleGroups,
                onSetType = { type -> onSetType(day, type) },
                onSetName = { name -> onSetName(day, name) },
                onToggleMuscle = { muscle -> onToggleMuscle(day, muscle) },
                onOpenPicker = { onOpenPicker(day) }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.padding(end = 12.dp)) {
                    Text("Cancel", color = TextGray, fontFamily = myCustomFont)
                }
                Button(onClick = onSave, colors = ButtonDefaults.buttonColors(containerColor = AccentYellow)) {
                    Text("Save Routine", color = Color.Black, fontFamily = myCustomFont, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ─── VIEW ROUTINE ─────────────────────────────────────────────────────────────

@Composable
private fun ViewRoutineContent(state: RoutineScreenState, onEdit: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Your Routine", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Training ${state.daysPerWeek} days per week", fontSize = 16.sp, color = TextGray, fontFamily = myCustomFont)
        Spacer(modifier = Modifier.height(24.dp))

        (1..state.daysPerWeek).forEach { day ->
            val muscles = state.routineDays[day.toString()] ?: emptyList()
            val dayName = state.routineDayNames[day.toString()]
            val typeLabel = if (state.routineDayTypes[day.toString()] == "manual") "Manual" else "By Muscle"
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                // Line 1: day number (yellow) + optional name (white), both bold
                Row {
                    Text("Day $day", fontSize = 16.sp, color = AccentYellow, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
                    if (!dayName.isNullOrBlank()) {
                        Text(" — $dayName", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Line 2: type + muscles
                Row {
                    Text("$typeLabel · ", fontSize = 14.sp, color = TextGray, fontFamily = myCustomFont)
                    Text(
                        text = if (muscles.isEmpty()) "Rest day" else muscles.joinToString(", "),
                        fontSize = 14.sp, color = Color.White, fontFamily = myCustomFont
                    )
                }
            }
            Divider(color = SurfaceDark)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onEdit, colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark)) {
            Text("Edit Routine", color = Color.White, fontFamily = myCustomFont)
        }
    }
}

// ─── EXERCISE PICKER ROUTE (branches by day type) ─────────────────────────────

@Composable
fun RoutineExercisePickerRoute(
    viewModel: RoutineViewModel,
    day: Int,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val type = state.editingDays[day]?.type ?: "per_muscle"
    if (type == "manual") {
        ManualPickerScreen(viewModel = viewModel, day = day, onDone = onDone, onCancel = onCancel)
    } else {
        PerMusclePickerScreen(viewModel = viewModel, day = day, onDone = onDone, onCancel = onCancel)
    }
}

// ─── PER-MUSCLE PICKER ────────────────────────────────────────────────────────

@Composable
private fun PerMusclePickerScreen(
    viewModel: RoutineViewModel,
    day: Int,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val daySelection = (state.editingDays[day] ?: DayEdit()).perMuscle
    val exercisesByMuscle = state.exercisesByMuscle

    LaunchedEffect(day) { viewModel.beginPickerEdit(day) }
    BackHandler { onCancel() }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Day $day — choose exercises", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tick the exercises to include, and set how many to draw each session.", fontSize = 13.sp, color = TextGray, fontFamily = myCustomFont)
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (daySelection.isEmpty()) {
                    item { Text("No muscle groups selected for this day yet.", color = TextGray, fontFamily = myCustomFont, fontSize = 14.sp) }
                }
                daySelection.forEach { (muscle, sel) ->
                    item(key = "hdr_$muscle") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp)) {
                            Text(muscle, fontSize = 16.sp, color = AccentYellow, fontWeight = FontWeight.Bold, fontFamily = myCustomFont, modifier = Modifier.weight(1f))
                            Text("draw", color = TextGray, fontFamily = myCustomFont, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            StepperButton(label = "-") { viewModel.changeCount(day, muscle, -1) }
                            Text("${sel.count}", color = Color.White, fontFamily = myCustomFont, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 12.dp))
                            StepperButton(label = "+") { viewModel.changeCount(day, muscle, 1) }
                        }
                    }
                    val exercises = exercisesByMuscle[muscle].orEmpty()
                    if (exercises.isEmpty()) {
                        item(key = "empty_$muscle") {
                            Text("No $muscle exercises in your library.", color = TextGray, fontFamily = myCustomFont, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp))
                        }
                    } else {
                        items(exercises, key = { "ex_${muscle}_${it.exercise_id}" }) { ex ->
                            val checked = sel.exerciseIds.contains(ex.exercise_id)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleExercise(day, muscle, ex.exercise_id) }.padding(vertical = 6.dp)
                            ) {
                                CheckboxBox(checked = checked)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(ex.exercise_name, color = Color.White, fontFamily = myCustomFont, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            PickerButtons(onCancel = onCancel, onDone = onDone)
        }
    }
}

// ─── MANUAL PICKER ────────────────────────────────────────────────────────────

@Composable
private fun ManualPickerScreen(
    viewModel: RoutineViewModel,
    day: Int,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val de = state.editingDays[day] ?: DayEdit()
    val exercisesByMuscle = state.exercisesByMuscle

    LaunchedEffect(day) { viewModel.beginPickerEdit(day) }
    BackHandler { onCancel() }

    val muscles = viewModel.muscleGroups.filter { !exercisesByMuscle[it].isNullOrEmpty() }.sorted()
    val active = de.manualActiveMuscles
    val checked = de.manualExerciseIds

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Day $day — choose exercises", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tick the exercises to include in this manual day.", fontSize = 13.sp, color = TextGray, fontFamily = myCustomFont)
            Spacer(modifier = Modifier.height(12.dp))

            // Muscle tabs — multi-select filter; count badge = checked exercises for that muscle.
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                muscles.forEach { m ->
                    val n = exercisesByMuscle[m].orEmpty().count { checked.contains(it.exercise_id) }
                    MuscleTab(label = m, count = n, active = active.contains(m)) { viewModel.toggleManualMuscleTab(day, m) }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                if (active.isEmpty()) {
                    item {
                        Text("Tap muscle groups above to show their exercises.", color = TextGray, fontFamily = myCustomFont, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                } else {
                    muscles.filter { active.contains(it) }.forEach { m ->
                        item(key = "mhdr_$m") {
                            Text(m, fontSize = 16.sp, color = AccentYellow, fontWeight = FontWeight.Bold, fontFamily = myCustomFont, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                        }
                        items(exercisesByMuscle[m].orEmpty(), key = { "mex_${m}_${it.exercise_id}" }) { ex ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleManualExercise(day, ex.exercise_id) }.padding(vertical = 6.dp)
                            ) {
                                CheckboxBox(checked = checked.contains(ex.exercise_id))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(ex.exercise_name, color = Color.White, fontFamily = myCustomFont, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("${checked.size} exercises picked", color = TextGray, fontFamily = myCustomFont, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(8.dp))
            PickerButtons(onCancel = onCancel, onDone = onDone)
        }
    }
}

// ─── SHARED COMPOSABLES ───────────────────────────────────────────────────────

@Composable
private fun PickerButtons(onCancel: () -> Unit, onDone: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
            Text("Cancel", color = TextGray, fontFamily = myCustomFont)
        }
        Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = AccentYellow), modifier = Modifier.weight(1f)) {
            Text("Done", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
        }
    }
}

@Composable
private fun DaySelector(selectedDays: Int, onSelectDays: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..7).forEach { day ->
            val isSelected = day == selectedDays
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .border(2.dp, if (isSelected) AccentYellow else Color.White, RoundedCornerShape(8.dp))
                    .background(if (isSelected) AccentYellow.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onSelectDays(day) },
                contentAlignment = Alignment.Center
            ) {
                Text("$day", color = if (isSelected) AccentYellow else Color.White, fontWeight = FontWeight.Bold, fontFamily = myCustomFont, fontSize = 18.sp)
            }
        }
    }
}

// Text-style type toggle: selected option bold yellow, other muted grey.
@Composable
private fun TypeToggle(current: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Type:", color = TextGray, fontFamily = myCustomFont, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(8.dp))
        val byMuscle = current == "per_muscle"
        Text(
            text = "By Muscle",
            color = if (byMuscle) AccentYellow else TextGray,
            fontWeight = if (byMuscle) FontWeight.Bold else FontWeight.Normal,
            fontFamily = myCustomFont,
            fontSize = 16.sp,
            modifier = Modifier.clickable { if (!byMuscle) onSelect("per_muscle") }.padding(vertical = 4.dp, horizontal = 2.dp)
        )
        Text("/", color = Color(0xFF555555), fontFamily = myCustomFont, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
        val manual = current == "manual"
        Text(
            text = "Manual",
            color = if (manual) AccentYellow else TextGray,
            fontWeight = if (manual) FontWeight.Bold else FontWeight.Normal,
            fontFamily = myCustomFont,
            fontSize = 16.sp,
            modifier = Modifier.clickable { if (!manual) onSelect("manual") }.padding(vertical = 4.dp, horizontal = 2.dp)
        )
    }
}

@Composable
private fun DayCard(
    day: Int,
    dayEdit: DayEdit,
    muscleGroups: List<String>,
    onSetType: (String) -> Unit,
    onSetName: (String) -> Unit,
    onToggleMuscle: (String) -> Unit,
    onOpenPicker: () -> Unit
) {
    var pendingType by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(12.dp)).padding(16.dp)) {
        // Row 1: day number + inline name field (now roomy — toggle moved below)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Day $day", fontSize = 18.sp, color = AccentYellow, fontWeight = FontWeight.Bold, fontFamily = myCustomFont)
            BasicTextField(
                value = dayEdit.name,
                onValueChange = { if (it.length <= 25) onSetName(it) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontFamily = myCustomFont, fontSize = 15.sp),
                cursorBrush = SolidColor(AccentYellow),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp)
                    .background(Color(0x22FFFFFF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                decorationBox = { inner ->
                    Box {
                        if (dayEdit.name.isEmpty()) {
                            Text("name (optional)", color = Color(0xFF888888), fontFamily = myCustomFont, fontSize = 15.sp)
                        }
                        inner()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: type toggle, centered
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TypeToggle(current = dayEdit.type) { newType ->
                val hasContent = dayEdit.perMuscle.isNotEmpty() || dayEdit.manualExerciseIds.isNotEmpty()
                if (hasContent) pendingType = newType else onSetType(newType)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (dayEdit.type == "manual") {
            val n = dayEdit.manualExerciseIds.size
            Text(
                text = "Choose exercises ($n) ›",
                color = AccentYellow, fontFamily = myCustomFont, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().clickable { onOpenPicker() }.padding(vertical = 4.dp)
            )
        } else {
            val rows = muscleGroups.sorted().chunked(3)
            rows.forEach { rowMuscles ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowMuscles.forEach { muscle ->
                        val isSelected = dayEdit.perMuscle.containsKey(muscle)
                        OutlinedButton(
                            onClick = { onToggleMuscle(muscle) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isSelected) AccentYellow.copy(alpha = 0.2f) else Color.Transparent),
                            border = BorderStroke(1.5.dp, if (isSelected) AccentYellow else Color.Gray),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text(muscle, color = if (isSelected) AccentYellow else Color.White, fontFamily = myCustomFont, fontSize = 11.sp)
                        }
                    }
                    repeat(3 - rowMuscles.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (dayEdit.perMuscle.isNotEmpty()) {
                val n = dayEdit.perMuscle.values.sumOf { it.exerciseIds.size }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose exercises ($n) ›",
                    color = AccentYellow, fontFamily = myCustomFont, fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPicker() }.padding(vertical = 4.dp)
                )
            }
        }
    }

    if (pendingType != null) {
        AlertDialog(
            onDismissRequest = { pendingType = null },
            containerColor = SurfaceDark,
            title = { Text("Switch day type?", color = Color.White, fontFamily = myCustomFont) },
            text = { Text("This will clear Day $day's current exercises.", color = TextGray, fontFamily = myCustomFont) },
            confirmButton = {
                TextButton(onClick = { onSetType(pendingType!!); pendingType = null }) {
                    Text("Switch", color = AccentYellow, fontFamily = myCustomFont)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingType = null }) {
                    Text("Cancel", color = TextGray, fontFamily = myCustomFont)
                }
            }
        )
    }
}

@Composable
private fun MuscleTab(label: String, count: Int, active: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .border(1.5.dp, if (active) AccentYellow else Color.Gray, RoundedCornerShape(16.dp))
            .background(if (active) AccentYellow.copy(alpha = 0.15f) else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (active) AccentYellow else Color.White, fontFamily = myCustomFont, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text("$count", color = if (count > 0) AccentYellow else Color.Gray, fontFamily = myCustomFont, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(28.dp).border(1.dp, Color.Gray, RoundedCornerShape(6.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontFamily = myCustomFont, fontSize = 18.sp)
    }
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