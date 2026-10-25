package com.labix.navirom.ui.radial

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.labix.navirom.data.lyrics.LyricsData
import com.labix.navirom.data.lyrics.LyricLine
import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.data.model.PlaybackState
import com.labix.navirom.data.model.RepeatMode
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.util.rememberNaviromHaptics
import kotlinx.coroutines.launch

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

/**
 * Astrolabe Orbital Full Player.
 * Restructured to faithfully recreate the calendar/wheel design of zoom-kal.jpg:
 * - Concentric wheel at top with starry night center, track grooves, album ticks, list arcs,
 *   and radiating stepped arcs, shelf rays, and constellation stars around the wheel.
 * - Symmetrical editorial sectors arranged directly around and below the wheel with numbers,
 *   solid 4.5dp black vertical bar, serif typography, full queue list, and lyrics below.
 * - Docked playback controls bar with stark monochrome borders.
 */
@Composable
fun AstrolabeFullPlayer(
    playbackState: PlaybackState,
    isFavorite: Boolean,
    queue: List<NaviromTrack>,
    queueIndex: Int,
    appLanguage: AppLanguage,
    lyricsData: LyricsData? = null,
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

    // Track Lathe Dial State
    val trackCount = queue.size.coerceAtLeast(1)
    val stepAngle = 360f / trackCount.coerceAtLeast(4)
    val latheState = rememberRadialDialState(
        initialAngle = 0f,
        stepSizeDegrees = stepAngle
    )

    // Sync lathe rotation when queue index changes
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

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("astrolabe_full_player")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        tint = Color.Black
                    )
                }

                // Mode Indicator with Switcher Button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
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
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CONCENTRIC WHEEL",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = Color.Black
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
                        tint = if (isFavorite) Color.Black else Color.Black.copy(alpha = 0.5f)
                    )
                }
            }

            // Scrollable Content Area: Wheel and the radiating editorial sectors
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Current Track Header Info (Roman Numeral / Title / Artist)
                val romanIndex = getRomanNumeral((queueIndex + 1).coerceAtLeast(1))
                Text(
                    text = "$romanIndex • ${currentTrack?.title ?: "Select a Track"}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${currentTrack?.artist ?: "Unknown Artist"} • ${currentTrack?.album ?: "Unknown Album"}",
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.5.sp),
                    color = Color(0xFF444444),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // Architectural Ring Hierarchy Guide
                Spacer(modifier = Modifier.height(4.dp))
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
                        color = Color.Black
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
                        color = Color.Black
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

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // THE CONCENTRIC WHEEL CANVAS
                // (With radiating stepped arcs, shelf rays & constellation stars around the perimeter)
                // ==========================================
                val wheelSize = 270.dp
                Box(
                    modifier = Modifier
                        .size(wheelSize)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    var centerOffset by remember { mutableStateOf(Offset.Zero) }

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .radialRotatable(
                                state = latheState,
                                scope = scope,
                                centerProvider = { centerOffset },
                                haptics = haptics,
                                onDragEnd = { finalAngle ->
                                    val normalizedAngle = RadialMath.normalizeDegrees(-finalAngle)
                                    val selectedIdx = (normalizedAngle / stepAngle).toInt() % trackCount
                                    if (selectedIdx in queue.indices && selectedIdx != queueIndex) {
                                        onTrackSelect(queue[selectedIdx], queue)
                                    }
                                }
                            )
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        centerOffset = center
                        val outerR = size.width * 0.48f

                        drawEditorialConcentricWheel(
                            center = center,
                            outerRadius = outerR,
                            progressFraction = progressFraction,
                            trackCount = queue.size.coerceAtLeast(1),
                            currentTrackIndex = queueIndex,
                            listCount = 5,
                            activeListIndex = 0,
                            rotationDeg = latheState.angle
                        )
                    }

                    // Center Celestial / Album Art Medallion
                    Box(
                        modifier = Modifier
                            .size(wheelSize * 0.20f)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.Black, CircleShape)
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
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Center Play/Pause indicator overlay
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

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // REAL-TIME ROTATIONAL TARGET TELEMETRY BANNER
                // (Direct visual feedback of what changes next as the wheel is turned)
                // ==========================================
                val targetedIndex = remember(latheState.angle, trackCount) {
                    val normalizedAngle = RadialMath.normalizeDegrees(-latheState.angle)
                    val idx = (normalizedAngle / stepAngle).toInt() % trackCount
                    idx.coerceIn(0, (queue.size - 1).coerceAtLeast(0))
                }
                val targetedTrack = queue.getOrNull(targetedIndex)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (latheState.isDragging) Color.Black else Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.RotateRight,
                                contentDescription = null,
                                tint = if (latheState.isDragging) Color.White else Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = if (latheState.isDragging) "ROTATING DIAL • TARGET NEXT TRACK" else "DIAL SELECTION • CUED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        fontSize = 9.sp
                                    ),
                                    color = if (latheState.isDragging) Color.White.copy(alpha = 0.8f) else Color(0xFF666666)
                                )
                                Text(
                                    text = if (targetedTrack != null) {
                                        "${(targetedIndex + 1).toString().padStart(2, '0')}. ${targetedTrack.title} — ${targetedTrack.artist}"
                                    } else {
                                        "Turn concentric circle to change track"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (latheState.isDragging) Color.White else Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = "${RadialMath.normalizeDegrees(-latheState.angle).toInt()}°",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (latheState.isDragging) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ==========================================
                // EDITORIAL SECTOR 1: CURRENT PLAYING LIST / QUEUE
                // (Full playing list arranged with numbers, shelf ticks & 4.5dp black bar)
                // ==========================================
                val queueSectorItems = queue.mapIndexed { idx, track ->
                    val isCurrent = (idx == queueIndex || track.id == currentTrack?.id)
                    val isTargeted = (idx == targetedIndex && latheState.isDragging)
                    EditorialSectorItem(
                        indexLabel = (idx + 1).toString().padStart(2, '0'),
                        title = track.title + if (isCurrent && playbackState.isPlaying) "  ▶" else "",
                        description = "${track.artist} • ${formatMs(track.durationSeconds * 1000L)}" +
                            if (isTargeted) " • [TARGETED NEXT ON RELEASE]" else if (isCurrent) " • [CURRENTLY PLAYING]" else "",
                        isSelected = isCurrent || isTargeted,
                        onClick = {
                            haptics.click()
                            onTrackSelect(track, queue)
                        }
                    )
                }

                EditorialSectorBlock(
                    sectorTitle = "CURRENT PLAYING LIST",
                    sectorSubTitle = "${queue.size} TRACKS IN ORBIT • TAP OR TURN WHEEL TO SELECT",
                    items = if (queueSectorItems.isNotEmpty()) queueSectorItems else listOf(
                        EditorialSectorItem(
                            indexLabel = "01",
                            title = currentTrack?.title ?: "No track queued",
                            description = currentTrack?.artist ?: "Unknown artist"
                        )
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ==========================================
                // EDITORIAL SECTOR 2: LYRICS (LOWER BELOW THE QUEUE)
                // (Displays synchronized or plain lyrics with serif typography)
                // ==========================================
                val hasSyncedLyrics = lyricsData?.syncedLines?.isNotEmpty() == true
                val hasPlainLyrics = lyricsData?.plainLyrics?.isNotBlank() == true

                if (hasSyncedLyrics) {
                    val syncedLines = lyricsData?.syncedLines ?: emptyList()
                    val activeLyricIdx = remember(currentPosMs, syncedLines) {
                        syncedLines.indexOfLast { (it.timeMs - 350L) <= currentPosMs }
                    }

                    // Display synced lyrics formatted as editorial sector items
                    val lyricItems = syncedLines.mapIndexed { idx, line ->
                        val isLineActive = (idx == activeLyricIdx)
                        EditorialSectorItem(
                            indexLabel = formatMs(line.timeMs),
                            title = line.text.ifBlank { "• • •" },
                            description = if (isLineActive) "Active vocal phase • tap line to cue playback" else "Synchronized stanza line",
                            isSelected = isLineActive,
                            onClick = {
                                haptics.click()
                                onSeekTo(line.timeMs)
                            }
                        )
                    }

                    EditorialSectorBlock(
                        sectorTitle = "MANUSCRIPT LYRICS",
                        sectorSubTitle = "SYNCHRONIZED VOCAL TRANSCRIPT",
                        items = lyricItems
                    )
                } else if (hasPlainLyrics) {
                    val plainLines = lyricsData?.plainLyrics.orEmpty()
                        .lines()
                        .filter { it.isNotBlank() }
                        .take(40)

                    val plainItems = plainLines.mapIndexed { idx, line ->
                        EditorialSectorItem(
                            indexLabel = (idx + 1).toString().padStart(2, '0'),
                            title = line,
                            description = "Unsynchronized lyric verse",
                            isSelected = false
                        )
                    }

                    EditorialSectorBlock(
                        sectorTitle = "MANUSCRIPT LYRICS",
                        sectorSubTitle = "POETIC TEXT ARCHIVE",
                        items = plainItems
                    )
                } else {
                    EditorialSectorBlock(
                        sectorTitle = "MANUSCRIPT LYRICS",
                        sectorSubTitle = "ASTRONOMICAL POETRY NOT FOUND",
                        items = listOf(
                            EditorialSectorItem(
                                indexLabel = "—",
                                title = "Transcript Unavailable",
                                description = "No synchronized lyrics or text registered for ${currentTrack?.title ?: "this celestial piece"}."
                            )
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==========================================
                // EDITORIAL SECTOR 3: NOW PLAYING CONTEXT & MATRIX
                // ==========================================
                EditorialSectorBlock(
                    sectorTitle = "ASTRONOMICAL SPECTRUM",
                    sectorSubTitle = "LOSSLESS ACOUSTIC HARMONICS",
                    items = listOf(
                        EditorialSectorItem(
                            indexLabel = "21",
                            title = currentTrack?.title ?: "Select a Track",
                            description = "${currentTrack?.artist ?: "Unknown"} • ${currentTrack?.album ?: "Unknown Album"} • 24-bit / 96.0 kHz Master."
                        ),
                        EditorialSectorItem(
                            indexLabel = "22",
                            title = "Temporal Equinox",
                            description = "${formatMs(currentPosMs)} / ${formatMs(totalDurationMs)} • Balance across acoustic registers."
                        )
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ==========================================
            // DOCKED BOTTOM TIME & CONTROLS DOCK
            // ==========================================
            // Elapsed and Remaining Time Readouts
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
                    color = Color.Black
                )
                Text(
                    text = "${queueIndex + 1} / ${queue.size.coerceAtLeast(1)}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.Black
                )
                Text(
                    text = "-${formatMs(totalDurationMs - currentPosMs)}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Playback Controls Bar with Stark Black Border on White
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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
                            tint = if (playbackState.isShuffle) Color.Black else Color.Black.copy(alpha = 0.35f)
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
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Main Center Play/Pause Satellite (Solid Black circle with White Icon)
                    FilledIconButton(
                        onClick = {
                            haptics.toggle()
                            onTogglePlayPause()
                        },
                        modifier = Modifier.size(50.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(28.dp)
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
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Repeat Satellite
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
                            tint = if (isRepeat) Color.Black else Color.Black.copy(alpha = 0.35f)
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
                                tint = Color.Black
                            )
                        }
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
fun EditorialSectorBlock(
    sectorTitle: String,
    sectorSubTitle: String? = null,
    items: List<EditorialSectorItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Sector Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = sectorTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.5.sp,
                    fontSize = 15.sp
                ),
                color = Color.Black
            )
            if (sectorSubTitle != null) {
                Text(
                    text = sectorSubTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = Color(0xFF666666)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Horizontal sector separator line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .background(Color.Black)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Items in sector
        items.forEachIndexed { _, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (item.onClick != null) {
                            Modifier.clickable { item.onClick.invoke() }
                        } else Modifier
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: Date / Track Number with shelf tick
                Column(
                    modifier = Modifier.width(44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = item.indexLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp
                        ),
                        color = Color.Black
                    )
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(1.dp)
                            .background(Color(0xFFCCCCCC))
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Signature 4.5dp Solid Black Vertical Accent Bar (from zoom-kal.jpg)
                Box(
                    modifier = Modifier
                        .width(4.5.dp)
                        .height(36.dp)
                        .background(if (item.isSelected) Color.Black else Color.Black.copy(alpha = 0.85f))
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Right Column: Title + Descriptive Text
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            letterSpacing = 0.4.sp,
                            fontSize = 13.sp
                        ),
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        ),
                        color = Color(0xFF444444),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
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
