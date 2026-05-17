package com.example.unifiedapp.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifiedapp.ui.views.UserPreferences
import kotlinx.coroutines.launch
import com.example.unifiedapp.theme.*

@Composable
fun ConsentScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    // Consent states
    var dataConsentGiven by remember { mutableStateOf(false) }
    var cameraConsentGiven by remember { mutableStateOf(false) }
    var disclaimerAcknowledged by remember { mutableStateOf(false) }

    // Popup states
    var showTermsPopup by remember { mutableStateOf(false) }
    var showPrivacyPopup by remember { mutableStateOf(false) }

    val allConsentsGiven = dataConsentGiven && cameraConsentGiven && disclaimerAcknowledged

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SageLight, BackgroundSage)
                )
            )
    ) {
        // Terms Popup
        if (showTermsPopup) {
            TermsAndConditionsPopup(
                onDismiss = { showTermsPopup = false }
            )
        }

        // Privacy Popup
        if (showPrivacyPopup) {
            PrivacyPolicyPopup(
                onDismiss = { showPrivacyPopup = false }
            )
        }

        // Close button
        IconButton(
            onClick = onDecline,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 16.dp)
                .background(Color.White.copy(alpha = 0.5f), CircleShape)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = SageDark,
                modifier = Modifier.size(20.dp)
            )
        }

        // Full screen card
        Card(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header with top padding to account for close button
                Spacer(modifier = Modifier.height(40.dp))

                // Header Section - Perfectly centered
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Icon with sage background
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(SagePrimary, SageAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        "Before we begin",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SageDark,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Please review and provide your consent",
                        fontSize = 14.sp,
                        color = SageDark.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // IMPORTANT MEDICAL DISCLAIMER - High visibility
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftApricot.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, SoftCoral),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = SoftCoral,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "Important Medical Disclaimer",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SageDark
                            )
                        }

                        Text(
                            "This app is a SCREENING TOOL ONLY and is NOT a medical diagnostic device. It is designed to provide helpful insights and wellness information, but should NOT be used as a substitute for professional medical advice, diagnosis, or treatment.",
                            fontSize = 14.sp,
                            color = SageDark,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Start
                        )

                        Text(
                            "If you are in crisis, experiencing severe symptoms, or need immediate help:",
                            fontSize = 13.sp,
                            color = SageDark,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            EmergencyChip("988", "Crisis Hotline")
                            EmergencyChip("911", "Emergency")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = disclaimerAcknowledged,
                                onCheckedChange = { disclaimerAcknowledged = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = SagePrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "I acknowledge and understand this disclaimer",
                                fontSize = 13.sp,
                                color = SageDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Camera Consent Card
                ConsentCard(
                    title = "Camera Access",
                    icon = Icons.Default.Videocam,
                    iconColor = MistyBlue,
                    isChecked = cameraConsentGiven,
                    onCheckedChange = { cameraConsentGiven = it },
                    description = "Allow camera access for facial expression analysis during screening sessions. The camera is only used when you actively start a session, and no raw images or videos are stored without your explicit request."
                )

                // Data Consent Card
                ConsentCard(
                    title = "Data Collection & Analysis",
                    icon = Icons.Default.Analytics,
                    iconColor = SageMint,
                    isChecked = dataConsentGiven,
                    onCheckedChange = { dataConsentGiven = it },
                    description = "Allow anonymous collection of your assessment responses and facial expression data to provide personalized insights and improve our AI models. Your data is encrypted and never sold to third parties."
                )

                // Legal Links - Perfectly centered
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "By continuing, you agree to our",
                        fontSize = 12.sp,
                        color = SageDark.copy(alpha = 0.6f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showTermsPopup = true },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                "Terms & Conditions",
                                fontSize = 12.sp,
                                color = SagePrimary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        }

                        Text(
                            "and",
                            fontSize = 12.sp,
                            color = SageDark.copy(alpha = 0.6f)
                        )

                        TextButton(
                            onClick = { showPrivacyPopup = true },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                "Privacy Policy",
                                fontSize = 12.sp,
                                color = SagePrimary,
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                }

                // Consent Summary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = PaleMint,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = SagePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "You can withdraw consent at any time from the app settings",
                            fontSize = 11.sp,
                            color = SageDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Continue Button
                Button(
                    onClick = onAccept,
                    enabled = allConsentsGiven,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allConsentsGiven) SagePrimary else SagePrimary.copy(alpha = 0.3f),
                        disabledContainerColor = SagePrimary.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        "Continue to App",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                // Progress indicator for consents
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConsentDot(active = disclaimerAcknowledged)
                    Spacer(modifier = Modifier.width(4.dp))
                    ConsentDot(active = cameraConsentGiven)
                    Spacer(modifier = Modifier.width(4.dp))
                    ConsentDot(active = dataConsentGiven)
                }

                Text(
                    text = "You can stop anytime.",
                    fontSize = 12.sp,
                    color = SageDark.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Extra bottom padding for better scrolling
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ConsentCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = WhiteSoft),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isChecked) SagePrimary else SageMedium.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SageDark
                    )

                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = onCheckedChange,
                        colors = CheckboxDefaults.colors(
                            checkedColor = SagePrimary,
                            uncheckedColor = SageDark.copy(alpha = 0.3f)
                        )
                    )
                }

                Text(
                    description,
                    fontSize = 12.sp,
                    color = SageDark.copy(alpha = 0.7f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun EmergencyChip(text: String, label: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SoftCoral.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, SoftCoral),
        modifier = Modifier.wrapContentWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = SoftCoral
            )
            Text(
                label,
                fontSize = 10.sp,
                color = SageDark
            )
        }
    }
}

@Composable
fun ConsentDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (active) SagePrimary else SageMedium.copy(alpha = 0.3f))
    )
}