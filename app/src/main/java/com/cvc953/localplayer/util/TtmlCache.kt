package com.cvc953.localplayer.util

import android.content.Context
import com.cvc953.localplayer.model.TtmlLine
import com.cvc953.localplayer.model.TtmlLyrics
import com.cvc953.localplayer.model.TtmlSyllable
import com.cvc953.localplayer.model.TtmlMetadata
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object TtmlCache {
    private fun cacheDir(context: Context): File {
        val dir = File(context.cacheDir, "ttml_cache")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun loadCached(context: Context, key: String): TtmlLyrics? {
        return try {
            val f = File(cacheDir(context), key)
            if (!f.exists()) return null
            val text = f.readText()
            val root = JSONObject(text)
            val type = root.optString("type", "Word")
            val metaObj = root.optJSONObject("metadata") ?: JSONObject()
            val metadata = TtmlMetadata(
                source = metaObj.optString("source", "TTML"),
                title = metaObj.optString("title", ""),
                language = metaObj.optString("language", ""),
                songWriters = (metaObj.optJSONArray("songWriters") ?: JSONArray()).let { arr ->
                    List(arr.length()) { i -> arr.optString(i) }
                }
            )

            val linesArr = root.optJSONArray("lines") ?: JSONArray()
            val lines = mutableListOf<TtmlLine>()
            for (i in 0 until linesArr.length()) {
                val ln = linesArr.getJSONObject(i)
                val syllabusArr = ln.optJSONArray("syllabus") ?: JSONArray()
                val syllabus = mutableListOf<TtmlSyllable>()
                for (j in 0 until syllabusArr.length()) {
                    val s = syllabusArr.getJSONObject(j)
                    syllabus.add(
                        TtmlSyllable(
                            text = s.optString("text", ""),
                            timeMs = s.optLong("timeMs", 0L),
                            durationMs = s.optLong("durationMs", 0L),
                            isBackground = s.optBoolean("isBackground", false),
                            continuesWord = s.optBoolean("continuesWord", false),
                        ),
                    )
                }

                lines.add(
                    TtmlLine(
                        timeMs = ln.optLong("timeMs", 0L),
                        durationMs = ln.optLong("durationMs", 0L),
                        text = ln.optString("text", ""),
                        syllabus = syllabus,
                        translation = ln.optString("translation", null),
                        transliteration = ln.optString("transliteration", null),
                    ),
                )
            }

            TtmlLyrics(type = type, metadata = metadata, lines = lines)
        } catch (_: Exception) {
            null
        }
    }

    fun saveCached(context: Context, key: String, ttml: TtmlLyrics) {
        try {
            val f = File(cacheDir(context), key)
            val root = JSONObject()
            root.put("type", ttml.type)
            val meta = JSONObject()
            meta.put("source", ttml.metadata.source)
            meta.put("title", ttml.metadata.title)
            meta.put("language", ttml.metadata.language)
            val writers = JSONArray()
            ttml.metadata.songWriters.forEach { writers.put(it) }
            meta.put("songWriters", writers)
            root.put("metadata", meta)

            val linesArr = JSONArray()
            ttml.lines.forEach { ln ->
                val lnObj = JSONObject()
                lnObj.put("timeMs", ln.timeMs)
                lnObj.put("durationMs", ln.durationMs)
                lnObj.put("text", ln.text)
                ln.translation?.let { lnObj.put("translation", it) }
                ln.transliteration?.let { lnObj.put("transliteration", it) }

                val sylArr = JSONArray()
                ln.syllabus.forEach { s ->
                    val sObj = JSONObject()
                    sObj.put("text", s.text)
                    sObj.put("timeMs", s.timeMs)
                    sObj.put("durationMs", s.durationMs)
                    sObj.put("isBackground", s.isBackground)
                    sObj.put("continuesWord", s.continuesWord)
                    sylArr.put(sObj)
                }
                lnObj.put("syllabus", sylArr)
                linesArr.put(lnObj)
            }
            root.put("lines", linesArr)

            f.writeText(root.toString())
        } catch (_: Exception) {
        }
    }

    fun keyForFile(path: String, lastModified: Long): String {
        val input = "$path|$lastModified"
        return input.hashCode().toString() + ".json"
    }
}
