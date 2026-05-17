// VideoFacialHeatmapProcessor.kt
package com.example.unifiedapp.ui.anxiety

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sqrt

class VideoFacialHeatmapProcessor(
    private val context: Context,
    private val config: ProcessingConfig = ProcessingConfig()
) {

    companion object {
        val FEATURE_WEIGHTS = mapOf(
            "left_eye" to 2.2f,
            "right_eye" to 2.2f,
            "nose" to 1.2f,
            "mouth" to 1.8f,
            "jaw" to 0.9f,
            "forehead" to 0.8f,
            "eyebrows" to 1.3f,
            "cheeks" to 0.7f
        )
    }

    private val attentionPoints = mutableListOf<AttentionPoint>()

    data class ProcessingStats(
        var totalFrames: Long = 0,
        var processedFrames: Long = 0,
        var facesDetected: Long = 0,
        var startTime: Long = 0,
        var endTime: Long = 0,
        var videoWidth: Int = 0,
        var videoHeight: Int = 0,
        var frameRate: Double = 0.0
    )

    private val stats = ProcessingStats()

    suspend fun processVideoFile(
        videoFile: File,
        onProgress: (Int, String) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {

        stats.startTime = System.currentTimeMillis()

        try {
            onProgress(10, "Extracting video frames...")

            // Extract and process frames
            processVideoFrames(videoFile, onProgress)

            onProgress(80, "Generating heatmap...")

            // Generate heatmap
            val heatmapFile = generateHeatmapImage(videoFile.nameWithoutExtension)

            stats.endTime = System.currentTimeMillis()

            onProgress(100, "Heatmap generated successfully")

            Result.success(heatmapFile)

        } catch (e: Exception) {
            Log.e("VideoHeatmapProcessor", "Error processing video", e)
            Result.failure(e)
        }
    }

    private suspend fun processVideoFrames(
        videoFile: File,
        onProgress: (Int, String) -> Unit
    ) {
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(videoFile.absolutePath)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0
            stats.frameRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toDouble() ?: 30.0
            stats.totalFrames = (duration * stats.frameRate / 1000).toLong()

            // Process every Nth frame based on sampling rate
            val samplingRate = config.samplingRate
            var frameIndex = 0L
            var processedCount = 0L

            while (frameIndex < stats.totalFrames) {
                val seekTime = (frameIndex * 1000 / stats.frameRate).toInt()

                val frame = extractFrame(retriever, seekTime)

                if (frameIndex % samplingRate == 0L) {
                    processFrame(frame, frameIndex, seekTime.toLong())
                    processedCount++

                    val progress = 10 + ((frameIndex.toFloat() / stats.totalFrames) * 70).toInt()
                    onProgress(progress, "Processing frames: $processedCount")
                }

                frame?.recycle()
                frameIndex++

                if (frameIndex % 30 == 0L) {
                    yield()
                }
            }

            stats.processedFrames = processedCount

        } finally {
            retriever.release()
        }
    }

    private fun extractFrame(retriever: MediaMetadataRetriever, timeMs: Int): Bitmap? {
        return try {
            retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            null
        }
    }

    private fun processFrame(frame: Bitmap?, frameIndex: Long, timestamp: Long) {
        if (frame == null) return

        // Simulate face detection
        val features = listOf("left_eye", "right_eye", "nose", "mouth", "jaw", "forehead", "eyebrows", "cheeks")

        features.forEach { feature ->
            val weight = FEATURE_WEIGHTS[feature] ?: 1.0f

            // Simulate 3-7 detection points per feature
            val pointCount = (3..7).random()
            repeat(pointCount) {
                val x = 0.2f + (0..100).random().toFloat() / 100f * 0.6f
                val y = 0.2f + (0..100).random().toFloat() / 100f * 0.6f

                attentionPoints.add(
                    AttentionPoint(
                        x = x,
                        y = y,
                        weight = weight,
                        feature = feature,
                        frameIndex = frameIndex,
                        timestamp = timestamp
                    )
                )
            }
        }

        stats.facesDetected++
    }

    private fun generateHeatmapImage(videoName: String): File {
        val size = config.heatmapSize
        val heatmapData = Array(size) { FloatArray(size) }

        // Find min/max frame index for temporal weighting
        val maxFrameIndex = attentionPoints.maxOfOrNull { it.frameIndex } ?: 0
        val minFrameIndex = attentionPoints.minOfOrNull { it.frameIndex } ?: 0
        val frameRange = (maxFrameIndex - minFrameIndex).toFloat()

        // Accumulate points
        attentionPoints.forEach { point ->
            val temporalFactor = if (frameRange > 0 && config.temporalDecay > 0) {
                1.0f - ((maxFrameIndex - point.frameIndex) / frameRange) * config.temporalDecay
            } else {
                1.0f
            }

            val weight = point.weight * temporalFactor

            val x = (point.x * (size - 1)).toInt().coerceIn(0, size - 1)
            val y = (point.y * (size - 1)).toInt().coerceIn(0, size - 1)

            // Apply Gaussian distribution
            val radius = (config.gaussianSigma * 2).toInt()
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx in 0 until size && ny in 0 until size) {
                        val distance = sqrt((dx * dx + dy * dy).toFloat())
                        val gaussianWeight = exp(-(distance * distance) / (2 * config.gaussianSigma * config.gaussianSigma))
                        heatmapData[ny][nx] += weight * gaussianWeight
                    }
                }
            }
        }

        // Normalize
        val maxValue = heatmapData.flatMap { it.asIterable() }.maxOrNull() ?: 1f
        if (maxValue > 0) {
            for (y in 0 until size) {
                for (x in 0 until size) {
                    heatmapData[y][x] /= maxValue
                }
            }
        }

        // Create bitmap
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        for (y in 0 until size) {
            for (x in 0 until size) {
                bitmap.setPixel(x, y, getHeatmapColor(heatmapData[y][x]))
            }
        }

        // Save to app storage
        val file = File(context.getExternalFilesDir(null), "heatmap_${videoName}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        return file
    }

    private fun getHeatmapColor(intensity: Float): Int {
        return when {
            intensity < 0.2f -> {
                val blue = (255 * (intensity / 0.2f)).toInt()
                android.graphics.Color.rgb(0, 0, blue)
            }
            intensity < 0.4f -> {
                val green = (255 * ((intensity - 0.2f) / 0.2f)).toInt()
                android.graphics.Color.rgb(0, green, 255)
            }
            intensity < 0.6f -> {
                val red = (255 * ((intensity - 0.4f) / 0.2f)).toInt()
                android.graphics.Color.rgb(red, 255, 255 - red)
            }
            intensity < 0.8f -> {
                val red = 255
                val green = 255
                val blue = (255 * (1 - (intensity - 0.6f) / 0.2f)).toInt()
                android.graphics.Color.rgb(red, green, blue)
            }
            else -> {
                val red = 255
                val green = (255 * (1 - (intensity - 0.8f) / 0.2f)).toInt()
                android.graphics.Color.rgb(red, green, 0)
            }
        }
    }

    fun getAttentionPoints(): List<AttentionPoint> = attentionPoints.toList()

    fun getStats(): ProcessingStats = stats.copy()

    fun clearData() {
        attentionPoints.clear()
    }
}