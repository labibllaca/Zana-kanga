package com.labix.navirom.ui.radial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.StrokeCap
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

enum class AstrolabeSearchFilter {
    ALL,
    SONGS,
    ALBUMS,
    ARTISTS
}

/**
 * Astrolabe Concentric Radial Search Screen with 360° A–Z Alphabet Scrubber Ring.
 */
@Composable
fun AstrolabeSearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: Triple<List<NaviromArtist>, List<NaviromAlbum>, List<NaviromTrack>>,
    allSongs: List<NaviromTrack> = emptyList(),
    allAlbums: List<NaviromAlbum> = emptyList(),
    allArtists: List<NaviromArtist> = emptyList(),
    appLanguage: AppLanguage,
    onTrackClick: (NaviromTrack, List<NaviromTrack>) -> Unit,
    onSelectAlbum: (String) -> Unit,
    onToggleClassicView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = rememberNaviromHaptics()
    val scope = rememberCoroutineScope()

    val alphabet = ('A'..'Z').toList()
    val alphabetStep = 360f / alphabet.size

    var selectedLetter by remember { mutableStateOf('A') }
    var activeFilter by remember { mutableStateOf(AstrolabeSearchFilter.ALL) }

    val alphabetDialState = rememberRadialDialState(
        initialAngle = 0f,
        stepSizeDegrees = alphabetStep
    )

    // Filter items based on active letter or query
    val displayedTracks = remember(query, selectedLetter, activeFilter, allSongs, searchResults.third) {
        if (query.isNotBlank()) {
            searchResults.third
        } else {
            allSongs.filter { it.title.startsWith(selectedLetter.toString(), ignoreCase = true) }
        }
    }

    val displayedAlbums = remember(query, selectedLetter, activeFilter, allAlbums, searchResults.second) {
        if (query.isNotBlank()) {
            searchResults.second
        } else {
            allAlbums.filter { it.name.startsWith(selectedLetter.toString(), ignoreCase = true) }
        }
    }

    val displayedArtists = remember(query, selectedLetter, activeFilter, allArtists, searchResults.first) {
        if (query.isNotBlank()) {
            searchResults.first
        } else {
            allArtists.filter { it.name.startsWith(selectedLetter.toString(), ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AstrolabeTheme.DeepSpaceMidnight)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("astrolabe_search_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "A–Z ASTROLABE SCRUBBER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = AstrolabeTheme.ParchmentRim
                )
                Text(
                    text = "ROTATIONAL ALPHABET NAVIGATION",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
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
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Classic", style = MaterialTheme.typography.labelMedium)
            }
        }

        // Search Input Field
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search by letter or keywords...", color = AstrolabeTheme.ParchmentRim.copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = AstrolabeTheme.AstrolabeGold) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = AstrolabeTheme.ParchmentRim)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AstrolabeTheme.AstrolabeGold,
                unfocusedBorderColor = AstrolabeTheme.CelestialGrid,
                focusedTextColor = AstrolabeTheme.ParchmentRim,
                unfocusedTextColor = AstrolabeTheme.ParchmentRim
            )
        )

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AstrolabeSearchFilter.entries.forEach { filter ->
                val isSelected = activeFilter == filter
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) AstrolabeTheme.AstrolabeGold else AstrolabeTheme.EngravedSlate.copy(alpha = 0.6f),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            haptics.click()
                            activeFilter = filter
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = if (isSelected) AstrolabeTheme.DeepSpaceMidnight else AstrolabeTheme.ParchmentRim
                        )
                    }
                }
            }
        }

        // Concentric A–Z Alphabet Dial Area
        Box(
            modifier = Modifier
                .height(180.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            var centerOffset by remember { mutableStateOf(Offset.Zero) }

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val dialSize = minOf(maxWidth, maxHeight)
                centerOffset = Offset(dialSize.value * 1.2f, dialSize.value * 1.2f)

                Canvas(
                    modifier = Modifier
                        .size(dialSize)
                        .radialRotatable(
                            state = alphabetDialState,
                            scope = scope,
                            centerProvider = { Offset(dialSize.value / 2f, dialSize.value / 2f) },
                            haptics = haptics,
                            onDragEnd = { finalAngle ->
                                val norm = RadialMath.normalizeDegrees(-finalAngle)
                                val letterIdx = ((norm / alphabetStep).toInt()).coerceIn(alphabet.indices)
                                selectedLetter = alphabet[letterIdx]
                            }
                        )
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val outerR = size.width * 0.46f
                    val innerR = size.width * 0.28f

                    // Bezel
                    drawAstrolabeBezel(center = center, outerRadius = outerR, innerRadius = innerR)

                    // 12 o'clock needle marker
                    val topNeedle = RadialMath.toCartesian(center, outerR + 5f, -90f)
                    val bottomNeedle = RadialMath.toCartesian(center, innerR - 2f, -90f)
                    drawLine(
                        color = AstrolabeTheme.ActivePointerGlow,
                        start = topNeedle,
                        end = bottomNeedle,
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Draw center core
                    drawCelestialCore(
                        center = center,
                        radius = innerR,
                        pulseEnergy = 1.0f,
                        rotationDeg = 0f,
                        isPlaying = false
                    )
                }

                // Center Display: Large Illuminated Active Letter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        // Advance to next letter on tap
                        haptics.tick()
                        val nextIdx = (alphabet.indexOf(selectedLetter) + 1) % alphabet.size
                        selectedLetter = alphabet[nextIdx]
                        scope.launch {
                            alphabetDialState.rotateBy(-alphabetStep)
                        }
                    }
                ) {
                    Text(
                        text = selectedLetter.toString(),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = AstrolabeTheme.AstrolabeGold
                    )
                    Text(
                        text = "SCRUB",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontSize = 9.sp),
                        color = AstrolabeTheme.StarCyan.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Results Section
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AstrolabeTheme.EngravedSlate.copy(alpha = 0.4f),
            border = androidx.compose.foundation.BorderStroke(1.dp, AstrolabeTheme.CelestialGrid),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Artists section if filter allows
                if (activeFilter == AstrolabeSearchFilter.ALL || activeFilter == AstrolabeSearchFilter.ARTISTS) {
                    if (displayedArtists.isNotEmpty()) {
                        item {
                            Text(
                                text = "ARTISTS (${displayedArtists.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = AstrolabeTheme.AstrolabeGold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(displayedArtists) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = AstrolabeTheme.StarCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AstrolabeTheme.ParchmentRim
                                )
                            }
                        }
                    }
                }

                // Albums section if filter allows
                if (activeFilter == AstrolabeSearchFilter.ALL || activeFilter == AstrolabeSearchFilter.ALBUMS) {
                    if (displayedAlbums.isNotEmpty()) {
                        item {
                            Text(
                                text = "ALBUMS (${displayedAlbums.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = AstrolabeTheme.AstrolabeGold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(displayedAlbums) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptics.click()
                                        onSelectAlbum(album.id)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Album, contentDescription = null, tint = AstrolabeTheme.StarCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AstrolabeTheme.ParchmentRim
                                    )
                                    Text(
                                        text = album.artist,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AstrolabeTheme.ParchmentRim.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tracks section
                if (activeFilter == AstrolabeSearchFilter.ALL || activeFilter == AstrolabeSearchFilter.SONGS) {
                    if (displayedTracks.isNotEmpty()) {
                        item {
                            Text(
                                text = "TRACKS (${displayedTracks.size})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                color = AstrolabeTheme.AstrolabeGold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(displayedTracks) { track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        haptics.click()
                                        onTrackClick(track, displayedTracks)
                                    }
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = AstrolabeTheme.StarCyan, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = AstrolabeTheme.ParchmentRim,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AstrolabeTheme.ParchmentRim.copy(alpha = 0.6f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Text(
                                    text = formatDuration(track.durationSeconds),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = AstrolabeTheme.ParchmentRim.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                if (displayedTracks.isEmpty() && displayedAlbums.isEmpty() && displayedArtists.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Rotate the alphabet scrubber to explore",
                                style = MaterialTheme.typography.bodySmall,
                                color = AstrolabeTheme.ParchmentRim.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(secs: Int): String {
    val m = secs / 60
    val s = secs % 60
    return String.format("%02d:%02d", m, s)
}
