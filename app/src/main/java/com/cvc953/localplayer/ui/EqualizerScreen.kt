package com.cvc953.localplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.ui.components.VerticalSlider
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.viewmodel.EqualizerViewModel
import kotlin.math.abs

@Suppress("ktlint:standard:function-naming")
@Composable
fun EqualizerScreen(
    viewModel: EqualizerViewModel,
    onClose: () -> Unit,
) {
    val bandCount by viewModel.bandCount.collectAsState()
    val bandFreqs by viewModel.bandFreqs.collectAsState()
    val bandLevels by viewModel.bandLevels.collectAsState()
    val bandLevelRange by viewModel.bandLevelRange.collectAsState()
    val equalizerPresets by viewModel.equalizerPresets.collectAsState()
    val userPresets by viewModel.userPresets.collectAsState()
    val selectedPreset by viewModel.selectedPresetIndex.collectAsState()
    val selectedPresetName by viewModel.selectedPresetName.collectAsState()

    var expandedPresets by remember { mutableStateOf(false) }

    val combinedPresets =
        remember(equalizerPresets, userPresets) {
            val systemPresets = equalizerPresets.map { it to false }
            val customPresets = userPresets.map { it.first to true }
            systemPresets + customPresets
        }

    BackHandler(onBack = onClose)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Ecualizador",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Ajusta bandas, presets y respuesta de audio",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                        fontSize = 13.sp,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

            EqualizerSectionCard(
                title = "Presets",
                subtitle = "Elige perfiles del sistema o creados por ti",
            ) {
                if (combinedPresets.isEmpty()) {
                    Text(
                        "No hay presets disponibles en esta sesion.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Preset activo", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text =
                                    selectedPresetName
                                        ?: if (selectedPreset in equalizerPresets.indices) {
                                            equalizerPresets[selectedPreset]
                                        } else {
                                            "Seleccionar"
                                        },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        OutlinedButton(onClick = { expandedPresets = true }) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cambiar")
                        }
                        DropdownMenu(
                            expanded = expandedPresets,
                            onDismissRequest = { expandedPresets = false },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            combinedPresets.forEachIndexed { index, (name, isUserPreset) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (isUserPreset) "$name (Usuario)" else name,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    trailingIcon = {
                                        if (isUserPreset) {
                                            IconButton(onClick = { viewModel.removeUserPreset(name) }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Eliminar preset",
                                                    tint = MaterialTheme.colorScheme.error,
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        if (isUserPreset) {
                                            viewModel.applyUserPreset(name)
                                        } else {
                                            viewModel.setEqualizerPreset(index)
                                        }
                                        expandedPresets = false
                                    },
                                )
                            }
                        }
                    }
                }
            }

            EqualizerSectionCard(
                title = "Bandas",
                subtitle = "Desliza para ajustar cada frecuencia",
            ) {
                if (bandCount > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(300.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (i in 0 until bandCount) {
                            val freq = if (i < bandFreqs.size) bandFreqs[i] / 1000 else 0
                            val level = if (i < bandLevels.size) bandLevels[i] else 0
                            BandSliderCard(
                                modifier = Modifier.weight(1f),
                                label = "${formatWithK(freq)}Hz",
                                initialLevel = level,
                                range = bandLevelRange,
                                onLevelChange = { viewModel.setBandLevel(i, it) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.resetBandLevels() },
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resetear bandas")
                    }
                } else {
                    Text(
                        "No hay ecualizador disponible para la sesion actual.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun EqualizerSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = LocalExtendedColors.current.surfaceSheet),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = LocalExtendedColors.current.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BandSliderCard(
    modifier: Modifier = Modifier,
    label: String,
    initialLevel: Int,
    range: Pair<Int, Int>,
    onLevelChange: (Int) -> Unit,
) {
    var sliderPos by remember { mutableStateOf(initialLevel.toFloat()) }

    LaunchedEffect(initialLevel) {
        sliderPos = initialLevel.toFloat()
    }

    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = LocalExtendedColors.current.surfaceSheet),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.height(208.dp), contentAlignment = Alignment.Center) {
                VerticalSlider(
                    value = sliderPos,
                    onValueChange = {
                        sliderPos = it
                        onLevelChange(it.toInt())
                    },
                    valueRange = range.first.toFloat()..range.second.toFloat(),
                    modifier = Modifier.fillMaxHeight().width(60.dp),
                    trackWidth = 2.dp,
                    thumbRadius = 9.dp,
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    backgroundColor = Color.Transparent,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(formatWithK(sliderPos.toInt()), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            Text("mB", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

private fun formatWithK(n: Int): String {
    val sign = if (n < 0) "-" else ""
    val absn = abs(n)
    return if (absn >= 1000) {
        val whole = absn / 1000
        val rem = absn % 1000
        if (rem == 0) {
            "${sign}${whole}k"
        } else {
            val decimal = rem / 100
            "${sign}$whole.${decimal}k"
        }
    } else {
        "${sign}$absn"
    }
}
