package com.example.unifiedapp.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.RegisterState
import com.example.unifiedapp.ui.views.generateAnonymousId
import com.example.unifiedapp.ui.views.UserPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import java.util.regex.Pattern
import com.example.unifiedapp.ui.navigation.Screen
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    userPreferences: UserPreferences,
    navController: NavController,
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegistrationSuccess: () -> Unit
) {
    Log.d("REGISTER_DEBUG", "🎯 RegisterScreen composed/recomposed")

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Popup states for terms and privacy
    var showTermsPopup by remember { mutableStateOf(false) }
    var showPrivacyPopup by remember { mutableStateOf(false) }

    // Field values
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var id by remember { mutableStateOf("") }
    var age by remember { mutableStateOf<Int?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Touched states for validation
    var nameTouched by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    var genderTouched by remember { mutableStateOf(false) }
    var idTouched by remember { mutableStateOf(false) }
    var ageTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmPasswordTouched by remember { mutableStateOf(false) }

    // Password validation states
    var passwordStrength by remember { mutableStateOf(PasswordStrength.WEAK) }
    var isPasswordValid by remember { mutableStateOf(false) }

    // Form submission attempted flag
    var formSubmitted by remember { mutableStateOf(false) }

    val genderOptions = listOf("Male", "Female", "Other")

    // Collect states from ViewModel
    val registerState by viewModel.registerState.collectAsStateWithLifecycle()
    Log.d("REGISTER_DEBUG", "📊 registerState collected: $registerState")

    // Handle IME padding
    val imeInsets = WindowInsets.ime.asPaddingValues()
    val navigationBarInsets = WindowInsets.navigationBars.asPaddingValues()
    val bottomPadding = with(LocalDensity.current) {
        maxOf(imeInsets.calculateBottomPadding(), navigationBarInsets.calculateBottomPadding(), 16.dp)
    }

    // Handle registration state changes and navigate on success
    LaunchedEffect(registerState) {
        when (registerState) {
            is RegisterState.Success -> {
                val success = registerState as RegisterState.Success
                Log.d("REGISTER_DEBUG", "✅ INSIDE SUCCESS CASE")
                Log.d("REGISTER_DEBUG", "Success message: ${success.message}")

                Toast.makeText(context, success.message, Toast.LENGTH_SHORT).show()
                Log.d("REGISTER_DEBUG", "Toast shown")

                // ⚠️ DON'T clear state here - move it after navigation!
                // viewModel.clearRegisterState()  // <- REMOVE THIS LINE

                delay(500)
                Log.d("REGISTER_DEBUG", "Delay completed - now navigating")

                // Call the callback
                onRegistrationSuccess()
                Log.d("REGISTER_DEBUG", "onRegistrationSuccess callback called")

                // Navigate to home
                navController.popBackStack(0, false)
                Log.d("REGISTER_DEBUG", "popBackStack completed")

                navController.navigate(Screen.Home.route) {
                    launchSingleTop = true
                }
                Log.d("REGISTER_DEBUG", "Navigation to Home completed")

                // ✅ Clear state AFTER navigation
                viewModel.clearRegisterState()
                Log.d("REGISTER_DEBUG", "State cleared after navigation")
            }
            is RegisterState.Error -> {
                val error = registerState as RegisterState.Error
                Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                viewModel.clearRegisterState()
            }
            else -> {
                Log.d("REGISTER_DEBUG", "Other state: $registerState")
            }
        }
    }

    // Validation functions
    fun isNameValid(): Boolean = name.isNotBlank()
    fun isEmailValid(): Boolean = email.isNotBlank() && Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    ).matcher(email).matches()
    fun isIdValid(): Boolean = id.isNotBlank() && id.matches(Regex("^[a-zA-Z0-9]+$"))

    fun isAgeValid(): Boolean {
        val currentAge = age
        return currentAge != null && currentAge in 18..120
    }

    fun isGenderValid(): Boolean = gender.isNotBlank()
    fun isPasswordStrongEnough(): Boolean = password.length >= 6 && !password.contains(" ")
    fun doPasswordsMatch(): Boolean = password == confirmPassword

    fun canRegister(): Boolean {
        return isNameValid() &&
                isEmailValid() &&
                isIdValid() &&
                isAgeValid() &&
                isGenderValid() &&
                isPasswordStrongEnough() &&
                doPasswordsMatch()
    }

    // Complete registration directly
    val completeRegistration: () -> Unit = {
        Log.d("REGISTER_DEBUG", "🟢 completeRegistration called")
        keyboardController?.hide()
        formSubmitted = true

        nameTouched = true
        emailTouched = true
        genderTouched = true
        idTouched = true
        ageTouched = true
        passwordTouched = true
        confirmPasswordTouched = true

        if (canRegister()) {
            val roll_no = generateAnonymousId(name, id)
            Log.d("REGISTER_DEBUG", "Calling registerWithoutPhone with roll_no: $roll_no")
            viewModel.registerWithoutPhone(
                registrationId = id,
                password = password,
                name = name,
                gender = gender,
                email = email,
                age = age!!,
                roll_no = roll_no
            )
        } else {
            Log.d("REGISTER_DEBUG", "Registration validation failed")
            when {
                !isNameValid() -> Toast.makeText(context, "Please enter your full name", Toast.LENGTH_SHORT).show()
                !isEmailValid() -> Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                !isIdValid() -> Toast.makeText(context, "Registration ID must be alphanumeric", Toast.LENGTH_SHORT).show()
                !isAgeValid() -> Toast.makeText(context, "You must be 18 years or older to register", Toast.LENGTH_LONG).show()
                !isGenderValid() -> Toast.makeText(context, "Please select your gender", Toast.LENGTH_SHORT).show()
                !isPasswordStrongEnough() -> Toast.makeText(context, "Password must be at least 6 characters with no spaces", Toast.LENGTH_SHORT).show()
                !doPasswordsMatch() -> Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                else -> Toast.makeText(context, "Please fill all required fields correctly", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Update password validation
    LaunchedEffect(password) {
        when {
            password.isEmpty() -> {
                passwordStrength = PasswordStrength.WEAK
                isPasswordValid = false
            }
            password.length < 6 -> {
                passwordStrength = PasswordStrength.WEAK
                isPasswordValid = false
            }
            password.contains(" ") -> {
                passwordStrength = PasswordStrength.WEAK
                isPasswordValid = false
            }
            password.matches(Regex("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$")) -> {
                passwordStrength = PasswordStrength.STRONG
                isPasswordValid = true
            }
            password.matches(Regex("^(?=.*[A-Za-z])[A-Za-z]{6,}$")) -> {
                passwordStrength = PasswordStrength.MEDIUM
                isPasswordValid = true
            }
            else -> {
                passwordStrength = PasswordStrength.WEAK
                isPasswordValid = false
            }
        }
    }

    // Show Terms Popup when needed
    if (showTermsPopup) {
        TermsAndConditionsPopup(onDismiss = { showTermsPopup = false })
    }

    // Show Privacy Popup when needed
    if (showPrivacyPopup) {
        PrivacyPolicyPopup(onDismiss = { showPrivacyPopup = false })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SageGreenGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // Registration Form Card
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = LogoAccentGreen,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.headlineMedium,
                        color = LogoDarkBrown,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Begin your wellness journey",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SoftGreyText
                    )

                    Spacer(Modifier.height(24.dp))

                    // Name Field
                    PeacefulInput(
                        value = name,
                        onValueChange = { name = it; nameTouched = true },
                        onFocusLost = { nameTouched = true },
                        label = "Full Name",
                        icon = Icons.Default.Person,
                        isRequired = true,
                        showError = (nameTouched || formSubmitted) && !isNameValid(),
                        errorMessage = if ((nameTouched || formSubmitted) && name.isBlank()) "Name is required" else null,
                        enabled = registerState !is RegisterState.Loading
                    )

                    Spacer(Modifier.height(12.dp))

                    // Registration ID
                    PeacefulInput(
                        value = id,
                        onValueChange = {
                            id = it.filter { char -> char.isLetterOrDigit() }
                            idTouched = true
                        },
                        onFocusLost = { idTouched = true },
                        label = "Registration ID",
                        icon = Icons.Default.Badge,
                        isRequired = true,
                        showError = (idTouched || formSubmitted) && !isIdValid(),
                        errorMessage = when {
                            (idTouched || formSubmitted) && id.isBlank() -> "Registration ID is required"
                            (idTouched || formSubmitted) && !id.matches(Regex("^[a-zA-Z0-9]+$")) -> "ID must be alphanumeric (letters and numbers only)"
                            else -> null
                        },
                        enabled = registerState !is RegisterState.Loading,
                        supportingText = "Only letters and numbers allowed"
                    )

                    Spacer(Modifier.height(12.dp))

                    // Age Field (with 18+ validation)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val currentAge = age

                        PeacefulIntInput(
                            value = age,
                            onValueChange = { age = it; ageTouched = true },
                            onFocusLost = { ageTouched = true },
                            label = "Age",
                            icon = Icons.Default.Info,
                            showError = (ageTouched || formSubmitted) && !isAgeValid(),
                            errorMessage = when {
                                (ageTouched || formSubmitted) && currentAge == null -> "Age is required"
                                (ageTouched || formSubmitted) && currentAge != null && currentAge < 18 -> "You must be 18 years or older to register"
                                (ageTouched || formSubmitted) && currentAge != null && currentAge > 120 -> "Enter valid age (18-120)"
                                else -> null
                            },
                            enabled = registerState !is RegisterState.Loading
                        )

                        if (!(ageTouched || formSubmitted) && age == null) {
                            Text(
                                text = "Minimum age: 18 years",
                                fontSize = 11.sp,
                                color = SoftGreyText,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Email Field
                    PeacefulInput(
                        value = email,
                        onValueChange = { email = it; emailTouched = true },
                        onFocusLost = { emailTouched = true },
                        label = "Email Address",
                        icon = Icons.Default.Email,
                        isRequired = true,
                        showError = (emailTouched || formSubmitted) && !isEmailValid(),
                        errorMessage = when {
                            (emailTouched || formSubmitted) && email.isBlank() -> "Email is required"
                            (emailTouched || formSubmitted) && !Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matcher(email).matches() -> "Enter a valid email address"
                            else -> null
                        },
                        enabled = registerState !is RegisterState.Loading
                    )

                    Spacer(Modifier.height(12.dp))

                    // Password with strength indicator
                    PasswordInputWithStrength(
                        value = password,
                        onValueChange = {
                            password = it.filter { char -> char != ' ' }
                            passwordTouched = true
                        },
                        onFocusLost = { passwordTouched = true },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isRequired = true,
                        showError = (passwordTouched || formSubmitted) && !isPasswordStrongEnough(),
                        errorMessage = when {
                            (passwordTouched || formSubmitted) && password.isBlank() -> "Password is required"
                            (passwordTouched || formSubmitted) && password.contains(" ") -> "Spaces are not allowed in password"
                            (passwordTouched || formSubmitted) && password.length < 6 -> "Password must be at least 6 characters"
                            else -> null
                        },
                        enabled = registerState !is RegisterState.Loading,
                        strength = passwordStrength,
                        showStrengthIndicator = password.isNotEmpty()
                    )

                    Spacer(Modifier.height(12.dp))

                    // Confirm Password
                    PeacefulInput(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it.filter { char -> char != ' ' }
                            confirmPasswordTouched = true
                        },
                        onFocusLost = { confirmPasswordTouched = true },
                        label = "Confirm Password",
                        icon = Icons.Default.Lock,
                        isRequired = true,
                        isPassword = true,
                        showError = (confirmPasswordTouched || formSubmitted) && (confirmPassword.isBlank() || password != confirmPassword),
                        errorMessage = when {
                            (confirmPasswordTouched || formSubmitted) && confirmPassword.isBlank() -> "Please confirm your password"
                            (confirmPasswordTouched || formSubmitted) && password != confirmPassword -> "Passwords don't match"
                            else -> null
                        },
                        enabled = registerState !is RegisterState.Loading
                    )

                    Spacer(Modifier.height(12.dp))

                    // Gender Dropdown
                    PeacefulDropdown(
                        value = gender,
                        onValueChange = { gender = it; genderTouched = true },
                        onExpandedChange = { genderTouched = true },
                        label = "Gender",
                        icon = Icons.Default.Face,
                        options = genderOptions,
                        showError = (genderTouched || formSubmitted) && !isGenderValid(),
                        errorMessage = if ((genderTouched || formSubmitted) && !isGenderValid()) "Please select a gender" else null,
                        enabled = registerState !is RegisterState.Loading
                    )

                    Spacer(Modifier.height(16.dp))

                    // Terms and Conditions
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    ) {
                        Text("By creating an account, you agree to our", fontSize = 12.sp, color = SoftGreyText, textAlign = TextAlign.Center)
                        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("Terms & Conditions", fontSize = 12.sp, color = LogoOrangeMuted, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { showTermsPopup = true }.padding(horizontal = 4.dp, vertical = 2.dp))
                            Text("and", fontSize = 12.sp, color = SoftGreyText, modifier = Modifier.padding(horizontal = 4.dp))
                            Text("Privacy Policy", fontSize = 12.sp, color = LogoOrangeMuted, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline, modifier = Modifier.clickable { showPrivacyPopup = true }.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Text("Your data is protected and never shared", fontSize = 11.sp, color = SoftGreyText.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Register Button
                    Button(
                        onClick = completeRegistration,
                        enabled = registerState !is RegisterState.Loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                    ) {
                        if (registerState is RegisterState.Loading) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Login link
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("Already have an account? ", color = SoftGreyText, fontSize = 14.sp)
                        TextButton(onClick = { keyboardController?.hide(); onNavigateToLogin() }) {
                            Text("Sign In", color = LogoOrangeMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // Loading overlay
        if (registerState is RegisterState.Loading) {
            Box(modifier = Modifier.fillMaxSize().background(LogoDarkBrown.copy(alpha = 0.3f))) {
                CircularProgressIndicator(color = LogoAccentGreen, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

@Composable
fun PasswordInputWithStrength(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit = {},
    label: String,
    icon: ImageVector,
    isRequired: Boolean = false,
    showError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    strength: PasswordStrength,
    showStrengthIndicator: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                if (!focusState.isFocused && wasFocused) onFocusLost()
                wasFocused = focusState.isFocused
            },
            label = { Text(label, color = SoftGreyText, fontSize = 14.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = LogoAccentGreen, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                Row {
                    if (showStrengthIndicator && value.isNotEmpty() && !showError) {
                        when (strength) {
                            PasswordStrength.STRONG -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LogoAccentGreen, modifier = Modifier.size(20.dp))
                            PasswordStrength.MEDIUM -> Icon(Icons.Default.Info, contentDescription = null, tint = LogoOrangeMuted, modifier = Modifier.size(20.dp))
                            else -> {}
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(20.dp))
                    }
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            isError = showError,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (showError) Color.Red else LogoAccentGreen,
                unfocusedBorderColor = if (showError) Color.Red.copy(alpha = 0.5f) else Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = LogoSoftGreen,
                cursorColor = LogoAccentGreen
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        if (showStrengthIndicator && value.isNotEmpty() && !showError) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f)) {
                    repeat(3) { index ->
                        Box(modifier = Modifier.weight(1f).height(4.dp).padding(end = if (index < 2) 4.dp else 0.dp).background(
                            color = when {
                                value.length < 6 -> InactiveGrey.copy(alpha = 0.3f)
                                index == 0 -> when (strength) { PasswordStrength.WEAK -> Color.Red; PasswordStrength.MEDIUM -> LogoOrangeMuted; PasswordStrength.STRONG -> LogoAccentGreen }
                                index == 1 -> when (strength) { PasswordStrength.WEAK -> InactiveGrey.copy(alpha = 0.3f); PasswordStrength.MEDIUM -> LogoOrangeMuted; PasswordStrength.STRONG -> LogoAccentGreen }
                                else -> when (strength) { PasswordStrength.STRONG -> LogoAccentGreen; else -> InactiveGrey.copy(alpha = 0.3f) }
                            }, shape = RoundedCornerShape(2.dp)
                        ))
                    }
                }
                Text(when { value.length < 6 -> "Too short"; strength == PasswordStrength.WEAK -> "Weak"; strength == PasswordStrength.MEDIUM -> "Medium"; else -> "Strong" }, color = when { value.length < 6 -> Color.Red; strength == PasswordStrength.WEAK -> Color.Red; strength == PasswordStrength.MEDIUM -> LogoOrangeMuted; else -> LogoAccentGreen }, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (showError && errorMessage != null) {
            Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

@Composable
fun PeacefulInput(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit = {},
    label: String,
    icon: ImageVector,
    isRequired: Boolean = false,
    isPassword: Boolean = false,
    showError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    supportingText: String? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var wasFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                if (!focusState.isFocused && wasFocused) onFocusLost()
                wasFocused = focusState.isFocused
            },
            label = { Text(label, color = SoftGreyText, fontSize = 14.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = LogoAccentGreen, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { passwordVisible = !passwordVisible }, modifier = Modifier.size(48.dp)) {
                        Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(20.dp))
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(12.dp),
            isError = showError,
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (showError) Color.Red else LogoAccentGreen,
                unfocusedBorderColor = if (showError) Color.Red.copy(alpha = 0.5f) else Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = LogoSoftGreen,
                cursorColor = LogoAccentGreen
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        if (supportingText != null && !showError) {
            Text(supportingText, color = SoftGreyText.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }

        if (showError && errorMessage != null) {
            Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

@Composable
fun PeacefulIntInput(
    value: Int?,
    onValueChange: (Int?) -> Unit,
    onFocusLost: () -> Unit = {},
    label: String,
    icon: ImageVector,
    showError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    var wasFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = { newText ->
                if (newText.isEmpty() || newText.all { it.isDigit() }) {
                    text = newText
                    onValueChange(newText.toIntOrNull())
                }
            },
            modifier = Modifier.fillMaxWidth().onFocusChanged { focusState ->
                if (!focusState.isFocused && wasFocused) onFocusLost()
                wasFocused = focusState.isFocused
            },
            label = { Text(label, color = SoftGreyText, fontSize = 14.sp) },
            leadingIcon = { Icon(icon, contentDescription = null, tint = LogoAccentGreen, modifier = Modifier.size(20.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = showError,
            singleLine = true,
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (showError) Color.Red else LogoAccentGreen,
                unfocusedBorderColor = if (showError) Color.Red.copy(alpha = 0.5f) else Color.Transparent,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = LogoSoftGreen,
                cursorColor = LogoAccentGreen
            ),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        if (showError && errorMessage != null) {
            Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeacefulDropdown(
    value: String,
    onValueChange: (String) -> Unit,
    onExpandedChange: () -> Unit = {},
    label: String,
    icon: ImageVector,
    options: List<String>,
    showError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = {
                if (enabled) {
                    expanded = !expanded
                    onExpandedChange()
                }
            }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text(label, color = SoftGreyText, fontSize = 14.sp) },
                leadingIcon = { Icon(icon, contentDescription = null, tint = LogoAccentGreen, modifier = Modifier.size(20.dp)) },
                trailingIcon = { Icon(imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null, tint = SoftGreyText, modifier = Modifier.size(24.dp)) },
                isError = showError,
                enabled = enabled,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (showError) Color.Red else LogoAccentGreen,
                    unfocusedBorderColor = if (showError) Color.Red.copy(alpha = 0.5f) else Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = LogoSoftGreen,
                    cursorColor = LogoAccentGreen
                ),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth().background(CardWhite, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                containerColor = CardWhite
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 14.sp, color = LogoDarkBrown, fontWeight = if (option == value) FontWeight.Medium else FontWeight.Normal) },
                        onClick = { onValueChange(option); expanded = false },
                        colors = MenuDefaults.itemColors(textColor = LogoDarkBrown, leadingIconColor = LogoAccentGreen),
                        modifier = Modifier.fillMaxWidth().background(if (option == value) LogoSoftGreen else Color.Transparent)
                    )
                    if (index < options.size - 1) {
                        Divider(color = SoftGreyText.copy(alpha = 0.2f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }

        if (showError && errorMessage != null) {
            Text(errorMessage, color = Color.Red, style = MaterialTheme.typography.bodySmall, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
        }
    }
}