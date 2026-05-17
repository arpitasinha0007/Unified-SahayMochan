package com.example.unifiedapp.ui.vision

import android.content.Context
import java.io.File


import android.util.Log

class AUCsvWriter(
    context: Context,
    anonymousId: String,
    keyAUs: List<String>
) {
    private val file = File(context.filesDir, "${anonymousId}-au.csv")
    private val writer = file.printWriter()
    private var frameNo = 0

    companion object {
        private const val TAG = "AUCsvWriter"
    }

    init {
        // Write header
        writer.println("frameNo,timestamp,${keyAUs.joinToString(",")}")
        writer.flush()
        Log.d(TAG, "✅ AU CSV created: ${file.absolutePath}")
        Log.d(TAG, "File exists: ${file.exists()}")
    }

    fun writeFrame(auValues: FloatArray, timestamp: Long) {
        frameNo++
        val row = "$frameNo,$timestamp,${auValues.joinToString(",")}"
        writer.println(row)

        // Flush every 30 frames
        if (frameNo % 30 == 0) {
            writer.flush()
            Log.d(TAG, "Flushed at frame $frameNo")
        }
    }

    fun flush() {
        writer.flush()
        Log.d(TAG, "✅ Flushed AU CSV, frames written: $frameNo")
    }

    fun close() {
        writer.flush()
        writer.close()
        Log.d(TAG, "✅ Closed AU CSV, total frames: $frameNo, file size: ${file.length()} bytes")
        Log.d(TAG, "File exists after close: ${file.exists()}")
    }

    fun getFile(): File = file
}