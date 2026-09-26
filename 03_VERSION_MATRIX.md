# Version Matrix — Aria (working title)

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
| AndroidX Media3 (ExoPlayer, Session) | 1.9.0 (stable per Des 2025, cek update terbaru) | Satu BOM untuk semua modul media3-* |
| Room | Stable terbaru saat implementasi (cek developer.android.com/jetpack/androidx/releases/room) | AndroidX baru merilis penerus dengan penamaan baru — cek dulu mana yang benar-benar stable sebelum commit |
| Hilt | sekitar 1.4.x (per pertengahan 2026, cek versi stable terbaru) | Opsional — boleh diganti Koin kalau kamu lebih familiar |
| Jetpack Compose (BOM) | BOM terbaru saat implementasi (Compose UI/Foundation/Material sekitar 1.11.x per pertengahan 2026) | Pakai Compose BOM, jangan pin versi per-modul manual |
| Kotlin | Versi stable terbaru (cek kotlinlang.org/docs/releases.html) | - |
| Android Gradle Plugin | Minimal 7.4.0 (syarat NewPipeExtractor untuk desugaring), disarankan versi 8.x stable terbaru | - |

## 3. SDK Target
- minSdk: 33 (Android 13) — direkomendasikan karena distribusi sideload-only ke device sendiri (OPPO A60), jadi tidak perlu core library desugaring yang disyaratkan NewPipeExtractor untuk minSdk di bawah 33. Kalau nanti mau support device lebih lama, turunkan ke 26 + aktifkan desugaring (desugar_jdk_libs_nio).
- targetSdk / compileSdk: pakai versi terbaru yang tersedia di SDK Manager kamu saat build (minimal API 34).

## 4. ProGuard/R8 Rules Wajib (dari NewPipeExtractor)

-keep class org.schabi.newpipe.extractor.timeago.patterns.** { *; }
-keep class org.mozilla.javascript.** { *; }
-keep class org.mozilla.classfile.ClassFileWriter
-dontwarn org.mozilla.javascript.tools.**

## 5. Versioning App
- v1.0.0 = rilis pertama yang lulus semua P0 (PRD-001 s/d PRD-012) sesuai Definition of Done.
- v1.1.x = penambahan item P1 (backlog), tidak wajib sekaligus — boleh dirilis satu-satu.
- Karena update extractor sering diperlukan mengikuti perubahan YouTube, siapkan skema versi terpisah untuk "patch extractor" (mis. v1.0.1 = bump versi NewPipeExtractor doang, tanpa fitur baru) supaya gampang dilacak kapan terakhir extractor di-update.
