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
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 1. Center in Playing-View (page 0)
        val playerCenterX = screenWidthPx / 2f
        val basePlayerCenterY = with(density) { 258.dp.toPx() }
        val playerCenterY = basePlayerCenterY - playerScrollOffsetYPx
        val playerRadius = with(density) { 132.5.dp.toPx() }

        // 2. Center in Library-View (page 1)
        val libraryCenterX = with(density) { 2.dp.toPx() }
        val libraryTopOffsetPx = with(density) { 110.dp.toPx() }
        val libraryCenterY = libraryTopOffsetPx + (screenHeightPx - libraryTopOffsetPx) / 2f
        val libraryRadius = with(density) { 115.dp.toPx() }

        // Continuous interpolation across pages with organic easing
        val rawT = continuousPage.coerceIn(0f, 1f)
        val t = androidx.compose.animation.core.FastOutSlowInEasing.transform(rawT)
        val (currentCenterX, currentCenterY, currentRadius) = if (continuousPage <= 1f) {
            Triple(
                lerp(playerCenterX, libraryCenterX, t),
                lerp(playerCenterY, libraryCenterY, t),
                lerp(playerRadius, libraryRadius, t)
            )
        } else {
            // Glides off-screen to the left on page 2 (Settings)
            val t2 = (continuousPage - 1f).coerceIn(0f, 1f)
            Triple(
                lerp(libraryCenterX, -libraryRadius * 1.5f, t2),
                libraryCenterY,
                libraryRadius
            )
        }

        // Rotation interpolation
        val currentRotation = if (continuousPage <= 1f) {
            lerp(playerTargetAngle, libraryWheelRotation, t)
        } else {
            libraryWheelRotation
        }

        // Center medallion size
        val baseMedallionSizeDp = with(density) { (currentRadius * 2f * 0.20f).toDp() }
        val expandedMedallionSizeDp = with(density) { (currentRadius * 2f * 0.72f).toDp() }
        val medallionTargetSizeDp = if (isVinylExpanded) expandedMedallionSizeDp else baseMedallionSizeDp
        val animatedMedallionSizeDp by animateDpAsState(
            targetValue = medallionTargetSizeDp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "unifiedMedallionSize"
        )
        val medallionSizePx = with(density) { animatedMedallionSizeDp.toPx() }

        // Primary Astrolabe Wheel Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            val center = Offset(currentCenterX, currentCenterY)
            if (t < 0.5f) {
                // Closer to player view: complete concentric wheel
                val progressFraction = (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f) * (1f - t)
                drawEditorialConcentricWheel(
                    center = center,
                    outerRadius = currentRadius,
                    progressFraction = progressFraction,
                    trackCount = trackCount,
                    currentTrackIndex = queueIndex,
                    listCount = 5,
                    activeListIndex = 0,
                    rotationDeg = currentRotation,
                    isDark = isDark
                )
            } else {
                // Closer to library view: left half vertical wheel
                drawEditorialLeftHalfWheel(
                    center = center,
                    outerRadius = currentRadius,
                    rotationDeg = currentRotation,
                    listCount = libraryTotalItems.coerceAtLeast(4),
                    activeListIndex = libraryActiveIndex,
                    isDark = isDark
                )
            }
        }

        // Center Medallion (Album Art / Vinyl Plate) in Player View
        if (t < 0.45f) {
            val medallionAlpha = (1f - (t / 0.45f)).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (currentCenterX - medallionSizePx / 2f).toInt(),
                            y = (currentCenterY - medallionSizePx / 2f).toInt()
                        )
                    }
                    .size(animatedMedallionSizeDp)
                    .graphicsLayer {
                        rotationZ = effectiveVinylRot
                        alpha = medallionAlpha
                    }
                    .clip(CircleShape)
                    .background(if (isVinylExpanded) Color.Black else Color.Transparent)
                    .border(
                        width = if (isVinylExpanded) 3.dp else 1.5.dp,
                        color = Color.Black,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves background when expanded
                if (isVinylExpanded) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2f
                        drawCircle(color = Color(0xFF111111), radius = radius)
                        var r = radius * 0.35f
                        while (r < radius * 0.94f) {
                            drawCircle(color = Color(0xFF242424), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f))
                            r += 5.dp.toPx()
                        }
                        val sheenBrush = androidx.compose.ui.graphics.Brush.sweepGradient(
                            0.0f to Color.Transparent,
                            0.12f to Color.White.copy(alpha = 0.14f),
                            0.25f to Color.Transparent,
                            0.50f to Color.Transparent,
                            0.62f to Color.White.copy(alpha = 0.14f),
                            0.75f to Color.Transparent,
                            1.0f to Color.Transparent,
                            center = center
                        )
                        drawCircle(brush = sheenBrush, radius = radius * 0.94f)
                    }
                }

                val artSize = if (isVinylExpanded) animatedMedallionSizeDp * 0.44f else animatedMedallionSizeDp
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .clip(CircleShape)
                        .border(1.dp, Color.White, CircleShape),
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
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (isVinylExpanded) 16.dp else 20.dp)
                            )
                        }
                    }
                }

                if (!isVinylExpanded) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.55f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Equator Index Tag in Library View
        if (t > 0.65f) {
            val tagAlpha = ((t - 0.65f) / 0.35f).coerceIn(0f, 1f)
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

        // Seek HUD Overlay in Player View
        if (isHoldSeeking && t < 0.35f) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.90f),
                border = BorderStroke(1.5.dp, Color.White),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (currentCenterX - with(density) { 90.dp.toPx() }).toInt(),
                            y = (currentCenterY - with(density) { 36.dp.toPx() }).toInt()
                        )
                    }
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (seekDeltaMs >= 0) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                        contentDescription = if (seekDeltaMs >= 0) "Fast Forward" else "Rewind",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val deltaSec = seekDeltaMs / 1000L
                        val sign = if (deltaSec >= 0) "+" else ""
                        Text(
                            text = "$sign${deltaSec}s",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "${formatMs(seekPreviewPosMs)} / ${formatMs(totalDurationMs)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Gesture Controller Layer
        // When t < 0.35f: Player gestures (vertical swipe -> change song, horizontal swipe -> seek, double tap -> options modal, center tap -> play/pause)
        // When t > 0.65f: Library gestures (vertical drag -> rotate wheel & scroll library items)
        if (t < 0.35f) {
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (currentCenterX - currentRadius).toInt(),
                            y = (currentCenterY - currentRadius).toInt()
                        )
                    }
                    .size(with(density) { (currentRadius * 2f).toDp() })
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val now = System.currentTimeMillis()
                            lastTouchTimestamp = now
                            val downPos = down.position
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val distanceFromCenter = hypot(downPos.x - center.x, downPos.y - center.y)
                            val isInsideWheel = distanceFromCenter <= size.width / 2f

                            if (!isInsideWheel) return@awaitEachGesture

                            val startPosMs = currentPosMsState.value
                            val durMs = totalDurationMsState.value

                            var gestureMode = 0 // 0 = UNDECIDED, 1 = VERTICAL_SONG_SWITCH, 2 = HORIZONTAL_SEEK
                            var totalMovedX = 0f
                            var totalMovedY = 0f
                            var lastPos = downPos
                            var activeSeekTargetMs = startPosMs

                            val dragSlop = 12f
                            val songChangeThreshold = 40f

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                if (change.pressed) {
                                    val currentPos = change.position
                                    val dx = currentPos.x - lastPos.x
                                    val dy = currentPos.y - lastPos.y
                                    totalMovedX += dx
                                    totalMovedY += dy
                                    lastPos = currentPos
                                    lastTouchTimestamp = System.currentTimeMillis()

                                    change.consume()

                                    if (gestureMode == 0) {
                                        val totalDist = hypot(totalMovedX, totalMovedY)
                                        if (totalDist >= dragSlop) {
                                            gestureMode = if (abs(totalMovedY) >= abs(totalMovedX)) 1 else 2
                                            if (gestureMode == 2) {
                                                isHoldSeeking = true
                                            }
                                        }
                                    }

                                    if (gestureMode == 1) {
                                        if (abs(totalMovedY) >= songChangeThreshold) {
                                            haptics.tick()
                                            if (totalMovedY < 0) {
                                                onNextTrackState.value()
                                            } else {
                                                onPreviousTrackState.value()
                                            }
                                            totalMovedY = 0f
                                        }
                                    } else if (gestureMode == 2) {
                                        val deltaSec = (totalMovedX * 0.22f).toLong()
                                        val deltaMs = deltaSec * 1000L
                                        seekDeltaMs = deltaMs
                                        activeSeekTargetMs = (startPosMs + deltaMs).coerceIn(0L, durMs)
                                    }
                                } else {
                                    break
                                }
                            }

                            if (gestureMode == 2) {
                                haptics.toggle()
                                isHoldSeeking = false
                                onSeekToState.value(activeSeekTargetMs)
                                seekDeltaMs = 0L
                                lastTouchTimestamp = System.currentTimeMillis()
                            } else if (gestureMode == 0) {
                                val timeDiff = now - lastTapTimestamp
                                val distDiff = hypot(downPos.x - lastTapPosition.x, downPos.y - lastTapPosition.y)
                                if (timeDiff in 1..320 && distDiff < 70f) {
                                    lastTapTimestamp = 0L
                                    haptics.toggle()
                                    onOpenOptionsModalState.value()
                                } else {
                                    lastTapTimestamp = now
                                    lastTapPosition = downPos
                                    if (isVinylExpanded) {
                                        isVinylExpanded = false
                                        lastTouchTimestamp = System.currentTimeMillis()
                                    } else {
                                        haptics.click()
                                        onTogglePlayPauseState.value()
                                    }
                                }
                            }
                        }
                    }
            )
        } else if (t > 0.65f) {
            val halfWheelWidthDp = 115.dp
            val halfWheelWidthPx = with(density) { halfWheelWidthDp.toPx() }
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
