# DESIGN.md — Aria UI/UX Implementation Spec
Untuk AI coding agent (bukan manusia). Dibaca setelah 01_PRD.md.

## 0. Aturan wajib — BACA DULU SEBELUM NGODING
Dokumen ini bukan cuma referensi visual, tapi kontrak fungsional. Berlaku aturan ini di semua bagian di bawah:

1. **Tidak ada dummy data di kode final.** Setiap list, judul lagu, artist, count, artwork yang tampil di UI harus hasil query nyata (Room DB / MediaStore / NewPipeExtractor / MediaController), bukan `remember { mutableStateOf("Neon Skyline") }` atau List hardcoded.
2. **Tidak ada tombol/kontrol yang no-op.** Kalau sebuah elemen terlihat interaktif (punya ripple, punya onClick), dia WAJIB mengubah state nyata. Kalau nggak ada fungsi nyata di baliknya, HAPUS elemen itu — jangan biarkan ada tapi kosong.
3. **Pengecualian HANYA yang eksplisit ditandai "mock policy" di 01_PRD.md** (contoh: PRD-007 — konten hero/rekomendasi "Listen Now" & "Browse" boleh statis karena algoritma rekomendasi di luar scope v1). Setiap pengecualian ini WAJIB ditandai komentar di kode: `// STATIC PER PRD-007 — recommendation algorithm out of scope v1`. Tanpa komentar itu, dianggap dummy yang melanggar aturan 1.
4. **Fitur P1/backlog yang UI-nya kepaksa nongol duluan (misal ikon lirik) harus jujur nonaktif** — disabled state + pesan singkat ("Lirik belum tersedia"), BUKAN tombol yang kelihatan jalan tapi isinya lorem ipsum.
5. Kalau ragu antara "buat fungsinya beneran" vs "skip dan sederhanakan UI", pilih **skip/sederhanakan**. UI yang jujur lebih kecil lebih baik daripada UI lengkap yang bohong.

## 1. Tech stack UI (asumsi berdasarkan 01_PRD.md)
- Kotlin + Jetpack Compose, Material3 sebagai base tapi di-theme habis-habisan (jangan biarkan komponen kelihatan stock Material)
- State: MVVM — ViewModel + StateFlow, UI collect via `collectAsStateWithLifecycle`
- Navigasi: Compose Navigation untuk 4 tab (bottom nav), Now Playing BUKAN destinasi nav terpisah — dia overlay full-screen yang di-drive boolean `isExpanded` di `PlaybackViewModel` bersama, supaya transisi mini-player ↔ full-screen bisa smooth dan balik ke scroll position tab sebelumnya
- Target device: OPPO A60 = Android 14/ColorOS 14 (upgradable ke Android 15) → API level 34 tersedia, jadi blur native (`RenderEffect`, API 31+) aman dipakai, TIDAK perlu workaround untuk device rendah
- Blur "kaca" yang benar (bukan blur gambar statis): `Modifier.blur()` Compose HANYA mem-blur isi elemen itu sendiri, BUKAN konten di belakangnya yang sedang di-scroll. Untuk tab bar & mini-player mengambang di atas list yang scroll (efek Liquid Glass asli), pakai library **Haze** (`dev.chrisbanes.haze`) yang memang dibuat untuk efek "blur konten di belakang" di Compose. Untuk background Now Playing (statis, gradient warna dari artwork, gak ada konten scroll di belakangnya) — gradient biasa udah cukup, gak perlu blur run-time.
- Image loading: Coil (artwork lokal via content URI, thumbnail YouTube via URL)
- Ekstraksi warna dominan: `androidx.palette:palette-ktx` — generate dari Bitmap artwork tiap track berganti

## 2. Font
SF Pro milik Apple **berlisensi khusus platform Apple**, tidak boleh di-bundle ke app Android untuk redistribusi. Jangan pura-pura pakai SF Pro. Pakai **Inter** (SIL Open Font License, gratis dipakai di mana saja) sebagai pengganti geometris yang paling dekat — bundle sebagai font resource di app supaya tampilan konsisten di semua device (tidak ketiban font OPPO Sans bawaan ColorOS).

Skala tipografi (sp):
| Role | Size/Weight |
|---|---|
| Large title | 34 / 700 |
| Title | 22 / 700 |
| Headline | 17 / 600 |
| Body | 15 / 400 |
| Subhead | 13 / 400 |
| Caption | 11 / 400 |

## 3. Design tokens

