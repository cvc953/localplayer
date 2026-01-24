package com.cvc953.localplayer.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.core.content.ContextCompat
import com.cvc953.localplayer.R
import com.cvc953.localplayer.viewmodel.MainViewModel
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.MediaMetadataCompat


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
    
    private lateinit var mediaSession: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        Log.e("MusicService", "✓ onCreate() called")
        
        createNotificationChannel()
        initMediaSession()
        
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
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )
        
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                MainViewModel.instance?.togglePlayPause()
            }
            
            override fun onPause() {
                MainViewModel.instance?.togglePlayPause()
            }
            
            override fun onSkipToNext() {
                MainViewModel.instance?.playNextSong()
            }
            
            override fun onSkipToPrevious() {
                MainViewModel.instance?.playPreviousSong()
            }
        })
        
        mediaSession.isActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("MusicService", "✓ onStartCommand() - action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                MainViewModel.instance?.togglePlayPause()
                return START_STICKY
            }
            ACTION_PREV -> {
                MainViewModel.instance?.playPreviousSong()
                return START_STICKY
            }
            ACTION_NEXT -> {
                MainViewModel.instance?.playNextSong()
                return START_STICKY
            }
            ACTION_UPDATE_STATE -> {
                isPlaying = intent.getBooleanExtra("IS_PLAYING", false)
                updateMediaSession()
                updateNotification()
                return START_STICKY
            }
        }

        // Nueva canción
        val songTitle = intent?.getStringExtra("TITLE")
        val songArtist = intent?.getStringExtra("ARTIST")
        val songUri = intent?.getStringExtra("SONG_URI")

        if (!songUri.isNullOrEmpty()) {
            title = songTitle ?: "Reproduciendo"
            artist = songArtist ?: ""
            isPlaying = true
            currentSongUri = songUri
            
            Log.e("MusicService", "✓ Song loaded: $title by $artist")
            
            // Limpiar carátula anterior
            albumArt?.recycle()
            albumArt = null
            
            loadAlbumArt(songUri)
            updateMediaSession()
            updateNotification()
        }

        return START_STICKY
    }
    
    private fun updateMediaSession() {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
            .build()
        
        mediaSession.setMetadata(metadata)
        
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .build()
        
        mediaSession.setPlaybackState(playbackState)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        Log.e("MusicService", "Creating notification: '$title' by '$artist', playing: $isPlaying")
        
        val artworkBitmap = try {
            if (albumArt != null && albumArt!!.width > 0) {
                albumArt
            } else {
                BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error getting artwork: ${e.message}")
            BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
        }

        // Intent para abrir la app sin reiniciarla
        val contentIntent = Intent(this, com.cvc953.localplayer.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (title.isBlank()) "Reproduciendo" else title)
            .setContentText(artist)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(artworkBitmap)
            .setContentIntent(contentPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0, 1, 2))
            .addAction(android.R.drawable.ic_media_previous, "Anterior", getPendingIntent(ACTION_PREV))
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pausar" else "Reproducir",
                getPendingIntent(ACTION_PLAY_PAUSE)
            )
            .addAction(android.R.drawable.ic_media_next, "Siguiente", getPendingIntent(ACTION_NEXT))
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
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotification() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
                        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                        
                        // Reciclar la anterior si es diferente
                        if (albumArt != null && albumArt != scaledBitmap) {
                            albumArt!!.recycle()
                        }
                        
                        albumArt = scaledBitmap
                        if (bitmap != scaledBitmap) bitmap.recycle()
                        
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
        albumArt?.recycle()
        mediaSession.isActive = false
        mediaSession.release()
        super.onDestroy()
    }
}
