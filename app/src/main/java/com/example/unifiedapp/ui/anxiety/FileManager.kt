package com.example.unifiedapp.ui.anxiety


import android.content.Context
import java.util.*

import android.graphics.Bitmap
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FileManager(private val context: Context) {

    fun createAnxietyFolders(): Map<AnxietyLevel, File> {
        val baseDir = File(context.getExternalFilesDir(null), "anxiety_assessments")
        if (!baseDir.exists()) baseDir.mkdirs()

        return AnxietyLevel.values().associateWith { level ->
            val folder = File(baseDir, level.name.lowercase())
            if (!folder.exists()) folder.mkdirs()
            folder
        }
    }

    fun saveHeatmapToAppStorage(
        bitmap: Bitmap,
        level: AnxietyLevel,
        confidence: Float,
        videoFileName: String
    ): File {
        val folders = createAnxietyFolders()
        val targetFolder = folders[level] ?: folders[AnxietyLevel.MILD]!!

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedName = videoFileName.replace(Regex("[^a-zA-Z0-9.]"), "_")
        val fileName = "${sanitizedName}_${level.name}_${timestamp}.png"

        val file = File(targetFolder, fileName)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, file.outputStream())

        return file
    }

    fun saveMetadata(heatmapFile: File, metadata: Map<String, Any>) {
        val metadataFile = File(heatmapFile.parent, heatmapFile.nameWithoutExtension + ".json")
        val json = org.json.JSONObject(metadata).toString()
        metadataFile.writeText(json)
    }

    fun getSavedAssessments(): Map<AnxietyLevel, List<File>> {
        val baseDir = File(context.getExternalFilesDir(null), "anxiety_assessments")
        if (!baseDir.exists()) return emptyMap()

        return AnxietyLevel.values().associateWith { level ->
            val folder = File(baseDir, level.name.lowercase())
            if (folder.exists()) {
                folder.listFiles { file -> file.extension == "png" }?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        }
    }
}