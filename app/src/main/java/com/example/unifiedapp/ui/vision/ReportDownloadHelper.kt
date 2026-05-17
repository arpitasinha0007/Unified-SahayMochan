package com.example.unifiedapp.vision

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.util.Log
import com.example.unifiedapp.vision.NotificationHelper
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.example.unifiedapp.R

object ReportDownloadHelper {
    private const val TAG = "ReportDownloadHelper"

    data class AiPredictionData(
        val anxietyPrediction: String,
        val anxietyScore: Float,
        val anxietyConfidence: Float
    )

    data class ResultStateData(
        val title: String,
        val message: String,
        val emoji: String,
        val color: Int
    )

    // Severity levels matching the 3-category system
    enum class SeverityLevel {
        MILD, MODERATE, SEVERE
    }

    fun generateReport(
        context: Context,
        score: Int,
        resultState: ResultStateData,
        aiData: AiPredictionData?,
        anonymousId: String = "user"
    ): String? {
        var fileOutputStream: FileOutputStream? = null
        var document: PdfDocument? = null

        return try {
            Log.d(TAG, "Starting report generation...")

            // Determine storage location based on Android version
            val reportsFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10+, use app-specific directory
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Sahay/Reports")
            } else {
                // For older versions, try Downloads folder
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, "Sahay/Reports")
            }

            Log.d(TAG, "Reports folder: ${reportsFolder.absolutePath}")

            // Create directories if they don't exist
            if (!reportsFolder.exists()) {
                val created = reportsFolder.mkdirs()
                Log.d(TAG, "Created reports folder: $created")
                if (!created) {
                    Log.e(TAG, "Failed to create reports folder")
                    // Try alternative location
                    val altFolder = File(context.getExternalFilesDir(null), "SahayReports")
                    altFolder.mkdirs()
                    return generateReportAlternative(context, score, resultState, aiData, anonymousId, altFolder)
                }
            }

            // Check if we can write to the folder
            if (!reportsFolder.canWrite()) {
                Log.e(TAG, "Cannot write to reports folder")
                val altFolder = File(context.getExternalFilesDir(null), "SahayReports")
                altFolder.mkdirs()
                return generateReportAlternative(context, score, resultState, aiData, anonymousId, altFolder)
            }

            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "sahay_mental_health_report_${timestamp}.pdf"
            val pdfFile = File(reportsFolder, fileName)

            Log.d(TAG, "PDF file path: ${pdfFile.absolutePath}")

            // Create PDF document
            document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Professional color palette
            val primaryColor = Color.parseColor("#2C3E50")      // Dark blue-gray
            val accentColor = Color.parseColor("#3498DB")       // Professional blue
            val textPrimary = Color.parseColor("#2C3E50")       // Dark blue-gray
            val textSecondary = Color.parseColor("#7F8C8D")     // Gray
            val borderColor = Color.parseColor("#ECF0F1")       // Light gray
            val mildColor = Color.parseColor("#27AE60")         // Green for mild
            val moderateColor = Color.parseColor("#F39C12")     // Orange for moderate
            val severeColor = Color.parseColor("#E74C3C")       // Red for severe
            val aiColor = Color.parseColor("#8E44AD")           // Purple for AI section

            // Determine severity from result state message
            val severity = determineSeverity(resultState.message)
            val severityColor = when (severity) {
                SeverityLevel.MILD -> mildColor
                SeverityLevel.MODERATE -> moderateColor
                SeverityLevel.SEVERE -> severeColor
            }

            // Paint configurations - Professional typography
            val headerLargePaint = Paint().apply {
                color = primaryColor
                textSize = 24f
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            }

            val headerMediumPaint = Paint().apply {
                color = primaryColor
                textSize = 18f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }

            val headerSmallPaint = Paint().apply {
                color = primaryColor
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }

            val bodyTextPaint = Paint().apply {
                color = textPrimary
                textSize = 11f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }

