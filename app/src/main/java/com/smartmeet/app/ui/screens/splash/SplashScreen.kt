package com.smartmeet.app.ui.screens.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.smartmeet.app.R
import com.smartmeet.app.ui.screens.splash.viewmodel.SplashViewModel
import com.smartmeet.app.ui.theme.PrimaryBlue
import com.smartmeet.app.ui.theme.SecondaryBlue

@Composable
fun SplashScreen(
    onNavigateNext: (Boolean) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val loggedIn by viewModel.loggedIn.collectAsState()
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.splash_animation))

    LaunchedEffect(loggedIn) {
        loggedIn?.let(onNavigateNext)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryBlue, SecondaryBlue))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(200.dp)
            )
            Text("SmartMeet", color = MaterialTheme.colorScheme.onPrimary, fontSize = 34.sp)
            Text("AI Meeting Recorder", color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp)
        }
    }
}
