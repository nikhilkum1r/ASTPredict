package com.astpredict.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_history")
data class AnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val timestamp: Long,
    val totalDetections: Int,
    val averageConfidence: Float,
    val dominantSpecies: String?,
    val inferenceTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int,
    val detectionsJson: String // Serialized list of detections
)
