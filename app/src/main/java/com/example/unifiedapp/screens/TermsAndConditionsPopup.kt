package com.example.unifiedapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.vector.ImageVector

// ============ LOCAL COLOR DEFINITIONS (Unique to this file) ============
private val TermsPurplePrimary = Color(0xFF8B5CF6)
private val TermsPurpleSecondary = Color(0xFFA78BFA)
private val TermsPurpleLight = Color(0xFFC4B5FD)
private val TermsPurpleUltraLight = Color(0xFFF5F3FF)
private val TermsSurfaceWhite = Color(0xFFFFFFFF)
private val TermsTextPrimary = Color(0xFF1F2937)
private val TermsTextSecondary = Color(0xFF4B5563)
private val TermsOrangeWarm = Color(0xFFF97316)

// ============ LOCAL GRADIENTS ============
private val TermsGradientPurple = Brush.linearGradient(
    colors = listOf(TermsPurplePrimary, TermsPurpleSecondary)
)
private val TermsGradientSunset = Brush.linearGradient(
    colors = listOf(Color(0xFFFF8A80), Color(0xFFFFB74D))
)

@Composable
fun TermsAndConditionsPopup(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .shadow(16.dp, RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = TermsSurfaceWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                TermsPurpleUltraLight,
                                TermsSurfaceWhite
                            )
                        )
                    )
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    TermsPurplePrimary.copy(alpha = 0.15f),
                                    TermsPurpleSecondary.copy(alpha = 0.05f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TermsGradientSunset),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    "Terms & Conditions",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TermsTextPrimary
                                )
                                Text(
                                    "Please read carefully",
                                    fontSize = 13.sp,
                                    color = TermsTextSecondary
                                )
                            }
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TermsPurpleUltraLight)
                                .border(1.dp, TermsPurpleLight.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TermsPurplePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Introduction
                    TermsSection(
                        title = "1. Acceptance of Terms",
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF10B981)
                    ) {
                        Text(
                            "By downloading, accessing, or using the Sahay and Mochan applications (\"Apps\"), you agree to be bound by these Terms and Conditions. If you do not agree, please do not use the Apps.",
                            fontSize = 14.sp,
                            color = TermsTextSecondary,
                            lineHeight = 20.sp
                        )
                    }

                    // Use of Services
                    TermsSection(
                        title = "2. Use of Services",
                        icon = Icons.Default.Info,
                        color = TermsPurplePrimary
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "The Apps provide mental health screening tools for research and wellness purposes. You agree to:",
                                fontSize = 14.sp,
                                color = TermsTextSecondary
                            )
                            TermsBulletPoint("Use the Apps only for their intended purpose")
                            TermsBulletPoint("Not misuse or attempt to reverse engineer the Apps")
                            TermsBulletPoint("Provide accurate information during assessments")
                            TermsBulletPoint("Not share your account credentials with others")
                        }
                    }

                    // Medical Disclaimer
                    TermsSection(
                        title = "3. Medical Disclaimer",
                        icon = Icons.Default.Warning,
                        color = TermsOrangeWarm
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "The Apps are screening tools only and NOT medical devices. They do not provide medical diagnosis or treatment recommendations.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TermsOrangeWarm
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Always consult a qualified healthcare professional for medical advice. In case of emergency, contact your local emergency services immediately.",
                                fontSize = 13.sp,
                                color = TermsTextSecondary
                            )
                        }
                    }

                    // Privacy
                    TermsSection(
                        title = "4. Privacy & Data Collection",
                        icon = Icons.Default.Shield,
                        color = TermsPurplePrimary
                    ) {
                        Text(
                            "Your use of the Apps is also governed by our Privacy Policy. By using the Apps, you consent to the collection and use of your data as described therein.",
                            fontSize = 14.sp,
                            color = TermsTextSecondary,
                            lineHeight = 20.sp
                        )
                    }

                    // User Responsibilities
                    TermsSection(
                        title = "5. User Responsibilities",
                        icon = Icons.Default.Gavel,
                        color = Color(0xFFF59E0B)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TermsBulletPoint("You are responsible for maintaining the confidentiality of your account")
                            TermsBulletPoint("You are responsible for all activities that occur under your account")
                            TermsBulletPoint("Notify us immediately of any unauthorized use")
                        }
                    }

                    // Limitations
                    TermsSection(
                        title = "6. Limitations of Liability",
                        icon = Icons.Default.Error,
                        color = Color(0xFFEF4444)
                    ) {
                        Text(
                            "To the maximum extent permitted by law, the Apps and their developers shall not be liable for any indirect, incidental, or consequential damages arising from your use of the Apps.",
                            fontSize = 14.sp,
                            color = TermsTextSecondary,
                            lineHeight = 20.sp
                        )
                    }

                    // Changes to Terms
                    TermsSection(
                        title = "7. Changes to Terms",
                        icon = Icons.Default.Update,
                        color = Color(0xFF3B82F6)
                    ) {
                        Text(
                            "We reserve the right to modify these Terms at any time. Continued use of the Apps after changes constitutes acceptance of the modified Terms.",
                            fontSize = 14.sp,
                            color = TermsTextSecondary,
                            lineHeight = 20.sp
                        )
                    }

                    // Contact
                    TermsSection(
                        title = "8. Contact Us",
                        icon = Icons.Default.Email,
                        color = Color(0xFF8B5CF6)
                    ) {
                        Text(
                            "For questions about these Terms, please contact us through the app's support channel.",
                            fontSize = 14.sp,
                            color = TermsTextSecondary,
                            lineHeight = 20.sp
                        )
                    }

                    // Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = TermsPurpleUltraLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, TermsPurpleLight.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "📋 Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TermsTextPrimary
                            )
                            TermsBulletPoint("Screening tool only - not a medical device")
                            TermsBulletPoint("Your privacy is protected")
                            TermsBulletPoint("Use responsibly and ethically")
                            TermsBulletPoint("Contact support for questions")
                        }
                    }
                }

                // Footer with accept button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    TermsPurpleUltraLight
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    TermsGradientPurple,
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "I Accept the Terms",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TermsSection(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TermsTextPrimary
            )
        }

        Column(
            modifier = Modifier.padding(start = 32.dp),
            content = content
        )
    }
}

@Composable
fun TermsBulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "•",
            fontSize = 14.sp,
            color = TermsPurplePrimary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text,
            fontSize = 13.sp,
            color = TermsTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun TermsAndConditionsLink(
    modifier: Modifier = Modifier,
    text: String = "Terms & Conditions",
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = TermsPurplePrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}