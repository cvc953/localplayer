package com.cvc953.localplayer.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.preferences.AppPrefs
import com.cvc953.localplayer.ui.components.PlaylistAlbumArt
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.ui.theme.md_textSecondary
import com.cvc953.localplayer.ui.theme.predefinedThemeColors
import com.cvc953.localplayer.viewmodel.EqualizerViewModel
import com.cvc953.localplayer.viewmodel.FolderEntry
import com.cvc953.localplayer.viewmodel.FolderViewModel
import com.cvc953.localplayer.viewmodel.MainViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlaylistsScreen(
    playlistViewModel: PlaylistViewModel,
    playbackViewModel: PlaybackViewModel,
    onPlaylistClick: (String) -> Unit,
) {
    val isScanning by playlistViewModel.isScanning.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()
    val songs by playlistViewModel.songs.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchBar by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }
    var sortModeName by rememberSaveable { mutableStateOf(PlaylistSortMode.TITLE_ASC.name) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var newPlaylistName by rememberSaveable { mutableStateOf("") }
    var createError by rememberSaveable { mutableStateOf<String?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var renamePlaylistName by rememberSaveable { mutableStateOf("") }
    var renameError by rememberSaveable { mutableStateOf<String?>(null) }
    var menuExpandedPlaylistId by remember { mutableStateOf<Long?>(null) }
    var playlistToExport by remember { mutableStateOf<Playlist?>(null) }
    val context = LocalContext.current
    val activity = context as? Activity
    var lastBackPressTime by remember { mutableStateOf(0L) }

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 1500) {
            activity?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Presiona de nuevo para salir", Toast.LENGTH_SHORT).show()
        }
    }

    val filteredPlaylists =
        remember(playlists, searchQuery) {
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) playlists else playlists.filter { it.name.lowercase().contains(q) }
        }

    val sortedPlaylists =
        remember(filteredPlaylists, sortModeName) {
            when (PlaylistSortMode.valueOf(sortModeName)) {
                PlaylistSortMode.TITLE_ASC -> {
                    filteredPlaylists.sortedBy { it.name.lowercase() }
                }

                PlaylistSortMode.TITLE_DESC -> {
                    filteredPlaylists.sortedByDescending { it.name.lowercase() }
                }
            }
        }

    if (isScanning) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Escaneando canciones", color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.Companion.CenterVertically,
            ) {
                Text(
                    text = "Listas",
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
                            contentDescription = "Ordenar",
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
                                    "Título A-Z",
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                sortModeName = PlaylistSortMode.TITLE_ASC.name
                                sortMenuExpanded = false
                            },
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Título Z-A",
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            onClick = {
                                sortModeName = PlaylistSortMode.TITLE_DESC.name
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
                        contentDescription = "Buscar",
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
                            "Buscar por lista",
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

            val scope2 = rememberCoroutineScope()
            val exportLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                    if (uri != null) {
                        scope2.launch {
                            try {
                                val resolver = context.contentResolver

                                // Try to persist permissions so write works reliably
                                try {
                                    resolver.takePersistableUriPermission(
                                        uri,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                                    )
                                } catch (sec: Exception) {
                                    Log.w(
                                        "PlaylistsScreen",
                                        "No persistable permission: ${sec.message}",
                                    )
                                }

                                val filename =
                                    "localplayer_playlists_${System.currentTimeMillis()}.json"

                                // Some providers reject the raw tree URI for createDocument. Try the tree URI first,
                                // then fall back to using the tree's document URI.
                                var docUri =
                                    try {
                                        DocumentsContract.createDocument(
                                            resolver,
                                            uri,
                                            "application/json",
                                            filename,
                                        )
                                    } catch (iae: IllegalArgumentException) {
                                        try {
                                            val treeId = DocumentsContract.getTreeDocumentId(uri)
                                            val parent =
                                                DocumentsContract.buildDocumentUriUsingTree(
                                                    uri,
                                                    treeId,
                                                )
                                            DocumentsContract.createDocument(
                                                resolver,
                                                parent,
                                                "application/json",
                                                filename,
                                            )
                                        } catch (e2: Exception) {
                                            Log.e(
                                                "PlaylistsScreen",
                                                "createDocument fallback failed",
                                                e2,
                                            )
                                            null
                                        }
                                    }

                                if (docUri == null) {
                                    Toast
                                        .makeText(
                                            context,
                                            "No se pudo crear archivo en la carpeta",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    return@launch
                                }

                                var exportError: String? = null
                                var exportSuccess = false
                                try {
                                    resolver.openOutputStream(docUri)?.use { os ->
                                        val text =
                                            playlistToExport?.let { p ->
                                                // serialize single playlist
                                                val array = JSONArray()
                                                val idsArray = JSONArray()
                                                p.songIds.forEach { idsArray.put(it) }
                                                val obj = JSONObject()
                                                obj.put("name", p.name)
                                                obj.put("songIds", idsArray)
                                                array.put(obj)
                                                array.toString()
                                            } ?: playlistViewModel.getPlaylistsJson()

                                        os.write(text.toByteArray())
                                        os.flush()
                                        exportSuccess = true
                                    }
                                } catch (e: Exception) {
                                    Log.e("PlaylistsScreen", "Error exporting playlists", e)
                                    exportError = e.message
                                }
                                if (exportSuccess) {
                                    Toast
                                        .makeText(
                                            context,
                                            "Playlists exportadas",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    playlistToExport = null
                                } else if (exportError != null) {
                                    Toast
                                        .makeText(
                                            context,
                                            "Error exportando playlists: $exportError",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                }
                            } catch (e: Exception) {
                            }
                        }
                    }
                }
            val treeLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                    if (uri != null) {
                        scope2.launch {
                            val resolver = context.contentResolver
                            val childrenUri =
                                DocumentsContract.buildChildDocumentsUriUsingTree(
                                    uri,
                                    DocumentsContract.getTreeDocumentId(uri),
                                )
                            val projection =
                                arrayOf(
                                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                )
                            val texts = mutableListOf<String>()
                            resolver
                                .query(childrenUri, projection, null, null, null)
                                ?.use { cursor ->
                                    while (cursor.moveToNext()) {
                                        val docId = cursor.getString(0)
                                        val name = cursor.getString(1) ?: ""
                                        if (name.endsWith(".json", true)) {
                                            try {
                                                val docUri =
                                                    DocumentsContract.buildDocumentUriUsingTree(
                                                        uri,
                                                        docId,
                                                    )
                                                resolver
                                                    .openInputStream(docUri)
                                                    ?.bufferedReader()
                                                    ?.use { r ->
                                                        texts.add(r.readText())
                                                    }
                                            } catch (_: Exception) {
                                            }
                                        }
                                    }
                                }

                            if (texts.isNotEmpty()) {
                                var imported = 0
                                texts.forEach { text ->
                                    try {
                                        val array =
                                            if (text.trimStart().startsWith("[")) {
                                                JSONArray(text)
                                            } else {
                                                JSONArray().apply {
                                                    put(JSONObject(text))
                                                }
                                            }
                                        for (i in 0 until array.length()) {
                                            val obj = array.getJSONObject(i)
                                            val name = obj.optString("name", "").trim()
                                            if (name.isEmpty()) continue
                                            val idsArr =
                                                obj.optJSONArray("songIds")
                                                    ?: JSONArray()
                                            val ids = mutableListOf<Long>()
                                            for (j in 0 until idsArr.length()) {
                                                ids.add(idsArr.optLong(j))
                                            }

                                            if (playlistViewModel.playlists.value.any {
                                                    it.name.equals(name, ignoreCase = true)
                                                }
                                            ) {
                                                continue
                                            }

                                            val created = playlistViewModel.createPlaylist(name)
                                            if (created && ids.isNotEmpty()) {
                                                playlistViewModel.addSongsToPlaylist(name, ids)
                                            }
                                            imported++
                                        }
                                    } catch (_: Exception) {
                                    }
                                }
                                Toast
                                    .makeText(
                                        context,
                                        "Importadas $imported playlists",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            } else {
                                Toast
                                    .makeText(
                                        context,
                                        "No se encontraron archivos .json en la carpeta",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                            }
                        }
                    }
                }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.Companion.CenterVertically,
            ) {
                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                    IconButton(onClick = { treeLauncher.launch(null) }) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Importar",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        text = "importar",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                    )
                }

                Spacer(Modifier.width(24.dp))

                Column(horizontalAlignment = Alignment.Companion.CenterHorizontally) {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Crear lista",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        text = "crear",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 12.sp,
                    )
                }
            }

            if (sortedPlaylists.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "No hay listas por ahora",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) { Text("Crear lista") }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            top = 16.dp,
                            bottom = 16.dp,
                            end = 4.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sortedPlaylists) { playlist ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlaylistAlbumArt(
                                playlistSongIds = playlist.songIds,
                                songs = songs,
                                context = LocalContext.current,
                                modifier =
                                    Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onPlaylistClick(playlist.name) },
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier =
                                    Modifier.Companion.weight(1f).clickable {
                                        onPlaylistClick(playlist.name)
                                    },
                            ) {
                                Text(
                                    text = playlist.name,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                )
                                Text(
                                    text = "${playlist.songIds.size} canciones",
                                    color = md_textSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = {
                                        menuExpandedPlaylistId =
                                            if (menuExpandedPlaylistId ==
                                                playlist.hashCode().toLong()
                                            ) {
                                                null
                                            } else {
                                                playlist.hashCode().toLong()
                                            }
                                    },
                                ) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "Opciones",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                    )
                                }
                                DropdownMenu(
                                    expanded =
                                        menuExpandedPlaylistId ==
                                            playlist
                                                .hashCode()
                                                .toLong(),
                                    onDismissRequest = { menuExpandedPlaylistId = null },
                                    containerColor = MaterialTheme.extendedColors.surfaceSheet,
                                ) {
                                    val appPrefs =
                                        remember {
                                            AppPrefs(context)
                                        }
                                    val order = appPrefs.getPlaylistOrder(playlist.name)
                                    val playlistSongs =
                                        remember(songs, playlist, order) {
                                            val base =
                                                playlist.songIds.mapNotNull { id -> songs.find { it.id == id } }
                                            when (order) {
                                                "AZ" -> base.sortedBy { it.title.lowercase() }
                                                "ZA" -> base.sortedByDescending { it.title.lowercase() }
                                                else -> base
                                            }
                                        }

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Reproducir ahora",
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = {
                                            menuExpandedPlaylistId = null
                                            if (playlistSongs.isNotEmpty()) {
                                                playbackViewModel.updateDisplayOrder(playlistSongs)
                                                playbackViewModel.play(playlistSongs.first())
                                            }
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Añadir como siguiente",
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = {
                                            menuExpandedPlaylistId = null
                                        
                                            // NO filter duplicates when adding full playlist
                                            val toAdd = playlistSongs
                                            playbackViewModel.addToQueueNextAll(toAdd)
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Añadido ${toAdd.size} canciones como siguiente",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Añadir al final",
                                                color = MaterialTheme.colorScheme.onSurface,
                                            )
                                        },
                                        onClick = {
                                            menuExpandedPlaylistId = null
                                        
                                            // NO filter duplicates when adding full playlist
                                            val toAdd = playlistSongs
                                            playbackViewModel.addToQueueEndAll(toAdd)
                                            Toast
                                                .makeText(
                                                    context,
                                                    "Añadido ${toAdd.size} canciones al final de la cola",
                                                    Toast.LENGTH_SHORT,
                                                ).show()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Exportar",
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        },
                                        onClick = {
                                            playlistToExport = playlist
                                            exportLauncher.launch(null)
                                            menuExpandedPlaylistId = null
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Renombrar",
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                        },
                                        onClick = {
                                            playlistToRename = playlist
                                            renamePlaylistName = playlist.name
                                            showRenameDialog = true
                                            menuExpandedPlaylistId = null
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Eliminar",
                                                color = Color(0xFFFF6B6B),
                                            )
                                        },
                                        onClick = {
                                            playlistToDelete = playlist
                                            menuExpandedPlaylistId = null
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (playlistToDelete != null) {
            val target = playlistToDelete
            AlertDialog(
                onDismissRequest = { playlistToDelete = null },
                containerColor = MaterialTheme.colorScheme.background,
                title = { Text("Eliminar lista", color = MaterialTheme.colorScheme.onBackground) },
                text = {
                    Text(
                        text = "Se eliminara la lista \"${target?.name}\".",
                        color = md_textSecondary,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            target?.name?.let { name -> playlistViewModel.deletePlaylist(name) }
                            playlistToDelete = null
                        },
                    ) { Text("Eliminar", color = Color(0xFFFF6B6B)) }
                },
                dismissButton = {
                    TextButton(onClick = { playlistToDelete = null }) {
                        Text("Cancelar", color = MaterialTheme.colorScheme.onBackground)
                    }
                },
            )
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreateDialog = false
                    newPlaylistName = ""
                    createError = null
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Nueva lista", color = MaterialTheme.colorScheme.onBackground) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            singleLine = true,
                            placeholder = { Text("Nombre de la lista") },
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                                ),
                        )
                        if (createError != null) {
                            Spacer(modifier = Modifier.Companion.height(8.dp))
                            Text(text = createError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val ok = playlistViewModel.createPlaylist(newPlaylistName)
                            if (ok) {
                                showCreateDialog = false
                                newPlaylistName = ""
                                createError = null
                            } else {
                                createError = "Nombre invalido o duplicado"
                            }
                        },
                    ) { Text("Crear", color = MaterialTheme.colorScheme.primary) }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCreateDialog = false
                            newPlaylistName = ""
                            createError = null
                        },
                    ) { Text("Cancelar", color = MaterialTheme.colorScheme.onBackground) }
                },
            )
        }

        if (showRenameDialog && playlistToRename != null) {
            AlertDialog(
                onDismissRequest = {
                    showRenameDialog = false
                    renamePlaylistName = ""
                    renameError = null
                },
                containerColor = MaterialTheme.colorScheme.background,
                title = { Text("Renombrar lista", color = MaterialTheme.colorScheme.onBackground) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = renamePlaylistName,
                            onValueChange = { renamePlaylistName = it },
                            singleLine = true,
                            placeholder = { Text("Nombre de la lista") },
                            colors =
                                TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                    cursorColor = MaterialTheme.colorScheme.primary,
                                    focusedTextColor = Color.Companion.White,
                                    unfocusedTextColor = Color.Companion.White,
                                ),
                        )
                        if (renameError != null) {
                            Spacer(modifier = Modifier.Companion.height(8.dp))
                            Text(text = renameError!!, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val ok =
                                playlistViewModel.renamePlaylist(
                                    playlistToRename!!.name,
                                    renamePlaylistName,
                                )
                            if (ok) {
                                showRenameDialog = false
                                renamePlaylistName = ""
                                renameError = null
                                playlistToRename = null
                            } else {
                                renameError = "Nombre inválido o duplicado"
                            }
                        },
                    ) { Text("Renombrar", color = MaterialTheme.colorScheme.primary) }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showRenameDialog = false
                            renamePlaylistName = ""
                            renameError = null
                            playlistToRename = null
                        },
                    ) { Text("Cancelar", color = Color.Companion.White) }
                },
            )
        }
    }
}

