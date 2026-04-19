package com.example.fitjournal_capstone_leandro.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitjournal_capstone_leandro.ui.theme.myCustomFont
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import com.example.fitjournal_capstone_leandro.R


@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMuscleGroupClick: ((String) -> Unit)? = null
) {
    // Observe the MVI state
    val state by viewModel.state.collectAsState()

    // Fetch muscle groups when screen loads
    LaunchedEffect(Unit) {
        viewModel.processAction(HomeScreenAction.FetchMuscleGroups)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B1B1E))
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))


        // Placeholder for Dashboard - Future stats will go here

        Spacer(modifier = Modifier.height(16.dp))

        // Render UI based on current UiState
        when (state.uiState) {
            is HomeUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFEB3B))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading muscle groups...", color = Color.White)
                }
            }

            is HomeUiState.Success -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Dashboard",
                        fontSize = 28.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = myCustomFont
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Image(
                        painter = painterResource(id = R.drawable.logo_alone),
                        contentDescription = "FitJournal Logo",
                        modifier = Modifier.size(200.dp).alpha(0.2f)
                    )
                }
            }

            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Error",
                        fontSize = 24.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        state.errorMessage ?: "Unknown error",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            // Trigger retry action
                            viewModel.processAction(HomeScreenAction.FetchMuscleGroups)
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }

            is HomeUiState.Empty -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "📭 No Muscle Groups",
                        fontSize = 24.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No muscle groups found", color = Color.Gray)
                }
            }
        }
    }
}