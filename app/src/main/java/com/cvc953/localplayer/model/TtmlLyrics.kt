package com.cvc953.localplayer.model

import com.cvc953.localplayer.util.LrcLine
import kotlinx.serialization.Serializable

/**
 * Representa una sílaba individual con sincronización temporal
 */
@Serializable
data class TtmlSyllable(
    val text: String,
    val timeMs: Long,
    val durationMs: Long,
    val isBackground: Boolean = false,
    val continuesWord: Boolean = false, // true si esta sílaba continúa en la siguiente (sin espacio)
    val isSustained: Boolean = false, // true si hay gap antes de la siguiente sílaba (nota sostenida)
)

/**
 * Alineación de la línea según el artista
 */
@Serializable
enum class TtmlAlignment {
    LEFT, // Artista principal (v1)
    RIGHT, // Segundo artista (v2) y Grupo (v3)
}

/**
 * Determina la alineación basada en el agente
 * - v1, v3: LEFT (artista principal, grupo)
 * - null, v2, v4, etc: LEFT (sin agente = alineación por defecto izquierda)
 */
fun alignmentFromAgent(agent: String?): TtmlAlignment =
    when (agent) {
        "v1", "v3" -> TtmlAlignment.LEFT

        "v2" -> TtmlAlignment.RIGHT

        // Segundo artista
        else -> TtmlAlignment.LEFT // null, v4, etc. -> LEFT por defecto
    }

/**
 * Representa una línea de letra con múltiples sílabas/palabras sincronizadas
 */
@Serializable
data class TtmlLine(
    val timeMs: Long,
    val durationMs: Long,
    val text: String,
    val syllabus: List<TtmlSyllable> = emptyList(),
    val translation: String? = null,
    val transliteration: String? = null,
    /**
     * ID del agente (artista) que canta esta línea.
     * Ej: "v1", "v2", "v3"
     */
    val agent: String? = null,
    /**
     * Alineación de la línea según el agente.
     * v1 = LEFT, v2 = RIGHT, v3 = RIGHT
     */
    val alignment: TtmlAlignment = TtmlAlignment.LEFT,
)

/**
 * Metadatos de las letras TTML
 */
@Serializable
data class TtmlMetadata(
    var source: String = "TTML",
    var title: String = "",
    var language: String = "",
    var songWriters: List<String> = emptyList(),
    var artist: String = "",
    var album: String = "",
) {
    /**
     * Convierte los metadatos a pares (label, valor) para mostrar en MetadataSection.
     */
    fun toMetadataPairs(): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()
        if (artist.isNotBlank()) pairs.add("Artista" to artist)
        if (album.isNotBlank()) pairs.add("Álbum" to album)
        if (title.isNotBlank()) pairs.add("Canción" to title)
        if (source.isNotBlank() && source != "TTML") pairs.add("Fuente" to source)
        if (songWriters.isNotEmpty()) pairs.add("Compositores" to songWriters.joinToString(", "))
        return pairs
    }

    /**
     * Convierte los metadatos a líneas LrcLine (isMetadata=true) para el fallback LRC.
     */
    fun toLrcMetadataLines(lastTimeMs: Long): List<LrcLine> {
        val pairs = toMetadataPairs()
        if (pairs.isEmpty()) return emptyList()
        return pairs.mapIndexed { i, (label, value) ->
            LrcLine(
                timeMs = lastTimeMs + (i + 1) * 100L,
                text = "$label: $value",
                isMetadata = true,
            )
        }
    }
}

/**
 * Estructura completa de letras TTML
 */
@Serializable
data class TtmlLyrics(
    val type: String = "Word", // Word, Line, Syllable
    val metadata: TtmlMetadata = TtmlMetadata(),
    val lines: List<TtmlLine> = emptyList(),
) {
    /**
     * Convierte las líneas TTML a LrcLine para el fallback de letras,
     * incluyendo metadatos al final.
     */
    fun toLrcLines(): List<LrcLine> {
        val timed = lines.filter { it.text.isNotBlank() }.map { line ->
            LrcLine(
                timeMs = line.timeMs,
                text = line.text,
                isSecondaryVoice = line.agent?.let { alignmentFromAgent(it) == TtmlAlignment.RIGHT } ?: false,
            )
        }
        if (timed.isEmpty()) return timed

        val lastTimeMs = timed.last().timeMs
        return timed + metadata.toLrcMetadataLines(lastTimeMs)
    }
}
