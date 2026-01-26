package com.cvc953.localplayer.ui


import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import MiniPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MainViewModel = viewModel(),
    onBack: () -> Unit
) {
    val showLyrics by viewModel.showLyrics.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val song = playerState.currentSong ?: return
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val screenHeightPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }
    val lyrics by viewModel.lyrics.collectAsState()
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val upcoming = remember(queue, playerState, songs, isShuffle, repeatMode) {
        viewModel.getUpcomingSongs()
    }
    val listState = rememberLazyListState()
    val dragList = remember { mutableStateListOf<com.cvc953.localplayer.model.Song>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    
    // Extraer información de formato de audio
    var audioFormat by remember { mutableStateOf("") }
    var audioBitrate by remember { mutableStateOf("") }
    
    LaunchedEffect(song.uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri)
                
                // Obtener mime type para el formato
                val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                audioFormat = when {
                    mimeType?.contains("flac", ignoreCase = true) == true -> "FLAC"
                    mimeType?.contains("mpeg", ignoreCase = true) == true -> "MP3"
                    mimeType?.contains("mp4", ignoreCase = true) == true -> "M4A"
                    mimeType?.contains("ogg", ignoreCase = true) == true -> "OGG"
                    mimeType?.contains("wav", ignoreCase = true) == true -> "WAV"
                    else -> mimeType?.substringAfterLast("/")?.uppercase() ?: "Unknown"
                }
                
                // Obtener bitrate
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                audioBitrate = if (bitrate != null) {
                    val kbps = bitrate.toInt() / 1000
                    "${kbps} kbps"
                } else {
                    ""
                }
                
                retriever.release()
            } catch (_: Exception) {
                audioFormat = ""
                audioBitrate = ""
            }
        }
    }

    LaunchedEffect(showQueue, queue) {
        // Only initialize/update the draggable list when the sheet is opened
        // or when the explicit queue changes. This prevents frequent updates
        // caused by shuffle reordering the upcoming list every second.
        if (showQueue && draggingIndex == null) {
            dragList.clear()
            dragList.addAll(upcoming)
        }
    }

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
                modifier = Modifier.fillMaxSize()
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
                            modifier = Modifier.fillMaxSize(),
                            onLineClick = { targetMs ->
                                viewModel.seekTo(targetMs)
                            }
                        )
                    }
                }

                // MiniPlayer al final
                MiniPlayer(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    onPlayPause = { viewModel.togglePlayPause() },
                    onClick = { viewModel.toggleLyrics() },
                    onNext = { viewModel.playNextSong() }
                )
            }
        } else {
            // CONTENIDO REAL
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {

                Spacer(Modifier.height(16.dp))

                Image(
                    painter = albumArt?.asImageBitmap()?.let { BitmapPainter(it) }
                        ?: painterResource(R.drawable.ic_default_album),
                    contentDescription = null,
                    modifier = Modifier
                        .size(250.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(32.dp))

                SongTitleSection(title = song.title, artist = song.artist)

                Spacer(Modifier.height(32.dp))

                PlayerControls(
                    viewModel = viewModel,
                    isPlaying = playerState.isPlaying,
                    onClick = { viewModel.togglePlayPause() },
                    audioFormat = audioFormat,
                    audioBitrate = audioBitrate
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Ver cola",
                            tint = Color.White
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    IconButton(onClick = { viewModel.toggleLyrics() }) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Mostrar letras", tint = Color.White
                        )
                    }
                }

            }

                if (showQueue) {
                    ModalBottomSheet(
                        onDismissRequest = { showQueue = false },
                        sheetState = sheetState,
                        containerColor = Color(0xFF121212)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text(
                                text = "Próximas canciones",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(Modifier.height(12.dp))

                            if (dragList.isEmpty()) {
                                Text(
                                    text = "No hay próximas canciones",
                                    color = Color(0xFFB0B0B0),
                                    fontSize = 14.sp
                                )
                            } else {
                                var itemPositions by remember { mutableStateOf(mapOf<Int, Pair<Int, Int>>()) }

                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pointerInput(dragList, itemPositions) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { pos ->
                                                    val layoutInfo = listState.layoutInfo
                                                    val visibleY = pos.y.toInt()
                                                    draggingIndex = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                                        visibleY >= item.offset && visibleY <= item.offset + item.size
                                                    }?.index
                                                    dragOffset = 0f
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                    val layoutInfo = listState.layoutInfo
                                                    val visibleY = change.position.y.toInt()
                                                    val target = layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                                        visibleY >= item.offset && visibleY <= item.offset + item.size
                                                    }?.index
                                                    val from = draggingIndex
                                                    if (from != null && target != null && target != from) {
                                                        val item = dragList.removeAt(from)
                                                        val newIndex = target.coerceIn(0, dragList.size)
                                                        dragList.add(newIndex, item)
                                                        draggingIndex = newIndex
                                                        dragOffset = 0f
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggingIndex = null
                                                    dragOffset = 0f
                                                    viewModel.setUpcomingOrder(dragList.toList())
                                                },
                                                onDragCancel = {
                                                    draggingIndex = null
                                                    dragOffset = 0f
                                                }
                                            )
                                        }
                                ) {
                                    itemsIndexed(dragList) { idx, queuedSong ->
                                        val isDragging = draggingIndex == idx
                                        val elevation by animateDpAsState(
                                            if (isDragging) 8.dp else 0.dp,
                                            label = "drag-elev"
                                        )
                                        val scale by animateFloatAsState(
                                            if (isDragging) 1.03f else 1f,
                                            label = "drag-scale"
                                        )
                                        val offsetDp = with(LocalDensity.current) { dragOffset.toDp() }
                                        val animatedOffset by animateDpAsState(
                                            if (isDragging) offsetDp else 0.dp,
                                            label = "drag-offset"
                                        )

                                        var albumArtBitmap by remember(queuedSong.uri) { mutableStateOf<Bitmap?>(null) }
                                        LaunchedEffect(queuedSong.uri) {
                                            albumArtBitmap = withContext(Dispatchers.IO) {
                                                try {
                                                    val retriever = MediaMetadataRetriever()
                                                    retriever.setDataSource(context, queuedSong.uri)
                                                    val picture = retriever.embeddedPicture
                                                    retriever.release()
                                                    picture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                                                } catch (_: Exception) { null }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .offset(y = if (isDragging) animatedOffset else 0.dp)
                                                .graphicsLayer(
                                                    scaleX = scale,
                                                    scaleY = scale,
                                                    shadowElevation = elevation.value
                                                )
                                                .shadow(elevation, RoundedCornerShape(12.dp), clip = true)
                                                .background(Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                                                .border(
                                                    width = if (isDragging) 2.dp else 1.dp,
                                                    color = if (isDragging) Color(0xFF2196F3) else Color(0xFF2A2A2A),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF2A2A2A))
                                            ) {
                                                if (albumArtBitmap != null) {
                                                    Image(
                                                        painter = BitmapPainter(albumArtBitmap!!.asImageBitmap()),
                                                        contentDescription = null,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.MusicNote,
                                                        contentDescription = null,
                                                        tint = Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .align(Alignment.Center)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.width(12.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = queuedSong.title,
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = queuedSong.artist,
                                                    color = Color(0xFFB0B0B0),
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.DragHandle,
                                                contentDescription = "Reordenar",
                                                tint = if (isDragging) Color(0xFF2196F3) else Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                        }
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
    audioFormat: String = "",
    audioBitrate: String = ""
) {

    val playerState by viewModel.playerState.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Slider(
                value = playerState.position.toFloat(),
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..playerState.duration.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFF2196F3),
                    inactiveTrackColor = Color(0xFF404040)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(playerState.position),
                    color = Color(0xFFB0B0B0),
                    fontSize = 10.sp
                )
                Text(
                    text = formatDuration(playerState.duration),
                    color = Color(0xFFB0B0B0),
                    fontSize = 10.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

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
        
        // Información de formato de audio
        if (audioFormat.isNotEmpty() || audioBitrate.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = buildString {
                    if (audioFormat.isNotEmpty()) append(audioFormat)
                    if (audioFormat.isNotEmpty() && audioBitrate.isNotEmpty()) append(" • ")
                    if (audioBitrate.isNotEmpty()) append(audioBitrate)
                },
                color = Color(0xFF808080),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
