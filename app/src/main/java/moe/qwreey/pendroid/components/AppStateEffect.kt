package moe.qwreey.pendroid.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 앱의 포그라운드/백그라운드 상태 변화를 감지하여 콜백을 실행하는 Composable.
 * @param onAppForegrounded 앱이 포그라운드로 전환될 때 실행될 람다.
 * @param onAppBackgrounded 앱이 백그라운드로 전환될 때 실행될 람다.
 */
@Composable
fun AppStateEffect(
    onAppForegrounded: () -> Unit = {},
    onAppBackgrounded: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                onAppForegrounded()
            } else if (event == Lifecycle.Event.ON_STOP) {
                onAppBackgrounded()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
