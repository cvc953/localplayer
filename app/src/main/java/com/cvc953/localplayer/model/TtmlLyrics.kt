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
    val continuesWord: Boolean = false,  // true si esta sílaba continúa en la siguiente (sin espacio)
    val isSustained: Boolean = false  // true si hay gap antes de la siguiente sílaba (nota sostenida)
)

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
    val transliteration: String? = null
)

/**
 * Metadatos de las letras TTML
 */
@Serializable
data class TtmlMetadata(
    val source: String = "TTML",
    val title: String = "",
    val language: String = "",
    val songWriters: List<String> = emptyList()
)

/**
 * Estructura completa de letras TTML
 */
@Serializable
data class TtmlLyrics(
    val type: String = "Word", // Word, Line, Syllable
    val metadata: TtmlMetadata = TtmlMetadata(),
    val lines: List<TtmlLine> = emptyList()
)
