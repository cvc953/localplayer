package com.cvc953.localplayer.Services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.scale
import com.cvc953.localplayer.MainActivity
import com.cvc953.localplayer.R
import com.cvc953.localplayer.controller.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MusicService : Service() {
    companion object {
        const val CHANNEL_ID = "music_playback"
        const val NOTIF_ID = 2001
        const val ACTION_PLAY_PAUSE = "com.cvc953.localplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.cvc953.localplayer.ACTION_NEXT"
        const val ACTION_PREV = "com.cvc953.localplayer.ACTION_PREV"
        const val ACTION_UPDATE_STATE = "com.cvc953.localplayer.ACTION_UPDATE_STATE"
    }

    private var title: String = "Reproduciendo"
    private var artist: String = ""
    private var albumArt: Bitmap? = null
    private var isPlaying: Boolean = false
    private var currentSongUri: String = ""
    private var positionMs: Long = 0L
    private var durationMs: Long = 0L

    private lateinit var mediaSession: MediaSessionCompat
    private var serviceJob: Job? = null
    private lateinit var serviceScope: CoroutineScope
    private lateinit var playerController: PlayerController

    override fun onCreate() {
        super.onCreate()
        Log.e("MusicService", "✓ onCreate() called")

        createNotificationChannel()
        initMediaSession()

        serviceScope = CoroutineScope(Dispatchers.Default + Job())
        playerController = PlayerController.getInstance(this, serviceScope)

        // Observe player state and update notification when it changes
        serviceJob =
            serviceScope.launch {
                var lastSongUri: String? = null
                playerController.state.collect { st ->
                    val newSong = st.currentSong
                    val newSongUri = newSong?.uri?.toString()
                    if (newSongUri != null && newSongUri != lastSongUri) {
                        // Cambió la canción, recarga carátula
                        title = newSong.title.ifBlank { "Reproduciendo" }
                        artist = newSong.artist
                        isPlaying = st.isPlaying
                        positionMs = st.position
                        durationMs = st.duration
                        lastSongUri = newSongUri
                        currentSongUri = newSongUri
                        albumArt = null
                        loadAlbumArt(newSongUri)
                    } else if (newSong != null) {
                        // Misma canción, solo actualiza estado
                        title = newSong.title.ifBlank { "Reproduciendo" }
                        artist = newSong.artist
                        isPlaying = st.isPlaying
                        positionMs = st.position
                        durationMs = st.duration
                        updateMediaSession()
                        updateNotification()
                    } else {
                        // No hay canción actual
                        isPlaying = st.isPlaying
                        positionMs = st.position
                        durationMs = st.duration
                        updateMediaSession()
                        updateNotification()
                    }
                }
            }

        val notification = createNotification()
        try {
            startForeground(NOTIF_ID, notification)
            Log.e("MusicService", "✓ startForeground() successful")
        } catch (e: Exception) {
            Log.e("MusicService", "✗ startForeground() failed: ${e.message}", e)
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
        )

        mediaSession.setCallback(
            object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    playerController.togglePlayPause()
                }

                override fun onPause() {
                    playerController.togglePlayPause()
                }

                override fun onSkipToNext() {
                    playerController.next()
                }

                override fun onSkipToPrevious() {
                    playerController.previous()
                }
            },
        )

        mediaSession.isActive = true
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Log.e("MusicService", "✓ onStartCommand() - action: ${intent?.action}")

        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                playerController.togglePlayPause()
                return START_STICKY
            }

            ACTION_PREV -> {
                playerController.previous()
                return START_STICKY
            }

            ACTION_NEXT -> {
                playerController.next()
                return START_STICKY
            }

            ACTION_UPDATE_STATE -> {
                isPlaying = intent.getBooleanExtra("IS_PLAYING", false)
                durationMs = intent.getLongExtra("DURATION", durationMs)
                positionMs = intent.getLongExtra("POSITION", positionMs)
                updateMediaSession()
                updateNotification()
                return START_STICKY
            }
        }

        // Nueva canción, pero solo si es diferente o cambia el estado de reproducción
        val songTitle = intent?.getStringExtra("TITLE")
        val songArtist = intent?.getStringExtra("ARTIST")
        val songUri = intent?.getStringExtra("SONG_URI")
        val requestedIsPlaying = intent?.getBooleanExtra("IS_PLAYING", true) ?: true

        if (!songUri.isNullOrEmpty()) {
            val isSameSong = (songUri == currentSongUri)
            val isSameState = (requestedIsPlaying == isPlaying)
            if (!isSameSong || !isSameState) {
                title = songTitle ?: "Reproduciendo"
                artist = songArtist ?: ""
                isPlaying = requestedIsPlaying
                currentSongUri = songUri

                Log.e("MusicService", "✓ Song loaded: $title by $artist")

                // Clear previous album art reference (do not recycle — avoid racing with Notification/Framework)
                albumArt = null

                loadAlbumArt(songUri)
                updateMediaSession()
                updateNotification()
            } else {
                Log.e("MusicService", "✓ Ignored duplicate song/state intent: $title by $artist")
            }
        }

        return START_STICKY
    }

    private fun updateMediaSession() {
        val metadata =
            MediaMetadataCompat
                .Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
                .build()

        mediaSession.setMetadata(metadata)

        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState =
            PlaybackStateCompat
                .Builder()
                // .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .setState(
                    if (isPlaying) {
                        PlaybackStateCompat.STATE_PLAYING
                    } else {
                        PlaybackStateCompat.STATE_PAUSED
                    },
                    positionMs,
                    if (isPlaying) 1f else 0f,
                ).setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                ).build()

        mediaSession.setPlaybackState(playbackState)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        Log.e("MusicService", "Creating notification: '$title' by '$artist', playing: $isPlaying")

        val artworkBitmap =
            try {
                val candidate =
                    if (albumArt != null && !albumArt!!.isRecycled && albumArt!!.width > 0) {
                        albumArt
                    } else {
                        BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
                    }
                // Ensure we won't pass a recycled bitmap to the notification builder
                if (candidate != null && candidate.isRecycled) {
                    BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
                } else {
                    candidate
                }
            } catch (e: Exception) {
                Log.e("MusicService", "Error getting artwork: ${e.message}")
                BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
            }

        // Intent para abrir la app sin reiniciarla
        val contentIntent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        val contentPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setContentTitle(if (title.isBlank()) "Reproduciendo" else title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(artworkBitmap)
            .setContentIntent(contentPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat
                    .MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            ).addAction(android.R.drawable.ic_media_previous, "Anterior", getPendingIntent(ACTION_PREV))
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pausar" else "Reproducir",
                getPendingIntent(ACTION_PLAY_PAUSE),
            ).addAction(android.R.drawable.ic_media_next, "Siguiente", getPendingIntent(ACTION_NEXT))
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun updateNotification() {
        try {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIF_ID, createNotification())
            Log.e("MusicService", "✓ Notification updated")
        } catch (e: Exception) {
            Log.e("MusicService", "✗ updateNotification error: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        try {
            val channel = NotificationChannel(CHANNEL_ID, "Reproducción", NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(false)

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            Log.e("MusicService", "✓ Notification channel created")
        } catch (e: Exception) {
            Log.e("MusicService", "✗ createNotificationChannel error: ${e.message}", e)
        }
    }

    private fun loadAlbumArt(uri: String) {
        Thread {
            try {
                // Si cambió la canción mientras se estaba cargando, cancela
                if (currentSongUri != uri) {
                    Log.e("MusicService", "✓ Song changed, skipping old art load")
                    return@Thread
                }

                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(this, Uri.parse(uri))
                val art = retriever.embeddedPicture
                retriever.release()

                // Verificar nuevamente si la canción cambió
                if (currentSongUri != uri) {
                    Log.e("MusicService", "✓ Song changed after loading art")
                    return@Thread
                }

                if (art != null && art.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    if (bitmap != null) {
                        val scaledBitmap = bitmap.scale(512, 512)

                        // Replace cached album art (do not recycle previous bitmap here)
                        albumArt = scaledBitmap
                        if (bitmap != scaledBitmap) {
                            try {
                                bitmap.recycle()
                            } catch (_: Exception) {
                            }
                        }

                        Log.e("MusicService", "✓ Album art loaded and scaled")
                        updateMediaSession()
                        updateNotification()
                    }
                } else {
                    Log.e("MusicService", "✓ No embedded picture found")
                    albumArt = null
                }
            } catch (e: Exception) {
                Log.e("MusicService", "Error loading album art: ${e.message}")
                albumArt = null
            }
        }.start()
    }

    override fun onDestroy() {
        // Don't explicitly recycle albumArt; let the system GC handle it to avoid races
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }
}
