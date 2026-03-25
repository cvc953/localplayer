# Design: Integrar BottomSheet para MiniPlayer-PlayerScreen

## Technical Approach

Reemplazar el sistema actual de overlay (Box + zIndex) por `BottomSheetScaffold` de Material3. El patrón sigue exactamente el ejemplo de `compose-animated-bottomsheet`:

1. Usar `BottomSheetScaffold` con `sheetPeekHeight = 72.dp` (altura del MiniPlayer)
2. El `sheetContent` contiene toda la lógica de los estados collapsed/expanded
3. Extensión `currentFraction` para interpolar propiedades (corner radius)
4. Eliminar el overlay flotante actual y el estado `isPlayerScreenVisible`

## Architecture Decisions

### Decision: Usar BottomSheetScaffold en lugar de overlay manual

**Choice**: `BottomSheetScaffold` de Material3
**Alternatives considered**: 
- Extender el sistema manual de `offsetY` + Animatable existente
- Usar `ModalBottomSheet` 
- Crear un sistema de animación custom

**Rationale**: El BottomSheetScaffold ya provee:
- Gestión de estados collapsed/expanded
- Animaciones de swipe integradas
- `sheetPeekHeight` para definir el MiniPlayer
- `sheetShape` dinámico para corner radius interpolado
- Es exactamente el patrón del ejemplo referenciado por el usuario

### Decision: Eliminar el estado `isPlayerScreenVisible` del PlayerViewModel

**Choice**: El BottomSheetScaffold controla la visibilidad automáticamente
**Alternatives considered**: 
- Mantener `isPlayerScreenVisible` y comunicar al BottomSheet cuando expandir

**Rationale**: El estado `isCollapsed` del `BottomSheetState` es la fuente de verdad. No necesitamos duplicar estado. El MiniPlayer clickeable llama `expand()` directamente.

### Decision: Mover MiniPlayer dentro del sheetContent

**Choice**: El MiniPlayer se renderiza dentro del sheetContent cuando está collapsed
**Alternatives considered**: 
- Mantener MiniPlayer fuera del BottomSheet y solo mostrarlo cuando `isCollapsed`

**Rationale**: El ejemplo muestra que ambos estados (collapsed y expanded) están dentro del sheetContent, permitiendo transiciones más fluidas y control unificado del estado.

### Decision: Animar corner radius con currentFraction

**Choice**: `val radius = (30 * scaffoldState.currentFraction).dp`
**Alternatives considered**: 
- Usar animaciones separadas para cada estado
- No animarlo y usar valor fijo

**Rationale**: Proporciona una transición visual suave y profesional, similar a apps como Spotify. El cálculo es lineal y eficiente.

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    MusicScreenUpdated                        │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │            BottomSheetScaffold                       │   │
│  │   - scaffoldState: BottomSheetScaffoldState         │   │
│  │   - sheetPeekHeight = 72.dp                         │   │
│  │   - sheetShape = RoundedCornerShape(radius)         │   │
│  │                                                      │   │
│  │   ┌────────────────────────────────────────────┐    │   │
│  │   │           sheetContent                     │    │   │
│  │   │                                            │    │   │
│  │   │  Expanded:                                 │    │   │
│  │   │  └── PlayerScreen (full)                  │    │   │
│  │   │        └── LyricsScreen (conditional)     │    │   │
│  │   │                                            │    │   │
│  │   │  Collapsed:                                │    │   │
│  │   │  └── MiniPlayer (72dp height)             │    │   │
│  │   │                                            │    │   │
│  │   └────────────────────────────────────────────┘    │   │
│  │                                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           AppNavigation (main content)               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           BottomNavigationBar                        │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

