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

    override fun querySongs(): List<Song> {
        val list = mutableListOf<Song>()
        val selectionInfo = buildSelectionForFolder()

        val cursor =
            try {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selectionInfo.first,
                    selectionInfo.second,
                    null,
                )
            } catch (_: IllegalArgumentException) {
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    baseProjection,
                    selectionInfo.first,
                    selectionInfo.second,
                    null,
                )
            } ?: throw IllegalStateException("MediaStore query returned no cursor")

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
                val uri =
                    Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toString(),
                    )

                list.add(
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

        return list
    }

    override fun currentSignature(): LibrarySignature? {
        val selectionInfo = buildSelectionForFolder()
        return try {
            val cursor =
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DATE_ADDED,
                        MediaStore.Audio.Media.DATE_MODIFIED,
                    ),
                    selectionInfo.first,
                    selectionInfo.second,
                    null,
                )

            cursor?.use {
                val idCol = it.getColumnIndex(MediaStore.Audio.Media._ID)
                val dateAddedCol = it.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val dateModifiedCol = it.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
                var maxId = 0L
                var maxDateAdded = 0L
                var maxDateModified = 0L
                while (it.moveToNext()) {
                    if (idCol >= 0) maxId = maxOf(maxId, it.getLong(idCol))
                    if (dateAddedCol >= 0) maxDateAdded = maxOf(maxDateAdded, it.getLong(dateAddedCol))
                    if (dateModifiedCol >= 0) maxDateModified = maxOf(maxDateModified, it.getLong(dateModifiedCol))
                }
                LibrarySignature(it.count, maxId, maxDateAdded, maxDateModified)
            }
        } catch (e: Exception) {
            Log.w("MediaStoreDataSource", "currentSignature failed", e)
            null
        }
    }

    override fun countSongsForFolder(folderUriString: String): Int {
        val selectionInfo = buildSelectionForSingleFolder(folderUriString)
        return try {
            val cursor =
                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media._ID),
                    selectionInfo.first,
                    selectionInfo.second,
                    null,
                )
            val count = cursor?.count ?: 0
            cursor?.close()
            count
        } catch (e: Exception) {
            Log.w("MediaStoreDataSource", "countSongsForFolder failed", e)
            0
        }
    }

    private fun buildSelectionForFolder(): Pair<String?, Array<String>?> {
        val folders = folderUris()
        val base = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        if (folders.isEmpty()) return Pair(base, null)

        val useRelative = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        folders.forEach { folderUriString ->
            try {
                val uri = Uri.parse(folderUriString)
                val treeId = DocumentsContract.getTreeDocumentId(uri)
                if (treeId.startsWith("primary:")) {
                    val rel = treeId.removePrefix("primary:").trimStart('/')
                    if (rel.isNotEmpty()) {
                        val prefix = if (rel.endsWith("/")) rel else "$rel/"
                        if (useRelative) {
                            clauses.add(MediaStore.Audio.Media.RELATIVE_PATH + " LIKE ?")
                            args.add("$prefix%")
                        } else {
                            val basePath = Environment.getExternalStorageDirectory().absolutePath
                            clauses.add(MediaStore.Audio.Media.DATA + " LIKE ?")
                            args.add("$basePath/$rel%")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("MediaStoreDataSource", "buildSelectionForFolder: failed to parse tree id for $folderUriString", e)
            }
            // raw path fallback
            if (folderUriString.startsWith("/")) {
                clauses.add(MediaStore.Audio.Media.DATA + " LIKE ?")
                args.add("$folderUriString%")
            }
        }

        if (clauses.isEmpty()) {
            return Pair(base, null)
        }

        return Pair("(" + clauses.joinToString(" OR ") + ") AND " + base, args.toTypedArray())
    }

    private fun buildSelectionForSingleFolder(folderUriString: String): Pair<String?, Array<String>?> {
        val base = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        if (folderUriString.isEmpty()) return Pair(base, null)

        val useRelative = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        try {
            val uri = Uri.parse(folderUriString)
            val treeId = DocumentsContract.getTreeDocumentId(uri)
            if (treeId.startsWith("primary:")) {
                val rel = treeId.removePrefix("primary:").trimStart('/')
                if (rel.isEmpty()) return Pair(base, null)
                val prefix = if (rel.endsWith("/")) rel else "$rel/"
                return if (useRelative) {
                    Pair(MediaStore.Audio.Media.RELATIVE_PATH + " LIKE ? AND " + base, arrayOf("$prefix%"))
                } else {
                    val basePath = Environment.getExternalStorageDirectory().absolutePath
                    Pair(MediaStore.Audio.Media.DATA + " LIKE ? AND " + base, arrayOf("$basePath/$rel%"))
                }
            }
        } catch (e: Exception) {
            Log.w("MediaStoreDataSource", "buildSelectionForSingleFolder: failed for $folderUriString", e)
        }

        if (folderUriString.startsWith("/")) {
            return Pair(MediaStore.Audio.Media.DATA + " LIKE ? AND " + base, arrayOf("$folderUriString%"))
        }

        return Pair(base, null)
    }
}
