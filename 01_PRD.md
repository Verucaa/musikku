# PRD — LiPhify Music Player
Local Playback + YouTube Music (via NewPipeExtractor), UI ala Apple Music

## 1. Ringkasan Produk
Aplikasi Android pribadi (bukan untuk Play Store) yang menggabungkan dua sumber musik dalam satu pengalaman: file audio lokal di device dan streaming YouTube Music lewat NewPipeExtractor. Struktur navigasi dan bahasa visual meniru Apple Music (tab bar bawah, full-screen Now Playing dengan blur artwork, mini-player persisten).

## 2. Tujuan (Goals)
- User bisa dengerin lagu lokal DAN cari/putar lagu dari YouTube Music dalam satu app, satu UI.
- Pengalaman visual & navigasi berasa seperti Apple Music.
- App jalan stabil di device kelas menengah (Snapdragon 680 / 8GB RAM).
- Sekali install lewat sideload, tidak bergantung ke Play Store.

## 3. Non-Tujuan (Out of Scope v1)
- Publish ke Google Play Store atau F-Droid.
- Download/cache permanen track YouTube untuk mode offline penuh.
- Login/sinkronisasi akun YouTube Music pribadi.
- Android Auto (masuk backlog P1, bukan v1).
- Video playback — scope murni audio.

## 4. Target Pengguna
Pengguna tunggal: pembuat app sendiri (P). Bukan produk multi-user, tidak ada onboarding sales, tidak ada monetisasi.

## 5. Asumsi & Batasan
- NewPipeExtractor (GPL-3.0) rawan patah kalau YouTube ubah struktur internal — wajib ada lapisan resilience (lihat PRD-012).
- Karena scraping non-API, ini melanggar ToS YouTube — risiko diterima karena distribusi cuma sideload pribadi, bukan publik.
- GPL-3.0 mewajibkan source terbuka KALAU didistribusikan ke pihak lain — jadi kalau repo ini nanti dipush publik ke GitHub (seperti kebiasaan publish di akun GitHub kamu), source code project ini otomatis kena kewajiban GPL juga.
- Target device utama: OPPO A60 (Snapdragon 680, 8GB RAM) — budget performa harus realistis untuk hardware ini, bukan flagship.
- Dev environment: Termux/proot Ubuntu untuk editing, build/release lewat GitHub Actions, inspeksi device via LADB.

## 6. Requirement P0 (Wajib untuk v1)

### PRD-001 — Local Library Scan & Indexing
- **Apa**: Scan MediaStore untuk semua file audio lokal, index metadata (title/artist/album/duration/artwork) ke database lokal.
- **Kenapa**: Basis fitur "player musik lokal" — tanpa ini tab Library kosong.
- **Perilaku**: Buka app pertama kali / pull-to-refresh Library → request izin READ_MEDIA_AUDIO (API 33+) / READ_EXTERNAL_STORAGE (API <33) → scan via MediaStore.Audio → simpan ke Room DB → tampilkan progress kalau >500 file.
- **Mock policy**: Boleh mock daftar dummy saat develop UI Library, tapi scan asli wajib jalan sebelum requirement dianggap selesai.
- **Acceptance Criteria**: File audio (mp3/flac/m4a/ogg/wav) terdeteksi & muncul di Library <5 detik untuk <2000 file; metadata title/artist/album/artwork ke-load benar dari tag.
- **Verifikasi**: Manual test di OPPO A60 dengan ≥50 file campuran format, cek jumlah & metadata cocok; test permission denied (app tidak crash, tampil empty state + tombol grant).

### PRD-002 — Local Audio Playback Engine
- **Apa**: Player untuk memutar file audio lokal via Media3 ExoPlayer.
- **Kenapa**: Fungsi inti — tanpa ini tidak ada "player musik lokal".
- **Perilaku**: Tap lagu di Library → ExoPlayer load via content URI → play, update Now Playing UI & MediaSession.
- **Mock policy**: Tidak boleh dimock — playback harus real dari awal.
- **Acceptance Criteria**: Playback mulai <500ms setelah tap; seek/pause/resume akurat; semua format target terbaca tanpa crash.
- **Verifikasi**: Manual test tiap format, test seek berbagai posisi, test background→foreground (state tetap sinkron).

