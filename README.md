# Changed Special — Android port (launcher + vGamepad + renderer + gamepad bind)

Game: RPG Maker VX (RGSS2, `RGSS202E.dll`). Di Android game ini dijalankan oleh **mkxp-z** (re-implementasi RGSS).
Project ini = **launcher + GameActivity**; mesin mkxp-z (native) harus Anda build sendiri.

## Isi
- `patched-game-files/Data/Scripts.rvdata` — script game yang sudah dibersihkan (juga ada di `assets/patch/`, otomatis ditimpa saat impor ZIP).
- Launcher: impor ZIP game, pilih renderer (OpenGL ES / Vulkan via ANGLE), opsi tampilan, pengaturan vGamepad, gamepad bind.
- `VGamepadView`: D-pad + OK/B/RUN/SKIP/F1 (+X/Y/Z/L/R opsional), multitouch, opasitas/ukuran/posisi bisa diatur.
- `BindActivity`: tekan aksi → tekan tombol gamepad (long-press = ganti, tap = tambah). Stick kiri & hat = arah.

## Yang HARUS Anda lengkapi (belum bisa saya build di sini)
1. Salin Java SDL2 (`org/libsdl/app/*.java`) dari build mkxp-z Android → `app/src/main/java/org/libsdl/app/`.
2. Salin `.so` hasil build (libSDL2.so, libmkxp-z.so, dll) → `app/src/main/jniLibs/arm64-v8a/` (dan `armeabi-v7a/`).
3. Sesuaikan `GameActivity.getLibraries()` dan kunci `mkxp.json` (`GameFiles.writeConfig`) dengan fork mkxp-z Anda.
4. Untuk renderer **Vulkan**: taruh `libEGL_angle.so` + `libGLESv2_angle.so` (ANGLE, backend Vulkan) di `jniLibs/<abi>/`.
   Tanpa itu, opsi Vulkan otomatis nonaktif di launcher. (mkxp memakai OpenGL ES, jadi "Vulkan" = ANGLE GLES→Vulkan.)
5. Build: `gradle assembleDebug` (JDK 17, Android SDK 34).

## Perubahan pada script game (agar jalan di mkxp)
- `Screen` (Fullscreen++): isinya Win32API murni → di-stub.
- `Save`: hack `RtlMoveMemory` untuk marshal Bitmap → versi portabel (pakai `Bitmap#raw_data` bila ada; kalau tidak, thumbnail kosong, save tetap jalan).
- `window_GamepadTest`: tombol vGamepad palsu (sprite di title) dihapus — diganti overlay native.
- `Scene_Settings`: pesan "Gamepad" → arahkan ke Launcher.
Script lain tidak diubah.

## Build APK tanpa Android Studio
Upload folder ini ke repo GitHub → tab **Actions → Build APK** → unduh artifact `.apk`.
Workflow mengambil Java SDL2 otomatis, jadi launcher (impor ZIP, setting, vGamepad, bind) langsung ter-build.
Game baru bisa dijalankan setelah `.so` mkxp-z ditaruh di `jniLibs/` sebelum push.
