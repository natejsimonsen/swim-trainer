package com.swimtrainer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.swimtrainer.app.ui.SwimApp
import com.swimtrainer.app.ui.theme.SwimTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwimTrainerTheme {
                SwimApp()
            }
        }
    }
}
