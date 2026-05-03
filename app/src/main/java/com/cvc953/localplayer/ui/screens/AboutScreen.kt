package com.cvc953.localplayer.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.ui.theme.LocalExtendedColors

@Suppress("ktlint:standard:function-naming")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    BackHandler {
        onBack()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .height(56.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(start = 4.dp, end = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    "Acerca de",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        // Content
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            // App Name and Icon
            Text(
                text = "Local Player",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "v1.0.7",
                fontSize = 14.sp,
                color = LocalExtendedColors.current.textSecondarySoft,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Description
            Text(
                text = "Reproductor de música local ligero y moderno",
                fontSize = 16.sp,
                // color = Color(0xFFCCCCCC),
                color = LocalExtendedColors.current.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Features Section
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Características",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                FeatureItem(
                    title = "Reproducción local",
                    description = "Accede a tu música sin internet",
                )

                FeatureItem(
                    title = "Soporte para letras",
                    description = "Visualiza las letras mientras reproduces",
                )

                FeatureItem(
                    title = "Cola de reproducción",
                    description = "Organiza tus próximas canciones",
                )

                FeatureItem(
                    title = "Búsqueda",
                    description = "Encuentra tus canciones rápidamente",
                )

                FeatureItem(
                    title = "Modos de repetición",
                    description = "Shuffle, repetir uno o repetir todo",
                )

                FeatureItem(
                    title = "Información de audio",
                    description = "Visualiza el formato y bitrate",
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Developer Section
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
            ) {
                Text(
                    text = "Desarrollador",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Cristian Villalobos C.",
                    fontSize = 14.sp,
                    // color = Color(0xFFCCCCCC),
                    color = LocalExtendedColors.current.textSecondary,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tech Stack Section
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
            ) {
                Text(
                    text = "Tecnologías",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(12.dp))

                TechItem(name = "Kotlin", description = "Lenguaje de programación")
                TechItem(name = "Jetpack Compose", description = "Framework de UI")
                TechItem(name = "Material Design 3", description = "Diseño")
                TechItem(name = "Android MediaPlayer", description = "Reproducción de audio")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Policy Section
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Política de privacidad",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Text(
                    text = "Local Player no recopila, almacena ni comparte datos personales del usuario.",
                    fontSize = 13.sp,
                    color = LocalExtendedColors.current.textSecondary,
                    lineHeight = 20.sp,
                )

                Text(
                    text = "La app funciona de forma local en tu dispositivo y solo accede a archivos de audio y carpetas que tú selecciones.",
                    fontSize = 13.sp,
                    color = LocalExtendedColors.current.textSecondarySoft,
                    lineHeight = 20.sp,
                )

                Text(
                    text = "No se envía información a servidores externos.",
                    fontSize = 13.sp,
                    color = LocalExtendedColors.current.textSecondarySoft,
                    lineHeight = 20.sp,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer
            Text(
                text = "© 2026 Local Player. Todos los derechos reservados.",
                fontSize = 12.sp,
                color = LocalExtendedColors.current.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun FeatureItem(
    title: String,
    description: String,
) {
    Column {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = LocalExtendedColors.current.textSecondarySoft,
        )
    }
}

@Suppress("ktlint:standard:function-naming")
@Composable
private fun TechItem(
    name: String,
    description: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = LocalExtendedColors.current.textSecondarySoft,
            )
        }
    }
}
