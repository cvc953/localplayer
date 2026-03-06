package com.cvc953.localplayer.util

import com.cvc953.localplayer.model.TtmlLyrics

data class LrcLine(
    val timeMs: Long,
    val text: String,
    val isSecondaryVoice: Boolean = false,
)

/**
 * Detecta si el contenido es TTML o LRC y parsea apropiadamente
 */
fun isTtml(content: String): Boolean = content.trimStart().startsWith("<?xml") || content.contains("<tt ")

fun parseLrc(content: String): List<LrcLine> {
    val regex = Regex("""\[(\d+):(\d+)\.(\d+)](.*)""")
    val secondaryVoiceRegex = Regex("""^\s*\((.*)\)\s*$""")

    return content.lines().mapNotNull { line ->
        val match = regex.find(line) ?: return@mapNotNull null

        val min = match.groupValues[1].toLong()
        val sec = match.groupValues[2].toLong()
        val ms = match.groupValues[3].toLong() * 10
        val rawText = match.groupValues[4]
        val secondaryMatch = secondaryVoiceRegex.matchEntire(rawText)
        val isSecondaryVoice = secondaryMatch != null
        val normalizedText = (secondaryMatch?.groupValues?.get(1) ?: rawText).trim()

        LrcLine(
            timeMs = min * 60_000 + sec * 1_000 + ms,
            text = normalizedText,
            isSecondaryVoice = isSecondaryVoice,
        )
    }
}

