package com.changedspecial.port

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Aksi RGSS2 + tombol keyboard yang dipahami mkxp (target dikirim ke SDL). */
enum class Act(val label: String, val target: Int) {
    UP("Atas", KeyEvent.KEYCODE_DPAD_UP), DOWN("Bawah", KeyEvent.KEYCODE_DPAD_DOWN),
    LEFT("Kiri", KeyEvent.KEYCODE_DPAD_LEFT), RIGHT("Kanan", KeyEvent.KEYCODE_DPAD_RIGHT),
    OK("OK / Konfirmasi (C)", KeyEvent.KEYCODE_Z), CANCEL("Batal / Menu (B)", KeyEvent.KEYCODE_X),
    DASH("Lari (A / Shift)", KeyEvent.KEYCODE_SHIFT_LEFT), SKIP("Skip dialog (Ctrl)", KeyEvent.KEYCODE_CTRL_LEFT),
    F1("Menu F1", KeyEvent.KEYCODE_F1),
    X("Tombol X (A)", KeyEvent.KEYCODE_A), Y("Tombol Y (S)", KeyEvent.KEYCODE_S), Z("Tombol Z (D)", KeyEvent.KEYCODE_D),
    L("L (Q / PageUp)", KeyEvent.KEYCODE_Q), R("R (W / PageDown)", KeyEvent.KEYCODE_W)
}

private class BoolPref(val sp: SharedPreferences, val k: String, val d: Boolean) : ReadWriteProperty<Any?, Boolean> {
    override fun getValue(t: Any?, p: KProperty<*>) = sp.getBoolean(k, d)
    override fun setValue(t: Any?, p: KProperty<*>, v: Boolean) { sp.edit().putBoolean(k, v).apply() }
}
private class IntPref(val sp: SharedPreferences, val k: String, val d: Int) : ReadWriteProperty<Any?, Int> {
    override fun getValue(t: Any?, p: KProperty<*>) = sp.getInt(k, d)
    override fun setValue(t: Any?, p: KProperty<*>, v: Int) { sp.edit().putInt(k, v).apply() }
}

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("launcher", Context.MODE_PRIVATE)

    // Renderer: "gles" (OpenGL ES native) | "vulkan" (ANGLE -> Vulkan)
    var renderer: String
        get() = sp.getString("renderer", "gles")!!
        set(v) { sp.edit().putString("renderer", v).apply() }

    // Tampilan
    var fixedAspect by BoolPref(sp, "fixedAspect", true)
    var smooth by BoolPref(sp, "smooth", false)
    var vsync by BoolPref(sp, "vsync", true)

    // vGamepad
    var vpadEnabled by BoolPref(sp, "vpad", true)
    var vpadAutoHide by BoolPref(sp, "vpadAutoHide", true)   // sembunyikan jika gamepad fisik terdeteksi
    var vpadExtra by BoolPref(sp, "vpadExtra", false)        // tombol X/Y/Z/L/R
    var vpadHaptic by BoolPref(sp, "vpadHaptic", true)
    var vpadOpacity by IntPref(sp, "vpadOpacity", 60)        // 10..100
    var vpadScale by IntPref(sp, "vpadScale", 100)           // 50..150

    fun pos(id: String, dx: Float, dy: Float) = Pair(sp.getFloat("px_$id", dx), sp.getFloat("py_$id", dy))
    fun setPos(id: String, x: Float, y: Float) { sp.edit().putFloat("px_$id", x).putFloat("py_$id", y).apply() }
    fun resetPositions() { sp.edit().apply { sp.all.keys.filter { it.startsWith("px_") || it.startsWith("py_") }.forEach { remove(it) } }.apply() }

    // Gamepad bind: keyCode fisik -> Act
    fun defaultBinds(): MutableMap<Int, Act> = mutableMapOf(
        KeyEvent.KEYCODE_DPAD_UP to Act.UP, KeyEvent.KEYCODE_DPAD_DOWN to Act.DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT to Act.LEFT, KeyEvent.KEYCODE_DPAD_RIGHT to Act.RIGHT,
        KeyEvent.KEYCODE_BUTTON_A to Act.OK, KeyEvent.KEYCODE_BUTTON_B to Act.CANCEL,
        KeyEvent.KEYCODE_BUTTON_X to Act.DASH, KeyEvent.KEYCODE_BUTTON_Y to Act.SKIP,
        KeyEvent.KEYCODE_BUTTON_L1 to Act.L, KeyEvent.KEYCODE_BUTTON_R1 to Act.R,
        KeyEvent.KEYCODE_BUTTON_START to Act.CANCEL, KeyEvent.KEYCODE_BUTTON_SELECT to Act.F1
    )
    fun binds(): MutableMap<Int, Act> {
        val s = sp.getString("binds", null) ?: return defaultBinds()
        val m = mutableMapOf<Int, Act>()
        s.split(';').filter { it.contains('=') }.forEach {
            val (k, a) = it.split('=')
            runCatching { m[k.toInt()] = Act.valueOf(a) }
        }
        return m
    }
    fun saveBinds(m: Map<Int, Act>) { sp.edit().putString("binds", m.entries.joinToString(";") { "${it.key}=${it.value.name}" }).apply() }
    fun resetBinds() { sp.edit().remove("binds").apply() }
}