            val bodyTextBoldPaint = Paint().apply {
                color = textPrimary
                textSize = 11f
                typeface = Typeface.create("sans-serif", Typeface.BOLD)
            }

            val secondaryTextPaint = Paint().apply {
                color = textSecondary
                textSize = 10f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }

            val smallTextPaint = Paint().apply {
                color = textSecondary
                textSize = 8f
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }

            var yPosition = 50f
            val leftMargin = 50f
            val rightMargin = 545f

            // Draw header with logo and title
            try {
                // Try to load logo from drawable folder
                val logoBitmap = BitmapFactory.decodeResource(context.resources,
                    android.R.drawable.ic_menu_gallery)  // Temporary test - use this first to verify

                // Once verified working, replace above line with:
                // val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img)

                if (logoBitmap != null) {
                    val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 80, 80, false)
                    canvas.drawBitmap(scaledLogo, leftMargin, yPosition - 40f, null)
                    scaledLogo.recycle()
                    logoBitmap.recycle()
                } else {
                    drawLogoFallback(canvas, leftMargin, yPosition, accentColor)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Logo not found in drawable, using fallback", e)
                drawLogoFallback(canvas, leftMargin, yPosition, accentColor)
            }

            // Title and subtitle
            canvas.drawText("SAHAY", leftMargin + 100f, yPosition - 15f, headerLargePaint)
            canvas.drawText("AI-Powered Mental Health Assessment", leftMargin + 100f, yPosition + 5f, secondaryTextPaint)

            // Date and reference number on the right
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentDate = Date()

            canvas.drawText("Report ID: SAH-${timestamp}", rightMargin - 150f, yPosition - 15f, smallTextPaint)
            canvas.drawText("Date: ${dateFormat.format(currentDate)}", rightMargin - 150f, yPosition + 5f, smallTextPaint)
            canvas.drawText("Time: ${timeFormat.format(currentDate)}", rightMargin - 150f, yPosition + 20f, smallTextPaint)

            yPosition += 60f

            // Horizontal divider
            val dividerPaint = Paint().apply {
                color = borderColor
                strokeWidth = 1f
            }
            canvas.drawLine(leftMargin, yPosition, rightMargin, yPosition, dividerPaint)
            yPosition += 25f

            // Assessment Summary Section
            canvas.drawText("ASSESSMENT SUMMARY", leftMargin, yPosition, headerMediumPaint)
            yPosition += 25f

            // Draw severity box
            val severityRectPaint = Paint().apply {
                color = severityColor
                alpha = 20
                style = Paint.Style.FILL
            }
            canvas.drawRect(leftMargin, yPosition - 5f, rightMargin, yPosition + 80f, severityRectPaint)

