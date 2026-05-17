package com.example.unifiedapp.ui.anxiety

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class VideoViewModel : ViewModel() {

    var isProcessing by mutableStateOf(false)
        private set

    var outputVideoPath by mutableStateOf<String?>(null)
        private set

    fun processVideo(
        context: Context,
        inputPath: String
    ) {

        viewModelScope.launch {

            isProcessing = true

            try {
                val output =
                    VideoProcessor.processVideo(
                        context,
                        inputPath
                    )

                outputVideoPath = output

            } catch (e: Exception) {
                e.printStackTrace()
            }

            isProcessing = false
        }
    }
}