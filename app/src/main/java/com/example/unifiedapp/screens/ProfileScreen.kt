package com.example.unifiedapp.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.security.MessageDigest
import java.util.Locale
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.example.unifiedapp.utils.UserSessionHelper
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
// ✅ Import ONLY from theme - remove local definitions
import com.example.unifiedapp.theme.*
import androidx.compose.foundation.shape.CircleShape
// Do NOT redefine gradients here - use the imported ones

// ============ COLOR DEFINITIONS (Only use theme imports, no local redefinitions) ============
// All colors and gradients are imported from com.example.unifiedapp..theme
// Do NOT redefine PurplePrimary, GradientPurple, etc. here

// ============ DATA CLASSES ============

data class UserProfile(
    val name: String,
    val gender: String,
    val email: String,
    val age: Int,
    val registration_id: String,
    val anonymousId: String = "",
    val phoneNumber: String = "",
    val phoneVerified: Boolean = false,
    val parent: ParentInfo? = null
)

data class ParentInfo(
    val parentName: String,
    val parentEmail: String,
    val isVerified: Boolean
)

data class TrialsInfo(
    val depressionRemaining: Int,
    val depressionTotal: Int,
    val anxietyRemaining: Int,
    val anxietyTotal: Int,
    val canTakeDepression: Boolean,
    val canTakeAnxiety: Boolean
)

data class SendOtpResponse(
    val success: Boolean,
    val message: String,
    val verificationId: String? = null,
    val testMode: Boolean = false,
    val demoOtp: String? = null
)

data class VerifyOtpResponse(
    val success: Boolean,
    val message: String,
    val verified: Boolean = false,
    val phoneNumber: String? = null
)

enum class AuthMode { LOGIN, SIGNUP }

// ============ VALIDATION FUNCTIONS ============

fun validateRegistrationId(regId: String): String? {
    return when {
        regId.isBlank() -> "Registration ID is required"
        regId.contains(" ") -> "Registration ID cannot contain spaces"
        else -> null
    }
}

fun validatePassword(password: String): String? {
    return when {
        password.isBlank() -> "Password is required"
        password.length < 6 -> "Password must be at least 6 characters"
        password.contains(" ") -> "Password cannot contain spaces"
        else -> null
    }
}

fun validateName(name: String): String? {
    return when {
        name.isBlank() -> "Name is required"
        name.length < 2 -> "Name must be at least 2 characters"
        else -> null
    }
}

fun validateEmail(email: String): String? {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    return when {
        email.isBlank() -> "Email is required"
        email.contains(" ") -> "Email cannot contain spaces"
        !emailRegex.matches(email) -> "Enter a valid email address"
        else -> null
    }
}

fun validateAge(age: String, minAge: Int = 18): String? {
    val ageInt = age.toIntOrNull()
    return when {
        age.isBlank() -> "Age is required"
        ageInt == null -> "Please enter a valid number"
        ageInt < minAge -> "You must be at least $minAge years old to register"
        ageInt > 120 -> "Please enter a valid age"
        else -> null
    }
}

fun validateConfirmPassword(password: String, confirmPassword: String): String? {
    return when {
        confirmPassword.isBlank() -> "Please confirm your password"
        password != confirmPassword -> "Passwords don't match"
        else -> null
    }
}


// ==========================================
// PROFILE SCREEN (Display user info)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val session = UserSessionHelper.getUserData(context)
    val isLoggedIn = session.isLoggedIn

    Scaffold(
        containerColor = SoftPurpleBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Profile",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLoggedIn) {
                // Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header with gradient
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    GradientPurpleDark,
                                    RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                                )
                                .padding(24.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(70.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = session.name.take(1).uppercase(),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        session.name,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        session.email,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Info section
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            ProfileInfoRow(
                                icon = Icons.Default.Person,
                                label = "Name",
                                value = session.name
                            )
                            ProfileInfoRow(
                                icon = Icons.Default.Email,
                                label = "Email",
                                value = session.email
                            )
                            ProfileInfoRow(
                                icon = Icons.Outlined.ConfirmationNumber,
                                label = "Registration ID",
                                value = session.registrationId
                            )
                            ProfileInfoRow(
                                icon = Icons.Default.Numbers,
                                label = "Age",
                                value = "${session.age} years"
                            )
                            ProfileInfoRow(
                                icon = Icons.Default.Person,
                                label = "Gender",
                                value = session.gender
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Logout Button
                Button(
                    onClick = {
                        UserSessionHelper.clearUserData(context)
                        Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        navController.navigate("auth") {
                            popUpTo("dashboard") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text(
                        "Logout",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            } else {
                // Not logged in
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = PurplePrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Not Logged In",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Please login to access your profile",
                            fontSize = 14.sp,
                            color = ColorTextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate("auth") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                        ) {
                            Text("Go to Login", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = PurplePrimary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                label,
                fontSize = 12.sp,
                color = ColorTextSecondary
            )
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = ColorTextPrimary
            )
        }
    }
}