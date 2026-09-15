package com.example.fitjournal_capstone_leandro.ui.shared

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.AccentYellow
import com.example.fitjournal_capstone_leandro.ui.theme.TextGray
import com.example.fitjournal_capstone_leandro.ui.theme.ErrorRed
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

/**
 * Shared button system for the app. Four roles, one shape:
 *
 *  - PrimaryButton   : solid yellow, black text            → the main action (Save, Done, Log)
 *  - SecondaryButton : outlined yellow, yellow text        → secondary action (Edit, Add, Create)
 *  - MutedButton     : outlined grey, grey text            → dismiss / cancel
 *  - DangerButton    : outlined red, red text              → destructive action (delete)
 *  - ChipToggle      : two-state selector (day chips, tabs)
 *
 * All use RoundedCornerShape(10.dp) — a rectangle with slightly rounded corners.
 *
 * Colours live here for now; centralising the whole palette into ui/theme is a
 * later step. Note: buttons that sit ON the yellow sticky notes (dashboard) are a
 * deliberate exception (black ink on yellow) and don't use these components.
 */

private val ButtonShape = RoundedCornerShape(10.dp)

/** Solid yellow — the primary action on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentYellow,
            contentColor = Color.Black,
            disabledContainerColor = AccentYellow.copy(alpha = 0.4f)
        )
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = myCustomFont)
    }
}

/** Outlined yellow — a secondary action. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = ButtonShape,
        border = BorderStroke(1.5.dp, AccentYellow),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentYellow)
    ) {
        Text(text, fontSize = 15.sp, fontFamily = myCustomFont)
    }
}

/** Outlined grey — a quiet dismiss / cancel action. */
@Composable
fun MutedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = ButtonShape,
        border = BorderStroke(1.5.dp, TextGray),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
    ) {
        Text(text, fontSize = 15.sp, fontFamily = myCustomFont)
    }
}

/** Outlined red — a destructive action (delete). */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(44.dp),
        shape = ButtonShape,
        border = BorderStroke(2.dp, ErrorRed),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, fontFamily = myCustomFont)
    }
}

/**
 * Two-state selector chip (day tabs, login/register toggle):
 *  selected   → yellow border + yellow-tinted fill + yellow text
 *  unselected → white border + transparent + white text
 */
@Composable
fun ChipToggle(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) AccentYellow else Color.White
    val fillColor = if (selected) AccentYellow.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (selected) AccentYellow else Color.White
    Box(
        modifier = modifier
            .height(40.dp)
            .border(1.5.dp, borderColor, ButtonShape)
            .background(fillColor, ButtonShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            fontFamily = myCustomFont
        )
    }
}

/**
 * Muscle-group pill with a count badge (used by the manual-log picker, the
 * routine editor's manual picker, etc.). A rounded pill: active = yellow border +
 * yellow-tinted fill + yellow text; inactive = grey border + white text. The count
 * turns yellow once > 0.
 */
@Composable
fun MuscleTab(
    label: String,
    count: Int,
    active: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .border(1.5.dp, if (active) AccentYellow else androidx.compose.ui.graphics.Color.Gray, RoundedCornerShape(16.dp))
            .background(if (active) AccentYellow.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = if (active) AccentYellow else androidx.compose.ui.graphics.Color.White, fontFamily = myCustomFont, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            "$count",
            color = if (count > 0) AccentYellow else androidx.compose.ui.graphics.Color.Gray,
            fontFamily = myCustomFont, fontSize = 13.sp, fontWeight = FontWeight.Bold
        )
    }
}