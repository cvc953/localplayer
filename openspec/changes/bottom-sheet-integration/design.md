# Design: BottomSheet Integration

## Technical Approach

Reemplazar el sistema de overlay actual (Box + zIndex) en `MusicScreenUpdated.kt` con `BottomSheetScaffold` de Material3. El MiniPlayer será el estado COLLAPSED y el PlayerScreen será el sheetContent en estado EXPANDED.

## Architecture Decisions

### Decision: BottomSheetScaffold como contenedor principal

**Choice**: Usar `BottomSheetScaffold` como el contenedor raíz en `MainMusicScreenUpdated` en lugar del `Box` actual.

**Alternatives considered**: 
- Custom BottomSheet con animaciones manuales
- ModalBottomSheet (no permite ver contenido debajo)

**Rationale**: `BottomSheetScaffold` proporciona gestures nativos, animaciones fluidas, y gestión de estados integrada. Es el patrón recomendado por Material3 para este caso de uso.

### Decision: Estado del BottomSheet en PlayerViewModel

**Choice**: Agregar un nuevo estado `bottomSheetState` de tipo `SheetValue` al PlayerViewModel, manteniendo `isPlayerScreenVisible` para backwards compatibility con Equalizer/Settings.

**Alternatives considered**:
- Estado local en el composable
- Eliminar completamente isPlayerScreenVisible

**Rationale**: Mantiene la consistencia con el resto del ViewModel y permite controlar el estado desde cualquier lugar de la app si es necesario.

### Decision: SheetState con rememberStandardBottomSheetState

**Choice**: Usar `rememberStandardBottomSheetState` para el estado del sheet.

**Alternatives considered**:
- `rememberBottomSheetState` (experimental)
- Estado manual con MutableState

**Rationale**: Es la API estable de Material3 para casos de uso estándar. Proporciona todas las transiciones necesarias.

## Data Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    PlayerViewModel                          │
├─────────────────────────────────────────────────────────────┤
│  - bottomSheetState: SheetValue (Hidden/PartiallyExpanded/Expanded) │
│  - currentSong: Song?                                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                  MusicScreenUpdated                         │
├─────────────────────────────────────────────────────────────┤
│  BottomSheetScaffold                                        │
│  ├── sheetState = rememberStandardBottomSheetState(        │
│  │       initialValue = PartiallyExpanded                  │
│  │   )                                                      │
│  ├── sheetContent = PlayerScreen (when expanded)           │
│  └── content =                                             │
│      ├── Scaffold                                           │
│      │   ├── AppNavigation                                  │
│      │   └── BottomNavigationBar                           │
│      └── MiniPlayer (integrated in bottom sheet peek)      │
└─────────────────────────────────────────────────────────────┘
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `ui/MusicScreenUpdated.kt` | Modify | Reemplazar Box por BottomSheetScaffold, integrar MiniPlayer y PlayerScreen |
| `ui/PlayerScreen.kt` | Modify | Ajustar padding para integración con BottomSheet |
| `viewmodel/PlayerViewModel.kt` | Modify | Agregar `bottomSheetState: SheetValue` y métodos associated |
| `ui/BottomSheetExtensions.kt` | Modify | Actualizar extensión `currentFraction` si es necesario |
| `ui/MiniPlayer.kt` | No change | Se mantiene igual, se integra como sheet peek |

## Interfaces / Contracts

### Nuevo estado en PlayerViewModel

```kotlin
import androidx.compose.material3.SheetValue

// Agregar al PlayerViewModel
private val _bottomSheetState = MutableStateFlow(SheetValue.Hidden)
val bottomSheetState: StateFlow<SheetValue> = _bottomSheetState

fun expandPlayer() {
    _bottomSheetState.value = SheetValue.Expanded
}

fun collapsePlayer() {
    _bottomSheetState.value = SheetValue.PartiallyExpanded
}

fun hidePlayer() {
    _bottomSheetState.value = SheetValue.Hidden
}
```

### Integración con BottomSheetScaffold

```kotlin
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberStandardBottomSheetState

@Composable
fun MainMusicScreenUpdated(...) {
    val playerViewModel: PlayerViewModel = viewModel()
    val bottomSheetStateValue by playerViewModel.bottomSheetState.collectAsState()
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = false
    )
    
    // Sincronizar estado del ViewModel con el SheetState
    LaunchedEffect(bottomSheetStateValue) {
        sheetState.snapTo(bottomSheetStateValue)
    }

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(
            sheetState = sheetState
        ),
        sheetContent = {
            PlayerScreen(
                onBack = { playerViewModel.collapsePlayer() },
                // ... otros parámetros
            )
        },
        content = { paddingValues ->
            // Scaffold principal con navegación
        },
        sheetPeekHeight = 64.dp // Altura del MiniPlayer
    ) { }
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | PlayerViewModel bottomSheetState transitions | Test cambio de estados |
| Integration | BottomSheetScaffold render | Test visual manual |
| E2E | Swipe gestures | Test con Espresso |

## Migration / Rollback

No se requiere migración de datos. El cambio es puramente de UI.

**Plan de rollback:**
1. Restaurar `Box` overlay en `MusicScreenUpdated.kt`
2. Eliminar imports de BottomSheetScaffold
3. Restaurar `isPlayerScreenVisible` en PlayerViewModel

## Open Questions

- [ ] ¿Cómo manejar la integración con Equalizer y Settings? ¿Mantener como overlay o moverlos también al BottomSheet?
- [ ] ¿Necesitamos mantener el MiniPlayer como widget separado o podemos extraer su contenido para el sheet peek?
- [ ] ¿Cuál debería ser el valor exacto de `sheetPeekHeight`? (depende del diseño actual del MiniPlayer)

**Recomendación**: Mantener Equalizer/Settings como overlays por ahora (fuera del scope), y usar sheetPeekHeight = 64.dp basado en la altura actual del MiniPlayer.
