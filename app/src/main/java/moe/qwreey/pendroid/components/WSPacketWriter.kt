package moe.qwreey.pendroid.components

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import moe.qwreey.pendroid.components.motionbox.FingerHandle
import moe.qwreey.pendroid.components.motionbox.StylusHandle
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WSPacketWriter(val context: Context, var wsService: WSService?) {
    private val displayMetrics by lazy { DisplayMetrics().also {
        metrics -> (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
    } }

    private val stylusBuffer = ByteBuffer.allocate(20).apply {
        order(ByteOrder.LITTLE_ENDIAN)
    }
    private val stylusArray = stylusBuffer.array()
    private val initBuffer = ByteBuffer.allocate(5).apply {
        order(ByteOrder.LITTLE_ENDIAN)
    }
    private val initArray = initBuffer.array()
    private val touchBuffer = ByteBuffer.allocate(16).apply {
        order(ByteOrder.LITTLE_ENDIAN)
    }
    private val touchArray = touchBuffer.array()

    fun hasConnection(): Boolean {
        return wsService?.hasConnection ?: false
    }

    fun getInit(): ByteArray {
        initBuffer.clear()
        initBuffer.put(0x0)

        initBuffer.putShort(displayMetrics.widthPixels.toShort())
        initBuffer.putShort(displayMetrics.heightPixels.toShort())

        return initArray
    }

    fun writeStylus(data: StylusHandle) {
        stylusBuffer.clear()
        stylusBuffer.put(0x1)

        stylusBuffer.put( (
            (if (data.down) { 0b0000_0001 } else { 0 }) // (Tip Switch)
            or (if (data.button) { 0b0000_0010 } else { 0 }) // (Eraser Switch)
            or (if (data.hover) { 0b0000_0100 } else { 0 }) // (In Range, Hovering)
        ).toByte() )

        // Write abs datas
        stylusBuffer.putShort(data.pressure.toShort())
        stylusBuffer.putShort((data.tiltX * 100).toShort())
        stylusBuffer.putShort((data.tiltY * 100).toShort())
        stylusBuffer.putShort(data.x.toShort())
        stylusBuffer.putShort(data.y.toShort())
        stylusBuffer.putInt(data.timestamp)

        wsService?.broadcast(stylusArray)
    }

    private fun writeTouch(touch: FingerHandle.Touch, len: Int) {
        touchBuffer.clear()

        touchBuffer.put(0x2)
        touchBuffer.put(touch.slot.toByte())
        touchBuffer.put(if (touch.down) 1 else 0)
        touchBuffer.put(len.toByte())
        touchBuffer.putInt(touch.trackingId)
        touchBuffer.putShort(touch.x.toShort())
        touchBuffer.putShort(touch.y.toShort())

        wsService?.broadcast(touchArray)
    }

    fun writeFinger(data: FingerHandle) {
        for (index in 0 ..< data.len) {
            val touch = data.touchList[index]
            if (touch == null || touch.slot > 4) continue
            writeTouch(touch, data.totalDown)
        }
    }
}
