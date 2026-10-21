# Local Player 🎵

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" alt="Local Player Logo" width="120"/>
</p>

<p align="center">
  <strong>Lightweight and modern local music player for Android</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-24%2B-brightgreen" alt="Android API"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-blue" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-%E2%9C%93-blue" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License"/>
</p>

---

## 📱 Features

- **🎵 Local Playback**: Access your music without needing an internet connection
- **📝 Lyrics Support**: View synchronized lyrics (LRC format) while playing
- **📋 Queue Management**: Organize and reorder your upcoming songs with ease
- **🔍 Advanced Search**: Quickly find songs by title or artist
- **🔀 Playback Modes**:
  - Shuffle
  - Repeat one song
  - Repeat all
- **📊 Audio Info**: View format (FLAC, MP3, etc.) and bitrate
- **🔄 Automatic Detection**: The app automatically detects newly added songs
- **🎨 Modern Interface**: Dark Material Design 3 UI
- **📱 Notifications**: Playback controls in the notification and lock screen
- **🎯 Miniplayer**: Quick control without leaving the library
- **📂 Sorting**: Sort by title (A-Z, Z-A) or artist

## 📸 Screenshots

<img style="display: block; margin: auto;" src = "https://github.com/user-attachments/assets/ca1434f1-34a2-4ad3-873e-0c9f3f7e8cce"/>

<img style="display: block; margin: auto;" src = "https://github.com/user-attachments/assets/6735b1d1-6df9-4808-9b70-04caa70108ff"/>

<img style="display: block; margin: auto;" src = "https://github.com/user-attachments/assets/bb7af540-edc9-44a3-8367-035b73e5655f"/>

<img style="display: block; margin: auto;" src = "https://github.com/user-attachments/assets/ba193088-5c18-45cd-b050-66c54b341842"/>

## 🛠️ Technologies

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Design**: Material Design 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Audio**: Android MediaPlayer
- **Persistence**: SharedPreferences & JSON Cache
- **Coroutines**: Kotlin Coroutines for asynchronous operations

## 📋 Requirements

- Android 7.0 (API 24) or higher
- Permissions:
  - `READ_MEDIA_AUDIO` (Android 13+)
  - `READ_EXTERNAL_STORAGE` (Android 12 or lower)
  - `FOREGROUND_SERVICE`
  - `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - `POST_NOTIFICATIONS`

## 🚀 Installation

### From Releases

1. Go to the [Releases](https://github.com/cvc953/localplayer/releases) section
2. Download the APK file for the latest version
3. Install the APK on your Android device
4. Open the app and grant the required permissions

### Build from Source

```bash
# Clone the repository
git clone https://github.com/cvc953/localplayer.git
cd localplayer

# Build with Gradle
./gradlew assembleRelease

# The APK will be generated in: app/build/outputs/apk/release/
```

## 💡 Usage

1. **First launch**:
   - Grant the media file access permission
   - The app will automatically scan your music library

2. **Play music**:
   - Tap any song in the list
   - Use the miniplayer for quick controls
   - Tap the miniplayer to open the full player

3. **Manage the queue**:
   - Long-press a song and drag it to reorder
   - Use the three-dot menu to add it to the queue

4. **View lyrics**:
   - Place `.lrc` or `.ttml` files with the same name as your song
   - In the player, tap the lyrics icon
   - Lyrics will synchronize automatically

5. **Update the library**:
   - The app automatically detects new songs
   - You can also refresh manually from the menu (⋮ → Refresh library)

## 📁 Project Structure

```
localplayer/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cvc953/localplayer/
│   │   │   │   ├── model/          # Data models
│   │   │   │   ├── ui/             # Compose screens
│   │   │   │   ├── viewmodel/      # ViewModels
│   │   │   │   ├── services/       # Background services
│   │   │   │   ├── util/           # Utilities
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/                # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/
│   └── build.gradle.kts
├── gradle/
├── .github/
│   └── workflows/                  # GitHub Actions
├── README.md
└── build.gradle.kts
```

## 🤝 Contributing

Contributions are welcome. Please:

1. Fork the project
2. Create a branch for your feature (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🐛 Reporting Issues

If you find a bug or have a suggestion, please open an [issue](https://github.com/cvc953/localplayer/issues).

## 📝 Roadmap

- [x] Built-in equalizer
- [x] Playlist support
- [x] Home screen widgets
- [x] Customizable themes
- [x] Specific folder scanning
- [x] Import/Export settings
- [x] Sleep timer

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## 👤 Author

**Cristian Villalobos** - [@cvc953](https://github.com/cvc953)

---