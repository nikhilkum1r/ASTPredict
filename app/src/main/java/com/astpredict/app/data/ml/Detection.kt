package com.astpredict.app.data.ml

/**
 * Represents a single colony detection result from the YOLOv8 model.
 */
data class Detection(
    val classId: Int,
    val className: String,
    val confidence: Float,
    val boundingBox: BoundingBox
) {
    val species: BacterialSpecies?
        get() = BacterialSpecies.fromClassId(classId)

    val displayColor: Long
        get() = BacterialSpecies.getColor(classId)
}

/**
 * Bounding box coordinates in both normalized and pixel formats.
 */
data class BoundingBox(
    val xCenter: Float,
    val yCenter: Float,
    val width: Float,
    val height: Float
) {
    /** Convert to pixel coordinates for a given image size */
    fun toPixelCoords(imageWidth: Int, imageHeight: Int): PixelBox {
        val x1 = ((xCenter - width / 2) * imageWidth).coerceIn(0f, imageWidth.toFloat())
        val y1 = ((yCenter - height / 2) * imageHeight).coerceIn(0f, imageHeight.toFloat())
        val x2 = ((xCenter + width / 2) * imageWidth).coerceIn(0f, imageWidth.toFloat())
        val y2 = ((yCenter + height / 2) * imageHeight).coerceIn(0f, imageHeight.toFloat())
        return PixelBox(x1, y1, x2, y2)
    }
}

/**
 * Pixel-space bounding box (x1, y1, x2, y2).
 */
data class PixelBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
) {
    val width get() = x2 - x1
    val height get() = y2 - y1
    val centerX get() = (x1 + x2) / 2
    val centerY get() = (y1 + y2) / 2
    val area get() = width * height
}

/**
 * Complete analysis result for a single image.
 */
data class AnalysisResult(
    val imageUri: String,
    val detections: List<Detection>,
    val inferenceTimeMs: Long,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    val totalDetections: Int get() = detections.size

    val averageConfidence: Float
        get() = if (detections.isEmpty()) 0f
                else detections.map { it.confidence }.average().toFloat()

    val speciesBreakdown: Map<String, List<Detection>>
        get() = detections.groupBy { it.className }

    val speciesCounts: Map<String, Int>
        get() = speciesBreakdown.mapValues { it.value.size }

    val dominantSpecies: String?
        get() = speciesCounts.maxByOrNull { it.value }?.key
}
