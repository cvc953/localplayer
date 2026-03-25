# Exploration: BottomSheet Integration

## Topic
Reemplazar el sistema actual de overlay (Box + zIndex) por BottomSheetScaffold de Material3

## Current State

### Sistema Actual de Overlay
El proyecto LocalPlayer usa un sistema de overlay básico en `MusicScreenUpdated.kt`:

```kotlin
// Líneas 143-173
Box(modifier = Modifier.fillMaxSize()) {
    // ... Scaffold principal con navegación
    
    if (showPlayerScreen) {
        Box(modifier = Modifier.fillMaxSize().zIndex(1f)) {
            PlayerScreen(...)
        }
    }
    
    if (showEqualizer) {
        Box(modifier = Modifier.fillMaxSize().zIndex(3f)) {
            EqualizerScreen(...)
        }
    } else if (showSettings) {
        Box(modifier = Modifier.fillMaxSize().zIndex(2f)) {
            SettingsScreen(...)
        }
    }
}
```

**Problemas identificados:**
1. **Sin integración nativa**: El PlayerScreen es un overlay que cubre toda la pantalla
2. **Sin gestures nativos**: No hay swipe para expandir/minimizar el reproductor
3. **zIndex manual**: Sistema frágil de gestión de capas
4. **No sigue patrones Material**: Debería usar componentes Material3 nativos

### Archivo Existente
Ya existe `BottomSheetExtensions.kt` que define una extensión `currentFraction` para `BottomSheetScaffoldState`, lo que indica intención previa de usar BottomSheet.

## Reference: Gramophone Implementation

El proyecto Gramophone usa `BottomSheetBehavior` de Material (no Compose) con:

```kotlin
// Estados del BottomSheet
- STATE_COLLAPSED: MiniPlayer visible en la parte inferior
- STATE_EXPANDED: PlayerScreen completo
- STATE_HIDDEN: Oculto

// Transiciones con animación
- onStateChanged: Controla visibilidad de previewPlayer vs fullPlayer
- onSlide: Animación alpha entre estados
```

## Affected Areas

| Archivo | Cambio requerido |
|---------|------------------|
| `ui/MusicScreenUpdated.kt` | Reemplazar Box overlay por BottomSheetScaffold |
| `ui/PlayerScreen.kt` | Adaptar como sheetContent del BottomSheet |
| `ui/MiniPlayer.kt` | Adaptar como estado COLLAPSED |
| `ui/BottomSheetExtensions.kt` | Extensiones existentes pueden reutilizarse |
| `viewmodel/PlayerViewModel.kt` | Cambiar estado `isPlayerScreenVisible` por estado del BottomSheet |

## Approaches

### 1. BottomSheetScaffold con SheetState (RECOMENDADO)
Usar `BottomSheetScaffold` de Material3 con `SheetValue.PartiallyExpanded` y `SheetValue.Expanded`

- **Pros:**
  - Componente nativo de Material3
  - Gestures incluidos (swipe up/down)
  - Animaciones integradas
  - Estados bien definidos
- **Cons:**
  - Requiere restructurar la jerarquía de Compose
  - Integration con navigation existente
- **Esfuerzo:** Medium

### 2. ModalBottomSheet con estado manual
Usar `ModalBottomSheet` con control manual del estado

- **Pros:** Simple de implementar inicialmente
- **Cons:** No permite ver contenido debajo cuando está expandido
- **Esfuerzo:** Low

### 3. Custom BottomSheet con Box + animateFloat
Crear un BottomSheet custom usando Box y animaciones manuales

- **Pros:** Control total
- **Cons:** Mucho código重复, reinventar la rueda
- **Esfuerzo:** High

## Recommendation

**Approach 1: BottomSheetScaffold** es la opción correcta porque:
1. Permite ver el MiniPlayer cuando está colapsado mientras el contenido principal permanece visible
2. Gestures nativos de Material3
3. Integración nativa con el sistema de navegación de Compose
4. El proyecto ya tiene código preparado (`BottomSheetExtensions.kt`)

### Implementación Propuesta

```
BottomSheetScaffold
├── sheetContent = PlayerScreen (EXPANDED)
└── content = 
    ├── Scaffold
    │   ├── Navigation content
    │   └── BottomNavigationBar
    └── MiniPlayer en estado COLLAPSED (visible como sheetpeek)
```

**Estados:**
- `PartiallyExpanded`: MiniPlayer visible (peekHeight)
- `Expanded`: PlayerScreen completo
- `Hidden`: Sin canción reproduciéndose

## Risks

1. **Navegación**: Integrar BottomSheetScaffold con Navigation Compose puede ser complejo
2. **Estado compartido**: El PlayerViewModel debe управля estado del BottomSheet
3. **Equalizer/Settings**: Estos overlays también deben integrarse o mantenerse como diálogos

## Ready for Proposal

**Sí**. La exploración está completa. El cambio es claro:
- Reemplazar overlay por BottomSheetScaffold
- MiniPlayer = COLLAPSED
- PlayerScreen = EXPANDED
- Integración limpia sin zIndex manuales

Se requiere un proposal formal para proceder con specs y design.
