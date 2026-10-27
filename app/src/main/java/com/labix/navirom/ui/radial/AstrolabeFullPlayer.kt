package com.labix.navirom.ui.radial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.labix.navirom.ui.util.formatMs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.labix.navirom.data.lyrics.LyricsData
import com.labix.navirom.data.lyrics.LyricLine
import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.data.model.PlaybackState
import com.labix.navirom.data.model.RepeatMode
import com.labix.navirom.data.model.DualSpeakerMode
import com.labix.navirom.data.model.SleepTimerOptions
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.util.rememberNaviromHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Editorial item model for sectors arranged around the wheel matching zoom-kal.jpg.
 */
data class EditorialSectorItem(
    val indexLabel: String,
    val title: String,
    val description: String,
    val isSelected: Boolean = false,
    val onClick: (() -> Unit)? = null
)

data class AstrolabePlayerState(
    val playbackState: PlaybackState = PlaybackState(),
    val isFavorite: Boolean = false,
    val queue: List<NaviromTrack> = emptyList(),
    val queueIndex: Int = 0,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH,
    val lyricsData: LyricsData? = null,
    val sleepTimerOptions: SleepTimerOptions = SleepTimerOptions(),
    val isDualAudioEnabled: Boolean = false,
    val dualSpeakerMode: DualSpeakerMode = DualSpeakerMode.DUAL_STEREO,
    val isCrossfadeEnabled: Boolean = false,
    val crossfadeDurationSeconds: Int = 5,
    val isVinylEffectEnabled: Boolean = true
)

data class AstrolabePlayerActions(
    val onDismiss: () -> Unit = {},
    val onTogglePlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onSeekTo: (Long) -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onCycleRepeat: () -> Unit = {},
    val onToggleFavorite: () -> Unit = {},
    val onTrackSelect: (NaviromTrack, List<NaviromTrack>) -> Unit = { _, _ -> },
    val onOpenWlanCast: (() -> Unit)? = null,
    val onToggleClassicView: () -> Unit = {},
    val onSetSleepTimer: ((Int?) -> Unit)? = null,
    val onUpdateSleepTimerOptions: ((SleepTimerOptions) -> Unit)? = null,
    val onToggleDualAudio: (() -> Unit)? = null,
    val onSetDualSpeakerMode: ((DualSpeakerMode) -> Unit)? = null,
    val onToggleCrossfade: (() -> Unit)? = null,
    val onSetCrossfadeDuration: ((Int) -> Unit)? = null,
    val onToggleVinylEffect: (() -> Unit)? = null,
    val onOpenOptionsModal: (() -> Unit)? = null
)

/**
 * Astrolabe Orbital Full Player.
 * Restructured matching user requirements & zoom-kal.jpg:
 * - Concentric wheel & track info elevated directly to the top (concentric wheel badge removed).
 * - Turning the wheel rotates the dial, updates real-time target telemetry, and scrolls the active playlist.
 * - Holding (>320ms) and sliding left/right on the wheel seeks backward/forward with an on-screen HUD.
 * - Double-clicking the wheel opens an editorial modal with countdown/sleep timer, double player, crossfade, and vinyl effect.
 * - Current playing list rendered in an editorial sector block, with manuscript lyrics directly below.
 * - Pure white background, solid black borders, serif typography, shelf ticks, and 4.5dp solid black vertical bars.
 */
