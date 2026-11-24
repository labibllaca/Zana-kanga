package com.labix.navirom.ui.screens

import androidx.activity.compose.BackHandler
import java.util.Calendar
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.navigationBarsPadding
import kotlinx.coroutines.delay
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.labix.navirom.data.model.*
import com.labix.navirom.data.local.LocalMusicFolder
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.LibrarySubTab
import com.labix.navirom.ui.NaviromStrings
import com.labix.navirom.ui.SongSortOrder
import com.labix.navirom.ui.components.ActionButtonGroup
import com.labix.navirom.ui.components.AlbumCard
import com.labix.navirom.ui.components.DualAudioPlayButton
import com.labix.navirom.ui.components.ArtistCard
import com.labix.navirom.ui.components.SleekFeatureCard
import com.labix.navirom.ui.components.SongAlbumCover
import com.labix.navirom.ui.components.TrackListItem
import com.labix.navirom.ui.util.rememberNaviromHaptics
import com.labix.ui.theme.*

import com.labix.navirom.ui.components.ArtistDetailView
import com.labix.navirom.ui.components.CloudServerOnly
import com.labix.ui.theme.PoppinsFamily

/** Which titles the library shows: everything, local files only, or server files only. */
enum class LibrarySourceFilter {
    ALL,
    LOCAL,
    SERVER;

    fun next(): LibrarySourceFilter = when (this) {
        ALL -> LOCAL
        LOCAL -> SERVER
        SERVER -> ALL
    }

    /** [isLocal] is true when the title also exists in the scanned local files. */
    fun matches(isLocal: Boolean): Boolean = when (this) {
        ALL -> true
        LOCAL -> isLocal
        SERVER -> !isLocal
    }

    val labelKey: String
        get() = when (this) {
            ALL -> "all_titles_online_local"
            LOCAL -> "local_only_titles"
            SERVER -> "server_only_titles"
        }

    val icon: ImageVector
        get() = when (this) {
            ALL -> Icons.Filled.Cloud
            LOCAL -> Icons.Filled.CloudOff
            SERVER -> CloudServerOnly
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    subTab: LibrarySubTab,
    onSubTabSelected: (LibrarySubTab) -> Unit,
    albums: List<NaviromAlbum>,
    artists: List<NaviromArtist>,
    quickMixTracks: List<NaviromTrack>,
    recentlyPlayedTracks: List<NaviromTrack> = emptyList(),
    librarySongs: List<NaviromTrack> = emptyList(),
    songSortOrder: SongSortOrder = SongSortOrder.NAME,
    isSongSortReversed: Boolean = false,
    onSetSongSortOrder: (SongSortOrder) -> Unit = {},
    newestAlbums: List<NaviromAlbum> = emptyList(),
    mostPlayedAlbums: List<NaviromAlbum> = emptyList(),
    randomAlbums: List<NaviromAlbum> = emptyList(),
    onRefreshRandomAlbums: () -> Unit = {},
    profileName: String = "",
    newestTracks: List<NaviromTrack> = emptyList(),
    selectedAlbumId: String?,
    selectedAlbumTracks: List<NaviromTrack>,
    currentTrack: NaviromTrack?,
    isPlaying: Boolean,
    downloadStatuses: Map<String, DownloadStatus>,
    downloadProgresses: Map<String, Float>,
    favoriteIds: List<String>,
    favoriteTracks: List<NaviromTrack> = emptyList(),
    appLanguage: AppLanguage,
    musicFolders: List<com.labix.navirom.data.api.dto.MusicFolderDto> = emptyList(),
    selectedMusicFolderId: String? = null,
    selectedMusicFolderIds: Set<String> = emptySet(),
    onSelectMusicFolder: (String?) -> Unit = {},
    onToggleMusicFolder: (String) -> Unit = {},
    onSelectAllMusicFolders: () -> Unit = {},
    onSyncLibrary: () -> Unit = {},
    onSelectAlbum: (String?) -> Unit,
    onTrackClick: (NaviromTrack, List<NaviromTrack>) -> Unit,
    onPlayAll: (List<NaviromTrack>) -> Unit,
    onShuffleAll: (List<NaviromTrack>) -> Unit,
    onSmartShuffle: ((List<NaviromTrack>?) -> Unit)? = null,
    onPlayTracksNext: ((List<NaviromTrack>) -> Unit)? = null,
    onAddTracksToQueue: ((List<NaviromTrack>) -> Unit)? = null,
    onAddAlbumsToQueue: ((List<String>, (Int) -> Unit) -> Unit)? = null,
    onAddArtistsToQueue: ((List<String>, (Int) -> Unit) -> Unit)? = null,
    miniPlayerVisible: Boolean = false,
    onDownloadTracks: (List<NaviromTrack>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadTrack: (NaviromTrack) -> Unit,
    onPlayNext: (NaviromTrack) -> Unit,
    onAddToQueue: (NaviromTrack) -> Unit,
    onGoToSettings: () -> Unit = {},
    onScanNetwork: () -> Unit = {},
    onOpenSidebar: () -> Unit = {},
    selectedArtistId: String? = null,
    currentArtist: NaviromArtist? = null,
    currentArtistAlbums: List<NaviromAlbum> = emptyList(),
    currentArtistSongs: List<NaviromTrack> = emptyList(),
    isLoadingArtistDetails: Boolean = false,
    onSelectArtist: (String?) -> Unit = {},
    localTracks: List<NaviromTrack> = emptyList(),
    localFolders: List<LocalMusicFolder> = emptyList(),
    disabledLocalFolderIds: Set<String> = emptySet(),
    onToggleLocalFolder: (String) -> Unit = {},
    onSetLocalFolderEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onSelectAllLocalFolders: () -> Unit = {},
    onDeselectAllLocalFolders: () -> Unit = {},
    isScanningLocalAudio: Boolean = false,
    onScanLocalAudio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    fun str(key: String): String = NaviromStrings.get(key, appLanguage)
    val haptics = rememberNaviromHaptics()
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onScanLocalAudio()
        }
    }