### PRD-003 — YouTube Music Search via NewPipeExtractor
- **Apa**: Search bar untuk cari track/album/artist/playlist di YouTube Music via NewPipeExtractor SearchExtractor.
- **Kenapa**: Basis fitur streaming — tanpa search, tidak ada cara menemukan konten YT.
- **Perilaku**: User ketik query → panggil extractor di coroutine IO → map hasil ke model internal (judul/artist/thumbnail/duration/videoId) → tampil per section (Songs/Albums/Artists/Playlists) ala Apple Music search.
- **Mock policy**: Boleh mock hasil dummy saat develop UI list, tapi query real ke extractor wajib sebelum requirement selesai — bukan fitur yang boleh permanen mock.
- **Acceptance Criteria**: Query umum return hasil <3 detik di koneksi normal; error network/parsing ditangani (pesan error, bukan crash); hasil kosong tampil empty state.
- **Verifikasi**: Manual test 10+ query berbeda; test tanpa internet; test simulasi struktur YouTube berubah (cek log — expect graceful error, bukan crash).

### PRD-004 — YouTube Audio Stream Resolution & Playback
- **Apa**: Resolve StreamInfo dari NewPipeExtractor untuk videoId terpilih, mainkan via ExoPlayer sama seperti local playback.
- **Kenapa**: Ini yang membuat "streaming YouTube Music" beneran berfungsi.
- **Perilaku**: Tap hasil search/track → resolve StreamInfo → pilih audio stream (itag audio-only) terbaik yang tersedia → set MediaItem ExoPlayer → play, share UI Now Playing dengan local.
- **Mock policy**: Tidak boleh dimock permanen — ini core value proposition. Mock hanya untuk loading state sementara integrasi belum selesai.
- **Acceptance Criteria**: Stream mulai <5 detik di wifi normal; kualitas ≥128kbps kalau tersedia; error (age-restricted/region-block/private) ditangani dengan pesan jelas, bukan crash.
- **Verifikasi**: Manual test 15+ video termasuk yang age-restricted & region-locked; test jaringan lambat (buffering indicator muncul).

### PRD-005 — Unified Now Playing Screen (Apple Music style)
- **Apa**: Full-screen player: artwork besar, background blur dari artwork, title/artist, seekbar, kontrol play/pause/next/prev/shuffle/repeat — sama untuk source lokal maupun YouTube.
- **Kenapa**: Ini inti "gaya UI Apple Music" yang diminta.
- **Perilaku**: Tap mini-player/notification → expand full-screen (swipe-up); background blur dari artwork (local: embedded art, YT: thumbnail); source transparan ke user — UI identik.
- **Mock policy**: Layout boleh dibangun duluan dengan data dummy, wajib dites dengan data real (lokal + YT) sebelum selesai.
- **Acceptance Criteria**: Transisi mini↔full-screen <300ms tanpa jank di Snapdragon 680; blur artwork render tanpa lag; kontrol responsif <100ms.
- **Verifikasi**: Manual test di OPPO A60, cek framerate visual; test lagu tanpa artwork (fallback placeholder, bukan crash).

### PRD-006 — Background Playback, Foreground Service & Media Notification
- **Apa**: Playback tetap jalan di background via foreground service + MediaSession + notification (play/pause/next/prev) + lock screen controls.
- **Kenapa**: Standar UX music app — tanpa ini app tidak usable untuk dengerin sambil buka app lain.
- **Perilaku**: Playback mulai → start foreground service dengan notification channel khusus; MediaSession publish state; notification & lock screen update real-time.
- **Mock policy**: Tidak boleh dimock — behavior sistem Android yang harus dites di device asli.
- **Acceptance Criteria**: Playback tidak putus saat minimize/screen off; notification muncul dengan artwork & kontrol fungsional; kontrol lock screen & tombol headset/Bluetooth bekerja.
- **Verifikasi**: Manual test minimize 10 menit sambil playback jalan; test kontrol lock screen & headset; test kill app dari recent apps (service stop graceful, notification tidak residual).

