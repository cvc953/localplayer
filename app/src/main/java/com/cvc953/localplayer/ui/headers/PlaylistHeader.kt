package com.cvc953.localplayer.ui.headers

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    Column(
        modifier = modifier.fillMaxWidth().padding(8.dp),
    ) {
        PlaylistAlbumArt(
            playlistSongIds = playlist?.songIds ?: emptyList(),
            songs = songs,
            context = context,
            modifier = Modifier.fillMaxWidth(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist?.name ?: "",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.songs_count, playlistSongs.size),
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
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(60.dp),
            ) {
                Button(
                    onClick = {
                        if (playlistSongs.isNotEmpty()) {
                            playbackViewModel.setShuffle(false)
                            playbackViewModel.updateDisplayOrder(playlistSongs)
                            playbackViewModel.play(playlistSongs.first())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.action_play_now),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(stringResource(R.string.action_play), color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(60.dp),
            ) {
                Button(
                    onClick = {
                        if (playlistSongs.isNotEmpty()) {
                            val shuffled = playlistSongs.shuffled()
                            playbackViewModel.setShuffle(true)
                            playbackViewModel.updateDisplayOrder(shuffled)
                            playbackViewModel.play(shuffled.first())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
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
            Spacer(Modifier.height(20.dp))
        }
    }
}
