package com.changedspecial.port

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipInputStream

object GameFiles {
    fun dir(ctx: Context) = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, "game")
    fun installed(ctx: Context) = File(dir(ctx), "Game.ini").exists()

    /** Ekstrak ZIP game (boleh dengan folder induk "ChangedSpecialPort/"), lalu terapkan patch script. */
    fun installZip(ctx: Context, uri: Uri, progress: (String) -> Unit) {
        val dest = dir(ctx).apply { mkdirs() }
        // Pass 1: cari prefix folder induk
        var prefix = ""
        ctx.contentResolver.openInputStream(uri)!!.use { raw ->
            ZipInputStream(raw.buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    if (e.name == "Game.ini") { prefix = ""; break }
                    if (e.name.endsWith("/Game.ini") && e.name.count { it == '/' } == 1) { prefix = e.name.removeSuffix("Game.ini"); break }
                }
            }
        }
        // Pass 2: ekstrak
        var n = 0
        val root = dest.canonicalPath
        ctx.contentResolver.openInputStream(uri)!!.use { raw ->
            ZipInputStream(raw.buffered(1 shl 16)).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    if (!e.name.startsWith(prefix)) continue
                    val rel = e.name.removePrefix(prefix)
                    if (rel.isEmpty()) continue
                    val out = File(dest, rel)
                    if (!out.canonicalPath.startsWith(root)) continue          // anti zip-slip
                    if (e.isDirectory) { out.mkdirs(); continue }
                    out.parentFile?.mkdirs()
                    out.outputStream().buffered(1 shl 16).use { z.copyTo(it) }
                    if (++n % 25 == 0) progress("Mengekstrak… $n file")
                }
            }
        }
        applyPatch(ctx)
        progress("Selesai: $n file")
    }

    /** Timpa Data/Scripts.rvdata dengan versi yang sudah dibersihkan dari Win32API. */
    fun applyPatch(ctx: Context) {
        val out = File(dir(ctx), "Data/Scripts.rvdata").apply { parentFile?.mkdirs() }
        ctx.assets.open("patch/Data/Scripts.rvdata").use { i -> out.outputStream().use { i.copyTo(it) } }
    }

    /** mkxp.json — sesuaikan nama kunci dengan fork mkxp-z yang dipakai. */
    fun writeConfig(ctx: Context, p: Prefs) {
        val j = JSONObject()
            .put("rgssVersion", 2)
            .put("gameFolder", dir(ctx).absolutePath)
            .put("defScreenW", 544).put("defScreenH", 416)
            .put("fullscreen", true)
            .put("fixedAspectRatio", p.fixedAspect)
            .put("smoothScaling", p.smooth)
            .put("vsync", p.vsync)
            .put("syncToRefreshrate", p.vsync)
            .put("debugMode", false)
        File(dir(ctx), "mkxp.json").writeText(j.toString(2))
    }
}
