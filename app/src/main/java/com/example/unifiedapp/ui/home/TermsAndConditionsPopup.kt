package com.example.unifiedapp.ui.home

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.example.unifiedapp.theme.*



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
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MintCream,
                                SurfaceWhite
                            )
                        )
                    )
            ) {
                // Header with sage gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    SagePrimary.copy(alpha = 0.1f),
                                    Seafoam.copy(alpha = 0.05f)
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
                            // Icon with sage gradient background
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SageGradient),
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
                                    color = SageDark
                                )
                                Text(
                                    "Sahay Wellness App Agreement",
                                    fontSize = 13.sp,
                                    color = SageDark.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Close button with sage styling
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SageLight)
                                .border(1.dp, SageMedium, RoundedCornerShape(10.dp))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = SagePrimary,
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
                        title = "Acceptance of Terms",
                        icon = Icons.Default.CheckCircle,
                        color = SagePrimary
                    ) {
                        Text(
                            "By downloading, installing, or using the Sahay mobile application (\"App\"), you agree to these Terms and Conditions. This AI-based tool is only meant to be used for early screening and wellness monitoring of possible signs of anxiety and depression and is not a substitute for medical diagnosis, treatment, or professional mental health advice.",
                            fontSize = 14.sp,
                            color = SageDark.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }

                    // How It Works
                    TermsSection(
                        title = "How the App Works",
                        icon = Icons.Default.Info,
                        color = PowderBlue
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "The App looks at facial expressions taken with your device's camera and processes non-identifiable data to provide wellness insights.",
                                fontSize = 14.sp,
                                color = SageDark.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = LavenderMist,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "🌿 Users must give their informed consent before using the analysis features.",
                                    fontSize = 12.sp,
                                    color = SageDark,
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // User Agreement
                    TermsSection(
                        title = "User Agreement",
                        icon = Icons.Default.Gavel,
                        color = DustyRose
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "By using this app, you agree to:",
                                fontSize = 14.sp,
                                color = SageDark.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                            BulletPoint("The collection and processing of limited data necessary for the app to function")
                            BulletPoint("Using the app only for its intended purpose of early screening and wellness monitoring")
                            BulletPoint("Understanding that this is NOT a diagnostic tool")

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "You can stop using the app at any time.",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = SagePrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Medical Disclaimer
                    TermsSection(
                        title = "Important Medical Disclaimer",
                        icon = Icons.Default.Warning,
                        color = SoftCoral
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Butter,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "The developers and researchers do not guarantee the accuracy or outcomes of the assessments.",
                                    fontSize = 13.sp,
                                    color = SageDark,
                                    modifier = Modifier.padding(12.dp),
                                    lineHeight = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DustyRose.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, DustyRose)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Emergency,
                                        contentDescription = null,
                                        tint = SoftCoral,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "If you are in distress or have severe symptoms, please seek help from a qualified healthcare professional or local mental health service.",
                                        fontSize = 13.sp,
                                        color = SageDark,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }

                    // Prohibited Uses
                    TermsSection(
                        title = "Prohibited Uses",
                        icon = Icons.Default.Block,
                        color = DustyRose
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "You cannot use the App for:",
                                fontSize = 14.sp,
                                color = SageDark.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                            BulletPoint("Making decisions in an emergency")
                            BulletPoint("Making a clinical diagnosis")
                            BulletPoint("Discrimination of any kind")
                            BulletPoint("Monitoring individuals without their consent")
                        }
                    }

                    // Intellectual Property
                    TermsSection(
                        title = "Intellectual Property",
                        icon = Icons.Default.Copyright,
                        color = LavenderMist
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "The developers own all intellectual property rights to the App, algorithms, and content.",
                                fontSize = 14.sp,
                                color = SageDark.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = LavenderMist,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "You cannot share, reverse engineer, or use them commercially without permission.",
                                    fontSize = 12.sp,
                                    color = SageDark,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // Privacy & Data
                    TermsSection(
                        title = "Privacy & Data",
                        icon = Icons.Default.PrivacyTip,
                        color = Seafoam
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Your privacy is important to us. We collect only the data necessary for the app to function:",
                                fontSize = 14.sp,
                                color = SageDark.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                            BulletPoint("Facial expression data (processed locally on your device)")
                            BulletPoint("Assessment responses (stored securely)")
                            BulletPoint("Basic usage analytics (anonymized)")

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                "For complete details, please review our Privacy Policy.",
                                fontSize = 13.sp,
                                color = SagePrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { /* Navigate to privacy policy */ }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    // Updates and Changes
                    TermsSection(
                        title = "Updates & Changes",
                        icon = Icons.Default.Update,
                        color = Seafoam
                    ) {
                        Text(
                            "The developers may update the App, these terms, and related services at any time. Continued use of the App constitutes acceptance of the updated terms.",
                            fontSize = 14.sp,
                            color = SageDark.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }

                    // Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MintCream),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SageMedium)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "🌿 Key Points Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SageDark
                            )

                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("App is for early screening only, NOT medical diagnosis", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Facial expressions analyzed with your consent", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Seek professional help for severe symptoms or emergencies", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("You can stop using the app at any time", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Terms may be updated; continued use means acceptance", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                        }
                    }

                    // Emergency Resources
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = PeachSorbet,
                        border = BorderStroke(1.dp, SoftCoral)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = SoftCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Emergency Resources",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SageDark
                                )
                            }
                            Text(
                                "If you're in crisis, please contact:",
                                fontSize = 12.sp,
                                color = SageDark.copy(alpha = 0.8f)
                            )
                            Text(
                                "• National Crisis Hotline: 988\n• Emergency Services: 911\n• Local Mental Health Services",
                                fontSize = 12.sp,
                                color = SageDark,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Footer with accept button - sage gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MintCream
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
                                    Brush.linearGradient(
                                        colors = listOf(
                                            SagePrimary,
                                            SageAccent,
                                            Seafoam
                                        )
                                    ),
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
            // Icon with colored background
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color.copy(alpha = 0.15f)),
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
                color = SageDark
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
fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "•",
            fontSize = 14.sp,
            color = SagePrimary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text,
            fontSize = 13.sp,
            color = SageDark.copy(alpha = 0.8f),
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// Composable to show a clickable terms link that opens the popup
@Composable
fun TermsLink(
    modifier: Modifier = Modifier,
    text: String = "Terms & Conditions",
    onClick: () -> Unit
) {
    Text(
        text = text,
        color = SagePrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    )
}