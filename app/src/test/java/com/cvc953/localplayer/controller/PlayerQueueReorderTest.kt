package com.cvc953.localplayer.controller

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import com.cvc953.localplayer.model.Song
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Contrato de la cola del reproductor tras un reordenamiento por arrastre.
 *
 * El arrastre en la hoja de cola actualiza el orden en el ViewModel y luego llama a
 * syncControllerQueue(), que termina en [PlayerController.replaceQueue]. Si eso no ocurre,
 * next() y el avance automático siguen recorriendo la cola anterior: es el bug que ya se
 * escapó antes, así que estos tests fijan el comportamiento observable.
 *
 * El orden que llega al reproductor es el que arma la UI: la canción actual primero y
 * después las siguientes en el orden elegido.
 */
class PlayerQueueReorderTest {
    private lateinit var scope: CoroutineScope

    @Before
    fun setUp() {
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun song(id: Long): Song =
        Song(
            id = id,
            title = "Song $id",
            artist = "Artist",
            album = "Album",
            year = null,
            uri = mockk<Uri>(relaxed = true),
            duration = 1000L,
            trackNumber = 1,
            discNumber = 1,
        )

    private fun controller(): PlayerController {
        val context =
            mockk<Context>(relaxed = true) {
                every { getSystemService(Context.AUDIO_SERVICE) } returns mockk<AudioManager>(relaxed = true)
            }
        return PlayerController(context, scope)
    }

    @Test
    fun `next follows the order applied by replaceQueue`() {
        val first = song(1)
        val second = song(2)
        val third = song(3)
        val controller = controller()
        controller.replaceQueue(listOf(first, second, third))
        controller.next()
        assertEquals(second, controller.state.value.currentSong)

        // El usuario arrastra la tercera canción al frente de las siguientes.
        val newOrder = listOf(second, third, first)
        controller.replaceQueue(newOrder, keepCurrentSong = true)

        controller.next()
        assertEquals(third, controller.state.value.currentSong)
    }

    @Test
    fun `replaceQueue keeps the current song playing at its new index`() {
        val first = song(1)
        val second = song(2)
        val third = song(3)
        val controller = controller()
        controller.replaceQueue(listOf(first, second, third))
        controller.next()
        assertEquals(second, controller.state.value.currentSong)

        val newOrder = listOf(second, third, first)
        controller.replaceQueue(newOrder, keepCurrentSong = true)

        assertEquals(second, controller.state.value.currentSong)
        assertEquals(newOrder, controller.getQueueSnapshot())
    }

    @Test
    fun `replaceQueue with the same order keeps the current position`() {
        val first = song(1)
        val second = song(2)
        val third = song(3)
        val controller = controller()
        val order = listOf(first, second, third)
        controller.replaceQueue(order)
        controller.next()
        assertEquals(second, controller.state.value.currentSong)

        controller.replaceQueue(order, keepCurrentSong = true)

        assertEquals(second, controller.state.value.currentSong)
        assertEquals(order, controller.getQueueSnapshot())
        controller.next()
        assertEquals(third, controller.state.value.currentSong)
    }
}
