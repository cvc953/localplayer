package com.cvc953.localplayer.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.R
import com.cvc953.localplayer.viewmodel.MainViewModel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.cvc953.localplayer.ui.PlayerState

@Composable
fun PlayerScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsState()
    val song = playerState.currentSong ?: return
    val context = LocalContext.current
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(song.uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri)
                retriever.embeddedPicture?.let {
                    albumArt = BitmapFactory.decodeByteArray(it, 0, it.size)
                }
                retriever.release()
            } catch (_: Exception) {}
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F0F0F), Color.Black)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.Start)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Spacer(Modifier.height(32.dp))

            Image(
                painter = albumArt?.asImageBitmap()?.let { BitmapPainter(it) }
                    ?: painterResource(R.drawable.ic_default_album), // Reemplaza por un recurso que tengas
                contentDescription = null,
                modifier = Modifier
                    .size(250.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(24.dp))

            SongTitleSection(title = song.title, artist = song.artist)

            Spacer(Modifier.weight(1f))

            PlayerControls( isPlaying = playerState.isPlaying,
                onClick = { viewModel.togglePlayPause() })

            Spacer(Modifier.height(32.dp))

        }
    }
}


@Composable
fun SongTitleSection(
    title: String,
    artist: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = artist,
            color = Color(0xFFAAAAAA),
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
fun PlayerControls(  viewModel: MainViewModel = viewModel(),
                     isPlaying: Boolean,
                     onClick: () -> Unit,
                     ) {

    val playerState by viewModel.playerState.collectAsState()
    val duration = playerState.duration.coerceAtLeast(1L)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Slider(
            value = playerState.position.toFloat(),
            onValueChange = { viewModel.seekTo(it.toLong()) },
            valueRange = 0f..playerState.duration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray
            )
        )


        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { viewModel.playPreviousSong() }) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White,
                    modifier = Modifier.size(64.dp))
            }

            IconButton(
                onClick = { viewModel.togglePlayPause() },
            ) {
                Icon(
                    imageVector = if (isPlaying)
                        Icons.Filled.Pause
                    else
                        Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            IconButton(onClick = { viewModel.playNextSong() }) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White,
                    modifier = Modifier.size(64.dp))
            }
        }
    }
}
