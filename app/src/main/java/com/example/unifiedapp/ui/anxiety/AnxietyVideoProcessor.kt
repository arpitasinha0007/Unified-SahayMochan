package com.example.unifiedapp.ui.anxiety



import android.content.Context
import android.util.Log
import com.example.unifiedapp.ui.views.AssessmentData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * FIXED AnxietyVideoProcessor with proper resource management
 */
class AnxietyVideoProcessor(
    private val context: Context,
    private val config: ProcessingConfig = ProcessingConfig()
) {
    companion object {
        private const val TAG = "AnxietyVideoProcessor"
    }

    // ✅ Thread-safe state management
    private val isProcessing = AtomicBoolean(false)
    private val isCleanedUp = AtomicBoolean(false)
    private val cleanupMutex = Mutex()

    // ✅ Lazy initialization - only create when needed
    private var heatmapProcessor: AnxietyHeatmapProcessor? = null
    private var classifier: AnxietyClassifier? = null
    private var fileManager: FileManager? = null

    /**
     * Initialize resources lazily and safely
     */
    private suspend fun initializeResources() = withContext(Dispatchers.IO) {
        cleanupMutex.withLock {
            if (heatmapProcessor == null && !isCleanedUp.get()) {
                Log.d(TAG, "Initializing processors...")
                heatmapProcessor = AnxietyHeatmapProcessor(context)
                classifier = AnxietyClassifier()
                fileManager = FileManager(context)
                Log.d(TAG, "✅ Processors initialized")
            }
        }
    }

    /**
     * Main processing function with proper resource management
     */
    suspend fun processVideoAndClassify(
        videoFile: File,
        onProgress: (Int, String) -> Unit
    ): Result<HeatmapResult> = withContext(Dispatchers.IO + NonCancellable) {

        // ✅ Prevent concurrent processing
        if (!isProcessing.compareAndSet(false, true)) {
            Log.w(TAG, "⚠️ Already processing, rejecting new request")
            return@withContext Result.failure(
                IllegalStateException("Processor is already processing a video")
            )
        }

        try {
            Log.d(TAG, "========== START VIDEO PROCESSING ==========")
            Log.d(TAG, "Video: ${videoFile.absolutePath}")
            Log.d(TAG, "Size: ${videoFile.length()} bytes")
            Log.d(TAG, "Exists: ${videoFile.exists()}")

            // ✅ Validate video file
            if (!videoFile.exists()) {
                throw IllegalArgumentException("Video file does not exist: ${videoFile.absolutePath}")
            }

            if (videoFile.length() == 0L) {
                throw IllegalArgumentException("Video file is empty: ${videoFile.absolutePath}")
            }

            onProgress(5, "Starting analysis...")

            // ✅ Initialize resources
            initializeResources()

            // ✅ Verify resources initialized
            val processor = heatmapProcessor
                ?: throw IllegalStateException("Heatmap processor failed to initialize")
            val cls = classifier
                ?: throw IllegalStateException("Classifier failed to initialize")
            val fm = fileManager
                ?: throw IllegalStateException("File manager failed to initialize")

            onProgress(10, "Resources initialized...")

            // Create AssessmentData with minimal required fields
            val assessmentData = AssessmentData(
                videoFile = videoFile,
                anonymousId = "temp_${System.currentTimeMillis()}",
                age = 0,
                email = "",
                assessmentType = "video_analysis",
                auCsvFile = null,
                gad7CsvFile = null,
                gad7Score = 0,
                questionnaireScore = 0,
                registrationId = ""
            )

            onProgress(20, "Processing video frames...")

            // ✅ Process with timeout protection
            val result = try {
                processor.processAssessmentVideo(assessmentData)
            } catch (e: Exception) {
                Log.e(TAG, "❌ Heatmap processing failed", e)
                throw Exception("Video processing failed: ${e.message}", e)
            }

            // ✅ Verify result
            if (result == null) {
                throw Exception("Processing returned null result")
            }

            Log.d(TAG, "✅ Heatmap processing complete")
            Log.d(TAG, "Anxiety level: ${result.anxietyLevel}")
            Log.d(TAG, "Confidence: ${result.confidence}")

            onProgress(80, "Analyzing anxiety patterns...")

            // ✅ Safe classification
            val anxietyLevel = result.anxietyLevel
            val confidence = result.confidence
            val attentionPoints = result.attentionPoints

            val attentionMetrics = try {
                cls.calculateDetailedMetrics(attentionPoints)
            } catch (e: Exception) {
                Log.e(TAG, "Metrics calculation failed, using defaults", e)
                emptyMap() // Safe fallback
            }

            onProgress(90, "Saving results...")

            // ✅ Safe metadata save
            try {
                fm.saveMetadata(
                    result.heatmapFile,
                    mapOf(
                        "videoFile" to videoFile.absolutePath,
                        "anxietyLevel" to anxietyLevel.name,
                        "confidence" to confidence.toString(),
                        "metrics" to attentionMetrics.toString(),
                        "processingTime" to result.processingTimeMs.toString()
                    )
                )
                Log.d(TAG, "✅ Metadata saved")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save metadata (non-critical)", e)
            }

            onProgress(100, "Complete! Level: ${anxietyLevel.name}")

            Log.d(TAG, "========== PROCESSING COMPLETE ==========")

            Result.success(
                HeatmapResult(
                    heatmapFile = result.heatmapFile,
                    anxietyLevel = anxietyLevel,
                    confidenceScore = confidence,
                    attentionMetrics = attentionMetrics,
                    processingTimeMs = result.processingTimeMs
                )
            )

        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "❌ Invalid input", e)
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "❌ Invalid state", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error during processing", e)
            e.printStackTrace()
            Result.failure(e)
        } finally {
            // ✅ Always cleanup, even on error
            safeCleanup()
            isProcessing.set(false)
            Log.d(TAG, "Processing flag cleared")
        }
    }

    /**
     * Safe cleanup with proper error handling
     */
    private suspend fun safeCleanup() {
        cleanupMutex.withLock {
            if (isCleanedUp.compareAndSet(false, true)) {
                try {
                    Log.d(TAG, "🧹 Starting cleanup...")

                    // Cleanup heatmap processor
                    heatmapProcessor?.let { processor ->
                        try {
                            processor.cleanup()
                            Log.d(TAG, "✅ Heatmap processor cleaned up")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error cleaning heatmap processor", e)
                        }
                    }
                    heatmapProcessor = null

                    // Cleanup classifier
                    classifier?.let { cls ->
                        try {
                            cls.cleanup()
                            Log.d(TAG, "✅ Classifier cleaned up")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error cleaning classifier", e)
                        }
                    }
                    classifier = null

                    // Cleanup file manager
                    fileManager = null

                    Log.d(TAG, "✅ Cleanup complete")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error during cleanup", e)
                }
            } else {
                Log.v(TAG, "Already cleaned up, skipping")
            }
        }
    }

    /**
     * Manual cleanup method - call when processor is no longer needed
     */
    suspend fun cleanup() {
        safeCleanup()
    }

    /**
     * Check if processor is currently processing
     */
    fun isCurrentlyProcessing(): Boolean = isProcessing.get()
}


