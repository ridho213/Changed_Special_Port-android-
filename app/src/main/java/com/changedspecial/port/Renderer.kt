package com.changedspecial.port

import android.content.Context
import android.content.pm.PackageManager
import android.system.Os
import java.io.File

/**
 * Pilihan renderer.
 *  - "gles"   : OpenGL ES native perangkat (default, paling kompatibel).
 *  - "vulkan" : OpenGL ES dijalankan di atas Vulkan lewat ANGLE (libEGL_angle.so + libGLESv2_angle.so
 *               harus ditaruh di app/src/main/jniLibs/<abi>/). mkxp memakai GL, jadi Vulkan = backend ANGLE.
 */
object Renderer {
    fun vulkanDeviceSupported(ctx: Context) =
        ctx.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 0)
    fun angleBundled(ctx: Context): Boolean {
        val d = File(ctx.applicationInfo.nativeLibraryDir)
        return File(d, "libEGL_angle.so").exists() && File(d, "libGLESv2_angle.so").exists()
    }
    fun vulkanAvailable(ctx: Context) = vulkanDeviceSupported(ctx) && angleBundled(ctx)

    /** Panggil SEBELUM super.onCreate() GameActivity (sebelum SDL memuat library). */
    fun apply(ctx: Context, choice: String) {
        if (choice == "vulkan" && vulkanAvailable(ctx)) {
            val d = ctx.applicationInfo.nativeLibraryDir
            Os.setenv("SDL_EGL_LIBRARY", "$d/libEGL_angle.so", true)
            Os.setenv("SDL_OPENGL_LIBRARY", "$d/libGLESv2_angle.so", true)
            Os.setenv("ANGLE_DEFAULT_PLATFORM", "vulkan", true)
        }
    }
}
