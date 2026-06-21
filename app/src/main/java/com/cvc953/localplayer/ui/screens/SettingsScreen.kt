package com.cvc953.localplayer.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
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
    val language by viewModel.language.collectAsState()
    val autoScan by viewModel.autoScanEnabled.collectAsState()
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsState()
    val primaryColorHex by viewModel.primaryColorHex.collectAsState()
    val eqEnabled by equalizerViewModel.equalizerEnabled.collectAsState()
    val albumArtShapeKey by viewModel.albumArtShape.collectAsState()
    val progressBarStyleKey by viewModel.progressBarStyle.collectAsState()
    val transportStyleKey by viewModel.transportStyle.collectAsState()
    val playPauseStyleKey by viewModel.playPauseStyle.collectAsState()
    val showAudioInfo by viewModel.showAudioInfo.collectAsState()
    val genresTabEnabled by viewModel.genresTabEnabled.collectAsState()
    val defaultStartTab by viewModel.defaultStartTab.collectAsState()

    var showColorPicker by remember { mutableStateOf(false) }

    val themeOptions =
        listOf(
            "sistema" to stringResource(id = R.string.theme_system),
            "claro" to stringResource(id = R.string.theme_light),
            "oscuro" to stringResource(id = R.string.theme_dark),
        )
    val languageOptions =
        listOf(
            "sistema" to stringResource(id = R.string.language_system_label),
            "es" to stringResource(id = R.string.language_spanish),
            "en" to stringResource(id = R.string.language_english),
            "it" to stringResource(id = R.string.language_italian),
        )
    var themeExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }
    var defaultTabExpanded by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<FolderEntry?>(null) }

    val defaultTabOptions =
        listOf(
            "songs" to stringResource(id = R.string.songs_title),
            "albums" to stringResource(id = R.string.albums_title),
            "artists" to stringResource(id = R.string.artists_title),
            "playlists" to stringResource(id = R.string.playlists_title),
            "genres" to stringResource(id = R.string.genres_title),
        )

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
                    title = stringResource(id = R.string.settings_section_language_title),
                    subtitle = stringResource(id = R.string.settings_section_language_subtitle),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(id = R.string.settings_language_label), color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(id = R.string.settings_language_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Box {
                            OutlinedButton(onClick = { languageExpanded = true }) {
                                Text(
                                    languageOptions.find { it.first == language }?.second?.replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase() else it.toString()
                                    } ?: language,
                                )
                            }
                            DropdownMenu(
                                expanded = languageExpanded,
                                onDismissRequest = { languageExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                languageOptions.forEach { (optionKey, optionName) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                optionName.replaceFirstChar {
                                                    if (it.isLowerCase()) it.titlecase() else it.toString()
                                                },
                                            )
                                        },
                                        onClick = {
                                            android.util.Log.d("SettingsScreen", "Language selected: $optionKey")

                                            // Update preference
                                            viewModel.setLanguage(optionKey)
                                            languageExpanded = false

                                            // Apply locale to resources
                                            val appPrefs =
                                                com.cvc953.localplayer.preferences
                                                    .AppPrefs(context)
                                            val languageCode = appPrefs.getLanguage()
                                            if (languageCode != "sistema") {
                                                val locale =
                                                    when (languageCode) {
                                                        "es" -> java.util.Locale("es")
                                                        "en" -> java.util.Locale("en")
                                                        "it" -> java.util.Locale("it")
                                                        else -> java.util.Locale.getDefault()
                                                    }
                                                val config = android.content.res.Configuration(context.resources.configuration)
                                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                    config.setLocale(locale)
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    config.locale = locale
                                                }
                                                context.resources.updateConfiguration(config, context.resources.displayMetrics)
                                            }
                                        },
                                    )
                                }
                            }
                        }
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
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

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
                        item {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                onClick = { showColorPicker = true },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Personalizar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }

                    if (showColorPicker) {
                        ColorPickerDialog(
                            initialHex = primaryColorHex,
                            onColorSelected = { hex ->
                                viewModel.setPrimaryColor(hex)
                                showColorPicker = false
                            },
                            onDismiss = { showColorPicker = false },
                        )
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

                    // --- Album Art Shape ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(id = R.string.settings_album_art_shape_label),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val albumArtOptions =
                        listOf(
                            "rounded_8" to stringResource(id = R.string.album_art_rounded_8),
                            "rounded_22" to stringResource(id = R.string.album_art_rounded_22),
                            "circle" to stringResource(id = R.string.album_art_circle),
                        )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        albumArtOptions.forEach { (key, label) ->
                            val isSelected = albumArtShapeKey == key
                            OutlinedButton(
                                onClick = { viewModel.setAlbumArtShape(key) },
                                modifier = Modifier.weight(1f),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.15f,
                                                )
                                            } else {
                                                Color.Transparent
                                            },
                                    ),
                                border =
                                    if (isSelected) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    },
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    // --- Progress Bar Style ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(id = R.string.settings_progress_bar_label),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val progressOptions =
                        listOf(
                            "material" to stringResource(id = R.string.progress_bar_material),
                            "wavy" to stringResource(id = R.string.progress_bar_wavy),
                            "squiggly" to stringResource(id = R.string.progress_bar_squiggly),
                        )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        progressOptions.forEach { (key, label) ->
                            val isSelected = progressBarStyleKey == key
                            OutlinedButton(
                                onClick = { viewModel.setProgressBarStyle(key) },
                                modifier = Modifier.weight(1f),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.15f,
                                                )
                                            } else {
                                                Color.Transparent
                                            },
                                    ),
                                border =
                                    if (isSelected) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    },
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    // --- Transport Style ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(id = R.string.settings_transport_label),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val transportOptions =
                        listOf(
                            "default" to stringResource(id = R.string.transport_default),
                            "lune" to stringResource(id = R.string.transport_lune),
                        )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        transportOptions.forEach { (key, label) ->
                            val isSelected = transportStyleKey == key
                            OutlinedButton(
                                onClick = { viewModel.setTransportStyle(key) },
                                modifier = Modifier.weight(1f),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.15f,
                                                )
                                            } else {
                                                Color.Transparent
                                            },
                                    ),
                                border =
                                    if (isSelected) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    },
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    // --- PlayPause Style ---
                    val isTransportLune = transportStyleKey == "lune"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(id = R.string.settings_playpause_label),
                                color =
                                    if (isTransportLune) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val playPauseOptions =
                        listOf(
                            "filled_circle" to stringResource(id = R.string.playpause_filled),
                            "outlined_circle" to stringResource(id = R.string.playpause_outlined),
                        )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        playPauseOptions.forEach { (key, label) ->
                            val isSelected = playPauseStyleKey == key
                            OutlinedButton(
                                onClick = { viewModel.setPlayPauseStyle(key) },
                                enabled = !isTransportLune,
                                modifier = Modifier.weight(1f),
                                colors =
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor =
                                            if (isSelected) {
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.15f,
                                                )
                                            } else {
                                                Color.Transparent
                                            },
                                    ),
                                border =
                                    if (isSelected) {
                                        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                    } else {
                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    },
                            ) {
                                Text(
                                    label,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 10.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    // --- Show Audio Info ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(id = R.string.settings_audio_info_label),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Switch(
                            checked = showAudioInfo,
                            onCheckedChange = { viewModel.setShowAudioInfo(it) },
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
                            Text(stringResource(id = R.string.settings_default_tab_label), color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                stringResource(id = R.string.settings_default_tab_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Box {
                            OutlinedButton(onClick = { defaultTabExpanded = true }) {
                                Text(
                                    defaultTabOptions.find { it.first == defaultStartTab }?.second
                                        ?: stringResource(id = R.string.songs_title),
                                )
                            }
                            DropdownMenu(
                                expanded = defaultTabExpanded,
                                onDismissRequest = { defaultTabExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                defaultTabOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            viewModel.setDefaultStartTab(key)
                                            defaultTabExpanded = false
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
                            Text(
                                stringResource(id = R.string.settings_genres_tab_label),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                stringResource(id = R.string.settings_genres_tab_description),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = genresTabEnabled,
                            onCheckedChange = { viewModel.setGenresTabEnabled(it) },
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
private fun ColorPickerDialog(
    initialHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // Convertir el color inicial a HSV
    val initialColor = try {
        android.graphics.Color.parseColor(initialHex)
    } catch (_: Exception) {
        android.graphics.Color.parseColor("#2196F3")
    }
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor, hsv)

    var hue by remember { mutableStateOf(hsv[0]) }
    var saturation by remember { mutableStateOf(hsv[1] * 100f) }
    var value by remember { mutableStateOf(hsv[2] * 100f) }

    fun currentColor(): Color {
        val c = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation / 100f, value / 100f))
        return Color(c)
    }

    fun currentHex(): String {
        val c = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation / 100f, value / 100f))
        return String.format("#%06X", 0xFFFFFF and c)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Color personalizado")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Preview del color actual
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(MaterialTheme.shapes.medium),
                    color = currentColor(),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = currentHex(),
                            color = if (currentColor().red * 0.299f + currentColor().green * 0.587f + currentColor().blue * 0.114f > 0.5f) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Hue Slider
                Text("Matiz", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                // Saturation Slider
                Text("Saturación", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )

                // Value Slider
                Text("Brillo", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentHex()) }) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.action_cancel))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    )
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
