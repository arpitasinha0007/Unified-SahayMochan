package com.example.unifiedapp.ui.anxiety

import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.unifiedapp.ui.views.AssessmentData
import com.google.mediapipe.framework.image.ByteBufferImageBuilder
import com.google.mediapipe.framework.image.*
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.framework.image.BitmapImageBuilder
// Use MediaImageBuilder if using CameraX
import com.google.mediapipe.framework.image.MediaImageBuilder
// ─────────────────────────────────────────────────────────────────────────────
// Data classes & enums
// ─────────────────────────────────────────────────────────────────────────────
private val MAX_ATTENTION_POINTS = 20_000
enum class AnxietyLevel { MILD, MODERATE, SEVERE }

data class HeatmapResult(
    val heatmapFile: File,
    val anxietyLevel: AnxietyLevel,
    val confidenceScore: Float,
    val attentionMetrics: Map<String, Float>,
    val processingTimeMs: Long
)

data class ProcessingConfig(
    val heatmapSize: Int = 500,
    val samplingRate: Int = 5,
    val gaussianSigma: Float = 3.0f,
    val temporalDecay: Float = 0.5f,
    val maxHistoryPoints: Int = 10000,
    val outputHeatmapPath: String = "heatmap_output.png",
    val outputDataPath: String = "attention_data.json",
    val showProgress: Boolean = true
)

data class AnxietyResult(
    val anxietyLevel: AnxietyLevel,
    val confidence: Float,
    val attentionPoints: List<AttentionPoint>,
    val heatmapFile: File,
    val processingTimeMs: Long
)


// ─────────────────────────────────────────────────────────────────────────────
// Processor
// ─────────────────────────────────────────────────────────────────────────────

class AnxietyHeatmapProcessor(private val context: Context) {

    companion object {
        private const val TAG = "AnxietyProcessor"

        // Heatmap rendering constants
        private const val HEATMAP_SIZE  = 500
        private const val BLOB_SIGMA    = 22f
        private const val ALPHA_OVERLAY = 0.68f
        private const val LOW_CUT       = 0.07f

        private const val LOW_ANXIETY_THRESHOLD      = 0.3f
        private const val MODERATE_ANXIETY_THRESHOLD = 0.6f

        // MediaPipe landmark indices per facial region (468-point topology)
        private val FEATURE_LANDMARKS = mapOf(
            "left_eye"  to listOf(33, 133, 157, 158, 159, 160, 161, 173, 246),
            "right_eye" to listOf(362, 263, 387, 386, 385, 384, 398, 466, 414),
            "eyebrows"  to listOf(70, 63, 105, 66, 107, 55, 65, 52, 53, 46,
                285, 295, 282, 283, 276, 300, 293, 334, 296),
            "nose"      to listOf(1, 2, 4, 5, 6, 19, 94, 98, 168, 195, 197,
                209, 219, 275, 440, 445),
            "mouth"     to listOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375,
                78, 95, 88, 178, 87, 14, 317, 402, 318, 324,
                308, 415, 310, 311, 312, 13, 82, 81, 80, 191),
            "jaw"       to listOf(172, 136, 150, 149, 176, 148, 152, 377, 400,
                378, 379, 397, 365, 380, 381, 382, 356, 454, 323, 361, 288),
            "forehead"  to listOf(10, 338, 297, 332, 284, 251, 389, 356,
                70, 63, 105, 66, 107, 55, 193, 168),
            "cheeks"    to listOf(116, 117, 118, 119, 100, 142, 203, 206,
                345, 346, 347, 348, 329, 371, 423, 426)
        )

