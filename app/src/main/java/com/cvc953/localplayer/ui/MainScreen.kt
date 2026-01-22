package com.cvc953.localplayer.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import coil.compose.rememberAsyncImagePainter
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.viewmodel.MainViewModel
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue


@Composable
fun MusicScreen(viewModel: MainViewModel = viewModel()) {
    val songs by viewModel.songs.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    // Fondo degradado
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF000000), Color(0xFF1A195B))
                )
            )
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
                IconButton(onClick = { /* TODO: filtro */ }) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort", tint = Color.White)
                }
                IconButton(onClick = { /* TODO: búsqueda */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
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

fun formatDuration(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return "%02d:%02d".format(minutes, seconds)
}


@Composable
fun SongItem(song: Song, isPlaying: Boolean, onClick: () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x00FFFFFF))
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model = song.albumArt,
                error = painterResource(id = R.drawable.ic_default_album),
                placeholder = painterResource(id = R.drawable.ic_default_album)
            ),
            contentDescription = song.title,
            modifier = Modifier
                .size(65.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(text = song.artist, color = Color(0xFFCCCCCC), fontSize = 12.sp)
        }

        Text(
            text = formatDuration(song.duration),
            color = Color(0xFFCCCCCC),
            fontSize = 12.sp
        )
    }
}