# Tasks: BottomSheet Integration

## Phase 1: Foundation

- [x] 1.1 Agregar imports necesarios en `ui/MusicScreenUpdated.kt`:
  - `androidx.compose.material3.BottomSheetScaffold`
  - `androidx.compose.material3.rememberStandardBottomSheetState`
  - `androidx.compose.material3.SheetValue`
  - `androidx.compose.material3.BottomSheetScaffoldState`
  - `androidx.compose.material3.rememberBottomSheetScaffoldState`

- [x] 1.2 Actualizar `viewmodel/PlayerViewModel.kt`:
  - Agregar import `androidx.compose.material3.SheetValue`
  - Agregar `_bottomSheetState` con MutableStateFlow<SheetValue>
  - Agregar propiedad `bottomSheetState` como StateFlow
  - Agregar métodos `expandPlayer()`, `collapsePlayer()`, `hidePlayer()`

- [x] 1.3 Verificar que `ui/BottomSheetExtensions.kt` funcione con el nuevo estado

## Phase 2: Core Implementation

- [x] 2.1 Modificar `ui/MusicScreenUpdated.kt`:
  - Reemplazar `Box(modifier = Modifier.fillMaxSize())` por `BottomSheetScaffold`
  - Configurar `sheetState` con `rememberStandardBottomSheetState`
  - Configurar `sheetContent` para mostrar `PlayerScreen`
  - Configurar `sheetPeekHeight` (aproximadamente 84.dp)

- [x] 2.2 Integrar estado del ViewModel con el SheetState:
  - Observar `currentSong` del PlaybackViewModel
  - Sincronizar estado del BottomSheet según si hay canción
  - Usar LaunchedEffect para sincronización

- [x] 2.3 Ajustar `ui/PlayerScreen.kt`:
  - Agregar parámetro `onBack: () -> Unit` para manejar collapse
  - Remover gestures manuales que conflictúan con BottomSheet
  - Remover offsetY, detectVerticalDragGestures

- [x] 2.4 Manejar la integración del MiniPlayer:
  - El MiniPlayer ahora es el peekHeight del BottomSheet (84.dp)
  - Eliminar el MiniPlayer del Column principal (ya no está duplicado)

- [x] 2.5 Manejar BackHandler:
  - Cuando el BottomSheet está Expanded, back hace collapse
  - Configurado en PlayerScreen via onBack callback

## Phase 3: Integration

- [x] 3.1 Conectar Equalizer y Settings:
  - Verificar que EqualizerScreen siga funcionando como overlay (zIndex 3)
  - Verificar que SettingsScreen siga funcionando como overlay (zIndex 2)
  - Ajustar zIndex si es necesario

- [x] 3.2 Verificar navegación:
  - La navegación funciona dentro del Scaffold
  - BottomNavigationBar se mantiene visible

- [x] 3.3 Test de gestures:
  - Gestures manejados por BottomSheetScaffold automáticamente

## Phase 4: Testing & Verification

- [x] 4.1 Verificar escenario: Canción reproduciéndose al iniciar app
  - El MiniPlayer aparece como PartiallyExpanded (peekHeight)

- [x] 4.2 Verificar escenario: Tap en MiniPlayer
  - Debe expandir al PlayerScreen completo

- [x] 4.3 Verificar escenario: Swipe down desde PlayerScreen
  - Debe colapsar de vuelta al MiniPlayer

- [x] 4.4 Verificar escenario: Back desde PlayerScreen
  - Debe colapsar al MiniPlayer

- [x] 4.5 Verificar escenario: Sin canción reproduciéndose
  - El BottomSheet está Hidden

## Phase 5: Cleanup

- [x] 5.1 Eliminar código de overlay viejo (el overlay del PlayerScreen fue removido)
- [x] 5.2 Limpiar imports no utilizados en PlayerScreen.kt
- [x] 5.3 Verificar que no haya warnings de compilación

(End of file - total 95 lines)
