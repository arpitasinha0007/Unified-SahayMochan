package com.example.unifiedapp.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.unifiedapp.utils.UserSessionHelper

// ============ LOCAL COLOR DEFINITIONS ============
private val LocalGradientStart = Color(0xFFFF385C)  // Bright pinkish-red
private val LocalGradientMid = Color(0xFFFF5E3A)    // Red-orange
private val LocalGradientEnd = Color(0xFFFF9345)    // Golden orange
private val LocalAccentGreen = Color(0xFF10B981)    // Success green

// Text colors
private val LocalColorTextMain = Color(0xFF1E293B)  // Slate 800
private val LocalColorTextSoft = Color(0xFF64748B)  // Slate 500
private val LocalColorTextMuted = Color(0xFF94A3B8) // Slate 400
private val LocalSurfaceWhite = Color.White
private val LocalColorBorder = Color(0xFFE5E7EB)    // Light gray border
private val LocalWarningYellow = Color(0xFFF59E0B)

@Composable
fun ConsentScreen(
    navController: NavController,
    onAccept: () -> Unit,
    onDecline: () -> Unit = {}
) {
    // Debug log to verify screen is loaded
    LaunchedEffect(Unit) {
        Log.d("ConsentScreen", "✅ ConsentScreen loaded successfully")
    }

    val context = LocalContext.current
    val session = UserSessionHelper.getUserData(context)
    val userName = session.name

    // State for popups
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTermsPopup by remember { mutableStateOf(false) }

    // Consent state
    var consentGiven by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            item {
                ConsentHeaderLocal(
                    userName = userName,
                    onBack = {
                        Log.d("ConsentScreen", "Back button clicked")
                        navController.popBackStack()
                    }
                )
            }

            // Main Content
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Welcome Card
                    WelcomeUserCardLocal(userName = userName)

                    // Information Card
                    InformationCardLocal()

                    PoliciesAndConsentCardLocal(
                        checked = consentGiven,
                        onCheckedChange = { consentGiven = it },
                        onTermsClick = {
                            Log.d("ConsentScreen", "Terms clicked")
                            showTermsPopup = true
                        },
                        onPrivacyClick = {
                            Log.d("ConsentScreen", "Privacy clicked")
                            showPrivacyPolicy = true
                        }
                    )

                    DisclaimerCardLocal()

                    Spacer(modifier = Modifier.height(8.dp))

                    // Accept Button
                    AcceptButtonLocal(
                        enabled = consentGiven,
                        onClick = {
                            Log.d("ConsentScreen", "Accept button clicked, consentGiven: $consentGiven")
                            if (consentGiven) {
                                onAccept()
                            }
                        }
                    )
                }
            }

            // Bottom padding
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }

        // Popups
        if (showPrivacyPolicy) {
            PrivacyPolicyPopup(
                onDismiss = {
                    Log.d("ConsentScreen", "Privacy popup dismissed")
                    showPrivacyPolicy = false
                }
            )
        }

        if (showTermsPopup) {
            TermsAndConditionsPopup(
                onDismiss = {
                    Log.d("ConsentScreen", "Terms popup dismissed")
                    showTermsPopup = false
                }
            )
        }
    }
}

@Composable
fun ConsentHeaderLocal(
    userName: String,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.4f))
            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.5f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1D2335),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Your Consent",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1D2335)
                )
                Text(
                    text = "Please review and accept to continue",
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563)
                )
            }
        }
    }
}

