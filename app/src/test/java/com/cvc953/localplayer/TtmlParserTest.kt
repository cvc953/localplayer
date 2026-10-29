package com.cvc953.localplayer

import com.cvc953.localplayer.model.TtmlAlignment
import com.cvc953.localplayer.util.TtmlParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TtmlParserTest {
    @Test
    fun testParseSimpleTtml() {
        val ttml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml" 
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
                itunes:timing="Word">
              <body>
                <div>
                  <p begin="00:00:10.000" end="00:00:14.000">
                    <span begin="00:00:10.000" end="00:00:10.500">Hello </span>
                    <span begin="00:00:10.500" end="00:00:11.000">world</span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()

        val result = TtmlParser.parseTtml(ttml)

        assertNotNull(result)
        assertEquals("Word", result.type)
        assertEquals(1, result.lines.size)

        val line = result.lines[0]
        assertEquals(10000L, line.timeMs)
        assertEquals(4000L, line.durationMs)
        assertEquals("Hello world", line.text)
        assertEquals(2, line.syllabus.size)

        val syllable1 = line.syllabus[0]
        assertEquals("Hello ", syllable1.text)
        assertEquals(10000L, syllable1.timeMs)
        assertEquals(500L, syllable1.durationMs)

        val syllable2 = line.syllabus[1]
        assertEquals("world", syllable2.text)
        assertEquals(10500L, syllable2.timeMs)
        assertEquals(500L, syllable2.durationMs)
    }

    @Test
    fun testParseEmptyTtml() {
        val result = TtmlParser.parseTtml("")

        assertNotNull(result)
        assertEquals(0, result.lines.size)
    }

    @Test
    fun testParseTimeFormats() {
        val ttml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p begin="01:30:45.500" end="01:30:50.000">
                    <span begin="01:30:45.500" end="01:30:46.000">Test</span>
                  </p>
                  <p begin="05:30.250" end="05:35.000">
                    <span begin="05:30.250" end="05:31.000">Test2</span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()

        val result = TtmlParser.parseTtml(ttml)

        assertEquals(2, result.lines.size)

        // HH:MM:SS.mmm format (1:30:45.500 = 5445500 ms)
        assertEquals(5445500L, result.lines[0].timeMs)

        // MM:SS.mmm format (5:30.250 = 330250 ms)
        assertEquals(330250L, result.lines[1].timeMs)
    }

    @Test
    fun testAgentAlignment() {
        val ttml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml" 
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                itunes:timing="Word">
              <body>
                <div>
                  <p begin="00:00:10.000" end="00:00:14.000" ttm:agent="v1">
                    <span begin="00:00:10.000" end="00:00:14.000">Line 1</span>
                  </p>
                  <p begin="00:00:15.000" end="00:00:19.000" ttm:agent="v2">
                    <span begin="00:00:15.000" end="00:00:19.000">Line 2</span>
                  </p>
                  <p begin="00:00:20.000" end="00:00:24.000" ttm:agent="v3">
                    <span begin="00:00:20.000" end="00:00:24.000">Line 3</span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()

        val result = TtmlParser.parseTtml(ttml)

        assertNotNull(result)
        assertEquals(3, result.lines.size)

        // v1 -> LEFT
        assertEquals("v1", result.lines[0].agent)
        assertEquals(TtmlAlignment.LEFT, result.lines[0].alignment)

        // v2 -> RIGHT
        assertEquals("v2", result.lines[1].agent)
        assertEquals(TtmlAlignment.RIGHT, result.lines[1].alignment)

        // v3 -> LEFT
        assertEquals("v3", result.lines[2].agent)
        assertEquals(TtmlAlignment.LEFT, result.lines[2].alignment)
    }

    @Test
    fun testAppleBareSecondTimestamps() {
        // Apple Music emite los tiempos anteriores al primer minuto como segundos
        // sin métrica ("20.333") y, a partir del minuto, como MM:SS.mmm ("1:04.896").
        // Ambos formatos deben resolverse a milisegundos absolutos.
        val ttml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                itunes:timing="Word">
              <body dur="3:51.000">
                <div begin="20.333" end="49.692" itunes:songPart="Verse">
                  <p begin="20.333" end="21.719" itunes:key="L1" ttm:agent="v1" xml:space="preserve">
                    <span begin="20.333" end="21.719" xml:space="preserve">Midnight</span>
                  </p>
                  <p begin="59.818" end="1:04.896" itunes:key="L10" ttm:agent="v1" xml:space="preserve">
                    <span begin="59.818" end="1:00.182" xml:space="preserve">You </span><span begin="1:00.182" end="1:00.433" xml:space="preserve">got </span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()

        val result = TtmlParser.parseTtml(ttml)

        assertEquals(2, result.lines.size)

        // Segundos sin métrica: 20.333 s = 20333 ms (redondeado, no truncado)
        assertEquals(20333L, result.lines[0].timeMs)
        assertEquals(1386L, result.lines[0].durationMs)

        // Línea que cruza el minuto: inicio en segundos, fin en MM:SS.mmm
        assertEquals(59818L, result.lines[1].timeMs)
        assertEquals(5078L, result.lines[1].durationMs)

        val syllables = result.lines[1].syllabus
        assertEquals(2, syllables.size)
        assertEquals(59818L, syllables[0].timeMs)
        assertEquals(364L, syllables[0].durationMs)
        assertEquals(60182L, syllables[1].timeMs)
        assertEquals(251L, syllables[1].durationMs)
    }

    @Test
    fun testNoAgentAlignment() {
        // TTML sin ttm:agent debe tener alineación LEFT (por defecto)
        val ttml =
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml" 
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
                itunes:timing="Word">
              <body>
                <div>
                  <p begin="00:00:10.000" end="00:00:14.000">
                    <span begin="00:00:10.000" end="00:00:14.000">Line sin agente</span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()

        val result = TtmlParser.parseTtml(ttml)

        assertNotNull(result)
        assertEquals(1, result.lines.size)

        // Sin agente -> LEFT
        assertNull(result.lines[0].agent)
        assertEquals(TtmlAlignment.LEFT, result.lines[0].alignment)
    }
}
