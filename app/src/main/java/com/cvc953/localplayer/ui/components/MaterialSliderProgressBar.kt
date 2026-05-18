package com.cvc953.localplayer.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cvc953.localplayer.ui.extendedColors

@Composable
fun MaterialSliderProgressBar(
    state: ProgressBarState,
    onSeek: (Long) -> Unit,
    onSeekStart: () -> Unit,
    onSeekEnd: () -> Unit,
    trackColor: Color,
    modifier: Modifier,
) {
    var sliderPosition by remember { mutableFloatStateOf(state.currentPosition.toFloat()) }
    var isUserSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(state.currentPosition, isUserSeeking) {
        if (!isUserSeeking) {
            sliderPosition = state.currentPosition.toFloat()
        }
    }

    Slider(
        value = sliderPosition,
        onValueChange = {
            isUserSeeking = true
            sliderPosition = it
            onSeekStart()
        },
        onValueChangeFinished = {
            onSeek(sliderPosition.toLong())
            onSeekEnd()
            isUserSeeking = false
        },
        valueRange = 0f..state.duration.toFloat(),
        modifier = modifier.height(20.dp),
        colors = SliderDefaults.colors(
            thumbColor = trackColor,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.extendedColors.textSecondarySoft,
        ),
    )
}
