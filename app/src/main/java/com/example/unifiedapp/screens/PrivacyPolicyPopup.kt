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
private val PrivacyPurplePrimary = Color(0xFF8B5CF6)
private val PrivacyPurpleSecondary = Color(0xFFA78BFA)
private val PrivacyPurpleLight = Color(0xFFC4B5FD)
private val PrivacyPurpleUltraLight = Color(0xFFF5F3FF)
private val PrivacySurfaceWhite = Color(0xFFFFFFFF)
private val PrivacyTextPrimary = Color(0xFF1F2937)
private val PrivacyTextSecondary = Color(0xFF4B5563)
private val PrivacyBlueBright = Color(0xFF60A5FA)
private val PrivacyGreenMint = Color(0xFF10B981)
private val PrivacyOrangeWarm = Color(0xFFF97316)

// ============ LOCAL GRADIENT ============
private val PrivacyGradientPurple = Brush.linearGradient(
    colors = listOf(PrivacyPurplePrimary, PrivacyPurpleSecondary)
)

@Composable
fun PrivacyPolicyPopup(
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
            colors = CardDefaults.cardColors(containerColor = PrivacySurfaceWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PrivacyPurpleUltraLight,
                                PrivacySurfaceWhite
                            )
                        )
                    )
            ) {
                // Header with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    PrivacyPurplePrimary.copy(alpha = 0.15f),
                                    PrivacyPurpleSecondary.copy(alpha = 0.05f)
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
                            // Icon with gradient background
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PrivacyGradientPurple),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    "Privacy Policy",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrivacyTextPrimary
                                )
                                Text(
                                    "How Sahay & Mochan protect your data",
                                    fontSize = 13.sp,
                                    color = PrivacyTextSecondary
                                )
                            }
                        }

                        // Close button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrivacyPurpleUltraLight)
                                .border(1.dp, PrivacyPurpleLight.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = PrivacyPurplePrimary,
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
                    // Introduction Card
                    PrivacyPolicySection(
                        title = "Our Commitment to Privacy",
                        icon = Icons.Default.Favorite,
                        color = Color(0xFFEC4899)
                    ) {
                        Text(
                            "Your privacy is respected and safeguarded by the Sahay and Mochan mobile applications (\"Apps\"). These apps are only meant to collect the bare minimum of data that is typically required to perform AI-based mental health assessments for depression and anxiety.",
                            fontSize = 14.sp,
                            color = PrivacyTextSecondary,
                            lineHeight = 20.sp
                        )
                    }

                    // Camera & Data Collection
                    PrivacyPolicySection(
                        title = "Camera & Facial Analysis",
                        icon = Icons.Default.Videocam,
                        color = PrivacyBlueBright
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "When a user initiates a screening session on their own, the apps may use the device's camera for a brief period of time to:",
                                fontSize = 14.sp,
                                color = PrivacyTextSecondary,
                                lineHeight = 20.sp
                            )
                            PrivacyBulletPoint("Examine common facial expression patterns")
                            PrivacyBulletPoint("Obtain computational features that the AI model requires")
                            PrivacyBulletPoint("Ensure data cannot be linked to a particular individual")

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = PrivacyPurpleUltraLight,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "⚠️ Raw photos or videos are not retained unless the user requests it. Data processing is only intended to provide the user with wellness information.",
                                    fontSize = 12.sp,
                                    color = PrivacyTextSecondary,
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Technical Information
                    PrivacyPolicySection(
                        title = "Technical Information Collected",
                        icon = Icons.Default.Info,
                        color = PrivacyPurplePrimary
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "We may collect technical information to improve research and system reliability:",
                                fontSize = 14.sp,
                                color = PrivacyTextSecondary,
                                lineHeight = 20.sp
                            )
                            PrivacyBulletPoint("Device type and specifications")
                            PrivacyBulletPoint("App usage logs and anonymized performance metrics")
                            PrivacyBulletPoint("Aggregated data for improving AI models")
                        }
                    }

                    // Data Protection
                    PrivacyPolicySection(
                        title = "Data Protection & Encryption",
                        icon = Icons.Default.Lock,
                        color = PrivacyGreenMint
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrivacyBulletPoint("Secure encryption protocols safeguard data transferred between your device and our servers")
                            PrivacyBulletPoint("Data handling complies with all relevant privacy and data protection regulations")
                            PrivacyBulletPoint("The apps are limited to research, early screening, and health monitoring")

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "✓ The collected data will not be sold, rented, or shared with third parties unless required by law or with your explicit consent.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Your Rights
                    PrivacyPolicySection(
                        title = "Your Rights & Choices",
                        icon = Icons.Default.Gavel,
                        color = PrivacyOrangeWarm
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            PrivacyBulletPoint("Users are free to stop using the apps at any moment")
                            PrivacyBulletPoint("You may request deletion of your data")
                            PrivacyBulletPoint("Choose whether to participate in screening sessions")
                            PrivacyBulletPoint("Withdraw consent at any time")

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "By continuing to use the Apps, you accept this Privacy Policy and its associated Terms and Conditions.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrivacyTextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PrivacyPurpleUltraLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PrivacyPurpleLight.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "📋 Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrivacyTextPrimary
                            )

                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = PrivacyPurplePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Minimal data collection for AI assessment", fontSize = 13.sp, color = PrivacyTextSecondary)
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = PrivacyPurplePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Camera used briefly during self-initiated sessions", fontSize = 13.sp, color = PrivacyTextSecondary)
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = PrivacyPurplePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Raw images/videos not retained without request", fontSize = 13.sp, color = PrivacyTextSecondary)
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = PrivacyPurplePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Data encrypted and never sold to third parties", fontSize = 13.sp, color = PrivacyTextSecondary)
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = PrivacyPurplePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("You can stop using the apps anytime", fontSize = 13.sp, color = PrivacyTextSecondary)
                            }
                        }
                    }

                    // Contact Information
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE3F2FD),
                        border = BorderStroke(1.dp, Color(0xFF90CAF9))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "For questions about this policy, contact us through the app's support channel.",
                                fontSize = 12.sp,
                                color = Color(0xFF0D47A1),
                                lineHeight = 16.sp
                            )
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
                                    PrivacyPurpleUltraLight
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
                                    PrivacyGradientPurple,
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "I Understand & Accept",
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
fun PrivacyPolicySection(
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
            // Icon with colored background
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

            // Title
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrivacyTextPrimary
            )
        }

        // Content with left padding
        Column(
            modifier = Modifier.padding(start = 32.dp),
            content = content
        )
    }
}

@Composable
fun PrivacyBulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "•",
            fontSize = 14.sp,
            color = PrivacyPurplePrimary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text,
            fontSize = 13.sp,
            color = PrivacyTextSecondary,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PrivacyPolicyLink(
    modifier: Modifier = Modifier,
    text: String = "Privacy Policy",
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = PrivacyPurplePrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}