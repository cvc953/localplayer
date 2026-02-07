package com.cvc953.localplayer.util

import com.cvc953.localplayer.model.TtmlLyrics

data class LrcLine(
    val timeMs: Long,
    val text: String
)

/**
 * Detecta si el contenido es TTML o LRC y parsea apropiadamente
 */
fun isTtml(content: String): Boolean {
    return content.trimStart().startsWith("<?xml") || content.contains("<tt ")
}

fun parseLrc(content: String): List<LrcLine> {
    val regex = Regex("""\[(\d+):(\d+)\.(\d+)](.*)""")

    return content.lines().mapNotNull { line ->
        val match = regex.find(line) ?: return@mapNotNull null

        val min = match.groupValues[1].toLong()
        val sec = match.groupValues[2].toLong()
        val ms  = match.groupValues[3].toLong() * 10

        LrcLine(
            timeMs = min * 60_000 + sec * 1_000 + ms,
            text = match.groupValues[4]
        )
    }
}

/**
 * Convierte letras TTML a formato LrcLine simple para compatibilidad
 */
fun TtmlLyrics.toLrcLines(): List<LrcLine> {
    return lines.map { line ->
        LrcLine(
            timeMs = line.timeMs,
            text = line.text
        )
    }
}
