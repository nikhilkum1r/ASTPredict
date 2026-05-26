package com.astpredict.app.ui.camera

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.astpredict.app.data.db.AnalysisEntity
import com.astpredict.app.data.db.AppDatabase
import com.astpredict.app.data.ml.AnalysisResult
import com.astpredict.app.data.ml.ColonyDetector
import com.astpredict.app.ui.navigation.Screen
import com.astpredict.app.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    navController: NavController,
    detector: ColonyDetector,
    database: AppDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isAnalyzing by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCamera by remember { mutableStateOf(false) }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            selectedBitmap = detector.loadBitmapFromUri(it)
            showCamera = false
        }
    }

    // Camera capture helpers
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    fun analyzeImage(bitmap: Bitmap, uri: String) {
        scope.launch {
            isAnalyzing = true
            try {
                val result = withContext(Dispatchers.Default) {
                    detector.detect(bitmap, uri)
                }

                // Save to database
                val entity = AnalysisEntity(
                    imageUri = uri,
                    timestamp = result.timestamp,
                    totalDetections = result.totalDetections,
                    averageConfidence = result.averageConfidence,
                    dominantSpecies = result.dominantSpecies,
                    inferenceTimeMs = result.inferenceTimeMs,
                    imageWidth = result.imageWidth,
                    imageHeight = result.imageHeight,
                    detectionsJson = Gson().toJson(result.detections)
                )
                val id = database.analysisDao().insert(entity)

                isAnalyzing = false
                navController.navigate(Screen.Results.createRoute(id))
            } catch (e: Exception) {
                isAnalyzing = false
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Analysis failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Colony Analysis",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    if (selectedBitmap != null || showCamera) {
                        IconButton(onClick = {
                            if (selectedBitmap != null) {
                                selectedBitmap = null
                                selectedImageUri = null
                            } else {
                                showCamera = false
                            }
                        }) {
                            Icon(Icons.Filled.ArrowBack, "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Analyzing state
                isAnalyzing -> {
                    AnalyzingOverlay()
                }

                // Image selected — show preview with analyze button
                selectedBitmap != null -> {
                    ImagePreviewScreen(
                        bitmap = selectedBitmap!!,
                        onAnalyze = {
                            analyzeImage(selectedBitmap!!, selectedImageUri?.toString() ?: "capture")
                        },
                        onRetake = {
                            selectedBitmap = null
                            selectedImageUri = null
                        }
                    )
                }

                // Camera preview
                showCamera -> {
                    if (cameraPermission.status.isGranted) {
                        CameraPreviewContent(
                            onImageCaptured = { bitmap, uri ->
                                selectedBitmap = bitmap
                                selectedImageUri = uri
                                showCamera = false
                            },
                            onGalleryClick = {
                                imagePickerLauncher.launch("image/*")
                            }
                        )
                    } else {
                        PermissionRequest(
                            onRequestPermission = { cameraPermission.launchPermissionRequest() }
                        )
                    }
                }

                // Selection screen
                else -> {
                    SourceSelectionScreen(
                        onCameraClick = {
                            showCamera = true
                            if (!cameraPermission.status.isGranted) {
                                cameraPermission.launchPermissionRequest()
                            }
                        },
                        onGalleryClick = {
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceSelectionScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Biotech,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Analyze Culture Plate",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Capture a photo of a Petri dish or import from your gallery",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Camera button
        Button(
            onClick = onCameraClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Open Camera", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gallery button
        OutlinedButton(
            onClick = onGalleryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Import from Gallery", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Tips
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📋 Tips for best results",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                TipItem("Ensure even lighting across the plate")
                TipItem("Avoid shadows and reflections on agar")
                TipItem("Center the Petri dish in the frame")
                TipItem("Use highest camera resolution available")
            }
        }
    }
}

@Composable
private fun TipItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Success,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CameraPreviewContent(
    onImageCaptured: (Bitmap, Uri?) -> Unit,
    onGalleryClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .build()
                    imageCapture = capture

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Circular guide overlay for Petri dish alignment
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = size.minDimension * 0.35f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Gallery
            IconButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Filled.PhotoLibrary,
                    contentDescription = "Gallery",
                    tint = Color.White
                )
            }

            // Capture button
            IconButton(
                onClick = {
                    val ic = imageCapture ?: return@IconButton
                    val photoFile = File(
                        context.cacheDir,
                        "capture_${System.currentTimeMillis()}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    ic.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                                val uri = Uri.fromFile(photoFile)
                                onImageCaptured(bitmap, uri)
                            }

                            override fun onError(e: ImageCaptureException) {
                                e.printStackTrace()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                Icon(
                    Icons.Filled.CameraAlt,
                    contentDescription = "Capture",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Placeholder for symmetry
            Box(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun ImagePreviewScreen(
    bitmap: Bitmap,
    onAnalyze: () -> Unit,
    onRetake: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Image preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val imageBitmap = remember(bitmap) {
                bitmap.asImageBitmap()
            }

            Image(
                bitmap = imageBitmap,
                contentDescription = "Selected image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
            )

            // Image info badge
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "${bitmap.width} × ${bitmap.height}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Choose Another")
            }

            Button(
                onClick = onAnalyze,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.Science, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Analyze", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AnalyzingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Analyzing Colonies...",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Running YOLOv8 inference on-device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PermissionRequest(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.CameraAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ASTPredict needs camera access to capture culture plate images for analysis",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Permission")
        }
    }
}
