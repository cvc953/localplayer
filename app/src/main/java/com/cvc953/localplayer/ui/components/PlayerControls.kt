package com.cvc953.localplayer.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.ui.RepeatMode

@Suppress("ktlint:standard:function-naming")
@Composable
fun PlayerControlsContent(
    config: PlayerConfig,
    state: PlayerControlState,
    actions: PlayerControlActions,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
    val isCompactLayout = aspectRatio >= 0.90f && aspectRatio <= 1.15f
    val isNormalLayout =
        (aspectRatio in 1.15f..1.6f) ||
            (aspectRatio in 0.50f..<0.75f)
    val isTallLayout = aspectRatio < 0.50f
    val isTablet = minOf(screenWidth, screenHeight) >= 600

    val progressBarState =
        ProgressBarState(
            currentPosition = state.currentPosition,
            duration = state.duration,
            isPlaying = state.isPlaying,
        )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            ProgressBarForConfig(
                style = config.progressBarStyle,
                state = progressBarState,
                color = state.primaryContentColor,
                onSeek = actions.onSeek,
                onSeekStart = actions.onSeekStart,
                onSeekEnd = actions.onSeekEnd,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatDuration(state.currentPosition),
                    color = state.secondaryContentColor,
                    fontSize = 10.sp,
                )
                Text(
                    text = formatDuration(state.duration),
                    color = state.secondaryContentColor,
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

        if (config.transportStyle == TransportStyle.LUNE) {
            LuneTransportRow(
                state = state,
                actions = actions,
                buttonSize = buttonSize,
            )
        } else {
            DefaultTransportRow(
                state = state,
                actions = actions,
                playPauseStyle = config.playPauseStyle,
                buttonSize = buttonSize,
            )
        }

        if (config.showAudioInfo &&
            (state.audioFormat.isNotEmpty() || state.audioBitrate.isNotEmpty() || state.audioSampleRate.isNotEmpty())
        ) {
            Spacer(
                Modifier.height(
                    when {
                        isCompactLayout -> if (isTablet) 14.dp else 10.dp
                        isNormalLayout -> if (isTablet) 16.dp else 12.dp
                        isTallLayout -> if (isTablet) 24.dp else 20.dp
                        else -> if (isTablet) 22.dp else 18.dp
                    },
                ),
            )
            Text(
                text =
                    buildString {
                        if (state.audioFormat.isNotEmpty()) append(state.audioFormat)
                        if (state.audioFormat.isNotEmpty() && state.audioBitrate.isNotEmpty()) append(" • ")
                        if (state.audioBitrate.isNotEmpty()) append(state.audioBitrate)
                        if ((state.audioFormat.isNotEmpty() || state.audioBitrate.isNotEmpty()) && state.audioSampleRate.isNotEmpty()) {
                            append(" • ")
                        }
                        if (state.audioSampleRate.isNotEmpty()) append(state.audioSampleRate)
                    },
                color = state.metaColor,
                fontSize =
                    when {
                        isCompactLayout -> if (isTablet) 12.sp else 9.sp
                        isNormalLayout -> if (isTablet) 13.sp else 10.sp
                        isTallLayout -> if (isTablet) 15.sp else 12.sp
                        else -> if (isTablet) 14.sp else 11.sp
                    },
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(if (isTablet) 18.dp else 14.dp))
        if (config.transportStyle == TransportStyle.LUNE) {
            val bottomBtnSize =
                with(LocalDensity.current) {
                    (buttonSize * 0.55f).coerceAtLeast(36.dp).coerceAtMost(48.dp)
                }
            LuneBottomRow(
                state = state,
                actions = actions,
                buttonSize = bottomBtnSize,
            )
        } else {
            IconButtonsBottomRow(
                state = state,
                actions = actions,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun ProgressBarForConfig(
    style: ProgressBarStyle,
    state: ProgressBarState,
    color: Color,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    modifier: Modifier,
) {
    when (style) {
        ProgressBarStyle.MATERIAL -> {
            MaterialSliderProgressBar(
                state = state,
                onSeek = onSeek,
                onSeekStart = onSeekStart,
                onSeekEnd = onSeekEnd,
                trackColor = color,
                modifier = modifier,
            )
        }

        ProgressBarStyle.WAVY -> {
            PixelPlayerProgressBar(
                progress = if (state.duration > 0L) state.currentPosition.toFloat() / state.duration.toFloat() else 0f,
                duration = state.duration,
                onSeek = onSeek,
                onSeekStart = onSeekStart,
                onSeekEnd = onSeekEnd,
                color = color,
                modifier = modifier,
            )
        }

        ProgressBarStyle.SQUIGGLY -> {
            SquigglyProgressBar(
                progress = if (state.duration > 0L) state.currentPosition.toFloat() / state.duration.toFloat() else 0f,
                duration = state.duration,
                isPlaying = state.isPlaying,
                onSeek = onSeek,
                onSeekStart = onSeekStart,
                onSeekEnd = onSeekEnd,
                color = color,
                modifier = modifier,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun DefaultTransportRow(
    state: PlayerControlState,
    actions: PlayerControlActions,
    playPauseStyle: PlayPauseStyle,
    buttonSize: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        IconButton(onClick = { actions.onShuffleToggle() }) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                tint = if (state.isShuffle) MaterialTheme.colorScheme.primary else state.primaryContentColor,
                modifier = Modifier.size(buttonSize),
            )
        }

        IconButton(onClick = { actions.onPrevious() }) {
            Icon(
                Icons.Rounded.SkipPrevious,
                null,
                tint = state.primaryContentColor,
                modifier = Modifier.size(buttonSize),
            )
        }

        val playPauseModifier =
            when (playPauseStyle) {
                PlayPauseStyle.FILLED_CIRCLE -> {
                    Modifier
                        .size(buttonSize)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50),
                        )
                }

                PlayPauseStyle.OUTLINED_CIRCLE -> {
                    Modifier
                        .size(buttonSize)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        )
                }
            }

        IconButton(
            onClick = { actions.onPlayPause() },
            modifier = playPauseModifier,
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = state.primaryContentColor,
                modifier = Modifier.size(buttonSize * 0.5f),
            )
        }

        IconButton(onClick = { actions.onNext() }) {
            Icon(
                Icons.Rounded.SkipNext,
                null,
                tint = state.primaryContentColor,
                modifier = Modifier.size(buttonSize),
            )
        }

        IconButton(onClick = { actions.onRepeatToggle() }) {
            Icon(
                when (state.repeatMode) {
                    RepeatMode.NONE -> Icons.Rounded.Repeat
                    RepeatMode.ONE -> Icons.Rounded.RepeatOne
                    RepeatMode.ALL -> Icons.Rounded.Repeat
                },
                contentDescription = "Repeat",
                tint =
                    if (state.repeatMode != RepeatMode.NONE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        state.primaryContentColor
                    },
                modifier = Modifier.size(buttonSize),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun IconButtonsBottomRow(
    state: PlayerControlState,
    actions: PlayerControlActions,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IconButton(onClick = { actions.onFavoriteToggle() }) {
            Icon(
                imageVector = if (state.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.Favorite,
                contentDescription = null,
                tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else state.primaryContentColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        IconButton(onClick = { actions.onShowQueue() }) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = state.primaryContentColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        IconButton(onClick = { actions.onShowAddToPlaylist() }) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null,
                tint = state.primaryContentColor,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        IconButton(onClick = { actions.onToggleLyrics() }) {
            Icon(
                Icons.Default.Lyrics,
                contentDescription = null,
                tint = state.primaryContentColor,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun LuneTransportRow(
    state: PlayerControlState,
    actions: PlayerControlActions,
    buttonSize: Dp,
) {
    val skipSize = (buttonSize * 0.85f).coerceAtLeast(40.dp).coerceAtMost(56.dp)
    val playSize = (skipSize * 1.2f).coerceAtMost(68.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        LuneCircleButton(
            icon = Icons.Rounded.Shuffle,
            tint = if (state.isShuffle) MaterialTheme.colorScheme.primary else state.primaryContentColor,
            onClick = actions.onShuffleToggle,
            size = skipSize,
        )

        LuneCircleButton(
            icon = Icons.Rounded.SkipPrevious,
            tint = state.primaryContentColor,
            onClick = actions.onPrevious,
            size = skipSize,
        )

        LuneCircleButton(
            icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            tint = state.primaryContentColor,
            onClick = actions.onPlayPause,
            size = playSize,
            containerColor = MaterialTheme.colorScheme.primary,
        )

        LuneCircleButton(
            icon = Icons.Rounded.SkipNext,
            tint = state.primaryContentColor,
            onClick = actions.onNext,
            size = skipSize,
        )

        LuneCircleButton(
            icon =
                when (state.repeatMode) {
                    RepeatMode.NONE -> Icons.Rounded.Repeat
                    RepeatMode.ONE -> Icons.Rounded.RepeatOne
                    RepeatMode.ALL -> Icons.Rounded.Repeat
                },
            tint = if (state.repeatMode != RepeatMode.NONE) MaterialTheme.colorScheme.primary else state.primaryContentColor,
            onClick = actions.onRepeatToggle,
            size = skipSize,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun LuneCircleButton(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    size: Dp,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "luneBounce",
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        interactionSource = interactionSource,
        modifier =
            Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun LuneBottomRow(
    state: PlayerControlState,
    actions: PlayerControlActions,
    buttonSize: Dp,
) {
    val segmentHeight = (buttonSize * 1.3f).coerceAtLeast(40.dp)
    val pillRadius = segmentHeight / 2f

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(segmentHeight)
                .clip(RoundedCornerShape(pillRadius))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            LunePillSegment(
                icon = Icons.Rounded.Favorite,
                tint = if (state.isFavorite) MaterialTheme.colorScheme.primary else state.primaryContentColor,
                active = state.isFavorite,
                onClick = actions.onFavoriteToggle,
                pillRadius = pillRadius,
                roundLeft = true,
                roundRight = false,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            LunePillSegment(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                tint = state.primaryContentColor,
                active = false,
                onClick = actions.onShowQueue,
                pillRadius = pillRadius,
                roundLeft = false,
                roundRight = false,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            LunePillSegment(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                tint = state.primaryContentColor,
                active = false,
                onClick = actions.onShowAddToPlaylist,
                pillRadius = pillRadius,
                roundLeft = false,
                roundRight = false,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            LunePillSegment(
                icon = Icons.Default.Lyrics,
                tint = state.primaryContentColor,
                active = false,
                onClick = actions.onToggleLyrics,
                pillRadius = pillRadius,
                roundLeft = false,
                roundRight = true,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun LunePillSegment(
    icon: ImageVector,
    tint: Color,
    active: Boolean,
    onClick: () -> Unit,
    pillRadius: Dp,
    roundLeft: Boolean,
    roundRight: Boolean,
    modifier: Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "lunePillBounce",
    )

    Surface(
        onClick = onClick,
        shape =
            RoundedCornerShape(
                topStart = if (roundLeft) pillRadius else 0.dp,
                bottomStart = if (roundLeft) pillRadius else 0.dp,
                topEnd = if (roundRight) pillRadius else 0.dp,
                bottomEnd = if (roundRight) pillRadius else 0.dp,
            ),
        color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
        interactionSource = interactionSource,
        modifier =
            modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}
