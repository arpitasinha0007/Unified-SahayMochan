package com.example.unifiedapp.ui.vision

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.unifiedapp.ui.views.AssessmentViewModel
import com.example.unifiedapp.ui.views.CameraUiState
import com.example.unifiedapp.ui.views.CameraViewModel


@Composable
fun CameraScreen(
    cameraViewModel: CameraViewModel = hiltViewModel(),
    assessmentViewModel: AssessmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by cameraViewModel.uiState.collectAsState()
    val recorderHelper = remember {

        VideoRecorderHelper(
            context = context,
            listener = object : VideoRecorderHelper.VideoRecordingListener {

                override fun onRecordingStarted(filePath: String) {
                    Log.d("RECORDER", "Recording started: $filePath")
                }

                override fun onRecordingStopped(filePath: String, durationMs: Long) {
                    assessmentViewModel.onRecordingStopped(
                       filePath,
                        durationMs
                    )
                }

                override fun onRecordingError(error: String) {
                    Log.e("RECORDER", "Recording error: $error")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        cameraViewModel.initialize(context, "")
    }

    Box(modifier = Modifier.fillMaxSize()) {

        when (uiState) {

            CameraUiState.Idle,
            CameraUiState.Initializing -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            CameraUiState.Ready -> {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onFrame = { imageProxy ->
                        assessmentViewModel.processImageFrame(
                            imageProxy,
                            isFrontCamera = true
                        )
                    },
                    recorderHelper = recorderHelper
                )
            }

            is CameraUiState.Error -> {
                Text(
                    text = (uiState as CameraUiState.Error).message,
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}