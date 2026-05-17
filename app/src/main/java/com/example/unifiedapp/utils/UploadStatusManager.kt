// utils/UploadStatusManager.kt

package com.example.unifiedapp.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.unifiedapp.services.UploadService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UploadStatusManager {

    private const val TAG = "UploadStatusManager"
    private const val PREFS_NAME = "upload_status"

    data class UploadStatus(
        val isUploading: Boolean = false,
        val success: Boolean? = null,
        val message: String = "",
        val progress: Int = 0,
        val status: String = "",
        val timestamp: Long = 0,
        val assessmentId: String? = null
    )

    private val _status = MutableStateFlow(UploadStatus())
    val status: StateFlow<UploadStatus> = _status.asStateFlow()

    private var broadcastReceiver: UploadStatusReceiver? = null

    fun initialize(context: Context) {
        if (broadcastReceiver == null) {
            broadcastReceiver = UploadStatusReceiver { update ->
                _status.value = update
            }

            LocalBroadcastManager.getInstance(context).registerReceiver(
                broadcastReceiver!!,
                IntentFilter(UploadService.ACTION_UPLOAD_COMPLETED).apply {
                    addAction(UploadService.ACTION_UPLOAD_PROGRESS)
                    addAction(UploadService.ACTION_UPLOAD_STATUS)
                }
            )

            // Load persisted status
            loadPersistedStatus(context)

            // Request current status from service
            requestStatusUpdate(context)
        }
    }

    fun cleanup(context: Context) {
        broadcastReceiver?.let {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(it)
            broadcastReceiver = null
        }
    }

    fun requestStatusUpdate(context: Context) {
        val intent = Intent(context, UploadService::class.java).apply {
            action = UploadService.ACTION_CHECK_STATUS
        }
        context.startService(intent)
    }

    fun saveStatus(context: Context, status: UploadStatus) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_uploading", status.isUploading)
            status.success?.let { putBoolean("success", it) }
            putString("message", status.message)
            putInt("progress", status.progress)
            putString("status", status.status)
            putLong("timestamp", status.timestamp)
            status.assessmentId?.let { putString("assessment_id", it) }
            apply()
        }
        _status.value = status
    }

    private fun loadPersistedStatus(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val status = UploadStatus(
            isUploading = prefs.getBoolean("is_uploading", false),
            success = if (prefs.contains("success")) prefs.getBoolean("success", false) else null,
            message = prefs.getString("message", "") ?: "",
            progress = prefs.getInt("progress", 0),
            status = prefs.getString("status", "") ?: "",
            timestamp = prefs.getLong("timestamp", 0),
            assessmentId = prefs.getString("assessment_id", null)
        )
        _status.value = status
    }

    fun clearStatus(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        _status.value = UploadStatus()
    }
    fun getStatusDisplayText(status: UploadStatus): String {
        return when {
            status.isUploading -> "Uploading... ${status.progress}%"
            status.success == true -> "✓ Upload successful"
            status.success == false -> "✗ Upload failed"
            else -> "Ready to upload"
        }
    }

    fun shouldShowSuccessNotification(status: UploadStatus): Boolean {
        return status.success == true && status.status == UploadService.STATUS_SUCCESS
    }

    fun shouldShowErrorNotification(status: UploadStatus): Boolean {
        return status.success == false && status.status == UploadService.STATUS_FAILED
    }

    private class UploadStatusReceiver(
        private val onUpdate: (UploadStatus) -> Unit
    ) : android.content.BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UploadService.ACTION_UPLOAD_COMPLETED -> {
                    val success = intent.getBooleanExtra(UploadService.EXTRA_SUCCESS, false)
                    val message = intent.getStringExtra(UploadService.EXTRA_MESSAGE) ?: ""
                    val status = intent.getStringExtra(UploadService.EXTRA_STATUS) ?: ""
                    val progress = intent.getIntExtra(UploadService.EXTRA_PROGRESS, 0)
                    val timestamp = intent.getLongExtra(UploadService.EXTRA_TIMESTAMP, System.currentTimeMillis())
                    val assessmentId = intent.getStringExtra(UploadService.EXTRA_ASSESSMENT_ID)

                    val uploadStatus = UploadStatus(
                        isUploading = false,
                        success = success,
                        message = message,
                        progress = progress,
                        status = status,
                        timestamp = timestamp,
                        assessmentId = assessmentId
                    )

                    // Save to preferences
                    UploadStatusManager.saveStatus(context, uploadStatus)
                    onUpdate(uploadStatus)

                    Log.d(TAG, "Received completed broadcast: success=$success, message=$message")
                }

                UploadService.ACTION_UPLOAD_PROGRESS -> {
                    val progress = intent.getIntExtra(UploadService.EXTRA_PROGRESS, 0)
                    val message = intent.getStringExtra(UploadService.EXTRA_MESSAGE) ?: ""

                    val uploadStatus = UploadStatus(
                        isUploading = true,
                        success = null,
                        message = message,
                        progress = progress,
                        status = UploadService.STATUS_UPLOADING,
                        timestamp = System.currentTimeMillis()
                    )

                    UploadStatusManager.saveStatus(context, uploadStatus)
                    onUpdate(uploadStatus)

                    Log.d(TAG, "Received progress broadcast: $progress% - $message")
                }

                UploadService.ACTION_UPLOAD_STATUS -> {
                    val success = intent.getBooleanExtra(UploadService.EXTRA_SUCCESS, false)
                    val message = intent.getStringExtra(UploadService.EXTRA_MESSAGE) ?: ""
                    val status = intent.getStringExtra(UploadService.EXTRA_STATUS) ?: ""
                    val timestamp = intent.getLongExtra(UploadService.EXTRA_TIMESTAMP, 0)

                    val uploadStatus = UploadStatus(
                        isUploading = false,
                        success = if (status == UploadService.STATUS_UPLOADING) null else success,
                        message = message,
                        progress = if (status == UploadService.STATUS_UPLOADING) 50 else 0,
                        status = status,
                        timestamp = timestamp
                    )

                    UploadStatusManager.saveStatus(context, uploadStatus)
                    onUpdate(uploadStatus)
                }
            }
        }
    }
}