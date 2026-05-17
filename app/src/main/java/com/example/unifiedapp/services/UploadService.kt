package com.example.unifiedapp.services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.unifiedapp.R
import com.example.unifiedapp.utils.SimpleServerClient
import kotlinx.coroutines.*
import java.io.File

class UploadService : Service() {

    companion object {
        private const val TAG = "UploadService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "upload_channel"

        // Action constants
        const val ACTION_START_UPLOAD = "ACTION_START_UPLOAD"
        const val ACTION_CANCEL_UPLOAD = "ACTION_CANCEL_UPLOAD"
        const val ACTION_CHECK_STATUS = "ACTION_CHECK_STATUS"

        // Broadcast action for UI updates
        const val ACTION_UPLOAD_COMPLETED = "com.example.unifiedapp.UPLOAD_COMPLETED"
        const val ACTION_UPLOAD_PROGRESS = "com.example.unifiedapp.UPLOAD_PROGRESS"
        const val ACTION_UPLOAD_STATUS = "com.example.unifiedapp.UPLOAD_STATUS"

        // Extras
        const val EXTRA_SUCCESS = "extra_success"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_ASSESSMENT_ID = "extra_assessment_id"
        const val EXTRA_TIMESTAMP = "extra_timestamp"

        // Status constants
        const val STATUS_UPLOADING = "uploading"
        const val STATUS_SUCCESS = "success"
        const val STATUS_FAILED = "failed"
        const val STATUS_PENDING = "pending"

        // Extras for upload data
        const val EXTRA_ANONYMOUS_ID = "extra_anonymous_id"
        const val EXTRA_AGE = "extra_age"
        const val EXTRA_AI_RAW_SCORE = "extra_ai_raw_score"
        const val EXTRA_EMAIL = "extra_email"
        const val EXTRA_REGISTRATION_ID = "extra_registration_id"
        const val EXTRA_VIDEO_PATH = "extra_video_path"
        const val EXTRA_AU_CSV_PATH = "extra_au_csv_path"
        const val EXTRA_PHQ9_CSV_PATH = "extra_phq9_csv_path"

        // SharedPreferences key for last upload status
        const val PREFS_LAST_UPLOAD_STATUS = "last_upload_status"
        const val PREFS_LAST_UPLOAD_MESSAGE = "last_upload_message"
        const val PREFS_LAST_UPLOAD_TIME = "last_upload_time"
        const val PREFS_LAST_UPLOAD_SUCCESS = "last_upload_success"
    }

