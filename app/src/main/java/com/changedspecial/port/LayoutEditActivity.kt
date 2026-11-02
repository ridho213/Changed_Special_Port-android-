package com.changedspecial.port

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

/** Geser tombol vGamepad ke posisi favorit. Posisi tersimpan otomatis. */
class LayoutEditActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val p = Prefs(this)
        val root = FrameLayout(this)
        root.addView(VGamepadView(this, p, editMode = true), FrameLayout.LayoutParams(-1, -1))
        root.addView(TextView(this).apply { text = "Geser tombol • posisi tersimpan otomatis"; setTextColor(-1) },
            FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        root.addView(Button(this).apply { text = "Selesai"; setOnClickListener { finish() } },
            FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        root.addView(Button(this).apply { text = "Reset"; setOnClickListener { p.resetPositions(); recreate() } },
            FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        setContentView(root)
    }
}
