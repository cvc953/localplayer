package com.cvc953.localplayer.ui.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.res.stringResource
import com.cvc953.localplayer.R
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.model.Artist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.components.AlphabetScrollerContent
import com.cvc953.localplayer.ui.components.ScrollLetterDisplay
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.ui.theme.md_textSecondary
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.collections.isNotEmpty
import kotlin.collections.map

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistsScreen(
    artistViewModel: ArtistViewModel,
    playbackViewModel: PlaybackViewModel,
    onArtistClick: (artistName: String) -> Unit,
) {
    // Use the global SongViewModel to fetch all songs so we can build artist thumbnails
    val songViewModel: SongViewModel = viewModel()
    val songs by songViewModel.songs.collectAsState()
    val artists by artistViewModel.artists.collectAsState()

    // Expand and deduplicate artist names using normalization
    val expandedArtists =
        remember(artists) {
            val seen = mutableSetOf<String>()
            val result = mutableListOf<Artist>()
            for (artist in artists) {
                for (name in normalizeArtistName(artist.name)) {
                    val norm = name.trim()
                    if (norm.isNotEmpty() && seen.add(norm.lowercase())) {
                        result.add(artist.copy(name = norm))
                    }
                }
            }
            result
        }
    val isScanning by songViewModel.isScanning.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(ArtistSortMode.TITLE_ASC) }
    var viewAsGrid by rememberSaveable { mutableStateOf(artistViewModel.isGridViewPreferred()) }
    val context = LocalContext.current
    val activity = context as? Activity
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 1000) {
            activity?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, context.getString(R.string.toast_press_back_again), Toast.LENGTH_SHORT).show()
        }
    }

    val filteredArtists =
        remember(expandedArtists, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) {
                expandedArtists
            } else {
                expandedArtists.filter {
                    it.name.lowercase().contains(q)
                }
            }
        }

    val sortedArtists =
        remember(filteredArtists, sortMode) {
            when (sortMode) {
                ArtistSortMode.TITLE_ASC -> filteredArtists.sortedBy { it.name.lowercase() }
                ArtistSortMode.TITLE_DESC -> filteredArtists.sortedByDescending { it.name.lowercase() }
            }
        }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var currentScrollLetter by remember { mutableStateOf<String?>(null) }

    if (isScanning) {
        Column(
            modifier = Modifier.Companion.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Companion.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.Companion.height(16.dp))
            Text(stringResource(R.string.scanning_songs), color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    Box(
        modifier = Modifier.Companion.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.Companion.fillMaxSize()) {
            Row(
                modifier = Modifier.Companion.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.Companion.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.artists_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Companion.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.Companion.weight(1f),
                )

                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = stringResource(R.string.action_sort),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        containerColor = MaterialTheme.extendedColors.surfaceSheet,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.sort_title_asc),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                sortMode = ArtistSortMode.TITLE_ASC
                                sortMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.sort_title_desc),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                sortMode = ArtistSortMode.TITLE_DESC
                                sortMenuExpanded = false
                            },
                        )
                    }
                }

                IconButton(
                    onClick = {
                        showSearchBar = !showSearchBar
                        if (!showSearchBar) searchQuery = ""
                    },
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.action_search),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(onClick = {
                    viewAsGrid = !viewAsGrid
                    artistViewModel.setGridViewPreferred(viewAsGrid)
                }) {
                    Icon(
                        imageVector = if (viewAsGrid) Icons.Default.ViewList else Icons.Default.ViewModule,
                        contentDescription = stringResource(R.string.action_toggle_view),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            if (showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    placeholder = {
                        Text(
                            stringResource(R.string.search_artists_placeholder),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    modifier = Modifier.Companion.fillMaxWidth().padding(horizontal = 16.dp),
                    colors =
                        TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                )
            }

            Box(modifier = Modifier.Companion.fillMaxSize()) {
                if (viewAsGrid) {
                    LazyVerticalGrid(
                        modifier = Modifier.Companion.fillMaxSize(),
                        columns = GridCells.Adaptive(140.dp),
                        state = gridState,
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedArtists) { artist ->
                            val context = LocalContext.current
                            // Buscar cualquier canción donde el artista normalizado coincida
                            val firstSong =
                                songs.firstOrNull { song ->
                                    normalizeArtistName(song.artist).any {
                                        it.equals(
                                            artist.name.trim(),
                                            ignoreCase = true,
                                        )
                                    }
                                }
                            var artistArt by remember(firstSong?.uri) { mutableStateOf<Bitmap?>(null) }

                            LaunchedEffect(firstSong?.uri, firstSong?.filePath) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val uri = firstSong?.uri ?: return@withContext
                                        Log.d(
                                            "ArtistsScreen",
                                            "Loading art for artist='${artist.name}' uri=$uri filePath=${firstSong?.filePath}",
                                        )
                                        val retriever = MediaMetadataRetriever()
                                        retriever.setDataSource(context, uri)
                                        val embedded = retriever.embeddedPicture
                                        if (embedded != null && embedded.isNotEmpty()) {
                                            artistArt =
                                                BitmapFactory.decodeByteArray(
                                                    embedded,
                                                    0,
                                                    embedded.size,
                                                )
                                            Log.d(
                                                "ArtistsScreen",
                                                "Embedded art found for uri=$uri size=${embedded.size}",
                                            )
                                        } else {
                                            Log.d("ArtistsScreen", "No embedded art for uri=$uri")
                                        }
                                        retriever.release()

                                        if (artistArt == null) {
                                            Log.d(
                                                "ArtistsScreen",
                                                "Trying contentResolver fallback for uri=$uri",
                                            )
                                            try {
                                                context.contentResolver
                                                    .openInputStream(uri)
                                                    ?.use { stream ->
                                                        artistArt =
                                                            BitmapFactory.decodeStream(stream)
                                                    }
                                            } catch (e: Exception) {
                                                Log.d(
                                                    "ArtistsScreen",
                                                    "contentResolver fallback failed: ${e.message}",
                                                )
                                            }
                                        }

                                        if (artistArt == null) {
                                            Log.d(
                                                "ArtistsScreen",
                                                "Trying file fallback for filePath=${firstSong?.filePath}",
                                            )
                                            val path = firstSong?.filePath
                                            if (!path.isNullOrBlank()) {
                                                try {
                                                    val dir = File(path).parentFile
                                                    val candidates =
                                                        listOf(
                                                            "cover.jpg",
                                                            "folder.jpg",
                                                            "album.jpg",
                                                            "front.jpg",
                                                            "cover.png",
                                                            "folder.png",
                                                        )
                                                    for (name in candidates) {
                                                        val f = File(dir, name)
                                                        if (f.exists() && f.length() > 0) {
                                                            artistArt =
                                                                BitmapFactory.decodeFile(f.absolutePath)
                                                            if (artistArt != null) {
                                                                Log.d(
                                                                    "ArtistsScreen",
                                                                    "Found external cover file=${f.absolutePath}",
                                                                )
                                                                break
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.d(
                                            "ArtistsScreen",
                                            "Unexpected error loading art: ${e.message}",
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier =
                                    Modifier.Companion
                                        .fillMaxWidth()
                                        .clickable { onArtistClick(artist.name) }
                                        .padding(6.dp),
                                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                            ) {
                                Box(modifier = Modifier.Companion.size(120.dp)) {
                                    Image(
                                        painter =
                                            artistArt?.let { BitmapPainter(it.asImageBitmap()) }
                                                ?: painterResource(R.drawable.ic_default_album),
contentDescription = stringResource(R.string.album_cover),
                                        modifier =
                                            Modifier.Companion
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(100.dp)),
                                        contentScale = ContentScale.Companion.Crop,
                                    )
                                    var menuExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.Companion.align(Alignment.Companion.TopEnd)) {
                                        IconButton(onClick = { menuExpanded = true }) {
                                            Icon(
                                                Icons.Default.MoreVert,
contentDescription = stringResource(R.string.action_more_options),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuExpanded,
                                            onDismissRequest = { menuExpanded = false },
                                            containerColor = MaterialTheme.extendedColors.surfaceSheet,
                                            modifier = Modifier.Companion.background(MaterialTheme.extendedColors.surfaceSheet),
                                        ) {
                                            // Solo usar las canciones del artista actual para el dropdown (más rápido)
                                            val artistSongs =
                                                songs
                                                    .filter {
                                                        it.artist.trim().equals(
                                                            artist.name.trim(),
                                                            ignoreCase = true,
                                                        )
                                                    }.sortedWith(
                                                        compareBy<Song>(
                                                            { it.album },
                                                            { it.discNumber },
                                                            { it.trackNumber },
                                                        ),
                                                    )
                                            val firstSongOfArtist = artistSongs.firstOrNull()
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.action_play_now),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                },
                                                onClick = {
                                                    menuExpanded = false
                                                    if (artistSongs.isNotEmpty()) {
                                                        playbackViewModel.setShuffle(false)
                                                        playbackViewModel.playArtist(
                                                            artist.name,
                                                            artistSongs,
                                                            songs,
                                                        )
                                                        playbackViewModel.updateDisplayOrder(
                                                            artistSongs,
                                                        )
                                                        playbackViewModel.play(artistSongs[0])
                                                    }
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.action_add_next),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                },
                                                onClick = {
                                                    menuExpanded = false
                                                
                                                    // NO filter duplicates when adding full artist
                                                    val toAdd = artistSongs
                                                    playbackViewModel.addToQueueNextAll(toAdd)
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            context.getString(R.string.toast_added_next_count, toAdd.size),
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                },
                                            )
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.action_add_to_queue_end),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                },
                                                onClick = {
                                                    menuExpanded = false
                                                
                                                    // NO filter duplicates when adding full artist
                                                    val toAdd = artistSongs
                                                    playbackViewModel.addToQueueEndAll(toAdd)
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            context.getString(R.string.toast_added_queue_end_count, toAdd.size),
                                                            Toast.LENGTH_SHORT,
                                                        ).show()
                                                },
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.Companion.height(6.dp))
                                Text(
                                    text = artist.name,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    textAlign = TextAlign.Companion.Center,
                                    overflow = TextOverflow.Companion.Ellipsis,
                                )
                                // Contar todas las canciones donde el artista aparece (normalizado)
                                val songCount =
                                    songs.count { song ->
                                        normalizeArtistName(song.artist).any {
                                            it.equals(
                                                artist.name.trim(),
                                                ignoreCase = true,
                                            )
                                        }
                                    }
                                Text(
                                    text = stringResource(R.string.songs_count, songCount),
                                    color = MaterialTheme.extendedColors.textSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.Companion.fillMaxSize(),
                        state = listState,
                        contentPadding =
                            PaddingValues(
                                start = 16.dp,
                                top = 16.dp,
                                bottom = 16.dp,
                                end = 16.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedArtists) { artist ->
                            val context = LocalContext.current
                            val firstSong =
                                songs.firstOrNull {
                                    it.artist.trim().equals(artist.name.trim(), ignoreCase = true)
                                }
                            var artistArt by remember(firstSong?.uri) { mutableStateOf<Bitmap?>(null) }

                            LaunchedEffect(firstSong?.uri, firstSong?.filePath) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val uri = firstSong?.uri ?: return@withContext
                                        val retriever = MediaMetadataRetriever()
                                        retriever.setDataSource(context, uri)
                                        val embedded = retriever.embeddedPicture
                                        if (embedded != null && embedded.isNotEmpty()) {
                                            artistArt =
                                                BitmapFactory.decodeByteArray(
                                                    embedded,
                                                    0,
                                                    embedded.size,
                                                )
                                        }
                                        retriever.release()

                                        if (artistArt == null) {
                                            try {
                                                context.contentResolver
                                                    .openInputStream(uri)
                                                    ?.use { stream ->
                                                        artistArt =
                                                            BitmapFactory.decodeStream(stream)
                                                    }
                                            } catch (_: Exception) {
                                            }
                                        }

                                        if (artistArt == null) {
                                            val path = firstSong.filePath
                                            if (!path.isNullOrBlank()) {
                                                try {
                                                    val dir = File(path).parentFile
                                                    val candidates =
                                                        listOf(
                                                            "cover.jpg",
                                                            "folder.jpg",
                                                            "album.jpg",
                                                            "front.jpg",
                                                            "cover.png",
                                                            "folder.png",
                                                        )
                                                    for (name in candidates) {
                                                        val f = File(dir, name)
                                                        if (f.exists() && f.length() > 0) {
                                                            artistArt =
                                                                BitmapFactory.decodeFile(f.absolutePath)
                                                            if (artistArt != null) break
                                                        }
                                                    }
                                                } catch (_: Exception) {
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {
                                    }
                                }
                            }

                            Row(
                                modifier =
                                    Modifier.Companion.fillMaxWidth().padding(8.dp).clickable {
                                        onArtistClick(artist.name)
                                    },
                                verticalAlignment = Alignment.Companion.CenterVertically,
                            ) {
                                Image(
                                    painter =
                                        artistArt?.let {
                                            androidx.compose.ui.graphics.painter.BitmapPainter(
                                                it.asImageBitmap(),
                                            )
                                        } ?: painterResource(R.drawable.ic_default_album),
                                    contentDescription = stringResource(R.string.album_cover),
                                    modifier =
                                        Modifier.Companion.size(60.dp).clip(
                                            androidx.compose.foundation.shape
                                                .RoundedCornerShape(100.dp),
                                        ),
                                    contentScale = ContentScale.Companion.Crop,
                                )

                                Spacer(modifier = Modifier.Companion.width(12.dp))

                                Column(modifier = Modifier.Companion.weight(1f)) {
                                    Text(
                                        text = artist.name,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Companion.Ellipsis,
                                    )
                                    Text(
                                        text = stringResource(R.string.songs_count, artist.songCount),
                                        color = md_textSecondary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                    )
                                }

                                var menuExpanded by remember { mutableStateOf(false) }

                                Box {
                                    IconButton(onClick = { menuExpanded = true }) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = stringResource(R.string.action_more_options),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false },
                                        containerColor = MaterialTheme.extendedColors.surfaceSheet,
                                        modifier = Modifier.Companion.background(MaterialTheme.extendedColors.surfaceSheet),
                                    ) {
                                        // Agrupar todas las canciones por artista, orden alfabético
                                        val artistsOrdered = sortedArtists.map { it.name.trim() }
                                        val artistSongs =
                                            songs
                                                .filter { song ->
                                                    normalizeArtistName(
                                                        song.artist,
                                                    ).any {
                                                        it.equals(
                                                            artist.name.trim(),
                                                            ignoreCase = true,
                                                        )
                                                    }
                                                }.sortedWith(
                                                    compareBy<Song>(
                                                        { it.album },
                                                        { it.discNumber },
                                                        { it.trackNumber },
                                                    ),
                                                )
                                        val firstSongOfArtist = artistSongs.firstOrNull()
                                        val startIndex = 0
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.action_play_now),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                                if (artistSongs.isNotEmpty()) {
                                                    playbackViewModel.setShuffle(false)
                                                    playbackViewModel.playArtist(
                                                        artist.name,
                                                        artistSongs,
                                                        songs,
                                                    )
                                                    playbackViewModel.updateDisplayOrder(artistSongs)
                                                    playbackViewModel.play(artistSongs[0])
                                                }
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.action_add_next),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                            
                                                // NO filter duplicates when adding full artist
                                                val toAdd = artistSongs
                                                playbackViewModel.addToQueueNextAll(toAdd)
                                                Toast
                                                    .makeText(
                                                        context,
                                                        context.getString(R.string.toast_added_next_count, toAdd.size),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            },
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    stringResource(R.string.action_add_to_queue_end),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                )
                                            },
                                            onClick = {
                                                menuExpanded = false
                                            
                                                // NO filter duplicates when adding full artist
                                                val toAdd = artistSongs
                                                playbackViewModel.addToQueueEndAll(toAdd)
                                                Toast
                                                    .makeText(
                                                        context,
                                                        context.getString(R.string.toast_added_queue_end_count, toAdd.size),
                                                        Toast.LENGTH_SHORT,
                                                    ).show()
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (sortedArtists.isNotEmpty()) {
                    AlphabetScrollerContent(
                        items = sortedArtists,
                        getItemName = { it.name },
                        currentScrollLetter = currentScrollLetter,
                        onLetterSelected = { letter ->
                            currentScrollLetter = letter
                        },
                        onScrollToIndex = { index, isGrid ->
                            scope.launch {
                                if (isGrid) {
                                    gridState.scrollToItem(index)
                                } else {
                                    listState.scrollToItem(index)
                                }
                            }
                        },
                        viewAsGrid = viewAsGrid,
                        scope = scope,
                    )
                }

                currentScrollLetter?.let { letter ->
                    ScrollLetterDisplay(letter = letter)
                }
            }
        }
    }
}

private enum class ArtistSortMode {
    TITLE_ASC,
    TITLE_DESC,
}

// Normaliza nombres de artistas, separando por ',' y '/' excepto 'AC/DC'
fun normalizeArtistName(artist: String): List<String> {
    val trimmed = artist.trim()
    return if (trimmed.equals("AC/DC", ignoreCase = true)) {
        listOf("AC/DC")
    } else {
        trimmed.split(',', '/').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
