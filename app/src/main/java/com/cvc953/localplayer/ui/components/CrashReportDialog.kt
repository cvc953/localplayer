package com.cvc953.localplayer.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.R
import com.cvc953.localplayer.ui.extendedColors
import com.cvc953.localplayer.util.CrashReporter

/**
 * Muestra el último crash registrado y permite copiarlo al portapapeles.
 *
 * El reporte nunca se envía solo: el usuario decide dónde pegarlo. Se borra después de
 * copiarlo o de descartarlo, para que no vuelva a aparecer en cada arranque.
 */
@Suppress("ktlint:standard:function-naming")
@Composable
fun CrashReportDialog() {
    val context = LocalContext.current
    var report by remember { mutableStateOf(CrashReporter.pendingReports(context).firstOrNull()) }
    val current = report ?: return
    val reportText = remember(current) { CrashReporter.read(current) }

    AlertDialog(
        onDismissRequest = { report = null },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                stringResource(R.string.crash_report_title),
                color = MaterialTheme.colorScheme.onBackground,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.crash_report_body),
                    color = MaterialTheme.extendedColors.textSecondary,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = reportText,
                    color = MaterialTheme.extendedColors.textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("LocalPlayer crash", reportText))
                    CrashReporter.delete(current)
                    report = null
                    Toast.makeText(context, R.string.crash_report_copied, Toast.LENGTH_LONG).show()
                },
            ) {
                Text(
                    stringResource(R.string.crash_report_copy),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    CrashReporter.delete(current)
                    report = null
                },
            ) {
                Text(
                    stringResource(R.string.crash_report_discard),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
        },
    )
}
