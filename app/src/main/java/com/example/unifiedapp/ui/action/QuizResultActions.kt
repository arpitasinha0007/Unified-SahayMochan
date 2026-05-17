package com.example.unifiedapp.ui.action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.material3.Button

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.example.unifiedapp.ui.remote.SimpleServerClient
import com.example.unifiedapp.ui.vision.CameraPreview

@Composable
fun QuizResultActions(
    score: Int,
    userEmail: String,
    name: String,
    onSendReport: () -> Unit
) {


    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onSendReport,
            modifier = Modifier.fillMaxWidth(),
            enabled = userEmail.isNotBlank()
        ) {
            Text("📧 Email My Report")
        }
    }
}
