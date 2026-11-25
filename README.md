# LunchBudget — Android source project

Aplikasi Android native Kotlin + Jetpack Compose untuk mencatat budget makan siang bulanan.

## Fitur versi awal
- Budget bulanan dan jumlah hari kerja dapat diatur per bulan.
- Catat, ubah, dan hapus pengeluaran makan siang untuk tanggal hari ini.
- Dashboard budget, pengeluaran, sisa dana, dan riwayat.
- Penyimpanan lokal menggunakan SharedPreferences; tidak memerlukan akun atau internet.
- Perhitungan carry-over: sisa dana dibagi ulang ke jumlah hari kerja yang belum dicatat.

## Cara membuka
1. Ekstrak ZIP.
2. Buka folder `LunchBudget` melalui Android Studio.
3. Tunggu Gradle Sync selesai.
4. Sambungkan perangkat Android dengan USB debugging aktif atau gunakan emulator.
5. Klik Run untuk memasang aplikasi.

Untuk membuat APK: Android Studio → Build → Build Bundle(s) / APK(s) → Build APK(s).

## Catatan
Ini adalah source project versi awal. Belum disertai APK hasil build dan belum diuji pada perangkat fisik. Jika mengubah jumlah hari kerja atau budget setelah ada transaksi, sistem akan menghitung ulang alokasi berdasarkan saldo dan jumlah hari yang belum dicatat.
