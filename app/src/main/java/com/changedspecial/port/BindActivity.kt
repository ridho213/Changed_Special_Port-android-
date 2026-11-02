package com.changedspecial.port

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.*

/** Gamepad bind: pilih aksi, lalu tekan tombol pada gamepad fisik. */
class BindActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var list: LinearLayout
    private lateinit var binds: MutableMap<Int, Act>

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        prefs = Prefs(this); binds = prefs.binds()
        val sv = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 32, 32, 32) }
        sv.addView(list); setContentView(sv)
        render()
    }

    private fun keyName(k: Int) = KeyEvent.keyCodeToString(k).removePrefix("KEYCODE_")

    private fun render() {
        list.removeAllViews()
        list.addView(TextView(this).apply { text = "Gamepad Bind"; textSize = 22f })
        list.addView(TextView(this).apply { text = "Ketuk aksi → tekan tombol gamepad. Satu aksi boleh punya beberapa tombol (tekan lagi untuk menambah, tahan lama = ganti)." ; setPadding(0, 8, 0, 24) })
        for (a in Act.values()) {
            val keys = binds.filter { it.value == a }.keys.joinToString(", ") { keyName(it) }.ifEmpty { "—" }
            list.addView(Button(this).apply {
                text = "${a.label}:  $keys"; isAllCaps = false
                setOnClickListener { capture(a, replace = false) }
                setOnLongClickListener { capture(a, replace = true); true }
            }, ViewGroup.LayoutParams(-1, -2))
        }
        list.addView(Button(this).apply { text = "Reset ke default"; setOnClickListener { prefs.resetBinds(); binds = prefs.binds(); render() } })
        list.addView(Button(this).apply { text = "Selesai"; setOnClickListener { finish() } })
    }

    private fun capture(a: Act, replace: Boolean) {
        val dlg = AlertDialog.Builder(this).setTitle(a.label)
            .setMessage("Tekan tombol gamepad…\n(Back untuk batal, Hapus untuk mengosongkan)")
            .setNeutralButton("Hapus") { _, _ -> binds.entries.removeAll { it.value == a }; prefs.saveBinds(binds); render() }
            .create()
        dlg.setOnKeyListener { d, code, ev ->
            if (ev.action != KeyEvent.ACTION_DOWN || ev.repeatCount != 0) return@setOnKeyListener true
            if (code == KeyEvent.KEYCODE_BACK) { d.dismiss(); return@setOnKeyListener true }
            if (replace) binds.entries.removeAll { it.value == a }
            binds[code] = a                       // tombol fisik yang sama hanya boleh satu aksi
            prefs.saveBinds(binds); d.dismiss(); render(); true
        }
        dlg.show()
    }
}