@Composable
fun WelcomeUserCardLocal(userName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalSurfaceWhite),
        border = BorderStroke(1.dp, LocalColorBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            LocalGradientStart.copy(alpha = 0.05f),
                            LocalGradientEnd.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(LocalGradientStart, LocalGradientMid, LocalGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "Welcome, $userName!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalColorTextMain
                )
                Text(
                    text = "Your privacy and wellbeing are our priority",
                    fontSize = 14.sp,
                    color = LocalColorTextSoft,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun InformationCardLocal() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalSurfaceWhite),
        border = BorderStroke(1.dp, LocalColorBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = LocalGradientStart,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Before You Begin",
                    fontWeight = FontWeight.SemiBold,
                    color = LocalColorTextMain,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "To use our mental health assessment and wellness features, we need your consent to our Terms and Conditions and Privacy Policy. Please take a moment to review these documents.",
                fontSize = 14.sp,
                color = LocalColorTextSoft,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", fontSize = 14.sp, color = LocalGradientStart, modifier = Modifier.padding(end = 8.dp))
                    Text("Camera used only during self-initiated sessions", fontSize = 13.sp, color = LocalColorTextSoft)
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", fontSize = 14.sp, color = LocalGradientStart, modifier = Modifier.padding(end = 8.dp))
                    Text("Data encrypted and never sold to third parties", fontSize = 13.sp, color = LocalColorTextSoft)
                }
                Row(verticalAlignment = Alignment.Top) {
                    Text("•", fontSize = 14.sp, color = LocalGradientStart, modifier = Modifier.padding(end = 8.dp))
                    Text("You can stop using the app at any time", fontSize = 13.sp, color = LocalColorTextSoft)
                }
            }
        }
    }
}

@Composable
fun PoliciesAndConsentCardLocal(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onTermsClick: () -> Unit,
    onPrivacyClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LocalSurfaceWhite),
        border = BorderStroke(1.dp, if (checked) LocalAccentGreen else LocalColorBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Review Our Policies",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = LocalColorTextMain,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Terms link
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTermsClick() },
                color = Color(0xFFF5F3FF),
                border = BorderStroke(1.dp, Color(0xFFE0E7FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(LocalGradientStart, LocalGradientMid)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Terms & Conditions",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalColorTextMain
                        )
                        Text(
                            "Tap to read full terms",
                            fontSize = 12.sp,
                            color = LocalColorTextSoft
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = LocalColorTextSoft,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Privacy link
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onPrivacyClick() },
                color = Color(0xFFF0F9FF),
                border = BorderStroke(1.dp, Color(0xFFBAE6FD))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF60A5FA), Color(0xFF22D3EE))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Privacy Policy",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = LocalColorTextMain
                        )
                        Text(
                            "How we protect your data",
                            fontSize = 12.sp,
                            color = LocalColorTextSoft
                        )
                    }

                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = LocalColorTextSoft,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = LocalColorBorder,
                thickness = 1.dp
            )

            // Checkbox
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCheckedChange(!checked) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = LocalAccentGreen,
                        uncheckedColor = LocalColorTextMuted
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "I have read and agree to the Terms & Conditions and Privacy Policy",
                        fontSize = 14.sp,
                        color = if (checked) LocalColorTextMain else LocalColorTextSoft
                    )
                }
            }

            if (!checked) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "You must accept the Terms and Privacy Policy to continue",
                        fontSize = 12.sp,
                        color = Color(0xFFB45309),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DisclaimerCardLocal() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
        border = BorderStroke(1.dp, LocalWarningYellow.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = LocalWarningYellow,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Important Notice",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
            }

            Text(
                text = "This app is a screening and wellness tool only.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF92400E),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "• NOT a medical diagnostic device",
                fontSize = 13.sp,
                color = Color(0xFF92400E),
                lineHeight = 18.sp
            )

            Text(
                text = "• Results are for informational purposes only",
                fontSize = 13.sp,
                color = Color(0xFF92400E),
                lineHeight = 18.sp
            )

            Text(
                text = "• Always consult a qualified professional",
                fontSize = 13.sp,
                color = Color(0xFF92400E),
                lineHeight = 18.sp
            )

            Divider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = LocalWarningYellow.copy(alpha = 0.3f),
                thickness = 1.dp
            )

            Text(
                text = "For guidance or emergencies:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF92400E)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Contact a mental health professional or emergency services in your area.",
                fontSize = 12.sp,
                color = Color(0xFF92400E).copy(alpha = 0.8f),
                lineHeight = 16.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

@Composable
fun AcceptButtonLocal(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (enabled) {
                        Brush.horizontalGradient(
                            colors = listOf(LocalGradientStart, LocalGradientMid, LocalGradientEnd)
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(LocalColorTextMuted.copy(alpha = 0.3f), LocalColorTextMuted.copy(alpha = 0.3f))
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (enabled) "Accept & Continue" else "Accept to continue",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}