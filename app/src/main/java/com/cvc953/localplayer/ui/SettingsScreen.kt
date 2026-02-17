package com.cvc953.localplayer.ui

import android.R
import android.app.Activity
import android.content.Intent
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val folderEntries by viewModel.folderEntries.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            viewModel.addMusicFolder(uri.toString())
            Toast.makeText(context, "Carpeta añadida", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        onClose()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Ajustes", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))



            // Theme selector
            val theme by viewModel.themeMode.collectAsState()
            val themeOptions = listOf("system", "light", "dark")
            var expanded by remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tema", color = Color.White)
                    Text("Selecciona tema de la aplicación", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Box {
                    Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) {
                        Text(theme.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        themeOptions.forEach { t ->
                            DropdownMenuItem(text = { Text(t.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) }, onClick = {
                                viewModel.setThemeMode(t)
                                expanded = false
                            })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            // Auto-scan option
            val autoScan by viewModel.autoScanEnabled.collectAsState()
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Escaneo automático", color = Color.White)
                    Text("Detectar cambios y escanear automáticamente", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Switch(
                    checked = autoScan,
                    onCheckedChange = { viewModel.toggleAutoScan(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF2196F3),
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Ecualizador", color = Color.White, fontWeight = FontWeight.SemiBold)
                    // Equalizer toggle
                    Spacer(modifier = Modifier.height(8.dp))
                    val eqEnabled by viewModel.equalizerEnabled.collectAsState()
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Activar ecualizador", color = Color.White)
                            Text("Usar ecualizador nativo del sistema", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        }
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = { viewModel.toggleEqualizer(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF2196F3),
                                checkedTrackColor = Color.White,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.White.copy(alpha = 0.12f)
                            )
                        )
                    }

                    // Open detailed equalizer screen for vertical sliders
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.openEqualizerScreen() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) {
                        Text("Abrir ecualizador avanzado")
                    }



            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = { launcher.launch(null) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) {
                Icon(Icons.Default.Folder, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar carpeta")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Carpetas configuradas:", color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            if (folderEntries.isEmpty()) {
                Text("Ninguna carpeta configurada", color = Color.White.copy(alpha = 0.7f))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(folderEntries) { entry ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.name, color = Color.White)
                                Text("${entry.count} canciones", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                            }
                            IconButton(onClick = {
                                viewModel.removeMusicFolder(entry.uri)
                                Toast.makeText(context, "Carpeta eliminada", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.White)
                            }
                        }
                    }
                }
            }

        }
    }
}