@Composable
fun AstrolabeFullPlayer(
    state: AstrolabePlayerState,
    actions: AstrolabePlayerActions,
    hasExternalWheel: Boolean = false,
    modifier: Modifier = Modifier
) {
    val playbackState = state.playbackState
    val isFavorite = state.isFavorite
    val queue = state.queue
    val queueIndex = state.queueIndex
    val appLanguage = state.appLanguage
    val lyricsData = state.lyricsData
    val sleepTimerOptions = state.sleepTimerOptions
    val isDualAudioEnabled = state.isDualAudioEnabled
    val dualSpeakerMode = state.dualSpeakerMode
    val isCrossfadeEnabled = state.isCrossfadeEnabled
    val crossfadeDurationSeconds = state.crossfadeDurationSeconds
    val isVinylEffectEnabled = state.isVinylEffectEnabled

    val onDismiss = actions.onDismiss
    val onTogglePlayPause = actions.onTogglePlayPause
    val onNext = actions.onNext
    val onPrevious = actions.onPrevious
    val onSeekTo = actions.onSeekTo
    val onToggleShuffle = actions.onToggleShuffle
    val onCycleRepeat = actions.onCycleRepeat
    val onToggleFavorite = actions.onToggleFavorite
    val onTrackSelect = actions.onTrackSelect
    val onOpenWlanCast = actions.onOpenWlanCast
    val onToggleClassicView = actions.onToggleClassicView
    val onSetSleepTimer = actions.onSetSleepTimer
    val onUpdateSleepTimerOptions = actions.onUpdateSleepTimerOptions
    val onToggleDualAudio = actions.onToggleDualAudio
    val onSetDualSpeakerMode = actions.onSetDualSpeakerMode
    val onToggleCrossfade = actions.onToggleCrossfade
    val onSetCrossfadeDuration = actions.onSetCrossfadeDuration
    val onToggleVinylEffect = actions.onToggleVinylEffect

    val haptics = rememberNaviromHaptics()
    val scope = rememberCoroutineScope()
    val currentTrack = playbackState.currentTrack ?: queue.getOrNull(queueIndex)
    
    // Track Lathe Dial State
    val trackCount = queue.size.coerceAtLeast(1)
    val stepAngle = 360f / trackCount.coerceAtLeast(4)
    val latheState = rememberRadialDialState(
        initialAngle = 0f,
        stepSizeDegrees = stepAngle
    )

    // Synchronized scroll state for queue list
    val queueLazyListState = rememberLazyListState()

    // Wheel options modal state (shown on double click)
    var showOptionsModal by remember { mutableStateOf(false) }

    // Hold + Horizontal Slide to Seek state
    var isHoldSeeking by remember { mutableStateOf(false) }
    var seekPreviewPosMs by remember { mutableLongStateOf(0L) }
    var seekDeltaMs by remember { mutableLongStateOf(0L) }

    // 5-second inactivity vinyl mode state
    var lastTouchTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var isVinylExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(lastTouchTimestamp, isVinylExpanded, latheState.isDragging) {
        if (!isVinylExpanded && !latheState.isDragging) {
            delay(5000L)
            if (!latheState.isDragging && System.currentTimeMillis() - lastTouchTimestamp >= 5000L) {
                isVinylExpanded = true
            }
        }
    }

    // Double click detection timestamp and position
    var lastTapTimestamp by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    // Real-time Rotational Target Telemetry
    val targetedIndex = remember(latheState.angle, trackCount) {
        if (trackCount <= 0) 0 else {
            val normalizedAngle = RadialMath.normalizeDegrees(-latheState.angle)
            val rawIdx = (normalizedAngle / stepAngle).toInt()
            val idx = ((rawIdx % trackCount) + trackCount) % trackCount
            idx.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
        }
    }
    val targetedTrack = queue.getOrNull(targetedIndex)

    // Sync lathe rotation when queue index changes normally
    LaunchedEffect(queueIndex, queue.size) {
        if (!latheState.isDragging && queue.isNotEmpty()) {
            val targetAngle = -(queueIndex * stepAngle)
            latheState.snapTo(targetAngle)
        }
    }

    // Concentric Progress Arc Scrubber State
    val totalDurationMs = (playbackState.durationMs ?: currentTrack?.durationSeconds?.toLong()?.times(1000L) ?: 1L).coerceAtLeast(1L)
    val currentPosMs = playbackState.currentPositionMs.coerceIn(0L, totalDurationMs)
    val progressFraction = (currentPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val canvasBg = MaterialTheme.colorScheme.background
    val inkColor = if (isDark) Color.White else Color.Black
    val secondaryInk = if (isDark) Color(0xFFAAAAAF) else Color(0xFF444444)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(isVinylExpanded) {
                if (isVinylExpanded) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isVinylExpanded = false
                        lastTouchTimestamp = System.currentTimeMillis()
                    }
                }
            }
            .testTag("astrolabe_full_player")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Astrolabe Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
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
                        tint = inkColor
                    )
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
                        tint = if (isFavorite) (if (isDark) Color.White else Color.Black) else (if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f))
                    )
                }
            }

            // ==========================================
            // PREPARED DATA FOR SECTORS
            // ==========================================
            val queueSectorItems = remember(queue, queueIndex, targetedIndex, playbackState.isPlaying) {
                queue.mapIndexed { idx, track ->
                    val isCurrent = (idx == queueIndex || track.id == currentTrack?.id)
                    val isTargeted = (idx == targetedIndex && latheState.isDragging)
                    EditorialSectorItem(
                        indexLabel = (idx + 1).toString().padStart(2, '0'),
                        title = track.title + if (isCurrent && playbackState.isPlaying) "  ▶" else "",
                        description = "${track.artist} • ${formatMs(track.durationSeconds * 1000L)}" +
                            if (isTargeted) " • [CUED ON WHEEL]" else if (isCurrent) " • [CURRENTLY PLAYING]" else "",
                        isSelected = isCurrent || isTargeted,
                        onClick = {
                            haptics.click()
                            onTrackSelect(track, queue)
                        }
                    )
                }
            }

            val hasSyncedLyrics = lyricsData?.syncedLines?.isNotEmpty() == true
            val hasPlainLyrics = lyricsData?.plainLyrics?.isNotBlank() == true

            val lyricItems = remember(lyricsData?.syncedLines, currentPosMs) {
                val syncedLines = lyricsData?.syncedLines ?: emptyList()
                val activeLyricIdx = syncedLines.indexOfLast { (it.timeMs - 350L) <= currentPosMs }
                syncedLines.mapIndexed { idx, line ->
                    val isLineActive = (idx == activeLyricIdx)
                    EditorialSectorItem(
                        indexLabel = "",
                        title = line.text.ifBlank { "• • •" },
                        description = if (isLineActive) "Active vocal phase • tap line to cue playback" else "Synchronized stanza line",
                        isSelected = isLineActive,
                        onClick = {
                            haptics.click()
                            onSeekTo(line.timeMs)
                        }
                    )
                }
            }

            val plainItems = remember(lyricsData?.plainLyrics) {
                lyricsData?.plainLyrics.orEmpty()
                    .lines()
                    .filter { it.isNotBlank() }
                    .take(40)
                    .map { line ->
                        EditorialSectorItem(
                            indexLabel = "",
                            title = line,
                            description = "Unsynchronized lyric verse",
                            isSelected = false
                        )
                    }
            }

            var isQueueCollapsed by remember { mutableStateOf(false) }
            var isLyricsCollapsed by remember { mutableStateOf(false) }

            val currentOnNext by rememberUpdatedState(onNext)
            val currentOnPrevious by rememberUpdatedState(onPrevious)
            val currentOnSeekTo by rememberUpdatedState(onSeekTo)
            val currentOnTogglePlayPause by rememberUpdatedState(onTogglePlayPause)
            val currentQueue by rememberUpdatedState(queue)
            val currentQueueIndex by rememberUpdatedState(queueIndex)
            val currentPlaybackState by rememberUpdatedState(playbackState)

            // Scrollable Content Area: Single unified LazyColumn ensuring fully scrollable song lists
            LazyColumn(
                state = queueLazyListState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item(key = "astrolabe_wheel_section") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Current Track Header Info (Roman Numeral / Title / Artist)
                        val romanIndex = getRomanNumeral((queueIndex + 1).coerceAtLeast(1))
                        Text(
                            text = "$romanIndex • ${currentTrack?.title ?: "Select a Track"}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 15.sp
                            ),
                            color = inkColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "${currentTrack?.artist ?: "Unknown Artist"} • ${currentTrack?.album ?: "Unknown Album"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 0.5.sp,
                                fontSize = 11.sp
                            ),
                            color = secondaryInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )

                        // Architectural Ring Hierarchy Guide
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRACK",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 9.sp
                                ),
                                color = inkColor
                            )
                            Text("•", color = Color.Gray, fontSize = 9.sp)
                            Text(
                                text = "ALBUM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 9.sp
                                ),
                                color = inkColor
                            )
                            Text("•", color = Color.Gray, fontSize = 9.sp)
                            Text(
                                text = "LISTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 9.sp
                                ),
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Spacer(modifier = Modifier.height(240.dp))

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ==========================================
                // EDITORIAL SECTOR 1: CURRENT PLAYING LIST / QUEUE
                // ==========================================
                item(key = "queue_header") {
                    EditorialSectorHeaderRow(
                        sectorTitle = "CURRENT PLAYING LIST",
                        sectorSubTitle = null,
                        isCollapsed = isQueueCollapsed,
                        onToggleCollapse = {
                            haptics.click()
                            isQueueCollapsed = !isQueueCollapsed
                        }
                    )
                }

                if (!isQueueCollapsed) {
                    val displayQueue = if (queueSectorItems.isNotEmpty()) queueSectorItems else listOf(
                        EditorialSectorItem(
                            indexLabel = "01",
                            title = currentTrack?.title ?: "No track queued",
                            description = currentTrack?.artist ?: "Unknown artist"
                        )
                    )
                    itemsIndexed(
                        items = displayQueue,
                        key = { idx, item -> "queue_${idx}_${item.title}" }
                    ) { _, item ->
                        EditorialSectorItemRow(item = item)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // ==========================================
                // EDITORIAL SECTOR 2: LYRICS
                // ==========================================
                if (hasSyncedLyrics || hasPlainLyrics) {
                    item(key = "lyrics_header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        EditorialSectorHeaderRow(
                            sectorTitle = "MANUSCRIPT LYRICS",
                            sectorSubTitle = null,
                            isCollapsed = isLyricsCollapsed,
                            onToggleCollapse = {
                                haptics.click()
                                isLyricsCollapsed = !isLyricsCollapsed
                            }
                        )
                    }

                    if (!isLyricsCollapsed) {
                        val activeLyricList = if (hasSyncedLyrics) lyricItems else plainItems
                        itemsIndexed(
                            items = activeLyricList,
                            key = { idx, _ -> "lyric_$idx" }
                        ) { _, item ->
                            EditorialSectorItemRow(item = item)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    } else {
                        val activeLine = lyricItems.firstOrNull { it.isSelected } ?: lyricItems.firstOrNull()
                        if (activeLine != null) {
                            item(key = "active_lyric_line") {
                                EditorialSectorItemRow(item = activeLine)
                            }
                        }
                    }
                }

                item(key = "list_bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ==========================================
            // DOCKED BOTTOM TIME & CONTROLS DOCK
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMs(currentPosMs),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = inkColor
                )
                Text(
                    text = "${queueIndex + 1} / ${queue.size.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = inkColor
                )
                Text(
                    text = "-${formatMs(totalDurationMs - currentPosMs)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = inkColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Playback Controls Bar with Stark Border
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDark) Color(0xFF18181D) else Color.White,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isDark) Color.White.copy(alpha = 0.3f) else Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptics.toggle()
                            onToggleShuffle()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playbackState.isShuffle) inkColor else inkColor.copy(alpha = 0.35f)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.click()
                            onPrevious()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous",
                            tint = inkColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            haptics.toggle()
                            onTogglePlayPause()
                        },
                        modifier = Modifier.size(50.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isDark) Color.White else Color.Black,
                            contentColor = if (isDark) Color.Black else Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.click()
                            onNext()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next",
                            tint = inkColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            haptics.click()
                            onCycleRepeat()
                        }
                    ) {
                        val isRepeat = playbackState.repeatMode != RepeatMode.OFF
                        Icon(
                            imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) inkColor else inkColor.copy(alpha = 0.35f)
                        )
                    }

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
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // DOUBLE-CLICK OPTIONS MODAL
        // Countdown timer, double player, crossfade, vinyl effect, etc.
        // ==========================================
        if (showOptionsModal) {
            AstrolabeOptionsModal(
                playbackState = playbackState,
                onSetSleepTimer = onSetSleepTimer,
                sleepTimerOptions = sleepTimerOptions,
                onUpdateSleepTimerOptions = onUpdateSleepTimerOptions,
                isDualAudioEnabled = isDualAudioEnabled,
                onToggleDualAudio = onToggleDualAudio,
                dualSpeakerMode = dualSpeakerMode,
                onSetDualSpeakerMode = onSetDualSpeakerMode,
                isCrossfadeEnabled = isCrossfadeEnabled,
                onToggleCrossfade = onToggleCrossfade,
                crossfadeDurationSeconds = crossfadeDurationSeconds,
                onSetCrossfadeDuration = onSetCrossfadeDuration,
                isVinylEffectEnabled = isVinylEffectEnabled,
                onToggleVinylEffect = onToggleVinylEffect,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onOpenWlanCast = onOpenWlanCast,
                onDismiss = { showOptionsModal = false }
            )
        }
    }
}

