package com.cvc953.localplayer.ui.headers

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.SongRepository
import com.cvc953.localplayer.ui.SongItem
import com.cvc953.localplayer.ui.components.DraggableSwipeRow
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.ui.normalizeArtistName
import com.cvc953.localplayer.viewmodel.ArtistViewModel
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.isNotEmpty

@Suppress("ktlint:standard:function-naming")
@Composable
fun ArtistHeader(
    artistViewModel: ArtistViewModel,
    artistName: String,
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier,
    onViewAllSongs: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        val songs by artistViewModel.getSongsForArtist(artistName).collectAsState(initial = emptyList())
        val repo = remember { SongRepository(artistViewModel.getApplication<Application>()) }
        val allSongs = remember { repo.loadSongs() }
        val artistSongs =
            remember(allSongs, artistName) {
                allSongs.filter { song -> normalizeArtistName(song.artist).any { it.equals(artistName, ignoreCase = true) } }
            }
        var artistArt by remember { mutableStateOf<Bitmap?>(null) }
        val firstSong = artistSongs.firstOrNull()
        val context = LocalContext.current

        LaunchedEffect(firstSong?.uri, firstSong?.filePath) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = firstSong?.uri ?: return@withContext
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val picture = retriever.embeddedPicture
                    retriever.release()
                    if (picture != null && picture.isNotEmpty()) {
                        artistArt = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                    }

                    if (artistArt == null) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                val bmp = BitmapFactory.decodeStream(stream)
                                if (bmp != null) artistArt = bmp
                            }
                        } catch (_: Exception) {
                        }
                    }

                    if (artistArt == null) {
                        val path = firstSong?.filePath
                        if (!path.isNullOrBlank()) {
                            try {
                                val dir = java.io.File(path).parentFile
                                val candidates = listOf("cover.jpg", "folder.jpg", "album.jpg", "front.jpg", "cover.png", "folder.png")
                                for (name in candidates) {
                                    val f = java.io.File(dir, name)
                                    if (f.exists() && f.length() > 0) {
                                        val bmp = BitmapFactory.decodeFile(f.absolutePath)
                                        if (bmp != null) {
                                            artistArt = bmp
                                            break
                                        }
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

        Image(
            painter = artistArt?.asImageBitmap()?.let { BitmapPainter(it) } ?: painterResource(R.drawable.ic_default_album),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxWidth(1f)
                    .aspectRatio(1f)
                    // .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artistName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "${artistSongs.size} canciones", fontSize = 16.sp, color = MaterialTheme.extendedColors.textSecondary)

        Spacer(modifier = Modifier.height(8.dp))

        val buttonColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Canciones", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(
                text = "Ver todas",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.extendedColors.textSecondary,
                modifier = Modifier.clickable { onViewAllSongs() },
            )
        }
    }
}
