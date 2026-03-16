# Tasks: Navigation Architecture Refactor

## Phase 1: Foundation / Infrastructure (Preparación)

### Dependencies
- [ ] 1.1 Agregar dependencia de Navigation Compose en `app/build.gradle.kts`
  - Implementación: `implementation("androidx.navigation:navigation-compose:2.7.7")`
  - Verificación: Gradle sync sin errores

- [ ] 1.2 Crear estructura de paquetes en `app/src/main/java/com/cvc953/localplayer/ui/navigation/`
  - Crear directorio `navigation/`
  - Verificación: Directorio existe y está vacío

- [ ] 1.3 Crear archivo `Destinations.kt` con definiciones de destinos
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/navigation/Destinations.kt`
  - Contenido: Sealed class con destinos principales (Songs, Albums, AlbumDetail, Artists, ArtistDetail, Playlists, PlaylistDetail, Player, Settings, Equalizer, About)
  - Verificación: Compilación exitosa

- [ ] 1.4 Crear archivo `NavigationActions.kt` con extensiones para navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/navigation/NavigationActions.kt`
  - Contenido: Funciones de extensión para NavController
  - Verificación: Compilación exitosa

- [ ] 1.5 Crear archivo `NavGraph.kt` con gráfico de navegación principal
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/navigation/NavGraph.kt`
  - Contenido: Composable `AppNavigation()` con NavHost y rutas definidas
  - Verificación: Compilación exitosa

## Phase 2: Core Implementation / Refactorización

### Dependencies: Phase 1 completada

- [ ] 2.1 Crear `NavigationState` wrapper para mantener compatibilidad temporal
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/navigation/NavigationState.kt`
  - Contenido: Clase que envuelve NavController y provee métodos compatibles con estado actual
  - Verificación: Compilación exitosa

- [ ] 2.2 Modificar `MainMusicScreen.kt` para usar NavHost en lugar de estado manual
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/MusicScreen.kt`
  - Cambio: Reemplazar `when (selectedTab)` con NavHost
  - Verificación: App compila y muestra contenido básico

- [ ] 2.3 Refactorizar `MainMusicScreen` en `MusicScreen` (contenedor)
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/MusicScreen.kt`
  - Cambio: Extraer lógica de contenedor a función `MusicScreen()`
  - Verificación: Compilación exitosa

- [ ] 2.4 Extraer `BottomNavigationBar` a componente separado
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/components/BottomNavigationBar.kt`
  - Contenido: Componente con NavigationBar y NavigationBarItem
  - Verificación: Compilación exitosa

- [ ] 2.5 Implementar feature flag `USE_NAVIGATION_COMPOSE` en `AppPrefs`
  - Ruta: `app/src/main/java/com/cvc953/localplayer/preferences/AppPrefs.kt`
  - Contenido: Boolean con valor por defecto `false`
  - Verificación: Tests unitarios pasan

## Phase 3: Migración de Pantallas

### Dependencies: Phase 2 completada

- [ ] 3.1 Migrar `SongsContent` a `SongsScreen` con navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/SongsScreen.kt` (nuevo)
  - Cambio: Usar `navController` en lugar de callbacks para navegación
  - Verificación: Lista de canciones se muestra correctamente

- [ ] 3.2 Migrar `AlbumsScreen` a nueva navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/AlbumsScreen.kt`
  - Cambio: Usar `navController.navigate()` en lugar de `onAlbumClick()`
  - Verificación: Lista de álbumes se muestra y navega correctamente

- [ ] 3.3 Migrar `AlbumDetailScreen` a nueva navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/AlbumsScreen.kt`
  - Cambio: Usar argumentos de navigation para albumName/artistName
  - Verificación: Detalle de álbum se muestra correctamente

- [ ] 3.4 Migrar `ArtistsScreen` a nueva navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/ArtistsScreen.kt`
  - Cambio: Usar `navController.navigate()` en lugar de `onArtistClick()`
  - Verificación: Lista de artistas se muestra y navega correctamente

- [ ] 3.5 Migrar `ArtistDetailScreen` y `ArtistSongsScreen` a nueva navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/ArtistsScreen.kt`
  - Cambio: Usar argumentos de navigation para artistName
  - Verificación: Detalle de artista y canciones se muestran correctamente

