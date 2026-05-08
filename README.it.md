# Local Player 🎵

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Logo di Local Player" width="120"/>
</p>

<p align="center">
  <strong>Un lettore musicale locale leggero e moderno per Android</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-24%2B-brightgreen" alt="Android API"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-blue" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-%E2%9C%93-blue" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License"/>
</p>

---

## 📱 Funzionalità

- **🎵 Riproduzione locale**: accedi alla tua musica senza bisogno di internet
- **📝 Supporto testi**: visualizza testi sincronizzati (formato LRC) durante la riproduzione
- **📋 Gestione coda**: organizza e riordina facilmente i brani successivi
- **🔍 Ricerca avanzata**: trova rapidamente i brani per titolo o artista
- **🔀 Modalità di riproduzione**:
  - Shuffle
  - Ripeti un brano
  - Ripeti tutto
- **📊 Info audio**: visualizza formato (FLAC, MP3, ecc.) e bitrate
- **🔄 Rilevamento automatico**: l'app rileva automaticamente i nuovi brani aggiunti
- **🎨 Interfaccia moderna**: UI scura con Material Design 3
- **📱 Notifiche**: controlli di riproduzione nella notifica e nella schermata di blocco
- **🎯 Miniplayer**: controllo rapido senza uscire dalla libreria
- **📂 Ordinamento**: ordina per titolo (A-Z, Z-A) o artista

## 📸 Screenshot

<img style="display: block; margin: auto;" src = "https://github.com/user-attachments/assets/ca1434f1-34a2-4ad3-873e-0c9f3f7e8cce"/>

<img style="display: block; margin: auto;" src = "https://github.com/user-attachments/assets/6735b1d1-6df9-4808-9b70-04caa70108ff"/>

<img style="display: block; margin: auto;" src = "https://github.com/user-attachments/assets/bb7af540-edc9-44a3-8367-035b73e5655f"/>

<img style="display: block; margin: auto;" src = "https://github.com/user-attachments/assets/ba193088-5c18-45cd-b050-66c54b341842"/>

## 🛠️ Tecnologie

- **Linguaggio**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Design**: Material Design 3
- **Architettura**: MVVM (Model-View-ViewModel)
- **Audio**: Android MediaPlayer
- **Persistenza**: SharedPreferences e cache JSON
- **Coroutines**: Kotlin Coroutines per operazioni asincrone

## 📋 Requisiti

- Android 7.0 (API 24) o superiore
- Permessi:
  - `READ_MEDIA_AUDIO` (Android 13+)
  - `READ_EXTERNAL_STORAGE` (Android 12 o inferiore)
  - `FOREGROUND_SERVICE`
  - `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - `POST_NOTIFICATIONS`

## 🚀 Installazione

### Dalle release

1. Vai alla sezione [Releases](https://github.com/cvc953/localplayer/releases)
2. Scarica il file APK dell'ultima versione
3. Installa l'APK sul tuo dispositivo Android
4. Apri l'app e concedi i permessi richiesti

### Compilare dal sorgente

```bash
# Clona il repository
git clone https://github.com/cvc953/localplayer.git
cd localplayer

# Compila con Gradle
./gradlew assembleRelease

# L'APK verrà generato in: app/build/outputs/apk/release/
```

## 💡 Utilizzo

1. **Primo avvio**:
   - Concedi il permesso di accesso ai file multimediali
   - L'app eseguirà automaticamente la scansione della tua libreria musicale

2. **Riprodurre musica**:
   - Tocca qualsiasi brano nell'elenco
   - Usa il miniplayer per controlli rapidi
   - Tocca il miniplayer per aprire il player completo

3. **Gestire la coda**:
   - Tieni premuto un brano e trascinalo per riordinarlo
   - Usa il menu con i tre puntini per aggiungerlo alla coda

4. **Vedere i testi**:
   - Inserisci file `.lrc` o `.ttml` con lo stesso nome del brano
   - Nel player, tocca l'icona dei testi
   - I testi si sincronizzeranno automaticamente

5. **Aggiornare la libreria**:
   - L'app rileva automaticamente i nuovi brani
   - Puoi anche aggiornare manualmente dal menu (⋮ → Aggiorna libreria)

## 📁 Struttura del progetto

```
localplayer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cvc953/localplayer/
│   │   │   │   ├── model/          # Modelli dati
│   │   │   │   ├── ui/             # Schermate Compose
│   │   │   │   ├── viewmodel/      # ViewModel
│   │   │   │   ├── services/       # Servizi in background
│   │   │   │   ├── util/           # Utility
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/                # Risorse
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
├── .github/
│   └── workflows/                  # GitHub Actions
├── README.md
└── build.gradle.kts
```

## 🤝 Contribuire

Le contribuzioni sono benvenute. Per favore:

1. Fai un fork del progetto
2. Crea un branch per la tua feature (`git checkout -b feature/AmazingFeature`)
3. Commetti le tue modifiche (`git commit -m 'Add some AmazingFeature'`)
4. Fai push del branch (`git push origin feature/AmazingFeature`)
5. Apri una Pull Request

## 🐛 Segnalare problemi

Se trovi un bug o hai un suggerimento, apri una [issue](https://github.com/cvc953/localplayer/issues).

## 📝 Roadmap

- [x] Equalizzatore integrato
- [x] Supporto alle playlist
- [ ] Widget per la schermata Home
- [x] Temi personalizzabili
- [x] Scansione di cartelle specifiche
- [ ] Importazione/Esportazione impostazioni
- [ ] Sleep timer

## 📄 Licenza

Questo progetto è distribuito con licenza MIT. Consulta il file [LICENSE](LICENSE) per maggiori dettagli.

## 👤 Autore

**Cristian Villalobos** - [@cvc953](https://github.com/cvc953)

---