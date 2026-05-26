package com.astpredict.app.ui.results

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.astpredict.app.data.db.AppDatabase
import com.astpredict.app.data.db.AnalysisEntity
import com.astpredict.app.data.ml.*
import com.astpredict.app.ui.theme.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    analysisId: Long,
    navController: NavController,
    database: AppDatabase
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var analysis by remember { mutableStateOf<AnalysisEntity?>(null) }
    var detections by remember { mutableStateOf<List<Detection>>(emptyList()) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }

    // Load analysis data
    LaunchedEffect(analysisId) {
        val entity = database.analysisDao().getAnalysisById(analysisId)
        analysis = entity

        entity?.let { e ->
            // Deserialize detections
            val type = object : TypeToken<List<Detection>>() {}.type
            detections = try {
                Gson().fromJson(e.detectionsJson, type) ?: emptyList()
            } catch (ex: Exception) {
                emptyList()
            }

            // Load bitmap
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(e.imageUri)
                    val input = context.contentResolver.openInputStream(uri)
                    bitmap = android.graphics.BitmapFactory.decodeStream(input)
                    input?.close()
                } catch (ex: Exception) {
                    // Image may no longer be available
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Results", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Share functionality
                    }) {
                        Icon(Icons.Outlined.Share, contentDescription = "Share")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        if (analysis == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val a = analysis!!

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Annotated Image
            item {
                AnnotatedImageSection(
                    bitmap = bitmap,
                    detections = detections,
                    imageWidth = a.imageWidth,
                    imageHeight = a.imageHeight
                )
            }

            // Summary Cards
            item {
                SummarySection(analysis = a, inferenceTime = a.inferenceTimeMs)
            }

            // Tab selector
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Species", modifier = Modifier.padding(12.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Detections", modifier = Modifier.padding(12.dp))
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // Species breakdown
                    val grouped = detections.groupBy { it.classId }
                        .toList()
                        .sortedByDescending { it.second.size }

                    items(grouped) { (classId, dets) ->
                        SpeciesBreakdownCard(
                            classId = classId,
                            detections = dets,
                            totalDetections = detections.size
                        )
                    }
                }
                1 -> {
                    // Individual detections
                    val sortedDetections = detections.sortedByDescending { it.confidence }
                        .take(100) // Limit for performance

                    items(sortedDetections.size) { index ->
                        val det = sortedDetections[index]
                        DetectionCard(detection = det, index = index + 1)
                    }

                    if (detections.size > 100) {
                        item {
                            Text(
                                text = "Showing top 100 of ${detections.size} detections",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotatedImageSection(
    bitmap: Bitmap?,
    detections: List<Detection>,
    imageWidth: Int,
    imageHeight: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    if (imageWidth > 0 && imageHeight > 0)
                        imageWidth.toFloat() / imageHeight.toFloat()
                    else 1f
                )
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Analyzed image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )

                // Draw bounding boxes overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val scaleX = size.width / imageWidth
                    val scaleY = size.height / imageHeight

                    detections.forEach { det ->
                        val box = det.boundingBox.toPixelCoords(imageWidth, imageHeight)
                        val color = Color(det.displayColor)

                        drawRect(
                            color = color.copy(alpha = 0.3f),
                            topLeft = Offset(box.x1 * scaleX, box.y1 * scaleY),
                            size = Size(box.width * scaleX, box.height * scaleY)
                        )
                        drawRect(
                            color = color,
                            topLeft = Offset(box.x1 * scaleX, box.y1 * scaleY),
                            size = Size(box.width * scaleX, box.height * scaleY),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            } else {
                // No image available
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Image,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Image not available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummarySection(analysis: AnalysisEntity, inferenceTime: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.BubbleChart,
            label = "Colonies",
            value = "${analysis.totalDetections}",
            color = Info
        )
        SummaryChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Speed,
            label = "Confidence",
            value = "${"%.1f".format(analysis.averageConfidence * 100)}%",
            color = when {
                analysis.averageConfidence > 0.8f -> Success
                analysis.averageConfidence > 0.5f -> Warning
                else -> Error
            }
        )
        SummaryChip(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Timer,
            label = "Inference",
            value = "${inferenceTime}ms",
            color = Primary
        )
    }
}

@Composable
private fun SummaryChip(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SpeciesBreakdownCard(
    classId: Int,
    detections: List<Detection>,
    totalDetections: Int
) {
    val species = BacterialSpecies.fromClassId(classId)
    val percentage = if (totalDetections > 0) detections.size.toFloat() / totalDetections else 0f
    val avgConf = detections.map { it.confidence }.average().toFloat()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(BacterialSpecies.getColor(classId)))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = species?.scientificName ?: "Class $classId",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = FontStyle.Italic
                )

                if (species != null) {
                    Text(
                        text = species.significance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { percentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(BacterialSpecies.getColor(classId)),
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${detections.size}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${"%.0f".format(percentage * 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "avg ${"%.0f".format(avgConf * 100)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DetectionCard(detection: Detection, index: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index
            Text(
                text = "#$index",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(36.dp)
            )

            // Color dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(detection.displayColor))
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Species name
            Text(
                text = detection.species?.commonName ?: "Class ${detection.classId}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Confidence badge
            val confColor = when {
                detection.confidence > 0.8f -> ConfidenceHigh
                detection.confidence > 0.5f -> ConfidenceMedium
                else -> ConfidenceLow
            }

            Text(
                text = "${"%.1f".format(detection.confidence * 100)}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = confColor
            )
        }
    }
}
