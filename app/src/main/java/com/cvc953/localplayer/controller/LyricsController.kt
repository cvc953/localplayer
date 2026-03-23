package com.cvc953.localplayer.controller


import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.util.LrcLine
import com.cvc953.localplayer.util.TtmlParser
import com.cvc953.localplayer.util.isTtml
import com.cvc953.localplayer.util.parseLrc

object LyricsController {
    /**
     * Carga y parsea letras desde el contenido de un archivo (TTML o LRC).
     * @param content Contenido del archivo de letras
     * @return Pair<List<LrcLine>, TtmlLyrics?>: LrcLine para compatibilidad, TtmlLyrics si es TTML
     */
    fun parseLyrics(content: String): Pair<List<LrcLine>, TtmlLyrics?> {
        return if (isTtml(content)) {
            val ttml = TtmlParser.parseTtml(content)
            Pair(ttml.lines.map { com.cvc953.localplayer.util.LrcLine(it.timeMs, it.text) }, ttml)
        } else {
            Pair(parseLrc(content), null)
        }
    }
}
