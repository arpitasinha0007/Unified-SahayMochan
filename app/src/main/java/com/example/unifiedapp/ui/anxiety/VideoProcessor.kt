package com.example.unifiedapp.ui.anxiety


import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object VideoProcessor {

    suspend fun processVideo(
        context: Context,
        inputPath: String
    ): String = withContext(Dispatchers.IO) {

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(inputPath)

        val duration =
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L

        val outputPath =
            "${context.getExternalFilesDir(null)}/processed_${System.currentTimeMillis()}.mp4"

        val frames = mutableListOf<Bitmap>()

        var timeUs = 0L

        while (timeUs < duration * 1000) {

            retriever.getFrameAtTime(
                timeUs,
                MediaMetadataRetriever.OPTION_CLOSEST
            )?.let { frame ->

                val processed = applyHeatmap(frame)
                frames.add(processed)
            }

            timeUs += 200_000 // every 0.2 sec
        }

        retriever.release()

        if (frames.isEmpty()) {
            throw Exception("No frames extracted")
        }

        val encoder = VideoEncoder(
            outputPath = outputPath,
            width = frames[0].width,
            height = frames[0].height
        )

        encoder.start()

        frames.forEachIndexed { index, bitmap ->
            encoder.encodeFrame(bitmap, index)
        }

        encoder.stop()

        return@withContext outputPath
    }

    private fun applyHeatmap(bitmap: Bitmap): Bitmap {

        val result =
            bitmap.copy(Bitmap.Config.ARGB_8888, true)

        val canvas = Canvas(result)
        val paint = Paint()

        paint.shader = RadialGradient(
            bitmap.width / 2f,
            bitmap.height / 2f,
            bitmap.width / 2f,
            intArrayOf(
                Color.argb(150, 255, 0, 0),
                Color.TRANSPARENT
            ),
            null,
            Shader.TileMode.CLAMP
        )

        canvas.drawRect(
            0f,
            0f,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            paint
        )

        return result
    }
}