package com.cvc953.localplayer.ui

import MiniPlayer
import android.app.Activity
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
    val sortedSongs = songs.sortedBy {it.title.lowercase()}
    val playerState by viewModel.playerState.collectAsState()
    var showPlayer by remember { mutableStateOf(false) }
    val showPlayerScreen by viewModel.isPlayerScreenVisible.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val listState = rememberLazyListState()


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
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Sort, contentDescription = null, tint = Color.White)
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                    }
                }

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