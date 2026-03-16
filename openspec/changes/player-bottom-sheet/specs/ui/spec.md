# Delta for UI - Player BottomSheet

## ADDED Requirements

### Requirement: PlayerScreen BottomSheet Behavior

El reproductor de música DEBE mostrarse como un BottomSheet que se desliza desde la parte inferior de la pantalla, en lugar de un overlay que cubre toda la pantalla con zIndex.

#### Scenario: Abrir reproductor desde MiniPlayer

- GIVEN El usuario está en cualquier pantalla de la app (Songs, Albums, Artists, Playlists) y el MiniPlayer está visible
- WHEN El usuario toca el MiniPlayer
- THEN El PlayerScreen DEBE desplegarse con una animación de slide-up desde la posición del MiniPlayer
- AND El BottomNavigationBar DEBE permanecer visible durante la apertura

#### Scenario: Cerrar reproductor con swipe-down

- GIVEN El usuario está en el PlayerScreen extendido
- WHEN El usuario hace swipe-down y suelta el gesto (drag > 25% de la altura de pantalla)
- THEN El PlayerScreen DEBE cerrarse animadamente hacia la posición del MiniPlayer
- AND El MiniPlayer DEBE volverse visible gradualmente a medida que el PlayerScreen se cierra
- AND El BottomNavigationBar DEBE permanecer visible durante toda la transición

#### Scenario: Cerrar reproductor parcialmente (swipe cancelado)

- GOUND El usuario está en el PlayerScreen extendido
- WHEN El usuario hace swipe-down pero suelta antes del 25% de la altura
- THEN El PlayerScreen DEBE volver a su posición extendida con animación
- AND El MiniPlayer DEBE permanecer oculto

### Requirement: MiniPlayer Visibility During Transition

El MiniPlayer DEBE mostrarse como estado intermedio visible durante la transición de cierre del PlayerScreen.

#### Scenario: Transición de cierre muestra MiniPlayer progresivamente

- GIVEN El usuario está cerrando el PlayerScreen con swipe-down
- WHEN El offset vertical del PlayerScreen supera el 50% de la altura del MiniPlayer
- THEN El MiniPlayer DEBE comenzar a ser visible
- AND El MiniPlayer DEBE mostrarse completamente cuando el PlayerScreen esté completamente cerrado

#### Scenario: MiniPlayer es funcional durante transición

- GIVEN El usuario está en proceso de cerrar el PlayerScreen
- WHEN El usuario toca los controles del MiniPlayer (play/pause, next)
- THEN Los controles DEBEN funcionar normalmente
- AND La transición DEBE cancelarse y el MiniPlayer DEBE mostrarse completamente

### Requirement: LyricsScreen Within BottomSheet

La pantalla de letras DEBE continuar funcionando correctamente dentro del contexto del BottomSheet.

#### Scenario: Mostrar letras mientras BottomSheet está extendido

- GIVEN El usuario está en el PlayerScreen extendido
- WHEN El usuario toca el botón de letras
- THEN La LyricsScreen DEBE mostrarse como overlay dentro del PlayerScreen
- AND La transición de cierre del PlayerScreen DEBE incluir la LyricsScreen

#### Scenario: Cerrar letras y reproductor secuencialmente

- GIVEN El usuario está en el PlayerScreen con LyricsScreen visible
- WHEN El usuario toca para cerrar letras (vuelve a PlayerScreen) Y luego hace swipe-down
- THEN Las letras DEBEN cerrarse primero
- AND Luego el PlayerScreen DEBE cerrarse hacia el MiniPlayer

## MODIFIED Requirements

### Requirement: PlayerScreen Overlay Behavior (Previously: Full-screen overlay with zIndex)

El PlayerScreen YA NO DEBE usar un Box con zIndex para dibujarse sobre todo el contenido. EN SU LUGAR, DEBE usar posicionamiento desde abajo (bottom) con offset negativo que se controla mediante gestures.

#### Scenario: Posicionamiento del PlayerScreen

- GIVEN El usuario abre el PlayerScreen
- THEN El PlayerScreen DEBE posicionarse comenzando desde el borde inferior de la pantalla
- AND El PlayerScreen DEBE expandirse hacia arriba hasta cubrir toda la pantalla excepto el área del BottomNavigationBar

### Requirement: BottomNavigationBar Visibility (Previously: Hidden when PlayerScreen is open)

El BottomNavigationBar DEBE permanecer visible incluso cuando el PlayerScreen está parcialmente abierto, a diferencia del comportamiento anterior donde se ocultaba completamente.

#### Scenario: BottomNavigation visible durante transición

- GIVEN El usuario está cerrando el PlayerScreen con swipe-down
- WHEN El PlayerScreen está entre 0% y 100% de cierre
- THEN El BottomNavigationBar DEBE permanecer visible en todo momento
- AND El MiniPlayer DEBE posicionarse entre el BottomNavigationBar y el contenido principal

## REMOVED Requirements

### Requirement: Full-screen zIndex Overlay (Reason: Replaced by BottomSheet positioning)

(Reason: El overlay con zIndex ya no es necesario porque el BottomSheet proporciona el mismo comportamiento de forma más elegante y con mejor UX)

### Requirement: Instant PlayerScreen Dismissal (Reason: Replaced by animated transition)

(Reason: El cierre instantáneo sin transición hacia MiniPlayer no proporciona feedback visual adecuado al usuario)
