package com.changedspecial.port

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.InputDevice
import android.view.ViewGroup
import android.widget.*
import kotlin.concurrent.thread

class LauncherActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var root: LinearLayout
    private val REQ_ZIP = 11

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = Prefs(this)
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 40, 40, 40) }
        setContentView(ScrollView(this).apply { addView(root) })
    }
    override fun onResume() { super.onResume(); build() }

    private fun h(t: String) = TextView(this).apply { text = t; textSize = 18f; setPadding(0, 36, 0, 8) }
    private fun add(v: android.view.View) = root.addView(v, ViewGroup.LayoutParams(-1, -2))
    private fun sw(t: String, v: Boolean, on: (Boolean) -> Unit) = Switch(this).apply {
        text = t; isChecked = v; setOnCheckedChangeListener { _, c -> on(c) }
    }
    private fun seek(t: String, min: Int, max: Int, v: Int, on: (Int) -> Unit) {
        val label = TextView(this).apply { text = "$t: $v" }
        add(label)
        add(SeekBar(this).apply {
            this.max = max - min; progress = v - min
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) { label.text = "$t: ${p + min}"; if (u) on(p + min) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        })
    }

    private fun build() {
        root.removeAllViews()
        add(TextView(this).apply { text = "Changed Special — Launcher"; textSize = 24f })

        // ---- Game
        add(h("Game"))
        val ok = GameFiles.installed(this)
        add(TextView(this).apply { text = if (ok) "✔ Game terpasang" else "Belum ada game. Pilih ChangedSpecialPort.zip"; })
        add(TextView(this).apply { text = GameFiles.dir(this@LauncherActivity).absolutePath; textSize = 11f })
        add(Button(this).apply { text = if (ok) "Pasang ulang dari ZIP" else "Impor game (ZIP)"; setOnClickListener { pickZip() } })

        // ---- Renderer
        add(h("Renderer"))
        val vkOk = Renderer.vulkanAvailable(this)
        val rg = RadioGroup(this)
        val gl = RadioButton(this).apply { text = "OpenGL ES (native, paling kompatibel)"; id = 1 }
        val vk = RadioButton(this).apply {
            text = "Vulkan (via ANGLE)" + if (vkOk) "" else when {
                !Renderer.vulkanDeviceSupported(this@LauncherActivity) -> " — HP tidak mendukung"
                else -> " — library ANGLE belum disertakan"
            }
            id = 2; isEnabled = vkOk
        }
        rg.addView(gl); rg.addView(vk)
        rg.check(if (prefs.renderer == "vulkan" && vkOk) 2 else 1)
        rg.setOnCheckedChangeListener { _, id -> prefs.renderer = if (id == 2) "vulkan" else "gles" }
        add(rg)
        add(sw("Rasio aspek tetap (544×416)", prefs.fixedAspect) { prefs.fixedAspect = it })
        add(sw("Smoothing (bilinear)", prefs.smooth) { prefs.smooth = it })
        add(sw("VSync", prefs.vsync) { prefs.vsync = it })

        // ---- vGamepad
        add(h("vGamepad"))
        add(sw("Aktifkan vGamepad", prefs.vpadEnabled) { prefs.vpadEnabled = it })
        add(sw("Sembunyikan otomatis jika gamepad fisik tersambung", prefs.vpadAutoHide) { prefs.vpadAutoHide = it })
        add(sw("Tombol tambahan (X, Y, Z, L, R)", prefs.vpadExtra) { prefs.vpadExtra = it })
        add(sw("Getar saat ditekan", prefs.vpadHaptic) { prefs.vpadHaptic = it })
        seek("Opasitas %", 10, 100, prefs.vpadOpacity) { prefs.vpadOpacity = it }
        seek("Ukuran %", 50, 150, prefs.vpadScale) { prefs.vpadScale = it }
        add(Button(this).apply { text = "Atur posisi tombol"; setOnClickListener { startActivity(Intent(this@LauncherActivity, LayoutEditActivity::class.java)) } })

        // ---- Gamepad
        add(h("Gamepad"))
        val pads = InputDevice.getDeviceIds().mapNotNull { InputDevice.getDevice(it) }
            .filter { (it.sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD || (it.sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK }
        add(TextView(this).apply { text = if (pads.isEmpty()) "Tidak ada gamepad tersambung" else "Terdeteksi: " + pads.joinToString { it.name } })
        add(Button(this).apply { text = "Gamepad bind"; setOnClickListener { startActivity(Intent(this@LauncherActivity, BindActivity::class.java)) } })

        // ---- Main
        add(Button(this).apply {
            text = "▶  MAIN"; textSize = 20f; isEnabled = ok
            setOnClickListener { startActivity(Intent(this@LauncherActivity, GameActivity::class.java)) }
        })
    }

    private fun pickZip() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = "*/*" }, REQ_ZIP)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data ?: return
        if (req != REQ_ZIP || res != RESULT_OK) return
        val tv = TextView(this).apply { text = "Memulai…"; setPadding(48, 32, 48, 32) }
        val dlg = android.app.AlertDialog.Builder(this).setTitle("Mengimpor game").setView(tv).setCancelable(false).show()
        thread {
            val msg = try { GameFiles.installZip(this, uri) { s -> runOnUiThread { tv.text = s } }; null } catch (t: Throwable) { t.message ?: t.toString() }
            runOnUiThread { dlg.dismiss(); if (msg != null) Toast.makeText(this, "Gagal: $msg", Toast.LENGTH_LONG).show(); build() }
        }
    }
}
