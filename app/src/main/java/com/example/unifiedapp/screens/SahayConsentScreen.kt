package com.example.unifiedapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifiedapp.ui.theme.*

@Composable
fun SahayConsentScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var consentGiven by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SahayGradient)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SahaySageAccent.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = SahaySageAccent, modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Before You Begin",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = SahayCharcoal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Please review and accept the terms",
            fontSize = 14.sp,
            color = SahayMutedSlate
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Consent Info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Sahay uses your camera for facial analysis during assessments.",
                    fontSize = 14.sp,
                    color = SahayCharcoal
                )
                Text(
                    "• Camera used only during self-initiated sessions",
                    fontSize = 13.sp,
                    color = SahayMutedSlate
                )
                Text(
                    "• Data is encrypted and never sold",
                    fontSize = 13.sp,
                    color = SahayMutedSlate
                )
                Text(
                    "• Results are for informational purposes only",
                    fontSize = 13.sp,
                    color = SahayMutedSlate
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = consentGiven,
                onCheckedChange = { consentGiven = it },
                colors = CheckboxDefaults.colors(checkedColor = SahaySageAccent)
            )
            Text(
                "I understand and agree to the terms",
                fontSize = 14.sp,
                color = if (consentGiven) SahayCharcoal else SahayMutedSlate
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SahayMutedSlate)
            ) {
                Text("Decline")
            }

            Button(
                onClick = onAccept,
                enabled = consentGiven,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (consentGiven) SahaySageAccent else SahaySageAccent.copy(alpha = 0.3f)
                )
            ) {
                Text("Accept & Continue", color = Color.White)
            }
        }
    }
}