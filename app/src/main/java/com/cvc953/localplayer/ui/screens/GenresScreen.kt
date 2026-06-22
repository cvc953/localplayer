package com.cvc953.localplayer.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Genre
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.components.AlphabetScrollerContent
import com.cvc953.localplayer.ui.components.NativeSearchBar
import com.cvc953.localplayer.ui.components.ScrollLetterDisplay
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.ui.theme.md_textSecondary
import com.cvc953.localplayer.viewmodel.GenreViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel
import com.cvc953.localplayer.viewmodel.SongViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:function-naming")
@Composable
fun GenresScreen(
    genreViewModel: GenreViewModel,
    playbackViewModel: PlaybackViewModel,
    playerViewModel: PlayerViewModel,
    songViewModel: SongViewModel = viewModel(),
    onGenreClick: (genreName: String) -> Unit,
) {
    val genres by genreViewModel.genres.collectAsState()
    val songs by songViewModel.songs.collectAsState()
    val isLoading by genreViewModel.isLoading.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val toastPressBackAgain = stringResource(R.string.toast_press_back_again)

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 1000) {
            activity?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, toastPressBackAgain, Toast.LENGTH_SHORT).show()
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(GenreSortMode.TITLE_ASC) }
    var viewAsGrid by rememberSaveable { mutableStateOf(true) }

    val filteredGenres =
        remember(genres, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) {
                genres
            } else {
                genres.filter { it.name.lowercase().contains(q) }
            }
        }

    val sortedGenres =
        remember(filteredGenres, sortMode) {
            when (sortMode) {
                GenreSortMode.TITLE_ASC -> filteredGenres.sortedBy { it.name.lowercase() }
                GenreSortMode.TITLE_DESC -> filteredGenres.sortedByDescending { it.name.lowercase() }
            }
        }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    var currentScrollLetter by remember { mutableStateOf<String?>(null) }

    if (isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.scanning_songs),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.genres_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
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
                                sortMode = GenreSortMode.TITLE_ASC
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
                                sortMode = GenreSortMode.TITLE_DESC
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
                IconButton(onClick = { viewAsGrid = !viewAsGrid }) {
                    Icon(
                        imageVector = if (viewAsGrid) Icons.Default.ViewList else Icons.Default.ViewModule,
                        contentDescription = stringResource(R.string.action_toggle_view),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }

                var moreMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { moreMenuExpanded = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.action_more_options),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    DropdownMenu(
                        expanded = moreMenuExpanded,
                        onDismissRequest = { moreMenuExpanded = false },
                        containerColor = MaterialTheme.extendedColors.surfaceSheet,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.settings_title),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                playerViewModel.showSettings(true)
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    stringResource(R.string.action_about),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                moreMenuExpanded = false
                                playerViewModel.showAbout(true)
                            },
                        )
                    }
                }
            }

            if (showSearchBar) {
                NativeSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = stringResource(R.string.search_genres_placeholder),
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.genres_count, sortedGenres.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            if (sortedGenres.isNotEmpty()) {
                                val genre = sortedGenres.random()
                                val genreSongs = songs.filter { song ->
                                    val g = song.genre.ifBlank { "Desconocido" }
                                    g.equals(genre.name, ignoreCase = true)
                                }
                                if (genreSongs.isNotEmpty()) {
                                    playbackViewModel.updateDisplayOrder(genreSongs)
                                    playbackViewModel.play(genreSongs.first())
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.Shuffle,
                            contentDescription = stringResource(R.string.action_shuffle),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = {
                            if (sortedGenres.isNotEmpty()) {
                                val genre = sortedGenres.first()
                                val genreSongs = songs.filter { song ->
                                    val g = song.genre.ifBlank { "Desconocido" }
                                    g.equals(genre.name, ignoreCase = true)
                                }
                                if (genreSongs.isNotEmpty()) {
                                    playbackViewModel.updateDisplayOrder(genreSongs)
                                    playbackViewModel.play(genreSongs.first())
                                }
                            }
                        },
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.action_play_all),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (viewAsGrid) {
                    LazyVerticalGrid(
                        modifier = Modifier.fillMaxSize(),
                        columns = GridCells.Adaptive(140.dp),
                        state = gridState,
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(sortedGenres) { genre ->
                            GenreGridItem(
                                genre = genre,
                                onClick = { onGenreClick(genre.name) },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
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
                        items(sortedGenres) { genre ->
                            GenreListItem(
                                genre = genre,
                                playbackViewModel = playbackViewModel,
                                onClick = { onGenreClick(genre.name) },
                            )
                        }
                    }
                }

                if (sortedGenres.isNotEmpty()) {
                    AlphabetScrollerContent(
                        items = sortedGenres,
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

private val genreColors =
    listOf(
        0xFFE57373.toInt(),
        0xFFF06292.toInt(),
        0xFFBA68C8.toInt(),
        0xFF9575CD.toInt(),
        0xFF7986CB.toInt(),
        0xFF64B5F6.toInt(),
        0xFF4FC3F7.toInt(),
        0xFF4DD0E1.toInt(),
        0xFF4DB6AC.toInt(),
        0xFF81C784.toInt(),
        0xFFAED581.toInt(),
        0xFFFFD54F.toInt(),
        0xFFFFB74D.toInt(),
        0xFFA1887F.toInt(),
        0xFF90A4AE.toInt(),
    )

private fun genreColor(index: Int): Color {
    val safeIndex = index and Int.MAX_VALUE
    return Color(genreColors[safeIndex % genreColors.size])
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun GenreGridItem(
    genre: Genre,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 150.dp)
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(genreColor(genre.name.hashCode())),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = genre.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.songs_count, genre.songCount),
                color = MaterialTheme.extendedColors.textSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun GenreListItem(
    genre: Genre,
    playbackViewModel: PlaybackViewModel,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(genreColor(genre.name.hashCode())),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = genre.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.songs_count, genre.songCount),
                    color = MaterialTheme.extendedColors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.extendedColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private enum class GenreSortMode {
    TITLE_ASC,
    TITLE_DESC,
}
