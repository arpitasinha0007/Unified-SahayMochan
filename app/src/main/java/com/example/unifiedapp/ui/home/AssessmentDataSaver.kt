package com.example.unifiedapp.ui.home

import android.os.Bundle
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import com.example.unifiedapp.ui.views.AssessmentData
import java.io.File

// Fixed Saver implementation for MutableState
val AssessmentDataSaver = Saver<MutableState<AssessmentData?>, Bundle>(
    save = { state ->
        val assessmentData = state.value
        if (assessmentData == null) {
            null
        } else {
            Bundle().apply {
                putString("anonymousId", assessmentData.anonymousId)
                putString("videoPath", assessmentData.videoFile?.absolutePath)
                putString("auDataPath", assessmentData.auCsvFile?.absolutePath)
                putString("gad7DataPath", assessmentData.gad7CsvFile?.absolutePath)
                putInt("age", assessmentData.age)
                putString("email", assessmentData.email)
                putString("registrationId", assessmentData.registrationId)
            }
        }
    },
    restore = { bundle ->
        val anonymousId = bundle.getString("anonymousId") ?: return@Saver mutableStateOf(null)
        val videoPath = bundle.getString("videoPath")
        val auDataPath = bundle.getString("auDataPath")
        val gad7DataPath = bundle.getString("gad7DataPath")
        val age = bundle.getInt("age", 0)
        val email = bundle.getString("email") ?: ""
        val registrationId = bundle.getString("registrationId") ?: ""

        mutableStateOf(
            AssessmentData(
                anonymousId = anonymousId,
                videoFile = videoPath?.let { File(it) },
                auCsvFile = auDataPath?.let { File(it) },
                gad7CsvFile = gad7DataPath?.let { File(it) },
                age = age,
                email = email,
                registrationId = registrationId
            )
        )
    }
)