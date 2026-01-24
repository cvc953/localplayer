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

        mediaSession = MediaSessionCompat(this, "LocalPlayer").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {

                override fun onPlay() {
                    player.play()
                    updatePlaybackState()
                    updateNotification()
                }

                override fun onPause() {
                    player.pause()
                    updatePlaybackState()
                    updateNotification()
                }

                override fun onSkipToNext() {
                    // acá luego llamaremos al ViewModel o cola
                    player.seekToNext()
                    updatePlaybackState()
                    updateNotification()
                }

                override fun onSkipToPrevious() {
                    player.seekToPrevious()
                    updatePlaybackState()
                    updateNotification()
                }
            })
        }

        updatePlaybackState()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            title = it.getStringExtra("TITLE") ?: title
            artist = it.getStringExtra("ARTIST") ?: artist

            it.getStringExtra("SONG_URI")?.let { uri ->
                // Solo cambiar la canción si es diferente a la actual
                if (uri != currentUri) {
                    currentUri = uri
                    player.stop()
                    player.clearMediaItems()
                    player.setMediaItem(MediaItem.fromUri(uri))
                    player.prepare()
                    player.play()
                    
                    // Cargar la carátula del álbum
                    loadAlbumArt(uri)
                }
            }
        }

        updateNotification()
        return START_STICKY
    }


    private fun buildNotification(): Notification {
        val isPlaying = player.isPlaying

        // Usar la carátula cargada o una carátula por defecto
        val defaultAlbumArt = BitmapFactory.decodeResource(
            resources,
            R.drawable.ic_launcher_foreground
        )
        val displayAlbumArt = albumArt ?: defaultAlbumArt


        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(displayAlbumArt)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "Prev",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (isPlaying) android.R.drawable.ic_media_pause
                    else android.R.drawable.ic_media_play,
                    "PlayPause",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
                    )
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "Next",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        this,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )
            .setOnlyAlertOnce(true)
            .setOngoing(isPlaying)
            .build()
    }



    private fun updateNotification() {
        notificationManager.notify(NOTIF_ID, buildNotification())
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Reproducción", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)

            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)        }
    }



    override fun onDestroy() {
        player.release()
        mediaSession.release()
        super.onDestroy()
    }

    private fun loadAlbumArt(uri: String) {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(this, Uri.parse(uri))
            val art = retriever.embeddedPicture
            retriever.release()

            albumArt = art?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        } catch (e: Exception) {
            albumArt = null
        }
    }

    private fun updatePlaybackState() {
        val state = if (player.isPlaying)
            PlaybackStateCompat.STATE_PLAYING
        else
            PlaybackStateCompat.STATE_PAUSED

        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(state, player.currentPosition, 1f)
                .build()
        )
    }



}
