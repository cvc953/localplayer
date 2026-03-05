@file:Suppress("ktlint:standard:no-wildcard-imports")

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
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.util.getDominantColor
import com.cvc953.localplayer.util.withLowTransparency
import com.cvc953.localplayer.viewmodel.LyricsViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playbackViewModel: PlaybackViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel(),
    lyricsViewModel: LyricsViewModel = viewModel(),
    songViewModel: SongViewModel = viewModel(),
    onBack: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String, String) -> Unit = { _, _ -> },
) {
    val showLyrics by playerViewModel.showLyrics.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()
    val queue by playbackViewModel.queue.collectAsState()
    val songs by songViewModel.songs.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val isShuffle by playbackViewModel.isShuffle.collectAsState()
    val repeatMode by playbackViewModel.repeatMode.collectAsState()
    val song = playerState.currentSong ?: return
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val screenHeightPx =
        with(LocalDensity.current) {
            LocalConfiguration.current.screenHeightDp.dp
                .toPx()
        }
    val lyrics by lyricsViewModel.lyrics.collectAsState()
    val ttmlLyrics by lyricsViewModel.ttmlLyrics.collectAsState()
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }
    var dominantColor by remember { mutableStateOf(Color.Black) }
    var showQueue by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Use the actual queue order for upcoming songs (after the current song)
    val currentSongIndex = queue.indexOfFirst { it.id == playerState.currentSong?.id }
    val upcoming =
        if (currentSongIndex >= 0 && currentSongIndex + 1 < queue.size) {
            queue.subList(currentSongIndex + 1, queue.size)
        } else {
            emptyList()
        }
    val listState = rememberLazyListState()
    val dragList = remember { mutableStateListOf<com.cvc953.localplayer.model.Song>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // Extraer información de formato de audio
    var audioFormat by remember { mutableStateOf("") }
    var audioBitrate by remember { mutableStateOf("") }
    var audioSampleRate by remember { mutableStateOf("") }

    LaunchedEffect(song.uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri)

                // Obtener mime type para el formato
                val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                audioFormat =
                    when {
                        mimeType?.contains("flac", ignoreCase = true) == true -> "FLAC"
                        mimeType?.contains("mpeg", ignoreCase = true) == true -> "MP3"
                        mimeType?.contains("mp4", ignoreCase = true) == true -> "M4A"
                        mimeType?.contains("wav", ignoreCase = true) == true -> "WAV"
                        else -> mimeType?.substringAfterLast("/")?.uppercase() ?: "Unknown"
                    }

                // Obtener bitrate
                val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                audioBitrate =
                    if (bitrate != null) {
                        val kbps = bitrate.toInt() / 1000
                        "$kbps kbps"
                    } else {
                        ""
                    }

                // Obtener sample rate (Hz)
                val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                audioSampleRate =
                    if (sampleRate != null) {
                        val khz = sampleRate.toInt() / 1000.0
                        String.format("%.1f kHz", khz)
                    } else {
                        ""
                    }

                retriever.release()
            } catch (_: Exception) {
                audioFormat = ""
                audioBitrate = ""
                audioSampleRate = ""
            }
        }
    }

    LaunchedEffect(showQueue, queue) {
        // Only initialize/update the draggable list when the sheet is opened
        // or when the explicit queue changes. This prevents frequent updates
        // caused by shuffle reordering the upcoming list every second.
        if (showQueue && draggingIndex == null) {
            dragList.clear()
            // Show only upcoming songs (exclude currently playing), in true playback order
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

    // Calcular color dominante basado en la carátula
    LaunchedEffect(albumArt) {
        if (albumArt != null) {
            dominantColor = albumArt!!.getDominantColor(Color.Black) // .withLowTransparency(0.1f)
        } else {
            dominantColor = Color.Black // .withLowTransparency(0.1f)
        }
    }

    LaunchedEffect(song.id, playlists) {
        isFavorite = playlistViewModel.isSongInPlaylist("Favoritos", song.id)
    }

    BackHandler(enabled = showLyrics) { playerViewModel.toggleLyrics() }
    BackHandler(enabled = !showLyrics) { onBack() }

    LaunchedEffect(song) { lyricsViewModel.loadLyricsForSong(song) }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .offset { IntOffset(0, offsetY.value.toInt()) }
                .background(
                    Brush.verticalGradient(
                        listOf(dominantColor.darken(0.5f), Color.Black),
                    ),
                ),
    ) {
        // Barra de drag
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, offsetY.value.toInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dragAmount ->
                                scope.launch {
                                    offsetY.snapTo(
                                        (
                                            offsetY.value +
                                                dragAmount
                                        ).coerceAtLeast(
                                            0f,
                                        ),
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
                                            tween(250),
                                        )
                                        onBack()
                                    } else {
                                        offsetY.animateTo(
                                            0f,
                                            tween(250),
                                        )
                                    }
                                }
                            },
                        )
                    },
        )

        if (showLyrics) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ZONA DE LETRAS
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Priorizar TTML sobre LRC
                    when {
                        ttmlLyrics != null &&
                            ttmlLyrics!!.lines.isNotEmpty() -> {
                            // Mostrar letras palabra por palabra
                            TtmlLyricsView(
                                lines = ttmlLyrics!!.lines,
                                currentPosition =
                                    playerState.position,
                                modifier = Modifier.fillMaxSize(),
                                dominantColor = dominantColor,
                                onLineClick = { targetMs -> playbackViewModel.seekTo(targetMs) },
                            )
                        }

                        lyrics.isNotEmpty() -> {
                            // Fallback a letras línea por línea
                            LyricsView(
                                lyrics = lyrics,
                                currentPosition =
                                    playerState.position,
                                modifier = Modifier.fillMaxSize(),
                                dominantColor = dominantColor,
                                onLineClick = { targetMs -> playbackViewModel.seekTo(targetMs) },
                            )
                        }

                        else -> {
                            Text(
                                text =
                                    "No hay letras para esta canción",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = LocalExtendedColors.current.textSecondary,
                            )
                        }
                    }
                }

                // MiniPlayer al final
                MiniPlayer(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    onPlayPause = { playbackViewModel.togglePlayPause() },
                    onClick = { playerViewModel.toggleLyrics() },
                    onNext = { playbackViewModel.playNextSong() },
                )
            }
        } else {
            // CONTENIDO REAL - Layout responsivo
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val screenHeight = LocalConfiguration.current.screenHeightDp
            val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

            // Determinar tipo de pantalla
            val isCompactLayout = aspectRatio >= 0.90f && aspectRatio < 1.15f // Cuadrada (0.9-1.15)
            val isNormalLayout = aspectRatio >= 1.15f && aspectRatio <= 1.6f // Normal/Media (1.15-1.6) - TU PANTALLA (1.5)
            val isLandscape = aspectRatio > 1.6f // Landscape
            val isTallLayout = aspectRatio < 0.80f // Rectangular alta
            
            // Tamaño dinámico de imagen
            val imageWidthPercent =
                when {
                    isLandscape -> 0.35f  // Landscape: 35%
                    isCompactLayout -> 0.45f  // Cuadrada: 45%
                    isNormalLayout -> 0.25f  // Normal/Media: 25% - TU PANTALLA (más pequeña para que represente ~35% del espacio)
                    isTallLayout -> 0.88f  // Rectangular alta: 88%
                    else -> 0.75f  // Default: 75%
                }

            // Spacers dinámicos según layout
            val betweenSpacer =
                when {
                    isCompactLayout -> 4.dp  // Cuadrada: mínimo
                    isNormalLayout -> 6.dp  // Normal/Media: muy compacto - TU PANTALLA
                    isTallLayout -> 24.dp  // Rectangular alta: mucho
                    isLandscape -> 12.dp  // Landscape: moderado
                    else -> 16.dp  // Default
                }

            // Padding vertical dinámico
            val verticalPadding = 
                when {
                    isCompactLayout -> 4.dp
                    isNormalLayout -> 2.dp
                    else -> 8.dp
                }

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = verticalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(Modifier.weight(1f))

                // Imagen del álbum responsiva
                Image(
                    painter =
                        albumArt?.asImageBitmap()?.let { BitmapPainter(it) }
                            ?: painterResource(
                                R.drawable.ic_default_album,
                            ),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .fillMaxWidth(imageWidthPercent)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )

                Spacer(Modifier.height(betweenSpacer))

                SongTitleSection(
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    albumArt = albumArt,
                    onArtistClick = {
                        // Extraer artista principal cuando hay múltiples artistas
                        val mainArtist = normalizeArtistName(song.artist).firstOrNull() ?: song.artist
                        onNavigateToArtist(mainArtist)
                    },
                    onAlbumClick = { onNavigateToAlbum(song.album, song.artist) },
                )

                Spacer(Modifier.height(betweenSpacer))

                PlayerControls(
                    playbackViewModel = playbackViewModel,
                    playerViewModel = playerViewModel,
                    isPlaying = playerState.isPlaying,
                    audioFormat = audioFormat,
                    audioBitrate = audioBitrate,
                    audioSampleRate = audioSampleRate,
                )

                Spacer(Modifier.height(betweenSpacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(
                        onClick = {
                            val favoritesName = "Favoritos"

                            if (isFavorite) {
                                // Quitar de favoritos
                                playlistViewModel.removeSongFromPlaylist(
                                    favoritesName,
                                    song.id,
                                )
                                isFavorite = false
                                Toast.makeText(context, "Quitado de Favoritos", Toast.LENGTH_SHORT).show()
                            } else {
                                // Agregar a favoritos
                                val favorites = playlists.find { it.name == favoritesName }
                                if (favorites == null) {
                                    playlistViewModel.createPlaylist(favoritesName)
                                }
                                playlistViewModel.addSongToPlaylist(favoritesName, song.id)
                                isFavorite = true
                                Toast.makeText(context, "Agregado a Favoritos", Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Icon(
                            imageVector =
                                if (isFavorite) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Outlined.FavoriteBorder
                                },
                            contentDescription = "Favoritos",
                            tint = Color.White,
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = "Ver cola",
                            tint = Color.White,
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    IconButton(onClick = { showAddToPlaylistDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = "Agregar a playlist",
                            tint = Color.White,
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    IconButton(onClick = { playerViewModel.toggleLyrics() }) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Mostrar letras",
                            tint = Color.White,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))
            }

            if (showAddToPlaylistDialog) {
                AlertDialog(
                    onDismissRequest = { showAddToPlaylistDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = { Text("Agregar a Playlist", color = MaterialTheme.colorScheme.onBackground) },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Button(
                                    onClick = {
                                        showCreatePlaylistDialog = true
                                    },
                                    colors =
                                        androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                        ),
                                ) {
                                    Text("Crear playlist")
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Create playlist dialog triggered from the Add-to-Playlist menu
                            if (showCreatePlaylistDialog) {
                                AlertDialog(
                                    onDismissRequest = {
                                        showCreatePlaylistDialog =
                                            false
                                        newPlaylistName =
                                            ""
                                    },
                                    containerColor =
                                        MaterialTheme.colorScheme.surface,
                                    title = {
                                        Text(
                                            "Nueva lista",
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    },
                                    text = {
                                        OutlinedTextField(
                                            value = newPlaylistName,
                                            onValueChange = {
                                                newPlaylistName =
                                                    it
                                            },
                                            singleLine = true,
                                            placeholder = {
                                                Text(
                                                    "Nombre de la lista",
                                                )
                                            },
                                            colors =
                                                TextFieldDefaults
                                                    .colors(
                                                        focusedContainerColor =
                                                            MaterialTheme.colorScheme.surface,
                                                        unfocusedContainerColor =
                                                            MaterialTheme.colorScheme.surface,
                                                        focusedIndicatorColor =
                                                            MaterialTheme.colorScheme.primary,
                                                        unfocusedIndicatorColor =
                                                            MaterialTheme.colorScheme.surfaceVariant,
                                                        cursorColor =
                                                            MaterialTheme.colorScheme.primary,
                                                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                                    ),
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            if (newPlaylistName.isNotBlank()) {
                                                playlistViewModel.createPlaylist(newPlaylistName)
                                                playlistViewModel.addSongToPlaylist(newPlaylistName, song.id)
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Creado y agregado a $newPlaylistName",
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                                newPlaylistName =
                                                    ""
                                                showCreatePlaylistDialog =
                                                    false
                                                showAddToPlaylistDialog =
                                                    false
                                            }
                                        }) {
                                            Text(
                                                "Crear",
                                                color =
                                                    MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            showCreatePlaylistDialog =
                                                false
                                        }) {
                                            Text(
                                                "Cancelar",
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        }
                                    },
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            LazyColumn(
                                modifier =
                                    Modifier.heightIn(
                                        max = 200.dp,
                                    ),
                            ) {
                                items(playlists) { playlist ->
                                    Card(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                                .clickable {
                                                    playlistViewModel.addSongToPlaylist(playlist.name, song.id)
                                                    Toast.makeText(context, "Agregado a ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                    showAddToPlaylistDialog = false
                                                },
                                        colors =
                                            CardDefaults
                                                .cardColors(
                                                    containerColor =
                                                        LocalExtendedColors.current.surfaceSheet,
                                                ),
                                        shape =
                                            RoundedCornerShape(
                                                8.dp,
                                            ),
                                    ) {
                                        Row(
                                            modifier =
                                                Modifier.padding(
                                                    12.dp,
                                                ),
                                            verticalAlignment =
                                                Alignment
                                                    .CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector =
                                                    Icons.AutoMirrored.Filled.PlaylistAdd,
                                                contentDescription =
                                                null,
                                                tint =
                                                    MaterialTheme.colorScheme.primary,
                                            )
                                            Spacer(
                                                Modifier.width(
                                                    12.dp,
                                                ),
                                            )
                                            Column {
                                                Text(
                                                    text =
                                                        playlist.name,
                                                    color =
                                                        MaterialTheme.colorScheme.onBackground,
                                                    fontSize =
                                                        14.sp,
                                                )
                                                Text(
                                                    text =
                                                        "${playlist.songIds.size} canciones",
                                                    color =
                                                        LocalExtendedColors.current.textSecondarySoft,
                                                    fontSize =
                                                        12.sp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showAddToPlaylistDialog = false
                            },
                        ) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
                    },
                )
            }

            if (showQueue) {
                ModalBottomSheet(
                    onDismissRequest = { showQueue = false },
                    sheetState = sheetState,
                    containerColor = LocalExtendedColors.current.surfaceSheet,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 20.dp,
                                    vertical = 16.dp,
                                ),
                    ) {
                        Text(
                            text = "Próximas canciones",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(Modifier.height(12.dp))

                        if (dragList.isEmpty()) {
                            Text(
                                text = "No hay próximas canciones",
                                color = LocalExtendedColors.current.textSecondarySoft,
                                fontSize = 14.sp,
                            )
                        } else {
                            var itemPositions by remember {
                                mutableStateOf(
                                    mapOf<Int, Pair<Int, Int>>(),
                                )
                            }

                            LazyColumn(
                                state = listState,
                                verticalArrangement =
                                    Arrangement.spacedBy(10.dp),
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .pointerInput(
                                            dragList,
                                            itemPositions,
                                        ) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { pos ->
                                                    val layoutInfo =
                                                        listState
                                                            .layoutInfo
                                                    val visibleY =
                                                        pos.y
                                                            .toInt()
                                                    draggingIndex =
                                                        layoutInfo
                                                            .visibleItemsInfo
                                                            .firstOrNull { item ->
                                                                visibleY >=
                                                                    item.offset &&
                                                                    visibleY <=
                                                                    item.offset +
                                                                    item.size
                                                            }?.index
                                                    dragOffset =
                                                        0f
                                                },
                                                onDrag = {
                                                    change,
                                                    dragAmount,
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

                                                    val edgeThresholdPx = 72
                                                    val autoScrollStepPx = 24f
                                                    val viewportStart = layoutInfo.viewportStartOffset
                                                    val viewportEnd = layoutInfo.viewportEndOffset

                                                    if (visibleY <= viewportStart + edgeThresholdPx) {
                                                        scope.launch {
                                                            listState.scrollBy(-autoScrollStepPx)
                                                        }
                                                    } else if (visibleY >= viewportEnd - edgeThresholdPx) {
                                                        scope.launch {
                                                            listState.scrollBy(autoScrollStepPx)
                                                        }
                                                    }

                                                    val target =
                                                        layoutInfo
                                                            .visibleItemsInfo
                                                            .firstOrNull { item ->
                                                                visibleY >=
                                                                    item.offset &&
                                                                    visibleY <=
                                                                    item.offset +
                                                                    item.size
                                                            }?.index
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
                                                                from,
                                                            )
                                                        val newIndex =
                                                            target.coerceIn(
                                                                0,
                                                                dragList.size,
                                                            )
                                                        dragList.add(
                                                            newIndex,
                                                            item,
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
                                                    // Merge current song back as first element when saving the new order
                                                    val currentSong = playerState.currentSong
                                                    val newOrder =
                                                        if (currentSong !=
                                                            null
                                                        ) {
                                                            listOf(currentSong) + dragList.toList()
                                                        } else {
                                                            dragList.toList()
                                                        }
                                                    playbackViewModel.updateDisplayOrder(newOrder)
                                                },
                                                onDragCancel = {
                                                    draggingIndex =
                                                        null
                                                    dragOffset =
                                                        0f
                                                },
                                            )
                                        },
                            ) {
                                itemsIndexed(dragList) {
                                    idx,
                                    queuedSong,
                                    ->
                                    val isDragging =
                                        draggingIndex == idx
                                    val elevation by
                                        animateDpAsState(
                                            if (isDragging) {
                                                8.dp
                                            } else {
                                                0.dp
                                            },
                                            label =
                                                "drag-elev",
                                        )
                                    val scale by
                                        animateFloatAsState(
                                            if (isDragging) {
                                                1.03f
                                            } else {
                                                1f
                                            },
                                            label =
                                                "drag-scale",
                                        )
                                    val offsetDp =
                                        with(
                                            LocalDensity
                                                .current,
                                        ) {
                                            dragOffset
                                                .toDp()
                                        }
                                    val animatedOffset by
                                        animateDpAsState(
                                            if (isDragging) {
                                                offsetDp
                                            } else {
                                                0.dp
                                            },
                                            label =
                                                "drag-offset",
                                        )

                                    var albumArtBitmap by
                                        remember(
                                            queuedSong
                                                .uri,
                                        ) {
                                            mutableStateOf<
                                                Bitmap?,
                                            >(
                                                null,
                                            )
                                        }
                                    LaunchedEffect(queuedSong.uri) {
                                        albumArtBitmap =
                                            withContext(
                                                Dispatchers
                                                    .IO,
                                            ) {
                                                try {
                                                    val retriever =
                                                        MediaMetadataRetriever()
                                                    retriever
                                                        .setDataSource(
                                                            context,
                                                            queuedSong
                                                                .uri,
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
                                                                    it.size,
                                                                )
                                                        }
                                                } catch (
                                                    _: Exception,
                                                ) {
                                                    null
                                                }
                                            }
                                    }

                                    Row(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .offset(
                                                    y =
                                                        if (isDragging) {
                                                            animatedOffset
                                                        } else {
                                                            0.dp
                                                        },
                                                ).graphicsLayer(
                                                    scaleX =
                                                    scale,
                                                    scaleY =
                                                    scale,
                                                    shadowElevation =
                                                        elevation
                                                            .value,
                                                ).shadow(
                                                    elevation,
                                                    RoundedCornerShape(
                                                        12.dp,
                                                    ),
                                                    clip =
                                                    true,
                                                ).background(
                                                    MaterialTheme.colorScheme.surface,
                                                    RoundedCornerShape(
                                                        12.dp,
                                                    ),
                                                ).border(
                                                    width =
                                                        if (isDragging) {
                                                            2.dp
                                                        } else {
                                                            1.dp
                                                        },
                                                    color =
                                                        if (isDragging) {
                                                            MaterialTheme.colorScheme.primary
                                                        } else {
                                                            MaterialTheme.colorScheme.outline
                                                        },
                                                    shape =
                                                        RoundedCornerShape(
                                                            12.dp,
                                                        ),
                                                ).padding(
                                                    horizontal =
                                                        12.dp,
                                                    vertical =
                                                        10.dp,
                                                ),
                                        verticalAlignment =
                                            Alignment
                                                .CenterVertically,
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(
                                                        48.dp,
                                                    ).clip(
                                                        RoundedCornerShape(
                                                            8.dp,
                                                        ),
                                                    ).background(
                                                        MaterialTheme.colorScheme.outline,
                                                    ),
                                        ) {
                                            if (albumArtBitmap !=
                                                null
                                            ) {
                                                Image(
                                                    painter =
                                                        BitmapPainter(
                                                            albumArtBitmap!!
                                                                .asImageBitmap(),
                                                        ),
                                                    contentDescription =
                                                    null,
                                                    modifier =
                                                        Modifier.fillMaxSize(),
                                                    contentScale =
                                                        ContentScale
                                                            .Crop,
                                                )
                                            } else {
                                                Icon(
                                                    imageVector =
                                                        Icons.Default
                                                            .MusicNote,
                                                    contentDescription =
                                                    null,
                                                    tint =
                                                        MaterialTheme.colorScheme.onBackground
                                                            .copy(
                                                                alpha =
                                                                0.5f,
                                                            ),
                                                    modifier =
                                                        Modifier
                                                            .size(
                                                                24.dp,
                                                            ).align(
                                                                Alignment
                                                                    .Center,
                                                            ),
                                                )
                                            }
                                        }

                                        Spacer(
                                            Modifier.width(
                                                12.dp,
                                            ),
                                        )

                                        Column(
                                            modifier =
                                                Modifier.weight(
                                                    1f,
                                                ),
                                        ) {
                                            Text(
                                                text =
                                                    queuedSong
                                                        .title,
                                                color =
                                                    MaterialTheme.colorScheme.onBackground,
                                                fontSize =
                                                    16.sp,
                                                maxLines =
                                                1,
                                                overflow =
                                                    TextOverflow
                                                        .Ellipsis,
                                            )
                                            Text(
                                                text =
                                                    queuedSong
                                                        .artist,
                                                color =
                                                    LocalExtendedColors.current.textSecondarySoft,
                                                fontSize =
                                                    13.sp,
                                                maxLines =
                                                1,
                                                overflow =
                                                    TextOverflow
                                                        .Ellipsis,
                                            )
                                        }

                                        Icon(
                                            imageVector =
                                                Icons.Default
                                                    .DragHandle,
                                            contentDescription =
                                                "Reordenar",
                                            tint =
                                                if (isDragging) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onBackground
                                                        .copy(
                                                            alpha =
                                                            0.7f,
                                                        )
                                                },
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

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongTitleSection(
    title: String,
    artist: String,
    album: String,
    albumArt: Bitmap?,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
    val isCompactLayout = aspectRatio >= 0.90f && aspectRatio <= 1.15f
    val isNormalLayout = aspectRatio > 1.15f && aspectRatio <= 1.6f
    val isTallLayout = aspectRatio < 0.80f

    val titleFontSize =
        when {
            isCompactLayout -> 16.sp
            isNormalLayout -> 18.sp
            isTallLayout -> 24.sp
            else -> 22.sp
        }
    val subtitleFontSize =
        when {
            isCompactLayout -> 11.sp
            isNormalLayout -> 12.sp
            isTallLayout -> 15.sp
            else -> 14.sp
        }
    val horizontalPadding =
        when {
            isCompactLayout -> 10.dp
            isNormalLayout -> 12.dp
            isTallLayout -> 20.dp
            else -> 24.dp
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = titleFontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.fillMaxWidth().basicMarquee(),
        )

        Spacer(Modifier.height(4.dp))

        Box {
            Text(
                text = if (album.isNotEmpty()) "$artist - $album" else artist,
                color = MaterialTheme.extendedColors.textSecondary,
                fontSize = subtitleFontSize,
                maxLines = 1,
                modifier = Modifier.clickable { showMenu = true },
            )
            if (showMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showMenu = false },
                    sheetState = sheetState,
                    containerColor = LocalExtendedColors.current.surfaceSheet,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 20.dp,
                                    vertical = 16.dp,
                                ),
                    ) {
                        Text(
                            text = "Opciones",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(Modifier.height(12.dp))

                        // Fila: Ir al artista + imagen a la izquierda +
                        // nombre a la derecha
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMenu = false
                                        onArtistClick()
                                    }.padding(vertical = 8.dp),
                            verticalAlignment =
                                Alignment.CenterVertically,
                        ) {
                            // Imagen del artista: usar albumArt si no
                            // hay imagen específica
                            Box(
                                modifier =
                                    Modifier
                                        .size(56.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                8.dp,
                                            ),
                                        ).background(
                                            MaterialTheme.colorScheme.outline,
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (albumArt != null) {
                                    Image(
                                        painter =
                                            BitmapPainter(
                                                albumArt.asImageBitmap(),
                                            ),
                                        contentDescription =
                                            "Artist image",
                                        modifier =
                                            Modifier.fillMaxSize(),
                                        contentScale =
                                            ContentScale
                                                .Crop,
                                    )
                                } else {
                                    Icon(
                                        imageVector =
                                            Icons.Default
                                                .Person,
                                        contentDescription =
                                        null,
                                        tint =
                                            MaterialTheme.colorScheme.onBackground
                                                .copy(
                                                    alpha =
                                                    0.6f,
                                                ),
                                        modifier =
                                            Modifier.size(
                                                28.dp,
                                            ),
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ir al artista",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                )
                                Text(
                                    text = artist,
                                    color = LocalExtendedColors.current.textSecondarySoft,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Fila: Ir al álbum + imagen a la izquierda +
                        // nombre a la derecha
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMenu = false
                                        onAlbumClick()
                                    }.padding(vertical = 8.dp),
                            verticalAlignment =
                                Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(56.dp)
                                        .clip(
                                            RoundedCornerShape(
                                                8.dp,
                                            ),
                                        ).background(MaterialTheme.colorScheme.outline),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (albumArt != null) {
                                    Image(
                                        painter =
                                            BitmapPainter(
                                                albumArt.asImageBitmap(),
                                            ),
                                        contentDescription =
                                            "Album art",
                                        modifier =
                                            Modifier.fillMaxSize(),
                                        contentScale =
                                            ContentScale
                                                .Crop,
                                    )
                                } else {
                                    Icon(
                                        imageVector =
                                            Icons.Default
                                                .MusicNote,
                                        contentDescription =
                                        null,
                                        tint =
                                            MaterialTheme.colorScheme.onBackground
                                                .copy(
                                                    alpha =
                                                    0.6f,
                                                ),
                                        modifier =
                                            Modifier.size(
                                                28.dp,
                                            ),
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ir al álbum",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                )
                                Text(
                                    text = album,
                                    color = LocalExtendedColors.current.textSecondarySoft,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlayerControls(
    playbackViewModel: PlaybackViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    isPlaying: Boolean,
    audioFormat: String = "",
    audioBitrate: String = "",
    audioSampleRate: String = "",
) {
    val playerState by playbackViewModel.playerState.collectAsState()
    val isShuffle by playbackViewModel.isShuffle.collectAsState()
    val repeatMode by playbackViewModel.repeatMode.collectAsState()

    var sliderPosition by remember { mutableStateOf(0f) }
    var isUserSeeking by remember { mutableStateOf(false) }

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
    val isCompactLayout = aspectRatio >= 0.90f && aspectRatio <= 1.15f
    val isNormalLayout = aspectRatio > 1.15f && aspectRatio <= 1.6f
    val isTallLayout = aspectRatio < 0.8f

    // Sincroniza el slider con el estado global solo si no se está arrastrando
    LaunchedEffect(playerState.position, playerState.duration, isUserSeeking) {
        if (!isUserSeeking) {
            sliderPosition = playerState.position.toFloat().coerceIn(0f, playerState.duration.toFloat())
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Slider(
                value = sliderPosition,
                onValueChange = {
                    isUserSeeking = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    playbackViewModel.seekTo(sliderPosition.toLong())
                    isUserSeeking = false
                },
                valueRange = 0f..playerState.duration.toFloat(),
                modifier = Modifier.fillMaxWidth().height(20.dp),
                colors =
                    SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.extendedColors.textSecondarySoft,
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDuration(playerState.position),
                    color = LocalExtendedColors.current.textSecondarySoft,
                    fontSize = 10.sp,
                )
                Text(
                    text = formatDuration(playerState.duration),
                    color = LocalExtendedColors.current.textSecondarySoft,
                    fontSize = 10.sp,
                )
            }
        }

        Spacer(
            Modifier.height(
                when {
                    isCompactLayout -> 4.dp
                    isNormalLayout -> 6.dp
                    isTallLayout -> 16.dp
                    else -> 12.dp
                },
            ),
        )

        val buttonSize =
            with(LocalDensity.current) {
                when {
                    isCompactLayout -> (LocalConfiguration.current.screenWidthDp.dp * 0.14f)
                    isNormalLayout -> (LocalConfiguration.current.screenWidthDp.dp * 0.15f)
                    isTallLayout -> (LocalConfiguration.current.screenWidthDp.dp * 0.20f)
                    else -> (LocalConfiguration.current.screenWidthDp.dp * 0.18f)
                }
            }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = { playbackViewModel.toggleShuffle() }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(buttonSize),
                )
            }

            IconButton(onClick = { playbackViewModel.playPreviousSong() }) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(buttonSize),
                )
            }

            IconButton(
                onClick = { playbackViewModel.togglePlayPause() },
                modifier =
                    Modifier
                        .size(buttonSize)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50),
                        ),
            ) {
                Icon(
                    imageVector =
                        if (isPlaying) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(buttonSize * 0.5f),
                )
            }

            IconButton(onClick = { playbackViewModel.playNextSong() }) {
                Icon(
                    Icons.Rounded.SkipNext,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(buttonSize),
                )
            }

            IconButton(onClick = { playbackViewModel.toggleRepeat() }) {
                Icon(
                    when (repeatMode) {
                        RepeatMode.NONE -> Icons.Rounded.Repeat
                        RepeatMode.ONE -> Icons.Rounded.RepeatOne
                        RepeatMode.ALL -> Icons.Rounded.Repeat
                    },
                    contentDescription = "Repeat",
                    tint =
                        if (repeatMode != RepeatMode.NONE) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.White
                        },
                    modifier = Modifier.size(buttonSize),
                )
            }
        }

        // Información de formato de audio
        if (audioFormat.isNotEmpty() || audioBitrate.isNotEmpty() || audioSampleRate.isNotEmpty()) {
            Spacer(
                Modifier.height(
                    when {
                        isCompactLayout -> 6.dp
                        isNormalLayout -> 6.dp
                        isTallLayout -> 14.dp
                        else -> 12.dp
                    },
                ),
            )
            Text(
                text =
                    buildString {
                        if (audioFormat.isNotEmpty()) append(audioFormat)
                        if (audioFormat.isNotEmpty() && audioBitrate.isNotEmpty()) append(" • ")
                        if (audioBitrate.isNotEmpty()) append(audioBitrate)
                        if ((audioFormat.isNotEmpty() || audioBitrate.isNotEmpty()) && audioSampleRate.isNotEmpty()) append(" • ")
                        if (audioSampleRate.isNotEmpty()) append(audioSampleRate)
                    },
                color = LocalExtendedColors.current.texMeta,
                fontSize =
                    when {
                        isCompactLayout -> 9.sp
                        isNormalLayout -> 10.sp
                        isTallLayout -> 12.sp
                        else -> 11.sp
                    },
                textAlign = TextAlign.Center,
            )
        }
    }
}
