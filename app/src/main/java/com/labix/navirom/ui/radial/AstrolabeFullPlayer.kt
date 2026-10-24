package com.labix.navirom.ui.radial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.data.model.PlaybackState
import com.labix.navirom.data.model.RepeatMode
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.NaviromStrings
import com.labix.navirom.ui.util.rememberNaviromHaptics
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Astrolabe Orbital Full Player featuring the Celestial Star Visualizer core,
 * radial album track lathe, concentric progress arc scrubber, and orbital satellite controls.
 */
@Composable
fun AstrolabeFullPlayer(
    playbackState: PlaybackState,
    isFavorite: Boolean,
    queue: List<NaviromTrack>,
    queueIndex: Int,
    appLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTrackSelect: (NaviromTrack, List<NaviromTrack>) -> Unit,
    onOpenWlanCast: (() -> Unit)? = null,
    onToggleClassicView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberNaviromHaptics()
    val scope = rememberCoroutineScope()
    val currentTrack = playbackState.currentTrack ?: queue.getOrNull(queueIndex)

    // Pulse animation for stars and celestial core
    val infiniteTransition = rememberInfiniteTransition(label = "AstrolabeCorePulse")
    val pulseEnergy by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "PulseEnergy"
    )
    val starfieldRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(90000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "StarfieldRotation"
    )

    // Track Lathe Dial State
    val trackCount = queue.size.coerceAtLeast(1)
    val stepAngle = 360f / trackCount.coerceAtLeast(4)
    val latheState = rememberRadialDialState(
        initialAngle = 0f,
        stepSizeDegrees = stepAngle
    )

    // Sync lathe rotation when queue index changes from external sources
    LaunchedEffect(queueIndex) {
        if (!latheState.isDragging && queue.isNotEmpty()) {
            val targetAngle = -(queueIndex * stepAngle)
            latheState.snapTo(targetAngle)
        }
    }

    // Concentric Progress Arc Scrubber State
    val totalDurationMs = (playbackState.durationMs ?: currentTrack?.durationSeconds?.toLong()?.times(1000L) ?: 1L).coerceAtLeast(1L)
    val currentPosMs = playbackState.currentPositionMs.coerceIn(0L, totalDurationMs)
    val progressFraction = (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AstrolabeTheme.DeepSpaceMidnight)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("astrolabe_full_player")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Astrolabe Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptics.click()
                        onDismiss()
                    },
                    modifier = Modifier.testTag("astrolabe_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AstrolabeTheme.ParchmentRim
                    )
                }

                // Astrolabe Mode Indicator with Switcher Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AstrolabeTheme.EngravedSlate.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AstrolabeTheme.AstrolabeGold.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        haptics.click()
                        onToggleClassicView()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RotateRight,
                            contentDescription = "Switch to Classic View",
                            tint = AstrolabeTheme.AstrolabeGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ASTROLABE WHEEL",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = AstrolabeTheme.ParchmentRim
                        )
                    }
                }

                // Favorite Button
                IconButton(
                    onClick = {
                        haptics.click()
                        onToggleFavorite()
                    },
                    modifier = Modifier.testTag("astrolabe_fav_btn")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color(0xFFE91E63) else AstrolabeTheme.ParchmentRim
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Current Track Header Info (Roman Numeral / Title / Artist)
            val romanIndex = getRomanNumeral((queueIndex + 1).coerceAtLeast(1))
            Text(
                text = "$romanIndex • ${currentTrack?.title ?: "Select a Track"}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                ),
                color = AstrolabeTheme.ParchmentRim,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = currentTrack?.artist ?: "Unknown Artist",
                style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.5.sp),
                color = AstrolabeTheme.StarCyan.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Concentric Orbital Area (Lathe + Progress Arc + Celestial Star Visualizer)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                var centerOffset by remember { mutableStateOf(Offset.Zero) }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val boxSize = minOf(maxWidth, maxHeight)
                    val radiusPx = (boxSize.value * 2.5f) / 2f
                    val centerPx = Offset(radiusPx, radiusPx)
                    centerOffset = centerPx

                    // Canvas rendering the background bezel, coordinate rings, and starfield
                    Canvas(
                        modifier = Modifier
                            .size(boxSize)
                            .radialRotatable(
                                state = latheState,
                                scope = scope,
                                centerProvider = { centerOffset },
                                haptics = haptics,
                                onDragEnd = { finalAngle ->
                                    // Lock in selected track when lathe settles
                                    val normalizedAngle = RadialMath.normalizeDegrees(-finalAngle)
                                    val selectedIdx = (normalizedAngle / stepAngle).toInt() % trackCount
                                    if (selectedIdx in queue.indices && selectedIdx != queueIndex) {
                                        onTrackSelect(queue[selectedIdx], queue)
                                    }
                                }
                            )
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerR = size.width * 0.48f
                        val middleR = size.width * 0.36f
                        val innerR = size.width * 0.22f

                        // Outer Astrolabe Bezel with Degree Ticks
                        drawAstrolabeBezel(
                            center = center,
                            outerRadius = outerR,
                            innerRadius = middleR
                        )

                        // Progress Scrubber Arc (Inner Ring)
                        val activeProgress = if (isScrubbing) scrubFraction else progressFraction
                        val sweepAngle = activeProgress * 360f

                        // Background inactive arc track
                        drawCircle(
                            color = AstrolabeTheme.EngravedSlate.copy(alpha = 0.5f),
                            radius = innerR + 10f,
                            center = center,
                            style = Stroke(width = 6f)
                        )
                        // Active progress illuminated arc
                        drawArc(
                            color = AstrolabeTheme.StarCyan,
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - innerR - 10f, center.y - innerR - 10f),
                            size = androidx.compose.ui.geometry.Size((innerR + 10f) * 2f, (innerR + 10f) * 2f),
                            style = Stroke(width = 6f, cap = StrokeCap.Round)
                        )

                        // Progress indicator celestial bead
                        val beadPos = RadialMath.toCartesian(center, innerR + 10f, -90f + sweepAngle)
                        drawCircle(
                            color = AstrolabeTheme.AstrolabeGold,
                            radius = 7f,
                            center = beadPos
                        )

                        // 12 o'clock Illuminated Needle Pointer for the Track Lathe
                        val needleTop = RadialMath.toCartesian(center, outerR + 4f, -90f)
                        val needleBottom = RadialMath.toCartesian(center, middleR - 2f, -90f)
                        drawLine(
                            color = AstrolabeTheme.ActivePointerGlow,
                            start = needleTop,
                            end = needleBottom,
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )

                        // Celestial Core with real-time starfield visualizer
                        drawCelestialCore(
                            center = center,
                            radius = innerR,
                            pulseEnergy = pulseEnergy,
                            rotationDeg = starfieldRotation,
                            isPlaying = playbackState.isPlaying
                        )
                    }

                    // Centered Album Art Thumbnail inside the Celestial Core
                    Box(
                        modifier = Modifier
                            .size(boxSize * 0.28f)
                            .clip(CircleShape)
                            .border(2.dp, AstrolabeTheme.AstrolabeGold.copy(alpha = 0.8f), CircleShape)
                            .clickable {
                                haptics.toggle()
                                onTogglePlayPause()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentTrack?.coverArtUrl?.isNotBlank() == true) {
                            AsyncImage(
                                model = currentTrack.coverArtUrl,
                                contentDescription = "Album Art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AstrolabeTheme.DeepSpaceMidnight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = AstrolabeTheme.AstrolabeGold,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        // Center Play/Pause Overlay Medallion
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                    tint = AstrolabeTheme.StarCyan,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Elapsed and Remaining Time Readouts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMs(currentPosMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = AstrolabeTheme.StarCyan
                )
                Text(
                    text = "ROTATE TO CUE TRACK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = AstrolabeTheme.AstrolabeGold.copy(alpha = 0.8f)
                )
                Text(
                    text = "-${formatMs(totalDurationMs - currentPosMs)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = AstrolabeTheme.ParchmentRim.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Orbital Satellite Playback Controls
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = AstrolabeTheme.EngravedSlate.copy(alpha = 0.6f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AstrolabeTheme.CelestialGrid),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Satellite
                    IconButton(
                        onClick = {
                            haptics.toggle()
                            onToggleShuffle()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffle) AstrolabeTheme.AstrolabeGold else AstrolabeTheme.ParchmentRim.copy(alpha = 0.6f)
                        )
                    }

                    // Previous Satellite
                    IconButton(
                        onClick = {
                            haptics.click()
                            onPrevious()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = AstrolabeTheme.ParchmentRim,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Main Center Play/Pause Satellite
                    FilledTonalIconButton(
                        onClick = {
                            haptics.toggle()
                            onTogglePlayPause()
                        },
                        modifier = Modifier.size(54.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = AstrolabeTheme.AstrolabeGold,
                            contentColor = AstrolabeTheme.DeepSpaceMidnight
                        )
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Next Satellite
                    IconButton(
                        onClick = {
                            haptics.click()
                            onNext()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = AstrolabeTheme.ParchmentRim,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat Satellite
                    IconButton(
                        onClick = {
                            haptics.toggle()
                            onCycleRepeat()
                        }
                    ) {
                        val isRepeat = playbackState.repeatMode != RepeatMode.OFF
                        Icon(
                            imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) AstrolabeTheme.AstrolabeGold else AstrolabeTheme.ParchmentRim.copy(alpha = 0.6f)
                        )
                    }

                    // WLAN Speaker Cast Satellite
                    if (onOpenWlanCast != null) {
                        IconButton(
                            onClick = {
                                haptics.click()
                                onOpenWlanCast()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cast,
                                contentDescription = "Cast",
                                tint = AstrolabeTheme.StarCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSecs = (ms / 1000).coerceAtLeast(0)
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun getRomanNumeral(n: Int): String {
    val map = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD",
        100 to "C", 90 to "XC", 50 to "L", 40 to "XL",
        10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )
    var num = n
    val sb = StringBuilder()
    for ((value, numeral) in map) {
        while (num >= value) {
            sb.append(numeral)
            num -= value
        }
    }
    return sb.toString()
}
