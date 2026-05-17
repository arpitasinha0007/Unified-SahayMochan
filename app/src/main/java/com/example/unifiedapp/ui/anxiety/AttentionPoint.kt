package com.example.unifiedapp.ui.anxiety

// models/AttentionPoint.kt

data class AttentionPoint(
    val x: Float,           // Normalized x coordinate (0-1)
    val y: Float,           // Normalized y coordinate (0-1)
    val weight: Float,      // Attention weight for this point
    val feature: String,    // Facial feature name
    val frameIndex: Long,   // Frame number in video
    val timestamp: Long     // Timestamp in milliseconds
)