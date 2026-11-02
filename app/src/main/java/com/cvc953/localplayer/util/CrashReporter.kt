package com.cvc953.localplayer.util

import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/** Cuántos reportes se conservan; un crash loop no debe llenar el disco. */
internal const val MAX_REPORTS = 5

private const val REPORTS_DIRECTORY = "crash-reports"
private const val TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"

/**
 * Guarda el stack trace de un crash y lo deja disponible para mostrarlo en el próximo arranque.
 *
 * Sin esto, un crash sólo existe en logcat: el usuario no puede reportarlo y el mantenedor no
 * puede reproducirlo. El reporte se escribe en almacenamiento interno (sin permisos) y nunca
 * se envía a ningún lado; el usuario decide qué hacer con el texto.
 */
object CrashReporter {
    @Volatile
    private var installed = false

    /**
     * Engancha el handler de excepciones no capturadas. Escribe el reporte y después delega en
     * el handler anterior: el proceso tiene que morir igual, con el diálogo del sistema.
     */
    fun install(context: Context) {
        if (installed) return
        installed = true

        val appContext = context.applicationContext ?: context
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { save(appContext, thread, error) }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                // Sin handler previo hay que replicar lo que hace el framework: dejar el
                // rastro en logcat y matar el proceso.
                Log.e("CrashReporter", "FATAL EXCEPTION: ${thread.name}", error)
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun save(
        context: Context,
        thread: Thread,
        error: Throwable,
    ): File? =
        saveReport(
            directory = reportsDirectory(context),
            thread = thread,
            error = error,
            device = deviceSummary(),
            timestamp = System.currentTimeMillis(),
        )

    /** Reportes pendientes, el más nuevo primero. */
    fun pendingReports(context: Context): List<File> = pendingReportsIn(reportsDirectory(context))

    fun read(file: File): String = readReport(file)

    fun delete(file: File) = deleteReport(file)
}

private fun reportsDirectory(context: Context): File = File(context.filesDir, REPORTS_DIRECTORY)

/**
 * Arma el texto del reporte. Separado de Android para poder probarlo en la JVM: [device] y
 * [timestamp] entran como parámetros.
 */
internal fun formatReport(
    thread: Thread,
    error: Throwable,
    device: String,
    timestamp: Long,
): String {
    val stackTrace = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()

    return buildString {
        appendLine("LocalPlayer crash report")
        appendLine("Timestamp: ${formatTimestamp(timestamp)}")
        appendLine("Device: $device")
        appendLine("Thread: ${thread.name}")
        appendLine("Exception: ${error.javaClass.name}${error.message?.let { ": $it" }.orEmpty()}")
        appendLine()
        append(stackTrace)
    }
}

/** Escribe el reporte en [directory] y aplica la retención. Devuelve el archivo o `null`. */
internal fun saveReport(
    directory: File,
    thread: Thread,
    error: Throwable,
    device: String,
    timestamp: Long,
): File? =
    runCatching {
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, "crash-$timestamp.txt")
        file.writeText(formatReport(thread, error, device, timestamp))
        trimReports(directory)
        file
    }.getOrNull()

/**
 * Reportes del directorio, el más nuevo primero.
 *
 * Ordena por nombre porque el nombre lleva el timestamp en milisegundos: no depende de
 * `lastModified`, que algunos sistemas de archivos redondean.
 */
internal fun pendingReportsIn(directory: File): List<File> {
    val reports = directory.listFiles { file -> file.isFile && file.name.endsWith(".txt") }
    return (reports ?: emptyArray()).sortedByDescending { it.name }
}

/** Deja sólo los [MAX_REPORTS] más nuevos. */
internal fun trimReports(directory: File) {
    val reports = pendingReportsIn(directory)
    if (reports.size <= MAX_REPORTS) return
    reports.drop(MAX_REPORTS).forEach { stale -> runCatching { stale.delete() } }
}

internal fun readReport(file: File): String = runCatching { file.readText() }.getOrDefault("")

internal fun deleteReport(file: File) {
    runCatching { file.delete() }
}

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(Date(timestamp))

private fun deviceSummary(): String =
    runCatching {
        "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }.getOrNull() ?: "unknown device"
