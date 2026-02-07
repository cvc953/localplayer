package com.cvc953.localplayer.ui

import MiniPlayer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.sharp.SkipNext
import androidx.compose.material.icons.sharp.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.cvc953.localplayer.R
import com.cvc953.localplayer.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
        viewModel: MainViewModel = viewModel(),
        onBack: () -> Unit,
        onNavigateToArtist: (String) -> Unit = {},
        onNavigateToAlbum: (String) -> Unit = {}
) {
        val showLyrics by viewModel.showLyrics.collectAsState()
        val playerState by viewModel.playerState.collectAsState()
        val queue by viewModel.queue.collectAsState()
        val songs by viewModel.songs.collectAsState()
        val playlists by viewModel.playlists.collectAsState()
        val isShuffle by viewModel.isShuffle.collectAsState()
        val repeatMode by viewModel.repeatMode.collectAsState()
        val song = playerState.currentSong ?: return
        val offsetY = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val screenHeightPx =
                with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
        val lyrics by viewModel.lyrics.collectAsState()
        var albumArt by remember { mutableStateOf<Bitmap?>(null) }
        var showQueue by remember { mutableStateOf(false) }
        var showAddToPlaylistDialog by remember { mutableStateOf(false) }
        var isFavorite by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val upcoming =
                remember(queue, playerState, songs, isShuffle, repeatMode) {
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
                                val mimeType =
                                        retriever.extractMetadata(
                                                MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                                        )
                                audioFormat =
                                        when {
                                                mimeType?.contains("flac", ignoreCase = true) ==
                                                        true -> "FLAC"
                                                mimeType?.contains("mpeg", ignoreCase = true) ==
                                                        true -> "MP3"
                                                mimeType?.contains("mp4", ignoreCase = true) ==
                                                        true -> "M4A"
                                                mimeType?.contains("wav", ignoreCase = true) ==
                                                        true -> "WAV"
                                                else ->
                                                        mimeType?.substringAfterLast("/")
                                                                ?.uppercase()
                                                                ?: "Unknown"
                                        }

                                // Obtener bitrate
                                val bitrate =
                                        retriever.extractMetadata(
                                                MediaMetadataRetriever.METADATA_KEY_BITRATE
                                        )
                                audioBitrate =
                                        if (bitrate != null) {
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
                albumArt =
                        withContext(Dispatchers.IO) {
                                try {
                                        val retriever = MediaMetadataRetriever()
                                        retriever.setDataSource(context, song.uri)
                                        val picture = retriever.embeddedPicture
                                        retriever.release()
                                        picture?.let {
                                                BitmapFactory.decodeByteArray(it, 0, it.size)
                                        }
                                } catch (_: Exception) {
                                        null
                                }
                        }
        }

        LaunchedEffect(song.id, playlists) {
                isFavorite = viewModel.isSongInPlaylist("Favoritos", song.id)
        }

        BackHandler(enabled = showLyrics) { viewModel.toggleLyrics() }
        BackHandler(enabled = !showLyrics) { onBack() }

        LaunchedEffect(song) { viewModel.loadLyricsForSong(song) }

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .offset { IntOffset(0, offsetY.value.toInt()) }
                                .background(
                                        Brush.verticalGradient(
                                                listOf(Color(0xFF0F0F0F), Color.Black)
                                        )
                                )
        ) {

                // Barra de drag
                Box(
                        modifier =
                                Modifier.fillMaxSize()
                                        .offset { IntOffset(0, offsetY.value.toInt()) }
                                        .pointerInput(Unit) {
                                                detectVerticalDragGestures(
                                                        onVerticalDrag = { _, dragAmount ->
                                                                scope.launch {
                                                                        offsetY.snapTo(
                                                                                (offsetY.value +
                                                                                                dragAmount)
                                                                                        .coerceAtLeast(
                                                                                                0f
                                                                                        )
                                                                        )
                                                                }
                                                        },
                                                        onDragEnd = {
                                                                scope.launch {
                                                                        if (offsetY.value >
                                                                                        screenHeightPx *
                                                                                                0.25f
                                                                        ) {
                                                                                offsetY.animateTo(
                                                                                        screenHeightPx,
                                                                                        tween(250)
                                                                                )
                                                                                onBack()
                                                                        } else {
                                                                                offsetY.animateTo(
                                                                                        0f,
                                                                                        tween(250)
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                )
                                        }
                )

                if (showLyrics) {

                        Column(modifier = Modifier.fillMaxSize()) {

                                // ZONA DE LETRAS
                                Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
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
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                                Spacer(Modifier.height(16.dp))

                                // Imagen del álbum responsiva (máximo 70% del ancho)
                                Image(
                                        painter =
                                                albumArt?.asImageBitmap()?.let { BitmapPainter(it) }
                                                        ?: painterResource(
                                                                R.drawable.ic_default_album
                                                        ),
                                        contentDescription = null,
                                        modifier =
                                                Modifier.fillMaxWidth(0.9f)
                                                        .aspectRatio(1f)
                                                        .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                )

                                Spacer(Modifier.height(32.dp))

                                SongTitleSection(
                                        title = song.title,
                                        artist = song.artist,
                                        album = song.album,
                                        onArtistClick = { onNavigateToArtist(song.artist) },
                                        onAlbumClick = { onNavigateToAlbum(song.album) }
                                )

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
                                        IconButton(
                                                onClick = {
                                                        val favoritesName = "Favoritos"

                                                        if (isFavorite) {
                                                                // Quitar de favoritos
                                                                val removed =
                                                                        viewModel
                                                                                .removeSongFromPlaylist(
                                                                                        favoritesName,
                                                                                        song.id
                                                                                )
                                                                if (removed) {
                                                                        isFavorite = false
                                                                        Toast.makeText(
                                                                                        context,
                                                                                        "Quitado de Favoritos",
                                                                                        Toast.LENGTH_SHORT
                                                                                )
                                                                                .show()
                                                                }
                                                        } else {
                                                                // Agregar a favoritos
                                                                val favorites =
                                                                        playlists.find {
                                                                                it.name ==
                                                                                        favoritesName
                                                                        }
                                                                if (favorites == null) {
                                                                        viewModel.createPlaylist(
                                                                                favoritesName
                                                                        )
                                                                }
                                                                val added =
                                                                        viewModel.addSongToPlaylist(
                                                                                favoritesName,
                                                                                song.id
                                                                        )
                                                                if (added) {
                                                                        isFavorite = true
                                                                        Toast.makeText(
                                                                                        context,
                                                                                        "Agregado a Favoritos",
                                                                                        Toast.LENGTH_SHORT
                                                                                )
                                                                                .show()
                                                                }
                                                        }
                                                }
                                        ) {
                                                Icon(
                                                        imageVector =
                                                                if (isFavorite)
                                                                        Icons.Default.Favorite
                                                                else Icons.Outlined.FavoriteBorder,
                                                        contentDescription = "Favoritos",
                                                        tint = Color.White
                                                )
                                        }

                                        Spacer(Modifier.width(16.dp))

                                        IconButton(onClick = { showQueue = true }) {
                                                Icon(
                                                        imageVector = Icons.Default.QueueMusic,
                                                        contentDescription = "Ver cola",
                                                        tint = Color.White
                                                )
                                        }

                                        Spacer(Modifier.width(16.dp))

                                        IconButton(onClick = { showAddToPlaylistDialog = true }) {
                                                Icon(
                                                        imageVector = Icons.Default.PlaylistAdd,
                                                        contentDescription = "Agregar a playlist",
                                                        tint = Color.White
                                                )
                                        }

                                        Spacer(Modifier.width(16.dp))

                                        IconButton(onClick = { viewModel.toggleLyrics() }) {
                                                Icon(
                                                        imageVector = Icons.Default.Lyrics,
                                                        contentDescription = "Mostrar letras",
                                                        tint = Color.White
                                                )
                                        }
                                }
                        }

                        if (showAddToPlaylistDialog) {
                                AlertDialog(
                                        onDismissRequest = { showAddToPlaylistDialog = false },
                                        title = { Text("Agregar a Playlist") },
                                        text = {
                                                LazyColumn {
                                                        items(playlists) { playlist ->
                                                                Text(
                                                                        text = playlist.name,
                                                                        modifier =
                                                                                Modifier.fillMaxWidth()
                                                                                        .clickable {
                                                                                                viewModel
                                                                                                        .addSongToPlaylist(
                                                                                                                playlist.name,
                                                                                                                song.id
                                                                                                        )
                                                                                                Toast.makeText(
                                                                                                                context,
                                                                                                                "Agregado a ${playlist.name}",
                                                                                                                Toast.LENGTH_SHORT
                                                                                                        )
                                                                                                        .show()
                                                                                                showAddToPlaylistDialog =
                                                                                                        false
                                                                                        }
                                                                                        .padding(
                                                                                                8.dp
                                                                                        )
                                                                )
                                                        }
                                                }
                                        },
                                        confirmButton = {
                                                TextButton(
                                                        onClick = {
                                                                showAddToPlaylistDialog = false
                                                        }
                                                ) { Text("Cancelar") }
                                        }
                                )
                        }

                        if (showQueue) {
                                ModalBottomSheet(
                                        onDismissRequest = { showQueue = false },
                                        sheetState = sheetState,
                                        containerColor = Color(0xFF121212)
                                ) {
                                        Column(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 20.dp,
                                                                        vertical = 16.dp
                                                                )
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
                                                        var itemPositions by remember {
                                                                mutableStateOf(
                                                                        mapOf<Int, Pair<Int, Int>>()
                                                                )
                                                        }

                                                        LazyColumn(
                                                                state = listState,
                                                                verticalArrangement =
                                                                        Arrangement.spacedBy(10.dp),
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .pointerInput(
                                                                                        dragList,
                                                                                        itemPositions
                                                                                ) {
                                                                                        detectDragGesturesAfterLongPress(
                                                                                                onDragStart = {
                                                                                                        pos
                                                                                                        ->
                                                                                                        val layoutInfo =
                                                                                                                listState
                                                                                                                        .layoutInfo
                                                                                                        val visibleY =
                                                                                                                pos.y
                                                                                                                        .toInt()
                                                                                                        draggingIndex =
                                                                                                                layoutInfo
                                                                                                                        .visibleItemsInfo
                                                                                                                        .firstOrNull {
                                                                                                                                item
                                                                                                                                ->
                                                                                                                                visibleY >=
                                                                                                                                        item.offset &&
                                                                                                                                        visibleY <=
                                                                                                                                                item.offset +
                                                                                                                                                        item.size
                                                                                                                        }
                                                                                                                        ?.index
                                                                                                        dragOffset =
                                                                                                                0f
                                                                                                },
                                                                                                onDrag = {
                                                                                                        change,
                                                                                                        dragAmount
                                                                                                        ->
                                                                                                        change.consume()
                                                                                                        dragOffset +=
                                                                                                                dragAmount
                                                                                                                        .y
                                                                                                        val layoutInfo =
                                                                                                                listState
                                                                                                                        .layoutInfo
                                                                                                        val visibleY =
                                                                                                                change.position
                                                                                                                        .y
                                                                                                                        .toInt()
                                                                                                        val target =
                                                                                                                layoutInfo
                                                                                                                        .visibleItemsInfo
                                                                                                                        .firstOrNull {
                                                                                                                                item
                                                                                                                                ->
                                                                                                                                visibleY >=
                                                                                                                                        item.offset &&
                                                                                                                                        visibleY <=
                                                                                                                                                item.offset +
                                                                                                                                                        item.size
                                                                                                                        }
                                                                                                                        ?.index
                                                                                                        val from =
                                                                                                                draggingIndex
                                                                                                        if (from !=
                                                                                                                        null &&
                                                                                                                        target !=
                                                                                                                                null &&
                                                                                                                        target !=
                                                                                                                                from
                                                                                                        ) {
                                                                                                                val item =
                                                                                                                        dragList.removeAt(
                                                                                                                                from
                                                                                                                        )
                                                                                                                val newIndex =
                                                                                                                        target.coerceIn(
                                                                                                                                0,
                                                                                                                                dragList.size
                                                                                                                        )
                                                                                                                dragList.add(
                                                                                                                        newIndex,
                                                                                                                        item
                                                                                                                )
                                                                                                                draggingIndex =
                                                                                                                        newIndex
                                                                                                                dragOffset =
                                                                                                                        0f
                                                                                                        }
                                                                                                },
                                                                                                onDragEnd = {
                                                                                                        draggingIndex =
                                                                                                                null
                                                                                                        dragOffset =
                                                                                                                0f
                                                                                                        viewModel
                                                                                                                .setUpcomingOrder(
                                                                                                                        dragList.toList()
                                                                                                                )
                                                                                                },
                                                                                                onDragCancel = {
                                                                                                        draggingIndex =
                                                                                                                null
                                                                                                        dragOffset =
                                                                                                                0f
                                                                                                }
                                                                                        )
                                                                                }
                                                        ) {
                                                                itemsIndexed(dragList) {
                                                                        idx,
                                                                        queuedSong ->
                                                                        val isDragging =
                                                                                draggingIndex == idx
                                                                        val elevation by
                                                                                animateDpAsState(
                                                                                        if (isDragging
                                                                                        )
                                                                                                8.dp
                                                                                        else 0.dp,
                                                                                        label =
                                                                                                "drag-elev"
                                                                                )
                                                                        val scale by
                                                                                animateFloatAsState(
                                                                                        if (isDragging
                                                                                        )
                                                                                                1.03f
                                                                                        else 1f,
                                                                                        label =
                                                                                                "drag-scale"
                                                                                )
                                                                        val offsetDp =
                                                                                with(
                                                                                        LocalDensity
                                                                                                .current
                                                                                ) {
                                                                                        dragOffset
                                                                                                .toDp()
                                                                                }
                                                                        val animatedOffset by
                                                                                animateDpAsState(
                                                                                        if (isDragging
                                                                                        )
                                                                                                offsetDp
                                                                                        else 0.dp,
                                                                                        label =
                                                                                                "drag-offset"
                                                                                )

                                                                        var albumArtBitmap by
                                                                                remember(
                                                                                        queuedSong
                                                                                                .uri
                                                                                ) {
                                                                                        mutableStateOf<
                                                                                                Bitmap?>(
                                                                                                null
                                                                                        )
                                                                                }
                                                                        LaunchedEffect(
                                                                                queuedSong.uri
                                                                        ) {
                                                                                albumArtBitmap =
                                                                                        withContext(
                                                                                                Dispatchers
                                                                                                        .IO
                                                                                        ) {
                                                                                                try {
                                                                                                        val retriever =
                                                                                                                MediaMetadataRetriever()
                                                                                                        retriever
                                                                                                                .setDataSource(
                                                                                                                        context,
                                                                                                                        queuedSong
                                                                                                                                .uri
                                                                                                                )
                                                                                                        val picture =
                                                                                                                retriever
                                                                                                                        .embeddedPicture
                                                                                                        retriever
                                                                                                                .release()
                                                                                                        picture
                                                                                                                ?.let {
                                                                                                                        BitmapFactory
                                                                                                                                .decodeByteArray(
                                                                                                                                        it,
                                                                                                                                        0,
                                                                                                                                        it.size
                                                                                                                                )
                                                                                                                }
                                                                                                } catch (
                                                                                                        _:
                                                                                                                Exception) {
                                                                                                        null
                                                                                                }
                                                                                        }
                                                                        }

                                                                        Row(
                                                                                modifier =
                                                                                        Modifier.fillMaxWidth()
                                                                                                .offset(
                                                                                                        y =
                                                                                                                if (isDragging
                                                                                                                )
                                                                                                                        animatedOffset
                                                                                                                else
                                                                                                                        0.dp
                                                                                                )
                                                                                                .graphicsLayer(
                                                                                                        scaleX =
                                                                                                                scale,
                                                                                                        scaleY =
                                                                                                                scale,
                                                                                                        shadowElevation =
                                                                                                                elevation
                                                                                                                        .value
                                                                                                )
                                                                                                .shadow(
                                                                                                        elevation,
                                                                                                        RoundedCornerShape(
                                                                                                                12.dp
                                                                                                        ),
                                                                                                        clip =
                                                                                                                true
                                                                                                )
                                                                                                .background(
                                                                                                        Color(
                                                                                                                0xFF1A1A1A
                                                                                                        ),
                                                                                                        RoundedCornerShape(
                                                                                                                12.dp
                                                                                                        )
                                                                                                )
                                                                                                .border(
                                                                                                        width =
                                                                                                                if (isDragging
                                                                                                                )
                                                                                                                        2.dp
                                                                                                                else
                                                                                                                        1.dp,
                                                                                                        color =
                                                                                                                if (isDragging
                                                                                                                )
                                                                                                                        Color(
                                                                                                                                0xFF2196F3
                                                                                                                        )
                                                                                                                else
                                                                                                                        Color(
                                                                                                                                0xFF2A2A2A
                                                                                                                        ),
                                                                                                        shape =
                                                                                                                RoundedCornerShape(
                                                                                                                        12.dp
                                                                                                                )
                                                                                                )
                                                                                                .padding(
                                                                                                        horizontal =
                                                                                                                12.dp,
                                                                                                        vertical =
                                                                                                                10.dp
                                                                                                ),
                                                                                verticalAlignment =
                                                                                        Alignment
                                                                                                .CenterVertically
                                                                        ) {
                                                                                Box(
                                                                                        modifier =
                                                                                                Modifier.size(
                                                                                                                48.dp
                                                                                                        )
                                                                                                        .clip(
                                                                                                                RoundedCornerShape(
                                                                                                                        8.dp
                                                                                                                )
                                                                                                        )
                                                                                                        .background(
                                                                                                                Color(
                                                                                                                        0xFF2A2A2A
                                                                                                                )
                                                                                                        )
                                                                                ) {
                                                                                        if (albumArtBitmap !=
                                                                                                        null
                                                                                        ) {
                                                                                                Image(
                                                                                                        painter =
                                                                                                                BitmapPainter(
                                                                                                                        albumArtBitmap!!
                                                                                                                                .asImageBitmap()
                                                                                                                ),
                                                                                                        contentDescription =
                                                                                                                null,
                                                                                                        modifier =
                                                                                                                Modifier.fillMaxSize(),
                                                                                                        contentScale =
                                                                                                                ContentScale
                                                                                                                        .Crop
                                                                                                )
                                                                                        } else {
                                                                                                Icon(
                                                                                                        imageVector =
                                                                                                                Icons.Default
                                                                                                                        .MusicNote,
                                                                                                        contentDescription =
                                                                                                                null,
                                                                                                        tint =
                                                                                                                Color.White
                                                                                                                        .copy(
                                                                                                                                alpha =
                                                                                                                                        0.5f
                                                                                                                        ),
                                                                                                        modifier =
                                                                                                                Modifier.size(
                                                                                                                                24.dp
                                                                                                                        )
                                                                                                                        .align(
                                                                                                                                Alignment
                                                                                                                                        .Center
                                                                                                                        )
                                                                                                )
                                                                                        }
                                                                                }

                                                                                Spacer(
                                                                                        Modifier.width(
                                                                                                12.dp
                                                                                        )
                                                                                )

                                                                                Column(
                                                                                        modifier =
                                                                                                Modifier.weight(
                                                                                                        1f
                                                                                                )
                                                                                ) {
                                                                                        Text(
                                                                                                text =
                                                                                                        queuedSong
                                                                                                                .title,
                                                                                                color =
                                                                                                        Color.White,
                                                                                                fontSize =
                                                                                                        16.sp,
                                                                                                maxLines =
                                                                                                        1,
                                                                                                overflow =
                                                                                                        TextOverflow
                                                                                                                .Ellipsis
                                                                                        )
                                                                                        Text(
                                                                                                text =
                                                                                                        queuedSong
                                                                                                                .artist,
                                                                                                color =
                                                                                                        Color(
                                                                                                                0xFFB0B0B0
                                                                                                        ),
                                                                                                fontSize =
                                                                                                        13.sp,
                                                                                                maxLines =
                                                                                                        1,
                                                                                                overflow =
                                                                                                        TextOverflow
                                                                                                                .Ellipsis
                                                                                        )
                                                                                }

                                                                                Icon(
                                                                                        imageVector =
                                                                                                Icons.Default
                                                                                                        .DragHandle,
                                                                                        contentDescription =
                                                                                                "Reordenar",
                                                                                        tint =
                                                                                                if (isDragging
                                                                                                )
                                                                                                        Color(
                                                                                                                0xFF2196F3
                                                                                                        )
                                                                                                else
                                                                                                        Color.White
                                                                                                                .copy(
                                                                                                                        alpha =
                                                                                                                                0.7f
                                                                                                                )
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
        artist: String,
        album: String,
        onArtistClick: () -> Unit,
        onAlbumClick: () -> Unit
) {
        var showMenu by remember { mutableStateOf(false) }
        var menuTarget by remember { mutableStateOf<String?>(null) }

        Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
        ) {
                Text(
                        text = title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        modifier = Modifier.fillMaxWidth().basicMarquee()
                )

                Spacer(Modifier.height(6.dp))

                Box {
                        Text(
                                text = if (album.isNotEmpty()) "$artist - $album" else artist,
                                color = Color(0xFFAAAAAA),
                                fontSize = 14.sp,
                                maxLines = 1,
                                modifier =
                                        Modifier.clickable {
                                                menuTarget = "info"
                                                showMenu = true
                                        }
                        )

                        DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                containerColor = Color.Black
                        ) {
                                DropdownMenuItem(
                                        text = { Text("Ir al artista", color = Color.White) },
                                        onClick = {
                                                showMenu = false
                                                onArtistClick()
                                        }
                                )
                                DropdownMenuItem(
                                        text = { Text("Ir al álbum", color = Color.White) },
                                        onClick = {
                                                showMenu = false
                                                onAlbumClick()
                                        }
                                )
                        }
                }
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
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Slider(
                                value = playerState.position.toFloat(),
                                onValueChange = { viewModel.seekTo(it.toLong()) },
                                valueRange = 0f..playerState.duration.toFloat(),
                                modifier = Modifier.fillMaxWidth().height(20.dp),
                                colors =
                                        SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFF2196F3),
                                                inactiveTrackColor = Color(0xFF404040)
                                        )
                        )

                        Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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

                val buttonSize =
                        with(LocalDensity.current) {
                                (LocalConfiguration.current.screenWidthDp.dp * 0.20f)
                        }
                val smallButtonSize =
                        with(LocalDensity.current) {
                                (LocalConfiguration.current.screenWidthDp.dp * 0.13f)
                        }

                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                ) {
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                                Icon(
                                        Icons.Rounded.Shuffle,
                                        contentDescription = "Shuffle",
                                        tint = if (isShuffle) Color(0xFF2196F3) else Color.White,
                                        modifier = Modifier.size(buttonSize)
                                )
                        }

                        IconButton(onClick = { viewModel.playPreviousSong() }) {
                                Icon(
                                        Icons.Rounded.SkipPrevious,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(buttonSize)
                                )
                        }

                        IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier =
                                        Modifier.size(buttonSize)
                                                .background(
                                                        Color(0xFF2196F3),
                                                        shape = RoundedCornerShape(50)
                                                )
                        ) {
                                Icon(
                                        imageVector =
                                                if (isPlaying) Icons.Rounded.Pause
                                                else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(buttonSize * 0.5f)
                                )
                        }

                        IconButton(onClick = { viewModel.playNextSong() }) {
                                Icon(
                                        Icons.Rounded.SkipNext,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(buttonSize)
                                )
                        }

                        IconButton(onClick = { viewModel.toggleRepeat() }) {
                                Icon(
                                        when (repeatMode) {
                                                RepeatMode.NONE -> Icons.Rounded.Repeat
                                                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                                RepeatMode.ALL -> Icons.Rounded.Repeat
                                        },
                                        contentDescription = "Repeat",
                                        tint =
                                                if (repeatMode != RepeatMode.NONE) Color(0xFF2196F3)
                                                else Color.White,
                                        modifier = Modifier.size(buttonSize)
                                )
                        }
                }

                // Información de formato de audio
                if (audioFormat.isNotEmpty() || audioBitrate.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Text(
                                text =
                                        buildString {
                                                if (audioFormat.isNotEmpty()) append(audioFormat)
                                                if (audioFormat.isNotEmpty() &&
                                                                audioBitrate.isNotEmpty()
                                                )
                                                        append(" • ")
                                                if (audioBitrate.isNotEmpty()) append(audioBitrate)
                                        },
                                color = Color(0xFF808080),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                        )
                }
        }
}
