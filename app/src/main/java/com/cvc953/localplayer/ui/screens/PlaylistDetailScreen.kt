package com.cvc953.localplayer.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.SongItem
import com.cvc953.localplayer.ui.components.DraggableSwipeRow
import com.cvc953.localplayer.ui.components.NativeSearchBar
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.ui.headers.PlaylistHeader
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import java.io.File
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlaylistDetailScreen(
    playlistViewModel: PlaylistViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistName: String,
    onBack: () -> Unit,
) {
    val songs by playlistViewModel.songs.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val context = LocalContext.current
    val appPrefs =
        remember {
            com.cvc953.localplayer.preferences
                .AppPrefs(context)
        }

    BackHandler { onBack() }

    val playlist = remember(playlists, playlistName) { playlists.find { it.name == playlistName } }

    var order by rememberSaveable(playlistName) { mutableStateOf(appPrefs.getPlaylistOrder(playlistName)) }
    LaunchedEffect(order) { appPrefs.setPlaylistOrder(playlistName, order) }

    val playlistSongs =
        remember(songs, playlist, order) {
            if (playlist != null) {
                val base = playlist.songIds.mapNotNull { id -> songs.find { it.id == id } }
                when (order) {
                    "AZ" -> base.sortedBy { it.title.lowercase() }
                    "ZA" -> base.sortedByDescending { it.title.lowercase() }
                    else -> base
                }
            } else {
                emptyList()
            }
        }

    val listState = rememberLazyListState()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val dragList =
        remember(playlistSongs) {
            mutableStateListOf<Song>().also { it.addAll(playlistSongs) }
        }
    val scope = rememberCoroutineScope()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }

    val addedNextMsg = stringResource(R.string.toast_added_next)
    val addedQueueEndMsg = stringResource(R.string.toast_added_queue_end)
    val removedFromPlaylistMsg = stringResource(R.string.toast_removed_from_playlist)
    val addedToPlaylist = stringResource(R.string.toast_added_to_playlist)

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null && playlist != null) {
            try {
                val input = context.contentResolver.openInputStream(uri)
                val dir = File(context.filesDir, "playlist_images")
                dir.mkdirs()
                val dest = File(dir, "${playlist.name}.jpg")
                input?.use { inputStream ->
                    dest.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                playlistViewModel.updatePlaylistImage(playlist.name, dest.toURI().toString())
                Toast.makeText(context, "Imagen de playlist actualizada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("PlaylistDetail", "Error saving image", e)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.action_go_back),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlistName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            var sortMenuExpanded by remember { mutableStateOf(false) }
            IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Cambiar imagen",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            IconButton(
                onClick = {
                    showSearchBar = !showSearchBar
                    if (!showSearchBar) searchQuery = ""
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.action_search),
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
            Box {
                IconButton(onClick = { sortMenuExpanded = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.action_sort),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false },
                    containerColor = MaterialTheme.extendedColors.surfaceSheet,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_by_playlist), color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            order = "PLAYLIST"
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_title_asc), color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            order = "AZ"
                            sortMenuExpanded = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_title_desc), color = MaterialTheme.colorScheme.onSurface) },
                        onClick = {
                            order = "ZA"
                            sortMenuExpanded = false
                        },
                    )
                    if (playlist?.imageUri != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        DropdownMenuItem(
                            text = { Text("Eliminar imagen", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                playlistViewModel.updatePlaylistImage(playlist.name, null)
                                sortMenuExpanded = false
                                Toast.makeText(context, "Imagen restablecida", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
            }
        }

        if (showSearchBar) {
            NativeSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = stringResource(R.string.search_songs_placeholder),
            )
        }

        val filteredDragList =
            remember(dragList, searchQuery) {
                val q = searchQuery.trim().lowercase()
                if (q.isEmpty()) {
                    dragList.toList()
                } else {
                    dragList.filter { song ->
                        song.title.lowercase().contains(q) ||
                            song.album.lowercase().contains(q) ||
                            song.artist.lowercase().contains(q)
                    }
                }
            }

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(filteredDragList, order) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { pos ->
                                val layoutInfo = listState.layoutInfo
                                val visibleY = pos.y.toInt()
                                val rawIndex =
                                    layoutInfo.visibleItemsInfo
                                        .firstOrNull { item ->
                                            visibleY >= item.offset &&
                                                visibleY <= item.offset + item.size
                                        }?.index
                                if (rawIndex != null && rawIndex >= 1) {
                                    val filteredIdx = rawIndex - 1
                                    if (filteredIdx < filteredDragList.size) {
                                        val song = filteredDragList[filteredIdx]
                                        draggingIndex =
                                            dragList.indexOfFirst { it.id == song.id }
                                    }
                                }
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y
                                val visibleY = change.position.y.toInt()
                                val edgeThresholdPx = 72
                                val autoScrollStepPx = 24f
                                val viewportStart = listState.layoutInfo.viewportStartOffset
                                val viewportEnd = listState.layoutInfo.viewportEndOffset

                                if (visibleY <= viewportStart + edgeThresholdPx) {
                                    scope.launch { listState.scrollBy(-autoScrollStepPx) }
                                } else if (visibleY >= viewportEnd - edgeThresholdPx) {
                                    scope.launch { listState.scrollBy(autoScrollStepPx) }
                                }

                                val from = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                val rawTarget =
                                    listState.layoutInfo.visibleItemsInfo
                                        .firstOrNull { item ->
                                            visibleY >= item.offset &&
                                                visibleY <= item.offset + item.size
                                        }?.index
                                if (rawTarget != null && rawTarget >= 1) {
                                    val targetFilteredIdx = rawTarget - 1
                                    if (targetFilteredIdx < filteredDragList.size) {
                                        val targetSong = filteredDragList[targetFilteredIdx]
                                        val targetDragIdx =
                                            dragList.indexOfFirst { it.id == targetSong.id }
                                        if (targetDragIdx >= 0 && targetDragIdx != from) {
                                            val item = dragList.removeAt(from)
                                            dragList.add(targetDragIdx, item)
                                            draggingIndex = targetDragIdx
                                            dragOffset = 0f
                                        }
                                    }
                                }
                            },
                            onDragEnd = {
                                draggingIndex = null
                                dragOffset = 0f
                                if (order != "PLAYLIST") {
                                    order = "PLAYLIST"
                                }
                                playlistViewModel.reorderPlaylistSongs(
                                    playlistName,
                                    dragList.map { it.id },
                                )
                            },
                            onDragCancel = {
                                draggingIndex = null
                                dragOffset = 0f
                            },
                        )
                    },
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                PlaylistHeader(
                    playlist = playlist,
                    songs = songs,
                    context = context,
                    playlistSongs = playlistSongs,
                    playbackViewModel = playbackViewModel,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            itemsIndexed(filteredDragList, key = { _, song -> song.id }) { _, song ->
                val actualIndex = dragList.indexOfFirst { it.id == song.id }
                if (actualIndex < 0) return@itemsIndexed
                val isDragging = draggingIndex == actualIndex
                val isCurrent = playerState.currentSong?.id == song.id
                val offsetDp =
                    with(LocalDensity.current) {
                        dragOffset.toDp()
                    }

                DraggableSwipeRow(
                    onSwipeThreshold = {
                        playbackViewModel.addToQueueNext(song)
                        Toast.makeText(context, addedNextMsg, Toast.LENGTH_SHORT).show()
                    },
                    onSwipeLeftThreshold = {
                        playbackViewModel.addToQueueEnd(song)
                        Toast.makeText(context, addedQueueEndMsg, Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .offset(y = if (isDragging) offsetDp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SongItem(
                                song = song,
                                isPlaying = isCurrent,
                                onClick = {
                                    playbackViewModel.updateDisplayOrder(dragList.toList())
                                    playbackViewModel.play(song)
                                },
                                onQueueNext = {
                                    playbackViewModel.addToQueueNext(song)
                                    Toast.makeText(context, addedNextMsg, Toast.LENGTH_SHORT).show()
                                },
                                onQueueEnd = {
                                    playbackViewModel.addToQueueEnd(song)
                                    Toast.makeText(context, addedQueueEndMsg, Toast.LENGTH_SHORT).show()
                                },
                                playlists = playlists,
                                onAddToPlaylist = { targetPlaylistName, songId ->
                                    playlistViewModel.addSongToPlaylist(targetPlaylistName, songId)
                                    Toast.makeText(context, addedToPlaylist, Toast.LENGTH_SHORT).show()
                                },
                                onRemoveFromPlaylist = {
                                    playlistViewModel.removeSongFromPlaylist(
                                        playlistName,
                                        song.id,
                                    )
                                    Toast.makeText(context, removedFromPlaylistMsg, Toast.LENGTH_SHORT).show()
                                },
                            )
                        }
                        Icon(
                            Icons.Default.DragHandle,
                            contentDescription = stringResource(R.string.action_reorder),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier =
                                Modifier
                                    .padding(start = 4.dp)
                                    .size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
