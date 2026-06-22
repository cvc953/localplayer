package com.cvc953.localplayer.util

data class LrcLine(
    val timeMs: Long,
    val text: String,
    val isSecondaryVoice: Boolean = false,
    val isMetadata: Boolean = false,
)

/**
 * Detecta si el contenido es TTML.
 */
fun isTtml(content: String): Boolean = content.trimStart().startsWith("<?xml") || content.contains("<tt ")

private val metadataRegex = Regex("""^\[(ar|al|ti|by|source|length|offset|re|ve|au|tool):\s*(.*)\]$""", RegexOption.IGNORE_CASE)

private val labelMap = mapOf(
    "AR" to "Artista",
    "AL" to "Álbum",
    "TI" to "Canción",
    "SOURCE" to "Fuente",
    "RE" to "Discográfica",
    "BY" to "App",
)

private val visibleTags = labelMap.keys

fun extractLyricsMetadata(content: String): List<Pair<String, String>> {
    return content.lines().mapNotNull { line ->
        val match = metadataRegex.find(line.trim()) ?: return@mapNotNull null
        val tag = match.groupValues[1].uppercase()
        val value = match.groupValues[2].trim()
        if (tag in visibleTags) {
            labelMap[tag]!! to value
        } else null
    }
}

/**
 * Verifica si el texto contiene la etiqueta [Instrumental] (case-insensitive).
 */
fun isInstrumentalContent(content: String): Boolean {
    val regex = Regex("""\[Instrumental\]""", RegexOption.IGNORE_CASE)
    return regex.containsMatchIn(content)
}

fun parseLrc(content: String): List<LrcLine> {
    val regex = Regex("""\[(\d+):(\d+)\.(\d+)](.*)""")
    val secondaryVoiceRegex = Regex("""^\s*\((.*)\)\s*$""")

    val timedLines = content.lines().mapNotNull { line ->
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

    // Si no hay líneas con timestamp
    if (timedLines.isEmpty()) {
        // Si hay metadatos, devolverlos solos (para InstrumentalView)
        val metadata = extractLyricsMetadata(content)
        if (metadata.isNotEmpty()) {
            return metadata.mapIndexed { i, (label, value) ->
                LrcLine(timeMs = (i + 1) * 100L, text = "$label: $value", isMetadata = true)
            }
        }
        // Texto plano sin timestamps ni metadatos → no mostrar nada
        return emptyList()
    }

    // Si hay timed lyrics, agregar metadatos al final
    val metadata = extractLyricsMetadata(content)
    if (metadata.isEmpty()) return timedLines

    val lastTimeMs = timedLines.last().timeMs
    return timedLines + metadata.mapIndexed { i, (label, value) ->
        LrcLine(timeMs = lastTimeMs + (i + 1) * 100L, text = "$label: $value", isMetadata = true)
    }
}
