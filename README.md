# LiPhify — `com.zaaam.liphify`

Player musik hybrid: file audio lokal (MediaStore) + streaming YouTube Music
(NewPipeExtractor), UI ala Apple Music. Sideload pribadi, bukan Play Store.

## Cara build

Semua build berat HANYA via GitHub Actions (jangan di device):

1. Push ke `main` → workflow `.github/workflows/build-release.yml`
   otomatis `assembleRelease` → upload artifact `LiPhify-release`
   (hanya APK signed; unsigned dihapus setelah sign + verify).
2. Signing: isi Secrets `KEYSTORE_B64`, `KEYSTORE_PASSWORD`,
   `KEY_ALIAS`, `KEY_PASSWORD`. Tanpa secrets → APK unsigned.
3. Gradle dipin 8.7 di workflow (tanpa wrapper jar di repo).
4. Install manual via LADB / transfer file ke OPPO A60.

Lokal cukup untuk edit kode. Jangan jalankan Gradle di Termux/proot.

## Risiko legal (pengingat)

- Scraping YouTube via NewPipeExtractor melanggar ToS YouTube.
  Diterima sadar untuk sideload pribadi, bukan distribusi publik.
- NewPipeExtractor = GPL-3.0. Repo ini dipublish publik, jadi source
  project ini ikut kewajiban copyleft GPL.

## Deviasi dari 03_VERSION_MATRIX.md

- `compileSdk/targetSdk = 35` (bukan 34): syarat mutlak
  `media3 1.9.0` (checkReleaseAarMetadata gagal di 34).
  `minSdk` tetap 33. Alasan dicatat di sini sesuai DoD §5.
- Media3 tanpa BOM (pin eksplisit `1.9.0` per modul) karena artefak
  `androidx.media3:media3-bom:1.9.0` tidak ketemu di repo Google/Maven.

## v1.1 backlog (P1, bukan silently drop)
- PRD-101 Lyrics, PRD-102 Dynamic theming penuh, PRD-103 Sleep timer,
  PRD-104 Equalizer. Ikon lirik di Now Playing sengaja disabled.
