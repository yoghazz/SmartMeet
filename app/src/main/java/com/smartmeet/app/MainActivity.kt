package com.smartmeet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.smartmeet.app.ui.navigation.SmartMeetNavGraph
import com.smartmeet.app.ui.theme.SmartMeetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            SmartMeetTheme {
                SmartMeetNavGraph()
            }
        }
    }
}
