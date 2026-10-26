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
 * Redesigned in editorial monochrome style matching zoom-kal.jpg:
 * Pure white background, solid black borders, serif typography, shelf ticks,
 * and 4.5dp solid black vertical accent bars.
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
    userName: String = "",
    onSetUserName: (String) -> Unit = {},
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

    // Bitrate Selection
    val bitrates = listOf("128k", "192k", "256k", "320k", "FLAC / RAW")
    var selectedBitrateIndex by remember { mutableIntStateOf(3) } // default 320k

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("astrolabe_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color.Black
                )
                Text(
                    text = "CONCENTRIC GAUGES & CELESTIAL EQ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFF666666)
                )
            }


        }

        // Listener Profile & Personal Identity Section
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.5.dp)
                                .height(28.dp)
                                .background(Color.Black)
                        )
                        Column {
                            Text(
                                text = "LISTENER IDENTITY",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    fontSize = 15.sp
                                ),
                                color = Color.Black
                            )
                            Text(
                                text = "CUSTOM NAME & ARCHIVE GREETING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFDDDDDD))
                )

                var localNameInput by remember(userName) { mutableStateOf(userName) }

                OutlinedTextField(
                    value = localNameInput,
                    onValueChange = {
                        localNameInput = it
                        onSetUserName(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("listener_name_input"),
                    label = {
                        Text(
                            text = "Listener Name / Alias",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif)
                        )
                    },
                    placeholder = {
                        Text(
                            text = "e.g. Johannes Kepler or your name",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Serif,
                                color = Color.Gray
                            )
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.5f),
                        cursorColor = Color.Black,
                        focusedLabelColor = Color.Black,
                        unfocusedLabelColor = Color(0xFF666666)
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "GREETING IN LIST-VIEW: WELCOME, ${localNameInput.ifBlank { "LISTENER" }.uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontSize = 10.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }

        // Section 1: Server Status Concentric Block with Editorial Styling
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SERVER INFRASTRUCTURE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            ),
                            color = Color.Black
                        )
                        Text(
                            text = if (serverState.isConnected) "Subsonic Online • Ready" else "Server Offline • Standby",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.Black
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (serverState.isConnected) Color.Black else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                        modifier = Modifier.clickable {
                            haptics.click()
                            onConnectServer()
                        }
                    ) {
                        Text(
                            text = if (serverState.isConnected) "CONNECTED" else "CONNECT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = if (serverState.isConnected) Color.White else Color.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))

                // Server URL row with 4.5dp black accent bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(4.5.dp).height(24.dp).background(Color.Black))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (serverState.serverUrl.isNotBlank()) serverState.serverUrl else "http://your-server.local:4533",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = Color(0xFF444444)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sync Library button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptics.toggle()
                                onSyncLibrary()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Sync Library",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Black
                            )
                        }
                    }

                    // Clear Cache button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                haptics.click()
                                onClearAllCache()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            val cacheMb = totalCacheSizeBytes / (1024 * 1024)
                            Text(
                                "${cacheMb}MB Clear",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Circular Astrolabe Equalizer (monochrome geometric radar)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ACOUSTIC SPECTRUM ROSE",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color.Black
                )
                Text(
                    text = "5-BAND RADIAL HARMONIC MATRIX",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFF666666)
                )

                // Canvas rendering the 5-point radial polygon in black lines
                val bands = listOf(band60Hz, band230Hz, band910Hz, band3600Hz, band14000Hz)
                val labels = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")

                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(8.dp)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.width * 0.44f

                    // Concentric dB guide circles in clean black lines
                    listOf(0.33f, 0.66f, 1.0f).forEach { fraction ->
                        drawCircle(
                            color = Color(0xFFDDDDDD),
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
                        val normR = ((value + 1f) / 2f).coerceIn(0.15f, 1.0f) * maxR
                        val spokeEnd = RadialMath.toCartesian(center, maxR, angleDeg)
                        val point = RadialMath.toCartesian(center, normR, angleDeg)

                        // Draw spoke line
                        drawLine(
                            color = Color(0xFFCCCCCC),
                            start = center,
                            end = spokeEnd,
                            strokeWidth = 1f
                        )

                        // Add point to polygon
                        if (i == 0) polygonPath.moveTo(point.x, point.y) else polygonPath.lineTo(point.x, point.y)

                        // Draw node point
                        drawCircle(
                            color = Color.Black,
                            radius = 5f,
                            center = point
                        )
                    }
                    polygonPath.close()

                    // Draw the filled and stroked equalizer polygon
                    drawPath(
                        path = polygonPath,
                        color = Color.Black.copy(alpha = 0.10f)
                    )
                    drawPath(
                        path = polygonPath,
                        color = Color.Black,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
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

        // Section 3: Bitrate Rotary Transcoding Block
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "STREAMING BITRATE",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = Color.Black
                )
                Text(
                    text = "Selected Master: ${bitrates[selectedBitrateIndex]}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFF444444)
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.Black))

                // Bitrate options chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bitrates.forEachIndexed { index, rate ->
                        val isSelected = selectedBitrateIndex == index
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color.Black else Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
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
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isSelected) Color.White else Color.Black
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
            color = Color.Black
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -1f..1f,
            modifier = Modifier
                .height(110.dp)
                .width(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = Color.Black,
                activeTrackColor = Color.Black,
                inactiveTrackColor = Color(0xFFCCCCCC)
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = Color.Black
        )
    }
}