### PRD-007 — Bottom Tab Navigation (Apple Music layout)
> SUPERSEDE oleh design.md §4.1 (keputusan user): 3 tab (Home, New, Library) + tombol Search bulat 44dp terpisah — bukan 4 tab. Label final: Home (=Listen Now), New (=Browse), Library.
- **Apa**: 4 tab utama: Listen Now, Browse, Library, Search — struktur & urutan niru Apple Music.
- **Kenapa**: Kerangka navigasi yang bikin app "berasa" Apple Music, bukan cuma warna/font.
- **Perilaku**: Tab bar persisten di atas mini-player; Listen Now = rekomendasi/recently played gabungan lokal+YT; Browse = jelajah genre/mood YT Music; Library = koleksi lokal + playlist; Search = unified search.
- **Mock policy**: Konten "Listen Now"/"Browse" awal boleh mock (algoritma rekomendasi kompleks bukan prioritas v1); struktur navigasi & Library/Search wajib real.
- **Acceptance Criteria**: 4 tab accessible, state per-tab dipertahankan (scroll position dll) saat pindah tab; mini-player tetap kelihatan lintas tab.
- **Verifikasi**: Manual test navigasi bolak-balik antar tab sambil playback jalan, cek tidak ada reset state aneh.

### PRD-008 — Playback Queue Management
- **Apa**: Antrian putar campuran lokal & YouTube, dengan reorder (drag), "Play Next"/"Add to Queue", shuffle, repeat (off/one/all).
- **Kenapa**: Fitur dasar music player — tanpa queue proper, app cuma bisa mutar 1 lagu.
- **Perilaku**: Context menu lagu (lokal/YT) → "Play Next"/"Add to Queue"; queue screen dari Now Playing, drag untuk reorder.
- **Mock policy**: Tidak boleh dimock — banyak edge case (habis lagu, re-shuffle, dll) yang harus real.
- **Acceptance Criteria**: Queue persist setelah app restart; shuffle tidak ulang lagu sama sebelum semua kebagian; auto-lanjut tanpa gap berarti.
- **Verifikasi**: Manual test queue campuran 20 lagu, test shuffle 3x (cek distribusi), test force-close lalu buka lagi (queue tetap ada).

### PRD-009 — Local & Mixed Playlist Management
- **Apa**: Buat/edit/hapus playlist custom berisi campuran lagu lokal + track YouTube Music.
- **Kenapa**: Cara organisir koleksi lintas source — value inti dari pendekatan hybrid.
- **Perilaku**: Library tab → "New Playlist" → nama → tambah lagu dari mana saja (lokal/YT/search); tersimpan di Room DB.
- **Mock policy**: UI awal boleh mock, CRUD wajib persist ke database real sebelum selesai.
- **Acceptance Criteria**: Playlist survive app restart; tambah/hapus/reorder lagu berfungsi; campuran source tanpa error.
- **Verifikasi**: Manual test buat playlist 10 lagu campuran, restart app, cek utuh; test hapus lagu dari playlist.

### PRD-010 — Persistent Mini-Player
- **Apa**: Bar mini-player di atas bottom tab bar, muncul saat ada playback aktif, tap untuk expand ke Now Playing.
- **Kenapa**: Pattern standar Apple Music/Spotify — akses kontrol cepat tanpa keluar context.
- **Perilaku**: Muncul otomatis saat playback mulai, hilang kalau playback stop total; tap untuk expand.
- **Mock policy**: Animasi boleh mock dulu, sinkronisasi state (title/artwork/progress) wajib real.
- **Acceptance Criteria**: Selalu sinkron dengan state aktual; tidak lag saat scroll konten lain.
- **Verifikasi**: Manual test scroll list panjang sambil mini-player nempel (cek jank); test transisi ke Now Playing dan balik.

### PRD-011 — Unified Search
- **Apa**: 1 search bar untuk cari lintas local library DAN YouTube Music, hasil dipisah section.
- **Kenapa**: User tidak perlu peduli sumber lagu — sesuai visi hybrid & UX Apple Music.
- **Perilaku**: Ketik → search paralel: query Room DB lokal (instant) + query NewPipeExtractor (async) → gabung tampilan section "Di Perangkat" & "YouTube Music".
- **Mock policy**: Bagian lokal wajib real dari awal; bagian YT boleh placeholder loading sampai PRD-003 selesai.
- **Acceptance Criteria**: Hasil lokal <200ms; hasil YT muncul saat resolve (indikator terpisah); query kosong/typo tampil empty state per section.
- **Verifikasi**: Manual test lagu yang cuma lokal, cuma YT, dan ada di dua-duanya (cek tidak duplikat rancu).