            // Severity indicator bar
            val severityBarPaint = Paint().apply {
                color = severityColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(leftMargin, yPosition - 5f, leftMargin + 5f, yPosition + 80f, severityBarPaint)

            // Severity title (clean without emoji)
            val severityTitlePaint = Paint().apply {
                color = severityColor
                textSize = 28f
                typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            }

            // Clean title without emoji
            val cleanTitle = resultState.title.replace(Regex("[🌱🤝🫂🌟🍃]"), "").trim()
            canvas.drawText(cleanTitle, leftMargin + 20f, yPosition + 25f, severityTitlePaint)

            // Classification based on severity
            val classificationText = when (severity) {
                SeverityLevel.MILD -> "Mild Anxiety - Early signs detected, good time for self-care"
                SeverityLevel.MODERATE -> "Moderate Anxiety - Additional support and coping strategies recommended"
                SeverityLevel.SEVERE -> "Severe Anxiety - Professional support is strongly recommended"
            }

            val classificationPaint = Paint().apply {
                color = severityColor
                textSize = 14f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            }
            canvas.drawText(classificationText, leftMargin + 20f, yPosition + 50f, classificationPaint)

            yPosition += 100f

            // ASSESSMENT DETAILS Section (replaces questionnaire assessment)
            canvas.drawText("ASSESSMENT DETAILS", leftMargin, yPosition, headerSmallPaint)
            yPosition += 20f

            val detailsBoxPaint = Paint().apply {
                color = borderColor
                alpha = 30
                style = Paint.Style.FILL
            }
            canvas.drawRect(leftMargin, yPosition - 5f, rightMargin, yPosition + 80f, detailsBoxPaint)

            // Use the message but clean it up
            val cleanMessage = resultState.message.replace(Regex("[🌱🤝🫂🌟🍃]"), "").trim()
            val interpretationLines = wrapText(cleanMessage, 85)
            var interpretationY = yPosition + 5f
            interpretationLines.forEach { line ->
                canvas.drawText(line, leftMargin + 10f, interpretationY, bodyTextPaint)
                interpretationY += 18f
            }

            yPosition = interpretationY + 25f

            // AI Analysis Section (if available)
            if (aiData != null) {
                canvas.drawText("AI FACIAL EXPRESSION ANALYSIS", leftMargin, yPosition, headerSmallPaint)
                yPosition += 20f

                val aiBoxPaint = Paint().apply {
                    color = aiColor
                    alpha = 15
                    style = Paint.Style.FILL
                }

                val aiBoxHeight = if (aiData.anxietyConfidence > 0) 110f else 90f
                canvas.drawRect(leftMargin, yPosition - 5f, rightMargin, yPosition + aiBoxHeight, aiBoxPaint)

                val aiBarPaint = Paint().apply {
                    color = aiColor
                    style = Paint.Style.FILL
                }
                canvas.drawRect(leftMargin, yPosition - 5f, leftMargin + 3f, yPosition + aiBoxHeight, aiBarPaint)

                canvas.drawText("AI Model Analysis:", leftMargin + 15f, yPosition + 8f, bodyTextBoldPaint)

                val aiPredictionText = when {
                    aiData.anxietyPrediction.contains("mild", ignoreCase = true) ->
                        "Mild anxiety indicators detected in facial expressions"
                    aiData.anxietyPrediction.contains("moderate", ignoreCase = true) ->
                        "Moderate anxiety indicators detected in facial expressions"
                    aiData.anxietyPrediction.contains("severe", ignoreCase = true) ->
                        "Significant anxiety indicators detected in facial expressions"
                    else ->
                        "AI analysis completed - Facial expression patterns analyzed"
                }
                canvas.drawText("• $aiPredictionText", leftMargin + 15f, yPosition + 28f, bodyTextPaint)

                canvas.drawText("• Analysis based on computer vision of facial expression patterns",
                    leftMargin + 15f, yPosition + 48f, secondaryTextPaint)


                canvas.drawText("Note: AI analysis is experimental and should be considered alongside self-assessment",
                    leftMargin + 15f, yPosition + (if (aiData.anxietyConfidence > 0) 88f else 68f), smallTextPaint)

                yPosition += aiBoxHeight + 20f
            }

            // Recommendations Section
            canvas.drawText("RECOMMENDATIONS", leftMargin, yPosition, headerSmallPaint)
            yPosition += 20f

            val recommendations = getRecommendations(severity, aiData)
            val recBoxHeight = (recommendations.size * 45f) + 15f

            val recBoxPaint = Paint().apply {
                color = borderColor
                alpha = 30
                style = Paint.Style.FILL
            }
            canvas.drawRect(leftMargin, yPosition - 5f, rightMargin, yPosition + recBoxHeight, recBoxPaint)

            var recYPos = yPosition + 5f
            recommendations.forEach { rec ->
                canvas.drawText("• ${rec.title}", leftMargin + 10f, recYPos, bodyTextBoldPaint)
                recYPos += 18f

                val descLines = wrapText(rec.description, 75)
                descLines.forEach { line ->
                    canvas.drawText("  $line", leftMargin + 20f, recYPos, bodyTextPaint)
                    recYPos += 16f
                }
                recYPos += 8f
            }

            yPosition = recYPos + 15f

            // Self-Care Strategies
            canvas.drawText("SELF-CARE STRATEGIES", leftMargin, yPosition, headerSmallPaint)
            yPosition += 20f

            val selfCareStrategies = getSelfCareStrategies(severity)
            val strategiesHeight = (selfCareStrategies.size * 30f) + 15f

            val strategiesBoxPaint = Paint().apply {
                color = borderColor
                alpha = 30
                style = Paint.Style.FILL
            }
            canvas.drawRect(leftMargin, yPosition - 5f, rightMargin, yPosition + strategiesHeight, strategiesBoxPaint)

            var strategyYPos = yPosition + 5f
            selfCareStrategies.forEach { strategy ->
                canvas.drawText("• $strategy", leftMargin + 10f, strategyYPos, bodyTextPaint)
                strategyYPos += 25f
            }

            yPosition = strategyYPos + 15f

            // Draw resources section
            drawResourcesSection(canvas, leftMargin, rightMargin, yPosition, smallTextPaint, bodyTextPaint, bodyTextBoldPaint, headerSmallPaint, borderColor)

            // Finish the page
            document.finishPage(page)

            // Write to file
            fileOutputStream = FileOutputStream(pdfFile)
            document.writeTo(fileOutputStream)

            Log.d(TAG, "Mental health report saved: ${pdfFile.absolutePath}")

            // Show notification
            try {
                NotificationHelper.showDownloadSuccessNotification(context, pdfFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show notification", e)
            }

            pdfFile.absolutePath

        } catch (e: Exception) {
            Log.e(TAG, "Error generating mental health report", e)
            e.printStackTrace()

            // Try alternative location
            return try {
                val altFolder = File(context.getExternalFilesDir(null), "SahayReports")
                altFolder.mkdirs()
                generateReportAlternative(context, score, resultState, aiData, anonymousId, altFolder)
            } catch (altError: Exception) {
                Log.e(TAG, "Alternative report generation also failed", altError)
                NotificationHelper.showDownloadErrorNotification(context, e.message ?: "Unknown error")
                null
            }
        } finally {
            // Clean up resources
            try {
                fileOutputStream?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing file output stream", e)
            }

            try {
                document?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing document", e)
            }
        }
    }

