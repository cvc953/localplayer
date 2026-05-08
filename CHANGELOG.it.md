# Changelog

Tutte le modifiche rilevanti di questo progetto saranno documentate in questo file.

Il formato si basa su [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
e questo progetto aderisce al [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Aggiunto
- 🌐 Localizzazione completa in spagnolo, inglese e italiano in tutta l'app
- 🗣️ Gestione globale della preferenza della lingua da MainActivity e SettingsScreen
- 📂 Flusso dei permessi di archiviazione e selezione cartella adattati per l'i18n
- 🖼️ Screenshot aggiornati per le varianti pubblicate in inglese
- 📋 Operazioni di coda estese per aggiungere tutti i brani senza duplicati e mantenere la riproduzione successiva

### Modificato
- ⚙️ Refactor di MainActivity, SettingsScreen e varie schermate principali per supportare l'i18n
- 🎨 Pulizia delle stringhe hardcoded in album, artisti, playlist, testi, equalizzatore e schermate di dettaglio
- 📱 Aggiustamenti alla navigazione e ai componenti UI per un'esperienza coerente tra le lingue

### Corretto
- 🐛 Correzione dei toast, dei conteggi dei brani e delle etichette delle azioni ancora da localizzare
- 🐛 Prevenzione dei duplicati quando si riproduce il brano successivo in coda

### Tecnico
- Implementazione di `LocaleUtil` e persistenza della lingua in `AppPrefs`
- Migliorati i test della logica di coda in `PlaybackQueueLogicTest`

## [1.0.7] - 2026-05-03

### Aggiunto
- 🇪🇸🇺🇸 Localizzazione completa in inglese e spagnolo con metadati e immagini
- 📲 Icone dell'app aggiornate per inglese e spagnolo
- 🍞 Toast per le azioni di coda in Albums, Artists, Playlists e MusicScreen
- 👆 Azione swipe-left in DraggableSwipeRow per aggiungere i brani alla fine della coda
- 📂 Migliorate MusicScreen, PlaylistsScreen, PlaylistDetailScreen e SettingsScreen
- 📋 Fastlane spostato nella root del repository per compatibilità con i manutentori di F-Droid

### Modificato
- ⚙️ Refactor di MusicScreen e PlaylistsScreen con funzionalità avanzate
- 🎨 UI delle schermate migliorata con funzionalità aggiuntive

### Corretto
- 🐛 Inclusione dei metadati delle dipendenze disabilitata in APK e bundle
- 🐛 Aggiornati i messaggi toast per una coerenza nell'aggiunta alla coda
- 🐛 Ottimizzati gli aggiornamenti di progresso e la sincronizzazione della posizione dei testi
- 🐛 Regolata la durata delle animazioni dei gesti di trascinamento
- 🐛 Corretta l'opacità del colore dei testi inattivi
- 🐛 Corretto il rendering del messaggio toast in ArtistDetailScreen
- 🐛 Corretto un bug che faceva collassare tra loro i testi giapponesi

### Tecnico
- 🏗️ ViewModel dedicati per le funzionalità avanzate delle schermate

## [1.0.5] - 2026-03-25

### Aggiunto
- 🎨 Selezione dinamica del colore di accento nella schermata impostazioni
- 📱 Screenshot dell'app per Play Store (album, libreria, testi, player) in inglese e spagnolo
- 📄 Metadati Fastlane per la pubblicazione su Play Store (EN e ES)

### Modificato
- 🎨 Refactor della gestione dei temi per supportare colori di accento personalizzati
- 💾 Persistenza del colore di accento selezionato nelle preferenze

### Corretto
- 🐛 Il parser TTML considerava come una sola parola più parole separate da `-`

### Tecnico
- Documentazione Openspec per l'integrazione bottom-sheet (pianificazione)

## [1.0.4] - 2026-03-23

### Aggiunto
- 🎵 Supporto ai testi TTML (sillabe sincronizzate, word-by-word, animazioni a punti per i gap strumentali)
- 🎛️ Equalizzatore integrato con gestione dei preset utente e persistenza dello stato
- 📂 Gestione delle cartelle musicali tramite FolderViewModel
- 🎤 Supporto per le voci secondarie nei testi sincronizzati
- 🔤 Scroller alfabetico nelle schermate album, artisti e brani
- 🎨 Colori dinamici del player e dei testi basati sulla luminanza dello sfondo
- ⚙️ Toggle dei colori dinamici nella schermata impostazioni
- 🔄 Auto-scan della libreria con debounce per evitare scan non necessari
- 💿 Brani caricati raggruppati per album e artista (AlbumViewModel)
- ▶️ Riproduzione diretta per artista (playArtist)
- 🔁 Modalità repeat sincronizzata tra PlayerController e PlaybackViewModel
- 🔊 Gestione dell'audio focus nel player
- 📋 Preferenze di ordinamento delle playlist persistite
- 📦 Esportazione e importazione delle playlist da e verso file JSON
- ➕ Creazione di nuove playlist e aggiunta di brani dal dialog del player
- ⭐ Funzionalità preferiti per i brani
- 🔍 Menu a tendina in album e artisti per le opzioni di riproduzione
- 🎵 Track number e disc number nel modello dati dei brani

### Modificato
- 🧭 Architettura di navigazione refactorizzata a Navigation Compose
- 🖥️ UI del player completamente ridisegnata
- 📊 Logica di riproduzione delegata a PlaybackViewModel per un'architettura più pulita
- 🎛️ Equalizzatore refactorizzato in un EqualizerViewModel dedicato
- 🎨 Sistema dei temi migliorato con ExtendedColors e gestione coerente dei colori
- ⚙️ Schermata impostazioni refactorizzata con toggle per equalizzatore e colori dinamici
- 🎯 Gestione del drag nelle liste di brani migliorata per maggiore reattività
- 📱 Gestione della status bar refactorizzata in MainActivity

### Corretto
- 🐛 Calcolo della durata dei gap nell'animazione a punti dei testi
- 🐛 Errori di compilazione Kotlin e miglioramenti della reattività della UI
- 🐛 Gestione del rapporto d'aspetto nella schermata del player
- 🐛 Scroll dei testi che non si centrava correttamente sulla posizione corrente
- 🐛 Inizializzazione dell'equalizzatore al cambio di sessione audio
- 🐛 Normalizzazione dei nomi degli album per un matching migliore

### Eliminato
- 🗑️ Dipendenze non utilizzate di Firebase Crashlytics ed ExoPlayer
- 🗑️ Codice commentato e componenti Spacer non necessari

### Tecnico
- ViewModel dedicati: Artist, Equalizer, Folder, Lyrics, Playback, Player, Playlist, Settings
- Parser TTML con supporto alle sillabe continue e unione delle parole tra righe
- Auto-scan della libreria nei ViewModel con debounce
- Estrazione automatica del changelog nel workflow GitHub Actions

## [1.0.0] - 2026-01-25

### Aggiunto
- 🎵 Riproduzione musicale locale
- 📝 Supporto ai testi sincronizzati (formato LRC)
- 📋 Gestione della coda di riproduzione con riordino
- 🔍 Ricerca per titolo e artista
- 🔀 Modalità casuale (shuffle)
- 🔁 Modalità repeat (una canzone, tutte)
- 📊 Visualizzazione del formato audio e del bitrate
- 🔄 Rilevamento automatico dei nuovi brani
- 🎨 Interfaccia moderna con Material Design 3
- 📱 Controlli nella notifica e nella schermata di blocco
- 🎯 Miniplayer per un controllo rapido
- 📂 Ordinamento dei brani (A-Z, Z-A, per artista)
- ℹ️ Schermata Informazioni con dettagli sull'app
- 🔄 Aggiornamento manuale della libreria
- 📱 Supporto da Android 7.0 (API 24)

### Tecnico
- Architettura MVVM
- Jetpack Compose per la UI
- Kotlin Coroutines per le operazioni asincrone
- ContentObserver per rilevare i cambiamenti nella libreria
- Cache JSON per un caricamento rapido
- MediaSession per i controlli multimediali di sistema

---

## Formato del Changelog

### Tipi di modifica
- `Aggiunto` per nuove funzionalità
- `Modificato` per cambiamenti a funzionalità esistenti
- `Deprecato` per funzionalità che saranno rimosse
- `Eliminato` per funzionalità rimosse
- `Corretto` per correzioni di bug
- `Sicurezza` per correzioni di vulnerabilità

[Unreleased]: https://github.com/cvc953/localplayer/compare/v1.0.7...HEAD
[1.0.7]: https://github.com/cvc953/localplayer/compare/v1.0.5...v1.0.7
[1.0.5]: https://github.com/cvc953/localplayer/compare/v1.0.4...v1.0.5
[1.0.4]: https://github.com/cvc953/localplayer/compare/v1.0.0...v1.0.4
[1.0.0]: https://github.com/cvc953/localplayer/releases/tag/v1.0.0