package com.cvc953.localplayer.model

import android.content.Context
import android.database.Cursor
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
        var failedVolumes = 0
        for ((volumeName, volumeTargets) in byVolume) {
            val contentUri = contentUriFor(volumeName)
            val (selection, args) = buildSelection(volumeTargets)
            if (!queryInto(list, contentUri, selection, args)) failedVolumes++
        }

        // Si ningún volumen respondió, es un fallo de lectura y no una biblioteca vacía:
        // el llamador tiene que poder distinguirlos para no borrar la lista anterior.
        if (failedVolumes == byVolume.size) {
            throw IllegalStateException("No se pudo leer ningún volumen de MediaStore")
        }
        return list
    }

    private fun queryWithFallback(
        contentUri: Uri,
        columns: Array<String>,
        selection: String?,
        args: Array<String>?,
    ): Cursor? =
        try {
            context.contentResolver.query(contentUri, columns, selection, args, null)
        } catch (e: Exception) {
            Log.w("MediaStoreDataSource", "consulta con la proyección completa falló; reintentando con la reducida", e)
            try {
                context.contentResolver.query(contentUri, baseProjection, selection, args, null)
            } catch (fallbackError: Exception) {
                Log.e("MediaStoreDataSource", "la consulta reducida también falló para $contentUri", fallbackError)
                null
            }
        }

    /**
     * Vuelca las filas de un volumen en [out]. Devuelve `false` cuando no se pudo leer el
     * cursor, para que [querySongs] pueda distinguir un fallo de una lista vacía.
     */
    private fun queryInto(
        out: MutableList<Song>,
        contentUri: Uri,
        selection: String?,
        args: Array<String>?,
    ): Boolean {
        val cursor = queryWithFallback(contentUri, projection, selection, args) ?: return false
        cursor.use { readSongsFromCursor(it, contentUri, out) }
        return true
    }

    /**
     * Firma de la biblioteca para detectar cambios. Devuelve `null` cuando algún volumen no
     * se pudo consultar, para que el llamador conserve la lista anterior en vez de asumir
     * que la biblioteca quedó vacía.
     */
    override fun currentSignature(): LibrarySignature? {
        val byVolume = resolvedTargets().groupBy { it.volumeName }
        if (byVolume.isEmpty()) return LibrarySignature.EMPTY

        var songCount = 0
        var maxId = 0L
        var maxDateAdded = 0L
        var maxDateModified = 0L
        var hasRows = false
        var failed = false

        for ((volumeName, volumeTargets) in byVolume) {
            val contentUri = contentUriFor(volumeName)
            val (selection, args) = buildSelection(volumeTargets)
            val cursor =
                try {
                    context.contentResolver.query(
                        contentUri,
                        arrayOf(
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media.DATE_ADDED,
                            MediaStore.Audio.Media.DATE_MODIFIED,
                        ),
                        selection,
                        args,
                        MediaStore.Audio.Media._ID + " DESC",
                    )
                } catch (e: Exception) {
                    Log.w("MediaStoreDataSource", "currentSignature failed for volume $volumeName", e)
                    null
                }

            if (cursor == null) {
                failed = true
                continue
            }

            cursor.use { c ->
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
        }

        if (failed) return null

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

/**
 * Construye un [Song] a partir de una fila de MediaStore.
 *
 * Las columnas de texto pueden llegar en NULL según el proveedor del dispositivo.
 * `Cursor.getString` devuelve un tipo de plataforma, así que asignarlo a un campo no nulo de
 * [Song] lanzaba NPE y abortaba el escaneo completo, dejando la biblioteca vacía. Este es el
 * único punto donde entran datos del proveedor, así que acá se normalizan.
 */
internal fun songFromRow(
    id: Long,
    title: String?,
    artist: String?,
    album: String?,
    year: Int,
    uri: Uri,
    duration: Long,
    filePath: String?,
    trackNumber: Int,
    discNumber: Int,
    sampleRate: Int?,
    mimeType: String?,
    dateAdded: Long,
    dateModified: Long,
    genre: String?,
): Song =
    Song(
        id = id,
        title = title ?: "",
        artist = artist ?: "",
        album = album ?: "",
        year = year,
        uri = uri,
        duration = duration,
        filePath = filePath,
        trackNumber = trackNumber,
        discNumber = discNumber,
        sampleRate = sampleRate,
        mimeType = mimeType,
        dateAdded = dateAdded,
        dateModified = dateModified,
        genre = genre ?: "",
    )

/**
 * Vuelca las filas de [cursor] en [out].
 *
 * Aislado del [Context] y del [android.content.ContentResolver] a propósito: así se puede
 * ejercitar con un cursor falso en un test JVM y reproducir lo que devuelve un proveedor
 * real sin necesidad de un dispositivo.
 */
internal fun readSongsFromCursor(
    cursor: Cursor,
    contentUri: Uri,
    out: MutableList<Song>,
) {
    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
    val titleCol = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
    val artistCol = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
    val albumCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
    val yearCol = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
    val durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
    val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
    val trackNumberCol = cursor.getColumnIndex(MediaStore.Audio.Media.CD_TRACK_NUMBER)
    val discNumberCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISC_NUMBER)
    val mimeTypeCol = cursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
    val dateAddedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
    val genreCol = cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
    val sampleRateCol = cursor.getColumnIndex("sample_rate")
    val dateModifiedCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)

    while (cursor.moveToNext()) {
        val id = cursor.getLong(idCol)
        // Una fila con datos inválidos se descarta: perder una canción es preferible
        // a perder la biblioteca entera.
        try {
            out.add(
                songFromRow(
                    id = id,
                    title = cursor.stringOrNull(titleCol),
                    artist = cursor.stringOrNull(artistCol),
                    album = cursor.stringOrNull(albumCol),
                    year = cursor.intOrZero(yearCol),
                    // El URI debe apuntar al volumen donde vive la canción (interno o SD).
                    uri = Uri.withAppendedPath(contentUri, id.toString()),
                    duration = cursor.longOrZero(durCol),
                    filePath = cursor.stringOrNull(dataCol),
                    trackNumber = cursor.intOrZero(trackNumberCol),
                    discNumber = cursor.intOrZero(discNumberCol),
                    sampleRate = if (sampleRateCol >= 0) cursor.getInt(sampleRateCol).takeIf { value -> value > 0 } else null,
                    mimeType = cursor.stringOrNull(mimeTypeCol),
                    dateAdded = cursor.longOrZero(dateAddedCol),
                    dateModified = cursor.longOrZero(dateModifiedCol),
                    genre = cursor.stringOrNull(genreCol),
                ),
            )
        } catch (e: Exception) {
            Log.w("MediaStoreDataSource", "fila $id descartada por datos inválidos", e)
        }
    }
}

/** Lectura tolerante a columnas que el proveedor no expone (`-1`). */
private fun Cursor.stringOrNull(column: Int): String? = if (column >= 0) getString(column) else null

private fun Cursor.intOrZero(column: Int): Int = if (column >= 0) getInt(column) else 0

private fun Cursor.longOrZero(column: Int): Long = if (column >= 0) getLong(column) else 0L
