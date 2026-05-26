package com.astpredict.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.astpredict.app.data.ml.ColonyDetector
import com.astpredict.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    detector: ColonyDetector
) {
    var confidenceThreshold by remember { mutableFloatStateOf(detector.confidenceThreshold) }
    var iouThreshold by remember { mutableFloatStateOf(detector.iouThreshold) }
    var maxDetections by remember { mutableIntStateOf(detector.maxDetections) }
    var showSpeciesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Detection Settings
            SettingsSection(title = "Detection Parameters") {
                // Confidence Threshold
                SettingsSlider(
                    icon = Icons.Outlined.Speed,
                    title = "Confidence Threshold",
                    subtitle = "Minimum confidence to accept a detection",
                    value = confidenceThreshold,
                    valueRange = 0.1f..0.95f,
                    valueLabel = "${"%.0f".format(confidenceThreshold * 100)}%",
                    onValueChange = {
                        confidenceThreshold = it
                        detector.confidenceThreshold = it
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // IoU Threshold
                SettingsSlider(
                    icon = Icons.Outlined.FilterCenterFocus,
                    title = "IoU Threshold (NMS)",
                    subtitle = "Overlap threshold for non-maximum suppression",
                    value = iouThreshold,
                    valueRange = 0.1f..0.9f,
                    valueLabel = "${"%.0f".format(iouThreshold * 100)}%",
                    onValueChange = {
                        iouThreshold = it
                        detector.iouThreshold = it
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Max Detections
                SettingsSlider(
                    icon = Icons.Outlined.Numbers,
                    title = "Max Detections",
                    subtitle = "Maximum colonies to detect per image",
                    value = maxDetections.toFloat(),
                    valueRange = 100f..5000f,
                    valueLabel = "$maxDetections",
                    onValueChange = {
                        maxDetections = it.toInt()
                        detector.maxDetections = it.toInt()
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Species Reference
            SettingsSection(title = "Reference") {
                SettingsItem(
                    icon = Icons.Outlined.Biotech,
                    title = "Species Database",
                    subtitle = "View all 24 detectable bacterial species",
                    onClick = { showSpeciesDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // About section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "ASTPredict",
                    subtitle = "v1.0.0 • AI-Powered Bacterial Colony Detection"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                SettingsItem(
                    icon = Icons.Outlined.Memory,
                    title = "Model",
                    subtitle = "YOLOv8 • 24 classes • TFLite on-device"
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                SettingsItem(
                    icon = Icons.Outlined.Science,
                    title = "Research Application",
                    subtitle = "Veterinary Microbiology • AMR Diagnostics"
                )
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // Species dialog
        if (showSpeciesDialog) {
            SpeciesReferenceDialog(onDismiss = { showSpeciesDialog = false })
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSlider(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    valueLabel: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(top = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (onClick != null) {
            IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpeciesReferenceDialog(onDismiss: () -> Unit) {
    val species = com.astpredict.app.data.ml.BacterialSpecies.entries

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Detectable Species (24)",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                species.forEach { sp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${sp.classId}.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp)
                        )
                        Column {
                            Text(
                                text = sp.scientificName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = sp.significance,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
