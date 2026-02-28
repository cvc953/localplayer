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
    mainViewModel: MainViewModel,
    onClose: () -> Unit,
) {
    val isEnabled by mainViewModel.equalizerEnabled.collectAsState()
    val bands by mainViewModel.bandLevels.collectAsState()
    val presets by mainViewModel.equalizerPresets.collectAsState()
    val userPresets by mainViewModel.userPresets.collectAsState()
    val selectedPresetIndex by mainViewModel.selectedPresetIndex.collectAsState()
    val selectedPresetName by mainViewModel.selectedPresetName.collectAsState()
    val bandCount by mainViewModel.bandCount.collectAsState()

    BackHandler { onClose() }

    // Forzar inicialización del ecualizador al entrar a la pantalla
    LaunchedEffect(Unit) {
        mainViewModel.toggleEqualizer(true)
    }

    // Obtener el sessionId actual para mostrarlo en la UI
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionId = try {
        com.cvc953.localplayer.controller.PlayerController.getInstance(context.applicationContext, null).getAudioSessionId()
    } catch (e: Exception) {
        -1
    }

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
            // Mostrar el sessionId para depuración
            Text("Audio sessionId: $sessionId", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ecualizador", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onClose,
                ) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onBackground) }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.height(8.dp))

            // Combine system and user presets
            val combinedPresets = remember(presets, userPresets) {
                val sys = presets.map { it to false }
                val usr = userPresets.map { it.first to true }
                sys + usr
            }
            if (combinedPresets.isNotEmpty()) {
                var expandedSys by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Presets:", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                    TextButton(onClick = { expandedSys = true }) {
                        Text(
                            text = selectedPresetName ?: "Seleccionar",
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    DropdownMenu(
                        expanded = expandedSys,
                        onDismissRequest = { expandedSys = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    ) {
                        combinedPresets.forEachIndexed { idx, pair ->
                            val (name, isUser) = pair
                            DropdownMenuItem(
                                text = {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(name, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                                        if (isUser) {
                                            IconButton(onClick = {
                                                mainViewModel.removeUserPreset(name)
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
                                        mainViewModel.applyUserPreset(name)
                                    } else {
                                        val idx = presets.indexOf(name)
                                        if (idx >= 0) mainViewModel.setEqualizerPreset(idx)
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
                        val freq = 60 * (i + 1) // Placeholder frequency: 60Hz, 120Hz, ...
                        val level = bands.getOrNull(i) ?: 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                            Text("${formatWithK(freq)}Hz", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            var sliderPos by remember { mutableStateOf(level.toFloat()) }
                            LaunchedEffect(key1 = level) {
                                sliderPos = level.toFloat()
                            }
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                androidx.compose.foundation.layout.Box(modifier = Modifier.size(64.dp, 260.dp)) {
                                    com.cvc953.localplayer.ui.components.VerticalSlider(
                                        value = sliderPos,
                                        onValueChange = {
                                            sliderPos = it
                                            mainViewModel.setBandLevel(i, it.toInt())
                                        },
                                        valueRange = -1500f..1500f,
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        mainViewModel.resetBandLevels()
                    }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Reset") }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Column {
                    Text("No hay ecualizador disponible para la sesión actual.", color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    // No se requiere refresh en MainViewModel, pero puedes agregar lógica si lo deseas
                    Button(onClick = { /* mainViewModel.refreshEqualizer() */ }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        Text("Reintentar")
                    }
                }
            }
        }
    }
}
