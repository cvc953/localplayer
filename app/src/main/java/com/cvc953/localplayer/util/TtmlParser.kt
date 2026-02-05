package com.cvc953.localplayer.util

private val TTML_P_REGEX =
        Regex(
                """<p\b[^>]*\bbegin\s*=\s*\"([^\"]+)\"[^>]*>(.*?)</p>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

private val TTML_SPAN_REGEX =
        Regex(
                """<span\b[^>]*\bbegin\s*=\s*\"([^\"]+)\"[^>]*\bend\s*=\s*\"([^\"]+)\"[^>]*>(.*?)</span>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )

fun parseTtml(content: String): List<LrcLine> {
    return TTML_P_REGEX
            .findAll(content)
            .mapNotNull { match ->
                val begin = match.groupValues.getOrNull(1)?.trim().orEmpty()
                val textRaw = match.groupValues.getOrNull(2).orEmpty()
                val timeMs = parseTtmlTimeToMs(begin) ?: return@mapNotNull null

                val words = parseTtmlWords(textRaw)
                val text =
                        when {
                            words.isNotEmpty() -> words.joinToString(" ") { it.text }
                            else -> cleanTtmlText(textRaw)
                        }

                if (text.isBlank()) return@mapNotNull null
                LrcLine(timeMs = timeMs, text = text, words = words)
            }
            .toList()
            .sortedBy { it.timeMs }
}

private fun parseTtmlWords(textRaw: String): List<LyricWord> {
    val spans =
            TTML_SPAN_REGEX
                    .findAll(textRaw)
                    .mapNotNull { spanMatch ->
                        val begin = spanMatch.groupValues.getOrNull(1)?.trim().orEmpty()
                        val end = spanMatch.groupValues.getOrNull(2)?.trim().orEmpty()
                        val wordText = cleanTtmlText(spanMatch.groupValues.getOrNull(3).orEmpty())
                        val startMs = parseTtmlTimeToMs(begin) ?: return@mapNotNull null
                        val endMs = parseTtmlTimeToMs(end) ?: return@mapNotNull null
                        if (wordText.isBlank() || endMs <= startMs) return@mapNotNull null
                        LyricWord(startMs = startMs, endMs = endMs, text = wordText)
                    }
                    .toList()

    if (spans.isNotEmpty()) {
        return spans
    }

    return emptyList()
}

private fun cleanTtmlText(input: String): String {
    val withBreaks = input.replace(Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE), "\n")
    val noTags = withBreaks.replace(Regex("""<[^>]+>"""), "")
    return noTags.replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")
            .trim()
}

private fun parseTtmlTimeToMs(input: String): Long? {
    val value = input.trim()
    if (value.isEmpty()) return null

    if (value.endsWith("ms", ignoreCase = true)) {
        val number = value.dropLast(2).trim().toDoubleOrNull() ?: return null
        return number.toLong()
    }

    if (value.endsWith("s", ignoreCase = true)) {
        val number = value.dropLast(1).trim().toDoubleOrNull() ?: return null
        return (number * 1000.0).toLong()
    }

    val parts = value.split(":")
    if (parts.isEmpty()) return null

    var hours = 0L
    var minutes = 0L
    var secondsPart = ""

    when (parts.size) {
        3 -> {
            hours = parts[0].toLongOrNull() ?: return null
            minutes = parts[1].toLongOrNull() ?: return null
            secondsPart = parts[2]
        }
        2 -> {
            minutes = parts[0].toLongOrNull() ?: return null
            secondsPart = parts[1]
        }
        1 -> {
            secondsPart = parts[0]
        }
        else -> return null
    }

    val secSplit = secondsPart.split(".", ",")
    val seconds = secSplit.getOrNull(0)?.toLongOrNull() ?: return null
    val fractionRaw = secSplit.getOrNull(1)
    val millis =
            when {
                fractionRaw.isNullOrEmpty() -> 0L
                else -> fractionRaw.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            }

    return hours * 3_600_000 + minutes * 60_000 + seconds * 1_000 + millis
}
