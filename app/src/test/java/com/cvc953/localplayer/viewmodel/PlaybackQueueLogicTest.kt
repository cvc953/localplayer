package com.cvc953.localplayer.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueLogicTest {

    data class TestSong(val id: Long, val title: String)

    private fun addToQueueNextAll(queue: MutableList<TestSong>, songs: List<TestSong>, currentSong: TestSong?) {
        if (songs.isEmpty()) return
        val currentIndex = currentSong?.let { cur -> queue.indexOfFirst { it.id == cur.id } } ?: -1
        val insertIndex = if (currentIndex >= 0) currentIndex + 1 else 0
        songs.reversed().forEach { song ->
            if (queue.none { it.id == song.id }) {
                queue.add(insertIndex, song)
            }
        }
    }

    private fun addToQueueEndAll(queue: MutableList<TestSong>, songs: List<TestSong>) {
        if (songs.isEmpty()) return
        songs.forEach { song ->
            if (queue.none { it.id == song.id }) {
                queue.add(song)
            }
        }
    }

    @Test
    fun `addToQueueNextAll - adds songs in correct order when no current song`() {
        val queue = mutableListOf<TestSong>()
        val songs = listOf(TestSong(1L, "Song 1"), TestSong(2L, "Song 2"), TestSong(3L, "Song 3"))
        val currentSong: TestSong? = null

        addToQueueNextAll(queue, songs, currentSong)

        assertEquals(3, queue.size)
        assertEquals(1L, queue[0].id)
        assertEquals(2L, queue[1].id)
        assertEquals(3L, queue[2].id)
    }

    @Test
    fun `addToQueueNextAll - inserts after current song when playing`() {
        val queue = mutableListOf<TestSong>(TestSong(100L, "Current Song"))
        val currentSong = TestSong(100L, "Current Song")
        val newSongs = listOf(TestSong(1L, "Song 1"), TestSong(2L, "Song 2"))

        addToQueueNextAll(queue, newSongs, currentSong)

        assertEquals(3, queue.size)
        assertEquals(100L, queue[0].id)
        assertEquals(1L, queue[1].id)
        assertEquals(2L, queue[2].id)
    }

    @Test
    fun `addToQueueNextAll - skips duplicates`() {
        val queue = mutableListOf<TestSong>(TestSong(1L, "Existing"))
        val currentSong: TestSong? = null
        val newSongs = listOf(TestSong(1L, "Duplicate"), TestSong(2L, "New Song"))

        addToQueueNextAll(queue, newSongs, currentSong)

        assertEquals(2, queue.size)
        assertEquals(2L, queue[0].id)
        assertEquals(1L, queue[1].id)
    }

    @Test
    fun `addToQueueNextAll - does nothing for empty list`() {
        val queue = mutableListOf<TestSong>()
        val songs = emptyList<TestSong>()
        val currentSong: TestSong? = null

        addToQueueNextAll(queue, songs, currentSong)

        assertTrue(queue.isEmpty())
    }

    @Test
    fun `addToQueueEndAll - adds songs at end`() {
        val queue = mutableListOf<TestSong>(TestSong(100L, "Existing"))
        val songs = listOf(TestSong(1L, "Song 1"), TestSong(2L, "Song 2"))

        addToQueueEndAll(queue, songs)

        assertEquals(3, queue.size)
        assertEquals(100L, queue[0].id)
        assertEquals(1L, queue[1].id)
        assertEquals(2L, queue[2].id)
    }

    @Test
    fun `addToQueueEndAll - skips duplicates`() {
        val queue = mutableListOf<TestSong>(TestSong(1L, "Existing"))
        val songs = listOf(TestSong(1L, "Duplicate"), TestSong(2L, "New Song"))

        addToQueueEndAll(queue, songs)

        assertEquals(2, queue.size)
        assertEquals(1L, queue[0].id)
        assertEquals(2L, queue[1].id)
    }

    @Test
    fun `addToQueueEndAll - does nothing for empty list`() {
        val queue = mutableListOf<TestSong>()
        val songs = emptyList<TestSong>()

        addToQueueEndAll(queue, songs)

        assertTrue(queue.isEmpty())
    }
}