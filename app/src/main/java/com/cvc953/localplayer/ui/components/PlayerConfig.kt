package com.cvc953.localplayer.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.ui.RepeatMode

enum class AlbumArtShapeConfig(val key: String, val displayNameRes: Int) {
    ROUNDED_8("rounded_8", R.string.album_art_rounded_8),
    ROUNDED_22("rounded_22", R.string.album_art_rounded_22),
    CIRCLE("circle", R.string.album_art_circle);

    fun toComposeShape(): Shape = when (this) {
        ROUNDED_8 -> RoundedCornerShape(8.dp)
        ROUNDED_22 -> RoundedCornerShape(22.dp)
        CIRCLE -> CircleShape
    }
}

enum class ProgressBarStyle(val key: String, val displayNameRes: Int) {
    MATERIAL("material", R.string.progress_bar_material),
    WAVY("wavy", R.string.progress_bar_wavy),
    SQUIGGLY("squiggly", R.string.progress_bar_squiggly);
}

enum class TransportStyle(val key: String, val displayNameRes: Int) {
    DEFAULT("default", R.string.transport_default),
    LUNE("lune", R.string.transport_lune);
}



enum class PlayPauseStyle(val key: String, val displayNameRes: Int) {
    FILLED_CIRCLE("filled_circle", R.string.playpause_filled),
    OUTLINED_CIRCLE("outlined_circle", R.string.playpause_outlined);
}

data class ProgressBarState(
    val currentPosition: Long,
    val duration: Long,
    val isPlaying: Boolean,
)

data class PlayerControlState(
    val isPlaying: Boolean,
    val currentPosition: Long,
    val duration: Long,
    val isShuffle: Boolean,
    val repeatMode: RepeatMode,
    val isFavorite: Boolean,
    val audioFormat: String = "",
    val audioBitrate: String = "",
    val audioSampleRate: String = "",
    val primaryContentColor: Color,
    val secondaryContentColor: Color,
    val metaColor: Color,
    val dominantColor: Color,
)

data class PlayerControlActions(
    val onPlayPause: () -> Unit,
    val onNext: () -> Unit,
    val onPrevious: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onSeekStart: () -> Unit,
    val onSeekEnd: () -> Unit,
    val onShuffleToggle: () -> Unit,
    val onRepeatToggle: () -> Unit,
    val onFavoriteToggle: () -> Unit,
    val onShowQueue: () -> Unit = {},
    val onShowAddToPlaylist: () -> Unit = {},
    val onToggleLyrics: () -> Unit = {},
)

data class PlayerConfig(
    val albumArtShape: AlbumArtShapeConfig = AlbumArtShapeConfig.ROUNDED_8,
    val progressBarStyle: ProgressBarStyle = ProgressBarStyle.MATERIAL,
    val transportStyle: TransportStyle = TransportStyle.DEFAULT,
    val playPauseStyle: PlayPauseStyle = PlayPauseStyle.FILLED_CIRCLE,
    val showAudioInfo: Boolean = true,
)

fun parseAlbumArtShape(key: String): AlbumArtShapeConfig =
    AlbumArtShapeConfig.entries.firstOrNull { it.key == key } ?: AlbumArtShapeConfig.ROUNDED_8

fun parseProgressBarStyle(key: String): ProgressBarStyle =
    ProgressBarStyle.entries.firstOrNull { it.key == key } ?: ProgressBarStyle.MATERIAL

fun parseTransportStyle(key: String): TransportStyle =
    TransportStyle.entries.firstOrNull { it.key == key } ?: TransportStyle.DEFAULT

fun parsePlayPauseStyle(key: String): PlayPauseStyle =
    PlayPauseStyle.entries.firstOrNull { it.key == key } ?: PlayPauseStyle.FILLED_CIRCLE
