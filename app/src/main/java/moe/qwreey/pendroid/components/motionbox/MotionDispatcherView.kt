package moe.qwreey.pendroid.components.motionbox

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout

// FrameLayout을 상속받는 커스텀 뷰 그룹을 만들어.
// FrameLayout은 자식 뷰들을 겹쳐서 쌓는 가장 단순한 레이아웃이야.
//class MotionDispatcherView(context: Context) : FrameLayout(context) {
class MotionDispatcherView(context: Context) : FrameLayout(context) {

    // 펜슬 라이브러리나 다른 곳으로 이벤트를 전달할 콜백 함수를 설정할 수 있도록 변수를 만들어.
    var onDispatchTouchEvent: ((MotionEvent) -> Unit)? = null
    var onDispatchGenericMotionEvent: ((MotionEvent) -> Unit)? = null

    // 이 View의 영역에서 터치 이벤트가 발생하면 가장 먼저 호출되는 함수!
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return super.dispatchTouchEvent(ev)
        // 이벤트가 null이 아닐 때만 우리가 설정한 콜백 함수를 호출해.
        onDispatchTouchEvent?.invoke(ev)
        return true
    }

    // 호버 이벤트(S펜이 화면에 닿지 않고 떠 있는 등)를 처리하고 싶을 때 사용해.
    override fun dispatchGenericMotionEvent(ev: MotionEvent?): Boolean {
        if (ev == null) return super.dispatchGenericMotionEvent(ev)
        onDispatchGenericMotionEvent?.invoke(ev)
        return true
    }
}