/**
 * Astrolabe Options Modal displayed when double-clicking the wheel.
 */
@Composable
fun AstrolabeOptionsModal(
    playbackState: PlaybackState,
    onSetSleepTimer: ((Int?) -> Unit)?,
    sleepTimerOptions: SleepTimerOptions = SleepTimerOptions(),
    onUpdateSleepTimerOptions: ((SleepTimerOptions) -> Unit)? = null,
    isDualAudioEnabled: Boolean,
    onToggleDualAudio: (() -> Unit)?,
    dualSpeakerMode: DualSpeakerMode,
    onSetDualSpeakerMode: ((DualSpeakerMode) -> Unit)?,
    isCrossfadeEnabled: Boolean,
    onToggleCrossfade: (() -> Unit)?,
    crossfadeDurationSeconds: Int,
    onSetCrossfadeDuration: ((Int) -> Unit)?,
    isVinylEffectEnabled: Boolean,
    onToggleVinylEffect: (() -> Unit)?,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenWlanCast: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val haptics = rememberNaviromHaptics()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
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
                                .height(24.dp)
                                .background(Color.Black)
                        )
                        Column {
                            Text(
                                text = "ORBITAL CONTROLS",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    fontSize = 16.sp
                                ),
                                color = Color.Black
                            )
                            Text(
                                text = "DOUBLE-CLICK WHEEL OPTIONS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    letterSpacing = 1.sp,
                                    fontSize = 9.sp
                                ),
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptics.click()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.Black
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.Black)
                )

                // 1. SLEEP TIMER / COUNTDOWN
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "COUNTDOWN & SLEEP TIMER",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            fontSize = 12.sp
                        ),
                        color = Color.Black
                    )

                    val isTimerActive = (playbackState.sleepTimerMinutesLeft ?: 0) > 0 || (playbackState.sleepTimerSecondsLeft ?: 0) > 0
                    if (isTimerActive) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ACTIVE: ${playbackState.sleepTimerMinutesLeft ?: 0}m ${playbackState.sleepTimerSecondsLeft ?: 0}s",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                                Text(
                                    text = "CANCEL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.clickable {
                                        haptics.click()
                                        onSetSleepTimer?.invoke(null)
                                    }
                                )
                            }
                        }
                    }

                    // Preset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(null to "OFF", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m").forEach { (mins, label) ->
                            val isSelected = if (mins == null) !isTimerActive else (playbackState.sleepTimerMinutesLeft == mins)
                            OutlinedButton(
                                onClick = {
                                    haptics.click()
                                    onSetSleepTimer?.invoke(mins)
                                },
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) Color.Black else Color.White,
                                    contentColor = if (isSelected) Color.White else Color.Black
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                        }
                    }

                    // Connectivity Deactivation Switch (Wi-Fi, Bluetooth, GPS)
                    var disableConnectivity by remember(playbackState.sleepTimerOptions, sleepTimerOptions) {
                        mutableStateOf(
                            sleepTimerOptions.disableWifi ||
                            sleepTimerOptions.disableBluetooth ||
                            sleepTimerOptions.disableGps ||
                            playbackState.sleepTimerOptions.disableWifi ||
                            playbackState.sleepTimerOptions.disableBluetooth ||
                            playbackState.sleepTimerOptions.disableGps
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DEACTIVATE WIFI / BT / GPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = Color.Black
                            )
                            Text(
                                text = if (disableConnectivity) "Deactivates Wi-Fi, Bluetooth & GPS on timer end" else "Keep wireless radios active",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 9.sp
                                ),
                                color = Color(0xFF666666)
                            )
                        }

                        Switch(
                            checked = disableConnectivity,
                            onCheckedChange = { checked ->
                                haptics.toggle()
                                disableConnectivity = checked
                                val newOpts = SleepTimerOptions(
                                    disableWifi = checked,
                                    disableBluetooth = checked,
                                    disableGps = checked,
                                    disableMobileData = checked
                                )
                                onUpdateSleepTimerOptions?.invoke(newOpts)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color.Black,
                                uncheckedThumbColor = Color.Black,
                                uncheckedTrackColor = Color.White
                            )
                        )
                    }
                }

                // 2. DUAL AUDIO / DOUBLE PLAYER
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DOUBLE PLAYER & DUAL AUDIO",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 12.sp
                                ),
                                color = Color.Black
                            )
                            Text(
                                text = if (isDualAudioEnabled) "Dual Output Active" else "Single Channel Output",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF666666)
                            )
                        }

                        Button(
                            onClick = {
                                haptics.toggle()
                                onToggleDualAudio?.invoke()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDualAudioEnabled) Color.Black else Color.White,
                                contentColor = if (isDualAudioEnabled) Color.White else Color.Black
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isDualAudioEnabled) "ACTIVE" else "ENABLE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    if (isDualAudioEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                DualSpeakerMode.DUAL_STEREO to "STEREO",
                                DualSpeakerMode.STEREO_PAIR_LR to "PAIR L/R",
                                DualSpeakerMode.STEREO_PAIR_RL to "PAIR R/L"
                            ).forEach { (mode, label) ->
                                val isSelected = dualSpeakerMode == mode
                                OutlinedButton(
                                    onClick = {
                                        haptics.click()
                                        onSetDualSpeakerMode?.invoke(mode)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Color.Black else Color.White,
                                        contentColor = if (isSelected) Color.White else Color.Black
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (onOpenWlanCast != null) {
                        OutlinedButton(
                            onClick = {
                                haptics.click()
                                onDismiss()
                                onOpenWlanCast()
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Cast, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "WLAN / DLNA SPEAKER CAST",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }

                // 3. CROSSFADE HARMONICS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CROSSFADE HARMONICS",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    fontSize = 12.sp
                                ),
                                color = Color.Black
                            )
                            Text(
                                text = if (isCrossfadeEnabled) "${crossfadeDurationSeconds}s overlap" else "Crossfade disabled",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 10.sp
                                ),
                                color = Color(0xFF666666)
                            )
                        }

                        Button(
                            onClick = {
                                haptics.toggle()
                                onToggleCrossfade?.invoke()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCrossfadeEnabled) Color.Black else Color.White,
                                contentColor = if (isCrossfadeEnabled) Color.White else Color.Black
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isCrossfadeEnabled) "ENABLED" else "DISABLED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    if (isCrossfadeEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(2, 4, 6, 8, 12).forEach { sec ->
                                val isSelected = crossfadeDurationSeconds == sec
                                OutlinedButton(
                                    onClick = {
                                        haptics.click()
                                        onSetCrossfadeDuration?.invoke(sec)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Color.Black else Color.White,
                                        contentColor = if (isSelected) Color.White else Color.Black
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${sec}s",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. VINYL WARMTH EFFECT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "VINYL WARMTH & CRACKLE",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontSize = 12.sp
                            ),
                            color = Color.Black
                        )
                        Text(
                            text = if (isVinylEffectEnabled) "Analog acoustic texture on" else "Pure digital fidelity",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFF666666)
                        )
                    }

                    Button(
                        onClick = {
                            haptics.toggle()
                            onToggleVinylEffect?.invoke()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isVinylEffectEnabled) Color.Black else Color.White,
                            contentColor = if (isVinylEffectEnabled) Color.White else Color.Black
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isVinylEffectEnabled) "ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // 5. REPEAT & SHUFFLE ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptics.toggle()
                            onToggleShuffle()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (playbackState.isShuffle) Color.Black else Color.White,
                            contentColor = if (playbackState.isShuffle) Color.White else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (playbackState.isShuffle) "SHUFFLE ON" else "SHUFFLE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            haptics.toggle()
                            onCycleRepeat()
                        },
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (playbackState.repeatMode != RepeatMode.OFF) Color.Black else Color.White,
                            contentColor = if (playbackState.repeatMode != RepeatMode.OFF) Color.White else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        val icon = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (playbackState.repeatMode) {
                                RepeatMode.OFF -> "REPEAT OFF"
                                RepeatMode.ONE -> "REPEAT ONE"
                                RepeatMode.ALL -> "REPEAT ALL"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders an editorial sector block formatted identically to zoom-kal.jpg:
 * Header in serif uppercase, numbers with shelf tick dividers on the left,
 * signature 4.5dp solid black vertical accent bar, and multi-line title/description on the right.
 */
@Composable
fun EditorialSectorHeaderRow(
    sectorTitle: String,
    sectorSubTitle: String? = null,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val inkColor = if (isDark) Color.White else Color.Black
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black
    val secondaryInk = if (isDark) Color(0xFFAAAAAF) else Color(0xFF666666)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleCollapse() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isCollapsed) "▶ $sectorTitle" else "▼ $sectorTitle",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.5.sp,
                        fontSize = 15.sp
                    ),
                    color = inkColor
                )
            }
            if (sectorSubTitle != null) {
                Text(
                    text = if (isCollapsed) "[MINIMIZED]" else sectorSubTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = secondaryInk
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(dividerColor)
        )

        Spacer(modifier = Modifier.height(6.dp))
    }
}

