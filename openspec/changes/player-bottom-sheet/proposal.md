# Proposal: Player como BottomSheet con MiniPlayer Transition

## Intent

Reemplazar el overlay actual del PlayerScreen (con zIndex) por un BottomSheet que se desliza desde abajo. Cuando el usuario cierra el PlayerScreen con swipe-down, debe transicionar suavemente hacia el MiniPlayer en lugar de desaparecer instantáneamente. Esto proporciona:
- Mejor UX con transición visual natural
- El BottomNavigationBar permanece visible durante la transición
- El MiniPlayer sirve como estado intermedio durante el cierre

## Scope

### In Scope
- Convertir PlayerScreen de overlay absoluto a BottomSheet posicionado desde abajo
- Mantener los gestos swipe-to-dismiss existentes (ya implementados en PlayerScreen)
- Mostrar MiniPlayer como estado visible durante la transición de cierre
- Ocultar BottomNavigationBar cuando PlayerScreen está expandido (ya funciona con zIndex actual, mantener comportamiento)
- LyricsScreen debe seguir al PlayerScreen (permanecer como overlay dentro)

### Out of Scope
- Cambios en la arquitectura de navegación (ya está migrado a Navigation Compose)
- Modificaciones al reproductor de audio o estado de playback
- Agregar nuevos features al reproductor

## Approach

**Reutilizar el sistema de gestos existente del PlayerScreen** y cambiar el posicionamiento:

1. **En MusicScreenUpdated**: Reemplazar el Box con zIndex por un Column que contenga:
   - Contenido principal (AppNavigation + MiniPlayer)
   - PlayerScreen posicionado desde abajo usando `Modifier.offset`

2. **PlayerScreen**: 
   - Ya tiene `offsetY` Animatable para gestos
   - Modificar para que el offset sea relativo a la posición "cerrada" (MiniPlayer visible)
   - Cuando offsetY > threshold, mostrar MiniPlayer + ocultar PlayerScreen progresivamente

3. **Estado de transición**:
   - Crear un estado compartido `playerSheetProgress` (0.0 = expandido, 1.0 = cerrado/miniplayer)
   - Usar AnimatedVisibility o animateFloatAsState para transiciones suaves

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `MusicScreenUpdated.kt` | Modified | Cambiar estructura de Box/Z-index a Column con offset para PlayerScreen |
| `PlayerScreen.kt` | Modified | Ajustar offset para comenzar desde posición "miniplayer" en lugar de 0 |
| `PlayerViewModel.kt` | Modified | Agregar estado `playerSheetProgress` para controlar transición |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Incompatibilidad con LyricsScreen overlay | Media | LyricsScreen ya es conditional dentro de PlayerScreen, mantener estructura |
| Performance con animaciones durante swipe | Baja | Reutilizar Animatable existente, es eficiente |
| Estado de navegación durante transición | Baja | Mantener NavController sin cambios |

## Rollback Plan

1. Revertir cambios en MusicScreenUpdated.kt a la versión con Box + zIndex
2. Eliminar estado `playerSheetProgress` de PlayerViewModel si se agregó
3. Restaurar offsetY del PlayerScreen a comportamiento original (comienza en 0)

## Dependencies

- Ninguna dependencia externa nueva
- Requiere que la migración a Navigation Compose esté completa (ya lo está)

## Success Criteria

- [ ] PlayerScreen se abre deslizando desde abajo
- [ ] Swipe-down cierra PlayerScreen transicionando al MiniPlayer
- [ ] BottomNavigationBar permanece visible durante toda la transición
- [ ] LyricsScreen sigue funcionando dentro del PlayerScreen
- [ ] El gesto de cierre es suave (mínimo 60fps)