### Warna dasar (dark theme)
- Background utama: `#000000`
- Surface elevated (card, sheet): `#1C1C1E`
- Surface sekunder (chip, input): `#2C2C2E`
- Divider: `rgba(255,255,255,0.08)`
- Teks primer: `#FFFFFF`
- Teks sekunder: `rgba(255,255,255,0.6)`
- Teks tersier/hint: `rgba(255,255,255,0.4)`
- Accent: `#FA233B` (approksimasi warna Apple Music yang paling sering dipakai desainer pihak ketiga — Apple sendiri tidak mempublikasikan hex resminya, jadi ini pendekatan, bukan nilai resmi)

⚠️ Catatan penting: warna boleh mirip, TAPI jangan pakai wordmark/logo Apple Music asli (ikon nada dua-lingkaran, teks "Apple Music") di splash screen/ikon app. Itu trademark, bukan cuma soal warna — beda isu dari kemiripan visual UI yang memang tujuan app ini untuk pemakaian pribadi.

### Warna dinamis per lagu (Liquid Glass background)
- `Palette.Builder(artworkBitmap).generate()` → ambil **Vibrant swatch** + **DarkVibrant/Muted swatch** → jadi gradient linear 135° dua warna itu untuk background Now Playing
- Darken hasil ekstraksi ~30-40% (kalikan tiap channel RGB × 0.6–0.7) supaya kontras teks putih tetap aman — kalau setelah darken luminance masih > 0.5, darken lagi sampai di bawah itu
- Fallback (artwork nggak ada / ekstraksi gagal / lagu YouTube belum load thumbnail): gradient netral `#2C2C2E → #1C1C1E`. JANGAN hardcode warna ungu/pink/dsb sebagai default — itu cuma buat demo mockup, bukan spek final.

### Spacing: 4 / 8 / 12 / 16 / 20 / 24 / 32 dp
### Radius: card 12dp, hero card 16dp, tab bar/mini-player pill 24-28dp (stadium shape), tombol pill 20dp
### Blur: tab bar & mini-player pakai Haze blur radius ~24dp di atas background `rgba(255,255,255,0.10)`

## 4. Komponen global

### 4.1 Bottom nav — "liquid glass" tab bar
- Bentuk pill mengambang, margin horizontal 10dp, margin bawah respect `WindowInsets.navigationBars`
- 3 tab: **Listen Now, Browse, Library** (TIDAK ada Radio — di luar scope PRD)
- Tombol Search terpisah, bulat 44dp, di kanan pill
- Aktif: icon+label tint `#FA233B`. Nonaktif: putih 65% opacity
- Wiring: `NavController.currentBackStackEntryAsState()` yang nentuin tab aktif — BUKAN state lokal terpisah yang bisa nggak sinkron sama nav asli. Tiap tab preserve scroll position sendiri (Compose Navigation state saving bawaan)

### 4.2 Mini-player (PRD-010)
- Composable ini **tidak dirender sama sekali** kalau `MediaController.playbackState == STATE_IDLE` — jangan disembunyikan pakai alpha 0, karena itu masih ada secara aksesibilitas/talkback
- Artwork 34dp dari `MediaMetadata.artworkUri` real (Coil), title/artist dari `MediaController.currentMediaItem` real
- Tombol play/pause → `MediaController.play()`/`pause()` asli, tombol skip-forward → `seekToNext()` asli
- Tap di luar 2 tombol → expand ke Now Playing (`isExpanded = true`)

### 4.3 Now Playing full-screen (PRD-005)
- Background: gradient dinamis (lihat 3.2), update real-time tiap track ganti — bukan warna statis
- Chevron-down: `isExpanded = false` (pop, bukan navigate baru) — supaya balik ke posisi scroll tab sebelumnya
- Artwork 1:1 real dari `MediaMetadata.artworkUri`; fallback kalau kosong: card kaca translucent `rgba(0,0,0,0.18)` + ikon musik putih di tengah (BUKAN gambar placeholder generik abu-abu)
- Judul/artis: real binding, `basicMarquee()` kalau teks kepanjangan
- Seekbar: `Slider` terhubung ke `MediaController.currentPosition`/`duration`, update tiap 200-500ms via coroutine ticker selama playing, `onValueChangeFinished` → `seekTo()` asli
- Kontrol: shuffle (toggle `shuffleModeEnabled` asli), prev/next (`seekToPrevious`/`seekToNext` asli), play/pause di lingkaran kaca 56dp, repeat (cycle `repeatMode` OFF→ONE→ALL asli)
- Ikon bawah: **queue** (buka Queue Panel real, lihat 4.4) — WAJIB fungsional karena PRD-008 P0. **Lirik** — kalau belum diimplementasi di sprint ini (PRD-101 masih P1), ikon ini HARUS disabled + snackbar "Lirik belum tersedia", bukan tombol hidup yang buka panel kosong/isi dummy

