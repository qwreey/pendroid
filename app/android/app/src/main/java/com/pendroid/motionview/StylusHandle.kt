package com.pendroid.motionview

import kotlin.math.*
import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.Arguments
import android.os.Looper
import android.os.Handler
import android.view.MotionEvent

class StylusHandle(val callback: Runnable) {
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
        private fun spherical2tilt(altitudeAngle: Double, azimuthAngle: Double): Pair<Double, Double> {
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
    private var button: Boolean = false
    private var tiltX: Int = 0
    private var tiltY: Int = 0
    private var pressure: Int = 0
    private var down: Boolean = false
    private var x: Int = 0
    private var y: Int = 0
    private var hover: Boolean = false

    // Process motion event
    private fun processMotionEvent(ev: MotionEvent, hover: Boolean) {
        // Update down state
        down = when (ev.action) {
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

        // Calc & Update tilts
        val altitudeAngle = HALF_PI - ev.getAxisValue(MotionEvent.AXIS_TILT).toDouble()
        val azimuthAngle = (ev.orientation + HALF_PI).mod(DOUBLE_PI)
        val tilts = spherical2tilt(altitudeAngle, azimuthAngle)
        tiltX = tilts.first.roundToInt().coerceIn(-90..90)
        tiltY = tilts.second.roundToInt().coerceIn(-90..90)

        callback.run()
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

    // Create argument map for js event
    fun getMap(): WritableMap {
        return Arguments.createMap().apply {
            putInt("tiltY", tiltY)
            putInt("tiltX", tiltX)
            putInt("pressure", pressure)
            putBoolean("down", down)
            putBoolean("hover", hover)
            putBoolean("button", button)
            putInt("x", x)
            putInt("y", y)
        }
    }
}
