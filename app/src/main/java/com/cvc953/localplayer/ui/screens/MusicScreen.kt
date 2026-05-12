package com.cvc953.localplayer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.cvc953.localplayer.R
import androidx.compose.ui.zIndex
import com.cvc953.localplayer.ui.SongItem
import com.cvc953.localplayer.ui.components.AlphabetScrollerContent
import com.cvc953.localplayer.ui.components.DraggableSwipeRow
import com.cvc953.localplayer.ui.components.ScrollLetterDisplay
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:function-naming")
@Composable
fun SongsContent(
    songViewModel: SongViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistViewModel: PlaylistViewModel,
    playerViewModel: PlayerViewModel,
) {
    val songs by songViewModel.songs.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.TITLE_ASC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    // About visibility moved to ViewModel so shell UI can react
    var menuExpanded by remember { mutableStateOf(false) }
    val showAbout by playerViewModel.isAboutVisible.collectAsState()
    val filteredSongs =
        remember(songs, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) {
                songs
            } else {
                songs.filter { song ->
                    song.title.lowercase().contains(q) ||
                        song.artist.lowercase().contains(q)
                }
            }
        }

    val sortedSongs =
        remember(filteredSongs, sortMode) {
            when (sortMode) {
                SortMode.TITLE_ASC -> {
                    filteredSongs.sortedBy { it.title.lowercase() }
                }

                SortMode.TITLE_DESC -> {
                    filteredSongs.sortedByDescending { it.title.lowercase() }
                }

                SortMode.ARTIST_ASC -> {
                    filteredSongs.sortedBy { it.artist.lowercase() }
                }
            }
        }

    // val playerState by playerViewModel.playerState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var currentScrollLetter by remember { mutableStateOf<String?>(null) }
    var selectedSongIds by remember { mutableStateOf(emptySet<Long>()) }
    val isSelectionMode = selectedSongIds.isNotEmpty()

    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    val isScanning by songViewModel.isScanning.collectAsState()

    if (isScanning) {
        Column(
            modifier =
                Modifier.Companion
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(stringResource(R.string.scanning_songs), color = MaterialTheme.colorScheme.onSurface)
        }
    } else {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.songs_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )

                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(
                                Icons.Default.Sort,
                                contentDescription = stringResource(R.string.action_sort),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = {
                                sortMenuExpanded = false
                            },
                            containerColor = MaterialTheme.extendedColors.surfaceSheet,
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.sort_title_asc),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    sortMode =
                                        SortMode.TITLE_ASC
                                    sortMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.sort_title_desc),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    sortMode =
                                        SortMode.TITLE_DESC
                                    sortMenuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.sort_artist_asc),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    sortMode =
                                        SortMode.ARTIST_ASC
                                    sortMenuExpanded = false
                                },
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) searchQuery = ""
                        },
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.action_search),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.action_more_options),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier =
                                Modifier.Companion.background(
                                    MaterialTheme.extendedColors.surfaceSheet,
                                ),
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.action_refresh_library),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    songViewModel.manualRefreshLibrary()
                                    menuExpanded = false
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.settings_title),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    // Redirigir a navegación de settings si es necesario
                                    menuExpanded = false
                                    playerViewModel.showSettings(true)
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(R.string.action_about),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                },
                                onClick = {
                                    // Redirigir a navegación de about si es necesario
                                    menuExpanded = false
                                    playerViewModel.showAbout(true)
                                },
                            )
                        }
                    }
                }

                // Top action bar for selected songs
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { selectedSongIds = emptySet() }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Cancelar selección",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    text = "${selectedSongIds.size} ${stringResource(R.string.songs_selected)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(0.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = { showPlaylistDialog = true }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = stringResource(R.string.add_to_playlist),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val favoritesName = "Favoritos"
                                        var favorites = playlists.find { it.name == favoritesName }
                                        if (favorites == null) {
                                            playlistViewModel.createPlaylist(favoritesName)
                                        }
                                        selectedSongIds.forEach { songId ->
                                            playlistViewModel.addSongToPlaylist(favoritesName, songId)
                                        }
                                        Toast.makeText(context, context.getString(R.string.toast_added_to_playlist, favoritesName), Toast.LENGTH_SHORT).show()
                                        selectedSongIds = emptySet()
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = stringResource(R.string.action_add_to_favorites),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val songList = selectedSongIds.mapNotNull { id -> sortedSongs.firstOrNull { it.id == id } }
                                        if (songList.isNotEmpty()) {
                                            playbackViewModel.addToQueueNextAll(songList)
                                            Toast.makeText(context, context.getString(R.string.toast_added_next), Toast.LENGTH_SHORT).show()
                                        }
                                        selectedSongIds = emptySet()
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = stringResource(R.string.action_add_next),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val songList = selectedSongIds.mapNotNull { id -> sortedSongs.firstOrNull { it.id == id } }
                                        if (songList.isNotEmpty()) {
                                            playbackViewModel.addToQueueEndAll(songList)
                                            Toast.makeText(context, context.getString(R.string.toast_added_queue_end), Toast.LENGTH_SHORT).show()
                                        }
                                        selectedSongIds = emptySet()
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.SkipPrevious,
                                        contentDescription = stringResource(R.string.action_add_to_queue_end),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = {
                            Text(
                                stringResource(R.string.search_songs_placeholder),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        },
                        modifier =
                            Modifier.Companion
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceVariant,
                                unfocusedContainerColor =
                                    MaterialTheme.colorScheme.surfaceVariant,
                                focusedIndicatorColor =
                                    MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor =
                                    MaterialTheme.colorScheme.outline,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                focusedLabelColor =
                                    MaterialTheme.colorScheme.primary,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription =
                                            stringResource(R.string.action_clear_search),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        },
                    )
                }
                Box(modifier = Modifier.Companion.fillMaxSize()) {
                    // Lista de canciones
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.Companion.fillMaxSize(),
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                bottom = 16.dp,
                                end = 4.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedSongs) { song ->
                            val isCurrent =
                                playerState.currentSong?.id == song.id

                            DraggableSwipeRow(
                                onSwipeThreshold = {
                                    playbackViewModel.addToQueueNext(song)
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.toast_added_next),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                },
                                onSwipeLeftThreshold = {
                                    playbackViewModel.addToQueueEnd(song)
                                    Toast
                                        .makeText(
                                            context,
                                            context.getString(R.string.toast_added_queue_end),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                },
                            ) {
                                SongItem(
                                    song = song,
                                    isPlaying =
                                        isCurrent &&
                                            playerState
                                                .isPlaying,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = selectedSongIds.contains(song.id),
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedSongIds = if (selectedSongIds.contains(song.id)) {
                                                selectedSongIds - song.id
                                            } else {
                                                selectedSongIds + song.id
                                            }
                                        } else {
                                            playbackViewModel.updateDisplayOrder(sortedSongs)
                                            playbackViewModel.play(song)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            selectedSongIds = setOf(song.id)
                                        } else {
                                            selectedSongIds = if (selectedSongIds.contains(song.id)) {
                                                selectedSongIds - song.id
                                            } else {
                                                selectedSongIds + song.id
                                            }
                                        }
                                    },
                                    onQueueNext = {
                                        playbackViewModel.addToQueueNext(song)
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.toast_added_next),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                    onQueueEnd = {
                                        playbackViewModel.addToQueueEnd(song)
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.toast_added_queue_end),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                    playlists = playlists,
                                    onAddToPlaylist = { playlistName, songId ->
                                        playlistViewModel.addSongToPlaylist(playlistName, songId)
                                        Toast
                                            .makeText(
                                                context,
                                                context.getString(R.string.toast_added_to_playlist, playlistName),
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                    },
                                )
                            }
                        }
                    }
                    // Barra de scroll alfabético
                    if (sortMode == SortMode.TITLE_ASC ||
                        sortMode == SortMode.TITLE_DESC ||
                        sortMode == SortMode.ARTIST_ASC
                    ) {
                        AlphabetScrollerContent(
                            items = sortedSongs,
                            getItemName = { song ->
                                when (sortMode) {
                                    SortMode.ARTIST_ASC -> song.artist
                                    else -> song.title
                                }
                            },
                            currentScrollLetter = currentScrollLetter,
                            onLetterSelected = { letter ->
                                currentScrollLetter = letter
                            },
                            onScrollToIndex = { index, _ ->
                                scope.launch {
                                    listState.scrollToItem(index)
                                }
                            },
                            viewAsGrid = false,
                            scope = scope,
                        )
                    }

                    // Overlay de letra grande para feedback
                    currentScrollLetter?.let { letter ->
                        ScrollLetterDisplay(letter = letter)
                    }
                }
            }

            if (showPlaylistDialog) {
                AlertDialog(
                    onDismissRequest = { showPlaylistDialog = false },
                    containerColor = MaterialTheme.extendedColors.surfaceSheet,
                    title = { Text(stringResource(R.string.add_to_playlist_title), color = MaterialTheme.colorScheme.onSurface) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Button(
                                    onClick = { showCreatePlaylistDialog = true },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                                ) {
                                    Text(stringResource(R.string.create_playlist_button))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (showCreatePlaylistDialog) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showCreatePlaylistDialog = false
                                        newPlaylistName = ""
                                    },
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    title = { Text(stringResource(R.string.dialog_create_playlist_title), color = MaterialTheme.colorScheme.onBackground) },
                                    text = {
                                        OutlinedTextField(
                                            value = newPlaylistName,
                                            onValueChange = { newPlaylistName = it },
                                            singleLine = true,
                                            placeholder = { Text(stringResource(R.string.playlist_name_placeholder)) },
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                                unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                                cursorColor = MaterialTheme.colorScheme.primary,
                                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                            ),
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                if (newPlaylistName.isNotBlank()) {
                                                    playlistViewModel.createPlaylist(newPlaylistName)
                                                    selectedSongIds.forEach { songId ->
                                                        playlistViewModel.addSongToPlaylist(newPlaylistName, songId)
                                                    }
                                                    Toast.makeText(context, context.getString(R.string.playlist_created_and_added, newPlaylistName), Toast.LENGTH_SHORT).show()
                                                    newPlaylistName = ""
                                                    showCreatePlaylistDialog = false
                                                    showPlaylistDialog = false
                                                    selectedSongIds = emptySet()
                                                }
                                            },
                                        ) {
                                            Text(stringResource(R.string.action_create), color = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                            Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.onBackground)
                                        }
                                    },
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(playlists) { playlist ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable {
                                                selectedSongIds.forEach { songId ->
                                                    playlistViewModel.addSongToPlaylist(playlist.name, songId)
                                                }
                                                Toast.makeText(context, context.getString(R.string.song_added_to_playlist, playlist.name), Toast.LENGTH_SHORT).show()
                                                showPlaylistDialog = false
                                                selectedSongIds = emptySet()
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.extendedColors.surfaceSheet,
                                        ),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = playlist.name,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    fontSize = 14.sp,
                                                )
                                                Text(
                                                    text = stringResource(R.string.songs_count, playlist.songIds.size),
                                                    color = MaterialTheme.extendedColors.textSecondarySoft,
                                                    fontSize = 12.sp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showPlaylistDialog = false }) {
                            Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.primary)
                        }
                    },
                )
            }

            if (showAbout) {
                Box(modifier = Modifier.Companion.fillMaxSize().zIndex(2f)) {
                    AboutScreen(onBack = { playerViewModel.showAbout(false) })
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return "%02d:%02d".format(minutes, seconds)
}

private enum class SortMode {
    TITLE_ASC,
    TITLE_DESC,
    ARTIST_ASC,
}

private fun SortMode.next(): SortMode =
    when (this) {
        SortMode.TITLE_ASC -> SortMode.TITLE_DESC
        SortMode.TITLE_DESC -> SortMode.ARTIST_ASC
        SortMode.ARTIST_ASC -> SortMode.TITLE_ASC
    }
