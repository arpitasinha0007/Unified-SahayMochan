package com.example.unifiedapp.ui.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

// Helper class to serve as a wrapper which abstrcts the MediaPipe Face Landmarker Task API complexities.
// It is responsible for initialization of face landmarker with configurable parameters such as confidence thresholding
// and run mode. It consumes images from the camera's live feed, passes them to the MediaPipe task for detection,
//  and sends the result (landmark information) or an error through a listener interface back to the caller fragment
//   in a systematic way.

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

    // ✅ NEW: Shutdown flag
    private var isShuttingDown = AtomicBoolean(false)

    private lateinit var yuvConverter: YuvToRgbConverter
    private lateinit var rgbBitmap: Bitmap
    private var faceLandmarker: FaceLandmarker? = null

    init {
        yuvConverter = YuvToRgbConverter(context)   // 👈 ADD THIS
        setupFaceLandmarker()
    }

    fun clearFaceLandmarker() {
        faceLandmarker?.close()
        faceLandmarker = null
    }

    fun setupFaceLandmarker() {
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
        } catch (e: Exception) {
            faceLandmarkerHelperListener?.onError("Face Landmarker failed to initialize.")
            Log.e(TAG, "MediaPipe failed to load the task with error: ${e.message}")
        }
    }

    @OptIn(ExperimentalGetImage::class)
    fun detectLiveStream(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        // ✅ Check if shutting down or helper is invalid
        if (isShuttingDown.get() || faceLandmarker == null) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        if (!::rgbBitmap.isInitialized ||
            rgbBitmap.width != imageProxy.width ||
            rgbBitmap.height != imageProxy.height
        ) {
            rgbBitmap = Bitmap.createBitmap(
                imageProxy.width,
                imageProxy.height,
                Bitmap.Config.ARGB_8888
            )
        }

        try {
            yuvConverter.yuvToRgb(mediaImage, rgbBitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting YUV to RGB: ${e.message}")
            imageProxy.close()
            return
        }

        imageProxy.close()

        val matrix = Matrix().apply {
            postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
            if (isFrontCamera) {
                postScale(-1f, 1f, rgbBitmap.width.toFloat(), rgbBitmap.height.toFloat())
            }
        }

        val rotatedBitmap = Bitmap.createBitmap(
            rgbBitmap,
            0,
            0,
            rgbBitmap.width,
            rgbBitmap.height,
            matrix,
            true
        )

        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        try {
            faceLandmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Error in detectAsync: ${e.message}")
        }
    }

    private fun returnLivestreamResult(result: FaceLandmarkerResult, input: MPImage) {
        // ✅ Don't send results if shutting down
        if (isShuttingDown.get()) {
            return
        }

        if (result.faceLandmarks().isNotEmpty()) {
            val finishTimeMs = SystemClock.uptimeMillis()
            val inferenceTime = finishTimeMs - result.timestampMs()
            faceLandmarkerHelperListener?.onResults(
                ResultBundle(result, inferenceTime, input.height, input.width)
            )
        } else {
            faceLandmarkerHelperListener?.onEmpty()
        }
    }

    private fun returnLivestreamError(error: RuntimeException) {
        // ✅ Don't send errors if shutting down
        if (isShuttingDown.get()) {
            return
        }
        faceLandmarkerHelperListener?.onError(error.message ?: "An unknown error has occurred")
    }

    // ✅ NEW: Shutdown method for safe cleanup
    fun shutdown() {
        isShuttingDown.set(true)
        clearFaceLandmarker()
    }

    companion object {
        const val TAG = "FaceLandmarkerHelper"
        private const val MP_FACE_LANDMARKER_TASK = "face_landmarker.task"
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DEFAULT_FACE_DETECTION_CONFIDENCE = 0.5F
        const val DEFAULT_FACE_TRACKING_CONFIDENCE = 0.5F
        const val DEFAULT_FACE_PRESENCE_CONFIDENCE = 0.5F
        const val DEFAULT_NUM_FACES = 1
        const val OTHER_ERROR = 0
        const val GPU_ERROR = 1
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