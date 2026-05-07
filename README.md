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
- **Save as backup** — every sync keeps a server-side copy, recoverable if your device file is lost
- **Manual file picker** — pick any save file via the system browser as a fallback
- **Secure auth** — JWT token stored in hardware-backed EncryptedSharedPreferences

## Requirements

- Android 10+ (API 29)
- A running [PokeSync server](https://github.com/gustavoarechigajr/PokeSync)
- `MANAGE_EXTERNAL_STORAGE` permission (required to read Eden's `Android/data/` directory)

## Setup

1. Install the APK on your device
2. Open the app and enter your PokeSync server URL
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

## Related

- [PokeSync Server](https://github.com/gustavoarechigajr/PokeSync) — the backend (forked from PKVault)
- [PKVault](https://github.com/Chnapy/PKVault) — original project by Chnapy

---

*Licensed under GPLv3*
