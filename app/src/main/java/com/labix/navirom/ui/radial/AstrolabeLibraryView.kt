package com.labix.navirom.ui.radial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.labix.navirom.data.model.NaviromAlbum
import com.labix.navirom.data.model.NaviromArtist
import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.NaviromStrings
import com.labix.navirom.ui.util.rememberNaviromHaptics
import kotlinx.coroutines.launch

/**
 * Astrolabe Concentric Radial Library Browser.
 * Features an outer rotary artist dial, middle album dial, and inner track lathe.
 */
@Composable
fun AstrolabeLibraryView(
    artists: List<NaviromArtist>,
    albums: List<NaviromAlbum>,
    librarySongs: List<NaviromTrack>,
    appLanguage: AppLanguage,
    onSelectAlbum: (String) -> Unit,
    onTrackClick: (NaviromTrack, List<NaviromTrack>) -> Unit,
    onPlayAll: (List<NaviromTrack>) -> Unit,
    onToggleClassicView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberNaviromHaptics()
    val scope = rememberCoroutineScope()

    var activeArtistIndex by remember { mutableIntStateOf(0) }
    val currentArtist = artists.getOrNull(activeArtistIndex)

    // Filter albums by current artist if available, otherwise display active albums
    val artistAlbums = remember(currentArtist, albums) {
        if (currentArtist != null && currentArtist.name.isNotBlank()) {
            val matched = albums.filter { it.artist.equals(currentArtist.name, ignoreCase = true) }
            if (matched.isNotEmpty()) matched else albums.take(8)
        } else {
            albums.take(8)
        }
    }

    var selectedAlbum by remember { mutableStateOf<NaviromAlbum?>(null) }
    LaunchedEffect(artistAlbums) {
        selectedAlbum = artistAlbums.firstOrNull()
    }

    val albumTracks = remember(selectedAlbum, librarySongs) {
        if (selectedAlbum != null) {
            val matched = librarySongs.filter { it.albumId == selectedAlbum?.id || it.album.equals(selectedAlbum?.name, ignoreCase = true) }
            if (matched.isNotEmpty()) matched else librarySongs.take(12)
        } else {
            librarySongs.take(12)
        }
    }

    // Artist Rotary Dial State
    val artistStep = if (artists.isNotEmpty()) 360f / artists.size.coerceAtMost(24) else 45f
    val artistDialState = rememberRadialDialState(
        initialAngle = 0f,
        stepSizeDegrees = artistStep
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("astrolabe_library_view")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header: Concentric Mode Indicator & Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PLANISPHERE LIBRARY",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        ),
                        color = Color.Black
                    )
                    Text(
                        text = "CONCENTRIC ARTIST & ALBUM SPHERES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp
                        ),
                        color = Color(0xFF444444)
                    )
                }

                FilledTonalButton(
                    onClick = {
                        haptics.click()
                        onToggleClassicView()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "Classic View",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Classic", style = MaterialTheme.typography.labelMedium, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Upper Section: Concentric Wheel (Track, Album, Lists - zero text in wheel)
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxWidth()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                var centerOffset by remember { mutableStateOf(Offset.Zero) }

                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val sizePx = minOf(maxWidth, maxHeight)
                    centerOffset = Offset(sizePx.value / 2f, sizePx.value / 2f)

                    // Canvas drawing concentric rings: innermost for track, middle for album, outer for lists
                    // Zero text inside the wheel
                    Canvas(
                        modifier = Modifier
                            .size(sizePx)
                            .radialRotatable(
                                state = artistDialState,
                                scope = scope,
                                centerProvider = { Offset(sizePx.value / 2f, sizePx.value / 2f) },
                                haptics = haptics,
                                onDragEnd = { angle ->
                                    if (artists.isNotEmpty()) {
                                        val norm = RadialMath.normalizeDegrees(-angle)
                                        val idx = (norm / artistStep).toInt() % artists.size
                                        activeArtistIndex = idx.coerceIn(artists.indices)
                                    }
                                }
                            )
                    ) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerR = size.width * 0.46f

                        drawEditorialConcentricWheel(
                            center = center,
                            outerRadius = outerR,
                            progressFraction = 0.5f,
                            trackCount = artistAlbums.size.coerceAtLeast(1),
                            currentTrackIndex = 0,
                            listCount = artists.size.coerceAtLeast(3),
                            activeListIndex = activeArtistIndex,
                            rotationDeg = artistDialState.angle
                        )
                    }

                    // Center Focus Display: Current Artist & Album (displayed cleanly outside/on the core)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(sizePx * 0.36f)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color.Black, CircleShape)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = currentArtist?.name ?: "All Music",
                            style = MaterialTheme.typography.titleSmall.copy(
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
                            text = "${artistAlbums.size} Albums",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF444444),
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Lower Section: Selected Artist's Album Carousel & Track Radiator
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.Black),
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Album selector row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedAlbum?.name ?: "Discography",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AstrolabeTheme.ParchmentRim,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (albumTracks.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    haptics.toggle()
                                    onPlayAll(albumTracks)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play Album",
                                    tint = AstrolabeTheme.AstrolabeGold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Track Lathe List radiating from selected album
                    if (albumTracks.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Rotate the sphere to discover tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = AstrolabeTheme.ParchmentRim.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(albumTracks) { index, track ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            haptics.click()
                                            onTrackClick(track, albumTracks)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = (index + 1).toString().padStart(2, '0'),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace
                                                ),
                                                color = AstrolabeTheme.StarCyan.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = track.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = AstrolabeTheme.ParchmentRim,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = formatSecs(track.durationSeconds),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = AstrolabeTheme.ParchmentRim.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSecs(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return String.format("%02d:%02d", m, s)
}
