package com.example.unifiedapp.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.navigation.NavController
import com.example.unifiedapp.ui.views.AuthViewModel
import com.example.unifiedapp.ui.views.RegisterState

@Composable
fun ClinicianRegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var genderError by remember { mutableStateOf<String?>(null) }

    val registerState by authViewModel.clinicianRegisterState.collectAsState()
    val regResult by authViewModel.clinicianRegResult.collectAsState()
    var showRegDialog by remember { mutableStateOf(false) }
    var regIdToShow by remember { mutableStateOf("") }

    val currentRegisterState = registerState
    LaunchedEffect(currentRegisterState) {
        when (currentRegisterState) {
            is RegisterState.Success -> {
                isLoading = false
            }
            is RegisterState.Error -> {
                errorMessage = currentRegisterState.message
                isLoading = false
                authViewModel.resetClinicianRegisterState()
            }
            is RegisterState.Loading -> isLoading = true
            else -> {}
        }
    }

    LaunchedEffect(regResult) {
        if (regResult != null) {
            regIdToShow = regResult!!.registrationId
            showRegDialog = true
        }
    }

    // Darker primary color for button and focus
    val primaryColor = Color(0xFF7C3AED)   // Dark purple
    val accentColor = Color(0xFF9D8DF1)    // Lighter for borders
    val bgColor = Color(0xFFFAF8FF)
    val textPrimary = Color(0xFF1F2937)
    val textSecondary = Color(0xFF6B7280)
    val errorRed = Color(0xFFEF4444)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(bgColor, Color.White)))
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

            Icon(Icons.Default.MedicalServices, null, tint = primaryColor, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Text("Clinician Registration", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Text("Create your clinician account", fontSize = 14.sp, color = textSecondary, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(32.dp))

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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Name with *
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = accentColor.copy(alpha = 0.5f)
                        ),
                        isError = name.isNotBlank() && name.length < 3,
                        supportingText = { if (name.isNotBlank() && name.length < 3) Text("Name must be at least 3 characters") }
                    )

                    // Email (no asterisk)
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
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
                            if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
                                Text("Enter a valid email address")
                        }
                    )

                    // Age with *
                    OutlinedTextField(
                        value = ageText, onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) ageText = it },
                        label = { Text("Age *") },
                        leadingIcon = { Icon(Icons.Default.Cake, null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = accentColor.copy(alpha = 0.5f)
                        ),
                        isError = ageError != null,
                        supportingText = { ageError?.let { Text(it) } }
                    )

                    // Gender Dropdown with *
                    var expanded by remember { mutableStateOf(false) }
                    val genders = listOf("male", "female", "other")
                    Column {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender *") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = primaryColor) },
                            trailingIcon = {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = accentColor.copy(alpha = 0.5f)
                            ),
                            isError = genderError != null
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            genders.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g) },
                                    onClick = {
                                        gender = g
                                        genderError = null
                                        expanded = false
                                    }
                                )
                            }
                        }
                        if (genderError != null) {
                            Text(genderError!!, color = errorRed, fontSize = 12.sp, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                        }
                    }

                    // Password with *
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Password *") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryColor) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
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
                        supportingText = { if (password.isNotBlank() && password.length < 6) Text("Password must be at least 6 characters") }
                    )

                    // Confirm Password (no asterisk)
                    OutlinedTextField(
                        value = confirmPassword, onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = primaryColor) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = accentColor.copy(alpha = 0.5f)
                        ),
                        isError = confirmPassword.isNotBlank() && password != confirmPassword,
                        supportingText = { if (confirmPassword.isNotBlank() && password != confirmPassword) Text("Passwords do not match") }
                    )

                    if (errorMessage != null) Text(errorMessage!!, color = errorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))

                    // Register Button with darker color
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
                            val age = ageText.toIntOrNull()
                            if (age == null || age < 18 || age > 120) {
                                ageError = "Age must be 18-120"
                                return@Button
                            }
                            ageError = null
                            if (gender.isBlank()) {
                                genderError = "Please select a gender"
                                return@Button
                            }
                            genderError = null
                            if (password.length < 6) {
                                errorMessage = "Password must be at least 6 characters"
                                return@Button
                            }
                            if (password != confirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@Button
                            }
                            errorMessage = null
                            authViewModel.registerClinician(name, email, password, age, gender)
                        },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Register", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Already have an account? ", color = textSecondary, fontSize = 14.sp)
                        Text(
                            "Login", color = primaryColor, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { navController.popBackStack() }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Dialog to show generated registration ID after successful registration
    if (showRegDialog && regResult != null) {
        AlertDialog(
            onDismissRequest = {
                showRegDialog = false
                authViewModel.resetClinicianRegResult()
                navController.popBackStack()
            },
            title = {
                Text(
                    "Registration Successful",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Your clinician account has been created.",
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Surface(
                        color = primaryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Registration ID",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                            SelectionContainer {
                                Text(
                                    text = regIdToShow,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryColor,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "Long press on the ID to copy it.",
                        fontSize = 12.sp,
                        color = textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegDialog = false
                        authViewModel.resetClinicianRegResult()
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}