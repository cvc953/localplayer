package com.cvc953.localplayer.ui.headers

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.model.Playlist
import com.cvc953.localplayer.model.Song
import com.cvc953.localplayer.ui.components.PlaylistAlbumArt
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import kotlinx.coroutines.launch

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlaylistHeader(
    playlist: Playlist?,
    songs: List<Song>,
    context: android.content.Context,
    playlistSongs: List<Song>,
    playbackViewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    val albumArtComponent: @Composable () -> Unit = {
        PlaylistAlbumArt(
            playlistSongIds = playlist?.songIds ?: emptyList(),
            songs = songs,
            context = context,
            customImageUri = playlist?.imageUri,
            modifier = Modifier.fillMaxWidth(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
        )
    }

    val infoColumn: @Composable () -> Unit = {
        Column {
            Text(
                text = playlist?.name ?: "",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val totalDurationMs = remember(playlistSongs) {
                var sum = 0L; for (s in playlistSongs) sum += s.duration; sum
            }
            Text(
                text = buildString {
                    append(playlistSongs.size)
                    append(" canciones")
                    if (totalDurationMs > 0) {
                        append(" • ")
                        val totalSec = totalDurationMs / 1000
                        val h = totalSec / 3600
                        val m = (totalSec % 3600) / 60
                        val s = totalSec % 60
                        if (h > 0) {
                            append("$h:")
                            append("%02d:%02d".format(m, s))
                        } else {
                            append("%02d:%02d".format(m, s))
                        }
                    }
                },
                fontSize = 16.sp,
                color = MaterialTheme.extendedColors.textSecondary,
            )

            Spacer(modifier = Modifier.height(20.dp))

            val buttonColor = MaterialTheme.colorScheme.primary

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val scope = rememberCoroutineScope()
                val playAnim = remember { Animatable(1f) }
                val shuffleAnim = remember { Animatable(1f) }

                Box(modifier = Modifier.weight(1f).height(60.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                playAnim.snapTo(0.92f)
                                playAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                            }
                            if (playlistSongs.isNotEmpty()) {
                                playbackViewModel.setShuffle(false)
                                playbackViewModel.updateDisplayOrder(playlistSongs)
                                playbackViewModel.play(playlistSongs.first())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = playAnim.value; scaleY = playAnim.value },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.action_play_now), tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(stringResource(R.string.action_play), color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).height(60.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                shuffleAnim.snapTo(0.92f)
                                shuffleAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
                            }
                            if (playlistSongs.isNotEmpty()) {
                                val shuffled = playlistSongs.shuffled()
                                playbackViewModel.setShuffle(true)
                                playbackViewModel.updateDisplayOrder(shuffled)
                                playbackViewModel.play(shuffled.first())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = shuffleAnim.value; scaleY = shuffleAnim.value },
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.action_shuffle), tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(stringResource(R.string.action_shuffle), color = Color.White, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Box(modifier = Modifier.weight(0.4f)) { albumArtComponent() }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(0.6f)) { infoColumn() }
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth().padding(8.dp),
        ) {
            albumArtComponent()
            Spacer(modifier = Modifier.height(8.dp))
            infoColumn()
        }
    }
}
