# Awd R2Cloud Android ☁️🚀

<p align="center">
  <img src="logo.png" width="130" height="130" alt="Awd R2Cloud Android Logo">
</p>

<p align="center">
  <a href="https://github.com/putuwahyu29/awd-r2cloud-android/blob/main/LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square" alt="License Badge">
  </a>
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square&logo=android" alt="Android Badge">
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=flat-square&logo=kotlin" alt="Kotlin Badge">
  <img src="https://img.shields.io/badge/Framework-Jetpack%20Compose-navy?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose Badge">
  <img src="https://img.shields.io/badge/Architecture-MVVM--Clean-orange?style=flat-square" alt="MVVM Clean Architecture">
</p>

**Awd R2Cloud Android** adalah klien penyimpanan cloud profesional untuk Cloudflare R2. Kelola bucket Anda, otomatisasi cadangan latar belakang, dan kelola file melalui antarmuka Material 3 yang modern. Fokus pada privasi, performa, dan pengalaman pengguna yang mulus.

---

## 🌐 Bahasa
*   [Versi Bahasa Inggris](README.md)
*   [Versi Bahasa Indonesia (Utama)](README.id.md)

---

## 📌 Daftar Isi
- [✨ Fitur Utama](#-fitur-utama)
- [📷 Cuplikan Layar](#-screenshots)
- [📁 Keamanan & Penyimpanan](#-storage--security)
- [🚀 Persiapan](#-getting-started)
- [🛠️ Panduan Pengembang](#️-developer-guide)
- [⚠️ Penafian](#️-disclaimer)
- [📄 Lisensi](#-license)

---

## ✨ Fitur Utama

*   **☁️ Integrasi Cloudflare R2**: Dukungan penuh untuk penyimpanan R2 (S3-compatible). Kelola banyak akun dengan mudah.
*   **🔍 Pencarian Global & Filter**: Cari file di seluruh bucket secara instan. Filter berdasarkan kategori: Gambar, Video, atau Dokumen.
*   **🔄 Layanan Cadangan Otomatis**: Sinkronisasi folder lokal ke bucket R2 tertentu di latar belakang menggunakan Android WorkManager.
*   **🔒 Enkripsi Sisi Klien**: Enkripsi AES-256 opsional. File Anda dienkripsi di perangkat sebelum diunggah.
*   **📊 Pemantauan Penggunaan**: Pelacakan kuota penyimpanan dan operasi bulanan Kelas A/B secara real-time.
*   **🎨 Desain Material 3**: Antarmuka bersih dengan dukungan penuh mode Gelap/Terang dan multibahasa (EN/ID).

---

## 📷 Cuplikan Layar

<p align="center">
  <img src="screenshoots/home.jpg" width="250" alt="Dashboard">
  <img src="screenshoots/bucket.jpg" width="250" alt="Bucket">
  <img src="screenshoots/list.jpg" width="250" alt="Penjelajah File">
</p>
<p align="center">
  <img src="screenshoots/preview.jpg" width="250" alt="Pratinjau File">
  <img src="screenshoots/backup.jpg" width="250" alt="Cadangan Otomatis">
  <img src="screenshoots/settings.jpg" width="250" alt="Pengaturan">
</p>

---

## 📁 Keamanan & Penyimpanan

*   **Kredensial Aman**: Semua kunci API dan token disimpan menggunakan `EncryptedSharedPreferences` (AES-256) Android.
*   **Kunci Biometrik**: Perlindungan aplikasi opsional menggunakan Sidik Jari atau Face ID.
*   **Data Pribadi**: Aplikasi berkomunikasi langsung dengan endpoint Cloudflare. Tidak ada data yang dikirim ke server pihak ketiga.

---

## 🚀 Persiapan

### Prasyarat
*   Akun Cloudflare dengan fitur R2 aktif.
*   Perangkat Android dengan versi 8.0 (API 26) atau lebih tinggi.

### Mendapatkan Kredensial
Untuk menggunakan aplikasi ini, Anda butuh:
1. **Account ID**: Dapat ditemukan di Dashboard Cloudflare (R2 overview).
2. **Access Key ID & Secret Access Key**: Buat di menu R2 -> Manage R2 API Tokens.
3. **API Token (Optional)**: Diperlukan untuk melihat statistik penggunaan dan mengelola domain publik.

---

## 🛠️ Panduan Pengembang

### Struktur Proyek
```
awd-r2cloud/
├── app/                    # Modul Inti Aplikasi Android
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/awd/r2cloud/
│   │   │   │   ├── data/        # Repository, Lokal, Remote
│   │   │   │   ├── domain/      # Model & Interface
│   │   │   │   ├── ui/          # Layar Compose & ViewModel
│   │   │   │   └── security/    # Logika Enkripsi
│   │   │   └── AndroidManifest.xml
└── .github/workflows       # Alur kerja CI/CD
```

### Membangun Aplikasi
```bash
./gradlew assembleDebug
```

---

## ⚠️ Penafian

Proyek ini adalah pengembangan independen dan tidak berafiliasi dengan Cloudflare. Pengguna bertanggung jawab penuh atas biaya penggunaan dan kepatuhan terhadap Ketentuan Layanan Cloudflare.

---

## 📄 Lisensi

Lisensi MIT. Lihat file [LICENSE](LICENSE) untuk detailnya.

<p align="center">Dibuat dengan ❤️ oleh <a href="mailto:aguswahyu@office.awd.my.id">I Putu Agus Wahyu Dupayana</a></p>
