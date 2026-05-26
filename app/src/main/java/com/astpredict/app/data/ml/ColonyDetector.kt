package com.astpredict.app.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

/**
 * Colony Detector — TFLite inference engine for YOLOv8.
 *
 * Handles:
 * - Model loading from assets (memory-mapped for efficiency)
 * - Image pre-processing (letterbox resize, normalization)
 * - YOLOv8 output tensor parsing
 * - Non-Maximum Suppression (NMS)
 * - GPU delegate acceleration (optional)
 */
class ColonyDetector(private val context: Context) {

    companion object {
        private const val TAG = "ColonyDetector"
        private const val MODEL_FILE = "best_float32.tflite"
        private const val INPUT_SIZE = 640
        private const val NUM_CLASSES = 24
        private const val NUM_PREDICTIONS = 8400 // YOLOv8 default for 640x640
        private const val BBOX_PARAMS = 4 // x_center, y_center, width, height
        private const val OUTPUT_FEATURES = BBOX_PARAMS + NUM_CLASSES // 28
        private const val DEFAULT_CONF_THRESHOLD = 0.25f
        private const val DEFAULT_IOU_THRESHOLD = 0.45f
        private const val DEFAULT_MAX_DETECTIONS = 3000
    }

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null
    private var isInitialized = false

    // Configurable thresholds
    var confidenceThreshold: Float = DEFAULT_CONF_THRESHOLD
    var iouThreshold: Float = DEFAULT_IOU_THRESHOLD
    var maxDetections: Int = DEFAULT_MAX_DETECTIONS

    /**
     * Initialize the TFLite interpreter.
     * Call this once at app startup.
     */
    fun initialize(useGpu: Boolean = false) {
        if (isInitialized) return

        try {
            val options = Interpreter.Options().apply {
                setNumThreads(4)

                if (useGpu) {
                    try {
                        gpuDelegate = GpuDelegate()
                        addDelegate(gpuDelegate)
                        Log.i(TAG, "GPU delegate enabled")
                    } catch (e: Exception) {
                        Log.w(TAG, "GPU delegate failed, falling back to CPU: ${e.message}")
                    }
                }
            }

            val modelBuffer = loadModelFile()
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true

            // Log model input/output shapes for debugging
            val inputShape = interpreter!!.getInputTensor(0).shape()
            val outputShape = interpreter!!.getOutputTensor(0).shape()
            Log.i(TAG, "Model loaded. Input: ${inputShape.contentToString()}, Output: ${outputShape.contentToString()}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize model: ${e.message}", e)
            throw RuntimeException("Failed to load ASTPredict model", e)
        }
    }

    /**
     * Run detection on a Bitmap image.
     * Returns AnalysisResult with all detections.
     */
    fun detect(bitmap: Bitmap, imageUri: String = ""): AnalysisResult {
        check(isInitialized) { "ColonyDetector not initialized. Please ensure 'best_float32.tflite' is present in the app's assets directory." }

        val startTime = System.currentTimeMillis()

        // 1. Pre-process: letterbox resize to INPUT_SIZE x INPUT_SIZE
        val (processedBitmap, scaleInfo) = letterboxResize(bitmap)

        // 2. Convert to input tensor (float buffer, normalized 0-1, RGB)
        val inputBuffer = bitmapToByteBuffer(processedBitmap)

        // 3. Run inference
        val outputBuffer = Array(1) { Array(OUTPUT_FEATURES) { FloatArray(NUM_PREDICTIONS) } }
        interpreter!!.run(inputBuffer, outputBuffer)

        // 4. Parse output and apply NMS
        val rawDetections = parseOutput(outputBuffer[0], scaleInfo, bitmap.width, bitmap.height)
        val nmsDetections = applyNMS(rawDetections)

        val inferenceTime = System.currentTimeMillis() - startTime
        Log.i(TAG, "Inference complete: ${nmsDetections.size} detections in ${inferenceTime}ms")

        return AnalysisResult(
            imageUri = imageUri,
            detections = nmsDetections,
            inferenceTimeMs = inferenceTime,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
    }

    /**
     * Run detection on an image URI.
     */
    fun detectFromUri(uri: Uri): AnalysisResult {
        val bitmap = loadBitmapFromUri(uri)
            ?: throw IllegalArgumentException("Failed to load image from URI: $uri")
        return detect(bitmap, uri.toString())
    }

    /**
     * Load and decode a bitmap from URI with memory-efficient downsampling.
     */
    fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Calculate sample size to prevent OOM on very large images
            val maxDim = max(options.outWidth, options.outHeight)
            var sampleSize = 1
            while (maxDim / sampleSize > 4096) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val stream2 = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(stream2, null, decodeOptions)
            stream2.close()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error loading bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Letterbox resize: Scale image to fit INPUT_SIZE while maintaining aspect ratio,
     * padding with gray (114/255) to fill the square.
     */
    private fun letterboxResize(bitmap: Bitmap): Pair<Bitmap, ScaleInfo> {
        val srcW = bitmap.width.toFloat()
        val srcH = bitmap.height.toFloat()

        val scale = min(INPUT_SIZE / srcW, INPUT_SIZE / srcH)
        val newW = (srcW * scale).toInt()
        val newH = (srcH * scale).toInt()

        val padX = (INPUT_SIZE - newW) / 2f
        val padY = (INPUT_SIZE - newH) / 2f

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true)

        val paddedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(paddedBitmap)
        canvas.drawColor(Color.rgb(114, 114, 114)) // YOLOv8 standard padding color
        canvas.drawBitmap(resizedBitmap, padX, padY, null)

        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }

