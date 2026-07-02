# Changelog

Todos los cambios notables de este proyecto serán documentados en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/lang/es/).


## [1.1.1]

### Añadido
- 📂 Soporte para abrir archivos de audio desde gestores de archivos (ACTION_VIEW)
- 💾 Exportar e importar configuración completa de la app
- 📋 Exportar playlists a formato M3U
- 🎨 Estilo de botón Play/Pause "Solo ícono" (sin borde ni relleno)
- 🔔 Categoría de transporte en notificaciones multimedia

### Cambiado
- 📐 Cabeceras de álbumes y artistas movidas dentro de LazyLayout como ítems de cuadrícula
- ⚙️ Secciones expandibles en pantalla de ajustes

### Corregido
- 🔄 Corregido el escaneo automático que leía de caché en vez de MediaStore
- 📊 Corregido el contador de canciones en playlists (incluía IDs huérfanos de temas eliminados)
- ▶️ Corregida la reproducción al seleccionar una canción con shuffle activo (ahora se respeta el orden aleatorio)
- ▶️ Corregida la reproducción al seleccionar una canción con shuffle inactivo (posición incorrecta en la cola)
- ⏭️ Corregido Siguiente/Anterior que reordenaba la cola aleatoriamente en cada salto
- 📈 Corregida la barra de progreso que se congelaba al pausar y reanudar
- 🎤 Restaurada la extracción de letras embebidas desde archivos de audio
- 🔀 Corregido drag-to-reorder que solo actualizaba la UI pero no el reproductor
- 🎵 Corregidos botones de reproducción en pantallas de álbumes y artistas
- 🔄 Corregida actualización de preferencias UI después de importar respaldo
- 🐛 Agregadas reglas ProGuard para jaudiotagger (clases awt/imageio faltantes en Android)

### Rendimiento
- ⚡ Optimizada apertura de PlaylistsScreen
- ⚡ Optimizada caché de carátulas combinadas con LRU cache

## [1.1.0]

### Añadido
- ✏️ Edición de metadatos y carátula de canciones (título, artista, álbum, género, año, pista, disco, imagen) desde el reproductor
- ✏️ Edición de metadatos y carátula de álbumes completos desde la pantalla de detalle de álbum
- 🏷️ Nueva pestaña de Géneros con vista configurable y pantalla de detalle
- 🎨 Selector de color de acento personalizado con entrada de código HEX
- ⚙️ Menú de Ajustes y Acerca de en pantallas de Álbumes, Artistas y Listas
- 🔤 Nuevas opciones de ordenamiento: Artista Z-A y Agregado recientemente
- 🔗 Enlace a TimeLyr en letras sincronizadas
- ❌ Eliminación de canciones
- 🎤 Detección de pistas instrumentales en letras

### Cambiado
- 🔄 Pestañas de navegación configurables (mostrar/ocultar cada una desde Ajustes)
- 🔀 Botones de reproducción aleatoria y secuencial ahora operan sobre la pantalla actual (Álbumes, Listas, Géneros) en vez de toda la biblioteca
- 🎨 Limpieza general de logs de depuración para reducir uso de batería

### Corregido
- 🔋 Reducción del consumo de batería — corregido polling de progreso, fugas de corrutinas y observadores duplicados
- 📋 Corregido el conflicto entre drag-to-reorder y selección múltiple en detalle de lista
- 🐛 Corregido el selector de color que no aplicaba el valor HEX personalizado
- 🔄 Corregido el manejo de columna GENRE nula en todos los métodos de escaneo
- 🗑️ Corregida la eliminación de canciones en Android 11+ (almacenamiento con ámbito)
- 🖼️ Corregida la edición de carátula en Android 11+ (almacenamiento con ámbito)
- 📝 Corregida la detección de letras en texto plano (ya no se muestran como letras sincronizadas)
- 🧹 Eliminadas referencias a sistema de skins inexistente en los changelogs

## [1.0.11]

### Cambiado
- 🔎 Migración de barras de búsqueda a SearchBar nativa de Material3 (con teclado automático al abrir)
- 🧩 Extracción y reutilización del componente de selección múltiple de canciones en pantallas de detalle
- 🏁 Nueva opción en Ajustes para elegir pestaña inicial por defecto (Canciones/Álbumes/Artistas/Listas)
- 🎛️ Nuevos valores por defecto de interfaz: barra serpenteante, carátula redondeada y botones Material3

### Corregido
- 🐛 Corrección del reordenamiento manual de la cola con shuffle activo (ya no reshufflea)
- 🐛 Correcciones edge-to-edge en barra inferior y miniplayer con navegación por botones/gestos

## [1.0.10]

### Añadido
- 🔄 Reordenamiento de canciones en playlists mediante arrastre (drag-to-reorder)
- 🔊 Visualización de frecuencia de muestreo y tipo MIME en información de canción
- 🎛️ Opciones de personalización del reproductor: estilos de barra de progreso (clásico, lineal, serpenteante) y estilos de botón de reproducción
- 🖼️ Soporte de imagen personalizada para playlists (URI de imagen)
- 📸 Capturas de pantalla adicionales para metadatos de Play Store

### Cambiado
- ♻️ Refactorización completa de LyricScreen para mejor mantenibilidad
- ⚙️ Pantalla de ajustes ampliada con nuevas opciones de personalización

### Corregido
- 🐛 Varias correcciones en interacciones de UI en cabeceras de álbum y playlist


## [1.0.9]

### Añadido
- 📱 Adaptación del PlayerScreen a tablets: layout responsivo landscape/portrait
- 🖼️ En landscape: carátula a la izquierda, controles a la derecha
- 📏 BottomSheetScaffold ahora ocupa todo el ancho en tablets (sheetMaxWidth)
- 🔍 Detección de tablets con escalado de fuentes, botones y espaciados

### Cambiado
- 📐 Umbral de landscape reducido de >1.6 a >1.5 para detectar tablets 1280x800
- 🔘 Tamaño de botones basado en minOf(ancho, alto) en vez de screenWidth
- 🎨 Ajuste de fuentes y espaciados en SongTitleSection y PlayerControls para tablets

### Corregido
- 🐛 Playlists con nombres de más de una palabra no mostraban canciones (decodificación URL en navegación)

## [1.0.8]

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

[Unreleased]: https://github.com/cvc953/localplayer/compare/v1.1.1...HEAD
[1.1.1]: https://github.com/cvc953/localplayer/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/cvc953/localplayer/compare/v1.0.11...v1.1.0
[1.0.11]: https://github.com/cvc953/localplayer/compare/v1.0.10...v1.0.11
[1.0.7]: https://github.com/cvc953/localplayer/compare/v1.0.5...v1.0.7
[1.0.4]: https://github.com/cvc953/localplayer/compare/v1.0.0...v1.0.4
[1.0.0]: https://github.com/cvc953/localplayer/releases/tag/v1.0.0
