@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.cvc953.localplayer.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.ui.theme.md_textSecondary
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun createCombinedAlbumArt(
    bitmaps: List<Bitmap?>,
    size: Int = 256,
): Bitmap {
    val canvas = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvasDrawer = Canvas(canvas)
    val count = minOf(4, bitmaps.size)
    if (count == 0) {
        return canvas
    }

    val halfSize = size / 2

    when (count) {
        1 -> {
            val bitmap = bitmaps[0] ?: return canvas
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
            canvasDrawer.drawBitmap(scaledBitmap, 0f, 0f, null)
        }

        2 -> {
            for (i in 0 until 2) {
                val bitmap = bitmaps[i] ?: continue
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, halfSize, size, true)
                val x = i * halfSize
                canvasDrawer.drawBitmap(scaledBitmap, x.toFloat(), 0f, null)
            }
        }

        3 -> {
            val leftBitmap = bitmaps[0]
            if (leftBitmap != null) {
                val scaledLeft = Bitmap.createScaledBitmap(leftBitmap, halfSize, size, true)
                canvasDrawer.drawBitmap(scaledLeft, 0f, 0f, null)
            }
            for (i in 1 until 3) {
                val bitmap = bitmaps[i] ?: continue
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, halfSize, halfSize, true)
                val x = halfSize
                val y = (i - 1) * halfSize
                canvasDrawer.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
            }
        }

        else -> {
            for (i in 0 until 4) {
                val bitmap = bitmaps[i] ?: continue
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, halfSize, halfSize, true)
                val x = (i % 2) * halfSize
                val y = (i / 2) * halfSize
                canvasDrawer.drawBitmap(scaledBitmap, x.toFloat(), y.toFloat(), null)
            }
        }
    }

    return canvas
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlaylistAlbumArt(
    playlistSongIds: List<Long>,
    songs: List<Song>,
    context: android.content.Context,
    modifier: Modifier = Modifier,
) {
    var combinedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(playlistSongIds) {
        if (playlistSongIds.isEmpty()) {
            combinedBitmap = null
            return@LaunchedEffect
        }

        val bitmaps = mutableListOf<Bitmap?>()
        val firstFourSongs =
            playlistSongIds.take(4).mapNotNull { songId -> songs.find { it.id == songId } }

        firstFourSongs.forEach { song ->
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, song.uri)
                retriever.embeddedPicture?.let {
                    val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                    bitmaps.add(bitmap)
                }
                retriever.release()
            } catch (_: Exception) {
                bitmaps.add(null)
            }
        }

        // Rellenar con nulls si hay menos de 4 canciones
        while (bitmaps.size < minOf(4, firstFourSongs.size)) {
            bitmaps.add(null)
        }

        if (bitmaps.isNotEmpty()) {
            withContext(Dispatchers.Default) { combinedBitmap = createCombinedAlbumArt(bitmaps) }
        }
    }

    if (combinedBitmap != null) {
        Image(
            painter = BitmapPainter(combinedBitmap!!.asImageBitmap()),
            contentDescription = "Carátula de la playlist",
            modifier = modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_default_album),
            contentDescription = "Carátula por defecto",
            modifier = modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                                                val array = org.json.JSONArray()
                                                val idsArray = org.json.JSONArray()
                                                p.songIds.forEach { idsArray.put(it) }
                                                val obj = org.json.JSONObject()
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
                                                org.json.JSONArray(text)
                                            } else {
                                                org.json.JSONArray().apply {
                                                    put(org.json.JSONObject(text))
                                                }
                                            }
                                        for (i in 0 until array.length()) {
                                            val obj = array.getJSONObject(i)
                                            val name = obj.optString("name", "").trim()
                                            if (name.isEmpty()) continue
                                            val idsArr =
                                                obj.optJSONArray("songIds")
                                                    ?: org.json.JSONArray()
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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            androidx.compose.material3.ButtonDefaults.buttonColors(
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
                                modifier = Modifier.clickable { onPlaylistClick(playlist.name) },
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier =
                                    Modifier.weight(1f).clickable {
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
                                            playlist.hashCode().toLong(),
                                    onDismissRequest = { menuExpandedPlaylistId = null },
                                    containerColor = MaterialTheme.extendedColors.surfaceSheet,
                                ) {
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
                containerColor = MaterialTheme.colorScheme.background,
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
                            Spacer(modifier = Modifier.height(8.dp))
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
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                ),
                        )
                        if (renameError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
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
                    ) { Text("Cancelar", color = Color.White) }
                },
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlaylistDetailScreen(
    playlistViewModel: PlaylistViewModel,
    playbackViewModel: PlaybackViewModel,
    playlistName: String,
    onBack: () -> Unit,
) {
    val songs by playlistViewModel.songs.collectAsState()
    val playerState by playbackViewModel.playerState.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()

    BackHandler { onBack() }

    val playlist =
        remember(playlists, playlistName) { playlists.find { it.name == playlistName } }

    val playlistSongs =
        remember(songs, playlist) {
            if (playlist != null) {
                songs.filter { it.id in playlist.songIds }
            } else {
                emptyList()
            }
        }

    val availableSongs =
        remember(songs, playlist) {
            if (playlist != null) {
                songs.filter { it.id !in playlist.songIds }
            } else {
                emptyList()
            }
        }

    var showAddSongsDialog by remember { mutableStateOf(false) }
    var selectedSongsToAdd by remember { mutableStateOf(setOf<Long>()) }
    var selectedSongsToRemove by remember { mutableStateOf(setOf<Long>()) }
    var isRemoveMode by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlistName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = "${playlistSongs.size} canciones",
                    color = md_textSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        if (playlist != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isRemoveMode) {
                    val allSelected =
                        playlistSongs.isNotEmpty() &&
                            selectedSongsToRemove.size == playlistSongs.size
                    TextButton(
                        onClick = {
                            selectedSongsToRemove =
                                if (allSelected) {
                                    setOf()
                                } else {
                                    playlistSongs.map { it.id }.toSet()
                                }
                        },
                    ) {
                        Text(
                            if (allSelected) "Limpiar" else "Seleccionar todo",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedSongsToRemove.isNotEmpty()) {
                                playlistViewModel.removeSongsFromPlaylist(
                                    playlistName,
                                    selectedSongsToRemove.toList(),
                                )
                                selectedSongsToRemove = setOf()
                                isRemoveMode = false
                            }
                        },
                        enabled = selectedSongsToRemove.isNotEmpty(),
                    ) { Text("Eliminar (${selectedSongsToRemove.size})") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            isRemoveMode = false
                            selectedSongsToRemove = setOf()
                        },
                    ) { Text("Cancelar", color = MaterialTheme.colorScheme.primary) }
                } else {
                    Button(
                        onClick = { showAddSongsDialog = true },
                        colors =
                            androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) { Text("Agregar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isRemoveMode = true
                            selectedSongsToRemove = setOf()
                        },
                        colors =
                            androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                            ),
                    ) { Text("Eliminar") }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(start = 16.dp, top = 8.dp, bottom = 16.dp, end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(playlistSongs) { song ->
                val isCurrent = playerState.currentSong?.id == song.id
                val isSelected = song.id in selectedSongsToRemove

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) {
                                    LocalExtendedColors.current.textSecondaryStrong
                                } else {
                                    Color.Transparent
                                },
                                shape = RoundedCornerShape(4.dp),
                            ).then(
                                if (isRemoveMode) {
                                    Modifier.clickable {
                                        selectedSongsToRemove =
                                            if (isSelected) {
                                                selectedSongsToRemove - song.id
                                            } else {
                                                selectedSongsToRemove + song.id
                                            }
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (isRemoveMode) {
                            androidx.compose.material3.Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedSongsToRemove =
                                        if (checked) {
                                            selectedSongsToRemove + song.id
                                        } else {
                                            selectedSongsToRemove - song.id
                                        }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                colors =
                                    androidx.compose.material3.CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = md_textSecondary,
                                        checkmarkColor = MaterialTheme.colorScheme.onBackground,
                                    ),
                            )
                        }

                        SongItem(
                            song = song,
                            isPlaying = isCurrent,
                            onClick = {
                                if (!isRemoveMode) {
                                    // Usar el orden de la playlist como cola de reproduccion
                                    playbackViewModel.updateDisplayOrder(playlistSongs)
                                    playbackViewModel.play(song)
                                }
                            },
                            onQueueNext = { playbackViewModel.addToQueueNext(song) },
                            onQueueEnd = { playbackViewModel.addToQueueEnd(song) },
                            playlists = playlists,
                            onAddToPlaylist = { targetPlaylistName, songId ->
                                playlistViewModel.addSongToPlaylist(targetPlaylistName, songId)
                            },
                            onRemoveFromPlaylist = {
                                if (!isRemoveMode) {
                                    playlistViewModel.removeSongFromPlaylist(
                                        playlistName,
                                        song.id,
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showAddSongsDialog) {
        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            title = {
                Text(
                    "Agregar canciones",
                    color = MaterialTheme.colorScheme.onBackground,
                )
            },
            text = {
                if (availableSongs.isEmpty()) {
                    Text("No hay canciones disponibles", color = md_textSecondary)
                } else {
                    Column {
                        val allSelected =
                            availableSongs.isNotEmpty() &&
                                selectedSongsToAdd.size == availableSongs.size
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(
                                onClick = {
                                    selectedSongsToAdd =
                                        if (allSelected) {
                                            setOf()
                                        } else {
                                            availableSongs.map { it.id }.toSet()
                                        }
                                },
                            ) {
                                Text(
                                    if (allSelected) "Limpiar" else "Seleccionar todo",
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        LazyColumn {
                            items(availableSongs) { song ->
                                val isSelectedToAdd = song.id in selectedSongsToAdd
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isSelectedToAdd) {
                                                    Color.Transparent
                                                } else {
                                                    Color.Transparent
                                                },
                                            ).clickable {
                                                selectedSongsToAdd =
                                                    if (isSelectedToAdd) {
                                                        selectedSongsToAdd -
                                                            song.id
                                                    } else {
                                                        selectedSongsToAdd +
                                                            song.id
                                                    }
                                            }.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    androidx.compose.material3.Checkbox(
                                        checked = isSelectedToAdd,
                                        onCheckedChange = { checked ->
                                            selectedSongsToAdd =
                                                if (checked) {
                                                    selectedSongsToAdd + song.id
                                                } else {
                                                    selectedSongsToAdd - song.id
                                                }
                                        },
                                        modifier = Modifier.padding(end = 8.dp),
                                        colors =
                                            androidx.compose.material3.CheckboxDefaults
                                                .colors(
                                                    checkedColor =
                                                        MaterialTheme.colorScheme.primary,
                                                    uncheckedColor =
                                                    md_textSecondary,
                                                    checkmarkColor = MaterialTheme.colorScheme.onBackground,
                                                ),
                                    )
                                    Text(
                                        text = song.title,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedSongsToAdd.isNotEmpty()) {
                            playlistViewModel.addSongsToPlaylist(
                                playlistName,
                                selectedSongsToAdd.toList(),
                            )
                            selectedSongsToAdd = setOf()
                            showAddSongsDialog = false
                        }
                    },
                    enabled = selectedSongsToAdd.isNotEmpty(),
                    colors =
                        androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                ) { Text("Agregar (${selectedSongsToAdd.size})") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSongsDialog = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.primary)
                }
            },
        )
    }
}

enum class PlaylistSortMode {
    TITLE_ASC,
    TITLE_DESC,
}
