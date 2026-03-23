# Proposal: Navigation Architecture Refactor

## Intent

El sistema de navegación actual de LocalPlayer está basado en gestión manual de estado con `rememberSaveable`, lo cual resulta en:

1. **Código monolítico**: La función `MainMusicScreen` tiene 913 líneas de código
2. **Testing imposible**: Acoplamiento estrecho entre pantallas
3. **Sin deep linking**: No se pueden implementar enlaces directos a álbumes/canciones
4. **Deuda técnica creciente**: Cada nueva pantalla aumenta la complejidad exponencialmente

**Objetivo**: Reemplazar la navegación manual por Navigation Compose para mejorar la escalabilidad, mantenibilidad y funcionalidad.

## Scope

### In Scope
- Agregar dependencia de Navigation Compose
- Crear gráfico de navegación centralizado (`NavGraph.kt`)
- Refactorizar `MainMusicScreen` en componentes más pequeños
- Migrar todas las pantallas existentes al nuevo sistema
- Implementar deep linking básico para álbumes, artistas y playlists
- Agregar tests de navegación básicos

### Out of Scope
- Cambios en la lógica de negocio o viewModels
- Rediseño completo de UI/UX
- Migración de datos o base de datos
- Features nuevos más allá del deep linking básico

## Approach

### Fase 1: Preparación (1-2 días)
1. Agregar dependencia: `androidx.navigation:navigation-compose:2.7.7`
2. Crear estructura de paquetes: `ui/navigation/` con:
   - `NavGraph.kt`: Gráfico de navegación principal
   - `NavigationActions.kt`: Extensión para acciones de navegación
   - `Destinations.kt`: Definiciones de destinos con argumentos
3. Crear `NavController` en `MainMusicScreen`

### Fase 2: Refactorización Core (3-5 días)
1. Extraer estado de navegación de `MainMusicScreen` a `NavController`
2. Crear wrapper de estado para mantener compatibilidad temporal
3. Refactorizar `MainMusicScreen` en:
   - `MusicScreen` (contenedor principal)
   - `MainContent` (contenido principal)
   - `BottomNavigationBar` (navegación inferior)

### Fase 3: Migración de Pantallas (5-7 días)
1. Migrar `SongsContent` a `SongsScreen`
2. Migrar `AlbumsScreen` y `AlbumDetailScreen`
3. Migrar `ArtistsScreen`, `ArtistDetailScreen`, `ArtistSongsScreen`
4. Migrar `PlaylistsScreen` y `PlaylistDetailScreen`
5. Migrar pantallas superpuestas (Settings, Equalizer, About)

### Fase 4: Deep Linking (2-3 días)
1. Definir URIs para cada destino: `localplayer://album/{name}/{artist}`
2. Configurar AndroidManifest para manejo de intents
3. Implementar navegación desde intents externos

### Fase 5: Testing y Limpieza (2-3 días)
1. Agregar tests de navegación básicos
2. Eliminar código obsoleto de navegación manual
3. Documentar nueva arquitectura

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/src/main/java/com/cvc953/localplayer/ui/MusicScreen.kt` | Modified | Se dividirá en componentes más pequeños |
| `app/src/main/java/com/cvc953/localplayer/ui/navigation/BottomNavItem.kt` | Modified | Se extenderá con definiciones de destinos |
| `app/src/main/java/com/cvc953/localplayer/ui/AlbumsScreen.kt` | Modified | Se adaptará al nuevo sistema de navegación |
| `app/src/main/java/com/cvc953/localplayer/ui/ArtistsScreen.kt` | Modified | Se adaptará al nuevo sistema de navegación |
| `app/src/main/java/com/cvc953/localplayer/ui/PlaylistsScreen.kt` | Modified | Se adaptará al nuevo sistema de navegación |
| `app/src/main/java/com/cvc953/localplayer/ui/PlayerScreen.kt` | Modified | Se adaptará al nuevo sistema de navegación |
| `app/build.gradle.kts` | Modified | Se agregará dependencia de Navigation Compose |
| `app/src/main/AndroidManifest.xml` | Modified | Se configurará manejo de deep links |
| `app/src/main/java/com/cvc953/localplayer/ui/navigation/NavGraph.kt` | New | Nuevo gráfico de navegación centralizado |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Breaking changes en navegación existente | Medium | Tests exhaustivos y feature flags temporales |
| Performance degradation por uso incorrecto de Navigation Compose | Low | Profiling y optimización en Fase 5 |
| Conflicto con ViewModels existentes | Medium | Wrapper temporal para mantener compatibilidad |
| Tiempo de desarrollo mayor al estimado | Medium | Desarrollo incremental con PRs pequeños |

## Rollback Plan

1. **Feature Flag**: Implementar feature flag `USE_NAVIGATION_COMPOSE` en `AppPrefs`
2. **Toggle Runtime**: Si hay problemas críticos, desactivar el flag y volver al sistema anterior
3. **Git Revert**: Si es necesario, revertir todos los commits relacionados con `navigation-architecture-refactor`
4. **Data Safety**: No hay cambios en datos, solo en UI, por lo que no hay riesgo de pérdida de datos

## Dependencies

- Navigation Compose 2.7.7 (compatible con Compose BOM actual)
- Android API 24+ (ya soportado por el proyecto)
- Gradle 8.0+ (ya configurado en el proyecto)

## Success Criteria

- [ ] Navegación entre todas las pantallas funciona correctamente
- [ ] Deep linking básico funciona para álbumes, artistas y playlists
- [ ] Tests de navegación pasan con cobertura > 80%
- [ ] `MainMusicScreen.kt` reduce de 913 líneas a < 200 líneas
- [ ] No hay regressions en funcionalidad existente
- [ ] Performance es igual o mejor que la implementación anterior