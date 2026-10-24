package com.labix.navirom.ui.radial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.ServerConnectionUiState
import com.labix.navirom.ui.util.rememberNaviromHaptics
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Astrolabe Concentric Radial Settings & Circular Equalizer.
 */
@Composable
fun AstrolabeSettingsScreen(
    serverState: ServerConnectionUiState,
    totalCacheSizeBytes: Long,
    appLanguage: AppLanguage,
    onConnectServer: () -> Unit,
    onSyncLibrary: () -> Unit,
    onClearAllCache: () -> Unit,
    onToggleClassicView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberNaviromHaptics()
    val scope = rememberCoroutineScope()

    // 5-band Circular Astrolabe Equalizer State (-12dB to +12dB, normalized -1f..1f)
    var band60Hz by remember { mutableFloatStateOf(0.2f) }
    var band230Hz by remember { mutableFloatStateOf(0.4f) }
    var band910Hz by remember { mutableFloatStateOf(0.1f) }
    var band3600Hz by remember { mutableFloatStateOf(0.5f) }
    var band14000Hz by remember { mutableFloatStateOf(0.3f) }

    // Bitrate Rotary Dial (128k, 192k, 256k, 320k, Lossless)
    val bitrates = listOf("128k", "192k", "256k", "320k", "FLAC / RAW")
    var selectedBitrateIndex by remember { mutableIntStateOf(3) } // default 320k
    val bitrateStep = 360f / bitrates.size
    val bitrateDialState = rememberRadialDialState(
        initialAngle = selectedBitrateIndex * bitrateStep,
        stepSizeDegrees = bitrateStep
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AstrolabeTheme.DeepSpaceMidnight)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("astrolabe_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RADIAL SETTINGS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = AstrolabeTheme.ParchmentRim
                )
                Text(
                    text = "CONCENTRIC GAUGES & CELESTIAL EQ",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = AstrolabeTheme.StarCyan.copy(alpha = 0.8f)
                )
            }

            FilledTonalButton(
                onClick = {
                    haptics.click()
                    onToggleClassicView()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AstrolabeTheme.EngravedSlate,
                    contentColor = AstrolabeTheme.AstrolabeGold
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Classic", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Section 1: Server Status Concentric Dial & Latency Gauge
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AstrolabeTheme.EngravedSlate.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AstrolabeTheme.CelestialGrid),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (serverState.isConnected) Color(0xFF2D7A4F) else MaterialTheme.colorScheme.error)
                        )
                        Text(
                            text = if (serverState.isConnected) "Subsonic / Navidrome Online" else "Server Offline / Standby",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AstrolabeTheme.ParchmentRim
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            haptics.click()
                            onConnectServer()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AstrolabeTheme.AstrolabeGold,
                            contentColor = AstrolabeTheme.DeepSpaceMidnight
                        )
                    ) {
                        Text("Connect", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Text(
                    text = if (serverState.serverUrl.isNotBlank()) serverState.serverUrl else "http://your-server.local:4533",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = AstrolabeTheme.StarCyan.copy(alpha = 0.8f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sync Library button
                    OutlinedButton(
                        onClick = {
                            haptics.toggle()
                            onSyncLibrary()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AstrolabeTheme.CelestialGrid)
                    ) {
                        Icon(Icons.Filled.Sync, contentDescription = null, tint = AstrolabeTheme.StarCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sync Astrolabe", color = AstrolabeTheme.ParchmentRim, fontSize = 12.sp)
                    }

                    // Clear Cache button
                    OutlinedButton(
                        onClick = {
                            haptics.click()
                            onClearAllCache()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AstrolabeTheme.CelestialGrid)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        val cacheMb = totalCacheSizeBytes / (1024 * 1024)
                        Text("${cacheMb}MB Clear", color = AstrolabeTheme.ParchmentRim, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 2: Circular Astrolabe Equalizer (EQ Rose)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AstrolabeTheme.EngravedSlate.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AstrolabeTheme.CelestialGrid),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CELESTIAL EQUALIZER ROSE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = AstrolabeTheme.AstrolabeGold
                )
                Text(
                    text = "5-BAND RADIAL ACOUSTIC SCULPTING",
                    style = MaterialTheme.typography.labelSmall,
                    color = AstrolabeTheme.StarCyan.copy(alpha = 0.8f)
                )

                // Canvas rendering the 5-point radial polygon
                val bands = listOf(band60Hz, band230Hz, band910Hz, band3600Hz, band14000Hz)
                val labels = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")

                Canvas(
                    modifier = Modifier
                        .size(240.dp)
                        .padding(12.dp)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.width * 0.45f

                    // Concentric dB guide circles
                    listOf(0.33f, 0.66f, 1.0f).forEach { fraction ->
                        drawCircle(
                            color = AstrolabeTheme.CelestialGrid.copy(alpha = 0.3f),
                            radius = maxR * fraction,
                            center = center,
                            style = Stroke(width = 1f)
                        )
                    }

                    // 5 radial spokes
                    val angleStep = 360f / 5f
                    val polygonPath = Path()

                    bands.forEachIndexed { i, value ->
                        val angleDeg = -90f + (i * angleStep)
                        // value is -1..1, mapped to radius 0.2..1.0
                        val normR = ((value + 1f) / 2f).coerceIn(0.15f, 1.0f) * maxR
                        val spokeEnd = RadialMath.toCartesian(center, maxR, angleDeg)
                        val point = RadialMath.toCartesian(center, normR, angleDeg)

                        // Draw spoke line
                        drawLine(
                            color = AstrolabeTheme.EngravedSlate,
                            start = center,
                            end = spokeEnd,
                            strokeWidth = 1.5f
                        )

                        // Add point to polygon
                        if (i == 0) polygonPath.moveTo(point.x, point.y) else polygonPath.lineTo(point.x, point.y)

                        // Draw node point
                        drawCircle(
                            color = AstrolabeTheme.AstrolabeGold,
                            radius = 6f,
                            center = point
                        )
                    }
                    polygonPath.close()

                    // Draw the filled and stroked equalizer polygon
                    drawPath(
                        path = polygonPath,
                        color = AstrolabeTheme.StarCyan.copy(alpha = 0.25f)
                    )
                    drawPath(
                        path = polygonPath,
                        color = AstrolabeTheme.StarCyan,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }

                // Interactive sliders row for the 5 bands
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    BandSlider(label = "60Hz", value = band60Hz, onValueChange = { band60Hz = it })
                    BandSlider(label = "230Hz", value = band230Hz, onValueChange = { band230Hz = it })
                    BandSlider(label = "910Hz", value = band910Hz, onValueChange = { band910Hz = it })
                    BandSlider(label = "3.6k", value = band3600Hz, onValueChange = { band3600Hz = it })
                    BandSlider(label = "14k", value = band14000Hz, onValueChange = { band14000Hz = it })
                }
            }
        }

        // Section 3: Bitrate Rotary Transcoding Dial
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = AstrolabeTheme.EngravedSlate.copy(alpha = 0.5f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AstrolabeTheme.CelestialGrid),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ROTARY BITRATE SELECTOR",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = AstrolabeTheme.AstrolabeGold
                )
                Text(
                    text = "Current: ${bitrates[selectedBitrateIndex]}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = AstrolabeTheme.StarCyan
                )

                // Bitrate options chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bitrates.forEachIndexed { index, rate ->
                        val isSelected = selectedBitrateIndex == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) AstrolabeTheme.AstrolabeGold else AstrolabeTheme.DeepSpaceMidnight,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptics.click()
                                    selectedBitrateIndex = index
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = rate,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) AstrolabeTheme.DeepSpaceMidnight else AstrolabeTheme.ParchmentRim
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BandSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "${if (value > 0f) "+" else ""}${(value * 12).toInt()}dB",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 9.sp),
            color = AstrolabeTheme.StarCyan
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -1f..1f,
            modifier = Modifier
                .height(110.dp)
                .width(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = AstrolabeTheme.AstrolabeGold,
                activeTrackColor = AstrolabeTheme.StarCyan,
                inactiveTrackColor = AstrolabeTheme.EngravedSlate
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            color = AstrolabeTheme.ParchmentRim
        )
    }
}
