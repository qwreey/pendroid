package moe.qwreey.pendroid.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun FullscreenMode() {
    val view = LocalView.current

    LaunchedEffect(Unit) {
        val window = (view.context as Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)

        // 앱 화면을 시스템 바 뒤까지 꽉 채워서 그리도록
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 상태 표시줄과 내비게이션 바(제스처 바)를 모두 숨기기
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // 화면 가장자리를 스와이프해야만 시스템 바가 잠깐 나타나도록
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
