package com.cvc953.localplayer.util

data class LyricWord(val startMs: Long, val endMs: Long, val text: String)

data class LrcLine(val timeMs: Long, val text: String, val words: List<LyricWord> = emptyList())

fun parseLrc(content: String): List<LrcLine> {
    val regex = Regex("""\[(\d+):(\d+)\.(\d+)](.*)""")

    return content.lines().mapNotNull { line ->
        val match = regex.find(line) ?: return@mapNotNull null

        val min = match.groupValues[1].toLong()
        val sec = match.groupValues[2].toLong()
        val ms = match.groupValues[3].toLong() * 10

        LrcLine(
                timeMs = min * 60_000 + sec * 1_000 + ms,
                text = match.groupValues[4],
                words = emptyList()
        )
    }
}
