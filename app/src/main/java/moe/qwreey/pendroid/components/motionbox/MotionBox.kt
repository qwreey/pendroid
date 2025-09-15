package moe.qwreey.pendroid.components.motionbox

import android.view.MotionEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MotionBox(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
    fingerCallback: (handle: FingerHandle) -> Unit = {},
    stylusCallback: (handle: StylusHandle) -> Unit = {}
) {
    val fingerCallbackUpdated = rememberUpdatedState(fingerCallback)
    val stylusCallbackUpdated = rememberUpdatedState(stylusCallback)

    val fingerHandle = remember { FingerHandle( { handle ->
        fingerCallbackUpdated.value.invoke(handle)
    }) }
    val stylusHandle = remember { StylusHandle( { handle ->
        stylusCallbackUpdated.value.invoke(handle)
    }) }

    // Box를 사용해서 TouchDispatcherView와 content를 겹치게 만들어.
    Box(modifier = modifier) {
        // AndroidView를 사용해서 TouchDispatcherView를 Compose에 삽입!
        AndroidView(
            // factory는 이 View가 처음 생성될 때 호출되는 람다야.
            factory = { context ->
                // 우리가 만든 커스텀 뷰를 생성해.
                MotionDispatcherView(context).apply {
                    // 여기서 onDispatchTouchEvent 콜백을 설정해!
                    // 펜슬 라이브러리의 이벤트 처리 함수를 연결해 주는 거지.
                    onDispatchTouchEvent = { motionEvent ->
                        when (motionEvent.getToolType(0)) {
                            MotionEvent.TOOL_TYPE_FINGER -> fingerHandle.touchEvent(motionEvent)
                            MotionEvent.TOOL_TYPE_STYLUS -> {
                                fingerHandle.reset()
                                stylusHandle.touchEvent(motionEvent)
                            }
                        }
                    }
                    onDispatchGenericMotionEvent = { motionEvent ->
                        if (motionEvent.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS) {
                            stylusHandle.hoverEvent(motionEvent)
                        }
                    }
                }
            },
            modifier = Modifier.matchParentSize()
        )
        // content 람다를 호출해서 자식 Composable들을 화면에 그려.
        // 이 Composable들은 TouchDispatcherView 위에 겹쳐서 보이게 돼.
        content()
    }
}
