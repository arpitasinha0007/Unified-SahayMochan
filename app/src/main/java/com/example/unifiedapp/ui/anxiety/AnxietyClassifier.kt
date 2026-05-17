package com.example.unifiedapp.ui.anxiety



import android.util.Log
import kotlin.math.*

class AnxietyClassifier {

    private val anxietyPatterns = mapOf(
        "avoidance_eye_contact" to mapOf(
            "features" to listOf("left_eye", "right_eye"),
            "weight" to -0.3f
        ),
        "mouth_tension" to mapOf(
            "features" to listOf("mouth"),
            "weight" to 0.4f
        ),
        "facial_touch_frequency" to mapOf(
            "features" to listOf("jaw", "cheeks"),
            "weight" to 0.5f
        ),
        "eyebrow_raise" to mapOf(
            "features" to listOf("eyebrows"),
            "weight" to 0.3f
        ),
        "forehead_tension" to mapOf(
            "features" to listOf("forehead"),
            "weight" to 0.4f
        )
    )
    fun cleanup() {
        // Release any resources
        Log.d("AnxietyClassifier", "Cleanup complete")
    }
    private val baselineAttention = mapOf(
        "left_eye" to 0.25f,
        "right_eye" to 0.25f,
        "nose" to 0.15f,
        "mouth" to 0.15f,
        "jaw" to 0.08f,
        "forehead" to 0.07f,
        "eyebrows" to 0.03f,
        "cheeks" to 0.02f
    )

    fun classifyAnxiety(attentionMetrics: Map<String, Float>): Pair<AnxietyLevel, Float> {
        var anxietyScore = 0.0f
        var totalWeight = 0.0f

        attentionMetrics.forEach { (feature, value) ->
            val baseline = baselineAttention[feature] ?: 0.0f
            val deviation = abs(value - baseline)

            anxietyPatterns.values.forEach { pattern ->
                if (feature in pattern["features"] as List<String>) {
                    val weight = pattern["weight"] as Float
                    anxietyScore += deviation * weight
                    totalWeight += abs(weight)
                }
            }
        }

        val normalizedScore = if (totalWeight > 0) {
            (anxietyScore / totalWeight + 1.0f) / 2.0f
        } else {
            0.5f
        }

        val level = when {
            normalizedScore < 0.3f -> AnxietyLevel.MILD
            normalizedScore < 0.6f -> AnxietyLevel.MODERATE
            else -> AnxietyLevel.SEVERE
        }

        return Pair(level, normalizedScore)
    }

    fun calculateDetailedMetrics(attentionPoints: List<AttentionPoint>): Map<String, Float> {
        if (attentionPoints.isEmpty()) return emptyMap()

        val totalPoints = attentionPoints.size.toFloat()
        val metrics = mutableMapOf<String, Float>()

        attentionPoints.groupBy { it.feature }
            .forEach { (feature, points) ->
                metrics[feature] = points.size / totalPoints
            }

        metrics["temporal_variance"] = calculateTemporalVariance(attentionPoints)
        metrics["spatial_concentration"] = calculateSpatialConcentration(attentionPoints)

        return metrics
    }

    private fun calculateTemporalVariance(points: List<AttentionPoint>): Float {
        if (points.size < 10) return 0.5f

        val frameGroups = points.groupBy { it.frameIndex }
        val attentionPerFrame = frameGroups.map { (_, framePoints) ->
            framePoints.size.toFloat()
        }

        val mean = attentionPerFrame.average().toFloat()
        val variance = attentionPerFrame.map { (it - mean).pow(2) }.average().toFloat()

        return (variance / (mean + 0.1f)).coerceIn(0f, 1f)
    }

    private fun calculateSpatialConcentration(points: List<AttentionPoint>): Float {
        if (points.size < 10) return 0.5f

        val centerX = points.map { it.x }.average().toFloat()
        val centerY = points.map { it.y }.average().toFloat()

        val avgDistance = points.map { point ->
            sqrt((point.x - centerX).pow(2) + (point.y - centerY).pow(2))
        }.average().toFloat()

        return (1f - avgDistance).coerceIn(0f, 1f)
    }
}