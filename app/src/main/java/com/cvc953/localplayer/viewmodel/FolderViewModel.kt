package com.cvc953.localplayer.viewmodel

import android.app.Application
import android.net.Uri
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

    private val _folderEntries = MutableStateFlow<List<FolderEntry>>(emptyList())
    val folderEntries: StateFlow<List<FolderEntry>> = _folderEntries

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _autoScanEnabled = MutableStateFlow(appPrefs.isAutoScanEnabled())
    val autoScanEnabled: StateFlow<Boolean> = _autoScanEnabled

    init {
        refreshFolderEntries()
    }

    fun refreshFolderEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = mutableListOf<FolderEntry>()
            val uris = appPrefs.getMusicFolderUris()
            uris.forEach { u ->
                var name = u
                try {
                    val treeId = android.provider.DocumentsContract.getTreeDocumentId(Uri.parse(u))
                    val rel = treeId.removePrefix("primary:").trimStart('/')
                    if (rel.isNotEmpty()) {
                        val last = rel.substringAfterLast('/')
                        if (last.isNotEmpty()) name = last
                    }
                } catch (_: Exception) {
                    try {
                        val p = Uri.parse(u).lastPathSegment
                        if (!p.isNullOrEmpty()) name = p
                    } catch (_: Exception) {
                    }
                }
                val count = try {
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
        refreshFolderEntries()
        // trigger a rescan in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.forceRescanSongs()
            } catch (_: Exception) {
            }
        }
    }

    fun removeMusicFolder(uri: String) {
        appPrefs.removeMusicFolder(uri)
        refreshFolderEntries()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.forceRescanSongs()
            } catch (_: Exception) {
            }
        }
    }

    fun setAutoScan(enabled: Boolean) {
        appPrefs.setAutoScanEnabled(enabled)
        _autoScanEnabled.value = enabled
        if (enabled) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    repository.forceRescanSongs()
                } catch (_: Exception) {
                }
            }
        }
    }
}
