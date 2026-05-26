package com.astpredict.app.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.astpredict.app.data.db.AppDatabase
import com.astpredict.app.data.db.AnalysisEntity
import com.astpredict.app.ui.navigation.Screen
import com.astpredict.app.ui.theme.*
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    database: AppDatabase
) {
    val recentAnalyses by database.analysisDao().getRecentAnalyses(5)
        .collectAsState(initial = emptyList())
    val totalCount by database.analysisDao().getTotalCount()
        .collectAsState(initial = 0)
    val totalDetections by database.analysisDao().getTotalDetections()
        .collectAsState(initial = 0)

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Hero Header with gradient
        HeroSection()

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Stats
        StatsRow(
            totalImages = totalCount,
            totalDetections = totalDetections ?: 0
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Action Buttons
        QuickActions(navController = navController)

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Analyses
        if (recentAnalyses.isNotEmpty()) {
            RecentAnalyses(
                analyses = recentAnalyses,
                navController = navController
            )
        } else {
            EmptyState()
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Species Info Card
        SpeciesInfoCard()

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun HeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GradientStart,
                        GradientMid,
                        GradientEnd
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    ) {
        // Animated particles/dots for scientific feel
        val infiniteTransition = rememberInfiniteTransition(label = "hero")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        // Decorative circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.1f),
                radius = 180.dp.toPx(),
                center = Offset(size.width * 0.85f, size.height * 0.2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.05f),
                radius = 120.dp.toPx(),
                center = Offset(size.width * 0.1f, size.height * 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Biotech,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ASTPredict",
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI-Powered Bacterial Colony Detection",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "24 Veterinary Pathogen Species • On-Device Inference",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StatsRow(totalImages: Int, totalDetections: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Image,
            label = "Images\nAnalyzed",
            value = "$totalImages",
            color = Info
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.BubbleChart,
            label = "Colonies\nDetected",
            value = "$totalDetections",
            color = Success
        )
        StatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Pets,
            label = "Species\nSupported",
            value = "24",
            color = Secondary
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun QuickActions(navController: NavController) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Capture button
            ActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.CameraAlt,
                label = "Capture\n& Analyze",
                gradient = listOf(GradientStart, GradientEnd),
                onClick = { navController.navigate(Screen.Camera.route) }
            )

            // Gallery button
            ActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.PhotoLibrary,
                label = "Import from\nGallery",
                gradient = listOf(GradientAccentStart, GradientAccentEnd),
                onClick = { navController.navigate(Screen.Camera.route) }
            )
        }
    }
}

@Composable
private fun ActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(gradient)
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun RecentAnalyses(
    analyses: List<AnalysisEntity>,
    navController: NavController
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Analyses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { navController.navigate(Screen.History.route) }) {
                Text("View All")
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(analyses) { analysis ->
            RecentAnalysisCard(
                analysis = analysis,
                onClick = {
                    navController.navigate(Screen.Results.createRoute(analysis.id))
                }
            )
        }
    }
}

@Composable
private fun RecentAnalysisCard(
    analysis: AnalysisEntity,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BubbleChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${analysis.totalDetections} colonies",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = dateFormat.format(Date(analysis.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Species info
            if (analysis.dominantSpecies != null) {
                Text(
                    text = analysis.dominantSpecies,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Confidence bar
            LinearProgressIndicator(
                progress = { analysis.averageConfidence },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = when {
                    analysis.averageConfidence > 0.8f -> ConfidenceHigh
                    analysis.averageConfidence > 0.5f -> ConfidenceMedium
                    else -> ConfidenceLow
                },
                trackColor = MaterialTheme.colorScheme.surface
            )

            Text(
                text = "Avg conf: ${"%.1f".format(analysis.averageConfidence * 100)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Science,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Analyses Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Capture or import a culture plate image to start detecting bacterial colonies",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun SpeciesInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Supported Species",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ASTPredict can detect and classify 24 veterinary-relevant " +
                        "bacterial species from agar culture plate images, including " +
                        "E. coli, S. aureus, Salmonella, Listeria, Pseudomonas, and more.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Powered by YOLOv8 • On-device AI • No internet required",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
            )
        }
    }
}
