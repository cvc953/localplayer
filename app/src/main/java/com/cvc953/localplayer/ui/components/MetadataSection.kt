package com.cvc953.localplayer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sección de metadatos estilo Apple Music: aparece al final de las letras
 * con etiquetas legibles ("Artista", "Álbum", etc.) y valores en texto
 * pequeño, sutil, separado por una línea delgada.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun MetadataSection(
    items: List<Pair<String, String>>,
    textColor: Color = Color.White,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .padding(top = 24.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Línea separadora delgada
        HorizontalDivider(
            thickness = 0.5.dp,
            color = textColor.copy(alpha = 0.15f),
        )

        Spacer(Modifier.height(4.dp))

        items.forEachIndexed { index, (label, value) ->
            MetadataRow(
                label = label,
                value = value,
                textColor = textColor,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun MetadataRow(
    label: String,
    value: String,
    textColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = textColor.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            color = textColor.copy(alpha = 0.45f),
            fontSize = 11.sp,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
