package com.example.unifiedapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.unifiedapp.navigation.Screen
import com.example.unifiedapp.navigation.UnifiedNavGraph
import com.example.unifiedapp.theme.UnifiedAppTheme

// testgit123
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UnifiedAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    UnifiedNavGraph(
                        navController = navController,
                        startDestination = Screen.LAUNCHER   // ✅ use constant
                    )
                }
            }
        }
    }
}