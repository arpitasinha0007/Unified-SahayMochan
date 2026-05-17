package com.example.unifiedapp.ui.anxiety

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Wraps a TFLite emotion classification model (MobileNetV2 / EfficientNet-Lite
 * fine-tuned on AffectNet-7 or RAF-DB).
 *
 * Expected model I/O
 * ──────────────────
 * Input  : [1, 224, 224, 3]  float32  RGB  normalised to [-1, 1]
 * Output : [1, 7]            float32  softmax probabilities
 *
 * Label order (AffectNet-7 standard):
 *   0 = Neutral   1 = Happy    2 = Sad      3 = Surprise
 *   4 = Fear      5 = Disgust  6 = Anger
 *
 * ── Getting the model ───────────────────────────────────────────────────────
 * Option 1 – Pretrained (recommended for quick start):
 *   Download emotion_mobilenetv2_affectnet.tflite from:
 *   https://github.com/HSE-asavchenko/face-emotion-recognition
 *   and place it in  app/src/main/assets/
 *
 * Option 2 – Convert your own:
 *   from deepface import DeepFace  # or use keras/pytorch
 *   # Export to TFLite with float16 quantization for smaller size
 *
 * ── Anxiety-relevant emotions ────────────────────────────────────────────────
 * Anxiety manifests across several FACS-coded expressions:
 *   Fear     → most direct marker  (AU1+2+4+5+20+26)
 *   Anger    → secondary marker    (AU4+5+7+23+24)
 *   Sad      → co-occurring state  (AU1+4+15+17)
 *   Disgust  → mild marker
 *   Surprise → can be anxiety-adjacent
 *   Neutral  → absence of distress
 *   Happy    → absence of anxiety
 */
class EmotionClassifier(private val context: Context) {

    companion object {
        private const val TAG           = "EmotionClassifier"
        private const val MODEL_FILE    = "emotion_mobilenetv2_affectnet.tflite"
        private const val INPUT_SIZE    = 224
        private const val NUM_CLASSES   = 7
        private const val PIXEL_NORM    = 127.5f   // (pixel / 127.5) - 1.0 → [-1, 1]

        // Emotion label indices (AffectNet-7 order)
        const val IDX_NEUTRAL  = 0
        const val IDX_HAPPY    = 1
        const val IDX_SAD      = 2
        const val IDX_SURPRISE = 3
        const val IDX_FEAR     = 4
        const val IDX_DISGUST  = 5
        const val IDX_ANGER    = 6

        val EMOTION_LABELS = arrayOf(
            "Neutral", "Happy", "Sad", "Surprise", "Fear", "Disgust", "Anger"
        )

        // ── Anxiety contribution weight per emotion ──────────────────────────
        // Based on FACS research linking Action Units to anxious appearance.
        // Fear and Anger are strongest anxiety markers; Happy is negative.
        val ANXIETY_CONTRIBUTION = floatArrayOf(
            0.05f,   // Neutral  → very low anxiety signal
            -0.15f,  // Happy    → negative: presence reduces anxiety score
            0.30f,   // Sad      → moderate anxiety co-occurrence
            0.20f,   // Surprise → can indicate startle / hypervigilance
            0.85f,   // Fear     → primary anxiety marker
            0.25f,   // Disgust  → mild anxiety co-occurrence
            0.45f    // Anger    → secondary anxiety marker (tension/agitation)
        )

        // ── Per-emotion facial region activation patterns ────────────────────
        // These define which facial regions "light up" for each emotion,
        // based on FACS Action Unit maps. Used to distribute model confidence
        // spatially onto the heatmap regions.
        //
        // Map: emotion_index → Map<region_name, activation_strength [0-1]>
        val EMOTION_REGION_ACTIVATION = mapOf(
            IDX_NEUTRAL to mapOf(
                "forehead" to 0.1f, "eyebrows" to 0.1f,
                "left_eye" to 0.1f, "right_eye" to 0.1f,
                "nose" to 0.1f, "cheeks" to 0.1f,
                "mouth" to 0.1f, "jaw" to 0.1f
            ),
            IDX_HAPPY to mapOf(
                "forehead" to 0.1f, "eyebrows" to 0.2f,
                "left_eye" to 0.5f, "right_eye" to 0.5f,   // AU6: cheek raiser / eye narrowing
                "nose" to 0.1f, "cheeks" to 0.8f,          // AU6: cheek raise
                "mouth" to 0.9f, "jaw" to 0.3f             // AU12/25: lip corner pull
            ),
            IDX_SAD to mapOf(
                "forehead" to 0.4f,
                "eyebrows" to 0.8f,                        // AU1: inner brow raise
                "left_eye" to 0.5f, "right_eye" to 0.5f,
                "nose" to 0.2f, "cheeks" to 0.3f,
                "mouth" to 0.7f,                           // AU15: lip corner depressor
                "jaw" to 0.4f
            ),
            IDX_SURPRISE to mapOf(
                "forehead" to 0.7f,                        // AU1+2: brow raise
                "eyebrows" to 0.9f,                        // AU1+2: brow raise
                "left_eye" to 0.8f, "right_eye" to 0.8f,  // AU5: upper lid raiser
                "nose" to 0.2f, "cheeks" to 0.2f,
                "mouth" to 0.8f,                           // AU26/27: jaw drop
                "jaw" to 0.7f
            ),
            IDX_FEAR to mapOf(
                "forehead" to 0.6f,                        // AU1+2+4: all brow raises
                "eyebrows" to 0.95f,                       // AU1+2+4: brow raise + furrow
                "left_eye" to 0.9f, "right_eye" to 0.9f,  // AU5+7: lid tensions
                "nose" to 0.3f, "cheeks" to 0.3f,
                "mouth" to 0.8f,                           // AU20+26: lip stretch
                "jaw" to 0.5f
            ),
            IDX_DISGUST to mapOf(
                "forehead" to 0.3f,
                "eyebrows" to 0.6f,                        // AU4: brow lowerer
                "left_eye" to 0.4f, "right_eye" to 0.4f,
                "nose" to 0.9f,                            // AU9: nose wrinkle
                "cheeks" to 0.5f,
                "mouth" to 0.8f,                           // AU16+25: lip depressor
                "jaw" to 0.3f
            ),
            IDX_ANGER to mapOf(
                "forehead" to 0.5f,
                "eyebrows" to 0.9f,                        // AU4: brow lowerer
                "left_eye" to 0.7f, "right_eye" to 0.7f,  // AU5+7: lid tension
                "nose" to 0.4f, "cheeks" to 0.4f,
                "mouth" to 0.7f,                           // AU23+24: lip tighten
                "jaw" to 0.6f                              // AU28: jaw clench
            )
        )
    }

