package com.cvc953.localplayer.ui


import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.media.MediaMetadataRetriever
import android.graphics.BitmapFactory
import coil.compose.rememberAsyncImagePainter
import com.cvc953.localplayer.R
import com.cvc953.localplayer.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun PlayerScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit
) {
    val showLyrics by viewModel.showLyrics.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val song = playerState.currentSong ?: return
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    val lyrics by viewModel.lyrics.collectAsState()
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }

    // Cargar carátula del álbum
    LaunchedEffect(song.uri) {
        albumArt = withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri)
                val picture = retriever.embeddedPicture
                retriever.release()
                picture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            } catch (_: Exception) {
                null
            }
        }
    }

    BackHandler(enabled = showLyrics) {
        viewModel.toggleLyrics()
    }
    BackHandler(enabled = !showLyrics) {
        onBack()
    }

    LaunchedEffect(song) {
        viewModel.loadLyricsForSong(song)
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.toInt()) }
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0F0F0F), Color.Black))
            )
    ) {

        // Barra de drag
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, offsetY.value.toInt()) }
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF0F0F0F), Color.Black))
                )
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            scope.launch {
                                offsetY.snapTo((offsetY.value + dragAmount).coerceAtLeast(0f))
                            }

                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetY.value > screenHeightPx * 0.25f) {
                                    offsetY.animateTo(screenHeightPx, tween(250))
                                    onBack()
                                } else {
                                    offsetY.animateTo(0f, tween(250))
                                }
                            }
                        }
                    )
                }
        )


        if (showLyrics) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {

                // ZONA DE LETRAS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (lyrics.isEmpty()) {
                        Text(
                            text = "No hay letras para esta canción",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    } else {
                        LyricsView(
                            lyrics = lyrics,
                            currentPosition = playerState.position,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.height(25.dp))

                SongTitleSection(title = song.title, artist = song.artist)


                Spacer(Modifier.height(25.dp))

                PlayerControls(
                    viewModel = viewModel,
                    isPlaying = playerState.isPlaying,
                    onClick = { viewModel.togglePlayPause() }
                )

                Spacer(Modifier.height(16.dp))
            }
        } else {
            // CONTENIDO REAL
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(23.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(Modifier.height(31.dp))

                Image(
                    painter = albumArt?.asImageBitmap()?.let { BitmapPainter(it) }
                        ?: painterResource(R.drawable.ic_default_album),
                    contentDescription = null,
                    modifier = Modifier
                        .size(250.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(23.dp))

                SongTitleSection(title = song.title, artist = song.artist)


                Spacer(Modifier.height(31.dp))

                PlayerControls(
                    viewModel = viewModel,
                    isPlaying = playerState.isPlaying,
                    onClick = { viewModel.togglePlayPause() })

                Spacer(Modifier.height(31.dp))

                IconButton(onClick = { viewModel.toggleLyrics() }) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = "Mostrar letras", tint = Color.White
                    )
                }

            }
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
fun PlayerControls(
    viewModel: MainViewModel = viewModel(),
    isPlaying: Boolean,
    onClick: () -> Unit,
) {

    val playerState by viewModel.playerState.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
        ) {
            Slider(
                value = playerState.position.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..playerState.duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF2196F3),
                    inactiveTrackColor = Color.White
                )
            )
        }


        Spacer(Modifier.height(32.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(onClick = { viewModel.toggleShuffle() }) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) Color(0xFF2196F3) else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { viewModel.playPreviousSong() }) {
                Icon(
                    Icons.Default.SkipPrevious, null, tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF2196F3), shape = RoundedCornerShape(32.dp))
            ) {
                Icon(
                    imageVector = if (isPlaying)
                        Icons.Filled.Pause
                    else
                        Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { viewModel.playNextSong() }) {
                Icon(
                    Icons.Default.SkipNext, null, tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }

            IconButton(onClick = { viewModel.toggleRepeat() }) {
                Icon(
                    when (repeatMode) {
                        RepeatMode.NONE -> Icons.Default.Repeat
                        RepeatMode.ONE -> Icons.Default.RepeatOne
                        RepeatMode.ALL -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode != RepeatMode.NONE) Color(0xFF2196F3) else Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
