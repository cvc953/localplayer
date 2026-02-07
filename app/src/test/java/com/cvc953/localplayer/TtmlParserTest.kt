package com.cvc953.localplayer.util

import org.junit.Test
import org.junit.Assert.*

class TtmlParserTest {
    
    @Test
    fun testParseSimpleTtml() {
        val ttml = """
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
        val ttml = """
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
    fun testWordSplitAcrossLines() {
        val ttml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml" 
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
                itunes:timing="Word">
              <body>
                <div>
                  <p begin="00:00:10.000" end="00:00:12.000">
                    <span begin="00:00:10.000" end="00:00:11.000">Ro</span>
                  </p>
                  <p begin="00:00:12.000" end="00:00:14.000">
                    <span begin="00:00:12.000" end="00:00:13.000">mance</span>
                  </p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val result = TtmlParser.parseTtml(ttml)
        assertNotNull(result)
        assertEquals(2, result.lines.size)
        val line1 = result.lines[0]
        val line2 = result.lines[1]
        assertEquals(1, line1.syllabus.size)
        assertEquals(1, line2.syllabus.size)
        val syll1 = line1.syllabus[0]
        val syll2 = line2.syllabus[0]
        assertEquals("Ro", syll1.text)
        assertEquals("mance", syll2.text)
        // La sílaba de la segunda línea debe marcar continuesWord=true
        assertTrue(syll2.continuesWord)
    }
}
