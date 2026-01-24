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
import com.cvc953.localplayer.services.MusicService




class MainActivity : ComponentActivity() {

    private var viewModel: MainViewModel? = null
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel?.let { vm ->
                when (intent?.action) {
                    MusicService.ACTION_PLAY_PAUSE -> vm.togglePlayPause()
                    MusicService.ACTION_NEXT -> vm.playNextSong()
                    MusicService.ACTION_PREV -> vm.playPreviousSong()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val vm: MainViewModel = viewModel()
            viewModel = vm
            MainMusicScreen(vm) { }
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