    private var interpreter: Interpreter? = null
    private val inputBuffer  = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        .apply { order(ByteOrder.nativeOrder()) }
    private val outputBuffer = Array(1) { FloatArray(NUM_CLASSES) }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    fun init(): Boolean {
        return try {
            val afd: AssetFileDescriptor = context.assets.openFd(MODEL_FILE)
            val model: MappedByteBuffer = afd.createInputStream().channel
                .map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
            val options = Interpreter.Options().apply {
                numThreads = 2
                useXNNPACK = true
            }
            interpreter = Interpreter(model, options)
            Log.d(TAG, "Emotion model loaded: $MODEL_FILE")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load emotion model – will use fallback scoring", e)
            false
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    val isReady get() = interpreter != null

    // ─────────────────────────────────────────────────────────────────────────
    // Inference
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Run emotion inference on a face crop.
     * @param faceBitmap  Any-size bitmap of the face region (will be resized internally).
     * @return FloatArray of length 7 — softmax probabilities in AffectNet-7 order,
     *         or null if the model is not loaded.
     */
    fun classify(faceBitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: return null
        return try {
            // 1. Resize to model input size
            val resized = Bitmap.createScaledBitmap(faceBitmap, INPUT_SIZE, INPUT_SIZE, true)

            // 2. Fill input buffer: RGB float32 normalised to [-1, 1]
            inputBuffer.rewind()
            val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
            resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
            if (resized != faceBitmap) resized.recycle()

            for (px in pixels) {
                inputBuffer.putFloat(((px shr 16 and 0xFF).toFloat() / PIXEL_NORM) - 1f)  // R
                inputBuffer.putFloat(((px shr  8 and 0xFF).toFloat() / PIXEL_NORM) - 1f)  // G
                inputBuffer.putFloat(((px        and 0xFF).toFloat() / PIXEL_NORM) - 1f)  // B
            }

            // 3. Run inference
            outputBuffer[0].fill(0f)
            interp.run(inputBuffer, outputBuffer)

            outputBuffer[0].copyOf()
        } catch (e: Exception) {
            Log.w(TAG, "Inference error", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Score helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compute a single anxiety score [0,1] from a probability vector.
     * Weighted sum of per-emotion anxiety contributions.
     */
    fun anxietyScore(probs: FloatArray): Float {
        var score = 0f
        for (i in probs.indices) {
            score += probs[i] * ANXIETY_CONTRIBUTION.getOrElse(i) { 0f }
        }
        return score.coerceIn(0f, 1f)
    }

    /**
     * Compute per-region activation weights driven by model output.
     * Each region weight = sum over emotions of (prob[e] * anxietyContrib[e] * regionActivation[e][region])
     * Result is normalised so the max region = 1.0.
     */
    fun regionWeights(probs: FloatArray): Map<String, Float> {
        val regions = listOf("forehead", "eyebrows", "left_eye", "right_eye",
            "nose", "cheeks", "mouth", "jaw")
        val raw = mutableMapOf<String, Float>()

        regions.forEach { region ->
            var w = 0f
            for (emotionIdx in 0 until NUM_CLASSES) {
                val contrib    = ANXIETY_CONTRIBUTION.getOrElse(emotionIdx) { 0f }.coerceAtLeast(0f)
                val activation = EMOTION_REGION_ACTIVATION[emotionIdx]?.get(region) ?: 0.1f
                w += probs.getOrElse(emotionIdx) { 0f } * contrib * activation
            }
            raw[region] = w
        }

        // Normalise to [0.2, 1.0] so no region is completely invisible
        val maxW = raw.values.maxOrNull()?.coerceAtLeast(1e-6f) ?: 1e-6f
        return raw.mapValues { (_, v) -> (v / maxW).coerceIn(0.2f, 1.0f) }
    }

    /**
     * Dominant emotion label from a probability vector.
     */
    fun dominantEmotion(probs: FloatArray): String {
        val idx = probs.indices.maxByOrNull { probs[it] } ?: 0
        return EMOTION_LABELS.getOrElse(idx) { "Unknown" }
    }

    /**
     * Compute detailed attention metrics for AnxietyVideoProcessor compatibility.
     */
    fun calculateDetailedMetrics(attentionPoints: List<AttentionPoint>): Map<String, Float> {
        val total = attentionPoints.size.toFloat().coerceAtLeast(1f)
        val m = mutableMapOf<String, Float>()
        attentionPoints.groupBy { it.feature }.forEach { (f, pts) -> m[f] = pts.size / total }
        m["total_attention_points"] = total
        return m
    }
}