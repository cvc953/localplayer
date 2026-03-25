# Delta for UI - BottomSheet Integration

## ADDED Requirements

### Requirement: BottomSheetScaffold for Player

El reproductor de música DEBE mostrarse como un BottomSheet que se integra naturalmente con la estructura de la aplicación, en lugar de un overlay flotante con zIndex.

#### Scenario: BottomSheet configurado con MiniPlayer como peek height

- GIVEN La aplicación está en cualquier pantalla (Songs, Albums, Artists, Playlists)
- WHEN El MiniPlayer está visible y el usuario no ha interactuado con él
- THEN El BottomSheet DEBE estar en estado collapsed
- AND DEBE mostrar únicamente el MiniPlayer con altura de 72dp
- AND El BottomNavigationBar DEBE ser visible debajo del MiniPlayer

#### Scenario: BottomSheet expandido muestra PlayerScreen completo

- GIVEN El BottomSheet está en estado collapsed (MiniPlayer visible)
- WHEN El usuario toca el MiniPlayer
- THEN El BottomSheet DEBE expandirse con animación slide-up
- AND DEBE mostrar el PlayerScreen completo (controles, artwork, info de canción)
- AND El BottomNavigationBar DEBE permanecer visible

### Requirement: Animación de Corner Radius Interpolado

El BottomSheet DEBE animar las esquinas superiores de forma fluida entre los estados collapsed y expanded.

#### Scenario: Corner radius cambia según progreso del BottomSheet

- GIVEN El BottomSheet está en cualquier estado entre collapsed y expanded
- WHEN El progreso del BottomSheet cambia
- THEN El corner radius superior DEBE interpolarse linealmente
- AND DEBE ser 30dp cuando está completamente collapsed
- AND DEBE ser 0dp cuando está completamente expandido

### Requirement: Transición Suave Entre Estados

El cierre del reproductor DEBE mostrar una transición fluida hacia el MiniPlayer en lugar de desaparecer instantáneamente.

#### Scenario: Swipe-down cierra hacia MiniPlayer

- GIVEN El BottomSheet está en estado expandido (PlayerScreen visible)
- WHEN El usuario hace swipe-down y suelta el gesto (drag > 25% de la altura de pantalla)
- THEN El BottomSheet DEBE cerrarse animadamente hacia el estado collapsed
- AND El MiniPlayer DEBE volverse visible gradualmente
- AND La animación DEBE ser suave (mínimo 60fps)

#### Scenario: Swipe parcial vuelve a estado expandido

- GIVEN El BottomSheet está en estado expandido
- WHEN El usuario hace swipe-down pero suelta antes del 25% de la altura
- THEN El BottomSheet DEBE volver a su posición expandida con animación
- AND El MiniPlayer DEBE permanecer oculto

#### Scenario: Gesto swipe-up expande el BottomSheet

- GIVEN El BottomSheet está en estado collapsed (MiniPlayer visible)
- WHEN El usuario hace swipe-up
- THEN El BottomSheet DEBE expandirse mostrando el PlayerScreen
- AND La animación DEBE ser suave y continua

### Requirement: Controles del MiniPlayer Funcionales

Los controles de play/pause y next DEBEN funcionar incluso durante la transición del BottomSheet.

#### Scenario: Play/Pause durante transición

- GIVEN El BottomSheet está en proceso de transición (abriendo o cerrando)
- WHEN El usuario toca el botón de play/pause en el MiniPlayer
- THEN La acción DEBE ejecutarse correctamente
- AND La transición DEBE cancelarse si está en progreso
- AND El estado de reproducción DEBE reflejarse en el MiniPlayer

#### Scenario: Next song durante transición

- GIVEN El BottomSheet está en proceso de transición
- WHEN El usuario toca el botón de next en el MiniPlayer
- THEN DEBE reproducirse la siguiente canción
- AND La transición DEBE cancelarse si está en progreso

### Requirement: LyricsScreen Dentro del BottomSheet

La pantalla de letras DEBE continuar funcionando correctamente dentro del contexto del BottomSheet.

#### Scenario: Mostrar letras mientras BottomSheet está expandido

- GIVEN El BottomSheet está en estado expandido (PlayerScreen visible)
- WHEN El usuario toca el botón de letras
- THEN La LyricsScreen DEBE mostrarse como parte del PlayerScreen
- AND La transición de cierre del BottomSheet DEBE incluir la LyricsScreen

#### Scenario: Cerrar letras y reproductor secuencialmente

- GIVEN El BottomSheet está expandido con LyricsScreen visible
- WHEN El usuario toca para cerrar letras Y luego hace swipe-down
- THEN Las letras DEBEN cerrarse primero
- AND Luego el BottomSheet DEBE cerrarse hacia el MiniPlayer

### Requirement: BottomNavigationBar Visible Durante Transición

El BottomNavigationBar DEBE permanecer visible en todo momento, incluso cuando el BottomSheet está en transición.

#### Scenario: BottomNavigation visible durante animación de apertura

- GIVEN El BottomSheet está collapsed y el MiniPlayer es visible
- WHEN El usuario toca el MiniPlayer para expandir
- THEN El BottomNavigationBar DEBE permanecer visible durante toda la animación
- AND NO DEBE ocultarse en ningún momento

#### Scenario: BottomNavigation visible durante animación de cierre

- GIVEN El BottomSheet está expandido
- WHEN El usuario hace swipe-down para cerrar
- THEN El BottomNavigationBar DEBE permanecer visible durante toda la animación
- AND El MiniPlayer DEBE posicionarse visible entre el BottomSheet y el BottomNavigationBar

## MODIFIED Requirements

### Requirement: PlayerScreen Overlay Behavior (Previously: Full-screen overlay with zIndex)

El PlayerScreen YA NO DEBE usar un Box con zIndex para dibujarse sobre todo el contenido. EN SU LUGAR, DEBE usar el sistema de BottomSheetScaffold.

#### Scenario: PlayerScreen como contenido del BottomSheet

- GIVEN La aplicación está mostrando el BottomSheet en estado expandido
- THEN El PlayerScreen DEBE renderizarse como contenido del sheetContent
- AND DEBE ocupar toda el área visible del BottomSheet
- AND Los gestos DEBEN ser manejados por el BottomSheetScaffold

### Requirement: MiniPlayer Position (Previously: Fixed above BottomNavigationBar)

El MiniPlayer YA NO DEBE estar posicionado manualmente entre el contenido y el BottomNavigationBar. EN SU LUGAR, DEBE mostrarse automáticamente cuando el BottomSheet está en estado collapsed.

#### Scenario: MiniPlayer mostrado por BottomSheet collapsed

- GIVEN El BottomSheet está en estado collapsed
- THEN El MiniPlayer DEBE mostrarse automáticamente
- AND DEBE tener la altura configurada en sheetPeekHeight (72dp)
- AND DEBE estar posicionado correctamente por el BottomSheetScaffold

## REMOVED Requirements

### Requirement: Full-screen zIndex Overlay (Reason: Replaced by BottomSheet positioning)

(Reason: El overlay con zIndex ya no es necesario porque el BottomSheet proporciona el mismo comportamiento de forma más elegante y con mejor UX)

### Requirement: Instant PlayerScreen Dismissal (Reason: Replaced by animated transition)

(Reason: El cierre instantáneo sin transición hacia MiniPlayer no proporciona feedback visual adecuado al usuario)
