package com.cvc953.localplayer.util

import android.util.Log
import com.cvc953.localplayer.model.TtmlLyrics
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.id3.AbstractID3v2Tag
import org.jaudiotagger.tag.id3.AbstractID3v2Frame
import org.jaudiotagger.tag.id3.framebody.FrameBodyUSLT
import org.jaudiotagger.tag.mp4.Mp4Tag
import java.io.File

/**
 * Result of extracting embedded lyrics from an audio file.
 * Can contain synchronized TTML (word-by-word), LRC-formatted text, or plain text lines.
 */
data class EmbeddedLyricsResult(
    val lrcLines: List<LrcLine>,
    val ttmlLyrics: TtmlLyrics? = null,
)

/**
 * Extracts embedded lyrics from audio file metadata.
 *
 * Detection priority:
 * 1. TTML (XML with <tt>) → word-by-word synchronized
 * 2. LRC format ([MM:SS.xx]) → line-synchronized
 * 3. Plain text → distributed evenly across song duration
 */
object EmbeddedLyricsExtractor {

    private const val TAG = "EmbeddedLyrics"

    /**
     * Extract lyrics from the audio file at [audioPath].
     * @param audioPath absolute path to the audio file
     * @param durationMs song duration in milliseconds (for plain text distribution)
     * @return EmbeddedLyricsResult if any lyrics found, null otherwise
     */
    fun extract(audioPath: String, durationMs: Long): EmbeddedLyricsResult? {
        return try {
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                return null
            }

            val tagFile = AudioFileIO.read(audioFile)
            val tag = tagFile.tag

            if (tag == null) {
                return null
            }

            val rawText = extractRawText(tag) ?: return null
            parseLyricsText(rawText, durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting embedded lyrics: ${e.message}", e)
            null
        }
    }

    /**
     * Parse raw lyrics text into structured format.
     * Detects TTML → LRC → plain text in priority order.
     */
    private fun parseLyricsText(rawText: String, durationMs: Long): EmbeddedLyricsResult? {
        // 1. Try TTML (XML word-by-word synchronized)
        if (isTtml(rawText)) {
            val ttml = TtmlParser.parseTtml(rawText)
            if (ttml.lines.isNotEmpty()) {
                val lrcLines = ttml.toLrcLines()
                return EmbeddedLyricsResult(lrcLines = lrcLines, ttmlLyrics = ttml)
            }
        }

        // 2. Try LRC format ([MM:SS.xx] timestamps)
        val lrcLines = parseLrc(rawText)
        if (lrcLines.isNotEmpty()) {
            return EmbeddedLyricsResult(lrcLines = lrcLines)
        }

        // 3. Plain text: split lines, distribute evenly
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (lines.isEmpty()) return null

        val interval = if (lines.size > 1) durationMs / lines.size else 0L
        val distributed = lines.mapIndexed { i, text ->
            LrcLine(timeMs = i * interval, text = text)
        }
        return EmbeddedLyricsResult(lrcLines = distributed)
    }

    /**
     * Extract raw lyrics text from the audio file tag.
     */
    private fun extractRawText(tag: org.jaudiotagger.tag.Tag): String? {
        return when (tag) {
            is AbstractID3v2Tag -> {
                extractUsltText(tag)
            }
            is Mp4Tag -> {
                tag.getFirst("\u00a9lyr").takeIf { it.isNotBlank() }
            }
            else -> {
                // FlacTag wraps VorbisCommentTag internally; OGG uses VorbisCommentTag directly.
                // Both expose getFirst("LYRICS") for Vorbis-style comment fields.
                tag.getFirst("LYRICS").takeIf { it.isNotBlank() }
            }
        }
    }

    /**
     * Extract lyrics from ID3v2 USLT (Unsynchronized Lyrics) frames.
     */
    private fun extractUsltText(tag: AbstractID3v2Tag): String? {
        val usltFrames = tag.getFields("USLT")
        for (frame in usltFrames) {
            val body = (frame as? AbstractID3v2Frame)?.body as? FrameBodyUSLT ?: continue
            val text = body.getLyric()?.trim()
            if (!text.isNullOrBlank()) {
                return text
            }
        }
        return null
    }
}
