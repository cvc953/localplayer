package com.cvc953.localplayer.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.viewmodel.MainViewModel

@Composable
fun PlaylistsScreen(viewModel: MainViewModel, onPlaylistClick: (String) -> Unit) {
    val isScanning by viewModel.isScanning
    val playlists: List<Playlist> by viewModel.playlists.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(PlaylistSortMode.TITLE_ASC) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var createError by rememberSaveable { mutableStateOf<String?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity

    BackHandler { activity?.moveTaskToBack(true) }

    val filteredPlaylists =
            remember(playlists, searchQuery) {
                val q = searchQuery.trim().lowercase()
                if (q.isEmpty()) playlists else playlists.filter { it.name.lowercase().contains(q) }
            }

    val sortedPlaylists =
            remember(filteredPlaylists, sortMode) {
                when (sortMode) {
                    PlaylistSortMode.TITLE_ASC -> filteredPlaylists.sortedBy { it.name.lowercase() }
                    PlaylistSortMode.TITLE_DESC ->
                            filteredPlaylists.sortedByDescending { it.name.lowercase() }
                }
            }

    if (isScanning) {
        Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Escaneando canciones", color = Color.White)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = "Listas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color.White)
                    }
                    DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            containerColor = Color(0xFF1A1A1A)
                    ) {
                        DropdownMenuItem(
                                text = { Text("Título A-Z", color = Color.White) },
                                onClick = {
                                    sortMode = PlaylistSortMode.TITLE_ASC
                                    sortMenuExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("Título Z-A", color = Color.White) },
                                onClick = {
                                    sortMode = PlaylistSortMode.TITLE_DESC
                                    sortMenuExpanded = false
                                }
                        )
                    }
                }

                IconButton(
                        onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) searchQuery = ""
                        }
                ) { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White) }
            }

            if (showSearchBar) {
                OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Buscar por lista", color = Color(0xFF808080)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1A1A1A),
                                        unfocusedContainerColor = Color(0xFF1A1A1A),
                                        focusedIndicatorColor = Color(0xFF2196F3),
                                        unfocusedIndicatorColor = Color(0xFF404040),
                                        cursorColor = Color(0xFF2196F3),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF2196F3),
                                        unfocusedLabelColor = Color(0xFF808080)
                                )
                )
            }

            Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
            ) { Button(onClick = { showCreateDialog = true }) { Text("Crear lista") } }

            if (sortedPlaylists.isEmpty()) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "No hay listas por ahora", color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showCreateDialog = true }) { Text("Crear lista") }
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                                PaddingValues(
                                        start = 16.dp,
                                        top = 16.dp,
                                        bottom = 16.dp,
                                        end = 4.dp
                                ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedPlaylists) { playlist ->
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                    modifier =
                                            Modifier.weight(1f).clickable {
                                                onPlaylistClick(playlist.name)
                                            }
                            ) {
                                Text(text = playlist.name, color = Color.White, fontSize = 16.sp)
                                Text(
                                        text = "${playlist.songIds.size} canciones",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { playlistToDelete = playlist }) {
                                Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = Color(0xFFFF6B6B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (playlistToDelete != null) {
        val target = playlistToDelete
        AlertDialog(
                onDismissRequest = { playlistToDelete = null },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Eliminar lista", color = Color.White) },
                text = {
                    Text(text = "Se eliminara la lista \"${target?.name}\".", color = Color.Gray)
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                target?.name?.let { name -> viewModel.deletePlaylist(name) }
                                playlistToDelete = null
                            }
                    ) { Text("Eliminar", color = Color(0xFFFF6B6B)) }
                },
                dismissButton = {
                    TextButton(onClick = { playlistToDelete = null }) {
                        Text("Cancelar", color = Color.White)
                    }
                }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
                onDismissRequest = {
                    showCreateDialog = false
                    newPlaylistName = ""
                    createError = null
                },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Nueva lista", color = Color.White) },
                text = {
                    Column {
                        OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                singleLine = true,
                                placeholder = { Text("Nombre de la lista") },
                                colors =
                                        TextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF1A1A1A),
                                                unfocusedContainerColor = Color(0xFF1A1A1A),
                                                focusedIndicatorColor = Color(0xFF2196F3),
                                                unfocusedIndicatorColor = Color(0xFF404040),
                                                cursorColor = Color(0xFF2196F3),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                        )
                        )
                        if (createError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = createError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                val ok = viewModel.createPlaylist(newPlaylistName)
                                if (ok) {
                                    showCreateDialog = false
                                    newPlaylistName = ""
                                    createError = null
                                } else {
                                    createError = "Nombre invalido o duplicado"
                                }
                            }
                    ) { Text("Crear", color = Color(0xFF2196F3)) }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                showCreateDialog = false
                                newPlaylistName = ""
                                createError = null
                            }
                    ) { Text("Cancelar", color = Color.White) }
                }
        )
    }
}