enum class PlaylistSortMode {
    TITLE_ASC,
    TITLE_DESC,
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    equalizerViewModel: EqualizerViewModel,
    folderViewModel: FolderViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val folderEntries by folderViewModel.folderEntries.collectAsState()
    val theme by viewModel.themeMode.collectAsState()
    val autoScan by viewModel.autoScanEnabled.collectAsState()
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsState()
    val primaryColorHex by viewModel.primaryColorHex.collectAsState()
    val eqEnabled by equalizerViewModel.equalizerEnabled.collectAsState()

    val themeOptions = listOf("sistema", "claro", "oscuro")
    var themeExpanded by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderEntry?>(null) }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                } catch (_: Exception) {
                }
                folderViewModel.addMusicFolder(uri.toString())
                Toast.makeText(context, "Carpeta añadida", Toast.LENGTH_SHORT).show()
            }
        }

    BackHandler(onBack = onClose)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ajustes",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Personaliza la app y tu biblioteca",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = "Apariencia y comportamiento",
                    subtitle = "Opciones globales de visualización y escaneo",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tema", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Selecciona tema de la aplicación",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Box {
                            OutlinedButton(onClick = { themeExpanded = true }) {
                                Text(theme.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                            }
                            DropdownMenu(
                                expanded = themeExpanded,
                                onDismissRequest = { themeExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                themeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option.replaceFirstChar {
                                                    if (it.isLowerCase()) it.titlecase() else it.toString()
                                                },
                                            )
                                        },
                                        onClick = {
                                            viewModel.setThemeMode(option)
                                            themeExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Color de acento", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Selecciona el color de acento de la app",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(predefinedThemeColors) { themeColor ->
                            val isSelected = themeColor.hex == primaryColorHex
                            val borderColor =
                                if (isSelected) themeColor.color else Color.Transparent
                            val borderWidth = if (isSelected) 3.dp else 0.dp

                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (borderWidth > 0.dp) {
                                                Modifier.Companion.border(
                                                    borderWidth,
                                                    borderColor,
                                                    CircleShape,
                                                )
                                            } else {
                                                Modifier
                                            },
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    color = themeColor.color,
                                    onClick = { viewModel.setPrimaryColor(themeColor.hex) },
                                ) {
                                    if (isSelected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = themeColor.name,
                                                tint = themeColor.onColor,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Color dinámico", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Usar color de la carátula en el reproductor",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { viewModel.toggleDynamicColor(it) },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.16f,
                                        ),
                                ),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                }
            }

            item {
                SettingsSectionCard(
                    title = "Audio",
                    subtitle = "Control del ecualizador y procesamiento de sonido",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Activar ecualizador", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Usar ecualizador nativo del sistema",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = { equalizerViewModel.toggleEqualizer(it) },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.16f,
                                        ),
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { equalizerViewModel.openEqualizerScreen() },
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir ecualizador avanzado")
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = "Biblioteca",
                    subtitle = "Gestiona las carpetas que se incluyen en la música",
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Escaneo automático", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Detectar cambios y escanear automáticamente",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = autoScan,
                            onCheckedChange = { viewModel.toggleAutoScan(it) },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.16f,
                                        ),
                                ),
                        )
                    }

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { launcher.launch(null) },
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar carpeta")
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Carpetas configuradas",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (folderEntries.isEmpty()) {
                        Text(
                            "Ninguna carpeta configurada",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        folderEntries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "${entry.count} canciones",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                    )
                                }
                                IconButton(onClick = { folderToDelete = entry }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        folderToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { folderToDelete = null },
                title = { Text("Eliminar carpeta") },
                text = {
                    Text(
                        "¿Eliminar la carpeta \"${entry.name}\"?\n\nLas ${entry.count} canciones de esta carpeta ya no se mostrarán en la app.",
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            folderViewModel.removeMusicFolder(entry.uri)
                            Toast.makeText(context, "Carpeta eliminada", Toast.LENGTH_SHORT).show()
                            folderToDelete = null
                        },
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToDelete = null }) {
                        Text("Cancelar")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = LocalExtendedColors.current.surfaceSheet),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(subtitle, color = LocalExtendedColors.current.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
