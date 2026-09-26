# Version Matrix — LiPhify

## 1. Requirement -> Target Version

| ID | Requirement | Target |
|---|---|---|
| PRD-001 | Local Library Scan & Indexing | v1.0 |
| PRD-002 | Local Audio Playback Engine | v1.0 |
| PRD-003 | YouTube Music Search | v1.0 |
| PRD-004 | YouTube Stream Resolution & Playback | v1.0 |
| PRD-005 | Unified Now Playing Screen | v1.0 |
| PRD-006 | Background Playback & Media Notification | v1.0 |
| PRD-007 | Bottom Tab Navigation | v1.0 |
| PRD-008 | Playback Queue Management | v1.0 |
| PRD-009 | Local & Mixed Playlist Management | v1.0 |
| PRD-010 | Persistent Mini-Player | v1.0 |
| PRD-011 | Unified Search | v1.0 |
| PRD-012 | NewPipeExtractor Resilience | v1.0 |
| PRD-101 | Lyrics Display | v1.1 (backlog) |
| PRD-102 | Dynamic Theming dari Artwork | v1.1 (backlog) |
| PRD-103 | Sleep Timer | v1.1 (backlog) |
| PRD-104 | Equalizer / Audio Effects | v1.1 (backlog) |

## 2. Dependency Pinning

| Dependency | Versi Rekomendasi | Catatan |
|---|---|---|
| NewPipeExtractor | v0.26.5 (rilis terbaru per pengecekan) | JitPack. Pin versi eksak, JANGAN pakai plus/wildcard — extractor sering breaking change antar minor version karena mengikuti perubahan struktur YouTube |
| AndroidX Media3 (ExoPlayer, Session) | 1.9.0 eksplisit per modul (TANPA BOM — artefak media3-bom:1.9.0 tidak ada di repo Google/Maven) | LiPhify-v1.0.0 |
| Room | 2.6.1 + KSP | - |
| Hilt | 2.51.1 | BUKAN 1.4.x (koreksi) |
| Jetpack Compose (BOM) | 2024.10.01 | - |
| Kotlin | 2.0.20 + Compose Compiler plugin + KSP 2.0.20-1.0.25 | Wajib plugin (bukan kotlinCompilerExtensionVersion) |
| Android Gradle Plugin | 8.5.2 | Gradle wrapper dipin 8.7 di workflow (tanpa wrapper jar) |

## 3. SDK Target
- minSdk: 33 (Android 13)
- targetSdk / compileSdk: 35 — NAIK dari 34 karena syarat mutlak media3 1.9.0 (checkReleaseAarMetadata gagal di 34).

## 4. ProGuard/R8 Rules Wajib (dari NewPipeExtractor)

-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

## 5. Versioning App
- v1.0.0 = rilis pertama yang lulus semua P0 (PRD-001 s/d PRD-012) sesuai Definition of Done.
- v1.1.x = penambahan item P1 (backlog), tidak wajib sekaligus — boleh dirilis satu-satu.
- Karena update extractor sering diperlukan mengikuti perubahan YouTube, siapkan skema versi terpisah untuk "patch extractor" (mis. v1.0.1 = bump versi NewPipeExtractor doang, tanpa fitur baru) supaya gampang dilacak kapan terakhir extractor di-update.
