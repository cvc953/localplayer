package com.cvc953.localplayer.util

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * El núcleo del reporte de crashes trabaja sobre un directorio, así que se prueba en la JVM
 * sin Android: formato, escritura, retención y orden.
 */
class CrashReporterTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun reportIncludesTypeMessageThreadAndDevice() {
        val error = IllegalStateException("boom")
        val thread = Thread({}, "crash-test-thread")

        val report = formatReport(
            thread = thread,
            error = error,
            device = "Test Device · Android 10 (API 29)",
            timestamp = 1_700_000_000_000L,
        )

        assertTrue(report.contains("IllegalStateException"))
        assertTrue(report.contains("boom"))
        assertTrue(report.contains("Test Device · Android 10 (API 29)"))
        assertTrue(report.contains("crash-test-thread"))
        // Al menos un frame del stack, con archivo y línea.
        assertTrue(report.contains("CrashReporterTest"))
    }

    @Test
    fun savingWritesOneFilePerCrash() {
        val dir = folder.newFolder("reports")

        val first = saveReport(dir, Thread.currentThread(), IllegalStateException("one"), "Device", 1_700_000_000_000L)
        val second = saveReport(dir, Thread.currentThread(), IllegalStateException("two"), "Device", 1_700_000_000_001L)

        val files = dir.listFiles()?.toList().orEmpty()
        assertEquals(2, files.size)
        assertTrue(files.any { it.name == first?.name })
        assertTrue(files.any { it.name == second?.name })
    }

    @Test
    fun retentionKeepsTheNewestReports() {
        val dir = folder.newFolder("reports")
        val base = 1_700_000_000_000L
        repeat(7) { index ->
            saveReport(dir, Thread.currentThread(), IllegalStateException("crash $index"), "Device", base + index)
        }

        val kept = pendingReportsIn(dir)

        assertEquals(MAX_REPORTS, kept.size)
        // Se descartan los dos más viejos.
        assertTrue(kept.none { it.name.endsWith("${base}.txt") })
        assertTrue(kept.none { it.name.endsWith("${base + 1}.txt") })
        assertTrue(kept.any { it.name.endsWith("${base + 6}.txt") })
    }

    @Test
    fun pendingReportsAreNewestFirst() {
        val dir = folder.newFolder("reports")
        val base = 1_700_000_000_000L
        listOf(base + 1, base + 3, base + 2).forEach { timestamp ->
            saveReport(dir, Thread.currentThread(), IllegalStateException("crash $timestamp"), "Device", timestamp)
        }

        val names = pendingReportsIn(dir).map { it.name }

        assertEquals(3, names.size)
        assertTrue(names[0].endsWith("${base + 3}.txt"))
        assertTrue(names[1].endsWith("${base + 2}.txt"))
        assertTrue(names[2].endsWith("${base + 1}.txt"))
    }

    @Test
    fun readingAndDeletingARecord() {
        val dir = folder.newFolder("reports")
        val file = saveReport(dir, Thread.currentThread(), IllegalStateException("bye"), "Device", 1_700_000_000_000L)!!

        assertTrue(readReport(file).contains("bye"))

        deleteReport(file)

        assertEquals(0, dir.listFiles()?.size ?: 0)
    }
}
