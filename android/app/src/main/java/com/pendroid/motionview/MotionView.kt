package com.pendroid.motionview

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import com.facebook.react.bridge.ReactContext
import com.facebook.react.uimanager.events.RCTEventEmitter
import com.facebook.react.views.view.ReactViewGroup

class MotionView(context: Context) : ReactViewGroup(context) {
    private val stylusHandle = StylusHandle(this::emitPen)
    private val fingerHandle = FingerHandle(this::emitFinger)

    private fun emitPen() {
        val reactContext = context as ReactContext
        reactContext
            .getJSModule(RCTEventEmitter::class.java)
            .receiveEvent(id, "stylus", stylusHandle.getMap())
    }
    private fun emitFinger() {
        val reactContext = context as ReactContext
        reactContext
            .getJSModule(RCTEventEmitter::class.java)
            .receiveEvent(id, "finger", fingerHandle.getMap())
    }

    override fun dispatchHoverEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return super.dispatchTouchEvent(ev)

        stylusHandle.hoverEvent(ev)

        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return super.dispatchTouchEvent(ev)

        when (ev.getToolType(0)) {
            MotionEvent.TOOL_TYPE_FINGER -> fingerHandle.touchEvent(ev)
            MotionEvent.TOOL_TYPE_STYLUS -> {
                fingerHandle.reset()
                stylusHandle.touchEvent(ev)
            }
        }

        return true
    }
}
