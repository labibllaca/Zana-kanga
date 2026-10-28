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
import androidx.compose.ui.graphics.luminance
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
    val userName by viewModel.userName.collectAsStateWithLifecycle()

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
        val pagerState = rememberPagerState(initialPage = 0) { 2 }
        var libraryTotalItems by remember { mutableIntStateOf(1) }
        var libraryActiveIndex by remember { mutableIntStateOf(0) }
        var libraryActiveFilter by remember { mutableStateOf(LibraryFilterType.ALL) }
        var librarySourceFilter by remember { mutableStateOf(SourceFilter.ALL) }

        LaunchedEffect(currentTab) {
            val targetPage = when (currentTab) {
                NaviromTab.SETTINGS -> 1
                else -> 0
            }
            if (pagerState.currentPage != targetPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }

        LaunchedEffect(pagerState.currentPage) {
            when (pagerState.currentPage) {
                0 -> if (currentTab == NaviromTab.SETTINGS) viewModel.setTab(NaviromTab.LIBRARY)
                1 -> if (currentTab != NaviromTab.SETTINGS) viewModel.setTab(NaviromTab.SETTINGS)
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                // Drawer content
            }
        ) {
            Scaffold(
                topBar = {
                    AppTopBar(
                        serverConnected = serverState.isConnected,
                        onOpenSearch = { viewModel.setTab(NaviromTab.SEARCH) },
                        onOpenSettings = { viewModel.setTab(NaviromTab.SETTINGS) },
                        onCloseApp = { showExitDialog = true },
                        onOpenSidebar = { scope.launch { drawerState.open() } },
                        isAstrolabeModeEnabled = isAstrolabeModeEnabled,
                        onToggleAstrolabeMode = { viewModel.toggleAstrolabeMode() }
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background)) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> {
                                TabContent(
                                    viewModel = viewModel,
                                    currentTab = if (currentTab == NaviromTab.SETTINGS) NaviromTab.LIBRARY else currentTab,
                                    onOpenSidebar = { scope.launch { drawerState.open() } },
                                    libraryActiveIndex = libraryActiveIndex,
                                    onLibraryActiveIndexChange = { libraryActiveIndex = it },
                                    libraryTotalItems = libraryTotalItems,
                                    onLibraryTotalItemsChange = { libraryTotalItems = it },
                                    libraryActiveFilter = libraryActiveFilter,
                                    onLibraryActiveFilterChange = { libraryActiveFilter = it },
                                    librarySourceFilter = librarySourceFilter,
                                    onLibrarySourceFilterChange = { librarySourceFilter = it }
                                )
                            }
                            1 -> {
                                TabContent(
                                    viewModel = viewModel,
                                    currentTab = NaviromTab.SETTINGS,
                                    onOpenSidebar = { scope.launch { drawerState.open() } },
                                    libraryActiveIndex = libraryActiveIndex,
                                    onLibraryActiveIndexChange = { libraryActiveIndex = it },
                                    libraryTotalItems = libraryTotalItems,
                                    onLibraryTotalItemsChange = { libraryTotalItems = it },
                                    libraryActiveFilter = libraryActiveFilter,
                                    onLibraryActiveFilterChange = { libraryActiveFilter = it },
                                    librarySourceFilter = librarySourceFilter,
                                    onLibrarySourceFilterChange = { librarySourceFilter = it }
                                )
                            }
                        }
                    }

                    UnifiedAstrolabeWheel(
                        pagerState = pagerState,
                        isAstrolabeModeEnabled = isAstrolabeModeEnabled,
                        playbackState = playbackState,
                        currentTrack = queue.getOrNull(queueIndex) ?: playbackState.currentTrack,
                        queue = queue,
                        queueIndex = queueIndex,
                        libraryTotalItems = libraryTotalItems,
                        libraryActiveIndex = libraryActiveIndex,
                        onLibraryIndexChanged = { libraryActiveIndex = it },
                        onPreviousTrack = { viewModel.previous() },
                        onNextTrack = { viewModel.next() },
                        onSeekTo = { viewModel.seekTo(it) },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onOpenOptionsModal = { viewModel.setFullPlayerVisible(true) },
                        isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Persistent MiniPlayerBar
                    if (playbackState.currentTrack != null && !isFullPlayerVisible) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(bottom = 6.dp)
                        ) {
                            MiniPlayerBar(
                                playbackState = playbackState,
                                onExpandPlayer = { viewModel.setFullPlayerVisible(true) },
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onNext = { viewModel.next() },
                                onPrevious = { viewModel.previous() }
                            )
                        }
                    }
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
fun AppTopBar(
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
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = "ZANA",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
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
                            tint = MaterialTheme.colorScheme.onBackground,
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
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun TabContent(
    viewModel: NaviromViewModel,
    currentTab: NaviromTab,
    onOpenSidebar: () -> Unit = {},
    libraryActiveIndex: Int = 0,
    onLibraryActiveIndexChange: (Int) -> Unit = {},
    libraryTotalItems: Int = 1,
    onLibraryTotalItemsChange: (Int) -> Unit = {},
    libraryActiveFilter: LibraryFilterType = LibraryFilterType.ALL,
    onLibraryActiveFilterChange: (LibraryFilterType) -> Unit = {},
    librarySourceFilter: SourceFilter = SourceFilter.ALL,
    onLibrarySourceFilterChange: (SourceFilter) -> Unit = {}
) {
    val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val isCrossfadeEnabled by viewModel.isCrossfadeEnabled.collectAsStateWithLifecycle()
    val crossfadeDurationSeconds by viewModel.crossfadeDurationSeconds.collectAsStateWithLifecycle()
    val isVinylEffectEnabled by viewModel.isVinylEffectEnabled.collectAsStateWithLifecycle()
    val isAstrolabeModeEnabled by viewModel.isAstrolabeModeEnabled.collectAsStateWithLifecycle()
    val focusUsernameTrigger by viewModel.focusUsernameTrigger.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

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

    val listeningStats by viewModel.listeningStats.collectAsStateWithLifecycle()
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
    val downloadStatuses by viewModel.downloadStatuses.collectAsStateWithLifecycle()
    val downloadProgresses by viewModel.downloadProgresses.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()

    val wlanSpeakerState by viewModel.wlanSpeakerState.collectAsStateWithLifecycle()
    val isDualAudioEnabled by viewModel.isDualAudioEnabled.collectAsStateWithLifecycle()

    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val autoCheckUpdates by viewModel.autoCheckUpdates.collectAsStateWithLifecycle()
    val updateGithubRepo by viewModel.updateGithubRepo.collectAsStateWithLifecycle()
    val lastUpdateCheckedTime by viewModel.lastUpdateCheckedTime.collectAsStateWithLifecycle()

    val localTracks by viewModel.localTracks.collectAsStateWithLifecycle()
    val localFolders by viewModel.localFolders.collectAsStateWithLifecycle()
    val disabledLocalFolderIds by viewModel.disabledLocalFolderIds.collectAsStateWithLifecycle()
    val allDiscoveredLocalFolders by viewModel.allDiscoveredLocalFolders.collectAsStateWithLifecycle()
    val settingsEnabledLocalFolderIds by viewModel.settingsEnabledLocalFolderIds.collectAsStateWithLifecycle()
    val isScanningLocalAudio by viewModel.isScanningLocalAudio.collectAsStateWithLifecycle()
    val currentMusicFolderPath by viewModel.currentMusicFolderPath.collectAsStateWithLifecycle()

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
                if (isAstrolabeModeEnabled && selectedAlbumId == null && selectedArtistId == null && selectedPlaylistId == null) {
                    AstrolabeLibraryView(
                        artists = artists,
                        albums = albums,
                        librarySongs = librarySongs,
                        playlists = playlists,
                        appLanguage = appLanguage,
                        onSelectAlbum = { viewModel.selectAlbum(it) },
                        onSelectPlaylist = { viewModel.selectPlaylist(it) },
                        onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                        onPlayAll = { viewModel.playAll(it) },
                        onToggleClassicView = { viewModel.toggleAstrolabeMode() },
                        userName = userName,
                        hasExternalWheel = true,
                        externalActiveListIndex = libraryActiveIndex,
                        onExternalActiveIndexChange = onLibraryActiveIndexChange,
                        onTotalItemsCountChange = onLibraryTotalItemsChange,
                        externalActiveFilter = libraryActiveFilter,
                        onActiveFilterChange = onLibraryActiveFilterChange,
                        externalSourceFilter = librarySourceFilter,
                        onSourceFilterChange = onLibrarySourceFilterChange
                    )
                } else {
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
                        onGoToSettings = { viewModel.setTab(NaviromTab.SETTINGS) },
                        onScanNetwork = { viewModel.startAutoScanAndFocusUsername() },
                        onOpenSidebar = onOpenSidebar,
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
            }
            NaviromTab.PLAYLISTS -> {
                PlaylistsScreen(
                    playlists = playlists,
                    selectedPlaylistId = selectedPlaylistId,
                    selectedPlaylistTracks = currentPlaylistTracks,
                    currentTrack = playbackState.currentTrack,
                    isPlaying = playbackState.isPlaying,
                    downloadStatuses = downloadStatuses,
                    downloadProgresses = downloadProgresses,
                    favoriteIds = favoriteIds,
                    appLanguage = appLanguage,
                    onSelectPlaylist = { viewModel.selectPlaylist(it) },
                    onCreatePlaylist = { viewModel.createPlaylist(it) },
                    onDeletePlaylist = { viewModel.deletePlaylist(it) },
                    onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                    onPlayAll = { viewModel.playAll(it) },
                    onShuffleAll = { viewModel.shuffleAll(it) },
                    onSmartShuffle = { viewModel.smartShuffle(it) },
                    onPlayTracksNext = { viewModel.playTracksNext(it) },
                    onAddTracksToQueue = { viewModel.addTracksToQueue(it) },
                    onDownloadPlaylist = { viewModel.downloadTracks(it) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onDownloadTrack = { viewModel.downloadTrack(it) },
                    onPlayNext = { viewModel.playNext(it) },
                    onAddToQueue = { viewModel.addToQueue(it) }
                )
            }
            NaviromTab.SEARCH -> {
                if (isAstrolabeModeEnabled) {
                    AstrolabeSearchScreen(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        searchResults = searchResults,
                        allSongs = librarySongs,
                        allAlbums = albums,
                        allArtists = artists,
                        appLanguage = appLanguage,
                        onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                        onSelectAlbum = { viewModel.selectAlbum(it) },
                        onToggleClassicView = { viewModel.toggleAstrolabeMode() }
                    )
                } else {
                    SearchScreen(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        isSearching = isSearching,
                        searchResults = searchResults,
                        searchHistory = searchHistory,
                        onRemoveSearchHistory = { viewModel.removeSearchHistoryItem(it) },
                        onClearSearchHistory = { viewModel.clearSearchHistory() },
                        currentTrack = playbackState.currentTrack,
                        isPlaying = playbackState.isPlaying,
                        downloadStatuses = downloadStatuses,
                        downloadProgresses = downloadProgresses,
                        favoriteIds = favoriteIds,
                        appLanguage = appLanguage,
                        onSelectAlbum = { viewModel.selectAlbum(it) },
                        onTrackClick = { track, list -> viewModel.playTrack(track, list) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDownloadTrack = { viewModel.downloadTrack(it) },
                        onPlayNext = { viewModel.playNext(it) },
                        onAddToQueue = { viewModel.addToQueue(it) },
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
                    onNavigateToFolder = { viewModel.navigateToMusicFolder(it) },
                    onNavigateUp = { viewModel.navigateUpMusicFolder() },
                    onRescanStorage = { viewModel.scanLocalAudio() },
                    onPlayTrack = { track, list -> viewModel.playTrack(track, list) },
                    onPlayFolder = { path, shuffle -> viewModel.playFolder(path, shuffle) },
                    onPlayFolderNext = { viewModel.playFolderNext(it) },
                    onAddFolderToQueue = { viewModel.addFolderToQueue(it) },
                    onPlayNext = { viewModel.playNext(it) },
                    onAddToQueue = { viewModel.addToQueue(it) },
                    onToggleFavorite = { track -> viewModel.toggleFavorite(track.id) },
                    favoriteIds = favoriteIds.toSet()
                )
            }
            NaviromTab.SETTINGS -> {
                if (isAstrolabeModeEnabled) {
                    AstrolabeSettingsScreen(
                        serverState = serverState,
                        totalCacheSizeBytes = totalCacheSizeBytes,
                        appLanguage = appLanguage,
                        appThemeMode = appThemeMode,
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        onConnectServer = { viewModel.connectServer() },
                        onSyncLibrary = { viewModel.syncLibrary() },
                        onClearAllCache = { viewModel.clearAllCache() },
                        onToggleClassicView = { viewModel.toggleAstrolabeMode() },
                        userName = userName,
                        onSetUserName = { viewModel.setUserName(it) }
                    )
                } else {
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
                        onUpdateConfig = { p, h, po, u, pw, t, ah -> viewModel.updateServerConfig(p, h, po, u, pw, t, ah) },
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
}
