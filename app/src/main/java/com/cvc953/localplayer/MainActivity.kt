package com.cvc953.localplayer

import LocalPlayerTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cvc953.localplayer.ui.MainMusicScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LocalPlayerTheme {
                // Crear la pantalla principal; el ViewModel se inicializará
                // dentro del bloque de permisos cuando sea necesario.
                MainMusicScreen {}
            }
        }
    }
}
