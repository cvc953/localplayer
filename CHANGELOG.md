# Changelog

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/lang/es/).

## [Unreleased]

### Añadido
- 🌐 Localización completa en español, inglés e italiano en toda la app
- 🗣️ Gestión de preferencia de idioma aplicada globalmente desde MainActivity y SettingsScreen
- 📂 Flujo de permisos de almacenamiento y selección de carpeta adaptado a i18n
- 🖼️ Capturas de pantalla actualizadas para las variantes publicadas en inglés
- 📋 Operaciones de cola ampliadas para agregar todos los temas sin duplicados y mantener la reproducción siguiente

### Cambiado
- ⚙️ Refactorización de MainActivity, SettingsScreen y varias pantallas principales para soportar i18n
- 🎨 Limpieza de textos hardcoded en álbumes, artistas, playlists, letras, ecualizador y detalles
- 📱 Ajustes de navegación y componentes de UI para una experiencia consistente entre idiomas

### Corregido
- 🐛 Correcciones de toasts, contadores de canciones y textos de acciones pendientes de localización
- 🐛 Duplicados evitados al reproducir la siguiente canción en la cola

### Técnico
- Implementación de `LocaleUtil` y persistencia de idioma en `AppPrefs`
- Mejora de las pruebas de lógica de cola en `PlaybackQueueLogicTest`

## [1.0.7] - 2026-05-03

### Añadido
- 🇪🇸🇺🇸 Localización completa en inglés y español con metadatos e imágenes
- 📲 Iconos de app actualizados para inglés y español
- 🍞 Notificaciones toast para acciones de cola en Albums, Artists, Playlists y MusicScreen
- 👆 Accion swipe-left en DraggableSwipeRow para agregar canciones al final de la cola
- 📂 Pantallas MusicScreen, PlaylistsScreen, PlaylistDetailScreen y SettingsScreen mejoradas
- 📋 Fastlane movido a la raíz para compatibilidad con mantenedores de F-Droid

### Cambiado
- ⚙️ Refactorización de MusicScreen y PlaylistsScreen con funcionalidades avanzadas
- 🎨 UI de pantallas mejorada con características adicionales

### Corregido
- 🐛 Inclusión de metadatos de dependencias deshabilitada en APK y bundle
- 🐛 Mensajes toast actualizados para consistencia en agregar canciones a la cola
- 🐛 Optimización de actualizaciones de progreso y sincronización de posición de letras
- 🐛 Ajuste de duración de animaciones para gestos de arrastre
- 🐛 Opacidad de color de letras inactivas corregida
- 🐛 Visualización de toast message en ArtistDetailScreen corregida
- 🐛 Bug que causaba collapse de letras japonesas entre sí

### Técnico
- 🏗️ ViewModels dedicados para funcionalidades de Pantallas mejoradas

## [1.0.5] - 2026-03-25

### Añadido
- 🎨 Selección de color de acento dinámico en pantalla de ajustes
- 📱 Screenshots de la app para Play Store (álbum, biblioteca, letras, reproductor) en inglés y español
- 📄 Metadatos de Fastlane para publicación en Play Store (EN y ES)

### Cambiado
- 🎨 Manejo de temas refactorizado para soportar colores de acento personalizados
- 💾 Persistencia de color de acento seleccionado en preferencias

### Corregido
- 🐛 Parser TTML detectaba múltiples palabras separadas por `-` como una sola palabra

### Técnico
- Documentación Openspec para integración de bottom-sheet (planificación)

## [1.0.4] - 2026-03-23

### Añadido
- 🎵 Soporte para letras TTML (sílabas sincronizadas, word-by-word, animaciones de puntos para gaps instrumentales)
- 🎛️ Ecualizador integrado con gestión de presets de usuario y persistencia de estado
- 📂 Gestión de carpetas de música mediante FolderViewModel
- 🎤 Soporte para voces secundarias en letras sincronizadas
- 🔤 Scroller alfabético en pantallas de álbumes, artistas y música
- 🎨 Colores dinámicos del reproductor y letras basados en luminancia del fondo
- ⚙️ Toggle de colores dinámicos en pantalla de ajustes
- 🔄 Auto-scan de biblioteca con debounce para evitar rescans innecesarios
- 💿 Carga de canciones agrupadas por álbum y artista (AlbumViewModel)
- ▶️ Reproducción directa por artista (playArtist)
- 🔁 Modo de repetición sincronizado entre PlayerController y PlaybackViewModel
- 🔊 Gestión de enfoque de audio en el reproductor
- 📋 Preferencias de ordenamiento de playlists persistidas
- 📦 Exportar e importar playlists desde/hacia archivos JSON
- ➕ Crear nuevas playlists y agregar canciones desde el diálogo del reproductor
- ⭐ Funcionalidad de favoritos en canciones
- 🔍 Menús desplegables en álbumes y artistas para opciones de reproducción
- 🎵 Track number y disc number en el modelo de datos de canciones

### Cambiado
- 🧭 Arquitectura de navegación refactorizada a Navigation Compose
- 🖥️ UI del reproductor completamente rediseñada
- 📊 Lógica de reproducción delegada a PlaybackViewModel (arquitectura más limpia)
- 🎛️ Ecualizador refactorizado a EqualizerViewModel dedicado
- 🎨 Sistema de temas mejorado con ExtendedColors y manejo consistente de colores
- ⚙️ Pantalla de ajustes refactorizada con toggle de ecualizador y colores dinámicos
- 🎯 Manejo de arrastre en listas de canciones mejorado para mayor responsividad
- 📱 Manejo del status bar refactorizado en MainActivity

### Corregido
- 🐛 Cálculo de duración de gaps en la animación de puntos de letras
- 🐛 Errores de compilación de Kotlin y mejoras en responsividad de UI
- 🐛 Manejo de relación de aspecto en la pantalla del reproductor
- 🐛 Scroll de letras que no se centraba correctamente en la posición actual
- 🐛 Inicialización del ecualizador al cambiar de sesión de audio
- 🐛 Normalización de nombres de álbumes para mejor coincidencia

### Eliminado
- 🗑️ Dependencias no utilizadas de Firebase Crashlytics y ExoPlayer
- 🗑️ Código comentado y componentes Spacer innecesarios

### Técnico
- ViewModels dedicados: Artist, Equalizer, Folder, Lyrics, Playback, Player, Playlist, Settings
- Parser TTML con soporte para sílabas continuas y unión de palabras entre líneas
- Auto-scan de biblioteca en ViewModels con debounce
- Extracción de changelog automatizada en workflow de GitHub Actions

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

[Unreleased]: https://github.com/cvc953/localplayer/compare/v1.0.7...HEAD
[1.0.7]: https://github.com/cvc953/localplayer/compare/v1.0.5...v1.0.7
[1.0.4]: https://github.com/cvc953/localplayer/compare/v1.0.0...v1.0.4
[1.0.0]: https://github.com/cvc953/localplayer/releases/tag/v1.0.0
