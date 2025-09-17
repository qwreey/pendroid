package moe.qwreey.pendroid.components

import android.Manifest
import androidx.annotation.RequiresPermission
import moe.qwreey.pendroid.components.motionbox.FingerHandle
import moe.qwreey.pendroid.components.motionbox.FingerHandle.Companion.TOUCH_MAX
import moe.qwreey.pendroid.components.motionbox.StylusHandle
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BtHIDPacketWriter(var btHidService: BtHIDService) {
    // Stylus states
    // SEE: https://learn.microsoft.com/en-us/windows-hardware/design/component-guidelines/windows-pen-states
    private var eraserActivated: Boolean = false
    private var barrelActivated: Boolean = false
    private var lastButtonState: Boolean = false
    private var lastButtonStartTimestamp: Int = -1
    private val barrelTimeout: Int = 800

    // Finger states
    private val lastTouches: Array<FingerHandle.Touch?> = arrayOfNulls(TOUCH_MAX)

    // Buffers
    private val stylusBuffer = ByteBuffer.allocate(11).apply {
        order(ByteOrder.LITTLE_ENDIAN)
    }
    private val stylusBufferArr = stylusBuffer.array()
    private val touchBuffer = ByteBuffer.allocate(6).apply {
        order(ByteOrder.LITTLE_ENDIAN)
    }
    private val touchBufferArr = touchBuffer.array()

    companion object {
        private val ID_STYLUS: Int = 1
        private val ID_TOUCHPAD: Int = 2
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun writeDropStylusState(data: StylusHandle, flags: Byte = 0) {
        stylusBuffer.clear()

        stylusBuffer.put(flags)
        stylusBuffer.putShort(data.pressure.toShort())
        stylusBuffer.putShort((data.tiltX * 100).toShort())
        stylusBuffer.putShort((data.tiltY * 100).toShort())
        stylusBuffer.putShort(data.x.toShort())
        stylusBuffer.putShort(data.y.toShort())

        btHidService.writeReport(stylusBufferArr, ID_STYLUS)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeStylus(data: StylusHandle) {
        dropAllTouches()

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
        if (!data.button && barrelActivated) {
            barrelActivated = false
            lastButtonStartTimestamp = -1
        }
        if (data.down) {
            lastButtonStartTimestamp = -1
        }

        // Update eraser state when only not down (for user convenience)
        if (data.button) {
            if (!data.down && !eraserActivated && !barrelActivated) {
                eraserActivated = true
                writeDropStylusState(data, 0b0000_0010)
            }
        } else {
            if (!data.down && eraserActivated) {
                eraserActivated = false
                writeDropStylusState(data)
            }
        }

        stylusBuffer.clear()

        // Write tip / eraser / range state
        stylusBuffer.put( (
            (if (data.down) { 0b0000_0001 } else { 0 }) // (Tip Switch)
            or (if (eraserActivated) { 0b0000_0010 } else { 0 }) // (Eraser Switch)
            or (if (data.hover) { 0b0000_0100 } else { 0 }) // (In Range, Hovering)
            or (if (barrelActivated) { 0b0000_1000 } else { 0 }) // (Barrel Button, Button double tab)
        ).toByte() )

        // Write abs datas
        stylusBuffer.putShort(data.pressure.toShort())
        stylusBuffer.putShort((data.tiltX * 100).toShort())
        stylusBuffer.putShort((data.tiltY * 100).toShort())
        stylusBuffer.putShort(data.x.toShort())
        stylusBuffer.putShort(data.y.toShort())

        btHidService.writeReport(stylusBufferArr, ID_STYLUS)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun dropAllTouches() {
        for ((slot, touch) in lastTouches.withIndex()) {
            if (touch == null || !touch.down) continue

            touchBuffer.clear()

            touchBuffer.put( (
                0
                or (slot shl 2)
            ).toByte() )
            touchBuffer.putShort(touch.x.toShort())
            touchBuffer.putShort(touch.y.toShort())
            touchBuffer.put(0.toByte())

            lastTouches[slot] = null

            btHidService.writeReport(touchBufferArr, ID_TOUCHPAD)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun writeTouch(touch: FingerHandle.Touch, len: Int) {
        touchBuffer.clear()

        // Tip and Slot
        touchBuffer.put( (
            1
            or (if (touch.down) { 0b0010 } else { 0 })
            or (touch.slot shl 2)
        ).toByte() )

        // no diff
        val old = lastTouches[touch.slot]
        if (FingerHandle.Touch.checkNotChanged(old, touch)) {
            return
        }

        // If no position, use last pos
        if (touch.x == -1) {
            touchBuffer.putShort((old?.x ?: 0).toShort())
            touchBuffer.putShort((old?.y ?: 0).toShort())
        } else {
            touchBuffer.putShort(touch.x.toShort())
            touchBuffer.putShort(touch.y.toShort())
        }

        // update length
        touchBuffer.put(len.toByte())

        lastTouches[touch.slot] = touch

        btHidService.writeReport(touchBufferArr, ID_TOUCHPAD)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun writeFinger(data: FingerHandle) {
        for (index in 0 ..< data.len) {
            val touch = data.touchList[index]
            if (touch == null || touch.slot > 4) continue
            writeTouch(touch, data.len)
        }
    }
}
