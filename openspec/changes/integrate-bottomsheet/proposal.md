# Proposal: Integrar BottomSheet para MiniPlayer-PlayerScreen

## Intent

Reemplazar el sistema actual de overlay flotante (con zIndex) por un BottomSheet integrado usando `BottomSheetScaffold` de Material3. El objetivo es lograr una transición fluida entre el MiniPlayer (estado collapsed) y el PlayerScreen (estado expanded), similar a como funcionan Spotify, YouTube Music y otros reproductores.

El problema actual es que:
- El PlayerScreen aparece como overlay sobre todo el contenido
- El cierre es abrupto sin animación suave hacia el MiniPlayer
- El BottomNavigationBar se oculta durante la transición

## Scope

### In Scope
- Implementar BottomSheetScaffold en MusicScreenUpdated
- Configurar sheetPeekHeight = 72dp para estado collapsed (MiniPlayer)
- Definir contenido collapsed (MiniPlayer) y expanded (PlayerScreen)
- Animar corner radius interpolado (0dp → 30dp)
- Mantener gestos swipe-up/down existentes
- Mantener LyricsScreen funcional dentro del BottomSheet
- BottomNavigationBar permanece visible durante toda la transición

### Out of Scope
- Cambios en la arquitectura de navegación
- Modificaciones al reproductor de audio
- Agregar nuevos features al reproductor
- Cambios en el sistema de permisos o storage

## Approach

Usar `BottomSheetScaffold` de Material3 con el patrón del ejemplo `compose-animated-bottomsheet`:

1. **Reemplazar estructura actual**: En MusicScreenUpdated, quitar el Box con zIndex y usar BottomSheetScaffold
2. **Configurar sheetPeekHeight**: 72.dp (altura del MiniPlayer)
3. **Separar estados**:
   - `sheetContent` = PlayerScreen (expandido)
   - Contenido dinámico según estado: MiniPlayer cuando collapsed
4. **Animación de corner radius**: Usar `currentFraction` del scaffold state para interpolar
5. **Reutilizar PlayerScreen existente**: Mantener toda la lógica dentro del sheetContent

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `MusicScreenUpdated.kt` | Modified | Reemplazar Box+zIndex por BottomSheetScaffold |
| `PlayerScreen.kt` | Modified | Ajustar para funcionar dentro del sheetContent |
| `MiniPlayer.kt` | Minor | Verificar compatibilidad con uso en BottomSheet |
| `build.gradle.kts` | Minor | Verificar dependencia de material3 |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| LyricsScreen no funciona dentro del BottomSheet | Low | Lyrics ya es conditional dentro de PlayerScreen |
| Gesto swipe conflictúa con gestures del BottomSheet | Medium | Configurar sheetGesturesEnabled apropiadamente |
| Performance durante animación | Low | BottomSheetScaffold es optimizado por Material3 |
| Compatibilidad con navegación existente | Low | Navigation Compose ya funciona dentro de scaffolds |

## Rollback Plan

1. Revertir MusicScreenUpdated.kt a versión con Box + zIndex
2. Restaurar PlayerScreen a comportamiento overlay original
3. Eliminar cualquier modificación en build.gradle.kts

## Dependencies

- Material3 BottomSheetScaffold (ya disponible en compose.material3)
- Ninguna dependencia externa nueva requerida

## Success Criteria

- [ ] El MiniPlayer se muestra cuando el BottomSheet está collapsed
- [ ] Tocar el MiniPlayer expande el BottomSheet con animación slide-up
- [ ] Swipe-down desde PlayerScreen cierra hacia MiniPlayer con animación fluida
- [ ] El corner radius anima de 0dp (expandido) a 30dp (collapsed)
- [ ] BottomNavigationBar permanece visible durante toda la transición
- [ ] Los controles del MiniPlayer funcionan durante la transición
- [ ] LyricsScreen sigue funcionando dentro del PlayerScreen/BottomSheet
- [ ] La animación es suave (mínimo 60fps)
