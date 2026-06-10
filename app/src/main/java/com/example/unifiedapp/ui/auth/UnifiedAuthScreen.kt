package com.example.unifiedapp.ui.auth

import android.app.Application
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.example.unifiedapp.navigation.Screen
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.utils.UserSessionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// Color scheme (lavender + sage)
val AuthLavenderPrimary = Color(0xFF9D8DF1)
val AuthLavenderAccent = Color(0xFFD9D1FF)
val AuthLavenderBackground = Color(0xFFFAF8FF)
val AuthTextPrimary = Color(0xFF1F2937)
val AuthTextSecondary = Color(0xFF6B7280)
val AuthErrorRed = Color(0xFFEF4444)

data class UserProfile(
    val name: String,
    val email: String,
    val registrationId: String,
    val age: Int,
    val gender: String,
    val isLoggedIn: Boolean = true,
    val anonymousId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAuthScreen(
    navController: NavController,
    onLoginSuccess: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Create ViewModel manually (no Hilt)
    val authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(context.applicationContext as Application) as T
            }
        }
    )

    var selectedRole by remember { mutableStateOf("patient") }

    // Auto‑redirect if already logged in (patient only)
    LaunchedEffect(Unit) {
        val session = UserSessionHelper.getUserData(context)
        if (session.isLoggedIn) {
            val profile = UserProfile(
                name = session.name,
                email = session.email,
                registrationId = session.registrationId,
                age = session.age,
                gender = session.gender,
                isLoggedIn = true,
                anonymousId = session.anonymousId
            )
            onLoginSuccess(profile)
        }
    }

    var isLoginMode by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Login fields
    var loginRegId by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var loginPasswordVisible by remember { mutableStateOf(false) }

    // Signup fields (patient only)
    var signupRegId by remember { mutableStateOf("") }
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupAge by remember { mutableStateOf("") }
    var signupGender by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }

    // Observe clinician login state
    val clinicianLoginState by authViewModel.clinicianLoginState.collectAsState()
    LaunchedEffect(clinicianLoginState) {
        when (clinicianLoginState) {
            is AuthViewModel.LoginState.Success -> {
                navController.navigate(Screen.CLINICIAN_DASHBOARD) {
                    popUpTo(Screen.AUTH) { inclusive = true }
                }
                authViewModel.resetClinicianLoginState()
            }
            is AuthViewModel.LoginState.Error -> {
                val error = (clinicianLoginState as AuthViewModel.LoginState.Error).message
                // Add helpful hint for patient trying to login as clinician
                val hint = if (error.contains("Invalid credentials", ignoreCase = true) ||
                    error.contains("Login failed", ignoreCase = true)) {
                    "\n\nTip: Make sure you are using your Clinician Registration ID. If you are a patient, please use the Patient tab."
                } else {
                    ""
                }
                errorMessage = error + hint
                authViewModel.resetClinicianLoginState()
                isLoading = false
            }
            else -> {}
        }
    }

    // ✅ FIX: Add imePadding and navigationBarsPadding for proper keyboard handling
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthLavenderBackground)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AuthLavenderPrimary, AuthLavenderAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sahay Mochan",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AuthTextPrimary
            )

            Text(
                text = if (isLoginMode) "Welcome back!" else "Create new account",
                fontSize = 14.sp,
                color = AuthTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Role selector (only in login mode)
            if (isLoginMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedRole == "patient",
                        onClick = { selectedRole = "patient" },
                        label = { Text("Patient") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AuthLavenderPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    FilterChip(
                        selected = selectedRole == "clinician",
                        onClick = { selectedRole = "clinician" },
                        label = { Text("Clinician") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AuthLavenderPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Error Message
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = AuthErrorRed.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = AuthErrorRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            errorMessage!!,
                            fontSize = 13.sp,
                            color = AuthErrorRed,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = AuthErrorRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Login Form
            if (isLoginMode) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            if (selectedRole == "patient") "Patient Login" else "Clinician Login",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuthTextPrimary
                        )

                        OutlinedTextField(
                            value = loginRegId,
                            onValueChange = { loginRegId = it },
                            label = { Text("Registration ID") },
                            leadingIcon = { Icon(Icons.Default.Badge, null, tint = AuthLavenderPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuthLavenderPrimary,
                                unfocusedBorderColor = AuthLavenderAccent.copy(alpha = 0.5f)
                            )
                        )

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = AuthLavenderPrimary) },
                            trailingIcon = {
                                IconButton(onClick = { loginPasswordVisible = !loginPasswordVisible }) {
                                    Icon(
                                        if (loginPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (loginPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuthLavenderPrimary,
                                unfocusedBorderColor = AuthLavenderAccent.copy(alpha = 0.5f)
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (selectedRole == "patient") {
                                    scope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        val result = performLogin(
                                            context = context,
                                            registrationId = loginRegId,
                                            password = loginPassword,
                                            expectedRole = "patient"
                                        )
                                        isLoading = false
                                        if (result.first != null) {
                                            onLoginSuccess(result.first!!)
                                        } else {
                                            errorMessage = result.second ?: "Login failed"
                                        }
                                    }
                                } else {
                                    if (loginRegId.isNotBlank() && loginPassword.isNotBlank()) {
                                        isLoading = true
                                        errorMessage = null
                                        authViewModel.loginClinician(loginRegId, loginPassword)
                                    } else {
                                        errorMessage = "Please enter Registration ID and Password"
                                    }
                                }
                            },
                            enabled = loginRegId.isNotBlank() && loginPassword.isNotBlank() && !isLoading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(
                                    Brush.linearGradient(colors = listOf(AuthLavenderPrimary, AuthLavenderAccent)),
                                    RoundedCornerShape(14.dp)
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Login", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }

                        // Clinician Registration Link & Hint Text
                        if (selectedRole == "clinician") {
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Clinician login requires the Registration ID sent to your registered email. Patient accounts cannot log in here.",
                                fontSize = 12.sp,
                                color = AuthTextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Don't have a clinician account? ",
                                    color = AuthTextSecondary,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Register",
                                    color = AuthLavenderPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        navController.navigate(Screen.CLINICIAN_REGISTER)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // ========== PATIENT SIGNUP FORM (Fully Scrollable – outer scroll handles it) ==========
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Patient Registration", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = AuthTextPrimary)

                        OutlinedTextField(
                            value = signupRegId,
                            onValueChange = { signupRegId = it },

                            label = { Text("Patient ID *") },

                            leadingIcon = { Icon(Icons.Default.Badge, null, tint = AuthLavenderPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuthLavenderPrimary,
                                unfocusedBorderColor = AuthLavenderAccent.copy(alpha = 0.5f)
                            ),
                            isError = signupRegId.isNotBlank() && signupRegId.length < 3,
                            supportingText = {
                                if (signupRegId.isNotBlank() && signupRegId.length < 3) {
                                    Text("Registration ID must be at least 3 characters")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = signupName,
                            onValueChange = { signupName = it },
                            label = { Text("Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = AuthLavenderPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuthLavenderPrimary,
                                unfocusedBorderColor = AuthLavenderAccent.copy(alpha = 0.5f)
                            )
                        )

                        OutlinedTextField(
                            value = signupEmail,
                            onValueChange = { signupEmail = it },
                            label = { Text("Email *") },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = AuthLavenderPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuthLavenderPrimary,
                                unfocusedBorderColor = AuthLavenderAccent.copy(alpha = 0.5f)
                            )
                        )

                        OutlinedTextField(
                            value = signupAge,
                            onValueChange = { signupAge = it },
                            label = { Text("Age *") },
                            leadingIcon = { Icon(Icons.Default.Cake, null, tint = AuthLavenderPrimary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuthLavenderPrimary,
                                unfocusedBorderColor = AuthLavenderAccent.copy(alpha = 0.5f)
                            )
                        )

                        var expanded by remember { mutableStateOf(false) }
                        val genders = listOf("Male", "Female", "Other", "Prefer not to say")
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = signupGender,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender *") },
                                leadingIcon = { Icon(Icons.Default.Person, null, tint = AuthLavenderPrimary) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AuthLavenderPrimary,
                                    unfocusedBorderColor = AuthLavenderAccent.copy(alpha = 0.5f)
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                genders.forEach { gender ->
                                    DropdownMenuItem(
                                        text = { Text(gender) },
                                        onClick = {
                                            signupGender = gender
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = signupPassword,
                            onValueChange = { signupPassword = it },
                            label = { Text("Password *") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = AuthLavenderPrimary) },
                            trailingIcon = {
                                IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                                    Icon(
                                        if (signupPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuthLavenderPrimary,
                                unfocusedBorderColor = AuthLavenderAccent.copy(alpha = 0.5f)
                            ),
                            isError = signupPassword.isNotBlank() && signupPassword.length < 6,
                            supportingText = {
                                if (signupPassword.isNotBlank() && signupPassword.length < 6) {
                                    Text("Password must be at least 6 characters")
                                }
                            }
                        )

                        OutlinedTextField(
                            value = signupConfirmPassword,
                            onValueChange = { signupConfirmPassword = it },
                            label = { Text("Confirm Password *") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = AuthLavenderPrimary) },
                            visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuthLavenderPrimary,
                                unfocusedBorderColor = AuthLavenderAccent.copy(alpha = 0.5f)
                            ),
                            isError = signupConfirmPassword.isNotBlank() && signupPassword != signupConfirmPassword,
                            supportingText = {
                                if (signupConfirmPassword.isNotBlank() && signupPassword != signupConfirmPassword) {
                                    Text("Passwords do not match")
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    if (signupRegId.isBlank()) {
                                        errorMessage = "Registration ID is required"
                                        return@launch
                                    }
                                    if (signupName.isBlank()) {
                                        errorMessage = "Name is required"
                                        return@launch
                                    }
                                    if (signupEmail.isBlank()) {
                                        errorMessage = "Email is required"
                                        return@launch
                                    }
                                    val ageInt = signupAge.toIntOrNull()
                                    if (ageInt == null || ageInt < 18) {
                                        errorMessage = "Age must be 18 or older"
                                        return@launch
                                    }
                                    if (signupGender.isBlank()) {
                                        errorMessage = "Gender is required"
                                        return@launch
                                    }
                                    if (signupPassword.length < 6) {
                                        errorMessage = "Password must be at least 6 characters"
                                        return@launch
                                    }
                                    if (signupPassword != signupConfirmPassword) {
                                        errorMessage = "Passwords do not match"
                                        return@launch
                                    }

                                    isLoading = true
                                    errorMessage = null

                                    val result = performSignup(
                                        registrationId = signupRegId,
                                        password = signupPassword,
                                        name = signupName,
                                        gender = signupGender,
                                        email = signupEmail,
                                        age = ageInt
                                    )

                                    isLoading = false

                                    if (result.first != null) {
                                        val loginResult = performLogin(
                                            context = context,
                                            registrationId = signupRegId,
                                            password = signupPassword,
                                            expectedRole = "patient"
                                        )
                                        if (loginResult.first != null) {
                                            onLoginSuccess(loginResult.first!!)
                                        } else {
                                            errorMessage = "Signup successful! Please login."
                                            isLoginMode = true
                                            loginRegId = signupRegId
                                            loginPassword = signupPassword
                                        }
                                    } else {
                                        errorMessage = result.second ?: "Signup failed"
                                    }
                                }
                            },
                            enabled = !isLoading &&
                                    signupRegId.isNotBlank() &&
                                    signupName.isNotBlank() &&
                                    signupEmail.isNotBlank() &&
                                    signupAge.isNotBlank() &&
                                    signupGender.isNotBlank() &&
                                    signupPassword.length >= 6 &&
                                    signupPassword == signupConfirmPassword,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize().background(
                                    Brush.linearGradient(colors = listOf(AuthLavenderPrimary, AuthLavenderAccent)),
                                    RoundedCornerShape(14.dp)
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Create Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        // Back to Login link inside signup card
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Already have an account? ", color = AuthTextSecondary, fontSize = 14.sp)
                            Text(
                                text = "Login",
                                color = AuthLavenderPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    isLoginMode = true
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ✅ FIX: Only show this outer toggle link when in login mode (prevents duplicate in signup)
            if (isLoginMode && selectedRole == "patient") {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Don't have an account? ",
                        color = AuthTextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Sign Up",
                        color = AuthLavenderPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            isLoginMode = false
                            errorMessage = null
                        }
                    )
                }
            }
        }
    }
}

// ==================== API FUNCTIONS (Patient only) – unchanged ====================

suspend fun performLogin(
    context: Context,
    registrationId: String,
    password: String,
    expectedRole: String = "patient"
): Pair<UserProfile?, String?> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("http://203.110.243.202:8000/login-user")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val requestBody = JSONObject().apply {
                put("registration_id", registrationId)
                put("password", password)
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            val response = if (responseCode == 200) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            connection.disconnect()

            if (responseCode == 200) {
                val json = JSONObject(response)
                val trialsObj = json.optJSONObject("trials")

                // Role validation – same as before
                if (expectedRole == "patient") {
                    val isPatient = trialsObj != null &&
                            trialsObj.has("depression") &&
                            trialsObj.has("anxiety")
                    if (!isPatient) {
                        return@withContext Pair(null, "This account is not a patient account. Please use the Clinician tab.")
                    }
                } else if (expectedRole == "clinician") {
                    val isClinician = trialsObj == null ||
                            (!trialsObj.has("depression") && !trialsObj.has("anxiety"))
                    if (!isClinician) {
                        return@withContext Pair(null, "This account is not a clinician account. Please use the Patient tab.")
                    }
                }

                val name = json.getString("name")
                val email = json.getString("email")
                val age = json.getInt("age")
                val gender = json.getString("gender")
                val regId = json.getString("registration_id")
                val anonymousId = json.optString("anonymous_id", generateAnonymousId(name, regId))

                saveUserSession(context, regId, name, email, age, gender, anonymousId)

                val profile = UserProfile(
                    name = name,
                    email = email,
                    registrationId = regId,
                    age = age,
                    gender = gender,
                    isLoggedIn = true,
                    anonymousId = anonymousId
                )
                Pair(profile, null)
            } else {
                // ✅ Parse backend error message from response (if any)
                val errorMsg = try {
                    val errorJson = JSONObject(response)
                    errorJson.optString("detail", errorJson.optString("message", "Login failed"))
                } catch (e: Exception) {
                    "Login failed. Please check your credentials."
                }
                Pair(null, errorMsg)
            }
        } catch (e: Exception) {
            Pair(null, "Connection failed: ${e.message}")
        }
    }
}

suspend fun performSignup(
    registrationId: String,
    password: String,
    name: String,
    gender: String,
    email: String,
    age: Int
): Pair<UserProfile?, String?> {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("http://203.110.243.202:8000/register-user")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val requestBody = JSONObject().apply {
                put("registration_id", registrationId)
                put("password", password)
                put("name", name)
                put("gender", gender)
                put("email", email)
                put("age", age)
                put("roll_no", registrationId)
                put("phone_no", JSONObject.NULL)
                put("is_underage", false)
                put("parent_name", JSONObject.NULL)
                put("parent_email", JSONObject.NULL)
            }

            connection.outputStream.use { os ->
                os.write(requestBody.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            val response = if (responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            connection.disconnect()

            if (responseCode in 200..299) {
                val anonymousId = generateAnonymousId(name, registrationId)
                val profile = UserProfile(
                    name = name,
                    email = email,
                    registrationId = registrationId,
                    age = age,
                    gender = gender,
                    isLoggedIn = false,
                    anonymousId = anonymousId
                )
                Pair(profile, null)
            } else {
                val errorMsg = try {
                    val json = JSONObject(response)
                    json.optString("detail", json.optString("message", response))
                } catch (e: Exception) {
                    response
                }
                Pair(null, "Signup failed: $errorMsg")
            }
        } catch (e: Exception) {
            Pair(null, "Connection failed: ${e.message}")
        }
    }
}

fun saveUserSession(
    context: Context,
    registrationId: String,
    name: String,
    email: String,
    age: Int,
    gender: String,
    anonymousId: String
) {
    val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putString("registration_id", registrationId)
        putString("name", name)
        putString("email", email)
        putInt("age", age)
        putString("gender", gender)
        putString("anonymous_id", anonymousId)
        putBoolean("is_logged_in", true)
        apply()
    }

    val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    userPrefs.edit().apply {
        putString("registration_id", registrationId)
        putString("user_name", name)
        putString("email", email)
        putInt("user_age", age)
        putString("user_gender", gender)
        putString("anonymous_id", anonymousId)
        putBoolean("is_logged_in", true)
        apply()
    }
}

fun generateAnonymousId(name: String, rollNo: String): String {
    return try {
        val input = "${name.lowercase().trim()}_${rollNo.lowercase().trim()}"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hash = md.digest(input.toByteArray())
        val hexString = hash.joinToString("") { "%02x".format(it) }
        "STU_${hexString.take(8).uppercase()}"
    } catch (e: Exception) {
        "STU_${System.currentTimeMillis().toString().takeLast(8)}"
    }
}