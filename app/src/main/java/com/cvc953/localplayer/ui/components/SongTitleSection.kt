package com.cvc953.localplayer.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.ui.theme.LocalExtendedColors

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongTitleSection(
    title: String,
    artist: String,
    album: String,
    albumArt: Bitmap?,
    primaryContentColor: Color,
    secondaryContentColor: Color,
    onArtistClick: () -> Unit,
    onAlbumClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val aspectRatio = screenWidth.toFloat() / screenHeight.toFloat()
    val isCompactLayout = aspectRatio >= 0.90f && aspectRatio <= 1.15f
    val isNormalLayout =
        (aspectRatio >= 1.15f && aspectRatio <= 1.6f) ||
            (aspectRatio >= 0.50f && aspectRatio < 0.75f)
    val isTallLayout = aspectRatio < 0.50f
    val isTablet = minOf(screenWidth, screenHeight) >= 600

    val titleFontSize =
        when {
            isCompactLayout -> if (isTablet) 22.sp else 16.sp
            isNormalLayout -> if (isTablet) 24.sp else 18.sp
            isTallLayout -> if (isTablet) 28.sp else 24.sp
            else -> if (isTablet) 26.sp else 22.sp
        }
    val subtitleFontSize =
        when {
            isCompactLayout -> if (isTablet) 14.sp else 11.sp
            isNormalLayout -> if (isTablet) 16.sp else 12.sp
            isTallLayout -> if (isTablet) 18.sp else 15.sp
            else -> if (isTablet) 17.sp else 14.sp
        }
    val horizontalPadding =
        when {
            isCompactLayout -> if (isTablet) 16.dp else 10.dp
            isNormalLayout -> if (isTablet) 20.dp else 12.dp
            isTallLayout -> if (isTablet) 28.dp else 20.dp
            else -> if (isTablet) 32.dp else 24.dp
        }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = title,
            color = primaryContentColor,
            fontSize = titleFontSize,
            fontWeight = FontWeight.Companion.Bold,
            maxLines = 1,
            overflow = TextOverflow.Companion.Visible,
            modifier = Modifier.fillMaxWidth().basicMarquee(),
        )

        Spacer(Modifier.height(4.dp))

        Box {
            Text(
                text = if (album.isNotEmpty()) "$artist - $album" else artist,
                color = secondaryContentColor,
                fontSize = subtitleFontSize,
                maxLines = 1,
                modifier = Modifier.clickable { showMenu = true },
            )
            if (showMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showMenu = false },
                    sheetState = sheetState,
                    containerColor = LocalExtendedColors.current.surfaceSheet,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 20.dp,
                                    vertical = 16.dp,
                                ),
                    ) {
                        Text(
                            text = "Opciones",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Companion.Bold,
                        )

                        Spacer(Modifier.height(12.dp))

                        // Fila: Ir al artista + imagen a la izquierda +
                        // nombre a la derecha
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMenu = false
                                        onArtistClick()
                                    }.padding(vertical = 8.dp),
                            verticalAlignment =
                                Alignment.CenterVertically,
                        ) {
                            // Imagen del artista: usar albumArt si no
                            // hay imagen específica
                            Box(
                                modifier =
                                    Modifier
                                        .size(56.dp)
                                        .clip(
                                            androidx.compose.foundation.shape.RoundedCornerShape(
                                                8.dp,
                                            ),
                                        ).background(
                                            MaterialTheme.colorScheme.outline,
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (albumArt != null) {
                                    Image(
                                        painter =
                                            androidx.compose.ui.graphics.painter.BitmapPainter(
                                                albumArt.asImageBitmap(),
                                            ),
                                        contentDescription =
                                            "Artist image",
                                        modifier =
                                            Modifier.fillMaxSize(),
                                        contentScale =
                                            ContentScale.Companion
                                                .Crop,
                                    )
                                } else {
                                    Icon(
                                        imageVector =
                                            Icons.Default
                                                .Person,
                                        contentDescription =
                                        null,
                                        tint =
                                            MaterialTheme.colorScheme.onBackground
                                                .copy(
                                                    alpha =
                                                    0.6f,
                                                ),
                                        modifier =
                                            Modifier.size(
                                                28.dp,
                                            ),
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ir al artista",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                )
                                Text(
                                    text = artist,
                                    color = LocalExtendedColors.current.textSecondarySoft,
                                    fontSize = 14.sp,
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Fila: Ir al álbum + imagen a la izquierda +
                        // nombre a la derecha
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMenu = false
                                        onAlbumClick()
                                    }.padding(vertical = 8.dp),
                            verticalAlignment =
                                Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(56.dp)
                                        .clip(
                                            androidx.compose.foundation.shape.RoundedCornerShape(
                                                8.dp,
                                            ),
                                        ).background(MaterialTheme.colorScheme.outline),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (albumArt != null) {
                                    Image(
                                        painter =
                                            androidx.compose.ui.graphics.painter.BitmapPainter(
                                                albumArt.asImageBitmap(),
                                            ),
                                        contentDescription =
                                            "Album art",
                                        modifier =
                                            Modifier.fillMaxSize(),
                                        contentScale =
                                            ContentScale.Companion
                                                .Crop,
                                    )
                                } else {
                                    Icon(
                                        imageVector =
                                            Icons.Default
                                                .MusicNote,
                                        contentDescription =
                                        null,
                                        tint =
                                            MaterialTheme.colorScheme.onBackground
                                                .copy(
                                                    alpha =
                                                    0.6f,
                                                ),
                                        modifier =
                                            Modifier.size(
                                                28.dp,
                                            ),
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Ir al álbum",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 16.sp,
                                )
                                Text(
                                    text = album,
                                    color = LocalExtendedColors.current.textSecondarySoft,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
