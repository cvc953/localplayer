package com.cvc953.localplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.rotate
import com.cvc953.localplayer.viewmodel.MainViewModel
import kotlin.math.abs

@Composable
fun EqualizerScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val bandCount by viewModel.bandCount.collectAsState()
    val bandFreqs by viewModel.bandFreqs.collectAsState()
    val bandLevels by viewModel.bandLevels.collectAsState()
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsState()

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
                "${sign}${whole}.${decimal}k"
            }
        } else {
            "${sign}${absn}"
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ecualizador", color = Color.White, fontSize = 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White) }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(8.dp))

            // System presets (from device Equalizer) and user presets are shown below
            val equalizerPresets by viewModel.equalizerPresets.collectAsState()
            val selectedPreset by viewModel.selectedPresetIndex.collectAsState()
            if (equalizerPresets.isNotEmpty()) {
                var expandedSys by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Presets del sistema:", color = Color.White, modifier = Modifier.weight(1f))
                    TextButton(onClick = { expandedSys = true }) { Text(if (selectedPreset >= 0 && selectedPreset < equalizerPresets.size) equalizerPresets[selectedPreset] else "Seleccionar", color = Color.White) }
                    DropdownMenu(expanded = expandedSys, onDismissRequest = { expandedSys = false }) {
                        equalizerPresets.forEachIndexed { idx, name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = {
                                viewModel.setEqualizerPreset(idx)
                                expandedSys = false
                            })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (bandCount > 0) {
                // Horizontal list of vertical sliders (one column per band)
                Row(modifier = Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    for (i in 0 until bandCount) {
                        val freq = if (i < bandFreqs.size) bandFreqs[i] / 1000 else 0
                        val level = if (i < bandLevels.size) bandLevels[i] else 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                            Text("${formatWithK(freq)}Hz", color = Color.White, fontSize = 12.sp)
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
                                        valueRange = -1500f..1500f,
                                        modifier = Modifier.matchParentSize(),
                                        trackWidth = 2.dp,
                                        thumbRadius = 10.dp,
                                        activeColor = Color(0xFFB58CFF),
                                        inactiveColor = Color.White.copy(alpha = 0.12f),
                                        backgroundColor = Color.Transparent
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(formatWithK(sliderPos.toInt()), color = Color.White, fontSize = 12.sp)
                                Text("mB", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Preset actions: save (dialog), reset and user presets list
                var showSaveDialog by remember { mutableStateOf(false) }
                var dialogName by remember { mutableStateOf("") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.resetBandLevels() }) { Text("Reset") }
                    Button(onClick = { showSaveDialog = true }, modifier = Modifier.weight(1f)) { Text("Guardar preset") }
                }
                if (showSaveDialog) {
                    AlertDialog(onDismissRequest = { showSaveDialog = false }, title = { Text("Guardar preset") }, text = {
                        Column {
                            OutlinedTextField(value = dialogName, onValueChange = { dialogName = it }, label = { Text("Nombre preset") })
                        }
                    }, confirmButton = {
                        TextButton(onClick = {
                            if (dialogName.isNotBlank()) {
                                viewModel.saveUserPreset(dialogName.trim())
                            }
                            dialogName = ""
                            showSaveDialog = false
                        }) { Text("Guardar") }
                    }, dismissButton = {
                        TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar") }
                    })
                }
                Spacer(modifier = Modifier.height(8.dp))
                val userPresets by viewModel.userPresets.collectAsState()
                if (userPresets.isNotEmpty()) {
                    Text("Presets de usuario:", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        userPresets.forEach { (name, levels) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(name, color = Color.White, modifier = Modifier.weight(1f))
                                Button(onClick = { viewModel.applyUserPreset(name) }) { Text("Aplicar") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = { viewModel.removeUserPreset(name) }) { Text("Eliminar") }
                            }
                        }
                    }
                }
            } else {
                Text("No hay ecualizador disponible para la sesión actual.", color = Color.White)
            }
        }
    }
}
