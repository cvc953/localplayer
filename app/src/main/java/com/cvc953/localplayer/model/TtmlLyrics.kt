package com.cvc953.localplayer.model

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
 */
fun alignmentFromAgent(agent: String?): TtmlAlignment =
    when (agent) {
        "v1", "v3" -> TtmlAlignment.LEFT
        else -> TtmlAlignment.RIGHT // v2, v4, etc.
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
     * v1 = LEFT, v2 = RIGHT, v3 = LEFT
     */
    val alignment: TtmlAlignment = TtmlAlignment.LEFT,
    /**
     * Fracción máxima del ancho de pantalla que puede ocupar esta línea.
     * 1.0 = línea ocupa todo el ancho
     * 0.67 = línea ocupa 2/3 del ancho (cuando hay múltiples voces)
     */
    val maxWidthFraction: Float = 1f,
)

/**
 * Metadatos de las letras TTML
 */
@Serializable
data class TtmlMetadata(
    val source: String = "TTML",
    val title: String = "",
    val language: String = "",
    val songWriters: List<String> = emptyList(),
)

/**
 * Estructura completa de letras TTML
 */
@Serializable
data class TtmlLyrics(
    val type: String = "Word", // Word, Line, Syllable
    val metadata: TtmlMetadata = TtmlMetadata(),
    val lines: List<TtmlLine> = emptyList(),
)
