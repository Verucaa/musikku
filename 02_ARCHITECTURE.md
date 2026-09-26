# Architecture — Aria (working title)

## 1. Tech Stack
- **Bahasa/UI**: Kotlin + Jetpack Compose
- **Playback engine**: AndroidX Media3 (ExoPlayer + MediaSession) — dipakai sama untuk source lokal maupun YouTube, lewat abstraksi MediaSource yang seragam
- **Ekstraksi YouTube Music**: NewPipeExtractor (JitPack, com.github.teamnewpipe:NewPipeExtractor)
- **Database lokal**: Room (index library lokal, playlist, queue persistence, metadata cache hasil search YT — bukan audio)
- **Sumber data lokal**: Android MediaStore (Audio)
- **Async**: Kotlin Coroutines + Flow
- **DI**: Hilt (rekomendasi — bisa diganti kalau kamu punya preferensi lain)
- **CI/CD**: GitHub Actions (build + sign release APK)

## 2. Struktur Modul
Rekomendasi: single-module dulu (bukan multi-module) — alasan utama: build environment kamu di Termux/proot Ubuntu, modul banyak = overhead konfigurasi Gradle yang tidak sepadan untuk project personal. Struktur package per-layer di dalam 1 modul:

com.[TBD_PACKAGE_NAME].aria/
- data/
  - local/          Room entities, DAO, MediaStore scanner
  - youtube/        Wrapper/repository di atas NewPipeExtractor (PRD-012)
  - repository/     Implementasi repository gabungan (unified search, dll)
- domain/
  - model/          Track, Playlist, QueueItem, PlaybackSource (sealed: Local/YouTube)
  - usecase/        SearchUnified, ResolveStreamUrl, ManageQueue, dll
- playback/
  - AriaMediaSessionService.kt   Media3 MediaSessionService, foreground service
  - source/         Custom MediaSource untuk YouTube (lazy resolve stream URL)
- ui/
  - nowplaying/
  - library/
  - browse/
  - search/
  - theme/          Apple Music-style design tokens (typography, spacing, blur)

## 3. Playback Abstraction (kunci dari fitur hybrid)
- Definisikan sealed interface PlaybackSource { data class Local(uri: Uri) ; data class YouTube(videoId: String) }
- Queue/Now Playing/MediaSession tidak peduli source-nya apa — mereka cuma pegang PlaybackSource + metadata umum (title/artist/artwork/duration).
- Untuk YouTube, MediaSource resolve StreamInfo lazy (saat item mau diputar, bukan saat ditambah ke queue) — karena URL stream YouTube itu time-limited/session-bound. Kalau user pause lama lalu resume, atau skip balik ke lagu lama di queue, URL lama kemungkinan besar sudah expired, wajib re-resolve, jangan cache URL mentah-mentah.
- NewPipeExtractor call SELALU lewat repository layer (YouTubeRepository), tidak pernah dipanggil langsung dari UI/playback layer — ini yang bikin PRD-012 (resilience) bisa diterapkan di satu titik saja.

## 4. Background Service
Pakai pola Media3 MediaSessionService (bukan Service manual) — sudah handle foreground notification, lock screen controls, dan integrasi tombol headset/Bluetooth secara built-in, mengurangi boilerplate dibanding implementasi MediaSession manual dari nol.

## 5. Dev Environment & Build Flow
- Editing kode: langsung di Termux/proot Ubuntu kamu.
- Build berat (assembleRelease, dsb): disarankan didorong ke GitHub Actions (cloud runner), bukan di device — Snapdragon 680/8GB RAM akan berat kalau full Gradle build (terutama R8/minify + Compose compiler) dilakukan on-device tiap kali.
- Testing cepat/logcat: pakai LADB buat inspeksi device saat debugging manual di OPPO A60.
- Gradle config yang membantu kalau tetap build sebagian di device: aktifkan Gradle build cache (org.gradle.caching=true), batasi org.gradle.workers.max sesuai core CPU device, dan pertimbangkan org.gradle.jvmargs dengan heap yang wajar (jangan default tinggi yang bisa bikin OOM di proot).

## 6. Distribusi (Sideload-only)
- Tidak ada Play App Signing — kamu generate & simpan sendiri release keystore.
- Keystore & password disimpan sebagai GitHub Secrets, bukan dikomit ke repo.
- GitHub Actions workflow: build, sign dengan keystore dari secret, upload APK sebagai release artifact/GitHub Release — instal manual via LADB atau transfer file ke device.

## 7. Lisensi & Legal
- NewPipeExtractor: GPL-3.0. Karena distribusi cuma sideload pribadi (bukan publikasi ke pihak lain), kewajiban copyleft GPL secara praktis belum "trigger". Kalau nanti repo ini dipush publik, source code Aria otomatis harus ikut lisensi GPL (atau kompatibel) — ini keputusan yang perlu disadari sebelum push publik, bukan sesuatu yang otomatis aman.
- Scraping YouTube tanpa API resmi = pelanggaran ToS YouTube. Risiko diterima secara sadar untuk penggunaan pribadi.
