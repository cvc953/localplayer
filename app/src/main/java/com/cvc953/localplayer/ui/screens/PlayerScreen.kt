package com.cvc953.localplayer.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.MiniPlayer
import com.cvc953.localplayer.ui.components.LyricsView
import com.cvc953.localplayer.ui.components.PlayerConfig
import com.cvc953.localplayer.ui.components.PlayerControlActions
import com.cvc953.localplayer.ui.components.PlayerControlState
import com.cvc953.localplayer.ui.components.PlayerControlsContent
import com.cvc953.localplayer.ui.components.SongTitleSection
import com.cvc953.localplayer.ui.components.TtmlLyricsView
import com.cvc953.localplayer.ui.components.parseAlbumArtShape
import com.cvc953.localplayer.ui.components.parsePlayPauseStyle
import com.cvc953.localplayer.ui.components.parseProgressBarStyle
import com.cvc953.localplayer.ui.components.parseTransportStyle
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.util.darken
import com.cvc953.localplayer.util.getDominantColor
import com.cvc953.localplayer.viewmodel.LyricsViewModel
import com.cvc953.localplayer.viewmodel.MainViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.toInt

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playbackViewModel: PlaybackViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    playlistViewModel: PlaylistViewModel = viewModel(),
    lyricsViewModel: LyricsViewModel = viewModel(),
    songViewModel: SongViewModel = viewModel(),
    mainViewModel: MainViewModel = viewModel(),
    onCollapse: () -> Unit,
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToAlbum: (String, String) -> Unit = { _, _ -> },
    onBackgroundColorChanged: (Color) -> Unit = {},
) {
    val showLyrics by playerViewModel.showLyrics.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()
    val queue by playbackViewModel.queue.collectAsState()
    val songs by songViewModel.songs.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val isShuffle by playbackViewModel.isShuffle.collectAsState()
    val dynamicColorEnabled by mainViewModel.dynamicColorEnabled.collectAsState()
    val repeatMode by playbackViewModel.repeatMode.collectAsState()
    val song = playerState.currentSong ?: return
    val albumArtShapeKey by mainViewModel.albumArtShape.collectAsState()
    val progressBarStyleKey by mainViewModel.progressBarStyle.collectAsState()
    val transportStyleKey by mainViewModel.transportStyle.collectAsState()
    val playPauseStyleKey by mainViewModel.playPauseStyle.collectAsState()
    val showAudioInfo by mainViewModel.showAudioInfo.collectAsState()
    val config =
        PlayerConfig(
            albumArtShape = parseAlbumArtShape(albumArtShapeKey),
            progressBarStyle = parseProgressBarStyle(progressBarStyleKey),
            transportStyle = parseTransportStyle(transportStyleKey),
            playPauseStyle = parsePlayPauseStyle(playPauseStyleKey),
            showAudioInfo = showAudioInfo,
        )

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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
    val dragList = remember { mutableStateListOf<Song>() }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    // Extraer información de formato de audio
    var audioFormat by remember { mutableStateOf("") }
    var audioBitrate by remember { mutableStateOf("") }
    var audioSampleRate by remember { mutableStateOf("") }
    val color = MaterialTheme.colorScheme.background

    LaunchedEffect(song.uri) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri)

                // Obtener mime type para el formato
                val mimeType =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
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
                val sampleRate =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
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

    // Notificar al padre del color de fondo (topo del gradiente)
    LaunchedEffect(dominantColor, dynamicColorEnabled) {
        val topColor = if (dynamicColorEnabled) dominantColor.darken(0.5f) else color
        onBackgroundColorChanged(topColor)
    }

    LaunchedEffect(song.id, playlists) {
        isFavorite = playlistViewModel.isSongInPlaylist("Favoritos", song.id)
    }

    BackHandler(enabled = showLyrics) { playerViewModel.toggleLyrics() }
    BackHandler(enabled = !showLyrics) { onCollapse() }

    LaunchedEffect(song) { lyricsViewModel.loadLyricsForSong(song) }

    // Determine background based on dynamic color setting
    val backgroundColor =
        if (dynamicColorEnabled) {
            Brush.verticalGradient(
                listOf(
                    dominantColor.darken(0.5f),
                    dominantColor.darken(0.1f),
                ),
            )
        } else {
            Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background),
            )
        }

    val forceLightForeground = dynamicColorEnabled && MaterialTheme.colorScheme.background.luminance() > 0.5f
    val playerPrimaryColor = if (forceLightForeground) Color.White else MaterialTheme.colorScheme.onBackground
    val playerSecondaryColor =
        if (forceLightForeground) {
            Color.White.copy(alpha = 0.74f)
        } else {
            LocalExtendedColors.current.textSecondary
        }
    val playerMetaColor =
        if (forceLightForeground) {
            Color.White.copy(alpha = 0.64f)
        } else {
            LocalExtendedColors.current.texMeta
        }

    val controlState =
        PlayerControlState(
            isPlaying = playerState.isPlaying,
            currentPosition = playerState.position,
            duration = playerState.duration,
            isShuffle = isShuffle,
            repeatMode = repeatMode,
            isFavorite = isFavorite,
            audioFormat = audioFormat,
            audioBitrate = audioBitrate,
            audioSampleRate = audioSampleRate,
            primaryContentColor = playerPrimaryColor,
            secondaryContentColor = playerSecondaryColor,
            metaColor = playerMetaColor,
            dominantColor = dominantColor,
        )
    val controlActions =
        PlayerControlActions(
            onPlayPause = { playbackViewModel.togglePlayPause() },
            onNext = { playbackViewModel.playNextSong() },
            onPrevious = { playbackViewModel.playPreviousSong() },
            onSeek = { playbackViewModel.seekTo(it) },
            onSeekStart = { },
            onSeekEnd = { },
            onShuffleToggle = { playbackViewModel.toggleShuffle() },
            onRepeatToggle = { playbackViewModel.toggleRepeat() },
            onFavoriteToggle = {
                val f = "Favoritos"
                if (isFavorite) {
                    playlistViewModel.removeSongFromPlaylist(f, song.id)
                    isFavorite = false
                    Toast.makeText(context, context.getString(R.string.removed_from_favorites), Toast.LENGTH_SHORT).show()
                } else {
                    val p = playlists.find { it.name == f }
                    if (p == null) {
                        playlistViewModel.createPlaylist(f)
                    }
                    playlistViewModel.addSongToPlaylist(f, song.id)
                    isFavorite = true
                    Toast.makeText(context, context.getString(R.string.added_to_favorites), Toast.LENGTH_SHORT).show()
                }
            },
            onShowQueue = { showQueue = true },
            onShowAddToPlaylist = { showAddToPlaylistDialog = true },
            onToggleLyrics = { playerViewModel.toggleLyrics() },
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(backgroundColor),
    ) {
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
                                useDynamicBackground = dynamicColorEnabled,
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
                                useDynamicBackground = dynamicColorEnabled,
                                onLineClick = { targetMs -> playbackViewModel.seekTo(targetMs) },
                            )
                        }

                        else -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(horizontal = 32.dp),
                            ) {
                                Text(
                                    text =
                                        stringResource(R.string.lyrics_not_available),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Companion.Medium,
                                    textAlign = TextAlign.Companion.Center,
                                    color = LocalExtendedColors.current.textSecondary,
                                )
                                Text(
                                    text = stringResource(R.string.lyrics_download_prompt),
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Companion.Center,
                                    color = LocalExtendedColors.current.textSecondary.copy(alpha = 0.7f),
                                )
                                Button(
                                    onClick = {
                                        val packageName = "com.cvc953.lyrio"
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        } else {
                                            val intent =
                                                Intent(
                                                    Intent.ACTION_VIEW,
                                                    Uri.parse("https://github.com/cvc953/TimeLyr/releases"),
                                                )
                                            context.startActivity(intent)
                                        }
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lyrics,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.download_timelyr))
                                }
                            }
                        }
                    }
                }

                // MiniPlayer al final, con padding para botones de navegación
                MiniPlayer(
                    song = song,
                    isPlaying = playerState.isPlaying,
                    onPlayPause = { playbackViewModel.togglePlayPause() },
                    onClick = { playerViewModel.toggleLyrics() },
                    onNext = { playbackViewModel.playNextSong() },
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        } else {
            // CONTENIDO REAL - Layout responsivo
            val screenWidth = LocalConfiguration.current.screenWidthDp
            val screenHeight = LocalConfiguration.current.screenHeightDp
            val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()

            // LOG TEMPORAL - Debug ratio detection
            Log.d("PlayerScreen", "Screen: ${screenWidth}x$screenHeight, Ratio: $aspectRatio")

            // Determinar tipo de pantalla (maneja portrait Y landscape)
            val isCompactLayout = aspectRatio >= 0.90f && aspectRatio < 1.15f // Cuadrada
            val isNormalLayout =
                (aspectRatio in 1.15f..1.6f) ||
                    (aspectRatio in 0.50f..<0.75f) // Normal landscape O portrait (TU 360x640=0.5625)
            val isLandscape = aspectRatio > 1.5f // Landscape ancho
            val isTallLayout = aspectRatio < 0.50f // Rectangular MUY alta (18:9 o más)

            // Tamaño dinámico de imagen
            val imageWidthPercent =
                when {
                    isLandscape -> 0.35f

                    // Landscape: 35%
                    isCompactLayout -> 0.45f

                    // Cuadrada: 45%
                    isNormalLayout -> 0.80f

                    // Normal/Media: 80% - TU PANTALLA
                    isTallLayout -> 0.88f

                    // Rectangular MUY alta: 88%
                    else -> 0.75f // Default: 75%
                }
            val effectiveArtWidth = imageWidthPercent

            // Spacers dinámicos según layout
            val betweenSpacer =
                when {
                    isCompactLayout -> 4.dp

                    // Cuadrada: mínimo
                    isNormalLayout -> 6.dp

                    // Normal/Media: muy compacto - AHORA SÍ DEBERÍA APLICARSE
                    isTallLayout -> 24.dp

                    // Rectangular alta: mucho
                    isLandscape -> 12.dp

                    // Landscape: moderado
                    else -> 16.dp // Default
                }

            // Padding vertical dinámico
            val verticalPadding =
                when {
                    isCompactLayout -> 4.dp
                    isNormalLayout -> 2.dp
                    else -> 8.dp
                }

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(0.5f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter =
                                albumArt?.asImageBitmap()?.let { BitmapPainter(it) }
                                    ?: painterResource(R.drawable.ic_default_album),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(0.85f).aspectRatio(1f).clip(config.albumArtShape.toComposeShape()),
                            contentScale = ContentScale.Companion.Crop,
                        )
                    }

                    Spacer(Modifier.width(24.dp))

                    Column(
                        modifier = Modifier.weight(0.5f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        SongTitleSection(
                            title = song.title,
                            artist = song.artist,
                            album = song.album,
                            albumArt = albumArt,
                            primaryContentColor = playerPrimaryColor,
                            secondaryContentColor = playerSecondaryColor,
                            onArtistClick = {
                                val mainArtist = normalizeArtistName(song.artist).firstOrNull() ?: song.artist
                                onNavigateToArtist(mainArtist)
                            },
                            onAlbumClick = { onNavigateToAlbum(song.album, song.artist) },
                        )
                        Spacer(Modifier.height(12.dp))

                        Spacer(Modifier.height(betweenSpacer))

                        PlayerControlsContent(config = config, state = controlState, actions = controlActions)
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = verticalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Image(
                        painter =
                            albumArt?.asImageBitmap()?.let { BitmapPainter(it) }
                                ?: painterResource(R.drawable.ic_default_album),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth(effectiveArtWidth).aspectRatio(1f).clip(config.albumArtShape.toComposeShape()),
                        contentScale = ContentScale.Companion.Crop,
                    )
                    Spacer(Modifier.Companion.height(betweenSpacer))

                    SongTitleSection(
                        title = song.title,
                        artist = song.artist,
                        album = song.album,
                        albumArt = albumArt,
                        primaryContentColor = playerPrimaryColor,
                        secondaryContentColor = playerSecondaryColor,
                        onArtistClick = {
                            val mainArtist = normalizeArtistName(song.artist).firstOrNull() ?: song.artist
                            onNavigateToArtist(mainArtist)
                        },
                        onAlbumClick = { onNavigateToAlbum(song.album, song.artist) },
                    )

                    PlayerControlsContent(config = config, state = controlState, actions = controlActions)

                    Spacer(Modifier.weight(1f))
                }
            }

            if (showAddToPlaylistDialog) {
                AlertDialog(
                    onDismissRequest = { showAddToPlaylistDialog = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    title = {
                        Text(
                            stringResource(R.string.add_to_playlist_title),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
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
                                        ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                        ),
                                ) {
                                    Text(stringResource(R.string.create_playlist_button))
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
                                            stringResource(R.string.dialog_create_playlist_title),
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
                                                    stringResource(R.string.playlist_name_placeholder),
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
                                                playlistViewModel.addSongToPlaylist(
                                                    newPlaylistName,
                                                    song.id,
                                                )
                                                Toast
                                                    .makeText(
                                                        context,
                                                        context.getString(R.string.playlist_created_and_added, newPlaylistName),
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
                                                stringResource(R.string.action_create),
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
                                                stringResource(R.string.action_cancel),
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        }
                                    },
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                            LazyColumn(
                                modifier =
                                    Modifier.Companion.heightIn(
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
                                                    playlistViewModel.addSongToPlaylist(
                                                        playlist.name,
                                                        song.id,
                                                    )
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            context.getString(R.string.song_added_to_playlist, playlist.name),
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                    showAddToPlaylistDialog = false
                                                },
                                        colors =
                                            CardDefaults
                                                .cardColors(
                                                    containerColor =
                                                        LocalExtendedColors.current.surfaceSheet,
                                                ),
                                        shape =
                                            androidx.compose.foundation.shape.RoundedCornerShape(
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
                        ) { Text(stringResource(R.string.action_cancel), color = MaterialTheme.colorScheme.primary) }
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
                            text = stringResource(R.string.queue_title_upcoming),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Companion.Bold,
                        )

                        Spacer(Modifier.height(12.dp))

                        if (dragList.isEmpty()) {
                            Text(
                                text = stringResource(R.string.queue_empty_upcoming),
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
                                                    val viewportStart =
                                                        layoutInfo.viewportStartOffset
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
                                                    androidx.compose.foundation.shape.RoundedCornerShape(
                                                        12.dp,
                                                    ),
                                                    clip =
                                                    true,
                                                ).background(
                                                    MaterialTheme.colorScheme.surface,
                                                    androidx.compose.foundation.shape.RoundedCornerShape(
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
                                                        androidx.compose.foundation.shape.RoundedCornerShape(
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
                                                Modifier.Companion
                                                    .size(
                                                        48.dp,
                                                    ).clip(
                                                        androidx.compose.foundation.shape.RoundedCornerShape(
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
                                                        androidx.compose.ui.graphics.painter.BitmapPainter(
                                                            albumArtBitmap!!
                                                                .asImageBitmap(),
                                                        ),
                                                    contentDescription =
                                                    null,
                                                    modifier =
                                                        Modifier.fillMaxSize(),
                                                    contentScale =
                                                        ContentScale.Companion
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
                                                    TextOverflow.Companion
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
                                                    TextOverflow.Companion
                                                        .Ellipsis,
                                            )
                                        }

                                        Icon(
                                            imageVector =
                                                Icons.Default
                                                    .DragHandle,
                                            contentDescription =
                                                stringResource(R.string.action_reorder),
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
