package com.example.unifiedapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.abs
import kotlin.math.max

class FaceLandmarkerHelper(
    var minFaceDetectionConfidence: Float = 0.7F,
    var minFaceTrackingConfidence: Float = 0.8F,
    var minFacePresenceConfidence: Float = 0.7F,
    var maxNumFaces: Int = DEFAULT_NUM_FACES,
    var currentDelegate: Int = DELEGATE_CPU,
    var runningMode: RunningMode = RunningMode.IMAGE,
    val context: Context,
    val faceLandmarkerHelperListener: LandmarkerListener? = null
) {
    private var faceLandmarker: FaceLandmarker? = null
    private var au17History = 0f
    private val au17SmoothingFactor = 0.3f
    private var isInitialized = false  // ✅ ADD THIS FLAG

    init {
        setupFaceLandmarker()
    }

    fun clearFaceLandmarker() {
        try {
            faceLandmarker?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing faceLandmarker: ${e.message}")
        }
        faceLandmarker = null
        au17History = 0f
        isInitialized = false
    }

    fun isClose(): Boolean {
        return faceLandmarker == null
    }

    fun isReady(): Boolean = isInitialized && faceLandmarker != null  // ✅ ADD THIS

    fun setupFaceLandmarker() {
        try {
            val baseOptionBuilder = BaseOptions.builder()

            when (currentDelegate) {
                DELEGATE_CPU -> baseOptionBuilder.setDelegate(Delegate.CPU)
                DELEGATE_GPU -> baseOptionBuilder.setDelegate(Delegate.GPU)
            }

            baseOptionBuilder.setModelAssetPath(MP_FACE_LANDMARKER_TASK)

            try {
                val baseOptions = baseOptionBuilder.build()

                val optionsBuilder =
                    FaceLandmarker.FaceLandmarkerOptions.builder()
                        .setBaseOptions(baseOptions)
                        .setMinFaceDetectionConfidence(minFaceDetectionConfidence)
                        .setMinTrackingConfidence(minFaceTrackingConfidence)
                        .setMinFacePresenceConfidence(minFacePresenceConfidence)
                        .setNumFaces(maxNumFaces)
                        .setOutputFaceBlendshapes(true)
                        .setRunningMode(runningMode)

                if (runningMode == RunningMode.LIVE_STREAM) {
                    optionsBuilder
                        .setResultListener(this::returnLivestreamResult)
                        .setErrorListener(this::returnLivestreamError)
                }

                val options = optionsBuilder.build()
                faceLandmarker = FaceLandmarker.createFromOptions(context, options)
                isInitialized = true  // ✅ MARK AS INITIALIZED
                Log.d(TAG, "FaceLandmarker initialized successfully")

            } catch (e: Exception) {
                Log.e(TAG, "MediaPipe init error: ${e.message}")
                isInitialized = false
                faceLandmarkerHelperListener?.onError("Face detection unavailable: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "FaceLandmarker setup failed: ${e.message}")
            isInitialized = false
            faceLandmarkerHelperListener?.onError("Failed to initialize face detection")
        }
    }

    fun detectLiveStream(
        imageProxy: ImageProxy,
        isFrontCamera: Boolean
    ) {
        // ✅ CHECK IF INITIALIZED BEFORE PROCESSING
        if (!isInitialized || faceLandmarker == null) {
            imageProxy.close()
            return
        }

        try {
            if (runningMode != RunningMode.LIVE_STREAM) {
                Log.e(TAG, "Wrong running mode")
                imageProxy.close()
                return
            }

            val frameTime = SystemClock.uptimeMillis()

            val bitmapBuffer = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )

            imageProxy.use {
                bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
            }

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer,
                0,
                0,
                bitmapBuffer.width,
                bitmapBuffer.height,
                matrix,
                true
            )

            val mpImage = BitmapImageBuilder(rotatedBitmap).build()

            detectAsync(mpImage, frameTime)

        } catch (e: Exception) {
            Log.e(TAG, "Crash in detectLiveStream: ${e.message}")
            imageProxy.close()
        }
    }

    @VisibleForTesting
    fun detectAsync(mpImage: MPImage, frameTime: Long) {
        try {
            faceLandmarker?.detectAsync(mpImage, frameTime)
        } catch (e: Exception) {
            Log.e(TAG, "detectAsync error: ${e.message}")
        }
    }

    fun detectImage(image: Bitmap): ResultBundle? {
        if (!isInitialized || faceLandmarker == null) {
            Log.w(TAG, "FaceLandmarker not initialized, cannot detect image")
            return null
        }

        return try {
            val startTime = SystemClock.uptimeMillis()
            val mpImage = BitmapImageBuilder(image).build()

            val detectionResult = faceLandmarker?.detect(mpImage)

            if (detectionResult != null) {
                val inferenceTime = SystemClock.uptimeMillis() - startTime

                ResultBundle(
                    detectionResult,
                    inferenceTime,
                    image.height,
                    image.width
                )
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "detectImage error: ${e.message}")
            null
        }
    }

    private fun computeAU17Enhanced(
        faceLandmarks: List<NormalizedLandmark>
    ): Float {
        return try {
            if (faceLandmarks.size < 468) {
                return au17History * (1f - au17SmoothingFactor)
            }

            val chinTip = faceLandmarks[152]
            val lowerLip = faceLandmarks[17]
            val jawLine = faceLandmarks[18]

            val primaryDistance = lowerLip.y() - chinTip.y()
            val jawValidation = abs(jawLine.y() - chinTip.y())

            val rawValue = primaryDistance * (1f + jawValidation)
            val scaled = max(0f, rawValue * 40f)
            val clamped = scaled.coerceIn(0f, 5f)

            val smoothed = clamped * (1f - au17SmoothingFactor) +
                    au17History * au17SmoothingFactor

            au17History = smoothed
            smoothed

        } catch (e: Exception) {
            Log.e(TAG, "AU17 error: ${e.message}")
            au17History
        }
    }

    private fun returnLivestreamResult(
        result: FaceLandmarkerResult,
        input: MPImage
    ) {
        try {
            if (result.faceLandmarks().isNotEmpty()) {

                for (faceLandmarks in result.faceLandmarks()) {
                    val au17 = computeAU17Enhanced(faceLandmarks)
                    faceLandmarkerHelperListener?.onAU17(au17)
                }

                faceLandmarkerHelperListener?.onResults(
                    ResultBundle(result, 0, input.height, input.width)
                )

            } else {
                faceLandmarkerHelperListener?.onEmpty()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in returnLivestreamResult: ${e.message}")
        }
    }

    private fun returnLivestreamError(error: RuntimeException) {
        Log.e(TAG, "MediaPipe error: ${error.message}")
        faceLandmarkerHelperListener?.onError("MediaPipe error: ${error.message}")
    }

    companion object {
        const val TAG = "FaceLandmarkerHelper"
        private const val MP_FACE_LANDMARKER_TASK = "face_landmarker.task"
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_NUM_FACES = 1
        const val OTHER_ERROR = 0
    }

    data class ResultBundle(
        val result: FaceLandmarkerResult,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    interface LandmarkerListener {
        fun onError(error: String, errorCode: Int = OTHER_ERROR)
        fun onResults(resultBundle: ResultBundle)
        fun onAU17(au17: Float)
        fun onEmpty() {}
    }
}