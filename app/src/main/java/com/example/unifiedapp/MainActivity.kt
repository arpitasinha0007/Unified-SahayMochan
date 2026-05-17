package com.example.unifiedapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.navigation
import androidx.navigation.compose.navigation
import com.example.unifiedapp.navigation.mochanNavGraph
import com.example.unifiedapp.navigation.MochanScreen
import com.example.unifiedapp.navigation.sahayNavGraph
import com.example.unifiedapp.navigation.SahayScreen
import com.example.unifiedapp.screens.LauncherScreen
import com.example.unifiedapp.theme.UnifiedAppTheme

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

                    NavHost(
                        navController = navController,
                        startDestination = "launcher"
                    ) {
                        composable("launcher") {
                            LauncherScreen(navController = navController)
                        }

                        // Sahay (Anxiety) Navigation Group
                        navigation(
                            startDestination = SahayScreen.DASHBOARD,
                            route = "sahay_graph"
                        ) {
                            sahayNavGraph(navController)
                        }

                        // Mochan (Depression) Navigation Group
                        navigation(
                            startDestination = MochanScreen.DASHBOARD,
                            route = "mochan_graph"
                        ) {
                            mochanNavGraph(navController)
                        }
                    }
                }
            }
        }
    }
}