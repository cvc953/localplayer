# Tasks: Player como BottomSheet con Transición a MiniPlayer

## Phase 1: Foundation

- [ ] 1.1 Revisar estado actual del PlayerViewModel y agregar estado para `playerDismissProgress` (opcional: puede manejarse via callback)
- [ ] 1.2 Documentar la altura del MiniPlayer (~64dp con padding 12dp*2 + content) para cálculos de transición

## Phase 2: Core Implementation

- [ ] 2.1 Modificar MusicScreenUpdated.kt: Cambiar estructura de Box/Z-index a Column con posicionamiento dinámico del PlayerScreen
- [ ] 2.2 Agregar callback `onDismissProgress` al PlayerScreen que notifique el progreso de cierre (0.0 = abierto, 1.0 = cerrado)
- [ ] 2.3 Modificar PlayerScreen.kt: Ajustar offsetY para que el punto de partida sea la posición del MiniPlayer (offset negativo desde abajo)
- [ ] 2.4 Implementar lógica de transición en MusicScreenUpdated: mostrar/ocultar MiniPlayer basado en el progreso del gesto
- [ ] 2.5 Asegurar que el BottomNavigationBar permanezca visible durante toda la transición

## Phase 3: Integration

- [ ] 3.1 Conectar el callback onDismissProgress desde MusicScreenUpdated al PlayerScreen
- [ ] 3.2 Verificar que los controles del MiniPlayer funcionen durante la transición
- [ ] 3.3 Verificar que LyricsScreen siga funcionando dentro del PlayerScreen

## Phase 4: Testing / Verification

- [ ] 4.1 Test: Abrir reproductor desde MiniPlayer - animación slide-up
- [ ] 4.2 Test: Swipe-down cierra reproductor transicionando a MiniPlayer
- [ ] 4.3 Test: Swipe parcial vuelve a abrir reproductor
- [ ] 4.4 Test: BottomNavigationBar visible durante transición
- [ ] 4.5 Test: Controles de MiniPlayer funcionan durante transición

## Phase 5: Cleanup

- [ ] 5.1 Eliminar comentarios de debug o código temporal
- [ ] 5.2 Verificar que build compile sin warnings
