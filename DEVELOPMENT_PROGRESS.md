# GS Renji — Development Progress

Status jujur per fitur. "Done (code)" = kode sudah ditulis dan secara logika
benar, TAPI belum pernah di-compile+jalankan di device fisik (sandbox
pengembangan ini tidak punya Android SDK/device). "Wired" = sudah
tersambung ke UI. "Not wired" = manager/logic ada tapi belum ada tombol UI
yang memanggilnya.

## Core overlay
- [x] Crosshair overlay (dynamic draw: shape/color/size/thickness/opacity) — Done (code), Wired
- [x] Crosshair Editor dengan live preview — Done (code), Wired
- [x] Stats HUD real-time (grafik CPU/RAM, gauge suhu) — Done (code), Wired
- [x] Monitor per-metrik toggle terpisah (FPS/CPU/RAM/GPU/Suhu) — Done (code), Wired
- [x] FPS dilabel jujur sebagai estimasi — Done (code), Wired
- [x] Color preset (Vivid/Ocean/Warm/Grayscale) — Done (code), Wired
- [x] Downscale resolusi + auto-restore (boot/exit/notif revert) — Done (code), Wired
- [x] Rendering backend changer (SkiaGL/SkiaVK/Vulkan/OpenGLES via setprop) — Done (code), Wired
- [x] Floating quick-panel penuh (Crosshair/Stats/DND/Brightness/Volume/Screenshot/Voice) — Done (code), Wired

## Akses & info
- [x] Access Manager (status jujur tiap permission) — Done (code), Wired
- [x] Device Information — Done (code), Wired
- [x] Graphics Manager (deteksi GLES/Vulkan/ANGLE) — Done (code), **Not wired ke UI tersendiri** (dipakai internal saja)
- [x] Display Controller (refresh rate, real system-wide via Shizuku/root) — Done (code), Wired

## Game Suite
- [x] Game Library (list, tambah custom) — Done (code), Wired
- [x] Deteksi game berjalan otomatis (UsageStats, fallback dumpsys) — Done (code), Wired
- [x] Auto-apply/revert profile saat game dibuka/ditutup (GameMonitorService) — Done (code), Wired
- [x] Game Lab — Touch Test (real multitouch+latency) — Done (code), Wired
- [x] Runtime Compiler (pm compile real, dengan disclaimer) — Done (code), Wired

## Kontrol sistem
- [x] DND toggle — Done (code), Wired (lewat bubble)
- [x] Brightness slider — Done (code), Wired (lewat bubble)
- [x] Volume slider (media) — Done (code), Wired (lewat bubble)
- [x] Screenshot (MediaProjection, consent tiap kali) — Done (code), Wired
- [x] Voice Changer (preview in-app, SENGAJA dibatasi — lihat disclaimer di kode) — Done (code), Wired

## Performa
- [x] Performance/Balanced/Battery Saver profiles (kombinasi renderer+composition+vsync+MSAA+downscale, TANPA overclock) — Done (code), **Not wired ke UI** — baru berupa manager, belum ada tombol pilih profil

## Keamanan
- [x] Validasi nama package sebelum masuk shell command (cegah injection) — Done (code)

## Belum pernah diverifikasi di device fisik
SEMUA item di atas ditulis berdasarkan API/command yang terdokumentasi resmi
AOSP, tapi belum ada satupun yang benar-benar dicompile dan dites di HP asli
dari sandbox pengembangan ini. Device referensi yang disebutkan (Redmi 10
2022 / Helio G88 / Mali-G52) dipakai untuk menyesuaikan path GPU di
`SystemStatsReader`, tapi path itu sendiri belum diverifikasi terbaca di
device itu — hanya riset dokumentasi chipset.

## Belum ada sama sekali / belum selesai
- Testing kompatibilitas nyata di berbagai device/Android version
- GitHub Actions belum pernah benar-benar sukses build sampai akhir (belum ada laporan hasil run dari user)
- Performance Profile belum ada tombol di UI (lihat di atas)
- Graphics Manager belum ada layar sendiri (datanya ada tapi belum ditampilkan ke user)
