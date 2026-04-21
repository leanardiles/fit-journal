package com.example.fitjournal_capstone_leandro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.R


// Sealed class for navigation items
sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector? = null,
    val logoRes: Int? = null,
    val title: String
) {
    object Timer : BottomNavItem("timer", Icons.Filled.Timer, null, "Timer")
    object Calendar : BottomNavItem("calendar", Icons.Filled.DateRange, null, "Calendar")
    object Home : BottomNavItem("home", null, R.drawable.logo_alone, "Home")
    object Exercises : BottomNavItem("exercises", Icons.Filled.FitnessCenter, null, "Exercises")
    object Settings : BottomNavItem("settings", Icons.Filled.Settings, null, "Settings")
}

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color(0xFF1B1B1E)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            BottomNavItemView(
                item = item,
                isSelected = currentRoute == item.route,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) Color(0xFFFFEB3B) else Color.Gray

    // Special handling for Home (logo)
    if (item.logoRes != null) {
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = item.logoRes),
                contentDescription = item.title,
                modifier = Modifier.size(45.dp)
            )
        }
    } else {
        // Regular icon items
        Column(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item.icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = item.title,
                    modifier = Modifier.size(24.dp),
                    tint = tint
                )
            }

            Text(
                text = item.title,
                color = tint,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}