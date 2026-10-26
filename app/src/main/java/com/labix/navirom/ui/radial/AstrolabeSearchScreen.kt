package com.labix.navirom.ui.radial

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
import androidx.compose.ui.graphics.luminance
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
 * Redesigned in editorial monochrome style matching zoom-kal.jpg:
 * Pure white background, solid black borders, serif typography, shelf ticks,
 * and 4.5dp solid black vertical accent bars.
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

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bgColor = if (isDark) Color(0xFF101014) else Color.White
    val surfaceColor = if (isDark) Color(0xFF18181D) else Color.White
    val contentColor = if (isDark) Color(0xFFF2F2F5) else Color.Black
    val borderColor = if (isDark) Color(0xFF404048) else Color.Black
    val subtextColor = if (isDark) Color(0xFFA0A0A8) else Color(0xFF666666)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
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
                    text = "ALPHABETIC ASTROLABE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = contentColor
                )
                Text(
                    text = "A–Z SCRUBBER & MANUSCRIPT SEARCH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    ),
                    color = subtextColor
                )
            }
        }

        // Search Input Field with solid border
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    "Search manuscripts or keywords...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif),
                    color = if (isDark) Color(0xFF707078) else Color(0xFF888888)
                )
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = contentColor) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = contentColor)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = contentColor,
                unfocusedBorderColor = borderColor,
                focusedTextColor = contentColor,
                unfocusedTextColor = contentColor,
                focusedContainerColor = surfaceColor,
                unfocusedContainerColor = surfaceColor
            )
        )

        // Filter Chips Row in monochrome serif style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AstrolabeSearchFilter.entries.forEach { filter ->
                val isSelected = activeFilter == filter
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) contentColor else surfaceColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
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
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                fontSize = 10.sp
                            ),
                            color = if (isSelected) (if (isDark) Color(0xFF101014) else Color.White) else contentColor
                        )
                    }
                }
            }
        }

        // Concentric A–Z Alphabet Dial Area
        Box(
            modifier = Modifier
                .height(170.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val dialSize = minOf(maxWidth, maxHeight)

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

                    // Draw concentric wheel rings with pure monochrome lines
                    drawEditorialConcentricWheel(
                        center = center,
                        outerRadius = outerR,
                        progressFraction = (alphabet.indexOf(selectedLetter).toFloat() / alphabet.size.toFloat()),
                        trackCount = alphabet.size,
                        currentTrackIndex = alphabet.indexOf(selectedLetter),
                        listCount = 4,
                        activeListIndex = 0,
                        rotationDeg = alphabetDialState.angle,
                        isDark = isDark
                    )
                }

                // Center Display: Large Illuminated Active Letter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .size(dialSize * 0.30f)
                        .clip(CircleShape)
                        .background(surfaceColor)
                        .border(1.5.dp, contentColor, CircleShape)
                        .clickable {
                            // Advance to next letter on tap
                            haptics.tick()
                            val nextIdx = (alphabet.indexOf(selectedLetter) + 1) % alphabet.size
                            selectedLetter = alphabet[nextIdx]
                            scope.launch {
                                alphabetDialState.rotateBy(-alphabetStep)
                            }
                        },
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = selectedLetter.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = contentColor
                    )
                    Text(
                        text = "SCRUB",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 1.sp,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = subtextColor
                    )
                }
            }
        }

        // Results Section formatted with editorial shelf ticks and 4.5dp accent bars
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = surfaceColor,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, top = 14.dp, end = 14.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Artists section if filter allows
                if (activeFilter == AstrolabeSearchFilter.ALL || activeFilter == AstrolabeSearchFilter.ARTISTS) {
                    if (displayedArtists.isNotEmpty()) {
                        item {
                            Text(
                                text = "ARTISTS • ${displayedArtists.size} CATALOGED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = contentColor,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(contentColor))
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        itemsIndexed(displayedArtists) { index, artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = (index + 1).toString().padStart(2, '0'),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                                        color = contentColor
                                    )
                                    Box(modifier = Modifier.width(22.dp).height(1.dp).background(borderColor))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.width(4.5.dp).height(32.dp).background(contentColor))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = artist.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
                                        color = contentColor
                                    )
                                    Text(
                                        text = "Cataloged Celestial Artist",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = subtextColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Albums section if filter allows
                if (activeFilter == AstrolabeSearchFilter.ALL || activeFilter == AstrolabeSearchFilter.ALBUMS) {
                    if (displayedAlbums.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ALBUMS • ${displayedAlbums.size} ARCHIVED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = contentColor,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(contentColor))
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        itemsIndexed(displayedAlbums) { index, album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.click()
                                        onSelectAlbum(album.id)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = (index + 1).toString().padStart(2, '0'),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                                        color = contentColor
                                    )
                                    Box(modifier = Modifier.width(22.dp).height(1.dp).background(borderColor))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.width(4.5.dp).height(32.dp).background(contentColor))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = album.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
                                        color = contentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = album.artist,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = subtextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "TRACKS • ${displayedTracks.size} MANUSCRIPTS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = contentColor,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(contentColor))
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        itemsIndexed(displayedTracks) { index, track ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.click()
                                        onTrackClick(track, displayedTracks)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.width(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = (index + 1).toString().padStart(2, '0'),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                                        color = contentColor
                                    )
                                    Box(modifier = Modifier.width(22.dp).height(1.dp).background(borderColor))
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(modifier = Modifier.width(4.5.dp).height(34.dp).background(contentColor))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = track.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
                                        color = contentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${track.artist} • ${formatDuration(track.durationSeconds)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = subtextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
                                text = "Rotate the alphabet scrubber to explore manuscripts",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Serif),
                                color = subtextColor
                            )
                        }
                    }
                }

                // Generous 50% vertical scroll space so bottom items can easily scroll up clear of MiniPlayer
                item {
                    Spacer(modifier = Modifier.fillParentMaxHeight(0.5f))
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
