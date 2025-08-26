package moe.qwreey.pendroid.components.motionbox

import android.view.MotionEvent
import kotlin.math.absoluteValue

class FingerHandle(var callback: (handle: FingerHandle) -> Unit = {}) {
    class TrackingIdHandle(val len: Int) {
        companion object {
            const val TRACKING_MAX = 65535
        }

        private var lastTrackingId = 0
        private var trackingIdList: IntArray = IntArray(len) { -1 }
        private fun nextTracking(): Int {
            lastTrackingId = (lastTrackingId+1) % TRACKING_MAX
            return lastTrackingId
        }
        fun slotTrackingId(slot: Int): Int {
            var trackingId = trackingIdList[slot]
            if (trackingId == -1) {
                trackingId = nextTracking()
                trackingIdList[slot] = trackingId
            }
            return trackingId
        }
        fun clearSlot(slot: Int): Boolean {
            val result = trackingIdList[slot] != -1
            trackingIdList[slot] = -1
            return result
        }
        fun length(): Int {
            return len - trackingIdList.count { it == -1 }
        }
    }

    class Touch(val x: Int, val y: Int, val slot: Int, val trackingId: Int) {
        companion object {
            fun uninit(slot: Int): Touch {
                return Touch(-1, -1, slot, -1)
            }
        }
    }

    companion object {
        const val TOUCH_MAX: Int = 4

        fun isDown(ev: MotionEvent, index: Int): Boolean {
            return if (ev.action == MotionEvent.ACTION_UP) {
                false
            } else if (ev.actionIndex == index) {
                ev.actionMasked != MotionEvent.ACTION_POINTER_UP
            } else { true }
        }
    }

    private val trackingIds = TrackingIdHandle(TOUCH_MAX)
    var touchList: Array<Touch?> = arrayOfNulls(TOUCH_MAX)
        private set
    var len: Int = 0
        private set
    private var downTime: Long = -1;

    fun touchEvent(ev: MotionEvent) {
        len = 0
        if (downTime < 0 && -downTime == ev.downTime) {
            return
        } else if (downTime != ev.downTime) {
            downTime = ev.downTime
        }
        for (index in 0..<ev.pointerCount) {
            val slot = ev.getPointerId(index)
            if (slot >= TOUCH_MAX) continue

            if (isDown(ev, index)) {
                touchList[len++] = Touch(ev.getX(index).toInt(), ev.getY(index).toInt(), slot, trackingIds.slotTrackingId(slot))
            } else if (trackingIds.clearSlot(slot)) {
                touchList[len++] = Touch.uninit(slot)
            }
        }
        if (len != 0) callback.invoke(this)
    }

    fun reset() {
        len = 0
        if (trackingIds.length() == 0) return
        for (slot in 0..<TOUCH_MAX) {
            if (trackingIds.clearSlot(slot)) touchList[len++] = Touch.uninit(slot)
        }
        downTime = -(downTime.absoluteValue)
        if (len != 0) callback.invoke(this)
    }
}
