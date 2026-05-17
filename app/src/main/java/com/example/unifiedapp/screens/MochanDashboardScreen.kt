package com.example.unifiedapp.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.unifiedapp.R
import com.example.unifiedapp.ui.theme.*

@Composable
fun MochanDashboardScreen(
    navController: NavController
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MochanGradient)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mochan_logo),
                        contentDescription = "Logo",
                        modifier = Modifier.size(50.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        "Mochan",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MochanPurplePrimary
                    )
                    Text(
                        "Depression Assessment",
                        fontSize = 14.sp,
                        color = MochanTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Start Assessment Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        navController.navigate("mochan_consent")
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MochanPurplePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Start",
                            tint = MochanPurplePrimary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Start Depression Assessment",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MochanPurplePrimary
                    )
                    Text(
                        "PHQ-9 Questionnaire • 9 questions",
                        fontSize = 14.sp,
                        color = MochanTextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "• Camera required for AI analysis",
                        fontSize = 12.sp,
                        color = MochanTextSecondary
                    )
                    Text(
                        "• Takes 2-3 minutes",
                        fontSize = 12.sp,
                        color = MochanTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}