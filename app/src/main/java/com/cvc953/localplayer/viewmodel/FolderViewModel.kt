package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.preferences.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FolderEntry(
    val uri: String,
    val name: String,
    val count: Int,
)

class FolderViewModel(application: Application) : AndroidViewModel(application) {
    private val appPrefs = AppPrefs(application)
    private val repository = SongRepository(application)

    private val _folderUris = MutableStateFlow(appPrefs.getMusicFolderUris())
    val folderUris: StateFlow<List<String>> = _folderUris

    private val _folderEntries = MutableStateFlow<List<FolderEntry>>(emptyList())
    val folderEntries: StateFlow<List<FolderEntry>> = _folderEntries

    init {
        refreshFolderEntries()
    }

    private fun refreshFolderEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<FolderEntry>()
            val uris = appPrefs.getMusicFolderUris()
            uris.forEach { u ->
                var name = u
                try {
                    val treeId = DocumentsContract.getTreeDocumentId(Uri.parse(u))
                    // treeId often like "primary:Music/monochrome" -> show last path segment
                    val rel = treeId.removePrefix("primary:").trimStart('/')
                    if (rel.isNotEmpty()) {
                        val last = rel.substringAfterLast('/')
                        if (last.isNotEmpty()) name = last
                    }
                } catch (_: Exception) {
                    // fallback to last path segment of URI
                    try {
                        val p = Uri.parse(u).lastPathSegment
                        if (!p.isNullOrEmpty()) name = p
                    } catch (_: Exception) {
                    }
                }
                val count =
                    try {
                        repository.countSongsForFolder(u)
                    } catch (_: Exception) {
                        0
                    }
                list.add(FolderEntry(uri = u, name = name, count = count))
            }
            _folderEntries.value = list
        }
    }

    fun addMusicFolder(uri: String) {
        appPrefs.addMusicFolder(uri)
        _folderUris.value = appPrefs.getMusicFolderUris()
        refreshFolderEntries()
    }

    fun removeMusicFolder(uri: String) {
        appPrefs.removeMusicFolder(uri)
        _folderUris.value = appPrefs.getMusicFolderUris()
        refreshFolderEntries()
    }
}
