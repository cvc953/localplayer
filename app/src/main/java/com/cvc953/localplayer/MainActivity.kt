package com.cvc953.localplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cvc953.localplayer.ui.MainMusicScreen
import com.cvc953.localplayer.viewmodel.MainViewModel


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            // Crear la pantalla principal; el ViewModel se inicializará
            // dentro del bloque de permisos cuando sea necesario.
                MainMusicScreen { }
        }
    }
}
