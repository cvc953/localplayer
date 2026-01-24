package com.cvc953.localplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.ui.MainMusicScreen
import com.cvc953.localplayer.viewmodel.MainViewModel
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.result.contract.ActivityResultContracts
import com.cvc953.localplayer.services.MusicService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach




class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicService.ACTION_PLAY_PAUSE -> viewModel.togglePlayPause()
                MusicService.ACTION_NEXT -> viewModel.playNextSong()
                MusicService.ACTION_PREV -> viewModel.playPreviousSong()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            viewModel = viewModel()
            
            // Observar cambios en el estado del player y actualizar el servicio
            viewModel.playerState.onEach { state ->
                val intent = Intent(this, MusicService::class.java).apply {
                    action = MusicService.ACTION_UPDATE_STATE
                    putExtra("IS_PLAYING", state.isPlaying)
                }
                startService(intent)
            }.launchIn(lifecycleScope)
            
            MainMusicScreen(viewModel) { }
        }
    }

    override fun onStart() {
        super.onStart()

        val filter = IntentFilter().apply {
            addAction(MusicService.ACTION_PLAY_PAUSE)
            addAction(MusicService.ACTION_NEXT)
            addAction(MusicService.ACTION_PREV)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                controlReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("DEPRECATION")
            registerReceiver(controlReceiver, filter)
        }
    }



    override fun onStop() {
        super.onStop()
        unregisterReceiver(controlReceiver)
    }

}
