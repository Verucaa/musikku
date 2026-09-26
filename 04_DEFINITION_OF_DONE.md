# Definition of Done — LiPhify v1.0

Rilis v1.0 dianggap selesai kalau SEMUA poin di bawah ini terpenuhi. P1 (PRD-101-104) TIDAK termasuk gate ini — dicatat eksplisit sebagai backlog v1.1, bukan silently didrop.

## 1. Functional Gate
- [ ] Semua requirement P0 (PRD-001 s/d PRD-012) lulus Acceptance Criteria masing-masing seperti tertulis di 01_PRD.md.
- [ ] Semua langkah Verifikasi manual di 01_PRD.md sudah dijalankan dan hasilnya sesuai ekspektasi, di device target OPPO A60 (bukan cuma emulator).
- [ ] Tidak ada requirement yang masih dalam status "mock" untuk bagian yang menurut kebijakan Mock Policy-nya wajib real.

## 2. Stability Gate (skenario wajib tidak crash)
- [ ] Airplane mode diaktifkan di tengah proses search/stream YouTube -> app tidak crash, fitur lokal tetap normal (PRD-012).
- [ ] Permission storage ditolak user -> app tidak crash, tampil empty state + tombol grant (PRD-001).
- [ ] App di-kill paksa dari recent apps saat playback aktif -> service berhenti graceful, tidak ada notification "zombie" tersisa (PRD-006).
- [ ] Video YouTube age-restricted / region-locked / private diputar -> error message jelas, bukan crash (PRD-004).
- [ ] Queue campuran lokal+YouTube 20 lagu -> app restart -> queue tetap utuh (PRD-008).

## 3. Performance Gate (di OPPO A60 / Snapdragon 680)
- [ ] Transisi mini-player ke Now Playing full-screen di bawah 300ms, tanpa dropped frame yang kentara secara visual.
- [ ] Local library scan di bawah 2000 file selesai di bawah 5 detik.
- [ ] Tidak ada memory leak kentara setelah pemakaian normal sekitar 30 menit (ganti-ganti lagu, buka-tutup Now Playing berulang) — cek lewat LADB/logcat kalau ada warning GC berlebihan.

## 4. Build & Release Gate
- [ ] Release APK berhasil di-build & di-sign lewat GitHub Actions workflow, tanpa error.
- [ ] Keystore & credential signing disimpan di GitHub Secrets, TIDAK ada di riwayat commit repo.
- [ ] ProGuard/R8 rules dari 03_VERSION_MATRIX.md bagian 4 sudah masuk ke file proguard project — build release dengan minify aktif tidak menghasilkan crash yang berbeda dari build debug (verifikasi manual, bukan cuma "berhasil compile").
- [ ] Versi NewPipeExtractor yang dipin di build.gradle sesuai 03_VERSION_MATRIX.md, bukan versi acak/latest otomatis.

## 5. Dokumentasi
- [ ] `com.zaaam.liphify` di semua file (termasuk applicationId di build.gradle) sudah final, bukan placeholder.
- [ ] README repo mencatat: cara build, catatan risiko legal (ToS YouTube + kewajiban GPL-3.0 kalau repo dipublish) sebagai pengingat untuk diri sendiri.
- [ ] 03_VERSION_MATRIX.md diperbarui kalau ada dependency yang ternyata harus pakai versi berbeda dari rekomendasi awal saat implementasi (tandai alasannya).

## 6. Sign-off
- [ ] Semua gate 1-5 di atas dicek satu-satu (bukan asumsi "harusnya udah bener").
- [ ] Item P1 yang belum dikerjakan didaftar eksplisit di README/issue tracker sebagai "v1.1 backlog", bukan hilang begitu saja dari radar.
