# Exploration: integrate-bottomsheet

## Current State

El proyecto tiene actualmente:
- **MiniPlayer**: Componente fijo en `MusicScreenUpdated.kt` (línea 131-138), posicionado entre el contenido principal y el BottomNavigationBar
- **PlayerScreen**: Overlay flotante con `zIndex` que cubre toda la pantalla cuando está abierto (línea 144-157)
- **Transición**: Cierre abrupto sin animación suave hacia MiniPlayer

## Affected Areas

| File | Impact | Description |
|------|--------|-------------|
| `MusicScreenUpdated.kt` | Modified | Cambiar de Box+zIndex a estructura compatible con BottomSheet |
| `PlayerScreen.kt` | Modified | Ajustar gestos y offset para transición suave |
| `PlayerViewModel.kt` | Minor | Estado para controlar visibilidad |

## Previous Work Found

**¡YA EXISTE un cambio planificado!** (`player-bottom-sheet`)

- **Proposal**: Documenta exactamente lo que el usuario quiere
- **Spec**: 98 líneas con escenarios detallados
- **Tasks**: 33 tareas divididas en 5 fases

El enfoque planificado usa el sistema de gestos existente (`offsetY` Animatable) pero lo modifica para transicionar suavemente.

## Dependencies Available

- ✅ `androidx.compose.material3` (línea 69 build.gradle.kts)
- ✅ `androidx.compose.material:material-icons-extended:1.6.0`
- ⚠️ `BottomSheetScaffold` NO está explícitamente importado

## Approaches

### Approach A: Extender cambio existente (player-bottom-sheet)
- **Pros**: Ya tiene proposal, specs y tasks; enfoque conservador
- **Cons**: No usa el patrón de BottomSheetScaffold moderno
- **Effort**: Medium

### Approach B: BottomSheetScaffold de Material3
- **Pros**: Más limpio, exactamente como el ejemplo de compose-animated-bottomsheet; menos código manual
- **Cons**: Requiere agregar dependencia explícita si no está
- **Effort**: Medium

### Approach C: BottomSheetScaffold con sheetPeekHeight
- **Pros**: Patrón exacto del ejemplo; `sheetPeekHeight=72dp` = MiniPlayer
- **Cons**: Requiere refactorizar PlayerScreen dentro del sheetContent
- **Effort**: Medium-High

## Recommendation

**Approach C** - Usar BottomSheetScaffold con sheetPeekHeight es el más limpio y alineado con el ejemplo que el usuario mostró. Esto da:
- Animación de corner radius interpolada
- sheetPeekHeight de 72dp para MiniPlayer
- Transición nativa entre collapsed/expanded

El cambio existente (player-bottom-sheet) es más manual y no aprovecha Material3.

## Risks

- El cambio es sustancial: requiere re-arquitectura de cómo se muestra PlayerScreen
- LyricsScreen dentro del BottomSheet debe probarse
- Compatibilidad con gestos existentes debe verificarse

## Ready for Proposal

**SÍ** - El usuario conoce el objetivo (integración tipo Spotify/YouTube Music).

Opciones:
1. **Reutilizar** el cambio existente `player-bottom-sheet` actualizando el enfoque a BottomSheetScaffold
2. **Crear** un nuevo cambio `integrate-bottomsheet` con el enfoque del ejemplo

Recomiendo opción 2 para mantener clean y no mezclar con el trabajo previo incompleto.
