package com.cvc953.localplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.ui.RepeatMode
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.viewmodel.PlaybackViewModel
import com.cvc953.localplayer.viewmodel.PlayerViewModel

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlayerControls(
    playbackViewModel: PlaybackViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    isPlaying: Boolean,
    foregroundColor: Color,
    secondaryColor: Color,
    metaColor: Color,
    audioFormat: String = "",
    audioBitrate: String = "",
    audioSampleRate: String = "",
) {
    val playerState by playbackViewModel.playerState.collectAsState()
    val isShuffle by playbackViewModel.isShuffle.collectAsState()
    val repeatMode by playbackViewModel.repeatMode.collectAsState()

    var sliderPosition by remember { mutableStateOf(0f) }
    var isUserSeeking by remember { mutableStateOf(false) }

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
    val isCompactLayout = aspectRatio >= 0.90f && aspectRatio <= 1.15f
    val isNormalLayout =
        (aspectRatio in 1.15f..1.6f) ||
            (aspectRatio in 0.50f..<0.75f)
    val isTallLayout = aspectRatio < 0.50f
    val isTablet = minOf(screenWidth, screenHeight) >= 600

    // Sincroniza el slider con el estado global solo si no se está arrastrando
    LaunchedEffect(playerState.position, playerState.duration, isUserSeeking) {
        if (!isUserSeeking) {
            sliderPosition = playerState.position.toFloat().coerceIn(0f, playerState.duration.toFloat())
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Slider(
                value = sliderPosition,
                onValueChange = {
                    isUserSeeking = true
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    playbackViewModel.seekTo(sliderPosition.toLong())
                    isUserSeeking = false
                },
                valueRange = 0f..playerState.duration.toFloat(),
                modifier = Modifier.fillMaxWidth().height(20.dp),
                colors =
                    SliderDefaults.colors(
                        thumbColor = foregroundColor,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.extendedColors.textSecondarySoft,
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDuration(playerState.position),
                    color = secondaryColor,
                    fontSize = 10.sp,
                )
                Text(
                    text = formatDuration(playerState.duration),
                    color = secondaryColor,
                    fontSize = 10.sp,
                )
            }
        }

        Spacer(
            Modifier.height(
                when {
                    isCompactLayout -> if (isTablet) 8.dp else 4.dp
                    isNormalLayout -> if (isTablet) 10.dp else 6.dp
                    isTallLayout -> if (isTablet) 20.dp else 16.dp
                    else -> if (isTablet) 16.dp else 12.dp
                },
            ),
        )

        val buttonSize =
            with(LocalDensity.current) {
                val ref = minOf(screenWidth, screenHeight)
                val tabletMul = if (isTablet) 1.3f else 1f
                when {
                    isCompactLayout -> (ref.dp * 0.14f * tabletMul)
                    isNormalLayout -> (ref.dp * 0.15f * tabletMul)
                    isTallLayout -> (ref.dp * 0.20f * tabletMul)
                    else -> (ref.dp * 0.18f * tabletMul)
                }
            }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(onClick = { playbackViewModel.toggleShuffle() }) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else foregroundColor,
                    modifier = Modifier.size(buttonSize),
                )
            }

            IconButton(onClick = { playbackViewModel.playPreviousSong() }) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    null,
                    tint = foregroundColor,
                    modifier = Modifier.size(buttonSize),
                )
            }

            IconButton(
                onClick = { playbackViewModel.togglePlayPause() },
                modifier =
                    Modifier
                        .size(buttonSize)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape =
                                androidx.compose.foundation.shape
                                    .RoundedCornerShape(50),
                        ),
            ) {
                Icon(
                    imageVector =
                        if (isPlaying) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                    contentDescription = null,
                    tint = foregroundColor,
                    modifier = Modifier.size(buttonSize * 0.5f),
                )
            }

            IconButton(onClick = { playbackViewModel.playNextSong() }) {
                Icon(
                    Icons.Rounded.SkipNext,
                    null,
                    tint = foregroundColor,
                    modifier = Modifier.size(buttonSize),
                )
            }

            IconButton(onClick = { playbackViewModel.toggleRepeat() }) {
                Icon(
                    when (repeatMode) {
                        RepeatMode.NONE -> Icons.Rounded.Repeat
                        RepeatMode.ONE -> Icons.Rounded.RepeatOne
                        RepeatMode.ALL -> Icons.Rounded.Repeat
                    },
                    contentDescription = "Repeat",
                    tint =
                        if (repeatMode != RepeatMode.NONE) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            foregroundColor
                        },
                    modifier = Modifier.size(buttonSize),
                )
            }
        }

        // Información de formato de audio
        if (audioFormat.isNotEmpty() || audioBitrate.isNotEmpty() || audioSampleRate.isNotEmpty()) {
            Spacer(
                Modifier.height(
                    when {
                        isCompactLayout -> if (isTablet) 10.dp else 6.dp
                        isNormalLayout -> if (isTablet) 10.dp else 6.dp
                        isTallLayout -> if (isTablet) 18.dp else 14.dp
                        else -> if (isTablet) 16.dp else 12.dp
                    },
                ),
            )
            Text(
                text =
                    buildString {
                        if (audioFormat.isNotEmpty()) append(audioFormat)
                        if (audioFormat.isNotEmpty() && audioBitrate.isNotEmpty()) append(" • ")
                        if (audioBitrate.isNotEmpty()) append(audioBitrate)
                        if ((audioFormat.isNotEmpty() || audioBitrate.isNotEmpty()) && audioSampleRate.isNotEmpty()) {
                            append(" • ")
                        }
                        if (audioSampleRate.isNotEmpty()) append(audioSampleRate)
                    },
                color = metaColor,
                fontSize =
                    when {
                        isCompactLayout -> if (isTablet) 12.sp else 9.sp
                        isNormalLayout -> if (isTablet) 13.sp else 10.sp
                        isTallLayout -> if (isTablet) 15.sp else 12.sp
                        else -> if (isTablet) 14.sp else 11.sp
                    },
                textAlign = TextAlign.Companion.Center,
            )
        }
    }
}
