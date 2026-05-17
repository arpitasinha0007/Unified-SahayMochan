package com.example.unifiedapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.UserPreferences
import kotlinx.coroutines.delay
import android.widget.Toast
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.BorderStroke
import androidx.navigation.NavController
import com.example.unifiedapp.ui.navigation.Screen

// These colors are used in LoginScreen
val SageMedium = Color(0xFFD3E4D6)
val SageAccent = Color(0xFF6B9071)
val Charcoal = Color(0xFF3E4E42)
val WhiteSoft = Color(0xFFFAFAFA)

// Gradient for background - enhanced with orange accent
val SageGreenGradient = Brush.verticalGradient(
    colors = listOf(
        LogoSoftGreen,
        SageLightest.copy(alpha = 0.7f),
        LogoSoftGreen
    )
)

// Accent gradient for decorative elements
val OrangeAccentGradient = Brush.horizontalGradient(
    colors = listOf(
        LogoOrangeMuted,
        LogoOrangeSoft,
        LogoOrangeMuted.copy(alpha = 0.5f)
    )
)

// Add this with the other color definitions at the top of LoginScreen.kt
val MintCream = Color(0xFFF0F7F0)  // Soft minty cream color

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    navController: NavController,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    var registrationId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // NEW: State for showing parent info dialog
    var showParentInfoDialog by remember { mutableStateOf(false) }
    var loggedInUserData by remember { mutableStateOf<com.example.unifiedapp.ui.views.UserData?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val userPreferences = UserPreferences(context)
    val userData by userPreferences.userData.collectAsStateWithLifecycle(
        initialValue = com.example.unifiedapp.ui.views.UserData(
            isLoggedIn = false,
            name = "",
            email = "",
            gender = "",
            id = "",
            age = 0,
            TOKEN = "",
            isUnderage = false,
            parentName = "",
            parentEmail = "",
            phoneNumber = ""
        ),
        lifecycle = lifecycleOwner.lifecycle
    )

    // Track if navigation has been triggered to prevent multiple navigations
    var hasNavigated by remember { mutableStateOf(false) }

    // Debug logging
    LaunchedEffect(Unit) {
        println("🔐 LoginScreen - Current loginState: $loginState")
        println("🔐 LoginScreen - User isLoggedIn: ${userData.isLoggedIn}")
    }

    // Show parent info dialog when underage user logs in
    LaunchedEffect(loginState, userData) {
        if (loginState is AuthViewModel.LoginState.Success && userData.isUnderage && userData.isLoggedIn) {
            loggedInUserData = userData
            showParentInfoDialog = true
        }
    }

    // Navigate when login succeeds - only once
    LaunchedEffect(loginState, userData.isLoggedIn) {
        when (loginState) {
            is AuthViewModel.LoginState.Success -> {
                if (!hasNavigated) {
                    hasNavigated = true
                    println("✅ Login Success detected in LoginScreen - isLoggedIn: ${userData.isLoggedIn}, isUnderage: ${userData.isUnderage}")

                    // Wait a bit for userData to be updated
                    delay(500)

                    // Show success toast with underage notice
                    val message = if (userData.isUnderage) {
                        "Login successful! Parent consent is active."
                    } else {
                        "Login successful!"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                    // Navigate to home/profile screen with clear back stack
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            is AuthViewModel.LoginState.Error -> {
                val error = loginState as AuthViewModel.LoginState.Error
                println("❌ Login Error: ${error.message}")
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                // Reset hasNavigated on error so user can try again
                hasNavigated = false
            }
            else -> {}
        }
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SageGreenGradient)
    ) {
        // Decorative elements - subtle accents
        Box(
            modifier = Modifier
                .size(200.dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LogoOrangeMuted.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(100.dp)
                )
        )

        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 80.dp, y = 80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            LogoAccentGreen.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(125.dp)
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Logo with enhanced design
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White,
                                        WhiteSoft
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = "Logo",
                                tint = SageAccent,
                                modifier = Modifier.size(48.dp)
                            )
                            // Small accent dot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .offset(y = (-4).dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(OrangeAccentGradient)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    color = LogoDarkBrown,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    "Sign in to continue your wellness journey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SoftGreyText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Decorative underline
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(3.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    LogoOrangeMuted,
                                    LogoOrangeSoft,
                                    LogoOrangeMuted
                                )
                            ),
                            shape = RoundedCornerShape(1.5.dp)
                        )
                        .padding(top = 8.dp)
                )

                Spacer(Modifier.height(40.dp))

                // Input fields with enhanced styling
                PremiumInput(
                    value = registrationId,
                    onValueChange = { registrationId = it },
                    label = "Registration ID",
                    icon = Icons.Default.Badge,
                    accentColor = SageAccent
                )

                Spacer(Modifier.height(16.dp))

                PremiumInput(
                    value = password,
                    onValueChange = { password = it },
                    label = "Password",
                    icon = Icons.Default.Lock,
                    isPasswordField = true,
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible },
                    accentColor = SageAccent
                )

                // Forgot password hint - NOW NAVIGATES TO FORGOT PASSWORD SCREEN
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot password?",
                        fontSize = 12.sp,
                        color = LogoOrangeMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Navigate to Forgot Password screen
                            navController.navigate(Screen.ForgotPassword.route)
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Login Button with gradient
                Button(
                    onClick = {
                        println("🔑 Login button clicked - ID: $registrationId")
                        if (registrationId.isNotBlank() && password.isNotBlank()) {
                            // Reset navigation flag on new login attempt
                            hasNavigated = false
                            viewModel.login(registrationId, password)
                        } else {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
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
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        SagePrimary,
                                        SageAccent,
                                        SagePrimary
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (loginState) {
                            is AuthViewModel.LoginState.Loading -> {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            else -> {
                                Text(
                                    "Sign In",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Error message display with icon
                if (loginState is AuthViewModel.LoginState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF0F0)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = (loginState as AuthViewModel.LoginState.Error).message,
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Divider with text
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(SoftGreyText.copy(alpha = 0.2f))
                    )
                    Text(
                        "New to Sahay?",
                        modifier = Modifier.padding(horizontal = 12.dp),
                        fontSize = 13.sp,
                        color = SoftGreyText
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(SoftGreyText.copy(alpha = 0.2f))
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Create Account Button (outlined)
                OutlinedButton(
                    onClick = onNavigateToRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SageAccent
                    ),
                    border = BorderStroke(1.dp, SageAccent.copy(alpha = 0.5f))
                ) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Create Account",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Terms hint
                Text(
                    text = "By signing in, you agree to our Terms & Privacy Policy",
                    fontSize = 11.sp,
                    color = SoftGreyText.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    // Parent Info Dialog - Shown when underage user logs in
    if (showParentInfoDialog && loggedInUserData != null) {
        ParentInfoDialog(
            userData = loggedInUserData!!,
            onDismiss = {
                showParentInfoDialog = false
                loggedInUserData = null
            }
        )
    }
}

// NEW: Parent Information Dialog for underage users
@Composable
fun ParentInfoDialog(
    userData: com.example.unifiedapp.ui.views.UserData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardWhite,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(LogoOrangeMuted, LogoOrangeSoft)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.FamilyRestroom,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Parent/Guardian Information",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LogoDarkBrown
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Since you're under 18, your parent/guardian has been notified about your account.",
                    fontSize = 14.sp,
                    color = SoftGreyText,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Parent Name Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftPeach),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = LogoOrangeMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Parent/Guardian Name",
                                fontSize = 11.sp,
                                color = SoftGreyText
                            )
                            Text(
                                userData.parentName.ifEmpty { "Not provided" },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = LogoDarkBrown
                            )
                        }
                    }
                }

                // Parent Email Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftLavender),
                    shape = RoundedCornerShape(12.dp)
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Parent/Guardian Email",
                                fontSize = 11.sp,
                                color = SoftGreyText
                            )
                            Text(
                                userData.parentEmail.ifEmpty { "Not provided" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = LogoDarkBrown
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Info note
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MintCream,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = SagePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Your parent will receive important updates about your wellness journey.",
                            fontSize = 11.sp,
                            color = SageDark
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SagePrimary
                )
            ) {
                Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PremiumInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPasswordField: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    accentColor: Color = SageAccent
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isFocused) LogoDarkBrown else Charcoal.copy(alpha = 0.6f),
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            interactionSource = interactionSource,
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isFocused) accentColor else Charcoal.copy(alpha = 0.4f)
                )
            },
            trailingIcon = {
                if (isPasswordField) {
                    IconButton(onClick = onPasswordToggle) {
                        val visibilityIcon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        Icon(
                            visibilityIcon,
                            contentDescription = null,
                            tint = if (isFocused) accentColor else Charcoal.copy(alpha = 0.4f)
                        )
                    }
                }
            },
            visualTransformation = if (isPasswordField && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = SageMedium.copy(alpha = 0.3f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = WhiteSoft,
                cursorColor = accentColor,
                focusedTextColor = LogoDarkBrown,
                unfocusedTextColor = Charcoal
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
        )

        // Subtle accent line when focused
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(2.dp)
                    .padding(start = 4.dp, top = 4.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(accentColor, LogoOrangeMuted)
                        ),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}