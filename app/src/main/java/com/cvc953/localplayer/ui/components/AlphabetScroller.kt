package com.cvc953.localplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Generic alphabet scroller component for scrolling through items starting with each letter.
 * Must be called within a Box composable context to use the align modifier.
 *
 * @param T The type of items in the list
 * @param items The list of items to scroll through
 * @param getItemName Function to extract the display name from each item
 * @param currentScrollLetter The currently active letter
 * @param onLetterSelected Callback when a letter is selected (will be called with the selected letter).
 * @param onScrollToIndex Callback to scroll to a specific index (receives: index, isGrid).
 * @param viewAsGrid Whether currently viewing as grid (true) or list (false)
 * @param scope CoroutineScope for launching scroll operations and managing timing
 */
@Composable
fun <T> BoxScope.AlphabetScrollerContent(
    items: List<T>,
    getItemName: (T) -> String,
    currentScrollLetter: String?,
    onLetterSelected: (String?) -> Unit,
    onScrollToIndex: (Int, Boolean) -> Unit,
    viewAsGrid: Boolean,
    scope: CoroutineScope,
) {
    if (items.isEmpty()) return

    val alphabet = listOf("#") + ('A'..'Z').map { it.toString() }
    var columnHeight = remember { 0f }

    fun findIndexForLetter(letter: String): Int {
        return if (letter == "#") {
            items.indexOfFirst { item ->
                val firstChar = getItemName(item).firstOrNull()?.uppercaseChar()
                firstChar == null || !firstChar.isLetter()
            }
        } else {
            items.indexOfFirst { item ->
                getItemName(item).firstOrNull()?.uppercaseChar() == letter[0]
            }
        }
    }

    fun scrollToLetter(letter: String) {
        onLetterSelected(letter)
        scope.launch {
            delay(800)
            onLetterSelected(null)  // Reset the current letter after detection
        }
        val index = findIndexForLetter(letter)
        if (index >= 0) {
            onScrollToIndex(index, viewAsGrid)
        }
    }

    Column(
        modifier =
            Modifier
                .align(Alignment.CenterEnd)
                .width(28.dp)
                .fillMaxHeight()
                .onGloballyPositioned { coords ->
                    columnHeight = coords.size.height.toFloat()
                }.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val index =
                                ((offset.y / columnHeight) * alphabet.size)
                                    .toInt()
                                    .coerceIn(0, alphabet.lastIndex)
                            scrollToLetter(alphabet[index])
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val y = change.position.y.coerceIn(0f, columnHeight)
                            val index =
                                ((y / columnHeight) * alphabet.size)
                                    .toInt()
                                    .coerceIn(0, alphabet.lastIndex)
                            scrollToLetter(alphabet[index])
                        },
                    )
                },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        alphabet.forEach { letter ->
            val isActive = currentScrollLetter == letter
            Text(
                text = letter,
                color =
                    if (isActive) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    },
                fontSize = if (isActive) 12.sp else 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .weight(1f)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .clickable { scrollToLetter(letter) },
            )
        }
    }
}

/**
 * Composable for displaying the current scroll letter in a centered box.
 * Must be called within a Box composable context to use the align modifier.
 *
 * @param letter The letter to display
 */
@Composable
fun BoxScope.ScrollLetterDisplay(letter: String) {
    Box(
        modifier =
            Modifier
                .align(Alignment.Center)
                .size(
                    with(LocalDensity.current) {
                        LocalConfiguration.current.screenWidthDp.dp * 0.25f
                    },
                ).background(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.8f),
                    RoundedCornerShape(16.dp),
                ).border(
                    2.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(16.dp),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