Interacción:
1. Usuario toca MiniPlayer → playerViewModel.openPlayerScreen() → scaffoldState.expand()
2. Usuario swipe-down → scaffoldState.collapse() → MiniPlayer visible
3. currentFraction = 0.0 (collapsed) → 1.0 (expanded)
4. radius = 30 * currentFraction → corner radius interpolado
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `ui/MusicScreenUpdated.kt` | Modify | Reemplazar Box+zIndex por BottomSheetScaffold |
| `ui/BottomSheetExtensions.kt` | Create | Extensión `currentFraction` para interpolación |
| `viewmodel/PlayerViewModel.kt` | Modify | Eliminar `isPlayerScreenVisible` (ya no necesario) |
| `ui/MiniPlayer.kt` | Modify | Agregar parámetro `onExpand` para expandir BottomSheet |
| `ui/PlayerScreen.kt` | Minor | Ajustar padding/layout para funcionar dentro del sheet |

## Interfaces / Contracts

### BottomSheetExtensions.kt (nuevo)

```kotlin
package com.cvc953.localplayer.ui

import androidx.compose.material.BottomSheetScaffoldState
import androidx.compose.material.BottomSheetValue.Collapsed
import androidx.compose.material.BottomSheetValue.Expanded
import androidx.compose.material.ExperimentalMaterialApi

/**
 * Convierte el progreso del BottomSheet en un valor único de 0.0 a 1.0
 * 0.0f = Collapsed (MiniPlayer visible)
 * 1.0f = Expanded (PlayerScreen completo)
 */
@OptIn(ExperimentalMaterialApi::class)
val BottomSheetScaffoldState.currentFraction: Float
    get() {
        val fraction = bottomSheetState.progress.fraction
        val targetValue = bottomSheetState.targetValue
        val currentValue = bottomSheetState.currentValue

        return when {
            currentValue == Collapsed && targetValue == Collapsed -> 0f
            currentValue == Expanded && targetValue == Expanded -> 1f
            currentValue == Collapsed && targetValue == Expanded -> fraction
            else -> 1f - fraction
        }
    }
```

### MiniPlayer modificado

```kotlin
@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,          // existing - para navegar a lyrics
    onNext: () -> Unit,
    onExpand: () -> Unit,         // NUEVO: para expandir BottomSheet
    modifier: Modifier = Modifier,
)
```

### MusicScreenUpdated estructura nueva

```kotlin
val scaffoldState = rememberBottomSheetScaffoldState(
    bottomSheetState = rememberBottomSheetState(BottomSheetValue.Collapsed)
)

val radius = (30 * scaffoldState.currentFraction).dp

BottomSheetScaffold(
    scaffoldState = scaffoldState,
    sheetShape = RoundedCornerShape(topStart = radius, topEnd = radius),
    sheetPeekHeight = 72.dp,
    sheetContent = {
        // Both states in one content
        if (scaffoldState.bottomSheetState.isCollapsed) {
            MiniPlayer(
                song = song,
                isPlaying = isPlaying,
                onClick = { /* expand */ }
                // ...
            )
        } else {
            PlayerScreen(/* ... */)
        }
    }
) {
    // Main content (AppNavigation)
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | currentFraction calculation | Testear casos: collapsed→expanded, expanded→collapsed, partial |
| Unit | MiniPlayer props | Verificar que onExpand se pasa correctamente |
| Integration | BottomSheetScaffold gestures | Swipe up/down funciona correctamente |
| Integration | Corner radius animation | Verificar interpolación visual (manual) |
| Integration | Lyrics dentro de BottomSheet | Verificar que las letras se muestran y cerran correctamente |

## Migration / Rollback

**No migration required** - Este cambio es puramente de UI.

Rollback:
1. Restaurar MusicScreenUpdated.kt a versión con Box + zIndex
2. Restaurar PlayerViewModel.isPlayerScreenVisible
3. Restaurar MiniPlayer firma (remover onExpand)

## Open Questions

- [x] ¿El ejemplo usa `androidx.compose.material` pero el proyecto tiene `material3`? El proyecto usa material3, pero BottomSheetScaffold está disponible en ambas librerías. En material3 se importa desde `androidx.compose.material3.BottomSheetScaffold`. Se debe usar la versión de material3.
- [ ] ¿Los gestos de swipe existentes en PlayerScreen (offsetY) conflictúan con los del BottomSheet? Probablemente haya que remover el código de gestos manuales de PlayerScreen ya que BottomSheetScaffold maneja los gestos automáticamente.
