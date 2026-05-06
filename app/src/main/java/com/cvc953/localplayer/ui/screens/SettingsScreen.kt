package com.cvc953.localplayer.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.ui.theme.LocalExtendedColors
import com.cvc953.localplayer.ui.theme.predefinedThemeColors
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
    val primaryColorHex by viewModel.primaryColorHex.collectAsState()
    val eqEnabled by equalizerViewModel.equalizerEnabled.collectAsState()

    val themeOptions =
        listOf(
            "sistema" to stringResource(id = R.string.theme_system),
            "claro" to stringResource(id = R.string.theme_light),
            "oscuro" to stringResource(id = R.string.theme_dark),
        )
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
                Toast.makeText(context, context.getString(R.string.toast_folder_added), Toast.LENGTH_SHORT).show()
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(id = R.string.settings_title),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(id = R.string.settings_subtitle),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.action_close),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = stringResource(id = R.string.settings_section_appearance_title),
                    subtitle = stringResource(id = R.string.settings_section_appearance_subtitle),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_theme_label), color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(id = R.string.settings_theme_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Box {
                            OutlinedButton(onClick = { themeExpanded = true }) {
                                Text(
                                    themeOptions.find { it.first == theme }?.second?.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase() else it.toString()
                                    } ?: theme,
                                )
                            }
                            DropdownMenu(
                                expanded = themeExpanded,
                                onDismissRequest = { themeExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                themeOptions.forEach { (optionKey, optionName) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                optionName.replaceFirstChar {
                                                    if (it.isLowerCase()) it.titlecase() else it.toString()
                                                },
                                            )
                                        },
                                        onClick = {
                                            viewModel.setThemeMode(optionKey)
                                            themeExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_accent_color_label), color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(id = R.string.settings_accent_color_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(predefinedThemeColors) { themeColor ->
                            val isSelected = themeColor.hex == primaryColorHex
                            val borderColor =
                                if (isSelected) themeColor.color else Color.Transparent
                            val borderWidth = if (isSelected) 3.dp else 0.dp

                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .then(
                                            if (borderWidth > 0.dp) {
                                                Modifier.Companion.border(
                                                    borderWidth,
                                                    borderColor,
                                                    CircleShape,
                                                )
                                            } else {
                                                Modifier
                                            },
                                        ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    color = themeColor.color,
                                    onClick = { viewModel.setPrimaryColor(themeColor.hex) },
                                ) {
                                    if (isSelected) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = themeColor.name,
                                                tint = themeColor.onColor,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_dynamic_color_label), color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(id = R.string.settings_dynamic_color_description),
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
                                    uncheckedTrackColor =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.16f,
                                        ),
                                ),
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )
                }
            }

            item {
                SettingsSectionCard(
                    title = stringResource(id = R.string.settings_section_audio_title),
                    subtitle = stringResource(id = R.string.settings_section_audio_subtitle),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_equalizer_enable_label), color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(id = R.string.settings_equalizer_enable_description),
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
                                    uncheckedTrackColor =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.16f,
                                        ),
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
                        Text(stringResource(id = R.string.action_open_equalizer))
                    }
                }
            }

            item {
                SettingsSectionCard(
                    title = stringResource(id = R.string.settings_section_library_title),
                    subtitle = stringResource(id = R.string.settings_section_library_subtitle),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_auto_scan_label), color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(id = R.string.settings_auto_scan_description),
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
                                    uncheckedTrackColor =
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.16f,
                                        ),
                                ),
                        )
                    }

                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { launcher.launch(null) },
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.action_add_folder))
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        stringResource(id = R.string.settings_folders_configured_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (folderEntries.isEmpty()) {
                        Text(
                            stringResource(id = R.string.settings_folders_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                                        stringResource(id = R.string.songs_count, entry.count),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                    )
                                }
                                IconButton(onClick = { folderToDelete = entry }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(id = R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
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
                title = { Text(stringResource(id = R.string.dialog_delete_folder_title)) },
                text = {
                    Text(
                        stringResource(id = R.string.dialog_delete_folder_message, entry.name, entry.count),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            folderViewModel.removeMusicFolder(entry.uri)
                            Toast.makeText(context, context.getString(R.string.toast_folder_deleted), Toast.LENGTH_SHORT).show()
                            folderToDelete = null
                        },
                    ) {
                        Text(stringResource(id = R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { folderToDelete = null }) {
                        Text(stringResource(id = R.string.action_cancel))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                textContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Suppress("ktlint:standard:function-naming")
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
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(subtitle, color = LocalExtendedColors.current.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
