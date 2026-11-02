package com.changedspecial.port

import android.hardware.input.InputManager
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import org.libsdl.app.SDLActivity

/**
 * Activity game. Mewarisi SDLActivity (SDL2 Java dari build mkxp-z Android).
 * Menyuntik tombol keyboard virtual ke SDL: vGamepad + gamepad fisik (lewat bind) berbagi satu "sink".
 */
class GameActivity : SDLActivity() {
    private lateinit var prefs: Prefs
    private var pad: VGamepadView? = null
    private val counts = HashMap<Int, Int>()
    private val axisHeld = HashSet<Act>()
    private var binds: Map<Int, Act> = emptyMap()
    private lateinit var im: InputManager
    private val devListener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(id: Int) = refreshPadVisibility()
        override fun onInputDeviceRemoved(id: Int) = refreshPadVisibility()
        override fun onInputDeviceChanged(id: Int) {}
    }

    // Sesuaikan dengan .so hasil build mkxp-z Anda (urutan = urutan dependensi).
    override fun getLibraries(): Array<String> = arrayOf("SDL2", "mkxp-z")

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        GameFiles.writeConfig(this, prefs)
        Renderer.apply(this, prefs.renderer)
        super.onCreate(savedInstanceState)
        binds = prefs.binds()
        if (prefs.vpadEnabled) {
            pad = VGamepadView(this, prefs) { a, d -> send(a.target, d) }
            addContentView(pad, FrameLayout.LayoutParams(-1, -1))
        }
        im = getSystemService(INPUT_SERVICE) as InputManager
        im.registerInputDeviceListener(devListener, null)
        refreshPadVisibility()
    }

    override fun onDestroy() { im.unregisterInputDeviceListener(devListener); super.onDestroy() }

    private fun gamepadConnected() = InputDevice.getDeviceIds().any {
        val s = InputDevice.getDevice(it)?.sources ?: 0
        (s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD || (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }
    private fun refreshPadVisibility() {
        pad?.visibility = if (prefs.vpadAutoHide && gamepadConnected()) View.GONE else View.VISIBLE
        if (pad?.visibility == View.GONE) pad?.releaseAll()
    }

    /** Refcount supaya vGamepad dan gamepad fisik tidak saling melepas tombol. */
    private fun send(code: Int, down: Boolean) {
        val c = counts[code] ?: 0
        if (down) { counts[code] = c + 1; if (c == 0) SDLActivity.onNativeKeyDown(code) }
        else if (c > 0) { counts[code] = c - 1; if (c == 1) SDLActivity.onNativeKeyUp(code) }
    }

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        val s = e.source
        val fromPad = (s and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
            (s and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (s and InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD
        if (!fromPad) return super.dispatchKeyEvent(e)          // keyboard asli: biarkan SDL
        if (e.keyCode == KeyEvent.KEYCODE_BACK) return super.dispatchKeyEvent(e)
        val act = binds[e.keyCode] ?: return true               // tombol tak ter-bind: telan (cegah jalur joystick SDL)
        if (e.action == KeyEvent.ACTION_DOWN && e.repeatCount == 0) send(act.target, true)
        else if (e.action == KeyEvent.ACTION_UP) send(act.target, false)
        return true
    }

    override fun dispatchGenericMotionEvent(e: MotionEvent): Boolean {
        if ((e.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && e.action == MotionEvent.ACTION_MOVE) {
            var x = e.getAxisValue(MotionEvent.AXIS_HAT_X); var y = e.getAxisValue(MotionEvent.AXIS_HAT_Y)
            if (x == 0f) x = e.getAxisValue(MotionEvent.AXIS_X)
            if (y == 0f) y = e.getAxisValue(MotionEvent.AXIS_Y)
            axis(Act.LEFT, x < -0.5f); axis(Act.RIGHT, x > 0.5f); axis(Act.UP, y < -0.5f); axis(Act.DOWN, y > 0.5f)
            return true
        }
        return super.dispatchGenericMotionEvent(e)
    }
    private fun axis(a: Act, on: Boolean) {
        if (on && axisHeld.add(a)) send(a.target, true) else if (!on && axisHeld.remove(a)) send(a.target, false)
    }
}
