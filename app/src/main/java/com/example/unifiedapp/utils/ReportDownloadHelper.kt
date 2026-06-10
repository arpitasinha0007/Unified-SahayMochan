package com.example.unifiedapp.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.unifiedapp.R
import com.example.unifiedapp.screens.AiPredictionData
import com.example.unifiedapp.screens.SeverityData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportDownloadHelper {
    private const val TAG = "ReportDownloadHelper"

    fun generateReport(
        context: Context,
        userName: String,
        userEmail: String,
        userAge: Int,
        userGender: String,
        anonymousId: String,
        registrationId: String,
        phq9Score: Int,
        phq9Severity: SeverityData,
        aiData: AiPredictionData?,
        assessmentType: String,
        onProgress: (Int) -> Unit = {}
    ): String? {
        var document: android.graphics.pdf.PdfDocument? = null
        return try {
            NotificationHelper.showDownloadStarted(context)
            onProgress(10)

            // Create a temporary file to build the PDF
            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.pdf")
            onProgress(20)

            document = android.graphics.pdf.PdfDocument()
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            onProgress(30)

            // ========== ALL YOUR EXISTING DRAWING CODE (unchanged) ==========
            // Determine report title
            val reportTitle = if (assessmentType.equals("depression", ignoreCase = true)) {
                "MOCHAN DEPRESSION ASSESSMENT REPORT"
            } else {
                "SAHAY ANXIETY ASSESSMENT REPORT"
            }

            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.mochan_logo)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading logo: ${e.message}")
                null
            }

            // Header
            val headerYPosition = 50f
            logoBitmap?.let {
                val scaledLogo = Bitmap.createScaledBitmap(it, 70, 70, false)
                val titlePaint = Paint().apply {
                    color = Color.parseColor("#1F2937")
                    textSize = 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val textWidth = titlePaint.measureText(reportTitle)
                val logoWidth = 70f
                val spacing = 15f
                val totalElementWidth = logoWidth + spacing + textWidth
                val pageWidth = 495f
                val leftMargin = 50f
                val startX = leftMargin + (pageWidth - totalElementWidth) / 2
                canvas.drawBitmap(scaledLogo, startX, headerYPosition - 35f, null)
                canvas.drawText(reportTitle, startX + logoWidth + spacing, headerYPosition, titlePaint)
            } ?: run {
                val titlePaint = Paint().apply {
                    color = Color.parseColor("#1F2937")
                    textSize = 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(reportTitle, 297.5f, headerYPosition, titlePaint)
            }

            var yPosition = headerYPosition + 40f

            val linePaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                strokeWidth = 2f
            }
            canvas.drawLine(50f, yPosition, 545f, yPosition, linePaint)
            yPosition += 30f

            val dateFormat = SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault())
            canvas.drawText("Report Generated: ${dateFormat.format(Date())}", 50f, yPosition,
                Paint().apply { color = Color.parseColor("#4B5563"); textSize = 10f }
            )
            yPosition += 30f

            // Patient Info
            val infoPaint = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val infoBoldPaint = Paint().apply {
                color = Color.parseColor("#1F2937")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val infoBoxPaint = Paint().apply {
                color = Color.parseColor("#F9FAFB")
                style = Paint.Style.FILL
            }
            canvas.drawRect(50f, yPosition - 10f, 545f, yPosition + 70f, infoBoxPaint)

            canvas.drawText("Name:", 60f, yPosition + 8f, infoBoldPaint)
            canvas.drawText(userName, 120f, yPosition + 8f, infoPaint)

            canvas.drawText("Email:", 60f, yPosition + 28f, infoBoldPaint)
            canvas.drawText(userEmail, 120f, yPosition + 28f, infoPaint)

            canvas.drawText("Registration ID:", 60f, yPosition + 48f, infoBoldPaint)
            canvas.drawText(registrationId, 180f, yPosition + 48f, infoPaint)

            yPosition += 80f

            // Assessment Results
            val headerPaint = Paint().apply {
                color = Color.parseColor("#4F46E5")
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("ASSESSMENT RESULTS", 50f, yPosition, headerPaint)
            yPosition += 30f

            val phqBoxPaint = Paint().apply {
                color = Color.parseColor("#F0F9FF")
                style = Paint.Style.FILL
            }
            canvas.drawRect(50f, yPosition - 10f, 545f, yPosition + 60f, phqBoxPaint)

            val sectionPaint = Paint().apply {
                color = Color.parseColor("#374151")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val boldTextPaint = Paint().apply {
                color = Color.parseColor("#1F2937")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textPaint = Paint().apply {
                color = Color.parseColor("#4B5563")
                textSize = 11f
            }

            val questionnaireLabel = if (assessmentType.equals("depression", ignoreCase = true)) {
                "📋 PHQ-9 QUESTIONNAIRE"
            } else {
                "📋 GAD-7 QUESTIONNAIRE"
            }
            canvas.drawText(questionnaireLabel, 70f, yPosition + 5f, sectionPaint)
            canvas.drawText("Severity: ${phq9Severity.level}", 70f, yPosition + 30f, boldTextPaint)
            canvas.drawText(phq9Severity.description, 70f, yPosition + 50f, textPaint)

            yPosition += 80f

            if (aiData != null && aiData.label.isNotBlank()) {
                val aiBoxPaint = Paint().apply {
                    color = Color.parseColor("#F5F3FF")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(50f, yPosition - 10f, 545f, yPosition + 70f, aiBoxPaint)

                canvas.drawText("🤖 AI FACIAL ANALYSIS", 70f, yPosition + 5f, sectionPaint)
                canvas.drawText("Prediction: ${aiData.label}", 70f, yPosition + 30f, boldTextPaint)
                yPosition += 80f
            }

            // Recommendations
            canvas.drawText("RECOMMENDATIONS", 50f, yPosition, headerPaint)
            yPosition += 30f

            val recCount = phq9Severity.recommendations.size
            val recBoxHeight = (recCount * 20f) + 20f
            val recBoxPaint = Paint().apply {
                color = Color.parseColor("#FFF7ED")
                style = Paint.Style.FILL
            }
            canvas.drawRect(50f, yPosition - 10f, 545f, yPosition + recBoxHeight, recBoxPaint)

            var recYPos = yPosition + 5f
            phq9Severity.recommendations.forEachIndexed { index, rec ->
                canvas.drawText("${index + 1}. ${rec.title}", 70f, recYPos, boldTextPaint)
                recYPos += 15f
                canvas.drawText("   ${rec.description}", 70f, recYPos, textPaint)
                recYPos += 20f
            }

            yPosition = recYPos + 20f

            // Crisis Resources
            canvas.drawText("CRISIS RESOURCES", 50f, yPosition, headerPaint)
            yPosition += 25f

            val crisisBoxPaint = Paint().apply {
                color = Color.parseColor("#FEF2F2")
                style = Paint.Style.FILL
            }
            canvas.drawRect(50f, yPosition - 10f, 545f, yPosition + 80f, crisisBoxPaint)

            canvas.drawText("📞 National Crisis Hotline: 988", 70f, yPosition + 5f, boldTextPaint)
            canvas.drawText("📞 Emergency Services: 911", 70f, yPosition + 25f, boldTextPaint)
            canvas.drawText("👥 Talk to a trusted friend or family member", 70f, yPosition + 45f, textPaint)
            canvas.drawText("🏥 Visit your nearest hospital emergency room", 70f, yPosition + 65f, textPaint)

            yPosition += 100f

            val footerPaint = Paint().apply {
                color = Color.parseColor("#9CA3AF")
                textSize = 8f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("This report is for informational purposes only and is not a medical diagnosis.", 297.5f, yPosition, footerPaint)
            canvas.drawText("Always consult with qualified mental health professionals.", 297.5f, yPosition + 12f, footerPaint)

            document.finishPage(page)
            onProgress(80)

            // Write to temp file
            FileOutputStream(tempFile).use { fos ->
                document.writeTo(fos)
            }
            document.close()
            document = null

            onProgress(90)

            // Save to Downloads folder (visible to user)
            val finalPath = savePdfToDownloads(context, tempFile, anonymousId, assessmentType)

            // Delete temp file
            tempFile.delete()

            onProgress(100)
            Log.d(TAG, "PDF saved to: $finalPath")
            NotificationHelper.showDownloadSuccess(context, finalPath)
            finalPath

        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF report: ${e.message}", e)
            NotificationHelper.showDownloadError(context, e.message ?: "Unknown error occurred")
            null
        } finally {
            document?.close()
        }
    }

    private fun savePdfToDownloads(context: Context, sourceFile: File, anonymousId: String, assessmentType: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${anonymousId}_${assessmentType}_${timestamp}.pdf"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ – use MediaStore
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    sourceFile.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                // Also copy to app cache for FileProvider to open
                val cacheFile = File(context.cacheDir, fileName)
                sourceFile.inputStream().use { input ->
                    cacheFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                cacheFile.absolutePath
            } ?: sourceFile.absolutePath
        } else {
            // Android 9 and below – write directly to external storage
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val destFile = File(downloadsDir, fileName)
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        }
    }
}