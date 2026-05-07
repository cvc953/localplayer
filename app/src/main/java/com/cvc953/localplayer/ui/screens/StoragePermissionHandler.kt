package com.cvc953.localplayer.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.cvc953.localplayer.R
import com.cvc953.localplayer.ui.extendedColors

@Composable
fun StoragePermissionHandler(
    isFolderConfiguredInitially: Boolean,
    onFolderSelected: (String) -> Unit,
    onSetupCompleted: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val permission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val hasPermission =
        rememberSaveable {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED,
            )
        }
    val hasFolderConfigured = rememberSaveable { mutableStateOf(isFolderConfiguredInitially) }
    val setupWasInitiallyCompleted =
        rememberSaveable {
            mutableStateOf(hasPermission.value && hasFolderConfigured.value)
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission.value = granted
            if (!granted) {
                Toast.makeText(context, context.getString(R.string.storage_toast_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

    val folderLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    onFolderSelected(uri.toString())
                    hasFolderConfigured.value = true
                    Toast.makeText(context, context.getString(R.string.storage_toast_folder_selected), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.w("StoragePermissionHandler", context.getString(R.string.storage_log_permission_error), e)
                    Toast.makeText(context, context.getString(R.string.storage_toast_permission_error), Toast.LENGTH_SHORT).show()
                }
            }
        }

    LaunchedEffect(hasPermission.value, hasFolderConfigured.value) {
        val setupCompleted = hasPermission.value && hasFolderConfigured.value
        if (setupCompleted && !setupWasInitiallyCompleted.value) {
            setupWasInitiallyCompleted.value = true
            onSetupCompleted()
        }
    }

    if (hasPermission.value && hasFolderConfigured.value) {
        content()
        return
    }

    val scrollState = rememberScrollState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
            Text(
                text = stringResource(R.string.storage_setup_title),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.storage_setup_subtitle),
                color = MaterialTheme.extendedColors.textSecondary,
                fontSize = 13.sp,
            )

            SetupSectionCard(
                title = stringResource(R.string.storage_permission_section_title),
                subtitle = stringResource(R.string.storage_permission_section_subtitle),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasPermission.value) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (hasPermission.value) MaterialTheme.colorScheme.primary else MaterialTheme.extendedColors.textSecondary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (hasPermission.value) stringResource(R.string.storage_permission_granted) else stringResource(R.string.storage_permission_pending),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!hasPermission.value) {
                            permissionLauncher.launch(permission)
                        }
                    },
                    enabled = !hasPermission.value,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.extendedColors.textSecondary,
                        ),
                ) {
                    Text(if (hasPermission.value) stringResource(R.string.storage_permission_active) else stringResource(R.string.storage_permission_grant))
                }
            }

            SetupSectionCard(
                title = stringResource(R.string.storage_folder_section_title),
                subtitle = stringResource(R.string.storage_folder_section_subtitle),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasFolderConfigured.value) Icons.Default.CheckCircle else Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (hasFolderConfigured.value) MaterialTheme.colorScheme.primary else MaterialTheme.extendedColors.textSecondary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (hasFolderConfigured.value) stringResource(R.string.storage_folder_configured) else stringResource(R.string.storage_folder_pending),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { folderLauncher.launch(null) },
                    enabled = hasPermission.value,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.extendedColors.textSecondary,
                        ),
                ) {
                    Text(stringResource(R.string.storage_folder_choose))
                }
                if (!hasPermission.value) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.storage_folder_permission_required),
                        color = MaterialTheme.extendedColors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.extendedColors.surfaceSheet),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.extendedColors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StoragePermissionHandler(
    isFolderConfiguredInitially: Boolean,
    onFolderSelected: (String) -> Unit,
    onSetupCompleted: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val permission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    val hasPermission =
        rememberSaveable {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED,
            )
        }
    val hasFolderConfigured = rememberSaveable { mutableStateOf(isFolderConfiguredInitially) }
    val setupWasInitiallyCompleted =
        rememberSaveable {
            mutableStateOf(hasPermission.value && hasFolderConfigured.value)
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { granted ->
            hasPermission.value = granted
            if (!granted) {
                Toast.makeText(context, "Se necesita acceso a la musica", Toast.LENGTH_SHORT).show()
            }
        }

    val folderLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                    onFolderSelected(uri.toString())
                    hasFolderConfigured.value = true
                    Toast.makeText(context, "Carpeta seleccionada", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.w("StoragePermissionHandler", "No se pudo guardar permiso persistente", e)
                    Toast.makeText(context, "No se pudo guardar acceso a la carpeta", Toast.LENGTH_SHORT).show()
                }
            }
        }

    LaunchedEffect(hasPermission.value, hasFolderConfigured.value) {
        val setupCompleted = hasPermission.value && hasFolderConfigured.value
        if (setupCompleted && !setupWasInitiallyCompleted.value) {
            setupWasInitiallyCompleted.value = true
            onSetupCompleted()
        }
    }

    if (hasPermission.value && hasFolderConfigured.value) {
        content()
        return
    }

    val scrollState = rememberScrollState()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
            Text(
                text = "Configurar biblioteca",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Un paso rapido para acceder a tu musica local",
                color = MaterialTheme.extendedColors.textSecondary,
                fontSize = 13.sp,
            )

            SetupSectionCard(
                title = "1. Permiso de almacenamiento",
                subtitle = "Necesario para leer tus archivos de audio",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasPermission.value) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (hasPermission.value) MaterialTheme.colorScheme.primary else MaterialTheme.extendedColors.textSecondary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (hasPermission.value) "Permiso concedido" else "Permiso pendiente",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        if (!hasPermission.value) {
                            permissionLauncher.launch(permission)
                        }
                    },
                    enabled = !hasPermission.value,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.extendedColors.textSecondary,
                        ),
                ) {
                    Text(if (hasPermission.value) "Permiso activo" else "Dar permiso")
                }
            }

            SetupSectionCard(
                title = "2. Carpeta de musica",
                subtitle = "Selecciona donde guardas tus canciones",
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasFolderConfigured.value) Icons.Default.CheckCircle else Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (hasFolderConfigured.value) MaterialTheme.colorScheme.primary else MaterialTheme.extendedColors.textSecondary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (hasFolderConfigured.value) "Carpeta configurada" else "Carpeta pendiente",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { folderLauncher.launch(null) },
                    enabled = hasPermission.value,
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.extendedColors.textSecondary,
                        ),
                ) {
                    Text("Elegir carpeta")
                }
                if (!hasPermission.value) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Primero debes conceder el permiso de almacenamiento.",
                        color = MaterialTheme.extendedColors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupSectionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.extendedColors.surfaceSheet),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.extendedColors.textSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