    // Use SupervisorJob to handle child job failures independently
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var uploadJob: Job? = null
    private var currentUploadId: String? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_UPLOAD -> {
                val anonymousId = intent.getStringExtra(EXTRA_ANONYMOUS_ID) ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val age = intent.getIntExtra(EXTRA_AGE, 0)
                val aiRawScore = intent.getFloatExtra(EXTRA_AI_RAW_SCORE, 0f)
                val email = intent.getStringExtra(EXTRA_EMAIL)
                val registrationId = intent.getStringExtra(EXTRA_REGISTRATION_ID) ?: run {
                    stopSelf()
                    return START_NOT_STICKY
                }
                val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
                val auCsvPath = intent.getStringExtra(EXTRA_AU_CSV_PATH)
                val phq9CsvPath = intent.getStringExtra(EXTRA_PHQ9_CSV_PATH)

                // Generate unique upload ID
                currentUploadId = "${anonymousId}_${System.currentTimeMillis()}"

                startUpload(
                    anonymousId = anonymousId,
                    age = age,
                    aiRawScore = aiRawScore,
                    email = email,
                    registrationId = registrationId,
                    videoPath = videoPath,
                    auCsvPath = auCsvPath,
                    phq9CsvPath = phq9CsvPath
                )
            }
            ACTION_CANCEL_UPLOAD -> {
                cancelUpload()
            }
            ACTION_CHECK_STATUS -> {
                broadcastCurrentStatus()
            }
        }
        return START_NOT_STICKY
    }

    private fun canShowNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun broadcastCurrentStatus() {
        val prefs = getSharedPreferences("upload_status", Context.MODE_PRIVATE)
        val intent = Intent(ACTION_UPLOAD_STATUS).apply {
            putExtra(EXTRA_SUCCESS, prefs.getBoolean(PREFS_LAST_UPLOAD_SUCCESS, false))
            putExtra(EXTRA_MESSAGE, prefs.getString(PREFS_LAST_UPLOAD_MESSAGE, ""))
            putExtra(EXTRA_STATUS, prefs.getString(PREFS_LAST_UPLOAD_STATUS, ""))
            putExtra(EXTRA_TIMESTAMP, prefs.getLong(PREFS_LAST_UPLOAD_TIME, 0))
        }
        sendBroadcast(intent)
    }

    private fun startUpload(
        anonymousId: String,
        age: Int,
        aiRawScore: Float,
        email: String?,
        registrationId: String,
        videoPath: String?,
        auCsvPath: String?,
        phq9CsvPath: String?
    ) {
        // Validate files
        val videoFile = videoPath?.let { File(it) }
        val auCsvFile = auCsvPath?.let { File(it) }
        val phq9CsvFile = phq9CsvPath?.let { File(it) }

        if (auCsvFile == null || !auCsvFile.exists()) {
            val error = "AU data file missing"
            Log.e(TAG, error)
            saveUploadStatus(false, error, STATUS_FAILED)
            if (canShowNotifications()) showErrorNotification(error)
            broadcastResult(false, error, 0, STATUS_FAILED)
            stopSelf()
            return
        }

        if (phq9CsvFile == null || !phq9CsvFile.exists()) {
            val error = "PHQ-9 data file missing"
            Log.e(TAG, error)
            saveUploadStatus(false, error, STATUS_FAILED)
            if (canShowNotifications()) showErrorNotification(error)
            broadcastResult(false, error, 0, STATUS_FAILED)
            stopSelf()
            return
        }

        // Save pending status
        saveUploadStatus(false, "Upload started...", STATUS_UPLOADING)

        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification("Starting upload...", 0))

        // Cancel any existing upload job
        uploadJob?.cancel()

        uploadJob = serviceScope.launch {
            try {
                Log.d(TAG, "========== STARTING UPLOAD PROCESS ==========")

                val serverClient = SimpleServerClient(this@UploadService)

                val assessmentData = SimpleServerClient.AssessmentData(
                    anonymousId = anonymousId,
                    age = age,
                    assessmentType = "depression",
                    videoFile = videoFile,
                    auCsvFile = auCsvFile,
                    phq9CsvFile = phq9CsvFile,
                    aiRawScore = aiRawScore,
                    email = email,
                    registrationId = registrationId
                )

                // Use CompletableDeferred to handle callback
                val uploadComplete = CompletableDeferred<Pair<Boolean, String>>()

                serverClient.uploadAnonymousAssessment(
                    assessmentData,
                    object : SimpleServerClient.UploadCallback {
                        override fun onProgress(progress: Int, message: String) {
                            if (isActive) {
                                updateNotification(message, progress)
                                broadcastProgress(progress, message)
                            }
                        }

                        override fun onSuccess(message: String) {
                            Log.d(TAG, "Upload callback success: $message")
                            uploadComplete.complete(Pair(true, message))
                        }

                        override fun onError(error: String) {
                            Log.e(TAG, "Upload callback error: $error")
                            uploadComplete.complete(Pair(false, error))
                        }
                    }
                )

                // Wait for upload to complete with timeout
                val (success, message) = withTimeout(120000L) { // 2 minute timeout
                    uploadComplete.await()
                }

                if (success) {
                    Log.d(TAG, "✅ Upload successful: $message")

                    // Extract assessment ID from message if available
                    val assessmentId = extractAssessmentId(message)

                    // Save successful status
                    saveUploadStatus(true, message, STATUS_SUCCESS, assessmentId)

                    // Show success notification
                    if (canShowNotifications()) showSuccessNotification(message)

                    // Broadcast to UI with explicit success status
                    broadcastResult(true, message, 100, STATUS_SUCCESS, assessmentId)

                    // Delete local files
                    deleteLocalFiles(anonymousId)

                    // Small delay to ensure everything is processed
                    delay(500)

                    // Stop service AFTER everything is done
                    stopSelf()
                } else {
                    Log.e(TAG, "❌ Upload failed: $message")
                    saveUploadStatus(false, message, STATUS_FAILED)

                    // Show error notification
                    if (canShowNotifications()) showErrorNotification(message)

                    // Broadcast to UI with explicit failure status
                    broadcastResult(false, message, 0, STATUS_FAILED)

                    stopSelf()
                }

            } catch (e: CancellationException) {
                Log.d(TAG, "Upload job was cancelled")
                saveUploadStatus(false, "Upload cancelled", STATUS_FAILED)
                if (canShowNotifications()) showErrorNotification("Upload was cancelled")
                broadcastResult(false, "Upload was cancelled", 0, STATUS_FAILED)
                stopSelf()
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Upload timed out", e)
                saveUploadStatus(false, "Upload timed out", STATUS_FAILED)
                if (canShowNotifications()) showErrorNotification("Upload timed out")
                broadcastResult(false, "Upload timed out", 0, STATUS_FAILED)
                stopSelf()
            } catch (e: Exception) {
                Log.e(TAG, "Upload failed", e)
                saveUploadStatus(false, "Upload failed: ${e.message}", STATUS_FAILED)
                if (canShowNotifications()) showErrorNotification("Upload failed: ${e.message}")
                broadcastResult(false, "Upload failed: ${e.message}", 0, STATUS_FAILED)
                stopSelf()
            }
        }
    }

    private fun extractAssessmentId(message: String): String? {
        val regex = "ID:?\\s*(\\d+)".toRegex()
        val matchResult = regex.find(message)
        return matchResult?.groupValues?.get(1)
    }

    private fun saveUploadStatus(success: Boolean, message: String, status: String, assessmentId: String? = null) {
        val prefs = getSharedPreferences("upload_status", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(PREFS_LAST_UPLOAD_SUCCESS, success)
            putString(PREFS_LAST_UPLOAD_MESSAGE, message)
            putString(PREFS_LAST_UPLOAD_STATUS, status)
            putLong(PREFS_LAST_UPLOAD_TIME, System.currentTimeMillis())
            assessmentId?.let { putString("last_assessment_id", it) }
            apply()
        }
    }

    private fun cancelUpload() {
        Log.d(TAG, "Upload cancelled by user")

        uploadJob?.cancel()
        saveUploadStatus(false, "Upload cancelled", STATUS_FAILED)

        if (canShowNotifications()) {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Upload Cancelled")
                .setContentText("Upload was cancelled")
                .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        broadcastResult(false, "Upload cancelled", 0, STATUS_FAILED)

        serviceScope.launch {
            delay(500)
            stopSelf()
        }
    }

    private fun updateNotification(message: String, progress: Int) {
        val notification = createNotification(message, progress)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        broadcastProgress(progress, message)
    }

    private fun broadcastProgress(progress: Int, message: String) {
        val intent = Intent(ACTION_UPLOAD_PROGRESS).apply {
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
        }
        sendBroadcast(intent)
        Log.d(TAG, "Progress broadcast: $progress% - $message")
    }

    private fun createNotification(message: String, progress: Int): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Uploading Assessment")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        if (progress > 0 && progress < 100) {
            builder.setProgress(100, progress, false)
        }

        // Add cancel action
        val cancelIntent = Intent(this, UploadService::class.java).apply {
            action = ACTION_CANCEL_UPLOAD
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Cancel",
            cancelPendingIntent
        )

        return builder.build()
    }

    private fun showSuccessNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Upload Complete")
            .setContentText(message.take(50))
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Upload Failed")
            .setContentText(error.take(50))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun broadcastResult(success: Boolean, message: String, progress: Int = 0, status: String, assessmentId: String? = null) {
        val intent = Intent(ACTION_UPLOAD_COMPLETED).apply {
            putExtra(EXTRA_SUCCESS, success)
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_STATUS, status)
            putExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
            assessmentId?.let { putExtra(EXTRA_ASSESSMENT_ID, it) }
        }
        sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent: success=$success, message=$message, status=$status")
    }

    private fun deleteLocalFiles(anonymousId: String) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val appFolder = File(downloadsDir, "unifiedapp")
            val userFolder = File(appFolder, anonymousId)

            if (userFolder.exists() && userFolder.isDirectory) {
                userFolder.deleteRecursively()
                Log.d(TAG, "Deleted local files for $anonymousId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting local files: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Upload Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Handles background upload of assessment data"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "UploadService destroyed")
        uploadJob?.cancel(CancellationException("Service is being destroyed"))
        serviceScope.coroutineContext.cancelChildren()
        super.onDestroy()
    }
}