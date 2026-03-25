# Proposal: BottomSheet Integration

## Intent

Reemplazar el sistema actual de overlay basado en `Box + zIndex` por `BottomSheetScaffold` de Material3. 

El sistema actual usa overlays flotantes que cubren toda la pantalla sin gestures nativos ni integración con el sistema de navegación. El objetivo es proporcionar una experiencia de usuario consistente con patrones Material Design, donde el MiniPlayer sea el estado COLLAPSED y el PlayerScreen sea el estado EXPANDED del BottomSheet, permitiendo transiciones fluidas mediante gestos de swipe.

## Scope

### In Scope
- Reemplazar sistema de overlay en `MusicScreenUpdated.kt` por `BottomSheetScaffold`
- Integrar MiniPlayer como estado COLLAPSED del BottomSheet
- Integrar PlayerScreen como sheetContent en estado EXPANDED
- Gestiones de gestures nativos (swipe up/down)
- Transiciones animadas entre estados
- Manejo de estado con `SheetValue` de Material3

### Out of Scope
- Modificaciones al reproductor de audio (Media3 service)
- Cambios en la base de datos o modelo de datos
- Funcionalidad del Equalizer (se mantiene como overlay)
- Funcionalidad de Settings (se mantiene como overlay)

## Approach

Usar `BottomSheetScaffold` de Material3 en lugar del Box overlay actual:

```
BottomSheetScaffold
├── scaffoldState = rememberBottomSheetScaffoldState(
│     sheetState = rememberStandardBottomSheetState(
│         initialValue = PartiallyExpanded,
│         skipHiddenState = false
│     )
│   )
├── sheetContent = PlayerScreen (cuando hay canción)
└── content = 
    ├── Scaffold principal
    │   ├── Navigation content (pantallas principales)
    │   └── MiniPlayer widget (integrado en bottomSheet)
    └── BottomNavigationBar
```

La integración seguirá el patrón de Gramophone:
- **COLLAPSED**: MiniPlayer visible en la parte inferior (peekHeight)
- **EXPANDED**: PlayerScreen completo
- **HIDDEN**: Oculto cuando no hay canción

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `ui/MusicScreenUpdated.kt` | Modified | Reemplazar Box overlay por BottomSheetScaffold |
| `ui/BottomSheetExtensions.kt` | Modified | Actualizar extensiones para nuevo estado |
| `viewmodel/PlayerViewModel.kt` | Modified | Estado del BottomSheet en lugar de isPlayerScreenVisible |
| `ui/PlayerScreen.kt` | Modified | Adaptar como sheetContent |
| `ui/MiniPlayer.kt` | Modified | Ajustar para integración con BottomSheet |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Conflictos con Navigation Compose | Medium | Usar SheetState controlado por ViewModel |
| Retrocompatibilidad con APIs antiguas | Low | Material3 BottomSheet tiene buen soporte |
| Performance con animaciones | Low | Usar animaciones nativas de Material3 |

## Rollback Plan

1. Revertir cambios en `MusicScreenUpdated.kt`
2. Restaurar el sistema de Box + zIndex original
3. Restaurar `PlayerViewModel.isPlayerScreenVisible` 
4. Eliminar imports de BottomSheetScaffold

## Dependencies

- Dependencia `androidx.compose.material3` ya está incluida en build.gradle.kts
- No se requieren dependencias adicionales

## Success Criteria

- [ ] El MiniPlayer aparece como COLLAPSED cuando hay canción reproduciéndose
- [ ] Swipe up expande al PlayerScreen completo (EXPANDED)
- [ ] Swipe down minimiza de vuelta al MiniPlayer
- [ ] El sistema de navegación sigue funcionando correctamente
- [ ] No hay overlays zIndex conflitivos
- [ ] Equalizer y Settings siguen funcionando
