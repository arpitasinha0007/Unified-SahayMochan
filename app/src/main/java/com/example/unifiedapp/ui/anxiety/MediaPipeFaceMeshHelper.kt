package com.example.unifiedapp.ui.anxiety



import java.util.concurrent.Executors


import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

class MediaPipeFaceMeshHelper(
    private val context: Context,
    private val onLandmarksDetected: (FaceLandmarkerResult) -> Unit
) {

    private var faceLandmarker: FaceLandmarker? = null
    private val tag = "MediaPipeHelper"

    // Landmark indices for facial features
    companion object {
        val FACIAL_FEATURES = mapOf(
            "left_eye" to listOf(33, 133, 157, 158, 159, 160, 161, 173, 246, 249, 263, 466),
            "right_eye" to listOf(362, 263, 387, 386, 385, 384, 398, 466, 414, 286, 258, 257),
            "nose" to listOf(1, 2, 4, 5, 6, 19, 94, 98, 168, 195, 197, 209, 219, 275, 440, 445),
            "mouth" to listOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 78, 95, 88, 178, 87, 14, 57, 287),
            "jaw" to listOf(172, 136, 150, 149, 176, 148, 152, 377, 400, 378, 379, 397, 365, 380),
            "forehead" to listOf(10, 338, 297, 332, 284, 251, 389, 356, 70, 63, 105, 66, 107, 55, 285),
            "eyebrows" to listOf(70, 63, 105, 66, 107, 55, 285, 296, 334, 293, 300, 168),
            "cheeks" to listOf(117, 118, 119, 120, 121, 128, 206, 207, 210, 211, 212, 213, 214, 352, 353)
        )
    }

    init {
        setupFaceLandmarker()
    }

    private fun setupFaceLandmarker() {
        try {
            val options = FaceLandmarker.FaceLandmarkerOptions.builder()
                .setBaseOptions(BaseOptions.builder().build())
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumFaces(1)
                .setMinFaceDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinFacePresenceConfidence(0.5f)
                .setOutputFaceBlendshapes(false)
                .setOutputFacialTransformationMatrixes(false)
                .setResultListener { result, image ->
                    onLandmarksDetected(result)
                }
                .setErrorListener { error ->
                    Log.e(tag, "Error: ${error.message}")
                }
                .build()

            faceLandmarker = FaceLandmarker.createFromOptions(context, options)
            Log.d(tag, "FaceLandmarker initialized successfully")

        } catch (e: Exception) {
            Log.e(tag, "Error initializing FaceLandmarker", e)
        }
    }

    fun processFrame(bitmap: Bitmap, timestamp: Long) {
        try {
            val mpImage = BitmapImageBuilder(bitmap).build()
            faceLandmarker?.detectAsync(mpImage, timestamp)
        } catch (e: Exception) {
            Log.e(tag, "Error processing frame", e)
        }
    }

    fun close() {
        faceLandmarker?.close()
    }
}