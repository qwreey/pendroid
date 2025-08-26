package moe.qwreey.pendroid.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun DraggableButton(
    onClick: () -> Unit = {}
) {
    // 1. 버튼의 위치를 기억하고, 이 값이 바뀌면 화면을 다시 그릴거야!
    var offset by remember { mutableStateOf(Offset(0f, 0f)) }

    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = { onClick() },
            modifier = Modifier
                // 2. 기억해둔 offset 만큼 버튼의 위치를 옮겨줘!
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                // 3. 드래그를 감지하는 마법의 코드!
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                    }
                }
        ) {
            Icon(Icons.Filled.Add, contentDescription = "추가")
        }
    }
}