- [ ] 3.6 Migrar `PlaylistsScreen` a nueva navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/PlaylistsScreen.kt`
  - Cambio: Usar `navController.navigate()` en lugar de `onPlaylistClick()`
  - Verificación: Lista de playlists se muestra y navega correctamente

- [ ] 3.7 Migrar `PlaylistDetailScreen` a nueva navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/PlaylistsScreen.kt`
  - Cambio: Usar argumentos de navigation para playlistName
  - Verificación: Detalle de playlist se muestra correctamente

- [ ] 3.8 Migrar `PlayerScreen` a nueva navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/PlayerScreen.kt`
  - Cambio: Usar `navController.navigate()` para artist/album navigation
  - Verificación: Navegación desde player a artist/album funciona

- [ ] 3.9 Migrar pantallas superpuestas (Settings, Equalizer, About)
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/MusicScreen.kt`
  - Cambio: Usar NavHost con `dialog()` para pantallas superpuestas
  - Verificación: Pantallas superpuestas se muestran correctamente

## Phase 4: Deep Linking

### Dependencies: Phase 3 completada

- [ ] 4.1 Definir URIs para cada destino en `Destinations.kt`
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/navigation/Destinations.kt`
  - Contenido: Agregar `deepLink` property a cada destino
  - Ejemplo: `localplayer://album/{albumName}/{artistName}`
  - Verificación: Compilación exitosa

- [ ] 4.2 Configurar AndroidManifest para manejo de intents
  - Ruta: `app/src/main/AndroidManifest.xml`
  - Cambio: Agregar `<intent-filter>` para cada destino con deep link
  - Verificación: App se registra como manejador de URIs

- [ ] 4.3 Implementar navegación desde intents externos
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/navigation/NavGraph.kt`
  - Cambio: Configurar `NavHost` con deep links
  - Verificación: Navegación desde URI externa funciona

## Phase 5: Testing y Limpieza

### Dependencies: Phase 4 completada

- [ ] 5.1 Escribir tests de navegación básica para `NavGraph.kt`
  - Ruta: `app/src/test/java/com/cvc953/localplayer/NavigationTest.kt` (nuevo)
  - Contenido: Tests para navegación entre pantallas principales
  - Verificación: Tests pasan

- [ ] 5.2 Escribir tests de deep linking
  - Ruta: `app/src/test/java/com/cvc953/localplayer/DeepLinkTest.kt` (nuevo)
  - Contenido: Tests para navegación desde URIs
  - Verificación: Tests pasan

- [ ] 5.3 Eliminar código obsoleto de navegación manual
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/MusicScreen.kt`
  - Cambio: Eliminar variables `selectedTab`, `selectedAlbumName`, etc.
  - Verificación: App funciona sin el código eliminado

- [ ] 5.4 Documentar nueva arquitectura de navegación
  - Ruta: `app/src/main/java/com/cvc953/localplayer/ui/navigation/README.md` (nuevo)
  - Contenido: Documentación de estructura y uso
  - Verificación: Documentación completa

- [ ] 5.5 Verificar cobertura de tests > 80%
  - Verificación: Reporte de cobertura muestra > 80%
  - Acción: Agregar tests si es necesario

- [ ] 5.6 Verificar reducción de líneas en `MainMusicScreen.kt`
  - Verificación: `MainMusicScreen.kt` tiene < 200 líneas
  - Acción: Refactorizar más si es necesario

- [ ] 5.7 Verificar no hay regressions en funcionalidad existente
  - Verificación: Prueba manual de todas las funcionalidades
  - Acción: Fix bugs si se encuentran

## Implementation Order

1. **Fase 1**: Infraestructura necesaria (dependencias, paquetes, archivos base)
2. **Fase 2**: Core de navegación (NavGraph, wrappers, feature flag)
3. **Fase 3**: Migración incremental de pantallas (una por una para minimizar riesgo)
4. **Fase 4**: Deep linking (después de que navegación básica funcione)
5. **Fase 5**: Testing y limpieza (antes de merge a main)

## Risk Mitigation

- Feature flag permite rollback inmediato si hay problemas
- Migración incremental minimiza riesgo de breaking changes
- Tests en cada fase aseguran calidad

## Success Criteria

- [ ] Navegación entre todas las pantallas funciona correctamente
- [ ] Deep linking básico funciona para álbumes, artistas y playlists
- [ ] Tests de navegación pasan con cobertura > 80%
- [ ] `MainMusicScreen.kt` reduce de 913 líneas a < 200 líneas
- [ ] No hay regressions en funcionalidad existente
- [ ] Performance es igual o mejor que la implementación anterior