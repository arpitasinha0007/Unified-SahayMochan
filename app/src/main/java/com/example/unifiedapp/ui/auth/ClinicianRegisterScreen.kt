package com.example.unifiedapp.ui.auth

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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.RegisterState

@Composable
fun ClinicianRegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            val app = LocalContext.current.applicationContext as android.app.Application
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AuthViewModel(app) as T
            }
        }
    )
) {
    val context = LocalContext.current

    // Form state
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Observe registration state
    val registerState by authViewModel.clinicianRegisterState.collectAsState()

    // Local copy for smart cast
    val currentState = registerState
    LaunchedEffect(currentState) {
        when (currentState) {
            is RegisterState.Success -> {
                Toast.makeText(context, currentState.message, Toast.LENGTH_LONG).show()
                authViewModel.resetClinicianRegisterState()
                navController.popBackStack()
            }
            is RegisterState.Error -> {
                errorMessage = currentState.message
                isLoading = false
                authViewModel.resetClinicianRegisterState()
            }
            is RegisterState.Loading -> {
                isLoading = true
            }
            else -> { /* Idle */ }
        }
    }

    // Colors (defined locally to avoid conflicts)
    val primaryColor = Color(0xFF9D8DF1)
    val accentColor = Color(0xFFD9D1FF)
    val bgColor = Color(0xFFFAF8FF)
    val textPrimary = Color(0xFF1F2937)
    val textSecondary = Color(0xFF6B7280)
    val errorRed = Color(0xFFEF4444)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgColor, Color.White)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Icon(
                Icons.Default.MedicalServices,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Clinician Registration",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Text(
                text = "Create your clinician account",
                fontSize = 14.sp,
                color = textSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

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
                    // Name field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = accentColor.copy(alpha = 0.5f)
                        ),
                        isError = name.isNotBlank() && name.length < 3,
                        supportingText = {
                            if (name.isNotBlank() && name.length < 3) {
                                Text("Name must be at least 3 characters")
                            }
                        }
                    )

                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = accentColor.copy(alpha = 0.5f)
                        ),
                        isError = email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                        supportingText = {
                            if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                Text("Enter a valid email address")
                            }
                        }
                    )

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryColor) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = accentColor.copy(alpha = 0.5f)
                        ),
                        isError = password.isNotBlank() && password.length < 6,
                        supportingText = {
                            if (password.isNotBlank() && password.length < 6) {
                                Text("Password must be at least 6 characters")
                            }
                        }
                    )

                    // Confirm password field
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryColor) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = accentColor.copy(alpha = 0.5f)
                        ),
                        isError = confirmPassword.isNotBlank() && password != confirmPassword,
                        supportingText = {
                            if (confirmPassword.isNotBlank() && password != confirmPassword) {
                                Text("Passwords do not match")
                            }
                        }
                    )

                    // Error message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = errorRed,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Register button
                    Button(
                        onClick = {
                            if (name.length < 3) {
                                errorMessage = "Name must be at least 3 characters"
                                return@Button
                            }
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                errorMessage = "Enter a valid email address"
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "Password must be at least 6 characters"
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@Button
                            }
                            errorMessage = null
                            authViewModel.registerClinician(name, email, password)
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Register", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Login link
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Already have an account? ", color = textSecondary, fontSize = 14.sp)
                        Text(
                            text = "Login",
                            color = primaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}