/**
 * Editorial item model for sectors arranged around the wheel matching zoom-kal.jpg.
 * Each item contains a left section with track index label, a subtle 1dp horizontal tick,
 * signature 4.5dp solid black vertical accent bar, and multi-line title/description on the right.
 */
@Composable
fun EditorialSectorBlock(
    sectorTitle: String,
    sectorSubTitle: String? = null,
    items: List<EditorialSectorItem>,
    lazyListState: LazyListState? = null,
    maxHeight: androidx.compose.ui.unit.Dp? = null,
    isManuscriptList: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isCollapsed by remember { mutableStateOf(false) }
    val haptics = rememberNaviromHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        EditorialSectorHeaderRow(
            sectorTitle = sectorTitle,
            sectorSubTitle = if (isCollapsed && isManuscriptList) "[ACTIVE LYRIC SHOWN]" else sectorSubTitle,
            isCollapsed = isCollapsed,
            onToggleCollapse = {
                haptics.click()
                isCollapsed = !isCollapsed
            }
        )

        if (isCollapsed) {
            if (isManuscriptList) {
                val activeOrFirst = items.firstOrNull { it.isSelected } ?: items.firstOrNull()
                if (activeOrFirst != null) {
                    EditorialSectorItemRow(item = activeOrFirst)
                }
            }
        } else {
            items.forEach { item ->
                EditorialSectorItemRow(item = item)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun EditorialSectorItemRow(
    item: EditorialSectorItem,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val isSelected = item.isSelected
    val bgShape = if (isSelected) {
        if (isDark) Color.White else Color.Black
    } else {
        Color.Transparent
    }
    val contentColor = if (isSelected) {
        if (isDark) Color.Black else Color.White
    } else {
        if (isDark) Color.White else Color.Black
    }
    val descColor = if (isSelected) {
        if (isDark) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
    } else {
        if (isDark) Color(0xFFAAAAAF) else Color(0xFF555555)
    }
    val tickColor = if (isSelected) {
        if (isDark) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
    } else {
        if (isDark) Color(0xFF444450) else Color(0xFFCCCCCC)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier
                        .background(bgShape, RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                } else {
                    Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                }
            )
            .then(
                if (item.onClick != null) {
                    Modifier.clickable { item.onClick.invoke() }
                } else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Track Number with shelf tick (only rendered if indexLabel is not blank)
        if (item.indexLabel.isNotBlank()) {
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = item.indexLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = contentColor
                )
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(1.dp)
                        .background(tickColor)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))
        }

        // Signature 4.5dp Solid Vertical Accent Bar
        Box(
            modifier = Modifier
                .width(4.5.dp)
                .height(36.dp)
                .background(contentColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Right Column: Title + Descriptive Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    fontSize = 13.sp
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.description.isNotBlank()) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    ),
                    color = descColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
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
