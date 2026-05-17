package com.example.unifiedapp.ui.vision


import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.Executors
import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.os.Environment
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicYuvToRGB
import androidx.camera.video.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner

import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
//
//object server{
//    const val TAG = "SimpleServerClient"
//    private const val SERVER_IP = "10.13.0.113"
//    private const val SERVER_PORT = 5000
//}
//
//private val baseUrl = "http://$server.SERVER_IP:$server.SERVER_PORT/"
//
//var currentRecording: Recording? = null
//
//@Composable
//fun CameraPreview(
//    modifier: Modifier = Modifier,
//    onFrame: (ImageProxy) -> Unit,
//    videoHelper: VideoRecorderHelper?
//) {
//    val context = LocalContext.current
//    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
//    val executor = remember { Executors.newSingleThreadExecutor() }
//
//    val TAG = "SimpleServerClient"
//
//    val serverUrl = baseUrl// your server endpoint
//
//    AndroidView(
//        modifier = modifier,
//        factory = { ctx ->
//            val previewView = PreviewView(ctx).apply {
//                scaleType = PreviewView.ScaleType.FILL_CENTER
//            }
//
//            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
//            cameraProviderFuture.addListener({
//
//                val cameraProvider = cameraProviderFuture.get()
//
//                // Preview setup
//                val preview = Preview.Builder().build()
//                preview.setSurfaceProvider(previewView.surfaceProvider)
//
//                // Image Analysis for frame callbacks
//                val imageAnalysis = ImageAnalysis.Builder()
//                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
//                    .build()
//
//                imageAnalysis.setAnalyzer(executor) { onFrame(it) }
//
//                // Video capture initialization
//                val videoCapture = videoHelper?.initializeVideoCapture(context)
//
////                cameraProvider.unbindAll()
//
//                // Bind lifecycle with/without video capture
//                if (videoCapture != null) {
//                    cameraProvider.bindToLifecycle(
//                        lifecycleOwner,
//                        CameraSelector.DEFAULT_FRONT_CAMERA,
//                        preview,
//                        imageAnalysis,
//                        videoCapture
//                    )
//
//                    // Start recording automatically
//                    val file = videoHelper.createVideoFile(context)
//                    val outputOptions = FileOutputOptions.Builder(file).build()
//
//                    // After recording finishes:
//                    val sizeInBytes = file.length()
//                    val sizeInKB = sizeInBytes / 1024
//                    val sizeInMB = sizeInBytes / (1024 * 1024)
//
//                    currentRecording = videoCapture.output
//                        .prepareRecording(context, outputOptions)
//                        .apply { withAudioEnabled() }
//                        .start(ContextCompat.getMainExecutor(context)) { event ->
//
//                            when (event) {
//
//                                is VideoRecordEvent.Start -> {
//                                    Log.d(TAG, "Recording started")
//                                }
//
//                                is VideoRecordEvent.Finalize -> {
//                                    Log.d(TAG, "Recording finalized: ${event.outputResults.outputUri}")
//                                    val fileUri = event.outputResults.outputUri
//                                    val sizeInBytes = file.length()
//                                    val sizeInMB = sizeInBytes / (1024.0 * 1024.0)
//
//                                    Log.d(TAG, "Recording finalized: $fileUri")
//                                    Log.d(TAG, "Final Video Size: ${String.format("%.2f", sizeInMB)} MB")
//
//                                    if (!event.hasError()) {
//                                        videoHelper.uploadVideo(file, serverUrl)
//                                    } else if(event.hasError()){
//
//                                        Log.e(TAG, "Recording failed!")
//                                        Log.e(TAG, "Error code: ${event.error}")
//
//                                        when (event.error) {
//                                            VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED ->
//                                                Log.e(TAG, "File size limit reached")
//
//                                            VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE ->
//                                                Log.e(TAG, "Not enough storage")
//
//                                            VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE ->
//                                                Log.e(TAG, "Camera became inactive")
//
//                                            else ->
//                                                Log.e(TAG, "Other error")
//                                        }
//
//
//
//                                    }  else
//                                    {
//                                        Log.e(TAG, "Recording error: ${event.error}")
//                                    }
//
//
//                                }
//                            }
//                        }
//
//
//                } else {
//                    cameraProvider.bindToLifecycle(
//                        lifecycleOwner,
//                        CameraSelector.DEFAULT_FRONT_CAMERA,
//                        preview,
//                        imageAnalysis
//                    )
//                }
//
//            }, ContextCompat.getMainExecutor(ctx))
//
//            previewView
//        }
//    )
//}
//
//
//// Create temporary video file
//private fun createVideoFile(context: Context): File {
//    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
//    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
//    return File.createTempFile("VIDEO_$timestamp", ".mp4", storageDir)
//}
//
//// Upload MP4 to server
//private fun uploadVideo(file: File, serverUrl: String) {
//    CoroutineScope(Dispatchers.IO).launch {
//        try {
//            val client = OkHttpClient()
//            val requestBody = MultipartBody.Builder()
//                .setType(MultipartBody.FORM)
//                .addFormDataPart(
//                    "file",
//                    file.name,
//                    RequestBody.create("video/mp4".toMediaTypeOrNull(), file)
//                )
//                .build()
//
//            val request = Request.Builder()
//                .url(serverUrl)
//                .post(requestBody)
//                .build()
//
//            client.newCall(request).enqueue(object : Callback {
//                override fun onFailure(call: Call, e: IOException) {
//                    Log.e(server.TAG, "Upload failed: ${e.message}")
//                }
//
//                override fun onResponse(call: Call, response: Response) {
//                    Log.d(server.TAG, "Upload success: ${response.body?.string()}")
//                }
//            })
//        } catch (e: Exception) {
//            Log.e(server.TAG, "Exception uploading video: ${e.message}")
//        }
//    }
// }



import android.annotation.SuppressLint
import android.renderscript.Allocation
import android.renderscript.Element

import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
class YuvToRgbConverter(context: Context) {

    private val rs = RenderScript.create(context)
    private val script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))

    fun yuvToRgb(image: Image, output: Bitmap) {
        val yuvBytes = yuv420ToNv21(image)
        val inputAllocation = Allocation.createSized(rs, Element.U8(rs), yuvBytes.size)
        val outputAllocation = Allocation.createFromBitmap(rs, output)

        inputAllocation.copyFrom(yuvBytes)
        script.setInput(inputAllocation)
        script.forEach(outputAllocation)
        outputAllocation.copyTo(output)
    }

    private fun yuv420ToNv21(image: Image): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFrame: (ImageProxy) -> Unit,
    recorderHelper: VideoRecorderHelper
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        modifier = modifier,
        factory = { ctx ->

            val previewView = PreviewView(ctx)

            val cameraProviderFuture =
                ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({

                val cameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Image analysis
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(
                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                    )
                    .build()

                imageAnalysis.setAnalyzer(
                    Executors.newSingleThreadExecutor()
                ) { imageProxy ->
                    onFrame(imageProxy)
                }

                // 🔥 IMPORTANT: Build VideoCapture here
                val videoCapture =
                    recorderHelper.buildVideoCapture()

                val cameraSelector =
                    CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider.unbindAll()

                // 🔥 IMPORTANT: Bind videoCapture
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                    videoCapture
                )

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}