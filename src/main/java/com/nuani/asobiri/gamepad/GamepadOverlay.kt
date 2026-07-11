package com.nuani.asobiri.gamepad

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout

/**
 * A translucent on-screen controller drawn over an SDL game surface. Each button
 * injects an Android key code through [inject]; the caller forwards it to SDL's
 * onNativeKeyDown/Up, which the engine reads as keyboard input — D-pad maps to
 * the arrow keys (menu focus), A to Enter (advance/confirm), B to Escape (menu).
 * Keyboard is the reliable target: SDL engines (Ren'Py, KiriKiri) always respond
 * to it, unlike engine-specific gamepad mappings.
 *
 * Only the buttons consume touches: taps anywhere else fall through to the game,
 * so tap-to-advance still works. Buttons are non-focusable so a real hardware
 * controller keeps SDL's input focus. Shared across plugins via the :gamepad
 * module; each plugin decides whether to add it (opt-in per engine).
 */
object GamepadOverlay {

    private const val TAG = "GamepadOverlay"

    fun create(context: Context, inject: (keycode: Int, down: Boolean) -> Unit): View {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        val root = FrameLayout(context).apply {
            isClickable = false
            isFocusable = false
        }

        // D-pad as a 3x3 grid (corners empty), anchored bottom-left.
        val dpad = GridLayout(context).apply {
            columnCount = 3
            rowCount = 3
        }
        fun cell(row: Int, col: Int, button: View, sizeDp: Int) {
            val lp = GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(col))
            lp.width = dp(sizeDp)
            lp.height = dp(sizeDp)
            lp.setMargins(dp(2), dp(2), dp(2), dp(2))
            dpad.addView(button, lp)
        }
        val d = 56
        cell(0, 1, padButton(context, "▲", KeyEvent.KEYCODE_DPAD_UP, inject), d)
        cell(1, 0, padButton(context, "◀", KeyEvent.KEYCODE_DPAD_LEFT, inject), d)
        cell(1, 2, padButton(context, "▶", KeyEvent.KEYCODE_DPAD_RIGHT, inject), d)
        cell(2, 1, padButton(context, "▼", KeyEvent.KEYCODE_DPAD_DOWN, inject), d)
        root.addView(
            dpad,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                setMargins(dp(16), 0, 0, dp(24))
            },
        )

        // A (advance/confirm) and B (menu/back), anchored bottom-right.
        val actions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(
            padButton(context, "A", KeyEvent.KEYCODE_ENTER, inject),
            LinearLayout.LayoutParams(dp(64), dp(64)).apply { setMargins(dp(6), 0, dp(6), 0) },
        )
        actions.addView(
            padButton(context, "B", KeyEvent.KEYCODE_ESCAPE, inject),
            LinearLayout.LayoutParams(dp(64), dp(64)),
        )
        root.addView(
            actions,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, dp(16), dp(24))
            },
        )

        return root
    }

    // Touch (not click) so a held direction repeats naturally and press/release
    // map to key down/up. performClick is intentionally not used; the button is
    // a raw input surface, not an accessibility target.
    @SuppressLint("ClickableViewAccessibility")
    private fun padButton(
        context: Context,
        label: String,
        keycode: Int,
        inject: (Int, Boolean) -> Unit,
    ): Button = Button(context).apply {
        text = label
        alpha = 0.35f
        isFocusable = false
        isFocusableInTouchMode = false
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.alpha = 0.6f
                    Log.d(TAG, "inject DOWN keycode=$keycode ($label)")
                    inject(keycode, true)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.alpha = 0.35f
                    Log.d(TAG, "inject UP keycode=$keycode ($label)")
                    inject(keycode, false)
                    true
                }
                else -> false
            }
        }
    }
}
