package com.nuani.asobiri.gamepad

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.hypot

/**
 * A translucent on-screen controller drawn over a game surface. Each control
 * injects an Android key code through [inject]; the caller forwards it to
 * whatever its engine reads (SDL native events, a dispatched KeyEvent, or a
 * synthesized DOM event). D-pad maps to the arrow keys, A to Enter
 * (advance/confirm), B to Escape (menu/back).
 *
 * The look deliberately follows the mkxp plugin's controller — a lighter rim
 * around a darker face, white glyphs, a proper cross D-pad — so the launcher
 * feels of a piece across engines, but the vector assets here are authored
 * fresh (mkxp-z's are GPL; this module is shared with permissive plugins).
 *
 * Only the controls consume touches: taps anywhere else fall through to the
 * game, so tap-to-advance still works. Everything is non-focusable so a real
 * hardware controller keeps input focus. Shared across plugins via :gamepad;
 * each plugin decides whether to add it (opt-in per engine).
 */
object GamepadOverlay {

    private const val TAG = "GamepadOverlay"

    // Faint by default like mkxp's controller — visible enough to aim at,
    // subtle enough not to fight the art. Fades in so it doesn't pop on launch.
    const val DEFAULT_OPACITY = 0.6f

    /**
     * @param opacity 0..1 steady-state alpha of the whole overlay.
     * @param scale multiplier on every control's size (1f is the design size);
     *   margins stay fixed so the cluster stays anchored to its corner.
     * @param diagonal when true the D-pad emits 8-way (diagonals), else 4-way.
     */
    fun create(
        context: Context,
        opacity: Float = DEFAULT_OPACITY,
        scale: Float = 1f,
        diagonal: Boolean = false,
        inject: (keycode: Int, down: Boolean) -> Unit,
    ): View {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()
        fun sz(value: Int) = (value * density * scale).toInt()

        val root = FrameLayout(context).apply {
            isClickable = false
            isFocusable = false
            alpha = 0f
        }

        // Cross D-pad, bottom-left.
        val dpad = DpadView(context, diagonal, inject)
        root.addView(
            dpad,
            FrameLayout.LayoutParams(sz(132), sz(132)).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                setMargins(dp(20), 0, 0, dp(24))
            },
        )

        // Action buttons, bottom-right, offset diagonally like a controller's
        // face buttons: A (confirm) low-right, B (back) high-left.
        val actions = FrameLayout(context)
        actions.addView(
            circleButton(context, "B", KeyEvent.KEYCODE_ESCAPE, inject),
            FrameLayout.LayoutParams(sz(64), sz(64)).apply { gravity = Gravity.TOP or Gravity.START },
        )
        actions.addView(
            circleButton(context, "A", KeyEvent.KEYCODE_ENTER, inject),
            FrameLayout.LayoutParams(sz(64), sz(64)).apply { gravity = Gravity.BOTTOM or Gravity.END },
        )
        root.addView(
            actions,
            FrameLayout.LayoutParams(sz(150), sz(132)).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                setMargins(0, 0, dp(20), dp(24))
            },
        )

        val target = opacity.coerceIn(0f, 1f)
        root.post { root.animate().alpha(target).setDuration(250).start() }
        return root
    }

    // A round button whose body is the shared vector; the label rides on top as
    // ordinary centred text. Press dims it (mkxp's own press feedback); release
    // restores. Touch, not click — it's a raw input surface, not an a11y target.
    @SuppressLint("ClickableViewAccessibility")
    private fun circleButton(
        context: Context,
        label: String,
        keycode: Int,
        inject: (Int, Boolean) -> Unit,
    ): TextView = TextView(context).apply {
        text = label
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
        gravity = Gravity.CENTER
        background = context.getDrawable(R.drawable.pad_button_circle)
        isFocusable = false
        isFocusableInTouchMode = false
        setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    view.alpha = 0.5f
                    Log.d(TAG, "inject DOWN keycode=$keycode ($label)")
                    inject(keycode, true)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                    view.alpha = 1f
                    Log.d(TAG, "inject UP keycode=$keycode ($label)")
                    inject(keycode, false)
                    true
                }
                else -> false
            }
        }
    }
}

/**
 * A single cross-shaped pad you touch anywhere on: it reads the touch position
 * relative to its centre and emits a direction. In 4-way mode (mkxp's default)
 * the axis with the larger offset wins, so a menu never gets two directions at
 * once; in 8-way mode a second axis joins once it is a large enough fraction of
 * the dominant one, giving true diagonals. Sliding between directions releases
 * the old key(s) before pressing the new one(s), so a held slide reads as
 * continuous movement.
 */
private class DpadView(
    context: Context,
    private val diagonal: Boolean,
    private val inject: (keycode: Int, down: Boolean) -> Unit,
) : View(context) {

    // The directions currently held down (a set so the diff on each move is
    // trivial). At most one element in 4-way mode, but modelled as a set so the
    // release/press bookkeeping stays uniform.
    private var pressed: Set<Int> = emptySet()

    init {
        background = context.getDrawable(R.drawable.pad_dpad)
        isFocusable = false
        contentDescription = "D-pad"
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_POINTER_DOWN,
            -> update(event.x, event.y)

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_POINTER_UP,
            -> release()
        }
        return true
    }

    private fun update(x: Float, y: Float) {
        val dx = x - width / 2f
        val dy = y - height / 2f
        // A centre dead zone avoids a jittery direction when the thumb rests on
        // the hub; below it, no direction is held.
        val dead = minOf(width, height) * 0.20f
        val target: Set<Int> = when {
            hypot(dx, dy) < dead -> emptySet()
            diagonal -> {
                // Include an axis once it reaches 40% of the dominant one, so a
                // near-cardinal touch stays cardinal and a true diagonal fires
                // both keys.
                val keys = mutableSetOf<Int>()
                val threshold = maxOf(abs(dx), abs(dy)) * 0.4f
                if (abs(dx) >= threshold) {
                    keys.add(if (dx < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
                }
                if (abs(dy) >= threshold) {
                    keys.add(if (dy < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN)
                }
                keys
            }
            abs(dx) >= abs(dy) ->
                setOf(if (dx < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT)
            else ->
                setOf(if (dy < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN)
        }
        if (target == pressed) return
        (pressed - target).forEach {
            Log.d(TAG, "inject UP keycode=$it")
            inject(it, false)
        }
        (target - pressed).forEach {
            Log.d(TAG, "inject DOWN keycode=$it")
            inject(it, true)
        }
        pressed = target
    }

    private fun release() {
        pressed.forEach {
            Log.d(TAG, "inject UP keycode=$it")
            inject(it, false)
        }
        pressed = emptySet()
    }

    private companion object {
        const val TAG = "GamepadOverlay"
    }
}
