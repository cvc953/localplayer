# Tasks: Integrar BottomSheet para MiniPlayer-PlayerScreen

## Phase 1: Foundation (Infraestructura)

- [x] 1.1 Crear archivo `ui/BottomSheetExtensions.kt` con extensión `currentFraction` para BottomSheetScaffoldState
- [x] 1.2 Verificar que las importaciones de material3 (`BottomSheetScaffold`, `BottomSheetValue`, `ExperimentalMaterial3Api`) estén disponibles en el proyecto
- [x] 1.3 Documentar la altura actual del MiniPlayer (aproximadamente 72dp) para verificar sheetPeekHeight

## Phase 2: Core Implementation (Implementación Principal)

- [x] 2.1 Modificar `ui/MusicScreenUpdated.kt`: reemplazar estructura Box+zIndex por BottomSheetScaffold
- [x] 2.2 Agregar imports necesarios: BottomSheetScaffold, BottomSheetValue, rememberBottomSheetState, rememberBottomSheetScaffoldState, RoundedCornerShape
- [x] 2.3 Crear scaffoldState con BottomSheetState inicial en estado Collapsed
- [x] 2.4 Configurar sheetPeekHeight = 72.dp en BottomSheetScaffold
- [x] 2.5 Calcular radius interpolado: `val radius = (30 * scaffoldState.currentFraction).dp`
- [x] 2.6 Configurar sheetShape con RoundedCornerShape(topStart = radius, topEnd = radius)
- [x] 2.7 Definir sheetContent con lógica condicional: si isCollapsed → MiniPlayer, sino → PlayerScreen

## Phase 3: MiniPlayer Integration

- [x] 3.1 Modificar `ui/MiniPlayer.kt`: agregar parámetro `onExpand: () -> Unit`
- [x] 3.2 En MusicScreenUpdated: pasar onExpand al MiniPlayer que llame a `scaffoldState.bottomSheetState.expand()`
- [x] 3.3 Verificar que MiniPlayer tenga sufiiente altura (72dp) dentro del sheet collapsed

## Phase 4: PlayerScreen Integration

- [x] 4.1 Mover la llamada de PlayerScreen dentro del sheetContent del BottomSheetScaffold
- [x] 4.2 Remover los gestos manuales de offsetY en PlayerScreen (ya que BottomSheetScaffold maneja swipe)
- [x] 4.3 Ajustar padding de PlayerScreen si es necesario para que se vea bien dentro del sheet
- [x] 4.4 Verificar que onBack en PlayerScreen llame a `scaffoldState.bottomSheetState.collapse()` en lugar de cerrar overlay

## Phase 5: LyricsScreen Verification

- [x] 5.1 Verificar que LyricsScreen siga funcionando dentro del PlayerScreen/BottomSheet
- [x] 5.2 Probar que el cierre de letras y el cierre del BottomSheet funcionen secuencialmente

## Phase 6: Navigation & Settings Integration

- [x] 6.1 Verificar que EqualizerScreen y SettingsScreen sigan funcionando como overlays (tienen zIndex mayor)
- [x] 6.2 Verificar que la navegación (navController) funcione correctamente con el BottomSheet

## Phase 7: Testing / Verification

- [ ] 7.1 Test: Abrir reproductor desde MiniPlayer - animación slide-up funciona
- [ ] 7.2 Test: Swipe-down cierra reproductor transicionando a MiniPlayer
- [ ] 7.3 Test: Swipe parcial vuelve a abrir reproductor
- [ ] 7.4 Test: BottomNavigationBar visible durante toda la transición
- [ ] 7.5 Test: Controles de MiniPlayer (play/pause, next) funcionan durante transición
- [ ] 7.6 Test: Corner radius anima de 0dp (expandido) a 30dp (collapsed)
- [ ] 7.7 Test: LyricsScreen funciona dentro del BottomSheet

## Phase 8: Cleanup

- [x] 8.1 Eliminar código de gestos offsetY原来的 de PlayerScreen si ya no se usa
- [x] 8.2 Eliminar imports no usados en MusicScreenUpdated (zIndex, offset si ya no se necesitan)
- [x] 8.3 Compilar y verificar que no haya warnings

## Implementation Notes

### Dependencias entre tareas
- Phase 1 debe completarse antes de Phase 2
- Phase 2 debe completarse antes de Phase 3 y 4
- Phase 5-7 pueden hacerse en paralelo después de Phase 2-4

### Puntos de atención
- El MiniPlayer necesita un `onExpand` separado del `onClick` existente (que actualmente navega a lyrics)
- El gesto de swipe del BottomSheetScaffold debe funcionar automáticamente sin código adicional
- El onBack del PlayerScreen debe hacer collapse() no closePlayerScreen()
