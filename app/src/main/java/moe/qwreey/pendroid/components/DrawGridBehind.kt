package moe.qwreey.pendroid.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

// 재사용하기 좋게 Modifier 확장 함수로 만들었어!
fun Modifier.drawGridBehind(
    gridSize: Dp,
    gridColor: Color
): Modifier = this.drawBehind {
    // 1. Dp 단위를 Pixel로 바꿔줘야 그릴 수 있어.
    val gridSizePx = gridSize.toPx()

    // 2. 세로선 그리기: 화면 너비만큼 반복
    var x = gridSizePx
    while (x < size.width) {
        drawLine(
            color = gridColor,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f
        )
        x += gridSizePx
    }

    // 3. 가로선 그리기: 화면 높이만큼 반복
    var y = gridSizePx
    while (y < size.height) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += gridSizePx
    }
}
