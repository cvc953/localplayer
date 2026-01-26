# Changelog

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/lang/es/).

## [Unreleased]

### Próximas características
- Ecualizador integrado
- Soporte para playlists
- Widgets para la pantalla de inicio
- Temas personalizables

## [1.0.0] - 2026-01-25

### Añadido
- 🎵 Reproducción de música local
- 📝 Soporte para letras sincronizadas (formato LRC)
- 📋 Gestión de cola de reproducción con reordenamiento
- 🔍 Búsqueda por título y artista
- 🔀 Modo aleatorio (shuffle)
- 🔁 Modos de repetición (una canción, todas)
- 📊 Visualización de formato de audio y bitrate
- 🔄 Detección automática de nuevas canciones
- 🎨 Interfaz moderna con Material Design 3
- 📱 Controles en notificación y pantalla de bloqueo
- 🎯 Miniplayer para control rápido
- 📂 Ordenamiento de canciones (A-Z, Z-A, por artista)
- ℹ️ Pantalla "Acerca de" con información de la app
- 🔄 Actualización manual de biblioteca
- 📱 Soporte desde Android 7.0 (API 24)

### Técnico
- Arquitectura MVVM
- Jetpack Compose para UI
- Kotlin Coroutines para operaciones asíncronas
- ContentObserver para detectar cambios en la biblioteca
- Caché JSON para carga rápida
- MediaSession para controles multimedia del sistema

---

## Formato del Changelog

### Tipos de cambios
- `Añadido` para nuevas características
- `Cambiado` para cambios en funcionalidad existente
- `Obsoleto` para características que serán removidas
- `Eliminado` para características eliminadas
- `Corregido` para correcciones de bugs
- `Seguridad` para vulnerabilidades corregidas

[Unreleased]: https://github.com/cvc953/localplayer/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/cvc953/localplayer/releases/tag/v1.0.0
