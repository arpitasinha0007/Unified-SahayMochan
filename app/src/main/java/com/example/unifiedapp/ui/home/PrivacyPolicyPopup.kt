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
import com.example.unifiedapp.theme.*
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
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PaleMint,
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
                                    SageMint.copy(alpha = 0.05f)
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
                                    color = SageDark
                                )
                                Text(
                                    "How Sahay protects your data",
                                    fontSize = 13.sp,
                                    color = SageDark.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Close button
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
                    // Introduction Card
                    PolicySection(
                        title = "Our Commitment to Privacy",
                        icon = Icons.Default.Favorite,
                        color = SoftRose
                    ) {
                        Text(
                            "Your privacy is respected and safeguarded by the Sahay mobile application (\"App\"). This app is only meant to collect the bare minimum of data that is typically required to perform AI-based mental health assessments for depression and anxiety.",
                            fontSize = 14.sp,
                            color = SageDark.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }

                    // Camera & Data Collection
                    PolicySection(
                        title = "Camera & Facial Analysis",
                        icon = Icons.Default.Videocam,
                        color = MistyBlue
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "When you initiate a screening session on your own, the app may use your device's camera for a brief period of time to:",
                                fontSize = 14.sp,
                                color = SageDark.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                            BulletPoint("Examine common facial expression patterns")
                            BulletPoint("Obtain computational features that the AI model requires")
                            BulletPoint("Ensure data cannot be linked to a particular individual")

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SoftLilac,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "🌿 Raw photos or videos are not retained unless you request it. Data processing is only intended to provide you with wellness information.",
                                    fontSize = 12.sp,
                                    color = SageDark,
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Technical Information
                    PolicySection(
                        title = "Technical Information Collected",
                        icon = Icons.Default.Info,
                        color = SoftLavender
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "We may collect technical information to improve research and system reliability:",
                                fontSize = 14.sp,
                                color = SageDark.copy(alpha = 0.8f),
                                lineHeight = 20.sp
                            )
                            BulletPoint("Device type and specifications")
                            BulletPoint("App usage logs and anonymized performance metrics")
                            BulletPoint("Aggregated data for improving AI models")
                        }
                    }

                    // Data Protection
                    PolicySection(
                        title = "Data Protection & Encryption",
                        icon = Icons.Default.Lock,
                        color = SageMint
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BulletPoint("Secure encryption protocols safeguard data transferred between your device and our servers")
                            BulletPoint("Data handling complies with all relevant privacy and data protection regulations")
                            BulletPoint("The app is limited to research, early screening, and health monitoring")

                            Spacer(modifier = Modifier.height(4.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = PaleMint,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "✓ Your data will not be sold, rented, or shared with third parties unless required by law or with your explicit consent.",
                                    fontSize = 12.sp,
                                    color = SagePrimary,
                                    modifier = Modifier.padding(10.dp),
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Your Rights
                    PolicySection(
                        title = "Your Rights & Choices",
                        icon = Icons.Default.Gavel,
                        color = SoftApricot
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BulletPoint("You are free to stop using the app at any moment")
                            BulletPoint("You may request deletion of your data")
                            BulletPoint("Choose whether to participate in screening sessions")
                            BulletPoint("Withdraw consent at any time")

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "By continuing to use the App, you accept this Privacy Policy and its associated Terms and Conditions.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = SagePrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Children's Privacy
                    PolicySection(
                        title = "Children's Privacy",
                        icon = Icons.Default.People,
                        color = BlushPink
                    ) {
                        Text(
                            "The App is not intended for individuals under the age of 13. We do not knowingly collect personal information from children. If you are a parent or guardian and believe your child has provided us with personal information, please contact us.",
                            fontSize = 14.sp,
                            color = SageDark.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }

                    // Changes to Policy
                    PolicySection(
                        title = "Changes to This Policy",
                        icon = Icons.Default.Update,
                        color = CloudBlue
                    ) {
                        Text(
                            "We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy on this page and updating the \"effective date\" at the top.",
                            fontSize = 14.sp,
                            color = SageDark.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }

                    // Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SageLight),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SageMedium)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "🌿 Summary",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SageDark
                            )

                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Minimal data collection for AI assessment", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Camera used briefly during self-initiated sessions", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Raw images/videos not retained without request", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("Data encrypted and never sold to third parties", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                            Row(verticalAlignment = Alignment.Top) {
                                Text("•", fontSize = 14.sp, color = SagePrimary, modifier = Modifier.padding(end = 8.dp))
                                Text("You can stop using the app anytime", fontSize = 13.sp, color = SageDark.copy(alpha = 0.8f))
                            }
                        }
                    }

                    // Contact Information
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = SoftPeach,
                        border = BorderStroke(1.dp, SoftApricot)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = SagePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "For questions about this policy, contact us through the app's support channel.",
                                fontSize = 12.sp,
                                color = SageDark,
                                lineHeight = 16.sp,
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
                                    PaleMint
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
                                            SageMint
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
fun PolicySection(
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



// Composable to show a clickable privacy policy link that opens the popup
@Composable
fun PrivacyPolicyLink(
    modifier: Modifier = Modifier,
    text: String = "Privacy Policy",
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