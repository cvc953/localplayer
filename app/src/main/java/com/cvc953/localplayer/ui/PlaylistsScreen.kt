package com.cvc953.localplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.cvc953.localplayer.viewmodel.MainViewModel

@Composable
fun PlaylistsScreen(viewModel: MainViewModel) {
    val isScanning by viewModel.isScanning
    val playlists by viewModel.playlists.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf(PlaylistSortMode.TITLE_ASC) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var createError by rememberSaveable { mutableStateOf<String?>(null) }

    val filteredPlaylists =
            remember(playlists, searchQuery) {
                val q = searchQuery.trim().lowercase()
                if (q.isEmpty()) playlists
                else playlists.filter { it.name.lowercase().contains(q) }
            }

    val sortedPlaylists =
            remember(filteredPlaylists, sortMode) {
                when (sortMode) {
                    PlaylistSortMode.TITLE_ASC ->
                            filteredPlaylists.sortedBy { it.name.lowercase() }
                    PlaylistSortMode.TITLE_DESC ->
                            filteredPlaylists.sortedByDescending { it.name.lowercase() }
                }
            }

    if (isScanning) {
        Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Escaneando canciones", color = Color.White)
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                        text = "Listas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                )

                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color.White)
                    }
                    DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            containerColor = Color(0xFF1A1A1A)
                    ) {
                        DropdownMenuItem(
                                text = { Text("Título A-Z", color = Color.White) },
                                onClick = {
                                    sortMode = PlaylistSortMode.TITLE_ASC
                                    sortMenuExpanded = false
                                }
                        )
                        DropdownMenuItem(
                                text = { Text("Título Z-A", color = Color.White) },
                                onClick = {
                                    sortMode = PlaylistSortMode.TITLE_DESC
                                    sortMenuExpanded = false
                                }
                        )
                    }
                }

                IconButton(
                        onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) searchQuery = ""
                        }
                ) { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = Color.White) }
            }

            if (showSearchBar) {
                OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("Buscar por lista", color = Color(0xFF808080)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors =
                                TextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF1A1A1A),
                                        unfocusedContainerColor = Color(0xFF1A1A1A),
                                        focusedIndicatorColor = Color(0xFF2196F3),
                                        unfocusedIndicatorColor = Color(0xFF404040),
                                        cursorColor = Color(0xFF2196F3),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFF2196F3),
                                        unfocusedLabelColor = Color(0xFF808080)
                                )
                )
            }

            if (sortedPlaylists.isEmpty()) {
                Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "No hay listas por ahora", color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showCreateDialog = true }) { Text("Crear lista") }
                }
            } else {
                LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                                PaddingValues(
                                        start = 16.dp,
                                        top = 16.dp,
                                        bottom = 16.dp,
                                        end = 4.dp
                                ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedPlaylists) { playlist ->
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = playlist.name,
                                        color = Color.White,
                                        fontSize = 16.sp
                                )
                                Text(
                                        text = "${playlist.songIds.size} canciones",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
                onDismissRequest = {
                    showCreateDialog = false
                    newPlaylistName = ""
                    createError = null
                },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Nueva lista", color = Color.White) },
                text = {
                    Column {
                        OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                singleLine = true,
                                placeholder = { Text("Nombre de la lista") },
                                colors =
                                        TextFieldDefaults.colors(
                                                focusedContainerColor = Color(0xFF1A1A1A),
                                                unfocusedContainerColor = Color(0xFF1A1A1A),
                                                focusedIndicatorColor = Color(0xFF2196F3),
                                                unfocusedIndicatorColor = Color(0xFF404040),
                                                cursorColor = Color(0xFF2196F3),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                        )
                        )
                        if (createError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = createError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                val ok = viewModel.createPlaylist(newPlaylistName)
                                if (ok) {
                                    showCreateDialog = false
                                    newPlaylistName = ""
                                    createError = null
                                } else {
                                    createError = "Nombre invalido o duplicado"
                                }
                            }
                    ) {
                        Text("Crear", color = Color(0xFF2196F3))
                    }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                showCreateDialog = false
                                newPlaylistName = ""
                                createError = null
                            }
                    ) {
                        Text("Cancelar", color = Color.White)
                    }
                }
        )
    }
}

private enum class PlaylistSortMode {
    TITLE_ASC,
    TITLE_DESC
}
