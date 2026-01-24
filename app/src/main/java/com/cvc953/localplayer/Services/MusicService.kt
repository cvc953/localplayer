package com.cvc953.localplayer.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media3.exoplayer.ExoPlayer
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.support.v4.media.session.PlaybackStateCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.cvc953.localplayer.R


class MusicService : Service() {

    lateinit var player: ExoPlayer
    lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager


    private var currentUri: String? = null
    private var title: String = "Reproduciendo"
    private var artist: String = ""
    private var albumArt: Bitmap? = null


    companion object {
        const val CHANNEL_ID = "playback_channel"
        const val NOTIF_ID = 1
        const val ACTION_PLAY_PAUSE = "ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PREV = "ACTION_PREV"
    }

    override fun onCreate() {
        super.onCreate()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        player = ExoPlayer.Builder(this).build()
        
        // Agregar listener para actualizar la notificación cuando cambia el estado
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackState()
                updateNotification()
            }
        })

        mediaSession = MediaSessionCompat(this, "LocalPlayer").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {

                override fun onPlay() {
                    player.play()
                }

                override fun onPause() {
                    player.pause()
                }

                override fun onSkipToNext() {
                    // Por ahora no hacemos nada, más adelante se agregará cola
                }

                override fun onSkipToPrevious() {
                    // Por ahora reinicia la canción
                    player.seekTo(0)
                }
            })
        }

        updatePlaybackState()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        
        // Manejar acciones de la notificación
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
                return START_STICKY
            }
            ACTION_PREV -> {
                player.seekTo(0)
                return START_STICKY
            }
            ACTION_NEXT -> {
                // Por ahora no hacemos nada
                return START_STICKY
            }
        }

        // Cargar nueva canción
        intent?.let {
            val newTitle = it.getStringExtra("TITLE")
            val newArtist = it.getStringExtra("ARTIST")
            val newUri = it.getStringExtra("SONG_URI")

            if (newUri != null && newUri != currentUri) {
                currentUri = newUri
                title = newTitle ?: "Reproduciendo"
                artist = newArtist ?: ""
                
                player.stop()
                player.clearMediaItems()
                player.setMediaItem(MediaItem.fromUri(newUri))
                player.prepare()
                player.play()
                
                // Cargar carátula
                loadAlbumArt(newUri)
            }
        }

        updateNotification()
        return START_STICKY
    }


    private fun buildNotification(): Notification {
        val isPlaying = player.isPlaying

        // Carátula del álbum
        val artworkBitmap = if (albumArt != null && albumArt!!.width > 0) {
            albumArt
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(artworkBitmap)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                android.R.drawable.ic_media_previous,
                "Anterior",
                getPendingIntent(ACTION_PREV)
            )
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pausar" else "Reproducir",
                getPendingIntent(ACTION_PLAY_PAUSE)
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "Siguiente",
                getPendingIntent(ACTION_NEXT)
            )
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .setShowWhen(false)
            .build()
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotification() {
        try {
            notificationManager.notify(NOTIF_ID, buildNotification())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción de música",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun loadAlbumArt(uri: String) {
        Thread {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, Uri.parse(uri))
                val art = retriever.embeddedPicture
                retriever.release()

                if (art != null) {
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    if (bitmap != null) {
                        // Escalar a 512x512 manteniendo aspecto
                        val size = 512
                        albumArt = Bitmap.createScaledBitmap(bitmap, size, size, true)
                        if (bitmap != albumArt) {
                            bitmap.recycle()
                        }
                        // Actualizar notificación con la nueva carátula
                        updateNotification()
                    }
                } else {
                    albumArt = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                albumArt = null
            }
        }.start()
    }

    private fun updatePlaybackState() {
        val state = if (player.isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, player.currentPosition, 1f)

        mediaSession.setPlaybackState(stateBuilder.build())
    }

    override fun onDestroy() {
        player.release()
        mediaSession.release()
        albumArt?.recycle()
        super.onDestroy()
    }
}
