package com.example.unifiedapp.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.unifiedapp.ui.views.AuthViewModel
import kotlinx.coroutines.launch
import com.example.unifiedapp.ui.views.PasswordResetState



@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    onPasswordReset: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Step tracking: 1 = Phone number, 2 = OTP & Reset
    var currentStep by remember { mutableStateOf(1) }

    // Form fields
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // UI states
    var isLoading by remember { mutableStateOf(false) }
    var otpSentMessage by remember { mutableStateOf<String?>(null) }

    // Collect password reset state from ViewModel
    val passwordResetState by viewModel.passwordResetState.collectAsStateWithLifecycle()

    // Handle password reset state changes
    LaunchedEffect(passwordResetState) {
        when (passwordResetState) {
            is PasswordResetState.OtpSent -> {  // Remove AuthViewModel. prefix
                isLoading = false
                otpSentMessage = "OTP sent successfully! Please check your phone."
                currentStep = 2
            }
            is PasswordResetState.Success -> {
                isLoading = false
                Toast.makeText(context, "Password reset successful! Please login.", Toast.LENGTH_LONG).show()
                onPasswordReset()
            }
            is PasswordResetState.Error -> {
                isLoading = false
                val errorMsg = (passwordResetState as PasswordResetState.Error).message
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    // Validation functions
    fun isPhoneValid(): Boolean = phoneNumber.matches(Regex("^[6-9]\\d{9}$"))
    fun isPasswordValid(): Boolean = newPassword.length >= 6 && !newPassword.contains(" ")
    fun doPasswordsMatch(): Boolean = newPassword == confirmPassword

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SageGreenGradient)
    ) {
        // Decorative background elements
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Icon
                    Icon(
                        Icons.Default.LockReset,
                        contentDescription = null,
                        tint = LogoOrangeMuted,
                        modifier = Modifier.size(56.dp)
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = if (currentStep == 1) "Reset Password" else "Create New Password",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = LogoDarkBrown
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = if (currentStep == 1) {
                            "Enter your registered phone number to receive OTP"
                        } else {
                            "Enter the OTP and create your new password"
                        },
                        fontSize = 14.sp,
                        color = SoftGreyText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(24.dp))

                    if (currentStep == 1) {
                        // STEP 1: Phone Number Input
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = {
                                phoneNumber = it.filter { char -> char.isDigit() }.take(10)
                            },
                            label = { Text("Phone Number") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = LogoAccentGreen
                                )
                            },
                            placeholder = { Text("Enter 10-digit mobile number") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = phoneNumber.isNotEmpty() && !isPhoneValid(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LogoAccentGreen,
                                unfocusedBorderColor = SoftGreyText.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = LogoSoftGreen
                            ),
                            singleLine = true
                        )

                        if (phoneNumber.isNotEmpty() && !isPhoneValid()) {
                            Text(
                                text = "Please enter a valid 10-digit mobile number",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Send OTP Button
                        Button(
                            onClick = {
                                if (isPhoneValid()) {
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            viewModel.forgotPassword(phoneNumber)
                                        } catch (e: Exception) {
                                            isLoading = false
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Send OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (otpSentMessage != null) {
                            Spacer(Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = LogoAccentGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = otpSentMessage!!,
                                    fontSize = 12.sp,
                                    color = LogoAccentGreen
                                )
                            }
                        }
                    } else {
                        // STEP 2: OTP and New Password
                        // OTP Field
                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { otpCode = it.filter { char -> char.isDigit() }.take(6) },
                            label = { Text("OTP Code") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = LogoAccentGreen
                                )
                            },
                            placeholder = { Text("Enter 6-digit OTP") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LogoAccentGreen,
                                unfocusedBorderColor = SoftGreyText.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = LogoSoftGreen
                            ),
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))

                        // New Password Field
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it.filter { char -> char != ' ' } },
                            label = { Text("New Password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = LogoAccentGreen
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = SoftGreyText
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = newPassword.isNotEmpty() && !isPasswordValid(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LogoAccentGreen,
                                unfocusedBorderColor = SoftGreyText.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = LogoSoftGreen
                            ),
                            singleLine = true
                        )

                        if (newPassword.isNotEmpty() && !isPasswordValid()) {
                            Text(
                                text = "Password must be at least 6 characters and no spaces",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // Confirm Password Field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it.filter { char -> char != ' ' } },
                            label = { Text("Confirm Password") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = LogoAccentGreen
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = SoftGreyText
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            isError = confirmPassword.isNotEmpty() && !doPasswordsMatch(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LogoAccentGreen,
                                unfocusedBorderColor = SoftGreyText.copy(alpha = 0.3f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = LogoSoftGreen
                            ),
                            singleLine = true
                        )

                        if (confirmPassword.isNotEmpty() && !doPasswordsMatch()) {
                            Text(
                                text = "Passwords do not match",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // Resend OTP link
                        TextButton(
                            onClick = {
                                scope.launch {
                                    isLoading = true
                                    try {
                                        viewModel.forgotPassword(phoneNumber)
                                    } catch (e: Exception) {
                                        isLoading = false
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Resend OTP", fontSize = 12.sp, color = LogoOrangeMuted)
                        }

                        Spacer(Modifier.height(8.dp))

                        // Reset Password Button
                        Button(
                            onClick = {
                                if (otpCode.length == 6 && isPasswordValid() && doPasswordsMatch()) {
                                    isLoading = true
                                    scope.launch {
                                        try {
                                            viewModel.resetPassword(phoneNumber, otpCode, newPassword)
                                        } catch (e: Exception) {
                                            isLoading = false
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    when {
                                        otpCode.length != 6 -> Toast.makeText(context, "Please enter valid 6-digit OTP", Toast.LENGTH_SHORT).show()
                                        !isPasswordValid() -> Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                        !doPasswordsMatch() -> Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SagePrimary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Reset Password", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Back to Login link
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (currentStep == 1) "Remember your password? " else "Back to ",
                            color = SoftGreyText,
                            fontSize = 14.sp
                        )
                        TextButton(
                            onClick = {
                                if (currentStep == 2) {
                                    currentStep = 1
                                    otpCode = ""
                                    newPassword = ""
                                    confirmPassword = ""
                                }
                                onBackToLogin()
                            }
                        ) {
                            Text(
                                if (currentStep == 1) "Sign In" else "Sign In",
                                color = LogoOrangeMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}