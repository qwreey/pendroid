package moe.qwreey.pendroid.components

import android.Manifest
import androidx.annotation.RequiresPermission
import moe.qwreey.pendroid.components.motionbox.FingerHandle
import moe.qwreey.pendroid.components.motionbox.FingerHandle.Companion.TOUCH_MAX
import moe.qwreey.pendroid.components.motionbox.StylusHandle
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PacketWriter(var btHidService: BtHIDService) {
    // Stylus states
    // SEE: https://learn.microsoft.com/en-us/windows-hardware/design/component-guidelines/windows-pen-states
    private var eraserActivated: Boolean = false
    private var barrelActivated: Boolean = false
    private var lastButtonState: Boolean = false
    private var lastButtonStartTimestamp: Int = -1
    private val barrelTimeout: Int = 540

    // Finger states
    private var lastTouchs: Array<FingerHandle.Touch?> = arrayOfNulls(TOUCH_MAX)

    companion object {
        private val ID_STYLUS: Int = 1
        private val ID_TOUCHPAD: Int = 2
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun writeDropStylusState(data: StylusHandle, flags: Byte = 0) {
        val buf = ByteBuffer.allocate(11)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        buf.put(flags)
        buf.putShort(data.pressure.toShort())
        buf.putShort((data.tiltX * 100).toShort())
        buf.putShort((data.tiltY * 100).toShort())
        buf.putShort(data.x.toShort())
        buf.putShort(data.y.toShort())

        btHidService.writeReport(buf.array(), ID_STYLUS)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun writeStylus(data: StylusHandle) {
        dropAllTouchs()

        val buf = ByteBuffer.allocate(11)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        // When barrel state updated
        if (!lastButtonState && data.button) {
            if (lastButtonStartTimestamp == -1) {
                // First clicking
                lastButtonStartTimestamp = data.timestamp
            } else if (data.timestamp - lastButtonStartTimestamp < barrelTimeout) {
                // Second clicking
                barrelActivated = true
                lastButtonStartTimestamp = -1
            } else {
                // Timeout
                lastButtonStartTimestamp = data.timestamp
            }
        }
        lastButtonState = data.button

        // Cleanup states
        if (!data.hover) {
            barrelActivated = false
            lastButtonStartTimestamp = -1
            lastButtonState = false
        }
        if (!data.button) {
            barrelActivated = false
        }
        if (data.down) {
            lastButtonStartTimestamp = -1
        }

        // Update eraser state when only not down (for user convenience)
        if (data.button) {
            if (!data.down && !eraserActivated && !barrelActivated) {
                eraserActivated = true
                writeDropStylusState(data)
            }
        } else {
            if (!data.down && eraserActivated) {
                eraserActivated = false
                writeDropStylusState(data)
            }
        }

        // Write tip / eraser / range state
        buf.put( (
            (if (data.down) { 0b0000_0001 } else { 0 }) // (Tip Switch)
            or (if (eraserActivated) { 0b0000_0010 } else { 0 }) // (Eraser Switch)
            or if (data.hover) { 0b0000_0100 } else { 0 } // (In Range, Hovering)
            or (if (barrelActivated) { 0b0000_1000 } else { 0 }) // (Barrel Button, Button double tab)
        ).toByte() )

        // Write abs datas
        buf.putShort(data.pressure.toShort())
        buf.putShort((data.tiltX * 100).toShort())
        buf.putShort((data.tiltY * 100).toShort())
        buf.putShort(data.x.toShort())
        buf.putShort(data.y.toShort())

        btHidService.writeReport(buf.array(), ID_STYLUS)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun dropAllTouchs() {
        for ((slot, touch) in lastTouchs.withIndex()) {
            if (touch == null) continue

            val buf = ByteBuffer.allocate(6)
            buf.order(ByteOrder.LITTLE_ENDIAN)

            buf.put( (
                0
                or (slot shl 2)
            ).toByte() )
            buf.putShort(touch.x.toShort())
            buf.putShort(touch.y.toShort())
            buf.put(0.toByte())

            lastTouchs[slot] = null

            btHidService.writeReport(buf.array(), ID_TOUCHPAD)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun writeTouch(touch: FingerHandle.Touch, len: Int) {
        val buf = ByteBuffer.allocate(6)
        buf.order(ByteOrder.LITTLE_ENDIAN)

        // Tip and Slot
        buf.put( (
            1
            or (if (touch.x != -1) { 0b0010 } else { 0 })
            or (touch.slot shl 2)
        ).toByte() )

        // If up, use last pos
        if (touch.x == -1) {
            buf.putShort((lastTouchs[touch.slot]?.x ?: 0).toShort())
            buf.putShort((lastTouchs[touch.slot]?.y ?: 0).toShort())
        } else {
            // no diff
            val old = lastTouchs[touch.slot]
            if (old != null && old.x == touch.x && old.y == touch.y) {
                return;
            }
            buf.putShort(touch.x.toShort())
            buf.putShort(touch.y.toShort())
        }

        // update length
        buf.put(len.toByte())

        lastTouchs[touch.slot] = touch

        btHidService.writeReport(buf.array(), ID_TOUCHPAD)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    public fun writeFinger(data: FingerHandle) {
        for (index in 0 ..< data.len) {
            val touch = data.touchList[index]
            if (touch == null || touch.slot > 4) continue
            writeTouch(touch, data.len)
        }
    }
}
