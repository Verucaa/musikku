# 🎵 LiPhify — `com.zaaam.liphify`

[![build-release](https://github.com/az925-crypto/Liphify-/actions/workflows/build-release.yml/badge.svg)](https://github.com/az925-crypto/Liphify-/actions/workflows/build-release.yml)
![minSdk 33](https://img.shields.io/badge/minSdk-33-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple)
![Compose](https://img.shields.io/badge/Compose-Material3-orange)
![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-red)

Player musik hybrid ala Apple Music: **lagu lokal + trending & streaming
YouTube Music dalam satu app, satu UI.** Sideload pribadi — rilis otomatis
tiap push lewat GitHub Actions.

## ✨ Fitur

| | |
|---|---|
| 📁 Library folder-scoped | Scan **hanya** `Music/LiPhify` — ringtone & suara WA tidak ikut |
| 🔥 Trending di Home | Kiosk Trending YouTube → fallback query, selalu ada isi |
| 🔍 Unified search | Lokal (instant) + YouTube (async), scope Semua/Perangkat/YouTube |
| ▶️ Satu engine | Media3 untuk semua sumber; URL YouTube di-resolve ulang tiap play |
| 📃 Queue + playlist | Campur lokal & YouTube, persist restart, Favorit via ☆ |
| 💬 Lirik synced | LRCLIB, highlight ikut posisi lagu (PRD-101 ✅) |
| 🌃 Liquid Glass | Haze blur, rim-light, motion `pressable`/`appear` |

## 🚀 Mulai 5 menit

```sh
git clone https://github.com/az925-crypto/Liphify-.git
# Android Studio → sync → Run ▶
```

1. Taruh MP3 di `Music/LiPhify` → tab Library → **Refresh**
2. Tab Home langsung hidup dari Trending (butuh internet)
3. Ketuk lagu → mini-player → geser ke atas → Now Playing

> Home kosong? Baca [Troubleshooting](#-troubleshooting) dulu sebelum
> buka issue.

## 📦 Ambil APK (jangan build di HP)

1. Push ke `main` → Actions build + sign + publish **otomatis**
2. Tab **Release** → `LiPhify-v1.0.0-<commit>-signed.apk` (selalu 1 file terbaru)
3. Install via LADB / transfer file ke OPPO A60

Signing dari Secrets. Tanpa secrets = unsigned = tidak bisa diinstal.

## 🛠 Stack

Kotlin 2.0 · Compose M3 · Media3 1.9 · Room v4 · Hilt · NewPipeExtractor
v0.26.5 · Coil · Haze · LRCLIB — single-module, `minSdk 33`, `target 35`.

```text
ui/ → ViewModel → repository → Room / NewPipeExtractor (via data/youtube SAJA)
              ↘ MediaController → LiPhifySessionService (ExoPlayer)
```

## 🗺 Peta

```text
app/src/main/java/com/zaaam/liphify/
├── MainActivity.kt     # tab Home/New/Library + search, Haze, BackHandler
├── playback/           # MediaSessionService  domain/  # Track, PlaybackSource
├── data/local/         # Room v4 + scanner folder-scoped + migrasi 1→4
├── data/youtube/       # SATU-SATUNYA pemanggil NewPipe
├── data/…Repository    # MusicRepository, LyricsRepository
└── ui/ theme|common|home|browse|library|search|playlist|player|nav/
```

## 🧭 Keputusan final (jangan dibalik diam-diam)

Scan cuma `Music/LiPhify` · Media3 tanpa BOM pin `1.9.0` · target 35 ·
tanpa Radio/Download/Login · repo publik = ikut copyleft **GPL-3.0**.

## 🐞 Troubleshooting

| Gejala | Penyebab paling mungkin | Fix |
|---|---|---|
| Home kosong | DB kosong (scan 0) / offline | `Music/LiPhify` ada isinya? Refresh → cek `scan: N lagu` |
| Force close | Lihat log dulu | `adb logcat -d \| grep -A25 'FATAL EXCEPTION: main'` |
| Lagu tidak bunyi | URL basi / file hilang | Tap lagi (auto re-resolve); scan ulang bila file pindah |
| Tidak bisa install | APK unsigned | Pastikan Secrets keystore terisi |

## 📄 Lisensi

**GPL-3.0** — file [`LICENSE`](LICENSE). Karena pakai NewPipeExtractor
(GPL), source repo publik ini ikut copyleft: bebas dipakai/dimodifikasi,
wajib tetap terbuka. Klaim sebagai karya tertutup = pelanggaran lisensi.

## 📚 Dokumen

[**ARCHITECTURE**](docs/ARCHITECTURE.md) · [**CONTRIBUTING**](docs/CONTRIBUTING.md) · [**UI.md**](UI.md)
