package com.pendroid.motionview

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewGroupManager
import com.facebook.react.views.view.ReactViewGroup

class MotionViewManager(
  private val callerContext: ReactApplicationContext
) : ViewGroupManager<ReactViewGroup>() {
  override fun getName() = REACT_CLASS
  override fun createViewInstance(context: ThemedReactContext) = MotionView(context)

  override fun getExportedCustomDirectEventTypeConstants(): MutableMap<String, Any> {
    val export = super.getExportedCustomDirectEventTypeConstants() ?: HashMap<String, Any?>()
    export["stylus"] = mapOf(
      "registrationName" to "onStylus",
    )
    export["finger"] = mapOf(
      "registrationName" to "onFinger",
    )
    return export
  }

  companion object {
    const val REACT_CLASS = "NativeMotionView"
  }
}