        return Pair(paddedBitmap, ScaleInfo(scale, padX, padY, srcW.toInt(), srcH.toInt()))
    }

    /**
     * Convert a bitmap to a ByteBuffer suitable for TFLite input.
     * Format: NHWC, float32, normalized to [0, 1].
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            // Extract RGB and normalize to [0, 1]
            byteBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
            byteBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
            byteBuffer.putFloat((pixel and 0xFF) / 255.0f)          // B
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    /**
     * Parse YOLOv8 raw output tensor into Detection objects.
     *
     * YOLOv8 output shape: [1, 28, 8400]
     * - 28 = 4 (bbox: x_center, y_center, w, h) + 24 (class scores)
     * - 8400 = number of prediction anchors
     *
     * We need to transpose to [8400, 28] for processing.
     */
    private fun parseOutput(
        output: Array<FloatArray>,
        scaleInfo: ScaleInfo,
        origWidth: Int,
        origHeight: Int
    ): List<Detection> {
        val detections = mutableListOf<Detection>()

        for (i in 0 until NUM_PREDICTIONS) {
            // Extract bbox (normalized to input size)
            val xc = output[0][i] / INPUT_SIZE
            val yc = output[1][i] / INPUT_SIZE
            val w = output[2][i] / INPUT_SIZE
            val h = output[3][i] / INPUT_SIZE

            // Find best class
            var maxScore = 0f
            var maxClassId = 0
            for (c in 0 until NUM_CLASSES) {
                val score = output[BBOX_PARAMS + c][i]
                if (score > maxScore) {
                    maxScore = score
                    maxClassId = c
                }
            }

            // Filter by confidence
            if (maxScore < confidenceThreshold) continue

            // Un-letterbox: convert from padded coords back to original image coords
            val unpadX = (xc * INPUT_SIZE - scaleInfo.padX) / (scaleInfo.scale * scaleInfo.origW)
            val unpadY = (yc * INPUT_SIZE - scaleInfo.padY) / (scaleInfo.scale * scaleInfo.origH)
            val unpadW = w * INPUT_SIZE / (scaleInfo.scale * scaleInfo.origW)
            val unpadH = h * INPUT_SIZE / (scaleInfo.scale * scaleInfo.origH)

            val detection = Detection(
                classId = maxClassId,
                className = BacterialSpecies.getSpeciesName(maxClassId),
                confidence = maxScore,
                boundingBox = BoundingBox(
                    xCenter = unpadX.coerceIn(0f, 1f),
                    yCenter = unpadY.coerceIn(0f, 1f),
                    width = unpadW.coerceIn(0f, 1f),
                    height = unpadH.coerceIn(0f, 1f)
                )
            )
            detections.add(detection)
        }

        return detections
    }

    /**
     * Non-Maximum Suppression — class-agnostic for speed with dense colonies.
     */
    private fun applyNMS(detections: List<Detection>): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        // Sort by confidence descending
        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<Detection>()

        while (sorted.isNotEmpty() && selected.size < maxDetections) {
            val best = sorted.removeAt(0)
            selected.add(best)

            sorted.removeAll { other ->
                computeIoU(best.boundingBox, other.boundingBox) > iouThreshold
            }
        }

        return selected
    }

    /**
     * Compute Intersection over Union between two bounding boxes.
     */
    private fun computeIoU(a: BoundingBox, b: BoundingBox): Float {
        val aX1 = a.xCenter - a.width / 2
        val aY1 = a.yCenter - a.height / 2
        val aX2 = a.xCenter + a.width / 2
        val aY2 = a.yCenter + a.height / 2

        val bX1 = b.xCenter - b.width / 2
        val bY1 = b.yCenter - b.height / 2
        val bX2 = b.xCenter + b.width / 2
        val bY2 = b.yCenter + b.height / 2

        val interX1 = max(aX1, bX1)
        val interY1 = max(aY1, bY1)
        val interX2 = min(aX2, bX2)
        val interY2 = min(aY2, bY2)

        val interArea = max(0f, interX2 - interX1) * max(0f, interY2 - interY1)
        val aArea = a.width * a.height
        val bArea = b.width * b.height
        val unionArea = aArea + bArea - interArea

        return if (unionArea > 0) interArea / unionArea else 0f
    }

    /**
     * Load model from assets as a memory-mapped file for efficiency.
     */
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /**
     * Release resources.
     */
    fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
        isInitialized = false
    }

    /**
     * Scale and padding info for un-letterboxing.
     */
    private data class ScaleInfo(
        val scale: Float,
        val padX: Float,
        val padY: Float,
        val origW: Int,
        val origH: Int
    )
}
