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


class MusicService : Service() {

    private lateinit var notificationManager: NotificationManager

    private var currentUri: String? = null
    private var title: String = "Reproduciendo"
    private var artist: String = ""
    private var albumArt: Bitmap? = null
    private var isPlaying: Boolean = false


    companion object {
        const val CHANNEL_ID = "playback_channel"
        const val NOTIF_ID = 1
        const val ACTION_PLAY_PAUSE = "com.cvc953.localplayer.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.cvc953.localplayer.ACTION_NEXT"
        const val ACTION_PREV = "com.cvc953.localplayer.ACTION_PREV"
        const val ACTION_UPDATE_STATE = "com.cvc953.localplayer.ACTION_UPDATE_STATE"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "onCreate()")
        
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        
        // Crear notificación inicial
        try {
            startForeground(NOTIF_ID, buildNotification())
            Log.d("MusicService", "startForeground() called")
        } catch (e: Exception) {
            Log.e("MusicService", "Error starting foreground: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicService", "onStartCommand() - Action: ${intent?.action}")
        
        // Manejar acciones de la notificación
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                Log.d("MusicService", "ACTION_PLAY_PAUSE clicked")
                MainViewModel.instance?.togglePlayPause()
                return START_STICKY
            }
            ACTION_PREV -> {
                Log.d("MusicService", "ACTION_PREV clicked")
                MainViewModel.instance?.playPreviousSong()
                return START_STICKY
            }
            ACTION_NEXT -> {
                Log.d("MusicService", "ACTION_NEXT clicked")
                MainViewModel.instance?.playNextSong()
                return START_STICKY
            }
            ACTION_UPDATE_STATE -> {
                isPlaying = intent?.getBooleanExtra("IS_PLAYING", false) ?: false
                Log.d("MusicService", "ACTION_UPDATE_STATE - isPlaying: $isPlaying")
                updateNotification()
                return START_STICKY
            }
        }

        // Cargar nueva canción
        val newTitle = intent?.getStringExtra("TITLE")
        val newArtist = intent?.getStringExtra("ARTIST")
        val newUri = intent?.getStringExtra("SONG_URI")

        if (newUri != null && newUri != currentUri) {
            currentUri = newUri
            title = newTitle ?: "Reproduciendo"
            artist = newArtist ?: ""
            isPlaying = true
            
            Log.d("MusicService", "New song: $title by $artist")
            
            // Cargar carátula
            loadAlbumArt(newUri)
            
            // Actualizar la notificación
            updateNotification()
        }

        return START_STICKY
    }

    private fun buildNotification(): Notification {
        Log.d("MusicService", "buildNotification() - title: $title, artist: $artist, isPlaying: $isPlaying")
        
        val artworkBitmap = if (albumArt != null && albumArt!!.width > 0) {
            Log.d("MusicService", "Using loaded album art")
            albumArt
        } else {
            Log.d("MusicService", "Using default album art")
            BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (title.isEmpty()) "Reproduciendo" else title)
            .setContentText(artist)
            .setLargeIcon(artworkBitmap)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
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
            .setOnlyAlertOnce(false)
            .setOngoing(isPlaying)

        return builder.build()
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
        Log.d("MusicService", "updateNotification()")
        try {
            notificationManager.notify(NOTIF_ID, buildNotification())
        } catch (e: Exception) {
            Log.e("MusicService", "Error updating notification: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reproducción",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controles de reproducción"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("MusicService", "Notification channel created")
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
                        albumArt = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
                        if (bitmap != albumArt) {
                            bitmap.recycle()
                        }
                        Log.d("MusicService", "Album art loaded and scaled")
                        updateNotification()
                    }
                } else {
                    Log.d("MusicService", "No embedded picture found")
                    albumArt = null
                }
            } catch (e: Exception) {
                Log.e("MusicService", "Error loading album art: ${e.message}")
                albumArt = null
            }
        }.start()
    }

    override fun onDestroy() {
        Log.d("MusicService", "onDestroy()")
        albumArt?.recycle()
        super.onDestroy()
    }
}
