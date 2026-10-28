package com.labix.navirom.ui.radial

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import kotlin.math.hypot

/**
 * Unified Astrolabe Wheel.
 * Exactly one wheel across the entire app that smoothly glides between:
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

    // Continuous page transition fraction
    val continuousPage = remember(pagerState) {
        derivedStateOf {
            pagerState.currentPage + pagerState.currentPageOffsetFraction
        }
    }.value

    // Vinyl animation state
    var isVinylExpanded by remember { mutableStateOf(false) }
    var lastTouchTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastTouchTimestamp, isVinylExpanded, playbackState.isPlaying) {
        if (!isVinylExpanded && playbackState.isPlaying && continuousPage < 0.35f) {
            delay(5000L)
            if (System.currentTimeMillis() - lastTouchTimestamp >= 5000L && continuousPage < 0.35f) {
                isVinylExpanded = true
            }
        }
    }

    // Double tap detection
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    // Hold-seek state
    var isHoldSeeking by remember { mutableStateOf(false) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }
    val totalDurationMs = (playbackState.durationMs ?: currentTrack?.durationSeconds?.toLong()?.times(1000L) ?: 1L).coerceAtLeast(1L)
    val currentPosMs = playbackState.currentPositionMs.coerceIn(0L, totalDurationMs)
    val seekPreviewPosMs = (currentPosMs + seekDeltaMs).coerceIn(0L, totalDurationMs)

    // Remember updated states for gesture handler so pointerInput isn't cancelled mid-gesture by position ticks
    val currentPosMsState = rememberUpdatedState(currentPosMs)
    val totalDurationMsState = rememberUpdatedState(totalDurationMs)
    val onSeekToState = rememberUpdatedState(onSeekTo)
    val onNextTrackState = rememberUpdatedState(onNextTrack)
    val onPreviousTrackState = rememberUpdatedState(onPreviousTrack)
    val onTogglePlayPauseState = rememberUpdatedState(onTogglePlayPause)
    val onOpenOptionsModalState = rememberUpdatedState(onOpenOptionsModal)

    // Infinite vinyl rotation
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
        val screenHeightPx = constraints.maxHeight.toFloat()

        // Semicircle Astrolabe Wheel in Library-View (page 0)
        val libraryCenterX = with(density) { 2.dp.toPx() }
        val libraryTopOffsetPx = with(density) { 110.dp.toPx() }
        val libraryCenterY = libraryTopOffsetPx + (screenHeightPx - libraryTopOffsetPx) / 2f
        val libraryRadius = with(density) { 115.dp.toPx() }

        // Glides off-screen to the left on page 1 (Settings)
        val rawT = continuousPage.coerceIn(0f, 1f)
        val t = androidx.compose.animation.core.FastOutSlowInEasing.transform(rawT)
        val currentCenterX = lerp(libraryCenterX, -libraryRadius * 1.5f, t)
        val currentCenterY = libraryCenterY
        val currentRadius = libraryRadius
        val currentRotation = libraryWheelRotation

        // Primary Astrolabe Left-Half Wheel Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            val center = Offset(currentCenterX, currentCenterY)
            drawEditorialLeftHalfWheel(
                center = center,
                outerRadius = currentRadius,
                rotationDeg = currentRotation,
                listCount = libraryTotalItems.coerceAtLeast(4),
                activeListIndex = libraryActiveIndex,
                isDark = isDark
            )
        }

        // Equator Index Tag in Library View
        if (t < 0.6f) {
            val tagAlpha = (1f - (t / 0.6f)).coerceIn(0f, 1f)
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

        // Library gestures: vertical drag -> rotate wheel & scroll library items
        if (t < 0.35f) {
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
