package moe.qwreey.pendroid.components.motionbox

import kotlin.math.*
import android.os.Looper
import android.os.Handler
import android.view.MotionEvent

class StylusHandle(var callback: (handle: StylusHandle) -> Unit = {}) {
    companion object {
        // Samsung hard coded action type
        const val ACTION_ERASER_DOWN = 211
        const val ACTION_ERASER_UP = 212

        // Source: https://w3c.github.io/pointerevents/#converting-between-tiltx-tilty-and-altitudeangle-azimuthangle
        const val HALF_PI = PI / 2
        const val DOUBLE_PI = PI * 2
        const val THREE_TWOS_PI = (PI * 3) / 2
        const val RAD_TO_DEG = 180 / PI
        const val EPSILON = 0.000000001

        @Suppress("NOTHING_TO_INLINE")
        private inline fun spherical2tilt(altitudeAngle: Double, azimuthAngle: Double): Pair<Double, Double> {
            var tiltXrad = 0.0
            var tiltYrad = 0.0

            if (altitudeAngle < EPSILON) {
                // the pen is in the X-Y plane
                if (azimuthAngle < EPSILON || abs(azimuthAngle - DOUBLE_PI) < EPSILON) {
                    // pen is on positive X axis
                    tiltXrad = HALF_PI
                }
                if (abs(azimuthAngle - HALF_PI) < EPSILON) {
                    // pen is on positive Y axis
                    tiltYrad = HALF_PI
                }
                if (abs(azimuthAngle - PI) < EPSILON) {
                    // pen is on negative X axis
                    tiltXrad = -HALF_PI
                }
                if (abs(azimuthAngle - THREE_TWOS_PI) < EPSILON) {
                    // pen is on negative Y axis
                    tiltYrad = -HALF_PI
                }
                if (azimuthAngle > EPSILON && abs(azimuthAngle - HALF_PI) < EPSILON) {
                    tiltXrad = HALF_PI
                    tiltYrad = HALF_PI
                }
                if (abs(azimuthAngle - HALF_PI) > EPSILON && abs(azimuthAngle - PI) < EPSILON) {
                    tiltXrad = -HALF_PI
                    tiltYrad = HALF_PI
                }
                if (abs(azimuthAngle - PI) > EPSILON && abs(azimuthAngle - THREE_TWOS_PI) < EPSILON) {
                    tiltXrad = -HALF_PI
                    tiltYrad = -HALF_PI
                }
                if (abs(azimuthAngle - THREE_TWOS_PI) > EPSILON && abs(azimuthAngle - DOUBLE_PI) < EPSILON) {
                    tiltXrad = HALF_PI
                    tiltYrad = -HALF_PI
                }
            } else {
                val tanAlt = tan(altitudeAngle)

                tiltXrad = atan(cos(azimuthAngle) / tanAlt)
                tiltYrad = atan(sin(azimuthAngle) / tanAlt)
            }

            val tiltX = round(tiltXrad * RAD_TO_DEG)
            val tiltY = round(tiltYrad * RAD_TO_DEG)

            return Pair(tiltX, tiltY)
        }
    }

    // States
    var button: Boolean = false
        private set
    var tiltX: Int = 0
        private set
    var tiltY: Int = 0
        private set
    var pressure: Int = 0
        private set
    var down: Boolean = false
        private set
    var x: Int = 0
        private set
    var y: Int = 0
        private set
    var hover: Boolean = false
        private set
    var timestamp: Int = 0
        private set

    // Process motion event
    @Suppress("NOTHING_TO_INLINE")
    private inline fun processMotionEvent(ev: MotionEvent, hover: Boolean) {
        // Update down state
        down = hover && when (ev.action) {
            MotionEvent.ACTION_DOWN, ACTION_ERASER_DOWN -> true
            MotionEvent.ACTION_UP, ACTION_ERASER_UP -> false
            else -> down
        }

        // Update x/y/pressure/btn
        button = ev.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY != 0
        pressure = if (down) { (ev.pressure * 4096).roundToInt() } else { 0 }
        this.hover = hover
        x = ev.x.toInt()
        y = ev.y.toInt()
        timestamp = ev.eventTime.toInt()

        // Calc & Update tilts
        val altitudeAngle = HALF_PI - ev.getAxisValue(MotionEvent.AXIS_TILT).toDouble()
        val azimuthAngle = (ev.orientation + HALF_PI).mod(DOUBLE_PI)
        val tilts = spherical2tilt(altitudeAngle, azimuthAngle)
        tiltX = tilts.first.roundToInt().coerceIn(-90..90)
        tiltY = tilts.second.roundToInt().coerceIn(-90..90)

        callback.invoke(this)
    }

    // Deferred unhover event processor (due to down/leave state is unsure)
    // Cancel if touch down occur
    private val unhoverDelayed: Handler = Handler(Looper.getMainLooper());
    private fun unhover(ev: MotionEvent) {
        processMotionEvent(ev, false)
    }

    // MotionEvent handler
    fun hoverEvent(ev: MotionEvent) {
        unhoverDelayed.removeCallbacksAndMessages(null)
        if (ev.actionMasked == MotionEvent.ACTION_HOVER_EXIT) {
            // Add deferred unhover
            unhoverDelayed.postDelayed({
                unhover(ev)
            }, 10)
        } else {
            // Process hover
            processMotionEvent(ev, true)
        }
    }
    fun touchEvent(ev: MotionEvent) {
        unhoverDelayed.removeCallbacksAndMessages(null)
        processMotionEvent(ev, true)
    }
}
