package com.breaktobreak.dailynews

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.breaktobreak.dailynews.ui.main.MainScreen
import com.breaktobreak.dailynews.ui.theme.DailynewsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailynewsTheme {
                MainScreen()
            }
        }
    }
}