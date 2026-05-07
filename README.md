<h1 align="center">PokeSync Android</h1>

<p align="center">
    Android companion app for the <a href="https://github.com/gustavoarechigajr/PokeSync">PokeSync</a> self-hosted Pokémon save manager
</p>

<p align="center">
    <img src="https://img.shields.io/badge/Platform-Android%20API%2029%2B-green" />
    <img src="https://img.shields.io/badge/Language-Kotlin-orange" />
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue" />
    <img src="https://img.shields.io/badge/License-GPLv3-blue.svg" />
</p>

---

## What it does

PokeSync Android connects to your self-hosted PokeSync server, scans your device for emulator save files, uploads them for parsing with PKHeX, and lets you browse your full Pokémon roster — all from your phone.

## Features

- **Emulator scanner** — automatically finds save files from:
  - RetroArch
  - Azahar (Citra fork)
  - Eden (Yuzu fork) — including `Android/data/` via full file access
  - DraStic
  - My Boy!
  - ClassicBoy
- **Save registry** — add a save once, the app remembers it forever
- **One-tap sync** — tap Sync on any registered save to re-upload the latest version from your device
- **Browse Pokémon** — view your full box roster with species, level, shiny status, nature, and moves
- **Vault** — personal Pokémon bank decoupled from any specific save
- **Drag-and-drop transfer** between save and vault (or vault and save)
- **Cross-game format conversion** — PA8/PB8/PK8/PK9 etc., preserving moves and held items
- **Pre-transfer compatibility check** — blocks transfers the destination game can't render and surfaces a clear error
- **Held item display** in the summary panel
- **Save as backup** — every sync keeps a server-side copy, recoverable if your device file is lost
- **Manual file picker** — pick any save file via the system browser as a fallback
- **Secure auth** — JWT token stored in hardware-backed EncryptedSharedPreferences

## Requirements

- Android 10+ (API 29)
- A running [PokeSync server](https://github.com/gustavoarechigajr/PokeSync)
- `MANAGE_EXTERNAL_STORAGE` permission (required to read Eden's `Android/data/` directory)

## Setup

1. Install the APK on your device
2. Open the app and type your PokeSync server URL into the empty field (e.g. `http://10.0.0.10:5100` on LAN, or `https://pokesync.<your-domain>` for a Cloudflare-Tunnel setup)
3. Register or log in
4. Tap **Scan Emulators** to find your save files, or **Pick File Manually**
5. Tap **Sync** any time to push the latest save to the server
6. Tap **Browse** to view your Pokémon

## Tech stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Networking | Retrofit + OkHttp + Moshi |
| Image loading | Coil |
| Storage | DataStore + EncryptedSharedPreferences |
| Auth | JWT via EncryptedSharedPreferences (AES256-GCM) |

## Building from source

1. Clone the repository:
   ```bash
   git clone https://github.com/gustavoarechigajr/PokeSync-Android.git
   cd PokeSync-Android
   ```

2. Install **Android Studio** — its bundled JetBrains Runtime (JBR) provides JDK 17.

3. Set `JAVA_HOME` to the bundled JBR (or build from inside Android Studio):
   ```bash
   export JAVA_HOME=/path/to/android-studio/jbr
   # On Linux this is typically ~/android-studio/jbr
   # On macOS: /Applications/Android Studio.app/Contents/jbr/Contents/Home
   ```
   Make this permanent by adding the export to `~/.profile`.

4. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

5. Install on a connected device:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

Current version: **0.2.0**.

## Related

- [PokeSync Server](https://github.com/gustavoarechigajr/PokeSync) — the backend (forked from PKVault)
- [PKVault](https://github.com/Chnapy/PKVault) — original project by Chnapy

---

*Licensed under GPLv3*
