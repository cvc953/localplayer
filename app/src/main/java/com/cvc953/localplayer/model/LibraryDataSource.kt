package com.cvc953.localplayer.model

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log


interface LibraryDataSource {
    fun querySongs(): List<Song>

    fun currentSignature(): LibrarySignature?

    fun countSongsForFolder(folderUriString: String): Int
}

class MediaStoreDataSource(
    private val context: Context,
    private val folderUris: () -> List<String>,
) : LibraryDataSource {
    /**
     * Carpeta SAF resuelta a un volumen de MediaStore concreto.
     *
     * En Android 10+ cada dispositivo de almacenamiento (interno y tarjeta SD) es un
     * volumen de MediaStore independiente. `EXTERNAL_CONTENT_URI` solo apunta al volumen
     * primario, así que hay que consultar cada volumen por su nombre con
     * [MediaStore.Audio.Media.getContentUri].
     */
    private data class FolderTarget(
        /** Identificador SAF: "primary" o el UUID del volumen (tarjeta SD). */
        val volumeId: String,
        /** Nombre de volumen de MediaStore ("external_primary", UUID en minúsculas, o "external" en <Q). */
        val volumeName: String,
        /** Path relativo dentro del volumen, sin slashes. Vacío = raíz del volumen. */
        val relativePath: String,
    )

    private val baseProjection =
        arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.CD_TRACK_NUMBER,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.GENRE,
        )

    private val projection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            baseProjection + arrayOf("sample_rate")
        } else {
            baseProjection
        }

    // -------------------------
    // Resolución de carpetas → volumen
    // -------------------------

    private fun resolveFolderTarget(folderUriString: String): FolderTarget? =
        try {
            val uri = Uri.parse(folderUriString)
            val treeId = DocumentsContract.getTreeDocumentId(uri)
            val colon = treeId.indexOf(':')
            if (colon <= 0) {
                Log.w("MediaStoreDataSource", "resolveFolderTarget: tree id sin volumen para $folderUriString")
                null
            } else {
                val volumeId = treeId.substring(0, colon)
                val relativePath = treeId.substring(colon + 1).trim('/')
                val volumeName = resolveVolumeName(volumeId)
                if (volumeName == null) {
                    Log.w("MediaStoreDataSource", "resolveFolderTarget: volumen no encontrado para '$volumeId'")
                    null
                } else {
                    FolderTarget(volumeId, volumeName, relativePath)
                }
            }
        } catch (e: Exception) {
            Log.w("MediaStoreDataSource", "resolveFolderTarget failed for $folderUriString", e)
            null
        }

    private fun resolveVolumeName(volumeId: String): String? {
        val isPrimary = volumeId.equals("primary", ignoreCase = true)
        // Pre-Q no hay multi-volumen: todo el almacenamiento externo vive en un único volumen,
        // así que primario y SD se agrupan bajo el mismo volumen y se resuelven por DATA path.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return MediaStore.VOLUME_EXTERNAL_PRIMARY
        }
        if (isPrimary) return MediaStore.VOLUME_EXTERNAL_PRIMARY
        return try {
            val names = MediaStore.getExternalVolumeNames(context)
            val target = normalizeVolumeKey(volumeId)
            names.find { normalizeVolumeKey(it) == target }
        } catch (e: Exception) {
            Log.w("MediaStoreDataSource", "getExternalVolumeNames failed", e)
            null
        }
    }

    /**
     * Normaliza un identificador de volumen (UUID de tarjeta SD) para compararlo con los
     * nombres de volumen de MediaStore, tolerando mayúsculas/minúsculas y guiones.
     */
    private fun normalizeVolumeKey(value: String): String =
        value.lowercase().replace("-", "").trim()

    private fun contentUriFor(volumeName: String): Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && volumeName != MediaStore.VOLUME_EXTERNAL_PRIMARY) {
            // Volumen extraíble (tarjeta SD): hay que apuntar a su volumen concreto.
            MediaStore.Audio.Media.getContentUri(volumeName)
        } else {
            // Volumen primario (o pre-Q unificado): mantener el URI histórico para no
            // alterar los URIs de reproducción existentes de la memoria interna.
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

    /**
     * Construye la selection (WHERE) para un conjunto de carpetas ya agrupadas en un
     * mismo volumen.
     */
    private fun buildSelection(targets: List<FolderTarget>): Pair<String, Array<String>?> {
        val base = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            targets.forEach { t ->
                if (t.relativePath.isEmpty()) {
                    // Raíz del volumen: incluir todo el volumen.
                    clauses.add("1 = 1")
                } else {
                    clauses.add(MediaStore.Audio.Media.RELATIVE_PATH + " LIKE ?")
                    args.add("${t.relativePath}/%")
                }
            }
        } else {
            targets.forEach { t ->
                val mount =
                    if (t.volumeId.equals("primary", ignoreCase = true)) {
                        Environment.getExternalStorageDirectory().absolutePath
                    } else {
                        "/storage/${t.volumeId}"
                    }
                val prefix = if (t.relativePath.isEmpty()) mount else "$mount/${t.relativePath}"
                clauses.add(MediaStore.Audio.Media.DATA + " LIKE ?")
                args.add("$prefix%")
            }
        }

        return Pair("(${clauses.joinToString(" OR ")}) AND $base", args.toTypedArray())
    }

    private fun resolvedTargets(): List<FolderTarget> = folderUris().mapNotNull { resolveFolderTarget(it) }

    // -------------------------
    // Consultas
    // -------------------------

    override fun querySongs(): List<Song> {
        val byVolume = resolvedTargets().groupBy { it.volumeName }
        if (byVolume.isEmpty()) return emptyList()

        val list = mutableListOf<Song>()
        for ((volumeName, volumeTargets) in byVolume) {
            val contentUri = contentUriFor(volumeName)
            val (selection, args) = buildSelection(volumeTargets)
            queryInto(list, contentUri, selection, args)
        }
        return list
    }

    private fun queryInto(
        out: MutableList<Song>,
        contentUri: Uri,
        selection: String?,
        args: Array<String>?,
    ) {
        val cursor =
            try {
                context.contentResolver.query(contentUri, projection, selection, args, null)
            } catch (_: IllegalArgumentException) {
                context.contentResolver.query(contentUri, baseProjection, selection, args, null)
            } ?: return

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val yearCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val trackNumberCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.CD_TRACK_NUMBER)
            val discNumberCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISC_NUMBER)
            val mimeTypeCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val dateAddedCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val genreCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.GENRE)
            val sampleRateCol = it.getColumnIndex("sample_rate")
            val dateModifiedCol = it.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                // El URI debe apuntar al volumen donde vive la canción (interno o SD).
                val uri = Uri.withAppendedPath(contentUri, id.toString())

                out.add(
                    Song(
                        id = id,
                        title = it.getString(titleCol),
                        artist = it.getString(artistCol),
                        album = it.getString(albumCol),
                        year = it.getInt(yearCol),
                        uri = uri,
                        duration = it.getLong(durCol),
                        albumArt = null,
                        filePath = it.getString(dataCol),
                        trackNumber = it.getInt(trackNumberCol),
                        discNumber = it.getInt(discNumberCol),
                        sampleRate = if (sampleRateCol >= 0) it.getInt(sampleRateCol).takeIf { v -> v > 0 } else null,
                        mimeType = it.getString(mimeTypeCol),
                        dateAdded = it.getLong(dateAddedCol),
                        dateModified = if (dateModifiedCol >= 0) it.getLong(dateModifiedCol) else 0L,
                        genre = it.getString(genreCol) ?: "",
                    ),
                )
            }
        }
    }

    override fun currentSignature(): LibrarySignature? {
        val byVolume = resolvedTargets().groupBy { it.volumeName }
        if (byVolume.isEmpty()) return LibrarySignature.EMPTY

        var songCount = 0
        var maxId = 0L
        var maxDateAdded = 0L
        var maxDateModified = 0L
        var hasRows = false

        for ((volumeName, volumeTargets) in byVolume) {
            val contentUri = contentUriFor(volumeName)
            val (selection, args) = buildSelection(volumeTargets)
            try {
                context.contentResolver
                    .query(
                        contentUri,
                        arrayOf(
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.DATE_ADDED,
                            MediaStore.Audio.Media.DATE_MODIFIED,
                        ),
                        selection,
                        args,
                        MediaStore.Audio.Media._ID + " DESC",
                    )?.use { c ->
                        if (c.moveToFirst()) {
                            hasRows = true
                            songCount += c.count
                            val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
                            val dateAddedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                            val dateModifiedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                            if (idCol >= 0) maxId = maxOf(maxId, c.getLong(idCol))
                            if (dateAddedCol >= 0) maxDateAdded = maxOf(maxDateAdded, c.getLong(dateAddedCol))
                            if (dateModifiedCol >= 0) maxDateModified = maxOf(maxDateModified, c.getLong(dateModifiedCol))
                        }
                    }
            } catch (e: Exception) {
                Log.w("MediaStoreDataSource", "currentSignature failed for volume $volumeName", e)
            }
        }

        return if (!hasRows) {
            LibrarySignature.EMPTY
        } else {
            LibrarySignature(
                songCount = songCount,
                maxId = maxId,
                maxDateAdded = maxDateAdded,
                maxDateModified = maxDateModified,
            )
        }
    }

    override fun countSongsForFolder(folderUriString: String): Int {
        val target = resolveFolderTarget(folderUriString) ?: return 0
        val contentUri = contentUriFor(target.volumeName)
        val (selection, args) = buildSelection(listOf(target))
        return try {
            val cursor = context.contentResolver.query(contentUri, arrayOf(MediaStore.Audio.Media._ID), selection, args, null)
            val count = cursor?.use { it.count } ?: 0
            count
        } catch (e: Exception) {
            Log.w("MediaStoreDataSource", "countSongsForFolder failed", e)
            0
        }
    }
}
