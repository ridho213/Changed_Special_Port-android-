package com.changedspecial.port

import android.content.Context
import android.graphics.*
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/** vGamepad overlay. Multi-touch; mode edit = geser tombol & simpan posisi. */
class VGamepadView(
    ctx: Context,
    private val prefs: Prefs,
    private val editMode: Boolean = false,
    private val send: (Act, Boolean) -> Unit = { _, _ -> }
) : View(ctx) {

    private class Btn(val id: String, val label: String, val act: Act?, val dx: Float, val dy: Float, val r: Float) {
        var cx = dx; var cy = dy
    }
    private val btns = mutableListOf<Btn>()
    private val held = mutableSetOf<Act>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private var dragging: Btn? = null

    init {
        // r = radius relatif terhadap tinggi layar
        btns += Btn("dpad", "", null, 0.16f, 0.68f, 0.22f)
        btns += Btn("ok", "OK", Act.OK, 0.90f, 0.72f, 0.095f)
        btns += Btn("cancel", "B", Act.CANCEL, 0.80f, 0.84f, 0.085f)
        btns += Btn("dash", "RUN", Act.DASH, 0.80f, 0.60f, 0.075f)
        btns += Btn("skip", "SKIP", Act.SKIP, 0.70f, 0.74f, 0.065f)
        btns += Btn("f1", "F1", Act.F1, 0.94f, 0.08f, 0.050f)
        if (prefs.vpadExtra || editMode) {
            btns += Btn("x", "X", Act.X, 0.90f, 0.46f, 0.055f)
            btns += Btn("y", "Y", Act.Y, 0.82f, 0.36f, 0.055f)
            btns += Btn("z", "Z", Act.Z, 0.90f, 0.30f, 0.055f)
            btns += Btn("l", "L", Act.L, 0.06f, 0.10f, 0.055f)
            btns += Btn("r", "R", Act.R, 0.82f, 0.10f, 0.055f)
        }
        btns.forEach { val (x, y) = prefs.pos(it.id, it.dx, it.dy); it.cx = x; it.cy = y }
    }

    private fun radius(b: Btn) = b.r * height * prefs.vpadScale / 100f
    private fun px(b: Btn) = b.cx * width
    private fun py(b: Btn) = b.cy * height

    override fun onDraw(c: Canvas) {
        val a = (prefs.vpadOpacity.coerceIn(10, 100) * 255 / 100)
        for (b in btns) {
            val r = radius(b); val x = px(b); val y = py(b)
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(a / 2, 255, 255, 255); c.drawCircle(x, y, r, paint)
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f
            paint.color = Color.argb(a, 255, 255, 255); c.drawCircle(x, y, r, paint)
            if (b.id == "dpad") {
                paint.style = Paint.Style.FILL
                fun tri(act: Act, ang: Double) {
                    paint.color = Color.argb(if (act in held) a else a / 2 + 30, 255, 255, 255)
                    val p = Path(); val d = r * 0.62f; val s = r * 0.2f
                    val ux = cos(ang).toFloat(); val uy = sin(ang).toFloat()
                    p.moveTo(x + ux * d, y + uy * d)
                    p.lineTo(x + ux * (d - s) - uy * s, y + uy * (d - s) + ux * s)
                    p.lineTo(x + ux * (d - s) + uy * s, y + uy * (d - s) - ux * s); p.close(); c.drawPath(p, paint)
                }
                tri(Act.RIGHT, 0.0); tri(Act.DOWN, PI / 2); tri(Act.LEFT, PI); tri(Act.UP, -PI / 2)
            } else {
                if (b.act in held) { paint.style = Paint.Style.FILL; paint.color = Color.argb(a, 255, 255, 255); c.drawCircle(x, y, r, paint) }
                text.textSize = r * 0.55f; text.color = if (b.act in held) Color.BLACK else Color.argb(a, 255, 255, 255)
                c.drawText(b.label, x, y + text.textSize * 0.35f, text)
            }
        }
    }

    private fun hit(x: Float, y: Float): Btn? =
        btns.firstOrNull { hypot(x - px(it), y - py(it)) <= radius(it) * (if (it.id == "dpad") 1.25f else 1f) }

    private fun setHeld(newSet: Set<Act>) {
        for (a in held - newSet) send(a, false)
        for (a in newSet - held) { send(a, true); if (prefs.vpadHaptic) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
        held.clear(); held.addAll(newSet); invalidate()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (editMode) {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { dragging = hit(e.x, e.y); return dragging != null }
                MotionEvent.ACTION_MOVE -> dragging?.let { it.cx = (e.x / width).coerceIn(0.03f, 0.97f); it.cy = (e.y / height).coerceIn(0.05f, 0.95f); invalidate() }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { dragging?.let { prefs.setPos(it.id, it.cx, it.cy) }; dragging = null }
            }
            return true
        }
        if (e.actionMasked == MotionEvent.ACTION_DOWN && hit(e.x, e.y) == null) return false  // teruskan ke SDL
        val now = mutableSetOf<Act>()
        if (e.actionMasked != MotionEvent.ACTION_UP && e.actionMasked != MotionEvent.ACTION_CANCEL) {
            for (i in 0 until e.pointerCount) {
                if (e.actionMasked == MotionEvent.ACTION_POINTER_UP && i == e.actionIndex) continue
                val x = e.getX(i); val y = e.getY(i)
                val b = hit(x, y) ?: continue
                if (b.id == "dpad") {
                    val dx = x - px(b); val dy = y - py(b); val dead = radius(b) * 0.22f
                    if (hypot(dx, dy) < dead) continue
                    val ang = atan2(dy, dx)
                    if (abs(cos(ang)) > 0.38) now += if (dx > 0) Act.RIGHT else Act.LEFT
                    if (abs(sin(ang)) > 0.38) now += if (dy > 0) Act.DOWN else Act.UP
                } else b.act?.let { now += it }
            }
        }
        setHeld(now)
        return true
    }

    fun releaseAll() = setHeld(emptySet())
}
