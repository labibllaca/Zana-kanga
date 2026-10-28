package com.labix.navirom.ui.radial

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.data.model.PlaybackState
import com.labix.navirom.ui.util.formatMs
import com.labix.navirom.ui.util.rememberNaviromHaptics
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Unified Astrolabe Wheel.
 * Exactly one single wheel across the entire app that smoothly glides between:
 * 1) Playing-View (page 0): Fully visible concentric circle centered in the player.
 * 2) Library-View (page 1): Semicircle half-wheel anchored against the left border.
 * 3) Settings-View (page 2): Glides off-screen to the left.
 */
@Composable
fun UnifiedAstrolabeWheel(
    pagerState: PagerState,
    isAstrolabeModeEnabled: Boolean,
    playbackState: PlaybackState,
    currentTrack: NaviromTrack?,
    queue: List<NaviromTrack>,
    queueIndex: Int,
    libraryTotalItems: Int,
    libraryActiveIndex: Int,
    onLibraryIndexChanged: (Int) -> Unit,
    onPreviousTrack: () -> Unit,
    onNextTrack: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onTogglePlayPause: () -> Unit,
    onOpenOptionsModal: () -> Unit,
    isDark: Boolean,
    playerScrollOffsetYPx: Float = 0f,
    modifier: Modifier = Modifier
) {
    if (!isAstrolabeModeEnabled) return

    val haptics = rememberNaviromHaptics()
    val density = LocalDensity.current

    // Rotation state for player wheel (tracks current queue track)
    val trackCount = queue.size.coerceAtLeast(1)
    val stepAnglePlayer = 360f / trackCount.coerceAtLeast(4)
    val playerTargetAngle = remember(queueIndex, trackCount) {
        -(queueIndex * stepAnglePlayer)
    }

    // Rotation state for library wheel
    val stepAngleLibrary = 180f / libraryTotalItems.coerceIn(4, 36).toFloat()
    var libraryWheelRotation by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(libraryActiveIndex, stepAngleLibrary) {
        libraryWheelRotation = -(libraryActiveIndex * stepAngleLibrary)
    }

    // Continuous page transition fraction across 3 pages: 0 (Player), 1 (Library), 2 (Settings)
    val continuousPage = remember(pagerState) {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }.value

    // Hold-seek state on the player wheel (swipe forward/reverse and letting go)
    var isHoldSeeking by remember { mutableStateOf(false) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }
    val totalDurationMs = (playbackState.durationMs.takeIf { it > 0 }
        ?: currentTrack?.durationSeconds?.toLong()?.times(1000L) ?: 1L).coerceAtLeast(1L)
    val currentPosMs = playbackState.currentPositionMs.coerceIn(0L, totalDurationMs)
    val seekPreviewPosMs = (currentPosMs + seekDeltaMs).coerceIn(0L, totalDurationMs)
    val progressFraction = (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

    val currentPosMsState = rememberUpdatedState(currentPosMs)
    val totalDurationMsState = rememberUpdatedState(totalDurationMs)
    val onSeekToState = rememberUpdatedState(onSeekTo)
    val onTogglePlayPauseState = rememberUpdatedState(onTogglePlayPause)
    val onOpenOptionsModalState = rememberUpdatedState(onOpenOptionsModal)

    // Infinite vinyl rotation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "unified_vinyl_rot")
    val vinylRot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylRot"
    )
    val effectiveVinylRot = if (playbackState.isPlaying) vinylRot else 0f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .testTag("unified_astrolabe_wheel")
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // Page 0 (Player View): Centered full concentric wheel
        val playerCenterX = screenWidthPx / 2f
        val playerBaseCenterY = with(density) { 190.dp.toPx() }
        val playerCenterY = playerBaseCenterY - playerScrollOffsetYPx
        val playerRadius = with(density) { 110.dp.toPx() }

        // Page 1 (Library View): Semicircle half-wheel anchored on left border
        val libraryCenterX = with(density) { 2.dp.toPx() }
        val libraryTopOffsetPx = with(density) { 110.dp.toPx() }
        val libraryCenterY = libraryTopOffsetPx + (screenHeightPx - libraryTopOffsetPx) / 2f
        val libraryRadius = with(density) { 115.dp.toPx() }

        // Page 2 (Settings View): Glides off-screen to the left
        val settingsCenterX = -libraryRadius * 1.5f
        val settingsCenterY = libraryCenterY
        val settingsRadius = libraryRadius

        // Continuous interpolation across pages
        val currentCenterX: Float
        val currentCenterY: Float
        val currentRadius: Float

        if (continuousPage <= 1f) {
            val t01 = FastOutSlowInEasing.transform(continuousPage.coerceIn(0f, 1f))
            currentCenterX = lerp(playerCenterX, libraryCenterX, t01)
            currentCenterY = lerp(playerCenterY, libraryCenterY, t01)
            currentRadius = lerp(playerRadius, libraryRadius, t01)
        } else {
            val t12 = FastOutSlowInEasing.transform((continuousPage - 1f).coerceIn(0f, 1f))
            currentCenterX = lerp(libraryCenterX, settingsCenterX, t12)
            currentCenterY = libraryCenterY
            currentRadius = libraryRadius
        }

        // 1. Primary Canvas Rendering
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            val center = Offset(currentCenterX, currentCenterY)

            if (continuousPage < 0.55f) {
                // In Player View (page 0): FULL concentric circle wheel
                val playerAlpha = (1f - (continuousPage / 0.55f) * 0.4f).coerceIn(0.6f, 1f)
                val seekAngleOffset = if (isHoldSeeking) (seekDeltaMs / 60f) else 0f
                drawEditorialConcentricWheel(
                    center = center,
                    outerRadius = currentRadius,
                    progressFraction = progressFraction,
                    trackCount = queue.size.coerceAtLeast(4),
                    currentTrackIndex = queueIndex,
                    listCount = libraryTotalItems.coerceAtLeast(4),
                    activeListIndex = libraryActiveIndex,
                    rotationDeg = playerTargetAngle + seekAngleOffset,
                    isDark = isDark
                )
            } else {
                // In Library View (page 1) & Settings (page 2): Semicircle left-half wheel
                drawEditorialLeftHalfWheel(
                    center = center,
                    outerRadius = currentRadius,
                    rotationDeg = libraryWheelRotation,
                    listCount = libraryTotalItems.coerceAtLeast(4),
                    activeListIndex = libraryActiveIndex,
                    isDark = isDark
                )
            }
        }

        // 2. Interactive Center Hub on Player Wheel (when on Page 0)
        if (continuousPage < 0.45f) {
            val hubRadiusDp = with(density) { (playerRadius * 0.32f).toDp() }
            val hubCenterXDp = with(density) { currentCenterX.toDp() }
            val hubCenterYDp = with(density) { currentCenterY.toDp() }
            val hubAlpha = (1f - (continuousPage / 0.45f)).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (currentCenterX - with(density) { hubRadiusDp.toPx() }).toInt(),
                            y = (currentCenterY - with(density) { hubRadiusDp.toPx() }).toInt()
                        )
                    }
                    .size(hubRadiusDp * 2f)
                    .graphicsLayer { alpha = hubAlpha }
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF0F0F14) else Color.Black)
                    .border(
                        BorderStroke(1.5.dp, if (isDark) Color.White.copy(alpha = 0.5f) else Color.White),
                        CircleShape
                    )
                    .clickable {
                        haptics.toggle()
                        onTogglePlayPause()
                    }
                    .testTag("player_wheel_center_hub"),
                contentAlignment = Alignment.Center
            ) {
                // Center Album Cover or Vinyl spinning
                if (!currentTrack?.coverArtUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = currentTrack?.coverArtUrl,
                        contentDescription = "Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .graphicsLayer {
                                rotationZ = effectiveVinylRot
                            }
                    )
                }

                // Center Play/Pause Indicator Overlay
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 3. Player-View Gestures: Swipe Forward/Reverse and Let Go, Double-Tap for Options
        if (continuousPage < 0.35f) {
            val wheelDiameterDp = with(density) { (playerRadius * 2f).toDp() }
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (playerCenterX - playerRadius).toInt(),
                            y = (playerCenterY - playerRadius).toInt()
                        )
                    }
                    .size(wheelDiameterDp)
                    .pointerInput(currentPosMs, totalDurationMs) {
                        var totalDragX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                                isHoldSeeking = true
                                seekDeltaMs = 0L
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                totalDragX += dragAmount
                                // 1 pixel drag = 120ms of audio seek delta
                                val newDelta = (totalDragX * 120f).toLong()
                                if (abs(newDelta - seekDeltaMs) >= 1000L) {
                                    haptics.tick()
                                }
                                seekDeltaMs = newDelta
                            },
                            onDragEnd = {
                                if (isHoldSeeking) {
                                    val finalPos = (currentPosMsState.value + seekDeltaMs).coerceIn(0L, totalDurationMsState.value)
                                    if (abs(seekDeltaMs) > 400L) {
                                        haptics.toggle()
                                        onSeekToState.value(finalPos)
                                    }
                                    seekDeltaMs = 0L
                                    isHoldSeeking = false
                                }
                            },
                            onDragCancel = {
                                seekDeltaMs = 0L
                                isHoldSeeking = false
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                haptics.click()
                                onOpenOptionsModalState.value()
                            },
                            onTap = {
                                haptics.toggle()
                                onTogglePlayPauseState.value()
                            }
                        )
                    }
                    .testTag("player_wheel_gesture_area")
            )
        }

        // 4. Floating On-Screen Seek HUD when Swiping Forward/Reverse
        if (continuousPage < 0.35f && isHoldSeeking && abs(seekDeltaMs) > 300L) {
            val deltaSeconds = seekDeltaMs / 1000L
            val sign = if (deltaSeconds >= 0) "+" else ""

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDark) Color(0xFF18181D) else Color.Black,
                border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (playerCenterY - playerRadius - with(density) { 36.dp.toPx() }).toInt()
                        )
                    }
                    .testTag("swipe_seek_hud")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (seekDeltaMs >= 0) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$sign${deltaSeconds}s  •  ${formatMs(seekPreviewPosMs)} / ${formatMs(totalDurationMs)}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }

        // 5. Equator Index Tag in Library View (Page 1)
        if (continuousPage in 0.65f..1.45f) {
            val tagAlpha = if (continuousPage < 1f) {
                ((continuousPage - 0.65f) / 0.35f).coerceIn(0f, 1f)
            } else {
                (1f - ((continuousPage - 1f) / 0.45f)).coerceIn(0f, 1f)
            }

            Surface(
                shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
                color = if (isDark) Color.White else Color.Black,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (currentCenterX + with(density) { 4.dp.toPx() }).toInt(),
                            y = (currentCenterY - with(density) { 11.dp.toPx() }).toInt()
                        )
                    }
                    .graphicsLayer { alpha = tagAlpha }
            ) {
                Text(
                    text = " ${(libraryActiveIndex + 1).toString().padStart(2, '0')} ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    color = if (isDark) Color.Black else Color.White
                )
            }
        }

        // 6. Library gestures: vertical drag -> rotate wheel & scroll library items (Page 1)
        if (continuousPage in 0.55f..1.45f) {
            val halfWheelWidthDp = 115.dp
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(halfWheelWidthDp)
                    .pointerInput(libraryTotalItems) {
                        var accumulatedDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { accumulatedDrag = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                accumulatedDrag += dragAmount
                                libraryWheelRotation += dragAmount * 0.45f

                                val threshold = when {
                                    libraryTotalItems > 200 -> 9f
                                    libraryTotalItems > 100 -> 13f
                                    libraryTotalItems > 50 -> 18f
                                    else -> 24f
                                }
                                if (abs(accumulatedDrag) >= threshold && libraryTotalItems > 0) {
                                    val steps = (accumulatedDrag / threshold).toInt()
                                    if (steps != 0) {
                                        val newIdx = (libraryActiveIndex - steps).coerceIn(0, libraryTotalItems - 1)
                                        if (newIdx != libraryActiveIndex) {
                                            haptics.tick()
                                            onLibraryIndexChanged(newIdx)
                                        }
                                        accumulatedDrag -= steps * threshold
                                    }
                                }
                                accumulatedDrag = accumulatedDrag.coerceIn(-threshold * 2f, threshold * 2f)
                            }
                        )
                    }
            )
        }
    }
}
