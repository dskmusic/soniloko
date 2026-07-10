# SoniLoko 🔴

**A skeuomorphic soundboard toy for Android.** Tap, record, download, remix — a pocket
"sound machine" with kits, a built-in editor, and a party game mode, 100% offline by design.

Built by [DSK Music](https://dskmusic.com) — free tools for musicians since 2002.

<p>
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white">
  <img alt="Android" src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white">
  <img alt="Min SDK" src="https://img.shields.io/badge/minSdk-26-blue">
  <img alt="PWA" src="https://img.shields.io/badge/Also-PWA-5A0FC8?logo=pwa&logoColor=white">
  <img alt="Personal project" src="https://img.shields.io/badge/license-Personal%20project-lightgrey">
</p>

<p>
  <img alt="Latest release" src="https://img.shields.io/github/v/release/dskmusic/soniloko?include_prereleases&label=latest%20release">
  <img alt="Build status" src="https://img.shields.io/github/actions/workflow/status/dskmusic/soniloko/check-newpipe.yml?label=auto-build">
</p>

---

## ✨ Features

### 🔴 The board
- 12-button skeuomorphic soundboard with a **speaker grille** that breathes and reacts to every tap
- Physical-feeling **press feedback** (scale, shadow, deepens further on long-press)
- Short **vibration** on tap (works even in silent/vibrate mode), toggleable
- **Swipe left/right** to switch kits, with the kit name flashing briefly in the speaker
- 6 dark color themes

### 🎛️ Edit mode
- Long-press any button (or "Customize sounds") to change its **icon** (searchable Font Awesome
  picker, ES/EN aliases), a **custom image** (with crop), or **custom text**
- Per-button **volume**
- Bundled kits plus your own **custom kits** (save, rename, delete)

### 🎙️ Sounds
- Record with the mic, import from the device, or **search and download online**
  (client-side extraction via NewPipeExtractor — no API key, no server)
- Optional **self-hosted download server** (reclip/yt-dlp) tried first, with automatic fallback
  to on-device extraction if it doesn't respond
- **Waveform view with playhead** and a start/end trim tool for every recorded, imported or
  downloaded sound — re-trim or rename any of them later
- Long-sound handling, simultaneous-sound control, sound preview toggle

### 🎮 Game mode
- "Simon says": repeat growing sequences of lit-up buttons, 3 lives, retro LED-style
  level/lives readout inside the speaker

### 📦 Everything else
- Export/import your whole configuration as `.json`
- Export/import your own sounds as a `.zip`
- **Self-updating** — checks GitHub Releases and installs new builds without the Play Store
- A companion **installable PWA** (mobile-friendly, offline-capable) sharing the same kits/sounds

---

## 🛠️ Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3), single-Activity, no XML layouts
- `SoundPool` + `MediaPlayer` hybrid audio engine, `Equalizer`/`LoudnessEnhancer` for the
  toy-speaker Fx
- `MediaExtractor`/`MediaMuxer`/`MediaCodec` for lossless trimming and waveform extraction
- **NewPipeExtractor** (+ OkHttp) for on-device online sound search/download
- `DataStore` for settings, plain `File` I/O for the `/SoniLoko` sound library
- Vanilla JS + Web Audio for the companion PWA — no framework, no build step
- **GitHub Actions** for scheduled NewPipeExtractor version checks and signed release builds

---

## 📦 Installation

Download the latest APK from the [Releases](../../releases) page. The app checks for updates
automatically on launch, or manually from **Settings → Check for updates**.

See `manual_github.md` (inside `app/src/main/assets/`) for how the auto-update pipeline is set
up, and `assets/web/manual.md` for deploying the companion PWA to your own server.

---

*Made with ❤️ by DSK.*
