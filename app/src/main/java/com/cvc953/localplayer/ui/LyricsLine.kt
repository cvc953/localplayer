package com.cvc953.localplayer.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.TtmlAlignment

@Suppress("ktlint:standard:function-naming")
@Composable
fun LyricLine(
    text: String,
    active: Boolean,
    isSecondaryVoice: Boolean = false,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.Transparent,
    horizontalAlignment: TtmlAlignment = TtmlAlignment.LEFT,
    maxWidthFraction: Float = 1f,
) {
    LyricLine(
        text = text,
        active = active,
        isSecondaryVoice = isSecondaryVoice,
        modifier = Modifier,
        activeColor = activeColor,
        inactiveColor = inactiveColor,
        horizontalAlignment = horizontalAlignment,
        maxWidthFraction = maxWidthFraction,
    )
}

@Suppress("ktlint:standard:function-naming")
@Composable
fun LyricLine(
    modifier: Modifier = Modifier,
    text: String,
    active: Boolean,
    isSecondaryVoice: Boolean = false,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.Transparent,
    horizontalAlignment: TtmlAlignment = TtmlAlignment.LEFT,
    maxWidthFraction: Float = 1f,
) {
    if (isSecondaryVoice && !active) return

    val transition = updateTransition(targetState = active, label = "lyricLine")

    val scale by transition.animateFloat(
        label = "scale",
        transitionSpec = {
            if (targetState) {
                spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow)
            } else {
                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
            }
        },
    ) { isActive -> if (isActive) 1.05f else 1f }

    val color by transition.animateColor(
        label = "color",
        transitionSpec = { tween(durationMillis = 350, easing = FastOutSlowInEasing) },
    ) { isActive -> if (isActive) activeColor else inactiveColor }

    val targetFontSize = if (isSecondaryVoice) 20f else 30f
    val fontSize by animateFloatAsState(
        targetValue = targetFontSize,
        label = "fontSize",
    )

    val textAlign =
        when (horizontalAlignment) {
            TtmlAlignment.LEFT -> TextAlign.Left
            TtmlAlignment.RIGHT -> TextAlign.Right
        }
    val alignment =
        when (horizontalAlignment) {
            TtmlAlignment.LEFT -> Alignment.TopStart
            TtmlAlignment.RIGHT -> Alignment.TopEnd
        }
    val pivotX =
        when (horizontalAlignment) {
            TtmlAlignment.LEFT -> 0f
            TtmlAlignment.RIGHT -> 1f
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(pivotFractionX = pivotX, pivotFractionY = 0.5f)
                },
        contentAlignment = alignment,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(maxWidthFraction),
            contentAlignment = alignment,
        ) {
            Text(
                text = text.trimStart(),
                color = color,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = textAlign,
                lineHeight = if (isSecondaryVoice) (fontSize + 8).sp else (fontSize + 10).sp,
                maxLines = Int.MAX_VALUE,
                softWrap = true,
                overflow = TextOverflow.Visible,
            )
        }
    }
}
