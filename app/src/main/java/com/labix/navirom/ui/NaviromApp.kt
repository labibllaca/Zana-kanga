package com.labix.navirom.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.labix.R
import com.labix.navirom.data.model.DownloadStatus
import com.labix.navirom.data.model.DualSpeakerMode
import com.labix.navirom.player.wlan.*
import com.labix.navirom.ui.components.*
import com.labix.navirom.update.AppUpdateInfo
import com.labix.navirom.update.UpdateState
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.labix.navirom.ui.util.rememberNaviromHaptics
import androidx.activity.compose.BackHandler
import com.labix.navirom.ui.screens.*
import com.labix.navirom.ui.util.RememberShakeDetector
import com.labix.navirom.ui.SongSortOrder
import com.labix.ui.theme.AccentEmerald
import com.labix.navirom.ui.radial.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NaviromApp(
    viewModel: NaviromViewModel,
    modifier: Modifier = Modifier,
    onCloseApp: () -> Unit = {}
) {
    val haptics = rememberNaviromHaptics()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkConnectionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val isCrossfadeEnabled by viewModel.isCrossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDurationSeconds by viewModel.crossfadeDurationSeconds.collectAsStateWithLifecycle()
    val isVinylEffectEnabled by viewModel.isVinylEffectEnabled.collectAsStateWithLifecycle()
    val isAstrolabeModeEnabled by viewModel.isAstrolabeModeEnabled.collectAsStateWithLifecycle()
    val focusUsernameTrigger by viewModel.focusUsernameTrigger.collectAsStateWithLifecycle()

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val librarySubTab by viewModel.librarySubTab.collectAsStateWithLifecycle()
    val selectedAlbumId by viewModel.selectedAlbumId.collectAsStateWithLifecycle()
    val selectedPlaylistId by viewModel.selectedPlaylistId.collectAsStateWithLifecycle()
    val selectedArtistId by viewModel.selectedArtistId.collectAsStateWithLifecycle()
    val currentArtist by viewModel.currentArtist.collectAsStateWithLifecycle()
    val currentArtistAlbums by viewModel.currentArtistAlbums.collectAsStateWithLifecycle()
    val currentArtistSongs by viewModel.currentArtistSongs.collectAsStateWithLifecycle()
    val isLoadingArtistDetails by viewModel.isLoadingArtistDetails.collectAsStateWithLifecycle()

    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val newestAlbums by viewModel.newestAlbums.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.mostPlayedAlbums.collectAsStateWithLifecycle()
    val randomAlbums by viewModel.randomAlbums.collectAsStateWithLifecycle()
    val newestTracks by viewModel.newestTracks.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val quickMixTracks by viewModel.quickMixTracks.collectAsStateWithLifecycle()
    val recentlyPlayedTracks by viewModel.recentlyPlayedTracks.collectAsStateWithLifecycle()
    val librarySongs by viewModel.librarySongs.collectAsStateWithLifecycle()
    val songSortOrder by viewModel.songSortOrder.collectAsStateWithLifecycle()
    val isSongSortReversed by viewModel.isSongSortReversed.collectAsStateWithLifecycle()
    val currentAlbumTracks by viewModel.currentAlbumTracks.collectAsStateWithLifecycle()
    val currentPlaylistTracks by viewModel.currentPlaylistTracks.collectAsStateWithLifecycle()

    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val listeningStats by viewModel.listeningStats.collectAsStateWithLifecycle()
    val isStatsScreenVisible by viewModel.isStatsScreenVisible.collectAsStateWithLifecycle()
    val shakeNotificationMessage by viewModel.shakeNotificationMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Global Shake detector: 2 seconds of shaking starts a random song from active library
    RememberShakeDetector(
        enabled = true,
        onShake2Seconds = {
            haptics.toggle()
            viewModel.playRandomTrackFromCurrentLibrary()
        }
    )

    LaunchedEffect(shakeNotificationMessage) {
        shakeNotificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.dismissShakeNotification()
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()

    val searchFocusTrigger by viewModel.searchFocusTrigger.collectAsStateWithLifecycle()
    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val allServers by viewModel.allServers.collectAsStateWithLifecycle()
    val cachedTracks by viewModel.cachedTracks.collectAsStateWithLifecycle()
    val totalCacheSizeBytes by viewModel.totalCacheSizeBytes.collectAsStateWithLifecycle()
    val isOfflineOnlyMode by viewModel.isOfflineOnlyMode.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val localTracks by viewModel.localTracks.collectAsStateWithLifecycle()
    val localFolders by viewModel.localFolders.collectAsStateWithLifecycle()
    val allDiscoveredLocalFolders by viewModel.allDiscoveredLocalFolders.collectAsStateWithLifecycle()
    val settingsEnabledLocalFolderIds by viewModel.settingsEnabledLocalFolderIds.collectAsStateWithLifecycle()
    val disabledLocalFolderIds by viewModel.disabledLocalFolderIds.collectAsStateWithLifecycle()
    val isScanningLocalAudio by viewModel.isScanningLocalAudio.collectAsStateWithLifecycle()
    val currentMusicFolderPath by viewModel.currentMusicFolderPath.collectAsStateWithLifecycle()

    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val queue by viewModel.currentQueue.collectAsStateWithLifecycle()
    val queueIndex by viewModel.currentQueueIndex.collectAsStateWithLifecycle()
    val downloadStatuses by viewModel.downloadStatuses.collectAsStateWithLifecycle()
    val downloadProgresses by viewModel.downloadProgresses.collectAsStateWithLifecycle()

    val isFullPlayerVisible by viewModel.isFullPlayerVisible.collectAsStateWithLifecycle()
    val isQueueSheetVisible by viewModel.isQueueSheetVisible.collectAsStateWithLifecycle()
    val sleepTimerOptions by viewModel.sleepTimerOptions.collectAsStateWithLifecycle()
    val isDualAudioEnabled by viewModel.isDualAudioEnabled.collectAsStateWithLifecycle()
    val secondaryPlaybackState by viewModel.secondaryPlaybackState.collectAsStateWithLifecycle()
    val isDeckSyncEnabled by viewModel.isDeckSyncEnabled.collectAsStateWithLifecycle()
    val dualSpeakerMode by viewModel.dualSpeakerMode.collectAsStateWithLifecycle()
    val deckSyncBalance by viewModel.deckSyncBalance.collectAsStateWithLifecycle()
    val availableOutputDevices by viewModel.availableOutputDevices.collectAsStateWithLifecycle()
    val player1DeviceId by viewModel.player1DeviceId.collectAsStateWithLifecycle()
    val player2DeviceId by viewModel.player2DeviceId.collectAsStateWithLifecycle()
    val wlanSpeakerState by viewModel.wlanSpeakerState.collectAsStateWithLifecycle()
    val isWlanCastSheetVisible by viewModel.isWlanCastSheetVisible.collectAsStateWithLifecycle()

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsStateWithLifecycle()
    val updateGithubRepo by viewModel.updateGithubRepo.collectAsStateWithLifecycle()
    val lastUpdateCheckedTime by viewModel.lastUpdateCheckedTime.collectAsStateWithLifecycle()

    fun str(key: String): String = NaviromStrings.get(key, appLanguage)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    var backPressedOnce by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000L)
            backPressedOnce = false
        }
    }

    BackHandler(enabled = true) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else if (isFullPlayerVisible) {
            viewModel.setFullPlayerVisible(false)
        } else if (isQueueSheetVisible) {
            viewModel.setQueueSheetVisible(false)
        } else if (isStatsScreenVisible) {
            viewModel.setStatsScreenVisible(false)
        } else if (selectedAlbumId != null || selectedPlaylistId != null || selectedArtistId != null) {
            viewModel.selectAlbum(null)
            viewModel.selectPlaylist(null)
            viewModel.selectArtist(null)
        } else if (searchQuery.isNotBlank()) {
            viewModel.onSearchQueryChange("")
        } else if (currentTab != NaviromTab.LIBRARY) {
            viewModel.setTab(NaviromTab.LIBRARY)
        } else {
            if (backPressedOnce) {
                showExitDialog = true
            } else {
                backPressedOnce = true
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = str("press_back_again_to_exit"),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalDualAudioContext provides DualAudioContext(
            isDualAudioEnabled = isDualAudioEnabled,
            onPlayPlayer1 = { viewModel.playTrack(it) },
            onPlayPlayer2 = { track, trackList -> viewModel.playSecondaryTrack(track, trackList ?: queue) },
            onPlayAllPlayer2 = { tracks ->
                tracks.firstOrNull()?.let { first ->
                    viewModel.playSecondaryTrack(first, tracks)
                }
            }
        )
    ) {
        val pagerState = rememberPagerState(initialPage = 1) { 3 }
        
        Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val backgroundColor = Color.White
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
            ) {
            when (page) {
                0 -> {
                    // Player Zone
                    AstrolabeFullPlayer(
                        playbackState = playbackState,
                        isFavorite = favoriteIds.contains(playbackState.currentTrack?.id),
                        queue = queue,
                        queueIndex = queueIndex,
                        appLanguage = appLanguage,
                        onDismiss = {},
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onNext = { viewModel.next() },
                        onPrevious = { viewModel.previous() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onToggleShuffle = { viewModel.toggleShuffle() },
                        onCycleRepeat = { viewModel.cycleRepeatMode() },
                        onToggleFavorite = { playbackState.currentTrack?.let { viewModel.toggleFavorite(it.id) } },
                        onTrackSelect = { track, list -> viewModel.playTrack(track, list) },
                        onToggleClassicView = { viewModel.toggleAstrolabeMode() }
                    )
                }
                1 -> {
                    // Library Zone
                    LibraryScreen(
                        subTab = librarySubTab,
                        onSubTabSelected = { viewModel.setLibrarySubTab(it) },
                        albums = albums,
                        newestAlbums = newestAlbums,
                        mostPlayedAlbums = mostPlayedAlbums,
                        randomAlbums = randomAlbums,
                        onRefreshRandomAlbums = { viewModel.refreshRandomAlbums() },
                        profileName = serverState.username,
                        newestTracks = newestTracks,
                        artists = artists,
                        quickMixTracks = quickMixTracks,
                        recentlyPlayedTracks = recentlyPlayedTracks,
                        librarySongs = librarySongs,
                        songSortOrder = songSortOrder,
                        isSongSortReversed = isSongSortReversed,
                        onSetSongSortOrder = { viewModel.setSongSortOrder(it) },
                        selectedAlbumId = selectedAlbumId,
                        selectedAlbumTracks = currentAlbumTracks,
                        currentTrack = playbackState.currentTrack,
                        isPlaying = playbackState.isPlaying,
                        downloadStatuses = downloadStatuses,
                        downloadProgresses = downloadProgresses,
                        favoriteIds = favoriteIds,
                        favoriteTracks = favoriteTracks,
                        appLanguage = appLanguage,
                        musicFolders = serverState.musicFolders,
                        selectedMusicFolderId = serverState.selectedMusicFolderId,
                        selectedMusicFolderIds = serverState.selectedMusicFolderIds,
                        onSelectMusicFolder = { viewModel.selectMusicFolder(it) },
                        onToggleMusicFolder = { viewModel.toggleMusicFolder(it) },
                        onSelectAllMusicFolders = { viewModel.selectAllMusicFolders() },
                        onSyncLibrary = { viewModel.syncLibrary() },
                        onSelectAlbum = { viewModel.selectAlbum(it) },
                        onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                        onPlayAll = { viewModel.playAll(it) },
                        onShuffleAll = { viewModel.shuffleAll(it) },
                        onSmartShuffle = { viewModel.smartShuffle(it) },
                        onPlayTracksNext = { viewModel.playTracksNext(it) },
                        onAddTracksToQueue = { viewModel.addTracksToQueue(it) },
                        onDownloadTracks = { viewModel.downloadTracks(it) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDownloadTrack = { viewModel.downloadTrack(it) },
                        onPlayNext = { viewModel.playNext(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
                        onGoToSettings = { pagerState.requestScrollToPage(2) },
                        onScanNetwork = { viewModel.startAutoScanAndFocusUsername() },
                        onOpenSidebar = { /* No-op */ },
                        selectedArtistId = selectedArtistId,
                        currentArtist = currentArtist,
                        currentArtistAlbums = currentArtistAlbums,
                        currentArtistSongs = currentArtistSongs,
                        isLoadingArtistDetails = isLoadingArtistDetails,
                        onSelectArtist = { viewModel.selectArtist(it) },
                        localTracks = localTracks,
                        localFolders = localFolders,
                        disabledLocalFolderIds = disabledLocalFolderIds,
                        onToggleLocalFolder = { viewModel.toggleLocalFolder(it) },
                        onSetLocalFolderEnabled = { id, enabled -> viewModel.setLocalFolderEnabled(id, enabled) },
                        onSelectAllLocalFolders = { viewModel.selectAllLocalFolders() },
                        onDeselectAllLocalFolders = { viewModel.deselectAllLocalFolders() },
                        isScanningLocalAudio = isScanningLocalAudio,
                        onScanLocalAudio = { viewModel.scanLocalAudio() }
                    )
                }
                2 -> {
                    // Settings Zone
                    ServerSettingsScreen(
                        serverState = serverState,
                        allServers = allServers,
                        onSelectServer = { viewModel.setServerConfig(it) },
                        onDeleteServer = { viewModel.deleteServerConfig(it) },
                        appLanguage = appLanguage,
                        appThemeMode = appThemeMode,
                        statsSummary = listeningStats,
                        isCrossfadeEnabled = isCrossfadeEnabled,
                        crossfadeDurationSeconds = crossfadeDurationSeconds,
                        isVinylEffectEnabled = isVinylEffectEnabled,
                        isDualAudioEnabled = isDualAudioEnabled,
                        wlanSpeakerState = wlanSpeakerState,
                        onOpenWlanCast = { viewModel.showWlanCastSheet() },
                        onSetCrossfadeEnabled = { viewModel.setCrossfadeEnabled(it) },
                        onSetCrossfadeDurationSeconds = { viewModel.setCrossfadeDurationSeconds(it) },
                        onSetVinylEffectEnabled = { viewModel.setVinylEffectEnabled(it) },
                        onSetDualAudioEnabled = { viewModel.setDualAudioEnabled(it) },
                        onViewStats = { viewModel.setStatsScreenVisible(true) },
                        onSetLanguage = { viewModel.setLanguage(it) },
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        onUpdateConfig = { protocol, host, port, user, p, t, altHost -> viewModel.updateServerConfig(protocol, host, port, user, p, t, altHost) },
                        onConnect = { viewModel.connectServer() },
                        onSyncLibrary = { viewModel.syncLibrary() },
                        onScanNetwork = { viewModel.startAutoScanAndFocusUsername() },
                        onSelectMusicFolder = { viewModel.selectMusicFolder(it) },
                        onToggleMusicFolder = { viewModel.toggleMusicFolder(it) },
                        onSelectAllMusicFolders = { viewModel.selectAllMusicFolders() },
                        allDiscoveredLocalFolders = allDiscoveredLocalFolders,
                        settingsEnabledLocalFolderIds = settingsEnabledLocalFolderIds,
                        onToggleSettingsLocalFolder = { viewModel.toggleSettingsLocalFolder(it) },
                        onSetSettingsLocalFolderEnabled = { id, enabled -> viewModel.setSettingsLocalFolderEnabled(id, enabled) },
                        onSelectAllSettingsLocalFolders = { viewModel.selectAllSettingsLocalFolders() },
                        onDeselectAllSettingsLocalFolders = { viewModel.deselectAllSettingsLocalFolders() },
                        isScanningLocalAudio = isScanningLocalAudio,
                        onScanLocalAudio = { viewModel.scanLocalAudio() },
                        focusUsernameTrigger = focusUsernameTrigger,
                        updateState = updateState,
                        autoCheckUpdates = autoCheckUpdates,
                        githubRepo = updateGithubRepo,
                        lastUpdateCheckedTime = lastUpdateCheckedTime,
                        onCheckForUpdates = { viewModel.checkForAppUpdates(isManual = true) },
                        onDownloadAndInstallUpdate = { viewModel.downloadAndInstallUpdate(it) },
                        onSetAutoCheckUpdates = { viewModel.setAutoCheckUpdates(it) },
                        onSetGithubRepo = { viewModel.setUpdateGithubRepo(it) },
                        onDismissUpdate = { viewModel.dismissAppUpdate() }
                    )
                }
            }
        }
        }

        // Persistent MiniPlayerBar for browsing zones (Library & Settings)
        if (pagerState.currentPage != 0 && playbackState.currentTrack != null && !isFullPlayerVisible) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 6.dp)
            ) {
                MiniPlayerBar(
                    playbackState = playbackState,
                    onExpandPlayer = {
                        scope.launch { pagerState.animateScrollToPage(0) }
                    },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.next() },
                    onPrevious = { viewModel.previous() }
                )
            }
        }
    }

        // Full Screen Player Modal
        if (isFullPlayerVisible && playbackState.currentTrack != null) {
            FullPlayerModal(
                playbackState = playbackState,
                isFavorite = favoriteIds.contains(playbackState.currentTrack?.id),
                downloadStatus = downloadStatuses[playbackState.currentTrack?.id] ?: DownloadStatus.NOT_DOWNLOADED,
                downloadProgress = downloadProgresses[playbackState.currentTrack?.id],
                queueIndex = queueIndex,
                queueSize = queue.size,
                lyricsData = currentLyrics,
                appLanguage = appLanguage,
                isVinylEffectEnabled = isVinylEffectEnabled,
                sleepTimerOptions = sleepTimerOptions,
                isDualAudioEnabled = isDualAudioEnabled,
                secondaryPlaybackState = secondaryPlaybackState,
                isDeckSyncEnabled = isDeckSyncEnabled,
                onToggleDeckSync = { viewModel.toggleDeckSync() },
                dualSpeakerMode = dualSpeakerMode,
                deckSyncBalance = deckSyncBalance,
                onSetDualSpeakerMode = { viewModel.setDualSpeakerMode(it) },
                onSetDeckSyncBalance = { viewModel.setDeckSyncBalance(it) },
                onRealignDeckSync = { viewModel.realignDeckSync() },
                availableOutputDevices = availableOutputDevices,
                player1DeviceId = player1DeviceId,
                player2DeviceId = player2DeviceId,
                onSetPlayer1PreferredDevice = { viewModel.setPlayer1PreferredDevice(it) },
                onSetPlayer2PreferredDevice = { viewModel.setPlayer2PreferredDevice(it) },
                onRefreshOutputDevices = { viewModel.refreshOutputDevices() },
                wlanSpeakerState = wlanSpeakerState,
                onOpenWlanCast = { viewModel.showWlanCastSheet() },
                queue = queue,
                isAstrolabeModeEnabled = isAstrolabeModeEnabled,
                onToggleAstrolabeMode = { viewModel.toggleAstrolabeMode() },
                onTrackSelect = { track, list -> viewModel.playTrack(track, list) },
                onToggleSecondaryPlayPause = { viewModel.toggleSecondaryPlayPause() },
                onPlaySecondaryNext = { viewModel.playSecondaryNext() },
                onPlaySecondaryPrevious = { viewModel.playSecondaryPrevious() },
                onSeekSecondaryTo = { viewModel.seekSecondaryTo(it) },
                onSeekSecondaryRelative = { viewModel.seekSecondaryRelative(it) },
                onSetSecondaryVolume = { viewModel.setSecondaryVolume(it) },
                onStopSecondaryTrack = { viewModel.stopSecondaryTrack() },
                onDismiss = { viewModel.setFullPlayerVisible(false) },
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() },
                onSeekTo = { viewModel.seekTo(it) },
                onSeekRelative = { viewModel.seekRelative(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onToggleFavorite = { playbackState.currentTrack?.let { viewModel.toggleFavorite(it.id) } },
                onDownloadTrack = { playbackState.currentTrack?.let { viewModel.downloadTrack(it) } },
                onOpenQueue = { viewModel.setQueueSheetVisible(true) },
                onSetSpeed = { viewModel.setPlaybackSpeed(it) },
                onSetSleepTimer = { minutes, options -> viewModel.setSleepTimer(minutes, options) },
                onUpdateSleepTimerOptions = { viewModel.updateSleepTimerOptions(it) },
                onRefetchLyrics = { viewModel.refetchCurrentLyrics() },
                onFetchTeksteShqipLyrics = { url ->
                    viewModel.fetchLyricsFromTeksteShqip(url, playbackState.currentTrack)
                },
                onArtistClick = { artistId ->
                    viewModel.setFullPlayerVisible(false)
                    viewModel.setTab(NaviromTab.LIBRARY)
                    viewModel.selectArtist(artistId)
                },
                onAlbumClick = { albumId ->
                    viewModel.setFullPlayerVisible(false)
                    viewModel.setTab(NaviromTab.LIBRARY)
                    viewModel.selectAlbum(albumId)
                }
            )
        }

        // Queue Bottom Sheet
        if (isQueueSheetVisible) {
            QueueDrawerSheet(
                queue = queue,
                currentIndex = queueIndex,
                unplayableTrackIds = playbackState.unplayableTrackIds,
                onSelectIndex = { viewModel.playAll(queue, it) },
                onRemoveIndex = { viewModel.removeFromQueue(it) },
                onClearQueue = { viewModel.clearQueue() },
                onDismiss = { viewModel.setQueueSheetVisible(false) }
            )
        }

        // WLAN Receiver Speaker Cast Bottom Sheet
        if (isWlanCastSheetVisible) {
            WlanSpeakerCastSheet(
                viewModel = viewModel,
                onDismiss = { viewModel.hideWlanCastSheet() }
            )
        }

        // Listening Statistics Modal / Screen
        if (isStatsScreenVisible) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("listening_stats_dialog"),
                color = MaterialTheme.colorScheme.background
            ) {
                ListeningStatsScreen(
                    stats = listeningStats,
                    appLanguage = appLanguage,
                    onClearStats = { viewModel.clearListeningStats() },
                    onBack = { viewModel.setStatsScreenVisible(false) }
                )
            }
        }

        // GitHub In-App Update Dialog / Download Progress
        AppUpdateDialog(
            updateState = updateState,
            appLanguage = appLanguage,
            onDismiss = { viewModel.dismissAppUpdate() },
            onDownloadAndInstall = { viewModel.downloadAndInstallUpdate(it) }
        )

        // Exit Confirmation Dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = {
                    Text(
                        text = str("exit_dialog_title"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = str("exit_dialog_desc"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            (context as? Activity)?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(str("exit_dialog_confirm"))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExitDialog = false }
                    ) {
                        Text(str("exit_dialog_cancel"))
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun AppTopBar(
    serverConnected: Boolean,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseApp: () -> Unit,
    onOpenSidebar: (() -> Unit)? = null,
    isAstrolabeModeEnabled: Boolean = false,
    onToggleAstrolabeMode: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                if (onOpenSidebar != null) {
                    IconButton(
                        onClick = onOpenSidebar,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterStart)
                            .testTag("open_sidebar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = "ZANA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 4.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("app_brand_title")
                )

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleAstrolabeMode,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("toggle_astrolabe_mode_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.RotateRight,
                            contentDescription = if (isAstrolabeModeEnabled) "Switch to Classic Mode" else "Switch to Astrolabe Mode",
                            tint = if (isAstrolabeModeEnabled) com.labix.navirom.ui.radial.AstrolabeTheme.AstrolabeGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onCloseApp,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("close_app_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun TabContent(
    currentTab: NaviromTab,
    isAstrolabeModeEnabled: Boolean = false,
    onToggleAstrolabeMode: () -> Unit = {},
    librarySubTab: LibrarySubTab,
    onSubTabSelected: (LibrarySubTab) -> Unit,
    albums: List<com.labix.navirom.data.model.NaviromAlbum>,
    newestAlbums: List<com.labix.navirom.data.model.NaviromAlbum> = emptyList(),
    mostPlayedAlbums: List<com.labix.navirom.data.model.NaviromAlbum> = emptyList(),
    randomAlbums: List<com.labix.navirom.data.model.NaviromAlbum> = emptyList(),
    onRefreshRandomAlbums: () -> Unit = {},
    profileName: String = "",
    newestTracks: List<com.labix.navirom.data.model.NaviromTrack> = emptyList(),
    artists: List<com.labix.navirom.data.model.NaviromArtist>,
    playlists: List<com.labix.navirom.data.model.NaviromPlaylist>,
    quickMixTracks: List<com.labix.navirom.data.model.NaviromTrack>,
    recentlyPlayedTracks: List<com.labix.navirom.data.model.NaviromTrack> = emptyList(),
    librarySongs: List<com.labix.navirom.data.model.NaviromTrack> = emptyList(),
    songSortOrder: SongSortOrder = SongSortOrder.NAME,
    isSongSortReversed: Boolean = false,
    onSetSongSortOrder: (SongSortOrder) -> Unit = {},
    selectedAlbumId: String?,
    selectedAlbumTracks: List<com.labix.navirom.data.model.NaviromTrack>,
    selectedPlaylistId: String?,
    selectedPlaylistTracks: List<com.labix.navirom.data.model.NaviromTrack>,
    selectedArtistId: String? = null,
    currentArtist: com.labix.navirom.data.model.NaviromArtist? = null,
    currentArtistAlbums: List<com.labix.navirom.data.model.NaviromAlbum> = emptyList(),
    currentArtistSongs: List<com.labix.navirom.data.model.NaviromTrack> = emptyList(),
    isLoadingArtistDetails: Boolean = false,
    onSelectArtist: (String?) -> Unit = {},
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchFocusTrigger: Int = 0,
    isSearching: Boolean,
    searchResults: Triple<List<com.labix.navirom.data.model.NaviromArtist>, List<com.labix.navirom.data.model.NaviromAlbum>, List<com.labix.navirom.data.model.NaviromTrack>>,
    searchHistory: List<String> = emptyList(),
    onRemoveSearchHistory: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    cachedTracks: List<com.labix.navirom.data.local.CachedTrackEntity>,
    totalCacheSizeBytes: Long,
    isOfflineOnlyMode: Boolean,
    serverState: ServerConnectionUiState,
    allServers: List<com.labix.navirom.data.local.ServerConfigEntity>,
    favoriteIds: List<String>,
    favoriteTracks: List<com.labix.navirom.data.model.NaviromTrack> = emptyList(),
    downloadStatuses: Map<String, DownloadStatus>,
    downloadProgresses: Map<String, Float>,
    playbackState: com.labix.navirom.data.model.PlaybackState,
    appLanguage: AppLanguage,
    appThemeMode: AppThemeMode,
    statsSummary: com.labix.navirom.data.stats.ListeningStatsSummary = com.labix.navirom.data.stats.ListeningStatsSummary(),
    isCrossfadeEnabled: Boolean,
    crossfadeDurationSeconds: Int = 5,
    isVinylEffectEnabled: Boolean = false,
    isDualAudioEnabled: Boolean = false,
    wlanSpeakerState: WlanSpeakerState? = null,
    onOpenWlanCast: (() -> Unit)? = null,
    onSetCrossfadeEnabled: (Boolean) -> Unit,
    onSetCrossfadeDurationSeconds: (Int) -> Unit = {},
    onSetVinylEffectEnabled: (Boolean) -> Unit = {},
    onSetDualAudioEnabled: (Boolean) -> Unit = {},
    onViewStats: () -> Unit = {},
    onSetLanguage: (AppLanguage) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSelectAlbum: (String?) -> Unit,
    onSelectPlaylist: (String?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onTrackClick: (com.labix.navirom.data.model.NaviromTrack, List<com.labix.navirom.data.model.NaviromTrack>) -> Unit,
    onPlayAll: (List<com.labix.navirom.data.model.NaviromTrack>) -> Unit,
    onShuffleAll: (List<com.labix.navirom.data.model.NaviromTrack>) -> Unit,
    onSmartShuffle: ((List<com.labix.navirom.data.model.NaviromTrack>?) -> Unit)? = null,
    onPlayTracksNext: ((List<com.labix.navirom.data.model.NaviromTrack>) -> Unit)? = null,
    onAddTracksToQueue: ((List<com.labix.navirom.data.model.NaviromTrack>) -> Unit)? = null,
    onPlayFolderNext: ((String) -> Unit)? = null,
    onAddFolderToQueue: ((String) -> Unit)? = null,
    onDownloadTracks: (List<com.labix.navirom.data.model.NaviromTrack>) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadTrack: (com.labix.navirom.data.model.NaviromTrack) -> Unit,
    onPlayNext: (com.labix.navirom.data.model.NaviromTrack) -> Unit,
    onAddToQueue: (com.labix.navirom.data.model.NaviromTrack) -> Unit,
    onToggleOfflineOnly: (Boolean) -> Unit,
    onDeleteCachedTrack: (String) -> Unit,
    onClearAllCache: () -> Unit,
    onUpdateServerConfig: (String, String, String, String, String, Boolean, String) -> Unit,
    onConnectServer: () -> Unit,
    onSyncLibrary: () -> Unit,
    onScanNetwork: () -> Unit,
    onSelectMusicFolder: (String?) -> Unit = {},
    onToggleMusicFolder: (String) -> Unit = {},
    onSelectAllMusicFolders: () -> Unit = {},
    focusUsernameTrigger: Long = 0L,
    onSelectServer: (com.labix.navirom.data.local.ServerConfigEntity) -> Unit,
    onDeleteServer: (com.labix.navirom.data.local.ServerConfigEntity) -> Unit = {},
    onGoToSettings: () -> Unit,
    onOpenSidebar: () -> Unit,
    updateState: UpdateState = UpdateState.Idle,
    autoCheckUpdates: Boolean = true,
    githubRepo: String = "labibllaca/Zana-player",
    lastUpdateCheckedTime: Long = 0L,
    onCheckForUpdates: () -> Unit = {},
    onDownloadAndInstallUpdate: (AppUpdateInfo) -> Unit = {},
    onSetAutoCheckUpdates: (Boolean) -> Unit = {},
    onSetGithubRepo: (String) -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    localTracks: List<com.labix.navirom.data.model.NaviromTrack> = emptyList(),
    localFolders: List<com.labix.navirom.data.local.LocalMusicFolder> = emptyList(),
    disabledLocalFolderIds: Set<String> = emptySet(),
    onToggleLocalFolder: (String) -> Unit = {},
    onSetLocalFolderEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onSelectAllLocalFolders: () -> Unit = {},
    onDeselectAllLocalFolders: () -> Unit = {},
    allDiscoveredLocalFolders: List<com.labix.navirom.data.local.LocalMusicFolder> = emptyList(),
    settingsEnabledLocalFolderIds: Set<String> = emptySet(),
    onToggleSettingsLocalFolder: (String) -> Unit = {},
    onSetSettingsLocalFolderEnabled: (String, Boolean) -> Unit = { _, _ -> },
    onSelectAllSettingsLocalFolders: () -> Unit = {},
    onDeselectAllSettingsLocalFolders: () -> Unit = {},
    isScanningLocalAudio: Boolean = false,
    onScanLocalAudio: () -> Unit = {},
    currentMusicFolderPath: String = "",
    onNavigateToMusicFolder: (String) -> Unit = {},
    onNavigateUpMusicFolder: () -> Boolean = { false },
    onPlayFolder: (String, Boolean) -> Unit = { _, _ -> }
) {
    AnimatedContent(
        targetState = currentTab,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            (fadeIn(animationSpec = tween(220)) +
                slideInHorizontally(animationSpec = tween(220)) { width -> if (forward) width / 5 else -width / 5 })
                .togetherWith(
                    fadeOut(animationSpec = tween(160)) +
                    slideOutHorizontally(animationSpec = tween(160)) { width -> if (forward) -width / 5 else width / 5 }
                )
        },
        label = "MainTabTransition"
    ) { activeTab ->
        when (activeTab) {
            NaviromTab.LIBRARY -> {
            if (isAstrolabeModeEnabled && selectedAlbumId == null && selectedArtistId == null) {
                AstrolabeLibraryView(
                    artists = artists,
                    albums = albums,
                    librarySongs = librarySongs,
                    appLanguage = appLanguage,
                    onSelectAlbum = onSelectAlbum,
                    onTrackClick = onTrackClick,
                    onPlayAll = onPlayAll,
                    onToggleClassicView = onToggleAstrolabeMode
                )
            } else {
                LibraryScreen(
                    subTab = librarySubTab,
                    onSubTabSelected = onSubTabSelected,
                    albums = albums,
                    newestAlbums = newestAlbums,
                    mostPlayedAlbums = mostPlayedAlbums,
                    randomAlbums = randomAlbums,
                    onRefreshRandomAlbums = onRefreshRandomAlbums,
                    profileName = profileName,
                    newestTracks = newestTracks,
                    artists = artists,
                    quickMixTracks = quickMixTracks,
                    recentlyPlayedTracks = recentlyPlayedTracks,
                    librarySongs = librarySongs,
                    songSortOrder = songSortOrder,
                    isSongSortReversed = isSongSortReversed,
                    onSetSongSortOrder = onSetSongSortOrder,
                    selectedAlbumId = selectedAlbumId,
                    selectedAlbumTracks = selectedAlbumTracks,
                    currentTrack = playbackState.currentTrack,
                    isPlaying = playbackState.isPlaying,
                    downloadStatuses = downloadStatuses,
                    downloadProgresses = downloadProgresses,
                    favoriteIds = favoriteIds,
                    favoriteTracks = favoriteTracks,
                    appLanguage = appLanguage,
                    musicFolders = serverState.musicFolders,
                    selectedMusicFolderId = serverState.selectedMusicFolderId,
                    selectedMusicFolderIds = serverState.selectedMusicFolderIds,
                    onSelectMusicFolder = onSelectMusicFolder,
                    onToggleMusicFolder = onToggleMusicFolder,
                    onSelectAllMusicFolders = onSelectAllMusicFolders,
                    onSyncLibrary = onSyncLibrary,
                    onSelectAlbum = onSelectAlbum,
                    onTrackClick = onTrackClick,
                    onPlayAll = onPlayAll,
                    onShuffleAll = onShuffleAll,
                    onSmartShuffle = onSmartShuffle,
                    onPlayTracksNext = onPlayTracksNext,
                    onAddTracksToQueue = onAddTracksToQueue,
                    onDownloadTracks = onDownloadTracks,
                    onToggleFavorite = onToggleFavorite,
                    onDownloadTrack = onDownloadTrack,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    onGoToSettings = onGoToSettings,
                    onScanNetwork = onScanNetwork,
                    onOpenSidebar = onOpenSidebar,
                    selectedArtistId = selectedArtistId,
                    currentArtist = currentArtist,
                    currentArtistAlbums = currentArtistAlbums,
                    currentArtistSongs = currentArtistSongs,
                    isLoadingArtistDetails = isLoadingArtistDetails,
                    onSelectArtist = onSelectArtist,
                    localTracks = localTracks,
                    localFolders = localFolders,
                    disabledLocalFolderIds = disabledLocalFolderIds,
                    onToggleLocalFolder = onToggleLocalFolder,
                    onSetLocalFolderEnabled = onSetLocalFolderEnabled,
                    onSelectAllLocalFolders = onSelectAllLocalFolders,
                    onDeselectAllLocalFolders = onDeselectAllLocalFolders,
                    isScanningLocalAudio = isScanningLocalAudio,
                    onScanLocalAudio = onScanLocalAudio
                )
            }
        }
        NaviromTab.PLAYLISTS -> {
            PlaylistsScreen(
                playlists = playlists,
                selectedPlaylistId = selectedPlaylistId,
                selectedPlaylistTracks = selectedPlaylistTracks,
                currentTrack = playbackState.currentTrack,
                isPlaying = playbackState.isPlaying,
                downloadStatuses = downloadStatuses,
                downloadProgresses = downloadProgresses,
                favoriteIds = favoriteIds,
                appLanguage = appLanguage,
                onSelectPlaylist = onSelectPlaylist,
                onCreatePlaylist = onCreatePlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onTrackClick = onTrackClick,
                onPlayAll = onPlayAll,
                onShuffleAll = onShuffleAll,
                onSmartShuffle = { onSmartShuffle?.invoke(it) ?: onShuffleAll(it) },
                onPlayTracksNext = onPlayTracksNext,
                onAddTracksToQueue = onAddTracksToQueue,
                onDownloadPlaylist = onDownloadTracks,
                onToggleFavorite = onToggleFavorite,
                onDownloadTrack = onDownloadTrack,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue
            )
        }
        NaviromTab.SEARCH -> {
            if (isAstrolabeModeEnabled) {
                AstrolabeSearchScreen(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    searchResults = searchResults,
                    allSongs = librarySongs,
                    allAlbums = albums,
                    allArtists = artists,
                    appLanguage = appLanguage,
                    onTrackClick = onTrackClick,
                    onSelectAlbum = { onSelectAlbum(it) },
                    onToggleClassicView = onToggleAstrolabeMode
                )
            } else {
                SearchScreen(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    isSearching = isSearching,
                    searchResults = searchResults,
                    searchHistory = searchHistory,
                    onRemoveSearchHistory = onRemoveSearchHistory,
                    onClearSearchHistory = onClearSearchHistory,
                    currentTrack = playbackState.currentTrack,
                    isPlaying = playbackState.isPlaying,
                    downloadStatuses = downloadStatuses,
                    downloadProgresses = downloadProgresses,
                    favoriteIds = favoriteIds,
                    appLanguage = appLanguage,
                    onSelectAlbum = { onSelectAlbum(it) },
                    onTrackClick = onTrackClick,
                    onToggleFavorite = onToggleFavorite,
                    onDownloadTrack = onDownloadTrack,
                    onPlayNext = onPlayNext,
                    onAddToQueue = onAddToQueue,
                    searchFocusTrigger = searchFocusTrigger
                )
            }
        }
        NaviromTab.FOLDERS -> {
            MusicFoldersScreen(
                currentFolderPath = currentMusicFolderPath,
                allLocalTracks = localTracks,
                allDiscoveredFolders = allDiscoveredLocalFolders,
                isScanning = isScanningLocalAudio,
                playbackState = playbackState,
                appLanguage = appLanguage,
                onNavigateToFolder = onNavigateToMusicFolder,
                onNavigateUp = onNavigateUpMusicFolder,
                onRescanStorage = onScanLocalAudio,
                onPlayTrack = onTrackClick,
                onPlayFolder = onPlayFolder,
                onPlayFolderNext = onPlayFolderNext,
                onAddFolderToQueue = onAddFolderToQueue,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onToggleFavorite = { track -> onToggleFavorite(track.id) },
                favoriteIds = favoriteIds.toSet()
            )
        }
        NaviromTab.SETTINGS -> {
            if (isAstrolabeModeEnabled) {
                AstrolabeSettingsScreen(
                    serverState = serverState,
                    totalCacheSizeBytes = totalCacheSizeBytes,
                    appLanguage = appLanguage,
                    onConnectServer = onConnectServer,
                    onSyncLibrary = onSyncLibrary,
                    onClearAllCache = onClearAllCache,
                    onToggleClassicView = onToggleAstrolabeMode
                )
            } else {
                ServerSettingsScreen(
                    serverState = serverState,
                    allServers = allServers,
                    onSelectServer = onSelectServer,
                    onDeleteServer = onDeleteServer,
                    appLanguage = appLanguage,
                    appThemeMode = appThemeMode,
                    statsSummary = statsSummary,
                    isCrossfadeEnabled = isCrossfadeEnabled,
                    crossfadeDurationSeconds = crossfadeDurationSeconds,
                    isVinylEffectEnabled = isVinylEffectEnabled,
                    isDualAudioEnabled = isDualAudioEnabled,
                    wlanSpeakerState = wlanSpeakerState,
                    onOpenWlanCast = onOpenWlanCast,
                    onSetCrossfadeEnabled = onSetCrossfadeEnabled,
                    onSetCrossfadeDurationSeconds = onSetCrossfadeDurationSeconds,
                    onSetVinylEffectEnabled = onSetVinylEffectEnabled,
                    onSetDualAudioEnabled = onSetDualAudioEnabled,
                    onViewStats = onViewStats,
                    onSetLanguage = onSetLanguage,
                    onSetThemeMode = onSetThemeMode,
                    onUpdateConfig = onUpdateServerConfig,
                    onConnect = onConnectServer,
                    onSyncLibrary = onSyncLibrary,
                    onScanNetwork = onScanNetwork,
                    onSelectMusicFolder = onSelectMusicFolder,
                    onToggleMusicFolder = onToggleMusicFolder,
                    onSelectAllMusicFolders = onSelectAllMusicFolders,
                    allDiscoveredLocalFolders = allDiscoveredLocalFolders,
                    settingsEnabledLocalFolderIds = settingsEnabledLocalFolderIds,
                    onToggleSettingsLocalFolder = onToggleSettingsLocalFolder,
                    onSetSettingsLocalFolderEnabled = onSetSettingsLocalFolderEnabled,
                    onSelectAllSettingsLocalFolders = onSelectAllSettingsLocalFolders,
                    onDeselectAllSettingsLocalFolders = onDeselectAllSettingsLocalFolders,
                    isScanningLocalAudio = isScanningLocalAudio,
                    onScanLocalAudio = onScanLocalAudio,
                    focusUsernameTrigger = focusUsernameTrigger,
                    updateState = updateState,
                    autoCheckUpdates = autoCheckUpdates,
                    githubRepo = githubRepo,
                    lastUpdateCheckedTime = lastUpdateCheckedTime,
                    onCheckForUpdates = onCheckForUpdates,
                    onDownloadAndInstallUpdate = onDownloadAndInstallUpdate,
                    onSetAutoCheckUpdates = onSetAutoCheckUpdates,
                    onSetGithubRepo = onSetGithubRepo,
                    onDismissUpdate = onDismissUpdate
                )
            }
        }
    }
}
}