@Composable
fun PlaylistDetailScreen(viewModel: MainViewModel, playlistName: String, onBack: () -> Unit) {
    val songs by viewModel.songs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val playlists: List<Playlist> by viewModel.playlists.collectAsState()

    BackHandler { onBack() }

    val playlist = remember(playlists, playlistName) { playlists.find { it.name == playlistName } }

    val playlistSongs =
            remember(songs, playlist) {
                if (playlist != null) {
                    songs.filter { it.id in playlist.songIds }
                } else {
                    emptyList()
                }
            }

    val availableSongs =
            remember(songs, playlist) {
                if (playlist != null) {
                    songs.filter { it.id !in playlist.songIds }
                } else {
                    emptyList()
                }
            }

    var showAddSongsDialog by remember { mutableStateOf(false) }
    var selectedSongsToAdd by remember { mutableStateOf(setOf<Long>()) }
    var selectedSongsToRemove by remember { mutableStateOf(setOf<Long>()) }
    var isRemoveMode by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                        text = playlistName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                )
                Text(text = "${playlistSongs.size} canciones", color = Color.Gray, fontSize = 12.sp)
            }
        }

        if (playlist != null) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRemoveMode) {
                    val allSelected =
                            playlistSongs.isNotEmpty() &&
                                    selectedSongsToRemove.size == playlistSongs.size
                    TextButton(
                            onClick = {
                                selectedSongsToRemove =
                                        if (allSelected) {
                                            setOf()
                                        } else {
                                            playlistSongs.map { it.id }.toSet()
                                        }
                            }
                    ) { Text(if (allSelected) "Limpiar" else "Seleccionar todo") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                            onClick = {
                                if (selectedSongsToRemove.isNotEmpty()) {
                                    viewModel.removeSongsFromPlaylist(
                                            playlistName,
                                            selectedSongsToRemove.toList()
                                    )
                                    selectedSongsToRemove = setOf()
                                    isRemoveMode = false
                                }
                            },
                            enabled = selectedSongsToRemove.isNotEmpty()
                    ) { Text("Eliminar (${selectedSongsToRemove.size})") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                            onClick = {
                                isRemoveMode = false
                                selectedSongsToRemove = setOf()
                            }
                    ) { Text("Cancelar", color = Color(0xFF2196F3)) }
                } else {
                    Button(onClick = { showAddSongsDialog = true }) { Text("Agregar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                            onClick = {
                                isRemoveMode = true
                                selectedSongsToRemove = setOf()
                            },
                            colors =
                                    androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFD32F2F)
                                    )
                    ) { Text("Eliminar") }
                }
            }
        }

        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                        PaddingValues(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(playlistSongs) { song ->
                val isCurrent = playerState.currentSong?.id == song.id
                val isSelected = song.id in selectedSongsToRemove

                Box(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .background(
                                                if (isSelected) Color(0xFF424242)
                                                else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                        )
                                        .then(
                                                if (isRemoveMode) {
                                                    Modifier.clickable {
                                                        selectedSongsToRemove =
                                                                if (isSelected) {
                                                                    selectedSongsToRemove - song.id
                                                                } else {
                                                                    selectedSongsToRemove + song.id
                                                                }
                                                    }
                                                } else {
                                                    Modifier
                                                }
                                        )
                ) {
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRemoveMode) {
                            androidx.compose.material3.Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedSongsToRemove =
                                                if (checked) {
                                                    selectedSongsToRemove + song.id
                                                } else {
                                                    selectedSongsToRemove - song.id
                                                }
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                            )
                        }

                        SongItem(
                                song = song,
                                isPlaying = isCurrent,
                                onClick = {
                                    if (!isRemoveMode) {
                                        // Usar el orden de la playlist como cola de reproduccion
                                        viewModel.updateDisplayOrder(playlistSongs)
                                        viewModel.playSong(song)
                                        viewModel.startService(context, song)
                                    }
                                },
                                onQueueNext = { viewModel.addToQueueNext(song) },
                                onQueueEnd = { viewModel.addToQueueEnd(song) },
                                playlists = playlists,
                                onAddToPlaylist = { targetPlaylistName, songId ->
                                    viewModel.addSongToPlaylist(targetPlaylistName, songId)
                                },
                                onRemoveFromPlaylist = {
                                    if (!isRemoveMode) {
                                        viewModel.removeSongFromPlaylist(playlistName, song.id)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    if (showAddSongsDialog) {
        AlertDialog(
                onDismissRequest = { showAddSongsDialog = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Agregar canciones", color = Color.White) },
                text = {
                    if (availableSongs.isEmpty()) {
                        Text("No hay canciones disponibles", color = Color.Gray)
                    } else {
                        Column {
                            val allSelected =
                                    availableSongs.isNotEmpty() &&
                                            selectedSongsToAdd.size == availableSongs.size
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                        onClick = {
                                            selectedSongsToAdd =
                                                    if (allSelected) {
                                                        setOf()
                                                    } else {
                                                        availableSongs.map { it.id }.toSet()
                                                    }
                                        }
                                ) { Text(if (allSelected) "Limpiar" else "Seleccionar todo") }
                            }
                            LazyColumn {
                                items(availableSongs) { song ->
                                    val isSelectedToAdd = song.id in selectedSongsToAdd
                                    Row(
                                            modifier =
                                                    Modifier.fillMaxWidth()
                                                            .background(
                                                                    if (isSelectedToAdd)
                                                                            Color(0xFF424242)
                                                                    else Color.Transparent
                                                            )
                                                            .clickable {
                                                                selectedSongsToAdd =
                                                                        if (isSelectedToAdd) {
                                                                            selectedSongsToAdd -
                                                                                    song.id
                                                                        } else {
                                                                            selectedSongsToAdd +
                                                                                    song.id
                                                                        }
                                                            }
                                                            .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        androidx.compose.material3.Checkbox(
                                                checked = isSelectedToAdd,
                                                onCheckedChange = { checked ->
                                                    selectedSongsToAdd =
                                                            if (checked) {
                                                                selectedSongsToAdd + song.id
                                                            } else {
                                                                selectedSongsToAdd - song.id
                                                            }
                                                },
                                                modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                                text = song.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                            onClick = {
                                if (selectedSongsToAdd.isNotEmpty()) {
                                    viewModel.addSongsToPlaylist(
                                            playlistName,
                                            selectedSongsToAdd.toList()
                                    )
                                    selectedSongsToAdd = setOf()
                                    showAddSongsDialog = false
                                }
                            },
                            enabled = selectedSongsToAdd.isNotEmpty()
                    ) { Text("Agregar (${selectedSongsToAdd.size})") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSongsDialog = false }) {
                        Text("Cancelar", color = Color(0xFF2196F3))
                    }
                }
        )
    }
}

private enum class PlaylistSortMode {
    TITLE_ASC,
    TITLE_DESC
}
