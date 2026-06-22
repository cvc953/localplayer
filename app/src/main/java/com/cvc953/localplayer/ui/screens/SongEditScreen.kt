package com.cvc953.localplayer.ui.screens

import android.app.Activity
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.viewmodel.SongEditViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongEditScreen(
    viewModel: SongEditViewModel,
    onNavigateBack: () -> Unit,
) {
    val formState by viewModel.formState.collectAsState()
    val coverState by viewModel.coverArtState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val context = LocalContext.current
    var pendingSave by remember { mutableStateOf(false) }

    val writeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.save()
        }
        pendingSave = false
    }

    val coverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { viewModel.onCoverSelected(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.saveComplete.collect { onNavigateBack() }
    }

    LaunchedEffect(Unit) {
        viewModel.saveError.collect { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_song_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                val songUri = viewModel.getSongUri()
                                if (songUri != null) {
                                    try {
                                        val pendingIntent = MediaStore.createWriteRequest(
                                            context.contentResolver,
                                            listOf(songUri),
                                        )
                                        pendingSave = true
                                        writeLauncher.launch(
                                            IntentSenderRequest.Builder(pendingIntent).build(),
                                        )
                                    } catch (_: Exception) {
                                        viewModel.save()
                                    }
                                } else {
                                    viewModel.save()
                                }
                            } else {
                                viewModel.save()
                            }
                        },
                        enabled = !isSaving && !pendingSave,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.action_save))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Cover art preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.DarkGray.copy(alpha = 0.3f))
                    .clickable { coverLauncher.launch("image/*") },
            ) {
                if (coverState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (coverState.currentBitmap != null) {
                    Image(
                        bitmap = coverState.currentBitmap!!.asImageBitmap(),
                        contentDescription = stringResource(R.string.cover_art_change),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    // Overlay inferior con icono de editar
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable(enabled = false) { }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(R.string.cover_art_change),
                                color = Color.White,
                                fontSize = 13.sp,
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = stringResource(R.string.cover_art_change),
                            color = Color.Gray,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = stringResource(R.string.cover_art_tap_hint),
                            color = Color.Gray.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                        )
                    }
                }
            }

            // Form fields
            OutlinedTextField(
                value = formState.title,
                onValueChange = { viewModel.onFieldChanged("title", it) },
                label = { Text(stringResource(R.string.field_title)) },
                isError = formState.titleError != null,
                supportingText = formState.titleError?.let { { Text(it) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = formState.artist,
                    onValueChange = { viewModel.onFieldChanged("artist", it) },
                    label = { Text(stringResource(R.string.field_artist)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = formState.album,
                    onValueChange = { viewModel.onFieldChanged("album", it) },
                    label = { Text(stringResource(R.string.field_album)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = formState.genre,
                    onValueChange = { viewModel.onFieldChanged("genre", it) },
                    label = { Text(stringResource(R.string.field_genre)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = formState.year,
                    onValueChange = { viewModel.onFieldChanged("year", it) },
                    label = { Text(stringResource(R.string.field_year)) },
                    isError = formState.yearError != null,
                    supportingText = formState.yearError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    modifier = Modifier.width(120.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = formState.trackNumber,
                    onValueChange = { viewModel.onFieldChanged("trackNumber", it) },
                    label = { Text(stringResource(R.string.field_track_number)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    modifier = Modifier.width(120.dp),
                )
                OutlinedTextField(
                    value = formState.discNumber,
                    onValueChange = { viewModel.onFieldChanged("discNumber", it) },
                    label = { Text(stringResource(R.string.field_disc_number)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                    modifier = Modifier.width(120.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
