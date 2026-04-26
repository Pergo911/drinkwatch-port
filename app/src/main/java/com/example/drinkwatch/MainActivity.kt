package com.example.drinkwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.drinkwatch.ui.navigation.AppNavHost
import com.example.drinkwatch.ui.theme.DrinkWatchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DrinkWatchTheme {
                AppNavHost()
            }
        }
    }
}