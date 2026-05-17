package com.example.unifiedapp.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.unifiedapp.services.UploadService  // Add this import

object UploadServiceHelper {
    private const val TAG = "UploadServiceHelper"

    fun startUploadService(
        context: Context,
        anonymousId: String,
        age: Int,
        aiRawScore: Float,
        email: String?,
        registrationId: String,
        videoPath: String?,
        auCsvPath: String?,
        phq9CsvPath: String?
    ) {
        try {
            Log.d(TAG, "Starting upload service with params:")
            Log.d(TAG, "  anonymousId: $anonymousId")
            Log.d(TAG, "  registrationId: $registrationId")
            Log.d(TAG, "  videoPath: $videoPath")
            Log.d(TAG, "  auCsvPath: $auCsvPath")
            Log.d(TAG, "  phq9CsvPath: $phq9CsvPath")

            val intent = Intent(context, UploadService::class.java).apply {
                action = UploadService.ACTION_START_UPLOAD
                putExtra(UploadService.EXTRA_ANONYMOUS_ID, anonymousId)
                putExtra(UploadService.EXTRA_AGE, age)
                putExtra(UploadService.EXTRA_AI_RAW_SCORE, aiRawScore)
                putExtra(UploadService.EXTRA_EMAIL, email)
                putExtra(UploadService.EXTRA_REGISTRATION_ID, registrationId)
                putExtra(UploadService.EXTRA_VIDEO_PATH, videoPath)
                putExtra(UploadService.EXTRA_AU_CSV_PATH, auCsvPath)
                putExtra(UploadService.EXTRA_PHQ9_CSV_PATH, phq9CsvPath)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            Log.d(TAG, "✅ Upload service started successfully for user: $anonymousId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start upload service: ${e.message}", e)
        }
    }

    fun cancelUploadService(context: Context) {
        try {
            Log.d(TAG, "Cancelling upload service")
            val intent = Intent(context, UploadService::class.java).apply {
                action = UploadService.ACTION_CANCEL_UPLOAD
            }
            context.startService(intent)
            Log.d(TAG, "✅ Upload cancellation requested")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to cancel upload service: ${e.message}", e)
        }
    }
}