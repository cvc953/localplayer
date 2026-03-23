@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.cvc953.localplayer.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.viewmodel.EqualizerViewModel
import com.cvc953.localplayer.viewmodel.FolderEntry
import com.cvc953.localplayer.viewmodel.FolderViewModel
import com.cvc953.localplayer.viewmodel.MainViewModel

@Suppress("ktlint:standard:function-naming")
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    equalizerViewModel: EqualizerViewModel,
    folderViewModel: FolderViewModel,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val folderEntries by folderViewModel.folderEntries.collectAsState()
    val theme by viewModel.themeMode.collectAsState()
    val autoScan by viewModel.autoScanEnabled.collectAsState()
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsState()
    val eqEnabled by equalizerViewModel.equalizerEnabled.collectAsState()

    val themeOptions = listOf("sistema", "claro", "oscuro")
    var themeExpanded by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderEntry?>(null) }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                } catch (_: Exception) {
                }
                folderViewModel.addMusicFolder(uri.toString())
                Toast.makeText(context, "Carpeta añadida", Toast.LENGTH_SHORT).show()
            }
        }

    BackHandler(onBack = onClose)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Ajustes",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Personaliza la app y tu biblioteca",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = "Apariencia y comportamiento",
                    subtitle = "Opciones globales de visualización y escaneo",
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Tema", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Selecciona tema de la aplicación",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Box {
                            OutlinedButton(onClick = { themeExpanded = true }) {
                                Text(theme.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })
                            }
                            DropdownMenu(
                                expanded = themeExpanded,
                                onDismissRequest = { themeExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                themeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                option.replaceFirstChar {
                                                    if (it.isLowerCase()) it.titlecase() else it.toString()
                                                },
                                            )
                                        },
                                        onClick = {
                                            viewModel.setThemeMode(option)
                                            themeExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Color dinámico", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Usar color de la carátula en el reproductor",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { viewModel.toggleDynamicColor(it) },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                ),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Escaneo automático", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Detectar cambios y escanear automáticamente",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = autoScan,
                            onCheckedChange = { viewModel.toggleAutoScan(it) },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                ),
                        )
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = "Audio",
                    subtitle = "Control del ecualizador y procesamiento de sonido",
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Activar ecualizador", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Usar ecualizador nativo del sistema",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = eqEnabled,
                            onCheckedChange = { equalizerViewModel.toggleEqualizer(it) },
                            colors =
                                SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                ),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { equalizerViewModel.openEqualizerScreen() },
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Abrir ecualizador avanzado")
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = "Biblioteca",
                    subtitle = "Gestiona las carpetas que se incluyen en la música",
                ) {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { launcher.launch(null) },
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Agregar carpeta")
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Carpetas configuradas", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    if (folderEntries.isEmpty()) {
                        Text("Ninguna carpeta configurada", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        folderEntries.forEachIndexed { index, entry ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        "${entry.count} canciones",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                    )
                                }
                                IconButton(onClick = { folderToDelete = entry }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        folderToDelete?.let { entry ->
            AlertDialog(
                onDismissRequest = { folderToDelete = null },
                title = { Text("Eliminar carpeta") },
                text = {
                    Text("¿Eliminar la carpeta \"${entry.name}\"?\n\nLas ${entry.count} canciones de esta carpeta ya no se mostrarán en la app.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            folderViewModel.removeMusicFolder(entry.uri)
                            Toast.makeText(context, "Carpeta eliminada", Toast.LENGTH_SHORT).show()
                            folderToDelete = null
                        },
                    ) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToDelete = null }) {
                        Text("Cancelar")
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun SettingsSectionCard(
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