    val requestLocalPermission = {
        val perm = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val hasPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            perm
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPerm) {
            onScanLocalAudio()
        } else {
            permissionLauncher.launch(perm)
        }
    }

    var showFolderSwitcherDialog by remember { mutableStateOf(false) }
    var librarySearchQuery by remember { mutableStateOf("") }
    var sourceFilter by remember { mutableStateOf(LibrarySourceFilter.ALL) }

    // Hoisted above the album/artist detail branches so the scroll position of the
    // library lists survives opening a detail view and coming back.
    val overviewListState = rememberLazyListState()
    val newestListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val artistsGridState = rememberLazyGridState()
    val songsListState = rememberLazyListState()

    // Long-press multi-select state. Albums, songs and artists have independent
    // selections; all are cleared when switching sub-tabs or on back.
    var isAlbumSelectionMode by remember { mutableStateOf(false) }
    var selectedAlbumIds by remember { mutableStateOf(emptySet<String>()) }
    var isSongSelectionMode by remember { mutableStateOf(false) }
    var selectedSongIds by remember { mutableStateOf(emptySet<String>()) }
    var isArtistSelectionMode by remember { mutableStateOf(false) }
    var selectedArtistIds by remember { mutableStateOf(emptySet<String>()) }

    // One-shot "wave" when selection starts: only the items that are on screen at
    // that moment are allowed to animate, capturing their keys once so scrolling
    // afterwards doesn't trigger late waves.
    var rippleTrigger by remember { mutableStateOf(0L) }
    var rippleOriginIndex by remember { mutableStateOf(-1) }
    var rippleVisibleIds by remember { mutableStateOf(emptySet<String>()) }

    fun resetRipple() {
        rippleTrigger = 0L
        rippleOriginIndex = -1
        rippleVisibleIds = emptySet()
    }

    fun clearArtistSelection() {
        isArtistSelectionMode = false
        selectedArtistIds = emptySet()
        resetRipple()
    }

    fun clearAlbumSelection() {
        isAlbumSelectionMode = false
        selectedAlbumIds = emptySet()
        resetRipple()
    }

    fun clearSongSelection() {
        isSongSelectionMode = false
        selectedSongIds = emptySet()
        resetRipple()
    }

    LaunchedEffect(rippleTrigger) {
        if (rippleTrigger != 0L) {
            delay(4400)
            resetRipple()
        }
    }

    LaunchedEffect(subTab) {
        clearAlbumSelection()
        clearSongSelection()
        clearArtistSelection()
    }

    BackHandler(enabled = isAlbumSelectionMode || isSongSelectionMode || isArtistSelectionMode) {
        clearAlbumSelection()
        clearSongSelection()
        clearArtistSelection()
    }

    // If an album is selected, show the Album Detail view (placed before artist detail view to allow navigating from artist -> album)
    if (selectedAlbumId != null) {
        val selectedAlbum = currentArtistAlbums.find { it.id == selectedAlbumId }
            ?: albums.find { it.id == selectedAlbumId }
            ?: newestAlbums.find { it.id == selectedAlbumId }
            ?: if (selectedAlbumTracks.isNotEmpty()) {
                NaviromAlbum(
                    id = selectedAlbumId,
                    name = selectedAlbumTracks.first().album,
                    artist = selectedAlbumTracks.first().artist,
                    artistId = selectedAlbumTracks.first().artistId,
                    coverArt = selectedAlbumTracks.first().coverArtId,
                    coverArtUrl = selectedAlbumTracks.first().coverArtUrl,
                    songCount = selectedAlbumTracks.size,
                    durationSeconds = selectedAlbumTracks.sumOf { it.durationSeconds }
                )
            } else null

        AlbumDetailView(
            album = selectedAlbum,
            tracks = selectedAlbumTracks,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            downloadStatuses = downloadStatuses,
            downloadProgresses = downloadProgresses,
            favoriteIds = favoriteIds,
            appLanguage = appLanguage,
            onBack = { onSelectAlbum(null) },
            onTrackClick = { track -> onTrackClick(track, selectedAlbumTracks) },
            onPlayAll = { onPlayAll(selectedAlbumTracks) },
            onShuffleAll = { onShuffleAll(selectedAlbumTracks) },
            onSmartShuffle = { if (onSmartShuffle != null) onSmartShuffle(selectedAlbumTracks) else onShuffleAll(selectedAlbumTracks) },
            onPlayTracksNext = {
                onPlayTracksNext?.invoke(selectedAlbumTracks)
                android.widget.Toast.makeText(context, String.format(str("toast_added_next"), selectedAlbumTracks.size), android.widget.Toast.LENGTH_SHORT).show()
            },
            onAddTracksToQueue = {
                onAddTracksToQueue?.invoke(selectedAlbumTracks)
                android.widget.Toast.makeText(context, String.format(str("toast_added_queue"), selectedAlbumTracks.size), android.widget.Toast.LENGTH_SHORT).show()
            },
            onDownloadAlbum = { onDownloadTracks(selectedAlbumTracks) },
            onToggleFavorite = onToggleFavorite,
            onDownloadTrack = onDownloadTrack,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            modifier = modifier
        )
        return
    }

    // If an artist is selected, show the Artist Detail view
    if (selectedArtistId != null) {
        ArtistDetailView(
            artist = currentArtist,
            albums = currentArtistAlbums,
            songs = currentArtistSongs,
            isLoading = isLoadingArtistDetails,
            currentTrack = currentTrack,
            isPlaying = isPlaying,
            downloadStatuses = downloadStatuses,
            downloadProgresses = downloadProgresses,
            favoriteIds = favoriteIds,
            appLanguage = appLanguage,
            onBack = { onSelectArtist(null) },
            onSelectAlbum = onSelectAlbum,
            onTrackClick = { track -> onTrackClick(track, currentArtistSongs) },
            onPlayAllSongs = { onPlayAll(currentArtistSongs) },
            onShuffleAllSongs = { onShuffleAll(currentArtistSongs) },
            onSmartShuffleSongs = {
                if (onSmartShuffle != null) {
                    onSmartShuffle(currentArtistSongs)
                } else {
                    onShuffleAll(currentArtistSongs)
                }
            },
            onPlayAllSongsNext = {
                onPlayTracksNext?.invoke(currentArtistSongs)
                android.widget.Toast.makeText(context, String.format(str("toast_added_next"), currentArtistSongs.size), android.widget.Toast.LENGTH_SHORT).show()
            },
            onAddAllSongsToQueue = {
                onAddTracksToQueue?.invoke(currentArtistSongs)
                android.widget.Toast.makeText(context, String.format(str("toast_added_queue"), currentArtistSongs.size), android.widget.Toast.LENGTH_SHORT).show()
            },
            onToggleFavorite = onToggleFavorite,
            onDownloadTrack = onDownloadTrack,
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            modifier = modifier
        )
        return
    }

    val hasLocalMusic = localTracks.isNotEmpty() || librarySongs.isNotEmpty() || localFolders.isNotEmpty()
    val hasServerMusic = albums.isNotEmpty() || artists.isNotEmpty() || quickMixTracks.isNotEmpty() || musicFolders.isNotEmpty()

    if (!hasLocalMusic && !hasServerMusic) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = str("empty_library_title"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = str("empty_library_desc"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onScanLocalAudio,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = str("scan_local_audio"), modifier = Modifier.size(24.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = onGoToSettings,
                            shape = CircleShape,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = str("btn_connect_server"), modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
        return
    }

    val searchQuery = librarySearchQuery.trim()
    val localAlbumNames = remember(localTracks) {
        localTracks.map { it.album.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }
    val localArtistNames = remember(localTracks) {
        localTracks.map { it.artist.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }
    val localTrackIds = remember(localTracks) { localTracks.map { it.id }.toSet() }

    // Applies the header source filter to any track list (local copies merged for LOCAL, stripped for SERVER).
    fun scopeSongs(tracks: List<NaviromTrack>): List<NaviromTrack> = when (sourceFilter) {
        LibrarySourceFilter.ALL -> tracks
        LibrarySourceFilter.LOCAL ->
            (localTracks + tracks.filter { it.id in localTrackIds }).distinctBy { it.id }
        LibrarySourceFilter.SERVER -> tracks.filter { it.id !in localTrackIds }
    }

    fun albumMatchesSource(album: NaviromAlbum): Boolean =
        sourceFilter.matches(localAlbumNames.contains(album.name.trim().lowercase()))

    fun artistMatchesSource(name: String): Boolean =
        sourceFilter.matches(localArtistNames.contains(name.trim().lowercase()))

    val filteredAlbums = albums.filter { album ->
        albumMatchesSource(album) &&
            (searchQuery.isBlank() ||
                album.name.contains(searchQuery, ignoreCase = true) ||
                album.artist.contains(searchQuery, ignoreCase = true))
    }
    val filteredArtists = artists.filter { artist ->
        artistMatchesSource(artist.name) &&
            (searchQuery.isBlank() || artist.name.contains(searchQuery, ignoreCase = true))
    }

    val isAllLibrariesSelected = selectedMusicFolderIds.isEmpty() || (musicFolders.isNotEmpty() && selectedMusicFolderIds.size >= musicFolders.size)

    var isSearchVisible by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("library_screen")
        ) {
            // 1. Time-of-day greeting header with profile name & search toggle
            GreetingHeader(
                profileName = profileName,
                appLanguage = appLanguage,
                sourceFilter = sourceFilter,
                onCycleSourceFilter = {
                    haptics.click()
                    sourceFilter = sourceFilter.next()
                }
            )

            // 2. Library search
            OutlinedTextField(
                value = librarySearchQuery,
                onValueChange = { query ->
                    librarySearchQuery = query
                    if (query.isNotBlank() && subTab != LibrarySubTab.SONGS) {
                        onSubTabSelected(LibrarySubTab.SONGS)
                    }
                },
                placeholder = {
                    Text(
                        text = if (appLanguage == AppLanguage.ALBANIAN) "Kërko në bibliotekë…" else "Bibliothek durchsuchen…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (librarySearchQuery.isNotEmpty()) {
                        IconButton(onClick = { librarySearchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("library_search_field")
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 3. Editorial Minimalist SubTabs (Inspired by ELOME navigation)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorialSubTabItem(
                    title = str("subtab_overview"),
                    isSelected = subTab == LibrarySubTab.OVERVIEW,
                    onClick = { onSubTabSelected(LibrarySubTab.OVERVIEW) },
                    testTag = "tab_overview"
                )
                EditorialSubTabItem(
                    title = if (librarySongs.isNotEmpty() || quickMixTracks.isNotEmpty()) {
                        val count = scopeSongs(if (librarySongs.isNotEmpty()) librarySongs else quickMixTracks).size
                        "${str("subtab_songs")} ($count)"
                    } else str("subtab_songs"),
                    isSelected = subTab == LibrarySubTab.SONGS,
                    onClick = { onSubTabSelected(LibrarySubTab.SONGS) },
                    testTag = "tab_songs"
                )
                EditorialSubTabItem(
                    title = "${str("subtab_albums")} (${filteredAlbums.size})",
                    isSelected = subTab == LibrarySubTab.ALBUMS,
                    onClick = { onSubTabSelected(LibrarySubTab.ALBUMS) },
                    testTag = "tab_albums"
                )
                EditorialSubTabItem(
                    title = "${str("subtab_artists")} (${filteredArtists.size})",
                    isSelected = subTab == LibrarySubTab.ARTISTS,
                    onClick = { onSubTabSelected(LibrarySubTab.ARTISTS) },
                    testTag = "tab_artists"
                )
                val localFolderCount = localFolders.count { it.tracks.isNotEmpty() && !disabledLocalFolderIds.contains(it.id) }
                val countSuffix = if (localFolderCount > 0) " ($localFolderCount)" else ""
                EditorialSubTabItem(
                    title = "${str("subtab_local_folders")}$countSuffix",
                    isSelected = subTab == LibrarySubTab.LIBRARIES,
                    onClick = { onSubTabSelected(LibrarySubTab.LIBRARIES) },
                    testTag = "tab_local_files"
                )
                EditorialSubTabItem(
                    title = str("newly_added_title"),
                    isSelected = subTab == LibrarySubTab.NEWEST,
                    onClick = { onSubTabSelected(LibrarySubTab.NEWEST) },
                    testTag = "tab_newest"
                )
                EditorialSubTabItem(
                    title = str("subtab_quick_mix"),
                    isSelected = subTab == LibrarySubTab.QUICK_MIX,
                    onClick = { onSubTabSelected(LibrarySubTab.QUICK_MIX) },
                    testTag = "tab_quick_mix"
                )
                EditorialSubTabItem(
                    title = if (recentlyPlayedTracks.isNotEmpty()) "${str("subtab_recent")} (${scopeSongs(recentlyPlayedTracks).size})" else str("subtab_recent"),
                    isSelected = subTab == LibrarySubTab.RECENT,
                    onClick = { onSubTabSelected(LibrarySubTab.RECENT) },
                    testTag = "tab_recent"
                )
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedContent(
                targetState = subTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    (fadeIn(animationSpec = tween(200)) +
                        slideInHorizontally(animationSpec = tween(200)) { width -> if (forward) width / 6 else -width / 6 })
                        .togetherWith(
                            fadeOut(animationSpec = tween(150)) +
                            slideOutHorizontally(animationSpec = tween(150)) { width -> if (forward) -width / 6 else width / 6 }
                        )
                },
                label = "LibrarySubTabAnimatedTransition",
                modifier = Modifier.fillMaxSize()
            ) { activeSubTab ->
                when (activeSubTab) {
                    LibrarySubTab.OVERVIEW -> {
                    // Overview Dashboard with the 4 requested groups:
                    // 1. Recent Played Songs
                    // 2. Recent Added (Albums & Tracks)
                    // 3. Most Played (Albums)
                    // 4. Random from Library (Albums)
                    val effectiveNewAlbums = (if (newestAlbums.isNotEmpty()) newestAlbums else albums).filter(::albumMatchesSource).take(12)
                    val effectiveMostPlayed = (if (mostPlayedAlbums.isNotEmpty()) mostPlayedAlbums else albums).filter(::albumMatchesSource).take(12)
                    val effectiveRandom = (if (randomAlbums.isNotEmpty()) randomAlbums else albums.shuffled()).filter(::albumMatchesSource).take(12)
                    val overviewRecentTracks = scopeSongs(recentlyPlayedTracks)

                    LazyColumn(
                        state = overviewListState,
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("library_overview_list")
                    ) {
                        // Group 1: 'Recent Played' Songs
                        item {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.History,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = str("recently_played_title"),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = str("recently_played_subtitle"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (overviewRecentTracks.isNotEmpty()) {
                                        TextButton(
                                            onClick = { onSubTabSelected(LibrarySubTab.RECENT) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(str("see_all"), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (overviewRecentTracks.isNotEmpty()) {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(overviewRecentTracks.take(15), key = { "overview_recent_${it.id}" }) { track ->
                                            val isThisPlaying = currentTrack?.id == track.id
                                            Surface(
                                                onClick = { onTrackClick(track, overviewRecentTracks) },
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isThisPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                modifier = Modifier.width(135.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Box {
                                                        SongAlbumCover(
                                                            coverArtUrl = track.coverArtUrl,
                                                            contentDescription = track.title,
                                                            isAlbum = false,
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(115.dp)
                                                        )
                                                        if (isThisPlaying && isPlaying) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .matchParentSize()
                                                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.PlayArrow,
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(28.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = track.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isThisPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = track.artist,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isThisPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // Clean empty state card for Recent
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = str("empty_recent_title"),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = str("empty_recent_desc"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Group 2: 'Recent Added' (Albums & Music)
                        if (effectiveNewAlbums.isNotEmpty()) {
                            item {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.FiberNew,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = str("recently_added_title"),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = str("recently_added_subtitle"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        TextButton(
                                            onClick = { onSubTabSelected(LibrarySubTab.NEWEST) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(str("see_all"), style = MaterialTheme.typography.labelMedium)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(effectiveNewAlbums.take(12), key = { "overview_new_${it.id}" }) { album ->
                                            Box(modifier = Modifier.width(145.dp)) {
                                                AlbumCard(
                                                    album = album,
                                                    onClick = { onSelectAlbum(album.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Group 3: 'Most Played' (Top Albums)
                        if (effectiveMostPlayed.isNotEmpty()) {
                            item {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.TrendingUp,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = str("most_played_title"),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = str("most_played_subtitle"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(effectiveMostPlayed.take(12), key = { "overview_most_played_${it.id}" }) { album ->
                                            Box(modifier = Modifier.width(145.dp)) {
                                                AlbumCard(
                                                    album = album,
                                                    onClick = { onSelectAlbum(album.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Group 4: 'Random from Library' (Alben)
                        if (effectiveRandom.isNotEmpty()) {
                            item {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Casino,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = str("random_albums_title"),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = str("random_albums_subtitle"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = onRefreshRandomAlbums,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Refresh,
                                                contentDescription = str("btn_refresh_random"),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(effectiveRandom.take(12), key = { "overview_random_${it.id}" }) { album ->
                                            Box(modifier = Modifier.width(145.dp)) {
                                                AlbumCard(
                                                    album = album,
                                                    onClick = { onSelectAlbum(album.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Local Songs in Overview
                        if (localTracks.isNotEmpty()) {
                            item {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Filled.FolderOpen,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    text = str("local_music_tab"),
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "${localTracks.size} ${str("songs_count_suffix")}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilledTonalButton(
                                                onClick = { onPlayAll(localTracks) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(str("btn_play_all"), style = MaterialTheme.typography.labelSmall)
                                            }
                                            IconButton(
                                                onClick = { onShuffleAll(localTracks) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Filled.Shuffle, contentDescription = str("btn_shuffle_all"), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(localTracks.take(15), key = { "overview_local_${it.id}" }) { track ->
                                            val isThisPlaying = currentTrack?.id == track.id
                                            Surface(
                                                onClick = { onTrackClick(track, localTracks) },
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (isThisPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                                modifier = Modifier.width(135.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Box {
                                                        SongAlbumCover(
                                                            coverArtUrl = track.coverArtUrl,
                                                            contentDescription = track.title,
                                                            isAlbum = false,
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(115.dp)
                                                        )
                                                        if (isThisPlaying && isPlaying) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .matchParentSize()
                                                                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.PlayArrow,
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(28.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(
                                                        text = track.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isThisPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = track.artist,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isThisPlaying) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
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
                LibrarySubTab.SONGS -> {
                    // List view for songs in library with sorting controls
                    val baseSongs = if (librarySongs.isNotEmpty()) librarySongs else {
                        val sorted = when (songSortOrder) {
                            SongSortOrder.NAME -> quickMixTracks.sortedBy { it.title.lowercase() }
                            SongSortOrder.DURATION -> quickMixTracks.sortedByDescending { it.durationSeconds }
                            SongSortOrder.RECENTLY_ADDED -> quickMixTracks
                            SongSortOrder.ARTIST -> quickMixTracks.sortedBy { it.artist.lowercase() }
                        }
                        if (isSongSortReversed) sorted.reversed() else sorted
                    }
                    val scopedSongs = scopeSongs(baseSongs)
                    val effectiveSongs = if (searchQuery.isBlank()) scopedSongs else scopedSongs.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                            it.artist.contains(searchQuery, ignoreCase = true) ||
                            it.album.contains(searchQuery, ignoreCase = true)
                    }
                    Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = songsListState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("library_songs_list_view")
                    ) {
                        // Song Sorting Controls Header
                        item {
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${effectiveSongs.size} ${str("subtab_songs").lowercase()}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = { onPlayAll(effectiveSongs) },
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(38.dp).testTag("songs_play_all_btn")
                                        ) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = str("btn_play_all"), modifier = Modifier.size(20.dp))
                                        }

                                        FilledTonalButton(
                                            onClick = { onShuffleAll(effectiveSongs) },
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(38.dp).testTag("songs_shuffle_all_btn")
                                        ) {
                                            Icon(Icons.Filled.Shuffle, contentDescription = str("btn_shuffle"), modifier = Modifier.size(20.dp))
                                        }

                                        FilledTonalButton(
                                            onClick = {
                                                haptics.click()
                                                if (onSmartShuffle != null) {
                                                    onSmartShuffle(effectiveSongs)
                                                } else {
                                                    onShuffleAll(effectiveSongs)
                                                }
                                            },
                                            shape = CircleShape,
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.size(38.dp).testTag("songs_smart_shuffle_btn")
                                        ) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = str("smart_shuffle"), modifier = Modifier.size(19.dp))
                                        }

                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Sort selection chips row: Name, Time/Duration, Last input / Recently Added, Artist
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${str("sort_by")}:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )

                                    FilterChip(
                                        selected = songSortOrder == SongSortOrder.NAME,
                                        onClick = { onSetSongSortOrder(SongSortOrder.NAME) },
                                        label = { Text(str("sort_name"), style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = {
                                             Icon(Icons.Filled.SortByAlpha, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        trailingIcon = if (songSortOrder == SongSortOrder.NAME) {
                                            {
                                                Icon(
                                                    imageVector = if (isSongSortReversed) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                                    contentDescription = if (isSongSortReversed) "Reversed" else "Normal",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("sort_name_chip")
                                    )

                                    FilterChip(
                                        selected = songSortOrder == SongSortOrder.DURATION,
                                        onClick = { onSetSongSortOrder(SongSortOrder.DURATION) },
                                        label = { Text(str("sort_duration"), style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = {
                                             Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        trailingIcon = if (songSortOrder == SongSortOrder.DURATION) {
                                            {
                                                Icon(
                                                    imageVector = if (isSongSortReversed) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                                    contentDescription = if (isSongSortReversed) "Reversed" else "Normal",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("sort_duration_chip")
                                    )

                                    FilterChip(
                                        selected = songSortOrder == SongSortOrder.RECENTLY_ADDED,
                                        onClick = { onSetSongSortOrder(SongSortOrder.RECENTLY_ADDED) },
                                        label = { Text(str("sort_recent"), style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = {
                                             Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        trailingIcon = if (songSortOrder == SongSortOrder.RECENTLY_ADDED) {
                                            {
                                                Icon(
                                                    imageVector = if (isSongSortReversed) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                                    contentDescription = if (isSongSortReversed) "Reversed" else "Normal",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("sort_recent_chip")
                                    )

                                    FilterChip(
                                        selected = songSortOrder == SongSortOrder.ARTIST,
                                        onClick = { onSetSongSortOrder(SongSortOrder.ARTIST) },
                                        label = { Text(str("sort_artist"), style = MaterialTheme.typography.labelSmall) },
                                        leadingIcon = {
                                             Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(14.dp))
                                        },
                                        trailingIcon = if (songSortOrder == SongSortOrder.ARTIST) {
                                            {
                                                Icon(
                                                    imageVector = if (isSongSortReversed) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                                                    contentDescription = if (isSongSortReversed) "Reversed" else "Normal",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else null,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("sort_artist_chip")
                                    )
                                }
                            }
                        }

                        // Songs list items
                        itemsIndexed(effectiveSongs, key = { _, track -> track.id }) { index, track ->
                            TrackListItem(
                                track = track,
                                isPlaying = isPlaying,
                                isCurrentTrack = currentTrack?.id == track.id,
                                downloadStatus = downloadStatuses[track.id] ?: DownloadStatus.NOT_DOWNLOADED,
                                downloadProgress = downloadProgresses[track.id],
                                isFavorite = favoriteIds.contains(track.id),
                                showCoverArt = true,
                                trackList = effectiveSongs,
                                onTrackClick = {
                                    if (isSongSelectionMode) {
                                        selectedSongIds = if (track.id in selectedSongIds) {
                                            selectedSongIds - track.id
                                        } else {
                                            selectedSongIds + track.id
                                        }
                                        if (selectedSongIds.isEmpty()) clearSongSelection()
                                    } else {
                                        onTrackClick(track, effectiveSongs)
                                    }
                                },
                                onToggleFavorite = { onToggleFavorite(track.id) },
                                onDownloadClick = { onDownloadTrack(track) },
                                onPlayNext = { onPlayNext(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onLongClick = {
                                    if (!isSongSelectionMode) {
                                        isSongSelectionMode = true
                                        selectedSongIds = setOf(track.id)
                                        rippleVisibleIds = songsListState.layoutInfo.visibleItemsInfo
                                            .map { it.key.toString() }.toSet()
                                        rippleOriginIndex = index
                                        rippleTrigger += 1
                                    }
                                },
                                isSelectionMode = isSongSelectionMode,
                                isSelected = track.id in selectedSongIds,
                                rippleTrigger = rippleTrigger,
                                rippleOriginIndex = rippleOriginIndex,
                                itemIndex = index,
                                rippleVisible = track.id in rippleVisibleIds,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    if (isSongSelectionMode) {
                        SelectionActionBar(
                            count = selectedSongIds.size,
                            allSelected = effectiveSongs.isNotEmpty() && selectedSongIds.containsAll(effectiveSongs.map { it.id }),
                            onCancel = { clearSongSelection() },
                            onSelectAll = { selectedSongIds = effectiveSongs.map { it.id }.toSet() },
                            onAdd = {
                                val tracks = effectiveSongs.filter { it.id in selectedSongIds }
                                onAddTracksToQueue?.invoke(tracks)
                                android.widget.Toast.makeText(
                                    context,
                                    String.format(str("toast_added_queue"), tracks.size),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                clearSongSelection()
                            },
                            str = ::str,
                            miniPlayerVisible = miniPlayerVisible,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                    }
                }
                LibrarySubTab.LIBRARIES -> {
                    // Local files tab: only on-device folders activated in Settings
                    val allLocalFoldersWithMusic = remember(localFolders) { localFolders.filter { it.tracks.isNotEmpty() } }
                    val musicLocalFolders = remember(allLocalFoldersWithMusic, disabledLocalFolderIds) {
                        allLocalFoldersWithMusic.filter { !disabledLocalFolderIds.contains(it.id) }
                    }
                    val activeLocalFolderCount = musicLocalFolders.size
                    val allLocalSelected = remember(allLocalFoldersWithMusic, disabledLocalFolderIds) {
                        allLocalFoldersWithMusic.none { disabledLocalFolderIds.contains(it.id) }
                    }

                    val searchedLocalFolders = remember(musicLocalFolders, librarySearchQuery) {
                        if (librarySearchQuery.isBlank()) musicLocalFolders
                        else musicLocalFolders.filter {
                            it.name.contains(librarySearchQuery, ignoreCase = true) ||
                            it.displayPath.contains(librarySearchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().testTag("local_files_tab_list")
                    ) {
                        // Section 0: Search input bar for Libraries/Folders
                        item {
                            OutlinedTextField(
                                value = librarySearchQuery,
                                onValueChange = { librarySearchQuery = it },
                                placeholder = { Text(str("local_folders_search_hint"), style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = str("local_folders_search_hint")) },
                                trailingIcon = {
                                    if (librarySearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { librarySearchQuery = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "Clear Search")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("libraries_search_field")
                            )
                        }

                        // Section 1: Local On-Device Audio Folders Banner
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().testTag("local_device_music_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.SdStorage,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onTertiary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = str("local_music_folders_title"),
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                text = if (allLocalFoldersWithMusic.isNotEmpty()) {
                                                    String.format(str("local_folders_active"), activeLocalFolderCount, allLocalFoldersWithMusic.size) + " • ${allLocalFoldersWithMusic.sumOf { it.trackCount }} ${str("tracks_count")}"
                                                } else {
                                                    str("local_music_folders_subtitle")
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = str("local_music_folders_title"),
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (musicLocalFolders.isNotEmpty()) {
                                                String.format(str("local_folders_active"), activeLocalFolderCount, musicLocalFolders.size) + " • ${musicLocalFolders.sumOf { it.trackCount }} ${str("tracks_count")}"
                                            } else {
                                                str("local_music_folders_subtitle")
                                            },
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (allLocalFoldersWithMusic.isNotEmpty()) {
                                            Button(
                                                onClick = {
                                                    haptics.click()
                                                    if (allLocalSelected) {
                                                        onDeselectAllLocalFolders()
                                                    } else {
                                                        onSelectAllLocalFolders()
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (allLocalSelected) Icons.Filled.Deselect else Icons.Filled.SelectAll,
                                                            contentDescription = if (allLocalSelected) str("deselect_all_local_folders") else str("select_all_local_folders"),
                                                            tint = MaterialTheme.colorScheme.onTertiary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                haptics.click()
                                                requestLocalPermission()
                                            },
                                            enabled = !isScanningLocalAudio,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f)),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (isScanningLocalAudio) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(14.dp),
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            strokeWidth = 1.5.dp
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Filled.Refresh,
                                                            contentDescription = if (musicLocalFolders.isEmpty()) str("scan_device_folders") else str("rescan_device_folders"),
                                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Local Folders List (Hiding folders with no music)
                        if (musicLocalFolders.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${str("local_folders_header")} (${searchedLocalFolders.size})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = String.format(
                                            str("local_folders_active"),
                                            musicLocalFolders.size,
                                            allLocalFoldersWithMusic.size
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }

                            items(searchedLocalFolders, key = { "local_folder_${it.id}" }) { folder ->
                                val isFolderEnabled = !disabledLocalFolderIds.contains(folder.id)
                                var isExpanded by remember(folder.id) { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("local_folder_card_${folder.id.hashCode()}"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isFolderEnabled) {
                                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                        }
                                    ),
                                    border = if (isFolderEnabled) {
                                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                                    } else {
                                        androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Compact single-row header; actions live in the expanded section
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(58.dp)
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isFolderEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isFolderEnabled) Icons.Filled.Folder else Icons.Outlined.FolderOff,
                                                        contentDescription = null,
                                                        tint = if (isFolderEnabled) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = folder.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isFolderEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${folder.trackCount} ${str("tracks_count")} • " +
                                                            if (folder.totalDurationSeconds >= 3600) {
                                                                "${folder.totalDurationSeconds / 3600}h ${(folder.totalDurationSeconds % 3600) / 60}m"
                                                            } else {
                                                                "${folder.totalDurationSeconds / 60} min"
                                                            },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    haptics.click()
                                                    if (!isFolderEnabled) {
                                                        onToggleLocalFolder(folder.id)
                                                    }
                                                    onPlayAll(folder.tracks)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = str("local_folder_play_all"),
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    haptics.click()
                                                    isExpanded = !isExpanded
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            haptics.click()
                                                            if (!isFolderEnabled) {
                                                                onToggleLocalFolder(folder.id)
                                                            }
                                                            onShuffleAll(folder.tracks)
                                                        },
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(32.dp),
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                        ),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Shuffle, contentDescription = str("local_folder_shuffle"), modifier = Modifier.size(16.dp))
                                                    }

                                                    FilledTonalButton(
                                                        onClick = {
                                                            haptics.click()
                                                            if (!isFolderEnabled) {
                                                                onToggleLocalFolder(folder.id)
                                                            }
                                                            onPlayTracksNext?.invoke(folder.tracks)
                                                            android.widget.Toast.makeText(context, String.format(str("toast_added_next"), folder.tracks.size), android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(32.dp).testTag("local_folder_play_next_${folder.id.hashCode()}"),
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                        ),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = str("folder_play_next"), modifier = Modifier.size(16.dp))
                                                    }

                                                    FilledTonalButton(
                                                        onClick = {
                                                            haptics.click()
                                                            if (!isFolderEnabled) {
                                                                onToggleLocalFolder(folder.id)
                                                            }
                                                            onAddTracksToQueue?.invoke(folder.tracks)
                                                            android.widget.Toast.makeText(context, String.format(str("toast_added_queue"), folder.tracks.size), android.widget.Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = CircleShape,
                                                        modifier = Modifier.size(32.dp).testTag("local_folder_add_to_queue_${folder.id.hashCode()}"),
                                                        colors = ButtonDefaults.filledTonalButtonColors(
                                                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                                        ),
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Icon(Icons.Filled.QueueMusic, contentDescription = str("folder_add_to_queue"), modifier = Modifier.size(16.dp))
                                                    }
                                                }

                                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                                folder.tracks.forEachIndexed { _, track ->
                                                    val isTrackPlaying = currentTrack?.id == track.id && isPlaying
                                                    TrackListItem(
                                                        track = track,
                                                        isPlaying = isTrackPlaying,
                                                        isCurrentTrack = currentTrack?.id == track.id,
                                                        downloadStatus = downloadStatuses[track.id] ?: DownloadStatus.NOT_DOWNLOADED,
                                                        downloadProgress = downloadProgresses[track.id] ?: 0f,
                                                        isFavorite = favoriteIds.contains(track.id),
                                                        trackList = folder.tracks,
                                                        onTrackClick = { onTrackClick(track, folder.tracks) },
                                                        onToggleFavorite = { onToggleFavorite(track.id) },
                                                        onDownloadClick = { onDownloadTrack(track) },
                                                        onPlayNext = { onPlayNext(track) },
                                                        onAddToQueue = { onAddToQueue(track) }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FolderOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = if (allLocalFoldersWithMusic.isEmpty()) {
                                                str("no_local_folders_found")
                                            } else {
                                                str("no_local_folders_enabled_in_settings")
                                            },
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        if (allLocalFoldersWithMusic.isNotEmpty()) {
                                            TextButton(onClick = {
                                                haptics.click()
                                                onGoToSettings()
                                            }) {
                                                Text(str("btn_open_settings_folders"))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
                LibrarySubTab.RECENT -> {
                    val scopedRecent = scopeSongs(recentlyPlayedTracks)
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().testTag("recent_tracks_list")
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.History,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = str("recently_played_title"),
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = str("recently_played_subtitle"),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }

                                    if (scopedRecent.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { onPlayAll(scopedRecent) },
                                                shape = CircleShape,
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(Icons.Filled.PlayArrow, contentDescription = str("play_all"), modifier = Modifier.size(20.dp))
                                            }
                                            FilledTonalButton(
                                                onClick = { onShuffleAll(scopedRecent) },
                                                shape = CircleShape,
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Icon(Icons.Filled.Shuffle, contentDescription = str("shuffle_all"), modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (scopedRecent.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Outlined.History,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = str("empty_recent_title"),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = str("empty_recent_desc"),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(scopedRecent, key = { it.id }) { track ->
                                TrackListItem(
                                    track = track,
                                    isPlaying = isPlaying,
                                    isCurrentTrack = currentTrack?.id == track.id,
                                    downloadStatus = downloadStatuses[track.id] ?: DownloadStatus.NOT_DOWNLOADED,
                                    downloadProgress = downloadProgresses[track.id],
                                    isFavorite = favoriteIds.contains(track.id),
                                    trackList = scopedRecent,
                                    onTrackClick = { onTrackClick(track, scopedRecent) },
                                    onToggleFavorite = { onToggleFavorite(track.id) },
                                    onDownloadClick = { onDownloadTrack(track) },
                                    onPlayNext = { onPlayNext(track) },
                                    onAddToQueue = { onAddToQueue(track) }
                                )
                            }
                        }
                    }
                }
            LibrarySubTab.NEWEST -> {
                val effectiveNewAlbums = (if (newestAlbums.isNotEmpty()) newestAlbums else albums).filter(::albumMatchesSource).take(12)
                val effectiveNewTracks = scopeSongs(if (newestTracks.isNotEmpty()) newestTracks else quickMixTracks.take(20))

                LazyColumn(
                    state = newestListState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize().testTag("newly_added_list")
                ) {
                    // Hero Banner for Newly Added
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.FiberNew,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = str("newly_added_title"),
                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = str("newly_added_subtitle"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (effectiveNewTracks.isNotEmpty()) onPlayAll(effectiveNewTracks)
                                        },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = str("play_all"), modifier = Modifier.size(20.dp))
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            if (effectiveNewTracks.isNotEmpty()) onShuffleAll(effectiveNewTracks)
                                        },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.Filled.Shuffle, contentDescription = str("shuffle_all"), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Newly Added Albums Section
                    if (effectiveNewAlbums.isNotEmpty()) {
                        item {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = str("newly_added_albums"),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${effectiveNewAlbums.size} ${str("subtab_albums").lowercase()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    effectiveNewAlbums.forEach { album ->
                                        Box(modifier = Modifier.width(150.dp)) {
                                            AlbumCard(
                                                album = album,
                                                onClick = { onSelectAlbum(album.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Removed Recently Played block from here since it's above the tabs now

                    // Newly Added Tracks Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = str("newly_added_tracks"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${effectiveNewTracks.size} ${str("tracks_count")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(effectiveNewTracks, key = { it.id }) { track ->
                        TrackListItem(
                            track = track,
                            isPlaying = isPlaying,
                            isCurrentTrack = currentTrack?.id == track.id,
                            downloadStatus = downloadStatuses[track.id] ?: DownloadStatus.NOT_DOWNLOADED,
                            downloadProgress = downloadProgresses[track.id],
                            isFavorite = favoriteIds.contains(track.id),
                            trackList = effectiveNewTracks,
                            onTrackClick = { onTrackClick(track, effectiveNewTracks) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onDownloadClick = { onDownloadTrack(track) },
                            onPlayNext = { onPlayNext(track) },
                            onAddToQueue = { onAddToQueue(track) }
                        )
                    }
                }
            }

            LibrarySubTab.ALBUMS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        state = albumsGridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredAlbums, key = { _, album -> album.id }) { index, album ->
                            AlbumCard(
                                album = album,
                                onClick = {
                                    if (isAlbumSelectionMode) {
                                        selectedAlbumIds = if (album.id in selectedAlbumIds) {
                                            selectedAlbumIds - album.id
                                        } else {
                                            selectedAlbumIds + album.id
                                        }
                                        if (selectedAlbumIds.isEmpty()) clearAlbumSelection()
                                    } else {
                                        onSelectAlbum(album.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isAlbumSelectionMode) {
                                        isAlbumSelectionMode = true
                                        selectedAlbumIds = setOf(album.id)
                                        rippleVisibleIds = albumsGridState.layoutInfo.visibleItemsInfo
                                            .map { it.key.toString() }.toSet()
                                        rippleOriginIndex = index
                                        rippleTrigger += 1
                                    }
                                },
                                isSelectionMode = isAlbumSelectionMode,
                                isSelected = album.id in selectedAlbumIds,
                                rippleTrigger = rippleTrigger,
                                rippleOriginIndex = rippleOriginIndex,
                                itemIndex = index,
                                rippleVisible = album.id in rippleVisibleIds
                            )
                        }
                    }

                    if (isAlbumSelectionMode) {
                        SelectionActionBar(
                            count = selectedAlbumIds.size,
                            allSelected = filteredAlbums.isNotEmpty() && selectedAlbumIds.containsAll(filteredAlbums.map { it.id }),
                            onCancel = { clearAlbumSelection() },
                            onSelectAll = { selectedAlbumIds = filteredAlbums.map { it.id }.toSet() },
                            onAdd = {
                                val ids = selectedAlbumIds.toList()
                                onAddAlbumsToQueue?.invoke(ids) { added ->
                                    if (added > 0) {
                                        android.widget.Toast.makeText(
                                            context,
                                            String.format(str("toast_added_queue"), added),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                clearAlbumSelection()
                            },
                            str = ::str,
                            miniPlayerVisible = miniPlayerVisible,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            LibrarySubTab.ARTISTS -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 140.dp),
                        state = artistsGridState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredArtists, key = { _, artist -> artist.id }) { index, artist ->
                            ArtistCard(
                                artist = artist,
                                onClick = {
                                    if (isArtistSelectionMode) {
                                        selectedArtistIds = if (artist.id in selectedArtistIds) {
                                            selectedArtistIds - artist.id
                                        } else {
                                            selectedArtistIds + artist.id
                                        }
                                        if (selectedArtistIds.isEmpty()) clearArtistSelection()
                                    } else {
                                        onSelectArtist(artist.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isArtistSelectionMode) {
                                        isArtistSelectionMode = true
                                        selectedArtistIds = setOf(artist.id)
                                        rippleVisibleIds = artistsGridState.layoutInfo.visibleItemsInfo
                                            .map { it.key.toString() }.toSet()
                                        rippleOriginIndex = index
                                        rippleTrigger += 1
                                    }
                                },
                                isSelectionMode = isArtistSelectionMode,
                                isSelected = artist.id in selectedArtistIds,
                                rippleTrigger = rippleTrigger,
                                rippleOriginIndex = rippleOriginIndex,
                                itemIndex = index,
                                rippleVisible = artist.id in rippleVisibleIds
                            )
                        }
                    }

                    if (isArtistSelectionMode) {
                        SelectionActionBar(
                            count = selectedArtistIds.size,
                            allSelected = filteredArtists.isNotEmpty() && selectedArtistIds.containsAll(filteredArtists.map { it.id }),
                            onCancel = { clearArtistSelection() },
                            onSelectAll = { selectedArtistIds = filteredArtists.map { it.id }.toSet() },
                            onAdd = {
                                val ids = selectedArtistIds.toList()
                                onAddArtistsToQueue?.invoke(ids) { added ->
                                    if (added > 0) {
                                        android.widget.Toast.makeText(
                                            context,
                                            String.format(str("toast_added_queue"), added),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                clearArtistSelection()
                            },
                            str = ::str,
                            miniPlayerVisible = miniPlayerVisible,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            LibrarySubTab.GENRES,
            LibrarySubTab.QUICK_MIX -> {
                val scopedQuickMix = scopeSongs(quickMixTracks)
                val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Sleek 2x2 Quick Actions / Discover Grid
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SleekFeatureCard(
                                        title = str("deck_liked"),
                                        subtitle = "${favoriteIds.size} ${str("deck_liked_sub")}",
                                        icon = Icons.Filled.Favorite,
                                        iconBgColor = if (isDark) SleekPillLikedBgDark else SleekPillLikedBg,
                                        iconColor = SleekPillLikedIcon,
                                        onClick = {
                                            if (favoriteTracks.isNotEmpty()) {
                                                onPlayAll(favoriteTracks)
                                            } else {
                                                val favTracks = scopedQuickMix.filter { favoriteIds.contains(it.id) }
                                                if (favTracks.isNotEmpty()) onPlayAll(favTracks) else onPlayAll(scopedQuickMix)
                                            }
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SleekFeatureCard(
                                        title = str("smart_shuffle"),
                                        subtitle = str("smart_shuffle_desc"),
                                        icon = Icons.Filled.AutoAwesome,
                                        iconBgColor = if (isDark) SleekPillMixBgDark else SleekPillMixBg,
                                        iconColor = SleekPillMixIcon,
                                        onClick = {
                                            if (onSmartShuffle != null) {
                                                onSmartShuffle(null)
                                            } else {
                                                onShuffleAll(scopedQuickMix)
                                            }
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    SleekFeatureCard(
                                        title = str("deck_shuffle"),
                                        subtitle = str("deck_shuffle_sub"),
                                        icon = Icons.Filled.Shuffle,
                                        iconBgColor = if (isDark) SleekPillFlashbackBgDark else SleekPillFlashbackBg,
                                        iconColor = SleekPillFlashbackIcon,
                                        onClick = { onShuffleAll(scopedQuickMix) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    SleekFeatureCard(
                                        title = str("subtab_albums"),
                                        subtitle = "${filteredAlbums.size} collections",
                                        icon = Icons.Filled.Album,
                                        iconBgColor = if (isDark) SleekPillDiscoverBgDark else SleekPillDiscoverBg,
                                        iconColor = SleekPillDiscoverIcon,
                                        onClick = { onSubTabSelected(LibrarySubTab.ALBUMS) }
                                    )
                                }
                            }
                        }
                    }

                    // Quick Mix Track List Section Header
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = str("subtab_quick_mix"),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.3).sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FilledTonalButton(
                                onClick = { onPlayAll(scopedQuickMix) },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(str("btn_play_all"), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    // Track items
                    items(scopedQuickMix, key = { it.id }) { track ->
                        TrackListItem(
                            track = track,
                            isPlaying = isPlaying,
                            isCurrentTrack = currentTrack?.id == track.id,
                            downloadStatus = downloadStatuses[track.id] ?: DownloadStatus.NOT_DOWNLOADED,
                            downloadProgress = downloadProgresses[track.id],
                            isFavorite = favoriteIds.contains(track.id),
                            trackList = scopedQuickMix,
                            onTrackClick = { onTrackClick(track, scopedQuickMix) },
                            onToggleFavorite = { onToggleFavorite(track.id) },
                            onDownloadClick = { onDownloadTrack(track) },
                            onPlayNext = { onPlayNext(track) },
                            onAddToQueue = { onAddToQueue(track) }
                        )
                    }
                }
            }
        }
    }
    }

    // Folder Switcher Dialog
        if (showFolderSwitcherDialog) {
            AlertDialog(
                onDismissRequest = { showFolderSwitcherDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderSpecial,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = str("switch_library_prompt"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = str("server_libraries_desc"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // All Libraries option
                        val isAllSelectedInDialog = selectedMusicFolderIds.isEmpty() || (musicFolders.isNotEmpty() && selectedMusicFolderIds.size >= musicFolders.size)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAllSelectedInDialog) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = {
                                onSelectAllMusicFolders()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AllInclusive,
                                        contentDescription = null,
                                        tint = if (isAllSelectedInDialog) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = str("all_libraries_title"),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isAllSelectedInDialog) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Checkbox(
                                    checked = isAllSelectedInDialog,
                                    onCheckedChange = { onSelectAllMusicFolders() }
                                )
                            }
                        }

                        // Specific Music Folders
                        musicFolders.forEach { folder ->
                            val isChecked = !isAllSelectedInDialog && selectedMusicFolderIds.contains(folder.id)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                onClick = {
                                    onSelectMusicFolder(folder.id)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Folder,
                                            contentDescription = null,
                                            tint = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Column {
                                            Text(
                                                text = folder.name,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "ID: ${folder.id}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { onToggleMusicFolder(folder.id) }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFolderSwitcherDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Animated Sidebar Drawer for Libraries on HomeScreen has been moved to NaviromApp
    }
}

@Composable
fun AlbumDetailView(
    album: NaviromAlbum?,
    tracks: List<NaviromTrack>,
    currentTrack: NaviromTrack?,
    isPlaying: Boolean,
    downloadStatuses: Map<String, DownloadStatus>,
    downloadProgresses: Map<String, Float>,
    favoriteIds: List<String>,
    appLanguage: AppLanguage,
    onBack: () -> Unit,
    onTrackClick: (NaviromTrack) -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onSmartShuffle: (() -> Unit)? = null,
    onPlayTracksNext: (() -> Unit)? = null,
    onAddTracksToQueue: (() -> Unit)? = null,
    onDownloadAlbum: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadTrack: (NaviromTrack) -> Unit,
    onPlayNext: (NaviromTrack) -> Unit,
    onAddToQueue: (NaviromTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    fun str(key: String): String = NaviromStrings.get(key, appLanguage)
    val haptics = rememberNaviromHaptics()
    BackHandler(enabled = true) { onBack() }

    val totalSeconds = remember(tracks) { tracks.sumOf { it.durationSeconds } }
    val totalDurationFormatted = remember(totalSeconds) {
        val min = totalSeconds / 60
        val sec = totalSeconds % 60
        "${min}m ${sec}s"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var dragX = 0f
                var dragY = 0f
                detectDragGestures(
                    onDragStart = { dragX = 0f; dragY = 0f },
                    onDrag = { change, dragAmount ->
                        dragX += dragAmount.x
                        dragY += dragAmount.y
                    },
                    onDragEnd = {
                        if (dragX > 75f && kotlin.math.abs(dragX) > kotlin.math.abs(dragY) * 1.2f) {
                            haptics.toggle()
                            onBack()
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("album_detail_view")
        ) {
        // Back Button & Top Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("album_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = album?.name ?: str("subtab_albums"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Album Header Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongAlbumCover(
                    coverArtUrl = album?.coverArtUrl,
                    contentDescription = album?.name,
                    isAlbum = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(110.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = album?.name ?: "Album",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = album?.artist ?: "Artist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${tracks.size} Songs • $totalDurationFormatted" + (if (album?.year != null) " • ${album.year}" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons Row (Play, Shuffle, Download)
        item {
            ActionButtonGroup {
                DualAudioPlayButton(
                    text = str("btn_play_all"),
                    icon = Icons.Filled.PlayArrow,
                    onClickPlayer1 = onPlayAll,
                    tracks = tracks,
                    testTag = "album_play_all"
                )

                DualAudioPlayButton(
                    text = str("btn_shuffle"),
                    icon = Icons.Filled.Shuffle,
                    onClickPlayer1 = onShuffleAll,
                    tracks = remember(tracks) { tracks.shuffled() },
                    isTonal = true,
                    testTag = "album_shuffle_all"
                )

                DualAudioPlayButton(
                    text = str("smart_shuffle"),
                    icon = Icons.Filled.AutoAwesome,
                    onClickPlayer1 = {
                        if (onSmartShuffle != null) {
                            onSmartShuffle()
                        } else {
                            onShuffleAll()
                        }
                    },
                    tracks = tracks,
                    isTonal = true,
                    testTag = "album_smart_shuffle_all"
                )

                FilledTonalButton(
                    onClick = {
                        haptics.click()
                        onPlayTracksNext?.invoke()
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("album_play_next_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = str("btn_play_next"), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(str("btn_play_next"))
                }

                FilledTonalButton(
                    onClick = {
                        haptics.click()
                        onAddTracksToQueue?.invoke()
                    },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("album_add_to_queue_btn")
                ) {
                    Icon(Icons.Filled.QueueMusic, contentDescription = str("btn_add_to_queue"), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(str("btn_add_to_queue"))
                }

                OutlinedButton(
                    onClick = onDownloadAlbum,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("album_download_btn")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = str("btn_download"), modifier = Modifier.size(20.dp))
                }
            }
        }

        // Tracklist
        items(tracks, key = { it.id }) { track ->
            val index = tracks.indexOf(track) + 1
            TrackListItem(
                track = track,
                isPlaying = isPlaying,
                isCurrentTrack = currentTrack?.id == track.id,
                downloadStatus = downloadStatuses[track.id] ?: DownloadStatus.NOT_DOWNLOADED,
                downloadProgress = downloadProgresses[track.id],
                isFavorite = favoriteIds.contains(track.id),
                showCoverArt = false,
                trackIndex = index,
                trackList = tracks,
                onTrackClick = { onTrackClick(track) },
                onToggleFavorite = { onToggleFavorite(track.id) },
                onDownloadClick = { onDownloadTrack(track) },
                onPlayNext = { onPlayNext(track) },
                onAddToQueue = { onAddToQueue(track) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
        }
    }
    }
}

@Composable
private fun GreetingHeader(
    profileName: String,
    appLanguage: AppLanguage,
    sourceFilter: LibrarySourceFilter,
    onCycleSourceFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun str(key: String): String = NaviromStrings.get(key, appLanguage)

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingKey = when (currentHour) {
        in 5..11 -> "greeting_morning"
        in 12..17 -> "greeting_afternoon"
        in 18..22 -> "greeting_evening"
        else -> "greeting_night"
    }

    val greetingPrefix = str(greetingKey)
    val displayName = if (profileName.isNotBlank()) profileName.trim() else str("profile_default_name")
    val greetingText = "$greetingPrefix, $displayName"

    Surface(
        color = Color.Transparent,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("library_greeting_header")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greetingText,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = str("greeting_subtitle"),
                    style = MaterialTheme.typography.bodySmall.copy(letterSpacing = 0.4.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Source filter: all sources -> local files only -> server files only
            val isFilterActive = sourceFilter != LibrarySourceFilter.ALL
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                onClick = onCycleSourceFilter,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("greeting_source_filter_toggle")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = sourceFilter.icon,
                        contentDescription = str(sourceFilter.labelKey),
                        tint = if (isFilterActive) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorialSubTabItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            style = if (isSelected) {
                MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.2.sp
                )
            },
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(1.5.dp)
                .width(28.dp)
                .background(if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent)
        )
    }
}

/** Bottom action bar shown while long-press multi-select is active in Albums or Songs. */
@Composable
private fun SelectionActionBar(
    count: Int,
    allSelected: Boolean,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onAdd: () -> Unit,
    str: (String) -> String,
    miniPlayerVisible: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            // The persistent mini-player is drawn on top of the library content, so
            // lift the action bar clear of it instead of hiding it underneath.
            .then(if (miniPlayerVisible) Modifier.padding(bottom = 84.dp) else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = str("exit_dialog_cancel")
                )
            }
            Text(
                text = String.format(str("selection_count"), count),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            IconButton(onClick = onSelectAll, enabled = !allSelected) {
                Icon(
                    imageVector = Icons.Filled.SelectAll,
                    contentDescription = str("select_all_libraries"),
                    tint = if (allSelected) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary
                )
            }
            Button(
                onClick = onAdd,
                enabled = count > 0,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(str("add_to_queue"))
            }
        }
    }
}