    private fun determineSeverity(message: String): SeverityLevel {
        return when {
            message.contains("Mild", ignoreCase = true) -> SeverityLevel.MILD
            message.contains("Moderate", ignoreCase = true) -> SeverityLevel.MODERATE
            message.contains("Severe", ignoreCase = true) -> SeverityLevel.SEVERE
            else -> SeverityLevel.MILD // Default to mild
        }
    }

    private fun generateReportAlternative(
        context: Context,
        score: Int,
        resultState: ResultStateData,
        aiData: AiPredictionData?,
        anonymousId: String,
        folder: File
    ): String? {
        return try {
            Log.d(TAG, "Using alternative folder: ${folder.absolutePath}")

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "sahay_mental_health_report_${timestamp}.pdf"
            val pdfFile = File(folder, fileName)

            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Simple report for alternative location
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 16f
            }

            canvas.drawText("SAHAY Mental Health Report", 50f, 50f, paint)

            val severity = determineSeverity(resultState.message)
            canvas.drawText("Result: ${resultState.title}", 50f, 100f, paint)
            canvas.drawText("Assessment: ${resultState.message}", 50f, 150f, paint)

            if (aiData != null) {
                canvas.drawText("AI Analysis: ${aiData.anxietyPrediction}", 50f, 200f, paint)
            }

            canvas.drawText("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 50f, 250f, paint)

            document.finishPage(page)

            val fileOutputStream = FileOutputStream(pdfFile)
            document.writeTo(fileOutputStream)
            document.close()
            fileOutputStream.close()

            Log.d(TAG, "Alternative report saved: ${pdfFile.absolutePath}")

            try {
                NotificationHelper.showDownloadSuccessNotification(context, pdfFile.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show notification", e)
            }

            pdfFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Alternative report generation failed", e)
            null
        }
    }

    private fun drawLogoFallback(canvas: Canvas, leftMargin: Float, yPosition: Float, accentColor: Int) {
        val logoPlaceholder = Paint().apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRect(leftMargin, yPosition - 40f, leftMargin + 80f, yPosition + 40f, logoPlaceholder)

        val logoTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 30f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("S", leftMargin + 40f, yPosition + 10f, logoTextPaint)
    }

    private fun drawResourcesSection(
        canvas: Canvas,
        leftMargin: Float,
        rightMargin: Float,
        yPosition: Float,
        smallTextPaint: Paint,
        bodyTextPaint: Paint,
        bodyTextBoldPaint: Paint,
        headerSmallPaint: Paint,
        borderColor: Int
    ) {
        var yPos = yPosition

        // Support Resources
        canvas.drawText("SUPPORT RESOURCES", leftMargin, yPos, headerSmallPaint)
        yPos += 20f

        val resourcesBoxPaint = Paint().apply {
            color = borderColor
            alpha = 30
            style = Paint.Style.FILL
        }
        canvas.drawRect(leftMargin, yPos - 5f, rightMargin, yPos + 90f, resourcesBoxPaint)

        canvas.drawText("• National Crisis Hotline: 988 (24/7, Confidential)", leftMargin + 10f, yPos + 5f, bodyTextPaint)
        canvas.drawText("• Crisis Text Line: Text HOME to 741741", leftMargin + 10f, yPos + 25f, bodyTextPaint)
        canvas.drawText("• Emergency Services: 911 (For immediate danger)", leftMargin + 10f, yPos + 45f, bodyTextPaint)
        canvas.drawText("• SAMHSA National Helpline: 1-800-662-4357", leftMargin + 10f, yPos + 65f, bodyTextPaint)
        canvas.drawText("• Find a Therapist: Psychology Today or BetterHelp", leftMargin + 10f, yPos + 85f, bodyTextPaint)

        yPos += 110f

        // Footer with disclaimer
        val footerPaint = Paint().apply {
            color = Color.parseColor("#95A5A6")
            textSize = 7f
            textAlign = Paint.Align.CENTER
        }

        val disclaimerPaint = Paint().apply {
            color = Color.parseColor("#BDC3C7")
            textSize = 6f
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("CONFIDENTIAL: This report contains sensitive health information.",
            297.5f, yPos, footerPaint)
        canvas.drawText("This AI-assisted assessment is for informational purposes only and does not constitute a medical diagnosis.",
            297.5f, yPos + 10f, footerPaint)
        canvas.drawText("AI facial analysis is experimental and should be interpreted with caution.",
            297.5f, yPos + 20f, footerPaint)
        canvas.drawText("Always consult with qualified mental health professionals for proper evaluation and treatment.",
            297.5f, yPos + 30f, footerPaint)
        canvas.drawText("© Sahay Mental Health | Generated on ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}",
            297.5f, yPos + 45f, disclaimerPaint)
    }

    private data class Recommendation(
        val title: String,
        val description: String
    )

    private fun getRecommendations(severity: SeverityLevel, aiData: AiPredictionData?): List<Recommendation> {
        val recommendations = mutableListOf<Recommendation>()

        // Add AI-specific recommendation if applicable
        if (aiData != null) {
            when {
                aiData.anxietyPrediction.contains("mild", ignoreCase = true) ->
                    recommendations.add(
                        Recommendation(
                            "AI-Detected Indicators",
                            "Facial expression analysis identified patterns consistent with mild anxiety. This observation may provide additional context for your self-assessment."
                        )
                    )
                aiData.anxietyPrediction.contains("moderate", ignoreCase = true) ->
                    recommendations.add(
                        Recommendation(
                            "AI-Detected Indicators",
                            "Facial expression analysis identified patterns consistent with moderate anxiety. Consider this alongside your self-assessment."
                        )
                    )
                aiData.anxietyPrediction.contains("severe", ignoreCase = true) ->
                    recommendations.add(
                        Recommendation(
                            "AI-Detected Indicators",
                            "Facial expression analysis identified significant anxiety indicators. We encourage you to reach out for support."
                        )
                    )
            }
        }

        // Add severity-based recommendations
        recommendations.addAll(
            when (severity) {
                SeverityLevel.MILD -> listOf(
                    Recommendation(
                        "Maintain Healthy Routines",
                        "Your responses indicate mild anxiety. Continue your existing self-care practices and consider incorporating stress-management techniques."
                    ),
                    Recommendation(
                        "Preventive Monitoring",
                        "Consider periodic self-assessments to track any changes in your anxiety levels over time."
                    ),
                    Recommendation(
                        "Lifestyle Balance",
                        "Evaluate sleep, exercise, and caffeine intake - small adjustments can help manage mild anxiety."
                    )
                )
                SeverityLevel.MODERATE -> listOf(
                    Recommendation(
                        "Professional Consultation Recommended",
                        "Consider speaking with a mental health professional to develop personalized coping strategies."
                    ),
                    Recommendation(
                        "Structured Support",
                        "Identify trusted individuals you can reach out to when feeling overwhelmed."
                    ),
                    Recommendation(
                        "Practice Grounding Techniques",
                        "When feeling anxious, try the 5-4-3-2-1 technique: Name 5 things you see, 4 you can touch, 3 you hear, 2 you smell, and 1 you taste."
                    )
                )
                SeverityLevel.SEVERE -> listOf(
                    Recommendation(
                        "Professional Support Strongly Recommended",
                        "We strongly encourage you to connect with a mental health professional for comprehensive evaluation and support."
                    ),
                    Recommendation(
                        "Crisis Resources Available",
                        "Save crisis hotline numbers in your phone. Help is available 24/7 at 988."
                    ),
                    Recommendation(
                        "Safety Planning",
                        "Consider developing a safety plan with a trusted person or professional."
                    ),
                    Recommendation(
                        "Reach Out Today",
                        "Don't wait - contact a mental health professional or crisis service today. You don't have to go through this alone."
                    )
                )
            }
        )

        return recommendations
    }

    private fun getSelfCareStrategies(severity: SeverityLevel): List<String> {
        return when (severity) {
            SeverityLevel.MILD -> listOf(
                "Morning mindfulness: 5-minute meditation or deep breathing exercises",
                "Regular physical activity: 30-minute walks 3-4 times weekly",
                "Digital boundaries: Screen-free time before bed",
                "Social connection: Regular contact with supportive individuals",
                "Gratitude journaling: Write 3 things you're grateful for daily"
            )
            SeverityLevel.MODERATE -> listOf(
                "Breathing technique: Practice diaphragmatic breathing when experiencing stress",
                "Gentle movement: Stretching or yoga 10-15 minutes daily",
                "Sleep hygiene: Maintain consistent sleep/wake schedule",
                "Reflective journaling: Write thoughts to process emotions",
                "Grounding techniques: Practice sensory awareness during acute anxiety"
            )
            SeverityLevel.SEVERE -> listOf(
                "Crisis resources: Save 988 crisis hotline in your phone contacts",
                "Safety planning: Identify safe environments and trusted contacts",
                "Basic needs: Prioritize regular meals and adequate rest",
                "Support network: Maintain daily contact with a trusted individual",
                "Professional help: Schedule an appointment with a mental health provider"
            )
        }
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        if (text.isEmpty()) return listOf("")

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.length + word.length + 1 <= maxCharsPerLine) {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    // Word is longer than maxCharsPerLine
                    val chunks = word.chunked(maxCharsPerLine)
                    lines.addAll(chunks.dropLast(1))
                    currentLine = StringBuilder(chunks.last())
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}