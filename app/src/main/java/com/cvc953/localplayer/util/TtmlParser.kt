package com.cvc953.localplayer.util

import com.cvc953.localplayer.model.TtmlLine
import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.model.TtmlMetadata
import com.cvc953.localplayer.model.TtmlSyllable
import com.cvc953.localplayer.model.alignmentFromAgent
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parser para archivos TTML (Timed Text Markup Language)
 * Basado en el formato de Apple Music
 */
object TtmlParser {
    private const val NS_ITUNES = "http://music.apple.com/lyric-ttml-internal"

    // Reuse factory to avoid expensive re-instantiation
    private val factory: XmlPullParserFactory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }

    // Simple in-memory cache to avoid reparsing identical TTML content
    private val cache = mutableMapOf<Int, TtmlLyrics>()

    val dashChars = setOf('-', '\u2013', '\u2014', '\u2010')

    fun parseTtml(content: String): TtmlLyrics {
        // cheap fingerprint: hashCode + length to reduce collision chance
        val key = content.hashCode() xor content.length
        synchronized(cache) {
            cache[key]?.let { return it }
        }

        val parser = factory.newPullParser()
        parser.setInput(content.reader())

        val result = parseTtmlDocument(parser)

        synchronized(cache) {
            // keep cache bounded
            if (cache.size > 50) cache.clear()
            cache[key] = result
        }

        return result
    }

    private fun parseTtmlDocument(parser: XmlPullParser): TtmlLyrics {
        val metadata = TtmlMetadata()
        val lines = mutableListOf<TtmlLine>()
        var timingMode = "Word"

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "tt" -> {
                            // Get timing mode from root element
                            timingMode = parser.getAttributeValue(NS_ITUNES, "timing") ?: "Word"
                        }

                        "div" -> {
                            // Parse lyrics lines within div
                            lines.addAll(parseDiv(parser))
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        // --- Unir palabras entre líneas (palabras partidas) ---
        for (i in 1 until lines.size) {
            val prevLine = lines[i - 1]
            val currLine = lines[i]
            if (prevLine.syllabus.isNotEmpty() && currLine.syllabus.isNotEmpty()) {
                val lastPrev = prevLine.syllabus.last()
                val firstCurr = currLine.syllabus.first()
                // Si la última sílaba de la línea anterior y la primera de la actual no terminan ni empiezan con espacio, es una palabra partida
                if ((!lastPrev.text.endsWith(" ") && !firstCurr.text.startsWith(" ")) &&
                    !dashChars.contains(lastPrev.text.lastOrNull())
                ) {
                    // Marcar la primera sílaba de la línea actual como continuación de palabra
                    val newFirst = firstCurr.copy(continuesWord = true)
                    val newSyllabus = currLine.syllabus.toMutableList()
                    newSyllabus[0] = newFirst
                    lines[i] = currLine.copy(syllabus = newSyllabus)
                }
            }
        }

        // --- Calcular maxWidthFraction para líneas que overlap con diferente alineación ---
        val processedLines =
            lines.mapIndexed { index, line ->
                val lineEnd = line.timeMs + line.durationMs
                // Buscar si hay alguna línea que overlap en el tiempo con diferente alineación
                val hasOverlapWithDifferentAlignment =
                    lines.any { otherLine ->
                        otherLine !== line &&
                            otherLine.alignment != line.alignment &&
                            otherLine.timeMs < lineEnd &&
                            (otherLine.timeMs + otherLine.durationMs) > line.timeMs
                    }

                if (hasOverlapWithDifferentAlignment) {
                    line.copy(maxWidthFraction = 0.67f)
                } else {
                    line
                }
            }

        return TtmlLyrics(
            type = timingMode,
            metadata = metadata,
            lines = processedLines,
        )
    }

    private fun parseDiv(parser: XmlPullParser): List<TtmlLine> {
        val lines = mutableListOf<TtmlLine>()
        var depth = 1

        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "p") {
                        parseParagraph(parser)?.let { lines.add(it) }
                    } else {
                        depth++
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "div") {
                        depth--
                    }
                }
            }
        }
        return lines
    }

    private fun parseParagraph(parser: XmlPullParser): TtmlLine? {
        val beginAttr = parser.getAttributeValue(null, "begin")
        val endAttr = parser.getAttributeValue(null, "end")

        if (beginAttr == null || endAttr == null) {
            // Consumir hasta el cierre del <p>
            var depth = 1
            while (depth > 0) {
                when (parser.next()) {
                    XmlPullParser.START_TAG -> depth++
                    XmlPullParser.END_TAG -> depth--
                }
            }
            return null
        }

        // Capturar el agente (artista) de la línea
        val agent = parser.getAttributeValue("http://www.w3.org/ns/ttml#metadata", "agent")
        val alignment = alignmentFromAgent(agent)

        val timeMs = parseTime(beginAttr)
        val endMs = parseTime(endAttr)
        val durationMs = endMs - timeMs

        val syllabus = mutableListOf<TtmlSyllable>()
        val textBuilder = StringBuilder()
        var insideParenthesis = false
        var previousContinuesWord = false

        // Procesar contenido del <p>
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "span" -> {
                            // Verificar si este span es un wrapper de background (ttm:role="x-bg")
                            val ttmRole = parser.getAttributeValue("http://www.w3.org/ns/ttml#metadata", "role")
                            val isBgWrapper = ttmRole == "x-bg"

                            // Parsear los spans internos con el flag de background
                            parseSpanWithChildren(parser, isBgWrapper).forEach { syllable ->
                                if (syllable != null) {
                                    // Detectar apertura de paréntesis para líneas sin wrapper
                                    if (syllable.text.startsWith("(")) {
                                        insideParenthesis = true
                                    }

                                    // Marcar como background si:
                                    // 1. El syllable ya tiene isBackground = true (de parseSpanWithChildren cuando es wrapper)
                                    // 2. Estamos dentro de paréntesis (sin wrapper)
                                    val updatedSyllable =
                                        if (syllable.isBackground || insideParenthesis) {
                                            syllable.copy(isBackground = true)
                                        } else {
                                            syllable
                                        }

                                    // Eliminar paréntesis
                                    var cleanedText = updatedSyllable.text.replace("(", "").replace(")", "")

                                    // Si la sílaba anterior continúa palabra, evitar espacios iniciales aquí
                                    if (previousContinuesWord) {
                                        cleanedText = cleanedText.trimStart()
                                    }

                                    // Si hay múltiples palabras en el span, dividirlas
                                    val words =
                                        cleanedText
                                            .split(
                                                "[\\s\\u00A0\\-\\u2013\\u2014\\u2010]+".toRegex(),
                                            ).filter { it.isNotEmpty() }

                                    if (words.size > 1) {
                                        // Múltiples palabras: distribuir tiempo proporcionalmente
                                        val timePerWord = updatedSyllable.durationMs / words.size
                                        words.forEachIndexed { idx, word ->
                                            val wordStartTime = updatedSyllable.timeMs + (idx * timePerWord)
                                            val wordContinuesWord = false

                                            val wordSyllable =
                                                updatedSyllable.copy(
                                                    text = word,
                                                    timeMs = wordStartTime,
                                                    durationMs = timePerWord,
                                                    continuesWord = wordContinuesWord,
                                                )

                                            syllabus.add(wordSyllable)
                                            textBuilder.append(word)
                                            if (idx < words.lastIndex) textBuilder.append(" ")
                                        }
                                        previousContinuesWord = false
                                    } else {
                                        // Una sola palabra: proceso normal
                                        // Detectar si continúa en la siguiente palabra (no termina con espacio)
                                        val continuesWord =
                                            cleanedText.isNotEmpty() && !cleanedText.endsWith(" ") &&
                                                !dashChars.contains(cleanedText.trimEnd().lastOrNull())

                                        // Si continúa la palabra, eliminar el espacio final
                                        val finalText =
                                            if (continuesWord) {
                                                cleanedText.trimEnd()
                                            } else {
                                                cleanedText
                                            }

                                        if (finalText.isNotEmpty()) {
                                            val cleanedSyllable =
                                                updatedSyllable.copy(
                                                    text = finalText,
                                                    continuesWord = continuesWord,
                                                )

                                            syllabus.add(cleanedSyllable)
                                            textBuilder.append(finalText)
                                        }

                                        previousContinuesWord = continuesWord
                                    }

                                    // Detectar cierre de paréntesis
                                    if (syllable.text.endsWith(")")) {
                                        insideParenthesis = false
                                    }
                                }
                            }
                        }

                        else -> {
                            depth++
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text
                    if (!text.isNullOrBlank()) {
                        textBuilder.append(text)
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "p") {
                        depth--
                    }
                }
            }
        }

        // Detectar sílabas sostenidas (gaps entre sílabas consecutivas)
        val syllabusWithSustain =
            syllabus.mapIndexed { i, syl ->
                if (i < syllabus.size - 1) {
                    val nextSyl = syllabus[i + 1]
                    val gap = nextSyl.timeMs - (syl.timeMs + syl.durationMs)
                    val isSustained = gap > 50 // Gap > 50ms indica sílaba sostenida
                    syl.copy(isSustained = isSustained)
                } else {
                    syl
                }
            }

        // Construir el texto de la línea a partir de las sílabas, respetando continuesWord
        val lineText =
            buildString {
                syllabusWithSustain.forEachIndexed { i, syl ->
                    if (i > 0 && !syl.continuesWord) append(" ")
                    append(syl.text)
                }
            }.trim()
        if (lineText.isEmpty() && syllabusWithSustain.isEmpty()) {
            return null
        }

        return TtmlLine(
            timeMs = timeMs,
            durationMs = durationMs,
            text = lineText,
            syllabus = syllabusWithSustain,
            agent = agent,
            alignment = alignment,
        )
    }

    /**
     * Parsea un span y sus hijos.
     * Si es un wrapper de background (ttm:role="x-bg"), procesa recursivamente los spans internos
     * y los marca como background.
     * Si no, usa el comportamiento normal de parseSpan.
     *
     * IMPORTANTE: El parser debe estar posicionado en el START_TAG del span cuando se llama.
     */
    private fun parseSpanWithChildren(
        parser: XmlPullParser,
        isBgWrapper: Boolean,
    ): List<TtmlSyllable?> {
        if (isBgWrapper) {
            // Wrapper de background: consumir el START_TAG wrapper y procesar los spans internos
            var depth = 1
            val syllables = mutableListOf<TtmlSyllable?>()

            while (depth > 0) {
                when (parser.next()) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "span") {
                            // Los spans internos también pueden ser wrappers
                            val innerTtmRole = parser.getAttributeValue("http://www.w3.org/ns/ttml#metadata", "role")
                            val isInnerBgWrapper = innerTtmRole == "x-bg"
                            syllables.addAll(parseSpanWithChildren(parser, isInnerBgWrapper))
                        } else {
                            depth++
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "span") {
                            depth--
                        }
                    }

                    XmlPullParser.TEXT -> {
                        // Ignorar texto directo dentro del wrapper
                    }
                }
            }
            // Marcar todos los syllables como background
            return syllables.map { it?.copy(isBackground = true) }
        } else {
            // No es wrapper: parsear normalmente con parseSpan
            // parseSpan espera estar en el START_TAG y consume hasta el END_TAG
            return listOf(parseSpan(parser, false))
        }
    }

    private fun parseSpan(
        parser: XmlPullParser,
        isBackground: Boolean = false,
    ): TtmlSyllable? {
        val beginAttr = parser.getAttributeValue(null, "begin")
        val endAttr = parser.getAttributeValue(null, "end")
        val durAttr = parser.getAttributeValue(null, "dur")

        if (beginAttr == null) {
            // Consumir hasta el cierre del <span>
            var depth = 1
            while (depth > 0) {
                when (parser.next()) {
                    XmlPullParser.START_TAG -> depth++
                    XmlPullParser.END_TAG -> depth--
                }
            }
            return null
        }

        val timeMs = parseTime(beginAttr)
        val endMs: Long
        val durationMs: Long

        // Soportar tanto 'end' como 'dur'
        if (endAttr != null) {
            endMs = parseTime(endAttr)
            durationMs = endMs - timeMs
        } else if (durAttr != null) {
            durationMs = parseDuration(durAttr)
            endMs = timeMs + durationMs
        } else {
            // Sin end ni dur, consumir y retornar null
            var depth = 1
            while (depth > 0) {
                when (parser.next()) {
                    XmlPullParser.START_TAG -> depth++
                    XmlPullParser.END_TAG -> depth--
                }
            }
            return null
        }

        val textBuilder = StringBuilder()
        var depth = 1

        // Procesar contenido del <span>
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.TEXT -> {
                    val text = parser.text
                    if (!text.isNullOrBlank()) {
                        textBuilder.append(text)
                    }
                }

                XmlPullParser.START_TAG -> {
                    depth++
                }

                XmlPullParser.END_TAG -> {
                    if (parser.name == "span") {
                        depth--
                    }
                }
            }
        }

        val text = textBuilder.toString()
        if (text.isBlank()) return null

        return TtmlSyllable(
            text = text,
            timeMs = timeMs,
            durationMs = durationMs,
            isBackground = isBackground,
        )
    }

    /**
     * Convierte un tiempo TTML (formato: HH:MM:SS.mmm o MM:SS.mmm) a milisegundos
     */
    private fun parseTime(timeStr: String): Long {
        val parts = timeStr.split(":")

        return when (parts.size) {
            3 -> {
                // HH:MM:SS.mmm
                val hours = parts[0].toLongOrNull() ?: 0
                val minutes = parts[1].toLongOrNull() ?: 0
                val seconds = parts[2].toDoubleOrNull() ?: 0.0

                (hours * 3600_000 + minutes * 60_000 + (seconds * 1000).toLong())
            }

            2 -> {
                // MM:SS.mmm
                val minutes = parts[0].toLongOrNull() ?: 0
                val seconds = parts[1].toDoubleOrNull() ?: 0.0

                (minutes * 60_000 + (seconds * 1000).toLong())
            }

            else -> {
                0
            }
        }
    }

    /**
     * Parsea duraciones en formato "3.917s" o "3917ms"
     */
    private fun parseDuration(durStr: String): Long =
        when {
            durStr.endsWith("s") -> {
                // Formato: "3.917s"
                val seconds = durStr.removeSuffix("s").toDoubleOrNull() ?: 0.0
                (seconds * 1000).toLong()
            }

            durStr.endsWith("ms") -> {
                // Formato: "3917ms"
                durStr.removeSuffix("ms").toLongOrNull() ?: 0L
            }

            else -> {
                // Asumir milisegundos si no hay sufijo
                durStr.toLongOrNull() ?: 0L
            }
        }
}