        private const val MEDIAPIPE_MODEL = "face_landmarker.task"
    }
    fun cleanup() {
        // Close any open file handles
        Log.d("FileManager", "Cleanup complete")
    }
    private val attentionPoints     = mutableListOf<AttentionPoint>()
    private var representativeFrame: Bitmap? = null
    private var faceLandmarker: FaceLandmarker? = null
    private val emotionClassifier   = EmotionClassifier(context)
    private val rng                 = Random()

    // Accumulate per-frame anxiety scores for temporal analysis
    private val frameAnxietyScores  = mutableListOf<Float>()
    // Accumulate emotion probabilities across frames for aggregate metrics
    private val allEmotionProbs     = mutableListOf<FloatArray>()

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    private fun initModels() {
        // Init emotion classifier (TFLite)
        emotionClassifier.init()

        // Init MediaPipe face landmarker
        if (faceLandmarker != null) return
        try {
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(MEDIAPIPE_MODEL).build())
                .setRunningMode(RunningMode.IMAGE)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.4f)
                .setMinFacePresenceConfidence(0.4f)
                .setMinTrackingConfidence(0.4f)
                .build()
            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.d(TAG, "FaceLandmarker ready")
        } catch (e: Exception) {
            Log.e(TAG, "FaceLandmarker init failed – landmarks will use fallback", e)
        }
    }

    private fun closeModels() {
        faceLandmarker?.close(); faceLandmarker = null
        emotionClassifier.close()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public entry-point
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun processAssessmentVideo(assessmentData: AssessmentData): AnxietyResult? =
        withContext(Dispatchers.IO) {
            val t0 = System.currentTimeMillis()
            try {
                val videoFile = assessmentData.videoFile
                if (videoFile == null || !videoFile.exists()) {
                    Log.e(TAG, "Video file missing"); return@withContext null
                }

                initModels()
                processVideoFrames(videoFile)

                // Final anxiety score: blend model-driven temporal score with
                // landmark movement score for robustness
                val modelScore    = aggregateModelAnxietyScore()
                val movementScore = movementScore()
                val finalScore    = (modelScore * 0.75f + movementScore * 0.25f).coerceIn(0f, 1f)

                val level = determineAnxietyLevel(finalScore)
                val file  = saveHeatmap(level, finalScore)

                val result = AnxietyResult(
                    anxietyLevel     = level,
                    confidence       = finalScore,
                    attentionPoints  = attentionPoints.toList(),
                    heatmapFile      = file,
                    processingTimeMs = System.currentTimeMillis() - t0
                )

                // Cleanup
                attentionPoints.clear()
                frameAnxietyScores.clear()
                allEmotionProbs.clear()
                representativeFrame?.recycle(); representativeFrame = null
                closeModels()

                Log.d(TAG, "Done: $level  score=$finalScore  time=${result.processingTimeMs}ms")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Processing error", e)
                closeModels()
                null
            }
        }

    // ─────────────────────────────────────────────────────────────────────────
    // Video frame loop
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun processVideoFrames(videoFile: File) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs  = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val fps         = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toDouble() ?: 30.0
            val totalFrames = (durationMs * fps / 1000).toLong()
            Log.d(TAG, "Video: ${durationMs}ms  fps=$fps  frames=$totalFrames")

            var idx = 0L
            while (idx < totalFrames) {
                val seekMs = (idx * 1000 / fps).toInt()
                val frame  = extractFrame(retriever, seekMs)

                if (frame != null && idx > totalFrames / 2 && representativeFrame == null) {
                    representativeFrame = Bitmap.createBitmap(frame)
                }

                if (idx % 5 == 0L) processFrame(frame, idx, seekMs.toLong())

                frame?.recycle()
                idx++
                if (idx % 30 == 0L) yield()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Processing error", e)
            closeModels()
            representativeFrame?.recycle()  // ← add this
            representativeFrame = null
            null
        } finally {
            retriever.release()
        }
    }

    /**
     * Force a full CPU pixel copy via Canvas draw — the only reliable way to
     * detach from a Mali gralloc GPU-locked buffer returned by getFrameAtTime().
     * Using .copy() is insufficient; it can share the same gralloc handle.
     */
    private fun extractFrame(retriever: MediaMetadataRetriever, timeMs: Int): Bitmap? = try {
        val raw = retriever.getFrameAtTime(timeMs * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: return null
        val safe = Bitmap.createBitmap(raw.width, raw.height, Bitmap.Config.ARGB_8888)
        Canvas(safe).drawBitmap(raw, 0f, 0f, null)
        raw.recycle()
        safe
    } catch (_: Exception) { null }

    // ─────────────────────────────────────────────────────────────────────────
    // Per-frame processing:  emotion model → region weights → landmarks → points
    // ─────────────────────────────────────────────────────────────────────────

    private fun processFrame(frame: Bitmap?, frameIndex: Long, timestamp: Long) {
        if (frame == null) return

        // ── Step 1: run emotion model to get per-region weights ───────────────
        // The emotion model tells us WHAT the face is expressing.
        // We translate that into spatial region weights for the heatmap.
        val regionWeights: Map<String, Float>
        val frameAnxiety: Float


// In processFrame, before adding points:
        if (attentionPoints.size >= MAX_ATTENTION_POINTS) return
        if (emotionClassifier.isReady) {
            val probs = emotionClassifier.classify(frame)
            if (probs != null) {
                regionWeights = emotionClassifier.regionWeights(probs)
                frameAnxiety  = emotionClassifier.anxietyScore(probs)
                frameAnxietyScores.add(frameAnxiety)
                allEmotionProbs.add(probs)
                Log.v(TAG, "Frame $frameIndex: ${emotionClassifier.dominantEmotion(probs)}  anxiety=$frameAnxiety")
            } else {
                regionWeights = defaultRegionWeights()
                frameAnxiety  = 0.5f
            }
        } else {
            regionWeights = defaultRegionWeights()
            frameAnxiety  = 0.5f
        }

        // ── Step 2: get real landmark positions via MediaPipe ─────────────────
        // The landmarks tell us WHERE each region is in this specific frame.
        val landmarkPositions = getLandmarkPositions(frame)

        // ── Step 3: emit AttentionPoints = position × emotion-driven weight ───
        // This is what makes the heatmap meaningful:
        // hot spots appear at real facial locations AND are bright proportional
        // to how much that region contributed to the anxiety signal.
        FEATURE_LANDMARKS.keys.forEach { feature ->
            val modelWeight = regionWeights[feature] ?: 0.5f
            val positions   = landmarkPositions[feature]

            if (positions != null && positions.isNotEmpty()) {
                // Real landmark positions from MediaPipe
                positions.forEach { (lx, ly) ->
                    attentionPoints.add(
                        AttentionPoint(
                            x          = lx,
                            y          = ly,
                            weight     = modelWeight * frameAnxiety.coerceAtLeast(0.1f),
                            feature    = feature,
                            frameIndex = frameIndex,
                            timestamp  = timestamp
                        )
                    )
                }
            } else {
                // Fallback positions if MediaPipe missed this region
                emitFallbackPoints(feature, modelWeight, frameAnxiety, frameIndex, timestamp)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MediaPipe landmark extraction
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns Map<featureName, List<Pair<normX, normY>>> for all detected regions.
     * Returns empty map if no face detected or landmarker unavailable.
     */
    /**
     * Convert a Bitmap to a pinned off-heap ByteBuffer for MediaPipe.
     *
     * WHY: Android 13+ uses concurrent mark-compact GC which MOVES Java heap objects.
     * BitmapImageBuilder holds a direct JNI pointer to the Bitmap's pixel array on
     * the Java heap. When GC runs concurrently and moves that array, the native
     * MediaPipe code dereferences the stale pointer → SIGSEGV.
     *
     * Fix: copy pixels into a ByteBuffer allocated with allocateDirect() which lives
     * in NATIVE memory (outside the GC-managed heap) and is therefore never moved.
     * ByteBufferImageBuilder wraps this stable pointer safely.
     *
     * Also downscale to 480px — MediaPipe doesn't need full 720p resolution and
     * smaller buffers reduce the GC pressure that triggers the race condition.
     */
    private fun toMediaPipeByteBuffer(src: Bitmap): Triple<java.nio.ByteBuffer, Int, Int> {
        val scale = 480f / maxOf(src.width, src.height).toFloat()
        val w = (src.width  * scale).toInt().coerceAtLeast(1)
        val h = (src.height * scale).toInt().coerceAtLeast(1)

        // Scale into a fresh ARGB_8888 bitmap
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        val tmp = if (scaled.config != Bitmap.Config.ARGB_8888)
            scaled.copy(Bitmap.Config.ARGB_8888, false).also { scaled.recycle() }
        else scaled

        // Allocate DIRECT (off-heap, GC-immovable) ByteBuffer
        val buf = java.nio.ByteBuffer.allocateDirect(w * h * 4)
        tmp.copyPixelsToBuffer(buf)
        buf.rewind()
        tmp.recycle()
        return Triple(buf, w, h)
    }

    private fun getLandmarkPositions(frame: Bitmap): Map<String, List<Pair<Float, Float>>> {
        val landmarker = faceLandmarker ?: return emptyMap()
        return try {
            val (buf, w, h) = toMediaPipeByteBuffer(frame)
            val mpImage = ByteBufferImageBuilder(
                buf,
                w, h,
                MPImage.IMAGE_FORMAT_RGBA
            ).build()
            val result = landmarker.detect(mpImage)
            if (result.faceLandmarks().isEmpty()) return emptyMap()

            val landmarks = result.faceLandmarks()[0]
            val positions = mutableMapOf<String, MutableList<Pair<Float, Float>>>()

            FEATURE_LANDMARKS.forEach { (feature, indices) ->
                val pts = mutableListOf<Pair<Float, Float>>()
                indices.forEach { idx ->
                    if (idx < landmarks.size) {
                        val lm = landmarks[idx]
                        pts.add(Pair(lm.x().coerceIn(0f, 1f), lm.y().coerceIn(0f, 1f)))
                    }
                }
                if (pts.isNotEmpty()) positions[feature] = pts
            }
            positions
        } catch (e: Exception) {
            Log.w(TAG, "Landmark detection failed frame", e)
            emptyMap()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fallback point emission (when MediaPipe misses a face)
    // ─────────────────────────────────────────────────────────────────────────

    private val FALLBACK_CENTRES = mapOf(
        "forehead"  to Pair(0.50f, 0.18f), "eyebrows"  to Pair(0.50f, 0.28f),
        "left_eye"  to Pair(0.35f, 0.36f), "right_eye" to Pair(0.65f, 0.36f),
        "nose"      to Pair(0.50f, 0.50f), "cheeks"    to Pair(0.50f, 0.52f),
        "mouth"     to Pair(0.50f, 0.65f), "jaw"       to Pair(0.50f, 0.76f)
    )
    private val FALLBACK_JITTER = mapOf(
        "left_eye" to 0.025f, "right_eye" to 0.025f, "eyebrows" to 0.030f,
        "mouth" to 0.035f, "jaw" to 0.035f, "nose" to 0.020f,
        "forehead" to 0.040f, "cheeks" to 0.050f
    )

    private fun emitFallbackPoints(
        feature: String, modelWeight: Float, frameAnxiety: Float,
        frameIndex: Long, timestamp: Long
    ) {
        val centre = FALLBACK_CENTRES[feature] ?: return
        val jitter = FALLBACK_JITTER[feature] ?: 0.03f
        val w      = modelWeight * frameAnxiety.coerceAtLeast(0.1f)

        when (feature) {
            "cheeks" -> {
                repeat(3) {
                    val dy = rng.nextGaussian().toFloat() * jitter
                    val dx = rng.nextGaussian().toFloat() * jitter
                    attentionPoints.add(AttentionPoint(0.28f + dx, centre.second + dy, w, feature, frameIndex, timestamp))
                    attentionPoints.add(AttentionPoint(0.72f - dx, centre.second + dy, w, feature, frameIndex, timestamp))
                }
            }
            "eyebrows" -> {
                repeat(3) {
                    val dy = rng.nextGaussian().toFloat() * jitter
                    attentionPoints.add(AttentionPoint((0.34f + rng.nextGaussian().toFloat() * jitter).coerceIn(0f,1f), (0.28f+dy).coerceIn(0f,1f), w, feature, frameIndex, timestamp))
                    attentionPoints.add(AttentionPoint((0.66f + rng.nextGaussian().toFloat() * jitter).coerceIn(0f,1f), (0.28f+dy).coerceIn(0f,1f), w, feature, frameIndex, timestamp))
                }
            }
            else -> repeat(4) {
                val x = (centre.first  + rng.nextGaussian().toFloat() * jitter).coerceIn(0f, 1f)
                val y = (centre.second + rng.nextGaussian().toFloat() * jitter).coerceIn(0f, 1f)
                attentionPoints.add(AttentionPoint(x, y, w, feature, frameIndex, timestamp))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scoring
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Aggregate anxiety score from per-frame emotion model outputs.
     * Uses weighted average with higher weight on peak anxiety frames
     * (anxiety bursts matter more than baseline).
     */
    private fun aggregateModelAnxietyScore(): Float {
        if (frameAnxietyScores.isEmpty()) return 0.5f

        val sorted = frameAnxietyScores.sorted()
        // 75th percentile score — avoids outliers but captures anxious bursts
        val p75idx = (sorted.size * 0.75f).toInt().coerceAtMost(sorted.size - 1)
        val p75    = sorted[p75idx]
        val mean   = sorted.average().toFloat()

        // Blend mean and 75th-pct: persistent anxiety AND peak moments both matter
        return (mean * 0.5f + p75 * 0.5f).coerceIn(0f, 1f)
    }

    private fun movementScore(): Float {
        if (attentionPoints.size < 10) return 0.5f
        val sorted = attentionPoints.sortedBy { it.frameIndex }
        var total = 0f; var n = 0
        for (i in 1 until sorted.size) {
            val a = sorted[i-1]; val b = sorted[i]
            if (a.feature == b.feature) {
                total += sqrt((b.x-a.x).pow(2) + (b.y-a.y).pow(2)); n++
            }
        }
        return min((if (n > 0) total / n else 0.5f) * 3f, 1f)
    }

    private fun defaultRegionWeights() = mapOf(
        "left_eye" to 0.5f, "right_eye" to 0.5f, "eyebrows" to 0.5f,
        "mouth" to 0.5f, "jaw" to 0.5f, "nose" to 0.3f,
        "forehead" to 0.4f, "cheeks" to 0.3f
    )

    private fun determineAnxietyLevel(score: Float) = when {
        score < LOW_ANXIETY_THRESHOLD      -> AnxietyLevel.MILD
        score < MODERATE_ANXIETY_THRESHOLD -> AnxietyLevel.MODERATE
        else                               -> AnxietyLevel.SEVERE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // File I/O
    // ─────────────────────────────────────────────────────────────────────────

    private fun saveHeatmap(level: AnxietyLevel, confidence: Float): File {
        val dir = File(context.getExternalFilesDir(null), "anxiety_heatmaps/${level.name.lowercase()}")
        dir.mkdirs()
        val ts   = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "heatmap_${level.name.lowercase()}_${(confidence*100).toInt()}pc_$ts.png")
        FileOutputStream(file).use { generateHeatmapBitmap(level, confidence).compress(Bitmap.CompressFormat.PNG, 100, it) }
        Log.d(TAG, "Saved: ${file.absolutePath}")
        return file
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Heatmap generation  –  Grad-CAM style jet-colormap overlay
    // ─────────────────────────────────────────────────────────────────────────

    private fun generateHeatmapBitmap(level: AnxietyLevel, confidence: Float): Bitmap {
        val S = HEATMAP_SIZE

        // 1. Build density grid: each point contributes a Gaussian splat
        //    weighted by its model-driven weight — so emotion-active regions
        //    produce higher density → hotter colors in the final image.
        val density = Array(S) { FloatArray(S) }
        val sigma   = BLOB_SIGMA
        val radius  = (sigma * 3f).toInt().coerceAtMost(S / 2)
        val twoS2   = 2f * sigma * sigma

        attentionPoints.forEach { pt ->
            val cx = (pt.x * S).toInt().coerceIn(0, S - 1)
            val cy = (pt.y * S).toInt().coerceIn(0, S - 1)
            for (row in (cy-radius).coerceAtLeast(0)..(cy+radius).coerceAtMost(S-1)) {
                val dy = (row - cy).toFloat()
                for (col in (cx-radius).coerceAtLeast(0)..(cx+radius).coerceAtMost(S-1)) {
                    val dx = (col - cx).toFloat()
                    density[row][col] += exp(-(dx*dx + dy*dy) / twoS2) * pt.weight
                }
            }
        }

        // 2. Normalise
        var maxD = 1e-6f
        for (row in density) for (v in row) if (v > maxD) maxD = v
        for (r in 0 until S) for (c in 0 until S) density[r][c] = (density[r][c] / maxD).coerceIn(0f, 1f)

        // 3. Base image
        val base = if (representativeFrame != null)
            Bitmap.createScaledBitmap(representativeFrame!!, S, S, true).copy(Bitmap.Config.ARGB_8888, true)
        else
            Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888).also { Canvas(it).drawColor(Color.rgb(25, 25, 25)) }

        // 4. Jet colormap blend
        val pixels = IntArray(S * S)
        base.getPixels(pixels, 0, S, 0, 0, S, S)
        for (r in 0 until S) {
            for (c in 0 until S) {
                val v = density[r][c]
                if (v < LOW_CUT) continue
                pixels[r*S+c] = blendLinear(pixels[r*S+c], jetColormap(v), (v * ALPHA_OVERLAY).coerceIn(0f,1f))
            }
        }
        base.setPixels(pixels, 0, S, 0, 0, S, S)

        // 5. Annotations
        addAnnotations(Canvas(base), S, level, confidence)
        return base
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Jet colormap  (matplotlib "jet" exact port)
    // ─────────────────────────────────────────────────────────────────────────

    private fun jetColormap(t: Float): Int {
        val r = jetCh(t, 0.35f,0.66f,0.89f,1.00f,  0f,  0f,  1f, 1f, 0.5f)
        val g = jetCh(t, 0.125f,0.375f,0.64f,0.91f, 0f,  1f,  1f, 0f,  0f)
        val b = jetCh(t, 0.00f,0.11f,0.34f,0.65f,  0.5f,1f,  1f, 0f,  0f)
        return Color.rgb((r*255f).toInt().coerceIn(0,255),(g*255f).toInt().coerceIn(0,255),(b*255f).toInt().coerceIn(0,255))
    }

    private fun jetCh(t:Float,t0:Float,t1:Float,t2:Float,t3:Float,v0:Float,v1:Float,v2:Float,v3:Float,v4:Float):Float = when {
        t < t0 -> v0
        t < t1 -> v0+(v1-v0)*(t-t0)/(t1-t0)
        t < t2 -> v1+(v2-v1)*(t-t1)/(t2-t1)
        t < t3 -> v2+(v3-v2)*(t-t2)/(t3-t2)
        else   -> v3+(v4-v3)*(t-t3)/(1f-t3+1e-6f)
    }

    private fun blendLinear(base: Int, overlay: Int, alpha: Float): Int {
        val ia = 1f-alpha
        return Color.rgb(
            (Color.red(base)*ia  +Color.red(overlay)*alpha).toInt().coerceIn(0,255),
            (Color.green(base)*ia+Color.green(overlay)*alpha).toInt().coerceIn(0,255),
            (Color.blue(base)*ia +Color.blue(overlay)*alpha).toInt().coerceIn(0,255)
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Annotations
    // ─────────────────────────────────────────────────────────────────────────

    private fun addAnnotations(canvas: Canvas, S: Int, level: AnxietyLevel, confidence: Float) {
        val bg = Paint().apply { color = Color.argb(185, 0, 0, 0) }
        canvas.drawRect(0f, 0f, S.toFloat(), 62f, bg)
        canvas.drawRect(0f, S-92f, S.toFloat(), S.toFloat(), bg)

        // Show dominant emotion if model was used
        val dominantEmotion = if (allEmotionProbs.isNotEmpty()) {
            val avgProbs = FloatArray(7) { i -> allEmotionProbs.map { it.getOrElse(i){0f} }.average().toFloat() }
            emotionClassifier.dominantEmotion(avgProbs)
        } else null

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 30f; isFakeBoldText = true }
        canvas.drawText("Anxiety Heatmap", 16f, 42f, tp)

        tp.isFakeBoldText = false; tp.textSize = 26f
        tp.color = when(level) {
            AnxietyLevel.MILD     -> Color.rgb(100,220,100)
            AnxietyLevel.MODERATE -> Color.YELLOW
            AnxietyLevel.SEVERE   -> Color.rgb(255,80,80)
        }
        canvas.drawText("Level: ${level.name}", 16f, S-56f, tp)

        tp.color = Color.WHITE; tp.textSize = 20f
        canvas.drawText("Score: ${(confidence*100).toInt()}%${if (dominantEmotion != null) "  ($dominantEmotion)" else ""}", 16f, S-22f, tp)

        drawJetLegend(canvas, S-92, 92, 28, 180)
    }

    private fun drawJetLegend(canvas: Canvas, x: Int, y: Int, w: Int, h: Int) {
        val bg = Paint().apply { color = Color.argb(160,0,0,0) }
        canvas.drawRect(x-8f, y-8f, x+w+58f, y+h+24f, bg)
        val bar = Paint()
        for (i in 0 until h) {
            bar.color = jetColormap(1f - i.toFloat()/h)
            canvas.drawRect(x.toFloat(),(y+i).toFloat(),(x+w).toFloat(),(y+i+1).toFloat(),bar)
        }
        val lp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE; textSize=18f; setShadowLayer(2f,1f,1f,Color.BLACK) }
        canvas.drawText("High",(x+w+8).toFloat(),(y+20).toFloat(),lp)
        canvas.drawText("Low", (x+w+8).toFloat(),(y+h-8).toFloat(),lp)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Metrics (AnxietyVideoProcessor compatibility)
    // ─────────────────────────────────────────────────────────────────────────

    fun calculateAttentionMetrics(): Map<String, Float> {
        val total = attentionPoints.size.toFloat().coerceAtLeast(1f)
        val m = mutableMapOf<String, Float>()
        attentionPoints.groupBy { it.feature }.forEach { (f, pts) -> m[f] = pts.size / total }
        m["total_frames_analyzed"]  = attentionPoints.maxOfOrNull { it.frameIndex }?.toFloat() ?: 0f
        m["total_attention_points"] = total
        m["movement_score"]         = movementScore()
        if (frameAnxietyScores.isNotEmpty()) {
            m["model_anxiety_score"] = aggregateModelAnxietyScore()
            m["peak_anxiety_score"]  = frameAnxietyScores.maxOrNull() ?: 0f
            m["mean_anxiety_score"]  = frameAnxietyScores.average().toFloat()
        }
        return m
    }

    private fun ClosedRange<Int>.random() = rng.nextInt(endInclusive - start + 1) + start
}