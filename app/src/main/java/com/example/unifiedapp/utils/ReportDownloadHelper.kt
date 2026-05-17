package com.example.unifiedapp.utils

import android.content.Context
import android.graphics.*
import android.os.Environment
import android.util.Log
import com.example.unifiedapp.R
import com.example.unifiedapp.screens.AiPredictionData
import com.example.unifiedapp.screens.SeverityData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

// ✅ ADD THIS DATA CLASS for UnderageResultScreen
data class ResultStateData(
    val title: String,
    val message: String,
    val emoji: String,
    val color: Int
)

// ✅ ADD THIS DATA CLASS for UnderageResultScreen
data class AiPredictionDataForReport(
    val anxietyPrediction: String,
    val anxietyScore: Float,
    val anxietyConfidence: Float
)

object ReportDownloadHelper {
    private const val TAG = "ReportDownloadHelper"

    fun generateReport(
        context: Context,
        userName: String,
        userAge: Int,
        userGender: String,
        anonymousId: String,
        registrationId: String,
        phq9Score: Int,
        phq9Severity: SeverityData,
        aiData: AiPredictionData?,
        onProgress: (Int) -> Unit = {}
    ): String? {
        return try {
            // Show started notification only (simplified)
            NotificationHelper.showDownloadStarted(context)
            onProgress(10)

            // Create directory in Downloads
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appFolder = File(downloadsDir, "unifiedapp")
            val reportsFolder = File(appFolder, "Reports")

            if (!reportsFolder.exists()) {
                reportsFolder.mkdirs()
            }

            onProgress(20)

            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${anonymousId}_assessment_${timestamp}.pdf"
            val pdfFile = File(reportsFolder, fileName)

            onProgress(30)

            // Create PDF document
            val document = android.graphics.pdf.PdfDocument()

            // Create a page
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            onProgress(40)

            // Load Mochan logo
            val logoBitmap = try {
                BitmapFactory.decodeResource(context.resources, R.drawable.mochan_logo)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading logo: ${e.message}")
                null
            }

            // ============ UPDATED HEADER SECTION ============
            // ============ UPDATED HEADER SECTION with proper centering ============
            val headerYPosition = 50f

            logoBitmap?.let {
                // Scale logo to appropriate size
                val scaledLogo = Bitmap.createScaledBitmap(it, 70, 70, false) // Slightly larger logo

                // Larger text size for title
                val titlePaint = Paint().apply {
                    color = Color.parseColor("#1F2937")
                    textSize = 28f // Increased from 20f to 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                // Measure text width to calculate proper centering
                val textWidth = titlePaint.measureText("MOCHAN ASSESSMENT REPORT")
                val logoWidth = 70f
                val spacing = 15f // Space between logo and text

                // Total width of logo + spacing + text
                val totalElementWidth = logoWidth + spacing + textWidth

                // Page usable width (from 50 to 545 = 495)
                val pageWidth = 495f
                val leftMargin = 50f

                // Calculate starting X position to center everything
                // We want the entire combination (logo + text) to be centered
                val startX = leftMargin + (pageWidth - totalElementWidth) / 2

                // Draw logo at calculated position
                canvas.drawBitmap(scaledLogo, startX, headerYPosition - 35f, null) // Adjusted vertical alignment

                // Draw text next to logo
                canvas.drawText("MOCHAN ASSESSMENT REPORT", startX + logoWidth + spacing, headerYPosition, titlePaint)

                Log.d(TAG, "Logo positioned at: $startX, Text at: ${startX + logoWidth + spacing}, Text width: $textWidth")
            } ?: run {
                // If logo not available, just draw centered text
                val titlePaint = Paint().apply {
                    color = Color.parseColor("#1F2937")
                    textSize = 28f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("MOCHAN ASSESSMENT REPORT", 297.5f, headerYPosition, titlePaint)
            }

            var yPosition = headerYPosition + 40f

            onProgress(50)

            // Draw line
            val linePaint = Paint().apply {
                color = Color.parseColor("#E5E7EB")
                strokeWidth = 2f
            }
            canvas.drawLine(50f, yPosition, 545f, yPosition, linePaint)
            yPosition += 30f

            // Date
            val dateFormat = SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault())
            canvas.drawText("Report Generated: ${dateFormat.format(Date())}", 50f, yPosition,
                Paint().apply {
                    color = Color.parseColor("#4B5563")
                    textSize = 10f
                }
            )
            yPosition += 30f

            onProgress(60)

            // Patient Information Section (left-aligned)
            val headerPaint = Paint().apply {
                color = Color.parseColor("#4F46E5")
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("PATIENT INFORMATION", 50f, yPosition, headerPaint)
            yPosition += 25f

            // Draw info box
            val infoBoxPaint = Paint().apply {
                color = Color.parseColor("#F9FAFB")
                style = Paint.Style.FILL
            }
            canvas.drawRect(50f, yPosition - 15f, 545f, yPosition + 85f, infoBoxPaint)

            val textPaint = Paint().apply {
                color = Color.parseColor("#4B5563")
                textSize = 11f
            }
            val boldTextPaint = Paint().apply {
                color = Color.parseColor("#1F2937")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            // Patient details
            canvas.drawText("Name: $userName", 70f, yPosition + 5f, boldTextPaint)
            canvas.drawText("Age: $userAge years", 70f, yPosition + 25f, textPaint)
            canvas.drawText("Gender: $userGender", 70f, yPosition + 45f, textPaint)
            canvas.drawText("ID: $anonymousId", 320f, yPosition + 5f, textPaint)
            canvas.drawText("Registration: $registrationId", 320f, yPosition + 25f, textPaint)

            yPosition += 100f

            onProgress(70)

            // Assessment Results Section
            canvas.drawText("ASSESSMENT RESULTS", 50f, yPosition, headerPaint)
            yPosition += 30f

            // PHQ-9 Results Box
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
            canvas.drawText("📋 PHQ-9 QUESTIONNAIRE", 70f, yPosition + 5f, sectionPaint)
            canvas.drawText("Severity: ${phq9Severity.level}", 70f, yPosition + 30f, boldTextPaint)
            canvas.drawText(phq9Severity.description, 70f, yPosition + 50f, textPaint)

            yPosition += 80f

            // AI Results (if available)
            if (aiData != null) {
                val aiBoxPaint = Paint().apply {
                    color = Color.parseColor("#F5F3FF")
                    style = Paint.Style.FILL
                }
                canvas.drawRect(50f, yPosition - 10f, 545f, yPosition + 100f, aiBoxPaint)

                canvas.drawText("🤖 AI FACIAL ANALYSIS", 70f, yPosition + 5f, sectionPaint)
                canvas.drawText("Assessment: ${aiData.label}", 70f, yPosition + 30f, boldTextPaint)
                canvas.drawText("Frames Analyzed: ${aiData.frameCount}", 70f, yPosition + 90f, textPaint)

                yPosition += 130f

                // Recommendation based on severity
                val recommendation = when (phq9Severity.level) {
                    "Mild" -> "✓ Continue with self-care practices and monitor your wellbeing"
                    "Moderate" -> "⚠️ Consider consulting with a mental health professional"
                    else -> "❗ We strongly recommend seeking professional help"
                }

                val agreementPaint = when (phq9Severity.level) {
                    "Mild" -> Paint().apply { color = Color.parseColor("#10B981") }
                    "Moderate" -> Paint().apply { color = Color.parseColor("#F59E0B") }
                    else -> Paint().apply { color = Color.parseColor("#EF4444") }
                }.apply {
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }

                canvas.drawText(recommendation, 70f, yPosition, agreementPaint)
                yPosition += 30f
            }

            onProgress(80)

            // Recommendations Section
            canvas.drawText("RECOMMENDATIONS", 50f, yPosition, headerPaint)
            yPosition += 30f

            // Calculate height needed for recommendations
            val recCount = phq9Severity.recommendations.size
            val recBoxHeight = (recCount * 20f) + 20f

            // Draw recommendations box
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

            onProgress(85)

            // Crisis Resources Section
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

            // Footer
            val footerPaint = Paint().apply {
                color = Color.parseColor("#9CA3AF")
                textSize = 8f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("This report is for informational purposes only and is not a medical diagnosis.", 297.5f, yPosition, footerPaint)
            canvas.drawText("Always consult with qualified mental health professionals.", 297.5f, yPosition + 12f, footerPaint)

            // Finish page
            document.finishPage(page)

            onProgress(90)

            // Write to file
            val fileOutputStream = FileOutputStream(pdfFile)
            document.writeTo(fileOutputStream)
            document.close()
            fileOutputStream.close()

            onProgress(100)

            Log.d(TAG, "PDF Report saved: ${pdfFile.absolutePath}")

            // Show success notification
            NotificationHelper.showDownloadSuccess(context, pdfFile.absolutePath)

            pdfFile.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF report: ${e.message}")
            e.printStackTrace()

            // Show error notification
            NotificationHelper.showDownloadError(context, e.message ?: "Unknown error occurred")

            null
        }
    }
}