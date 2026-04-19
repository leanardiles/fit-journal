package com.example.fitjournal_capstone_leandro.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    // State to hold the selected date
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    // Format the selected date for display (using UTC to avoid timezone issues)
    val selectedDate = datePickerState.selectedDateMillis?.let { millis ->
        val formatter = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        formatter.timeZone = TimeZone.getTimeZone("UTC")  // ← ADD THIS LINE!
        formatter.format(Date(millis))
    } ?: "No date selected"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1E))
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Calendar",
            fontSize = 32.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display selected date
        Text(
            text = "Selected Date:",
            fontSize = 18.sp,
            color = Color.Gray,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = selectedDate,
            fontSize = 20.sp,
            color = Color(0xFFFFEB3B),
            fontWeight = FontWeight.Bold,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(24.dp))

        // DatePicker with custom colors
        DatePicker(
            state = datePickerState,
            modifier = Modifier.fillMaxWidth(),
            colors = DatePickerDefaults.colors(
                containerColor = Color(0xFF2A2A2E),
                titleContentColor = Color.White,
                headlineContentColor = Color.White,
                weekdayContentColor = Color.Gray,
                subheadContentColor = Color.White,
                yearContentColor = Color.White,
                currentYearContentColor = Color(0xFFFFEB3B),
                selectedYearContentColor = Color.Black,
                selectedYearContainerColor = Color(0xFFFFEB3B),
                dayContentColor = Color.White,
                disabledDayContentColor = Color.Gray,
                selectedDayContentColor = Color.Black,
                disabledSelectedDayContentColor = Color.Gray,
                selectedDayContainerColor = Color(0xFFFFEB3B),
                disabledSelectedDayContainerColor = Color.Gray,
                todayContentColor = Color(0xFFFFEB3B),
                todayDateBorderColor = Color(0xFFFFEB3B),
                dayInSelectionRangeContentColor = Color.White,
                dayInSelectionRangeContainerColor = Color(0xFF3A3A3E)
            )
        )
    }
}