### PRD-012 — NewPipeExtractor Resilience & Error Handling
- **Apa**: Lapisan error handling khusus untuk kegagalan ekstraksi (YouTube ubah struktur, extractor outdated, rate limit).
- **Kenapa**: NewPipeExtractor rawan patah — tanpa ini app bisa crash total tiap YouTube update sesuatu.
- **Perilaku**: Semua panggilan NewPipeExtractor dibungkus try-catch spesifik (ParsingException, ExtractionException) → gagal → pesan "Gagal ambil data dari YouTube, coba lagi" + log detail, fitur lokal tetap jalan normal.
- **Mock policy**: Tidak boleh dimock — exception handling real wajib dari awal.
- **Acceptance Criteria**: Simulasi extractor gagal tidak crash app; fitur lokal 100% jalan meski fitur YT error total.
- **Verifikasi**: Manual test airplane mode di tengah search/stream YT; review code cek semua call ke extractor ter-wrap try-catch.

## 7. Requirement P1 (Backlog, tidak blocking rilis v1)

### PRD-101 — Lyrics Display
- **Apa**: Tampilkan lirik (plain/time-synced LRC) di Now Playing, sumber API pihak ketiga (mis. LRCLIB).
- **Kenapa**: Fitur signature Apple Music, tapi bukan blocker v1.
- **Perilaku**: Swipe di Now Playing buka panel lirik; highlight baris berjalan kalau ada sync data.
- **Mock policy**: Boleh full dummy untuk P1 — real integration menyusul.
- **Acceptance Criteria**: Lirik muncul untuk lagu yang tersedia; tanpa lirik → "Lirik tidak tersedia", bukan error.
- **Verifikasi**: Manual test 5 lagu populer + 1 lagu obscure.

### PRD-102 — Dynamic Theming dari Artwork
- **Apa**: Ekstrak warna dominan artwork aktif, jadi aksen warna Now Playing/mini-player.
- **Kenapa**: Detail visual khas Apple Music.
- **Perilaku**: Ganti lagu → color extraction (Palette API) background → animate transisi aksen.
- **Mock policy**: Boleh warna default statis dulu — non-blocking sepenuhnya.
- **Acceptance Criteria**: Warna berubah sesuai artwork tanpa lag transisi; fallback netral kalau gagal.
- **Verifikasi**: Manual test ganti lagu artwork kontras, cek performa di OPPO A60.

### PRD-103 — Sleep Timer
- **Apa**: Auto-stop playback setelah durasi tertentu / setelah lagu selesai.
- **Kenapa**: Fitur umum untuk dengerin sambil tidur.
- **Perilaku**: Menu Now Playing → pilih durasi (15/30/45/60 menit / end of track) → auto pause.
- **Mock policy**: Non-blocking, logic timer sederhana, tidak perlu mock.
- **Acceptance Criteria**: Berhenti tepat waktu ±2 detik; UI kasih tau timer aktif & bisa dibatalkan.
- **Verifikasi**: Manual test set timer 1 menit.

### PRD-104 — Equalizer / Audio Effects
- **Apa**: EQ dasar (bass boost, preset genre) via Media3 AudioProcessor/AudioEffect API.
- **Kenapa**: Fitur umum music player, bukan inti value proposition.
- **Perilaku**: Settings → Equalizer → toggle preset/manual band, apply real-time ke playback (lokal & YT).
- **Mock policy**: Boleh skip total di v1 kalau waktu terbatas — murni backlog.
- **Acceptance Criteria**: Perubahan EQ terdengar real-time tanpa restart; setting persist antar sesi.
- **Verifikasi**: Manual test toggle beberapa preset, cek tersimpan setelah restart.

## 8. Risiko Utama
- **Legal/ToS**: Scraping YouTube via NewPipeExtractor melanggar ToS — diterima karena sideload pribadi, bukan distribusi publik.
- **Fragility upstream**: YouTube sering ubah struktur — extractor bisa berhenti kerja sampai TeamNewPipe rilis patch; PRD-012 mitigasi dampaknya, bukan mencegah.
- **Lisensi GPL-3.0**: Kalau repo nanti dipublish, source code project ini kena kewajiban copyleft.
