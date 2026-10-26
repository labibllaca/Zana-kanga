package com.labix.navirom.ui.radial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.labix.navirom.data.model.NaviromAlbum
import com.labix.navirom.data.model.NaviromArtist
import com.labix.navirom.data.model.NaviromPlaylist
import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.util.rememberNaviromHaptics
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class LibraryFilterType {
    ALL, PLAYLISTS, ALBUMS, ARTISTS, SONGS
}

data class EditorialListItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: LibraryFilterType,
    val trackCount: Int,
    val rawAlbum: NaviromAlbum? = null,
    val rawPlaylist: NaviromPlaylist? = null,
    val rawArtist: NaviromArtist? = null,
    val rawSongsBatch: List<NaviromTrack>? = null,
    val isLocal: Boolean = false
)

/**
 * Astrolabe Library View featuring a half vertical wheel on the left side
 * and an unobstructed, prominent list browser on the right.
 * Long playlist and album names are fully visible and comfortably browsable on phones.
 */
@Composable
fun AstrolabeLibraryView(
    artists: List<NaviromArtist>,
    albums: List<NaviromAlbum>,
    librarySongs: List<NaviromTrack>,
    playlists: List<NaviromPlaylist> = emptyList(),
    appLanguage: AppLanguage,
    onSelectAlbum: (String) -> Unit,
    onSelectPlaylist: ((String) -> Unit)? = null,
    onTrackClick: (NaviromTrack, List<NaviromTrack>) -> Unit,
    onPlayAll: (List<NaviromTrack>) -> Unit,
    onToggleClassicView: () -> Unit = {},
    userName: String = "",
    modifier: Modifier = Modifier
) {
    val haptics = rememberNaviromHaptics()
    val scope = rememberCoroutineScope()

    var activeFilter by remember { mutableStateOf(LibraryFilterType.ALL) }
    var activeListIndex by remember { mutableIntStateOf(0) }
    val lazyListState = rememberLazyListState()

    // Unique songs across all lists (no duplicate elements)
    val uniqueSongs = remember(librarySongs) { librarySongs.distinctBy { it.id } }
    var visibleSongCount by remember(activeFilter) { mutableIntStateOf(25) }

    val songBatchItems = remember(uniqueSongs) {
        uniqueSongs.chunked(25).mapIndexed { batchIdx, batch ->
            val startNum = batchIdx * 25 + 1
            val endNum = startNum + batch.size - 1
            EditorialListItem(
                id = "song_batch_$batchIdx",
                title = "SONGS BATCH ${batchIdx + 1}",
                subtitle = "TRACKS $startNum–$endNum • ${batch.size} MANUSCRIPTS",
                type = LibraryFilterType.SONGS,
                trackCount = batch.size,
                rawSongsBatch = batch,
                isLocal = false
            )
        }
    }

    // Build unified editorial list items with Server vs Local distinction
    val allItems = remember(playlists, albums, artists, librarySongs) {
        val list = mutableListOf<EditorialListItem>()

        // 1. Playlists first
        playlists.forEach { pl ->
            val durationText = if (pl.durationSeconds > 0) " • ${formatSecs(pl.durationSeconds)}" else ""
            val sourceTag = if (pl.isLocal) "[LOCAL] " else "[SERVER] "
            list.add(
                EditorialListItem(
                    id = "pl_${pl.id}",
                    title = pl.name.ifBlank { "Untitled Playlist" },
                    subtitle = "$sourceTag PLAYLIST • ${pl.songCount} TRACKS$durationText",
                    type = LibraryFilterType.PLAYLISTS,
                    trackCount = pl.songCount,
                    rawPlaylist = pl,
                    isLocal = pl.isLocal
                )
            )
        }

        // 2. Albums
        albums.forEach { alb ->
            val albTracks = librarySongs.filter { it.albumId == alb.id || it.album.equals(alb.name, ignoreCase = true) }
            val trackCount = if (albTracks.isNotEmpty()) albTracks.size else alb.songCount
            val isLocalAlb = alb.id.startsWith("local_") || (albTracks.isNotEmpty() && albTracks.any { it.id.startsWith("local_") || !it.localFilePath.isNullOrBlank() })
            val sourceTag = if (isLocalAlb) "[LOCAL] " else "[SERVER] "
            list.add(
                EditorialListItem(
                    id = "alb_${alb.id}",
                    title = alb.name.ifBlank { "Unknown Album" },
                    subtitle = "$sourceTag ALBUM • ${alb.artist} • $trackCount TRACKS",
                    type = LibraryFilterType.ALBUMS,
                    trackCount = trackCount,
                    rawAlbum = alb,
                    isLocal = isLocalAlb
                )
            )
        }

        // 3. Artists
        artists.forEach { art ->
            val artTracks = librarySongs.filter { it.artist.equals(art.name, ignoreCase = true) }
            val albCount = albums.count { it.artist.equals(art.name, ignoreCase = true) }
            val isLocalArt = art.id.startsWith("local_") || (artTracks.isNotEmpty() && artTracks.any { it.id.startsWith("local_") || !it.localFilePath.isNullOrBlank() })
            val sourceTag = if (isLocalArt) "[LOCAL] " else "[SERVER] "
            list.add(
                EditorialListItem(
                    id = "art_${art.id}",
                    title = art.name.ifBlank { "Unknown Artist" },
                    subtitle = "$sourceTag ARTIST • $albCount ALBUMS RECORDED",
                    type = LibraryFilterType.ARTISTS,
                    trackCount = albCount,
                    rawArtist = art,
                    isLocal = isLocalArt
                )
            )
        }

        list
    }

    // Filtered lists
    val displayLists = remember(allItems, songBatchItems, activeFilter) {
        when (activeFilter) {
            LibraryFilterType.ALL -> allItems
            LibraryFilterType.PLAYLISTS -> allItems.filter { it.type == LibraryFilterType.PLAYLISTS }
            LibraryFilterType.ALBUMS -> allItems.filter { it.type == LibraryFilterType.ALBUMS }
            LibraryFilterType.ARTISTS -> allItems.filter { it.type == LibraryFilterType.ARTISTS }
            LibraryFilterType.SONGS -> songBatchItems
        }
    }

    val displayedSongs = remember(uniqueSongs, visibleSongCount) {
        uniqueSongs.take(visibleSongCount)
    }

    // Ensure active index stays within bounds
    LaunchedEffect(displayLists.size) {
        if (displayLists.isNotEmpty()) {
            activeListIndex = activeListIndex.coerceIn(0, displayLists.size - 1)
        } else {
            activeListIndex = 0
        }
    }

    // Left Half-Wheel state
    val safeItemCount = displayLists.size.coerceAtLeast(1)
    val angularStep = 180f / safeItemCount.coerceIn(4, 24)
    var wheelRotationDeg by remember { mutableFloatStateOf(0f) }

    // Synchronize wheel rotation when index changes
    LaunchedEffect(activeListIndex) {
        if (displayLists.isNotEmpty() && activeListIndex in displayLists.indices) {
            lazyListState.animateScrollToItem(activeListIndex)
        }
    }

    val currentSelectedItem = displayLists.getOrNull(activeListIndex)

    // Resolved tracks for the selected list
    val selectedTracks = remember(currentSelectedItem, librarySongs) {
        when (currentSelectedItem?.type) {
            LibraryFilterType.ALBUMS -> {
                val albId = currentSelectedItem.rawAlbum?.id
                val albName = currentSelectedItem.rawAlbum?.name
                librarySongs.filter { it.albumId == albId || it.album.equals(albName, ignoreCase = true) }
            }
            LibraryFilterType.ARTISTS -> {
                val artName = currentSelectedItem.rawArtist?.name
                librarySongs.filter { it.artist.equals(artName, ignoreCase = true) }
            }
            else -> {
                // For playlists, match or fallback to songs
                librarySongs.take(currentSelectedItem?.trackCount?.coerceAtLeast(1) ?: 10)
            }
        }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val canvasBg = MaterialTheme.colorScheme.background
    val inkColor = if (isDark) Color.White else Color.Black
    val secondaryInk = if (isDark) Color(0xFFAAAAAF) else Color(0xFF666666)
    val borderDividerColor = if (isDark) Color.White.copy(alpha = 0.25f) else Color.Black

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .statusBarsPadding()
            .testTag("astrolabe_library_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Header Bar: Greeting & Collections Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (userName.isNotBlank()) "WELCOME, ${userName.uppercase()}" else "ASTROLABE LIBRARY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 15.sp
                        ),
                        color = inkColor
                    )
                    Text(
                        text = "${displayLists.size} COLLECTIONS • HALF-WHEEL RADIAL SELECTION",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 1.sp,
                            fontSize = 9.sp
                        ),
                        color = secondaryInk
                    )
                }

                if (selectedTracks.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = {
                            haptics.toggle()
                            onPlayAll(selectedTracks)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isDark) Color.White else Color.Black,
                            contentColor = if (isDark) Color.Black else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "Play", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PLAY LIST",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            // Filter Chips Row (ALL | PLAYLISTS | ALBUMS | ARTISTS)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    LibraryFilterType.ALL to "ALL (${allItems.size})",
                    LibraryFilterType.PLAYLISTS to "PLAYLISTS (${playlists.size})",
                    LibraryFilterType.ALBUMS to "ALBUMS (${albums.size})",
                    LibraryFilterType.ARTISTS to "ARTISTS (${artists.size})",
                    LibraryFilterType.SONGS to "SONGS (${uniqueSongs.size})"
                ).forEach { (filterType, label) ->
                    val isSelected = activeFilter == filterType
                    val chipBg = if (isSelected) {
                        if (isDark) Color.White else Color.Black
                    } else {
                        if (isDark) Color(0xFF1C1C22) else Color.White
                    }
                    val chipContent = if (isSelected) {
                        if (isDark) Color.Black else Color.White
                    } else {
                        if (isDark) Color.White else Color.Black
                    }
                    val chipBorder = if (isSelected) {
                        if (isDark) Color.White else Color.Black
                    } else {
                        if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = chipBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                        modifier = Modifier
                            .clickable {
                                haptics.click()
                                activeFilter = filterType
                                activeListIndex = 0
                                wheelRotationDeg = 0f
                            }
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                fontSize = 9.sp
                            ),
                            color = chipContent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Divider bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(borderDividerColor)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Main Content Area:
            // LEFT SIDE: Half Vertical Wheel anchored to the left border
            // RIGHT SIDE: Fully Visible Lists Browser
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ==========================================
                // 1. LEFT HALF VERTICAL WHEEL
                // ==========================================
                val halfWheelWidth = 115.dp

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(halfWheelWidth)
                        .background(canvasBg)
                        .pointerInput(displayLists.size) {
                            var accumulatedDrag = 0f
                            detectVerticalDragGestures(
                                onDragStart = {
                                    accumulatedDrag = 0f
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDrag += dragAmount

                                    // Rotate wheel by drag
                                    wheelRotationDeg += dragAmount * 0.45f

                                    // Check threshold for index change
                                    val threshold = 28f
                                    if (abs(accumulatedDrag) >= threshold && displayLists.isNotEmpty()) {
                                        val steps = (accumulatedDrag / threshold).toInt()
                                        if (steps != 0) {
                                            haptics.tick()
                                            val newIdx = (activeListIndex - steps).coerceIn(0, displayLists.size - 1)
                                            if (newIdx != activeListIndex) {
                                                activeListIndex = newIdx
                                                accumulatedDrag = 0f
                                            }
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerY = size.height / 2f
                            val center = Offset(0f, centerY)
                            val outerRadPx = minOf(size.width * 0.95f, size.height * 0.46f)

                            drawEditorialLeftHalfWheel(
                                center = center,
                                outerRadius = outerRadPx,
                                rotationDeg = wheelRotationDeg,
                                listCount = displayLists.size.coerceAtLeast(4),
                                activeListIndex = activeListIndex,
                                isDark = isDark
                            )
                        }

                        // Center index tag at equator
                        Surface(
                            shape = RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp),
                            color = if (isDark) Color.White else Color.Black,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .offset(x = 4.dp)
                        ) {
                            Text(
                                text = " ${(activeListIndex + 1).toString().padStart(2, '0')} ",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = if (isDark) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Fine vertical divider separating half-wheel and lists
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.width(6.dp))

                // ==========================================
                // 2. RIGHT SIDE: PROMINENT FULLY VISIBLE LISTS / SONGS
                // ==========================================
                if (activeFilter == LibraryFilterType.SONGS) {
                    if (uniqueSongs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No songs cataloged in library",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 12.sp
                                ),
                                color = secondaryInk
                            )
                        }
                    } else {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(top = 6.dp, start = 2.dp, end = 2.dp, bottom = 80.dp)
                        ) {
                            itemsIndexed(displayedSongs) { idx, track ->
                                EditorialSongItemRow(
                                    track = track,
                                    indexNumber = (idx + 1).toString().padStart(2, '0'),
                                    isDark = isDark,
                                    onTrackClick = {
                                        haptics.click()
                                        onTrackClick(track, uniqueSongs)
                                    }
                                )
                            }

                            if (visibleSongCount < uniqueSongs.size) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                haptics.click()
                                                visibleSongCount = (visibleSongCount + 25).coerceAtMost(uniqueSongs.size)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, inkColor)
                                        ) {
                                            Text(
                                                text = "LOAD NEXT 25 MANUSCRIPTS (${visibleSongCount} / ${uniqueSongs.size})",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = inkColor
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.fillParentMaxHeight(0.5f))
                            }
                        }
                    }
                } else if (displayLists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No lists registered in this filter",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = 12.sp
                            ),
                            color = secondaryInk
                        )
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(top = 6.dp, start = 2.dp, end = 2.dp, bottom = 80.dp)
                    ) {
                        itemsIndexed(displayLists) { idx, item ->
                            val isSelected = (idx == activeListIndex)

                            EditorialListItemRow(
                                item = item,
                                indexNumber = (idx + 1).toString().padStart(2, '0'),
                                isSelected = isSelected,
                                isDark = isDark,
                                onSelect = {
                                    haptics.click()
                                    activeListIndex = idx
                                    wheelRotationDeg = -(idx * angularStep)
                                    if (item.rawPlaylist != null && onSelectPlaylist != null) {
                                        onSelectPlaylist(item.rawPlaylist.id)
                                    } else if (item.rawAlbum != null) {
                                        onSelectAlbum(item.rawAlbum.id)
                                    }
                                },
                                onPlay = {
                                    haptics.toggle()
                                    activeListIndex = idx
                                    if (selectedTracks.isNotEmpty()) {
                                        onPlayAll(selectedTracks)
                                    }
                                }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.fillParentMaxHeight(0.5f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialListItemRow(
    item: EditorialListItem,
    indexNumber: String,
    isSelected: Boolean,
    isDark: Boolean = false,
    onSelect: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerBg = if (isSelected) {
        if (isDark) Color.White else Color.Black
    } else {
        if (isDark) Color.Black else Color.White
    }
    val contentColor = if (isSelected) {
        if (isDark) Color.Black else Color.White
    } else {
        if (isDark) Color.White else Color.Black
    }
    val subtitleColor = if (isSelected) {
        if (isDark) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
    } else {
        if (isDark) Color(0xFFDDDDDD) else Color(0xFF555555)
    }
    val borderColor = if (isSelected) {
        if (isDark) Color.White else Color.Black
    } else {
        if (isDark) Color.White else Color.Black
    }
    val tickColor = if (isSelected) {
        if (isDark) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.6f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFFCCCCCC)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerBg,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        shadowElevation = if (isSelected) 3.dp else 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Index with shelf tick
            Column(
                modifier = Modifier.width(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = indexNumber,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = contentColor
                )
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.dp)
                        .background(tickColor)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Signature 4.5dp Solid Vertical Bar
            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .height(34.dp)
                    .background(contentColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Title & Subtitle with full visibility
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        fontSize = 13.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    ),
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Play Icon Button
            IconButton(
                onClick = onPlay,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play List",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatSecs(secs: Int): String {
    val h = secs / 3600
    val m = (secs % 3600) / 60
    val s = secs % 60
    return if (h > 0) String.format("%dh %02dm", h, m) else String.format("%02d:%02d", m, s)
}

@Composable
private fun EditorialSongItemRow(
    track: NaviromTrack,
    indexNumber: String,
    isDark: Boolean = false,
    onTrackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocalTrack = track.id.startsWith("local_") || !track.localFilePath.isNullOrBlank() || track.streamUrl.startsWith("content://") || track.streamUrl.startsWith("file://")
    val sourceTag = if (isLocalTrack) "[LOCAL] " else "[SERVER] "

    val containerBg = if (isDark) Color.Black else Color.White
    val contentColor = if (isDark) Color.White else Color.Black
    val subtitleColor = if (isDark) Color(0xFFDDDDDD) else Color(0xFF555555)
    val borderColor = if (isDark) Color.White else Color.Black
    val tickColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFFCCCCCC)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerBg,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTrackClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = indexNumber,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = contentColor
                )
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(1.dp)
                        .background(tickColor)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .height(34.dp)
                    .background(contentColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title.ifBlank { "Untitled Manuscript" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                        fontSize = 13.5.sp,
                        lineHeight = 17.sp
                    ),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$sourceTag${track.artist} • ${formatDurationSecs(track.durationSeconds)} • ${track.album.ifBlank { "Celestial Archive" }}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    ),
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onTrackClick,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play Track",
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatDurationSecs(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return String.format("%02d:%02d", m, s)
}
