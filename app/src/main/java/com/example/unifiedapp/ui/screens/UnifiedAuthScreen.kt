package com.example.unifiedapp.ui.auth

import android.content.Context
import com.example.unifiedapp.theme.*
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

// Color Palette
val PurplePrimary = Color(0xFF8B5CF6)
val PurpleSecondary = Color(0xFFA78BFA)
val PurpleDark = Color(0xFF7C3AED)
val GradientPurple = Brush.linearGradient(colors = listOf(PurplePrimary, PurpleSecondary))
val GradientPurpleDark = Brush.linearGradient(colors = listOf(PurpleDark, PurplePrimary))

val TextPrimary = Color(0xFF1F2937)
val TextSecondary = Color(0xFF6B7280)
val TextTertiary = Color(0xFF9CA3AF)
val InputBgColor = Color(0xFFF9FAFB)
val BorderColor = Color(0xFFE5E7EB)
val ColorSuccess = Color(0xFF10B981)
val ColorError = Color(0xFFEF4444)

enum class AuthMode { LOGIN, SIGNUP }

data class UserProfile(
    val name: String = "",
    val email: String = "",
    val registrationId: String = "",
    val age: Int = 0,
    val gender: String = "",
    val isLoggedIn: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedAuthScreen(
    navController: NavController,
    onLoginSuccess: (UserProfile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Form fields
    var registrationId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    // UI states
    var isLoading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Touched states for validation
    var registrationIdTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmPasswordTouched by remember { mutableStateOf(false) }
    var nameTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var ageTouched by remember { mutableStateOf(false) }
    var genderTouched by remember { mutableStateOf(false) }

    // Validation functions
    fun validateRegistrationId(regId: String): String? {
        return when {
            regId.isBlank() -> "Registration ID is required"
            regId.contains(" ") -> "Registration ID cannot contain spaces"
            else -> null
        }
    }

    fun validatePassword(pwd: String): String? {
        return when {
            pwd.isBlank() -> "Password is required"
            pwd.length < 6 -> "Password must be at least 6 characters"
            pwd.contains(" ") -> "Password cannot contain spaces"
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
            !emailRegex.matches(email) -> "Enter a valid email address"
            else -> null
        }
    }

    fun validateAge(age: String): String? {
        val ageInt = age.toIntOrNull()
        return when {
            age.isBlank() -> "Age is required"
            ageInt == null -> "Please enter a valid number"
            ageInt < 18 -> "You must be at least 18 years old"
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

    val registrationIdError = validateRegistrationId(registrationId)
    val passwordError = validatePassword(password)
    val nameError = validateName(name)
    val emailError = validateEmail(email)
    val ageError = validateAge(age)
    val confirmPasswordError = validateConfirmPassword(password, confirmPassword)
    val genderError = if (genderTouched && gender.isBlank()) "Please select a gender" else null

    val isLoginValid = registrationId.isNotBlank() && password.isNotBlank() &&
            registrationIdError == null && passwordError == null

    val isSignupValid = registrationId.isNotBlank() && password.isNotBlank() &&
            confirmPassword.isNotBlank() && name.isNotBlank() &&
            email.isNotBlank() && age.isNotBlank() && gender.isNotBlank() &&
            registrationIdError == null && passwordError == null &&
            confirmPasswordError == null && nameError == null &&
            emailError == null && ageError == null && genderError == null

    suspend fun performLogin(regId: String, pwd: String): Pair<UserProfile?, String?> {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL("http://203.110.243.202:8000/login-user")
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Connection", "Keep-Alive")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                // Send request body
                val requestBody = JSONObject().apply {
                    put("registration_id", regId)
                    put("password", pwd)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                // Get response code first
                val responseCode = connection.responseCode
                println("Response Code: $responseCode")

                // Read response properly
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                } else {
                    connection.errorStream?.bufferedReader()?.use { reader ->
                        reader.readText()
                    } ?: ""
                }

                println("Response Body: $response")

                if (responseCode in 200..299 && response.isNotEmpty()) {
                    val json = JSONObject(response)
                    val profile = UserProfile(
                        name = json.getString("name"),
                        email = json.getString("email"),
                        registrationId = json.getString("registration_id"),
                        age = json.getInt("age"),
                        gender = json.getString("gender"),
                        isLoggedIn = true
                    )
                    return@withContext Pair(profile, null)
                } else {
                    return@withContext Pair(null, "Invalid credentials or server error")
                }
            } catch (e: java.net.SocketException) {
                println("Socket Exception: ${e.message}")
                return@withContext Pair(null, "Connection error. Please try again.")
            } catch (e: java.net.SocketTimeoutException) {
                println("Timeout Exception: ${e.message}")
                return@withContext Pair(null, "Connection timeout. Please try again.")
            } catch (e: Exception) {
                println("Exception: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
                return@withContext Pair(null, "Error: ${e.message}")
            } finally {
                connection?.disconnect()
            }
        }
    }

    suspend fun performSignup(
        regId: String,
        pwd: String,
        userName: String,
        userEmail: String,
        userAge: Int,
        userGender: String
    ): Pair<UserProfile?, String?> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://203.110.243.202:8000/register-user")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                // Create JSONObject directly (matching original Mochan format)
                val requestBody = JSONObject()
                requestBody.put("registration_id", regId)
                requestBody.put("password", pwd)
                requestBody.put("roll_no", regId)  // roll_no same as registration_id
                requestBody.put("name", userName)
                requestBody.put("gender", userGender)
                requestBody.put("email", userEmail)
                requestBody.put("age", userAge)
                requestBody.put("phone_no", "99${regId.take(8)}")  // Generate dummy phone number

                println("=== SIGNUP REQUEST ===")
                println("Request Body: ${requestBody.toString(2)}")

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                println("Response Code: $responseCode")
                println("Response Body: $response")
                connection.disconnect()

                if (responseCode in 200..299) {
                    val profile = UserProfile(
                        name = userName,
                        email = userEmail,
                        registrationId = regId,
                        age = userAge,
                        gender = userGender,
                        isLoggedIn = true
                    )
                    Pair(profile, null)
                } else if (responseCode == 400) {
                    if (response.contains("already exists", ignoreCase = true)) {
                        Pair(null, "Registration ID already exists")
                    } else if (response.contains("email", ignoreCase = true)) {
                        Pair(null, "Email already registered")
                    } else {
                        Pair(null, "Registration failed: $response")
                    }
                } else {
                    Pair(null, "Registration failed. Please try again.")
                }
            } catch (e: Exception) {
                println("Signup Error: ${e.message}")
                Pair(null, "Connection failed: ${e.message}")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PurplePrimary.copy(alpha = 0.05f), Color.White)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo/Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(brush = GradientPurple),
                contentAlignment = Alignment.Center
            ) {
                Text("🧠", fontSize = 44.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (authMode == AuthMode.LOGIN) "Welcome Back!" else "Create Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = if (authMode == AuthMode.LOGIN)
                    "Sign in to continue your wellness journey"
                else
                    "Join us to start your mental wellness journey",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Error Message
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = ColorError.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = ColorError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage!!, color = ColorError, fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Registration ID Field
            AuthTextField(
                value = registrationId,
                onValueChange = { registrationId = it; registrationIdTouched = true },
                label = "Registration ID / Roll Number",
                icon = Icons.Outlined.ConfirmationNumber,
                isError = registrationIdTouched && registrationIdError != null,
                errorText = if (registrationIdTouched) registrationIdError else null,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            AuthTextField(
                value = password,
                onValueChange = { password = it; passwordTouched = true },
                label = "Password",
                icon = Icons.Outlined.Lock,
                isPassword = true,
                showPassword = showPassword,
                onShowPasswordChange = { showPassword = it },
                isError = passwordTouched && passwordError != null,
                errorText = if (passwordTouched) passwordError else null,
                enabled = !isLoading
            )

            if (authMode == AuthMode.SIGNUP) {
                Spacer(modifier = Modifier.height(12.dp))

                // Confirm Password Field
                AuthTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; confirmPasswordTouched = true },
                    label = "Confirm Password",
                    icon = Icons.Outlined.Lock,
                    isPassword = true,
                    showPassword = showConfirmPassword,
                    onShowPasswordChange = { showConfirmPassword = it },
                    isError = confirmPasswordTouched && confirmPasswordError != null,
                    errorText = if (confirmPasswordTouched) confirmPasswordError else null,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Name Field
                AuthTextField(
                    value = name,
                    onValueChange = { name = it; nameTouched = true },
                    label = "Full Name",
                    icon = Icons.Outlined.Person,
                    isError = nameTouched && nameError != null,
                    errorText = if (nameTouched) nameError else null,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Email Field
                AuthTextField(
                    value = email,
                    onValueChange = { email = it; emailTouched = true },
                    label = "Email",
                    icon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email,
                    isError = emailTouched && emailError != null,
                    errorText = if (emailTouched) emailError else null,
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Age Field
                    AuthTextField(
                        value = age,
                        onValueChange = { age = it; ageTouched = true },
                        label = "Age (18+)",
                        icon = Icons.Default.Numbers,
                        keyboardType = KeyboardType.Number,
                        isError = ageTouched && ageError != null,
                        errorText = if (ageTouched) ageError else null,
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    )

                    // Gender Dropdown
                    GenderDropdown(
                        selectedOption = gender,
                        onOptionSelected = { gender = it; genderTouched = true },
                        label = "Gender",
                        enabled = !isLoading,
                        isError = genderTouched && genderError != null,
                        errorText = if (genderTouched) genderError else null,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = {
                    if (authMode == AuthMode.LOGIN) {
                        registrationIdTouched = true
                        passwordTouched = true
                        if (!isLoginValid) return@Button

                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            val (profile, error) = performLogin(registrationId, password)
                            isLoading = false

                            if (profile != null) {
                                Toast.makeText(context, "Welcome back, ${profile.name}!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(profile)
                            } else {
                                errorMessage = error ?: "Login failed"
                            }
                        }
                    } else {
                        registrationIdTouched = true
                        passwordTouched = true
                        confirmPasswordTouched = true
                        nameTouched = true
                        emailTouched = true
                        ageTouched = true
                        genderTouched = true

                        if (!isSignupValid) return@Button

                        val ageInt = age.toIntOrNull() ?: 0
                        if (ageInt < 18) {
                            errorMessage = "You must be at least 18 years old to register"
                            return@Button
                        }

                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            val (profile, error) = performSignup(
                                registrationId, password, name, email, ageInt, gender
                            )
                            isLoading = false

                            if (profile != null) {
                                Toast.makeText(context, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(profile)
                            } else {
                                errorMessage = error ?: "Registration failed"
                            }
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = if ((if (authMode == AuthMode.LOGIN) isLoginValid else isSignupValid) && !isLoading)
                                GradientPurple
                            else
                                Brush.horizontalGradient(listOf(BorderColor, BorderColor)),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = Color.White)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (authMode == AuthMode.LOGIN) "Sign In" else "Create Account",
                                color = if ((if (authMode == AuthMode.LOGIN) isLoginValid else isSignupValid)) Color.White else TextTertiary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = if ((if (authMode == AuthMode.LOGIN) isLoginValid else isSignupValid)) Color.White else TextTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Switch between Login and Signup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Don't have an account? " else "Already have an account? ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Sign Up" else "Sign In",
                    color = PurplePrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        authMode = if (authMode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN
                        errorMessage = null
                    }
                )
            }

            if (authMode == AuthMode.SIGNUP) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "By creating an account, you agree to our Terms and Privacy Policy",
                    fontSize = 11.sp,
                    color = TextTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onShowPasswordChange: ((Boolean) -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorText: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text(label, color = TextTertiary) },
            leadingIcon = {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (value.isNotBlank() && !isError) PurplePrimary else TextTertiary,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = if (isPassword && onShowPasswordChange != null) {
                {
                    IconButton(onClick = { onShowPasswordChange(!showPassword) }) {
                        Icon(
                            if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = TextPrimary
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            enabled = enabled,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PurplePrimary,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = InputBgColor,
                errorBorderColor = ColorError,
                errorContainerColor = ColorError.copy(alpha = 0.05f),
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = PurplePrimary
            )
        )
        if (errorText != null) {
            Text(errorText, fontSize = 11.sp, color = ColorError, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

@Composable
fun GenderDropdown(
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Male", "Female", "Other", "Prefer not to say")

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(when {
                    isError -> ColorError.copy(alpha = 0.05f)
                    expanded || selectedOption.isNotEmpty() -> Color.White
                    else -> InputBgColor
                })
                .border(
                    width = 1.dp,
                    color = when {
                        isError -> ColorError
                        expanded -> PurplePrimary
                        else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = when {
                        selectedOption.isNotEmpty() && !isError -> PurplePrimary
                        isError -> ColorError
                        else -> TextTertiary
                    },
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (selectedOption.isEmpty()) label else selectedOption,
                    color = when {
                        selectedOption.isEmpty() -> TextTertiary
                        isError -> ColorError
                        else -> TextPrimary
                    },
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = when {
                        expanded || isError -> if (isError) ColorError else PurplePrimary
                        else -> TextTertiary
                    },
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        if (errorText != null) {
            Text(errorText, fontSize = 11.sp, color = ColorError, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color.White)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            color = if (option == selectedOption) PurplePrimary else TextPrimary,
                            fontWeight = if (option == selectedOption) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(
                            when (option.lowercase()) {
                                "male" -> Icons.Default.Male
                                "female" -> Icons.Default.Female
                                else -> Icons.Outlined.Person
                            },
                            contentDescription = null,
                            tint = if (option == selectedOption) PurplePrimary else TextTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
    }
}