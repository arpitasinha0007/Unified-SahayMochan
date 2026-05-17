package com.example.unifiedapp.vision

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.example.unifiedapp.R
import java.io.File

object NotificationHelper {
    private const val CHANNEL_ID = "sahay_report_channel"
    private const val CHANNEL_NAME = "Report Downloads"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Main channel
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for report downloads"
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)

            // Error channel
            val errorChannel = NotificationChannel(
                "${CHANNEL_ID}_error",
                "Report Errors",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Error notifications for failed downloads"
            }
            notificationManager.createNotificationChannel(errorChannel)
        }
    }

    fun showDownloadSuccessNotification(context: Context, filePath: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val file = File(filePath)

            if (!file.exists()) {
                showDownloadErrorNotification(context, "File not found")
                return
            }

            // Create intent to open the PDF
            val uri: Uri = try {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } catch (e: IllegalArgumentException) {
                // Fallback to file URI for Android < 7.0
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            // Check if there's an app that can handle PDF
            val packageManager = context.packageManager
            if (intent.resolveActivity(packageManager) == null) {
                // No PDF viewer installed
                showNoPdfViewerNotification(context, filePath)
                return
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(), // Unique ID
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("✅ Report Downloaded")
                .setContentText("Your anxiety assessment report has been saved")
                .setSubText(file.name)
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText("File saved to: $filePath"))
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)

        } catch (e: Exception) {
            e.printStackTrace()
            showDownloadErrorNotification(context, e.message ?: "Unknown error")
        }
    }

    private fun showNoPdfViewerNotification(context: Context, filePath: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create intent to open file location instead
        val file = File(filePath)
        val parentDir = file.parentFile
        val uri = Uri.fromFile(parentDir)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "resource/folder")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("✅ Report Downloaded")
            .setContentText("No PDF viewer found. File saved to Downloads/Sahay/Reports/")
            .setContentIntent(pendingIntent)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showDownloadErrorNotification(context: Context, error: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "${CHANNEL_ID}_error")
            .setContentTitle("❌ Download Failed")
            .setContentText("Failed to download report")
            .setSubText(error)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Error: $error"))
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
}