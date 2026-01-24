package com.cvc953.localplayer.ui

import MiniPlayer
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.viewmodel.MainViewModel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.util.StoragePermissionHandler



@Composable
fun MusicScreen(viewModel: MainViewModel = viewModel(), onOpenPlayer: () -> Unit) {
    val songs by viewModel.songs.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(SortMode.TITLE_ASC) }

    val filteredSongs = remember(songs, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) songs else songs.filter { song ->
            song.title.lowercase().contains(q) || song.artist.lowercase().contains(q)
        }
    }

    val sortedSongs = remember(filteredSongs, sortMode) {
        when (sortMode) {
            SortMode.TITLE_ASC -> filteredSongs.sortedBy { it.title.lowercase() }
            SortMode.TITLE_DESC -> filteredSongs.sortedByDescending { it.title.lowercase() }
            SortMode.ARTIST_ASC -> filteredSongs.sortedBy { it.artist.lowercase() }
        }
    }
    val playerState by viewModel.playerState.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }
    val showPlayerScreen by viewModel.isPlayerScreenVisible.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val listState = rememberLazyListState()

    // Actualizar el servicio cuando cambia el estado del player
    LaunchedEffect(playerState.isPlaying) {
        val intent = Intent(context, com.cvc953.localplayer.services.MusicService::class.java).apply {
            action = com.cvc953.localplayer.services.MusicService.ACTION_UPDATE_STATE
            putExtra("IS_PLAYING", playerState.isPlaying)
        }
        androidx.core.content.ContextCompat.startForegroundService(context, intent)
    }

    BackHandler {
        activity?.moveTaskToBack(true)
    }

    BackHandler() {
    }

    Scaffold(
        containerColor = Color.Black,
            bottomBar = {
                if (
                    playerState.currentSong != null &&
                    !showPlayerScreen
                ) {
                    MiniPlayer(
                        song = playerState.currentSong!!,
                        isPlaying = playerState.isPlaying,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onClick = { viewModel.openPlayerScreen() },
                        onNext = { viewModel.playNextSong() }
                    )
                }
            }

            ) { padding ->


        val isScanning by viewModel.isScanning

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
        } else {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {

            Column(modifier = Modifier.fillMaxSize()) {


                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Biblioteca",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        sortMode = sortMode.next()
                    }) {
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color.White)
                    }
                    IconButton(onClick = {
                        searchQuery = ""
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = { Text("Buscar por título o artista") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.White,
                        unfocusedIndicatorColor = Color.Gray,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color.White)
                            }
                        }
                    }
                )

                // Lista de canciones
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues( start = 16.dp,
                        top = 16.dp,
                        bottom = 16.dp,
                        end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedSongs) { song ->
                        val isCurrent = playerState.currentSong?.id == song.id
                        SongItem(
                            song = song,
                            isPlaying = isCurrent && playerState.isPlaying,
                            onClick = {
                                viewModel.playSong(song)
                                viewModel.startService(context, song)
                                onOpenPlayer()
                            }
                        )
                    }
                }
            }



            if (showPlayerScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(1f)
                ) {
                    PlayerScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.closePlayerScreen() }
                    )
                }
            }
        }
        }
    }
}

@Composable
fun MainMusicScreen(viewModel: MainViewModel, onOpenPlayer: () -> Unit) {
    StoragePermissionHandler {
        MusicScreen(viewModel, onOpenPlayer)
    }
}



fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return "%02d:%02d".format(minutes, seconds)
}

private enum class SortMode { TITLE_ASC, TITLE_DESC, ARTIST_ASC }

private fun SortMode.next(): SortMode = when (this) {
    SortMode.TITLE_ASC -> SortMode.TITLE_DESC
    SortMode.TITLE_DESC -> SortMode.ARTIST_ASC
    SortMode.ARTIST_ASC -> SortMode.TITLE_ASC
}