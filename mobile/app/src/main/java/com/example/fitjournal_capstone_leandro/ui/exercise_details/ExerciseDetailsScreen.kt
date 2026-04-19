package com.example.fitjournal_capstone_leandro.ui.exercise_details

import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont

@Composable
fun ExerciseDetailsScreen(
    viewModel: ExerciseDetailsViewModel,
) {
    val exercise = viewModel.selectedExercise
    val context = LocalContext.current

    // If exercise is null, show error screen
    if (exercise == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1B1B1E))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "ERROR: No Exercise Selected",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "The exercise data is missing. This shouldn't happen!",
                color = Color.White,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

        }
        return
    }

    LaunchedEffect(Unit) {
        val exercise = viewModel.selectedExercise
        Log.d("GIF_DEBUG", "Exercise name: ${exercise?.name}")
        Log.d("GIF_DEBUG", "GIF URL: ${exercise?.gifUrl}")
    }

    // Normal ExerciseDetailsScreen (exercise exists)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1E))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // Exercise Name
        Text(
            text = exercise.name.replaceFirstChar { it.uppercase() },
            fontSize = 28.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(16.dp))

        // GIF Image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(exercise.gifUrl)
                .decoderFactory(
                    if (Build.VERSION.SDK_INT >= 28) {
                        ImageDecoderDecoder.Factory()
                    } else {
                        GifDecoder.Factory()
                    }
                )
                .crossfade(true)
                .build(),
            contentDescription = exercise.name,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Equipment
        Text(
            text = "Equipment: ${exercise.equipment}",
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Body Part
        Text(
            text = "Body Part: ${exercise.bodyPart}",
            color = Color.White,
            fontSize = 18.sp,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions
        Text(
            text = "Instructions:",
            fontSize = 22.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontFamily = myCustomFont
        )

        Spacer(modifier = Modifier.height(8.dp))

        exercise.instructions.forEachIndexed { index, instruction ->
            Text(
                text = "${index + 1}. $instruction",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = myCustomFont
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}