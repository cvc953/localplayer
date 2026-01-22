package com.cvc953.localplayer.ui

import MiniPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.viewmodel.MainViewModel
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable




@Composable
fun MusicScreen(viewModel: MainViewModel = viewModel()) {
    val songs by viewModel.songs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            playerState.currentSong?.let { song ->
                MiniPlayer(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    onPlayPause = {
                        viewModel.togglePlayPause()
                    },
                    onClick = {
                        // más adelante: abrir pantalla completa
                    }
                )
            }
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                /*.background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Black)
                    )
                )*/
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(songs) { song ->
                        val isCurrent = playerState.currentSong?.id == song.id
                        SongItem(
                            song = song,
                            isPlaying = isCurrent && playerState.isPlaying,
                            onClick = { viewModel.onSongClicked(song) }
                        )
                    }
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