### 4.4 Queue panel (PRD-008 — P0, wajib fungsional)
- List real dari `Player.getMediaItemAt(i)` untuk semua item di queue, bukan 2 baris hardcoded
- Reorder: drag-and-drop pakai `Player.moveMediaItem(from, to)` (native Media3, bukan reimplementasi manual)
- "Play Next"/"Add to Queue" dari context menu manapun harus benar-benar manggil `player.addMediaItem(index, item)` / `player.addMediaItem(item)`

### 4.5 Context menu (long-press lagu, opsional tapi disarankan — dukung PRD-008/009)
- Bottom sheet real dengan: **Play Next**, **Add to Queue**, **Add to Playlist** (buka playlist picker dari Room DB asli)
- JANGAN taruh opsi yang gak ada fiturnya di app ini (Share, Create Station, Download, Suggest Less Like This — semua di luar scope PRD, skip total, jangan taruh terus di-disable)

## 5. Spesifikasi per tab

### 5.1 Listen Now (PRD-007)
- Header "Listen now" — TANPA tombol avatar/akun (app ini single-user, tidak ada sistem profile/akun di PRD manapun, jadi tombol itu cuma bakal jadi dummy — hapus daripada dipaksain ada)
- Section "Recently played": query real dari tabel playback history (Room) `ORDER BY lastPlayedAt DESC`. Kosong → empty state "Belum ada riwayat, mulai putar musik dari Library", BUKAN card abu-abu kosong
- Hero card besar "New Music Mix": ini yang BOLEH statis sesuai PRD-007 mock policy (rekomendasi algoritmik di luar scope v1) — WAJIB komentar kode yang menandai ini exception, lihat aturan 0.3

### 5.2 Browse (PRD-007, non-prioritas)
- Chip genre & featured card boleh curated manual/statis (sesuai mock policy PRD-007), TAPI tap chip genre harus benar-benar filter/query sesuatu yang nyata (filter local library by genre tag, atau jadi search query ke NewPipeExtractor) — jangan chip yang ditekan tapi diam aja

### 5.3 Library (PRD-001, PRD-009)
- 4 baris navigasi nyata: **Playlists** (Room `PlaylistEntity`), **Artists** (`GROUP BY artist` dari hasil scan MediaStore), **Albums** (`GROUP BY album`), **Songs** (full list, count dari `COUNT(*)` query asli — bukan angka hardcoded)
- "Recently added": `ORDER BY dateAdded DESC LIMIT 2` dari data scan asli
- State belum pernah scan / permission `READ_MEDIA_AUDIO` ditolak: CTA "Pindai musik di perangkat" dengan real permission request flow, bukan diam-diam nampilin library kosong

### 5.4 Search (PRD-011)
- Input search trigger 2 query paralel: Room FTS lokal (debounce ~150ms) + NewPipeExtractor (debounce ~400ms, ada loading indicator terpisah karena network lebih lambat)
- Section "Di perangkat" & "YouTube Music" collapse total (bukan nampilin "0 hasil" kosong) kalau query masih kosong — tampilkan recent search history sebagai gantinya
- Error NewPipeExtractor (PRD-012): section YouTube tampil pesan "Gagal ambil data dari YouTube, coba lagi", section lokal tetap jalan normal — DILARANG seluruh search screen crash/kosong gara-gara YouTube gagal

## 6. Motion
- Ganti tab: instant, tanpa crossfade berat (Apple Music sendiri cepat, bukan animasi berat)
- Mini-player → Now Playing: slide-up + scale artwork ringan, ~300ms, easing `FastOutSlowInEasing`
- Queue panel: slide-up dari bawah DALAM Now Playing (bukan dialog terpisah)

## 7. Checklist anti-dummy (WAJIB dicek sebelum lapor "selesai")
- [ ] Tidak ada string/list lagu hardcoded di kode final, kecuali yang ditandai exception PRD-007
- [ ] Semua tombol play/pause/next/prev/shuffle/repeat manggil Media3 `Player` asli
- [ ] Search benar-benar query NewPipeExtractor + Room, bukan list di-hardcode
- [ ] Count "Songs • N" dari `COUNT(*)` asli, bukan angka tetap
- [ ] Fitur belum jadi (lirik, EQ, sleep timer) → UI disabled + pesan jujur, bukan tombol yang keliatan jalan
- [ ] Tombol/elemen yang gak ada fungsi nyatanya sudah dihapus, bukan dibiarkan kosong

## 8. Non-goals (ingatkan diri sendiri, jangan scope creep)
Radio tab, download/cache offline permanen, login akun YouTube Music, Android Auto, terjemahan lirik, Create Station, video playback — semua di luar PRD v1, jangan diimplementasi.
