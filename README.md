# LiPhify — `com.zaaam.liphify`

Player musik hybrid: lagu lokal (khusus folder `Music/LiPhify`) + streaming
YouTube Music (NewPipeExtractor), UI ala Apple Music. Sideload pribadi,
rilis otomatis via GitHub Actions.

## Stack 30 detik

Kotlin 2.0 + Compose Material3 + Media3 1.9 + Room 4 + Hilt + NewPipeExtractor
v0.26.5 + Coil + Haze. Single-module. `minSdk 33`, `compile/target 35`.
Detail versi: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Mulai dalam 5 menit

```sh
git clone https://github.com/az925-crypto/Liphify-.git
# buka di Android Studio, sync, Run ▶ (debug)
```

Yang perlu tahu sebelum ngoding:

1. **Library = 1 folder.** Scan hanya baca `Music/LiPhify`
   (`RELATIVE_PATH LIKE`, bukan seluruh storage). Taruh MP3 di situ → tab
   Library → Refresh. Filter: bukan ringtone/notif, ≥30 dtk, ≥50KB.
2. **Home hidup dari network.** Section Trending = kiosk Trending YouTube →
   fallback 3 query. Offline = pesan + retry, bukan crash.
3. **Satu engine putar.** Lokal & YouTube lewat `MediaController` +
   `LiPhifySessionService`. URL YouTube basi → resolve ulang tiap play,
   jangan cache URL.
4. **Aturan UI.** Lihat [`UI.md`](UI.md) (spesifikasi per-inci) dan
   [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) (aturan anti-dummy +
   ponytail).

## Build & rilis (jangan build di HP)

```sh
git push origin main
# → Actions: assembleRelease → sign → publish ke Release v1.0.0 (otomatis)
# → ambil APK LiPhify-v1.0.0-<commit>-signed.apk di tab Release
```

- Signing dari Secrets (`KEYSTORE_B64/PASSWORD/ALIAS`). Tanpa secrets = unsigned.
- Push berisi **hanya `.md`** tidak trigger build (`paths-ignore`).
- Keystore fisik cuma ada di Secrets — backup di tempat aman, hilang =
  ganti signature = install ulang.

## Peta project

```text
app/src/main/java/com/zaaam/liphify/
├── MainActivity.kt          # scaffold: tab Home/New/Library + search, Haze, BackHandler
├── LiPhifyApp.kt            # Hilt + NewPipe.init
├── playback/                # LiPhifySessionService (Media3)
├── domain/model/            # Track, PlaybackSource(Local|YouTube), Lyrics
├── data/
│   ├── local/               # Room v4 + MediaStoreScanner (folder-scoped) + migrasi 1→4
│   ├── youtube/             # SATU-SATUNYA pemanggil NewPipeExtractor + OkHttpDownloader
│   ├── repository/          # MusicRepository (unified search)
│   └── LyricsRepository.kt  # LRCLIB (PRD-101)
├── di/                      # DatabaseModule
└── ui/
    ├── theme/               # Theme (Inter) + Glass.kt + Motion.kt (pressable/appear)
    ├── common/              # TrackRow, Artwork+fallback, TrackSheet, Genres, EmptyState
    ├── home|browse|library|search|playlist|player|nav/
app/src/main/res/            # font Inter, ikon launcher, ic_music_note
.github/workflows/           # build-release.yml (build+sign+publish+bersih asset)
```

## Keputusan yang jangan dibalik diam-diam

| Keputusan | Alasan |
|---|---|
| Scan cuma `Music/LiPhify` | `IS_MUSIC` OEM tidak可 dipercaya (ringtone ikut) |
| Tanpa BOM Media3, pin `1.9.0` | artefak BOM tidak ada di repo |
| `compile/target 35` | syarat mutlak media3 1.9.0 |
| Tanpa Radio/Download/Login/Auto | di luar scope v1, UI-nya dilarang ada |
| GPL-3.0 (NewPipe) | repo publik → source ikut copyleft |

## Dokumen

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — lapisan, alur playback, DB & migrasi, batas NewPipe
- [`docs/CONTRIBUTING.md`](docs/CONTRIBUTING.md) — cara kontribusi, aturan kode, checklist push
- [`UI.md`](UI.md) — spesifikasi UI per-inci + checklist terima per layar
