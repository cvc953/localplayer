package com.cvc953.localplayer.util

import com.cvc953.localplayer.model.TtmlLine
import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.model.TtmlMetadata
import com.cvc953.localplayer.model.TtmlSyllable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parser para archivos TTML (Timed Text Markup Language)
 * Basado en el formato de Apple Music usado por YouLyPlus
 */
object TtmlParser {

    private const val NS_ITUNES = "http://music.apple.com/lyric-ttml-internal"
    
    fun parseTtml(content: String): TtmlLyrics {
        return try {
            android.util.Log.d("TtmlParser", "Iniciando parseo de TTML, ${content.length} chars")
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(content.reader())
            
            val result = parseTtmlDocument(parser)
            android.util.Log.d("TtmlParser", "✓ Parseado completo: ${result.lines.size} líneas")
            result
        } catch (e: Exception) {
            android.util.Log.e("TtmlParser", "✗ Error parseando TTML", e)
            e.printStackTrace()
            TtmlLyrics() // Return empty lyrics on error
        }
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

        return TtmlLyrics(
            type = timingMode,
            metadata = metadata,
            lines = lines
        )
    }

    private fun parseDiv(parser: XmlPullParser): List<TtmlLine> {
        val lines = mutableListOf<TtmlLine>()
        var depth = 1

        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "p") {
                        parseParagraph(parser)?.let { 
                            lines.add(it)
                            android.util.Log.d("TtmlParser", "Línea parseada: '${it.text}' con ${it.syllabus.size} sílabas")
                        }
                        // parseParagraph ya consumió su END_TAG
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

        android.util.Log.d("TtmlParser", "Div parseado: ${lines.size} líneas")
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

        val timeMs = parseTime(beginAttr)
        val endMs = parseTime(endAttr)
        val durationMs = endMs - timeMs

        val syllabus = mutableListOf<TtmlSyllable>()
        val textBuilder = StringBuilder()

        // Procesar contenido del <p>
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "span") {
                        val syllable = parseSpan(parser)
                        if (syllable != null) {
                            syllabus.add(syllable)
                            textBuilder.append(syllable.text)
                        }
                        // parseSpan ya consumió su END_TAG
                    } else {
                        depth++
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

        val finalText = textBuilder.toString().trim()
        if (finalText.isEmpty() && syllabus.isEmpty()) {
            return null
        }

        return TtmlLine(
            timeMs = timeMs,
            durationMs = durationMs,
            text = finalText,
            syllabus = syllabus
        )
    }

    private fun parseSpan(parser: XmlPullParser): TtmlSyllable? {
        val beginAttr = parser.getAttributeValue(null, "begin")
        val endAttr = parser.getAttributeValue(null, "end")

        if (beginAttr == null || endAttr == null) {
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
        val endMs = parseTime(endAttr)
        val durationMs = endMs - timeMs

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
                XmlPullParser.START_TAG -> depth++
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
            durationMs = durationMs
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
            else -> 0
        }
    }
}
