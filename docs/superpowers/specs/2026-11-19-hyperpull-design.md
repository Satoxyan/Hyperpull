# Desain: modul "HyperPull"

## Tujuan

Semua notifikasi biasa (bukan focus notification / super island) di HyperOS 4
punya handle tarik-turun yang berfungsi, sehingga bisa dibuka jadi floating
window — dengan memanfaatkan mekanisme native yang sudah ada, bukan membuat
freeform sendiri.

## Alasan

WhatsApp bisa ditarik, Telegram tidak, padahal Telegram bisa freeform manual.
Artinya gate-nya ada di kelayakan notifikasi (SystemUI), bukan di kemampuan app.
Repo HyperIsland membuktikan bar ada di layout (`heads_up_mini_window_bar.xml`
di dalam `status_bar_notification_row`) dan gesture dikendalikan field
`canSlide` yang dibaca `AppMiniWindowRowTouchHelper` saat `ACTION_DOWN`
(`NotificationMiniBarHook.kt`).

## Pendekatan terpilih

**A — paksa di `ExpandableNotificationRowInjector`.**

Hook `com.android.systemui.statusbar.notification.row.ExpandableNotificationRowInjector`:

- `updateMiniWindowBar()` — setelah method asli jalan.
- `updateFreeformTipView()` (no-arg) — setelah method asli jalan, karena method
  ini juga menimpa `canSlide` (terlihat dari pemakaian di repo acuan).

Setiap kali, paksa:

- `getMiniBar()` → `visibility = VISIBLE`, `alpha = 1f`
- field `canSlide = true`
- field `miniBarVisible = true`

Semua reflection dibungkus `runCatching` dan fail-open: kalau method/field tidak
ada, modul diam dan tidak mengubah apa pun.

Alternatif yang ditolak: (B) mencari method penentu kelayakan di SystemUI —
butuh decompile perangkat; (C) membuat gesture/peluncur freeform sendiri —
terlalu besar dan rapuh.

## Scope

- Hanya `com.android.systemui`, static scope.
- HyperOS 4 / Android 17. `minSdk 33`, `compileSdk 37`, libxposed API 102.
- Repo baru, Kotlin, tanpa Compose, tanpa layar setting, tanpa toggle.
- Package: `io.github.satoxyan.hyperpull`, nama modul: `HyperPull`.

## Non-goals

- Tidak menyentuh super island / focus notification.
- Tidak membuat gesture atau peluncur freeform sendiri.
- Tidak ada filter/blacklist per app di v1.

## Anti-goals

- SystemUI tidak boleh crash: kalau method/field tidak ditemukan, modul diam.
- Tidak mengubah notifikasi yang bukan baris notifikasi normal.

## Risiko utama

Sistem bisa tetap mengecek kelayakan saat gesture dilepas, sehingga bar bergerak
tapi window tidak terbuka. Kalau itu terjadi, langkah berikutnya adalah
pendekatan B. Modul v1 mencatat nilai `canSlide`/`miniBarVisible`, keberadaan
`getMiniBar()`, dan nama class ke log sekali per class agar diagnosis cepat.

## Verifikasi

1. `./gradlew :app:assembleDebug`, install APK.
2. Aktifkan modul di LSPosed, scope hanya System UI, reboot.
3. Kirim notifikasi Telegram, tarik bar bawah notifikasi.
4. Kalau gagal: `adb logcat -s HyperPull`.

Mesin pengembangan saat ini tidak punya JDK/Android SDK/perangkat, jadi build
dan uji akhir dilakukan di mesin pengguna.

## Struktur repo

```
android/
  app/src/main/kotlin/io/github/satoxyan/hyperpull/
    HyperPullModule.kt
    NotificationMiniWindowHook.kt
    Reflect.kt
  app/src/main/resources/META-INF/xposed/
    module.prop  scope.list  java_init.list
  README.md
```

## Keputusan yang diambil

- Tanpa UI/launcher: modul tetap terdeteksi LSPosed lewat metadata.
- Tanpa blacklist: app yang benar-benar tidak bisa freeform dibiarkan; bisa
  ditambah nanti kalau mengganggu.
- Toolchain mengikuti HyperIsland: AGP 9.5.1, Kotlin 2.4.21, Gradle 9.9.0,
  libxposed 102.0.0.
