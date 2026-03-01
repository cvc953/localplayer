package com.cvc953.localplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.viewmodel.MainViewModel
import kotlin.math.abs

@Suppress("ktlint:standard:function-naming")
@Composable
fun EqualizerScreen(
    viewModel: MainViewModel,
    onClose: () -> Unit,
) {
    val bandCount by viewModel.bandCount.collectAsState()
    val bandFreqs by viewModel.bandFreqs.collectAsState()
    val bandLevels by viewModel.bandLevels.collectAsState()
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsState()
    val bandLevelRange by viewModel.bandLevelRange.collectAsState()

    BackHandler { onClose() }

    fun formatWithK(n: Int): String {
        val sign = if (n < 0) "-" else ""
        val absn = abs(n)
        return if (absn >= 1000) {
            val whole = absn / 1000
            val rem = absn % 1000
            if (rem == 0) {
                "${sign}${whole}k"
            } else {
                // one decimal place
                val decimal = (rem / 100)
                "${sign}$whole.${decimal}k"
            }
        } else {
            "${sign}$absn"
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ecualizador", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onClose,
                ) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onBackground) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(8.dp))

            // System presets (from device Equalizer) and user presets are shown below
            val equalizerPresets by viewModel.equalizerPresets.collectAsState()
            val userPresets by viewModel.userPresets.collectAsState()
            val selectedPreset by viewModel.selectedPresetIndex.collectAsState()
            val selectedPresetName by viewModel.selectedPresetName.collectAsState()
            // combine system presets and user presets into single menu; user presets marked true
            val combined =
                remember(equalizerPresets, userPresets) {
                    val sys = equalizerPresets.map { it to false }
                    val usr = userPresets.map { it.first to true }
                    sys + usr
                }
            if (combined.isNotEmpty()) {
                var expandedSys by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Presets:", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    TextButton(onClick = { expandedSys = true }) {
                        Text(
                            text =
                                selectedPresetName
                                    ?: if (selectedPreset >= 0 &&
                                        selectedPreset < equalizerPresets.size
                                    ) {
                                        equalizerPresets[selectedPreset]
                                    } else {
                                        "Seleccionar"
                                    },
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    DropdownMenu(
                        expanded = expandedSys,
                        onDismissRequest = { expandedSys = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    ) {
                        combined.forEachIndexed { idx, pair ->
                            val (name, isUser) = pair
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                                        if (isUser) {
                                            IconButton(onClick = {
                                                viewModel.removeUserPreset(name)
                                            }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Eliminar preset",
                                                    tint = MaterialTheme.colorScheme.onBackground,
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    if (isUser) {
                                        viewModel.applyUserPreset(name)
                                    } else {
                                        // system preset index is the same as idx when in sys part
                                        viewModel.setEqualizerPreset(idx)
                                    }
                                    expandedSys = false
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (bandCount > 0) {
                // Horizontal list of vertical sliders (one column per band)
                Row(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 0 until bandCount) {
                        val freq = if (i < bandFreqs.size) bandFreqs[i] / 1000 else 0
                        val level = if (i < bandLevels.size) bandLevels[i] else 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                            Text("${formatWithK(freq)}Hz", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            // Vertical slider implemented rotating a Slider
                            var sliderPos by remember { mutableStateOf(level.toFloat()) }
                            // Keep local slider position in sync when ViewModel level changes (e.g., reset or preset apply)
                            LaunchedEffect(key1 = level) {
                                sliderPos = level.toFloat()
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.layout.Box(modifier = Modifier.size(64.dp, 260.dp)) {
                                    com.cvc953.localplayer.ui.components.VerticalSlider(
                                        value = sliderPos,
                                        onValueChange = {
                                            sliderPos = it
                                            // live update
                                            viewModel.setBandLevel(i, it.toInt())
                                        },
                                        valueRange = bandLevelRange.first.toFloat()..bandLevelRange.second.toFloat(),
                                        modifier = Modifier.matchParentSize(),
                                        trackWidth = 2.dp,
                                        thumbRadius = 10.dp,
                                        activeColor = MaterialTheme.colorScheme.primary,
                                        inactiveColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                                        backgroundColor = Color.Transparent,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatWithK(sliderPos.toInt()), color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                                Text("mB", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f), fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Preset actions: save (dialog), reset and user presets list
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        viewModel.resetBandLevels()
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Reset") }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // User presets section removed per user request
            } else {
                Text("No hay ecualizador disponible para la sesión actual.", color = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}
