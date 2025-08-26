package moe.qwreey.pendroid.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConnectDialog(
    devices: List<String>,
    onDeviceSelected: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {}
) {
    // 1. 어떤 기기를 선택했는지 기억하는 저장소!
    var selectedDevice by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("연결할 기기 선택") },
        // 2. 본문 내용에 기기 목록을 넣어줄 거야.
        text = {
            LazyColumn {
                items(devices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDevice = device } // 줄을 누르면 선택!
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (device == selectedDevice),
                            onClick = { selectedDevice = device }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(device)
                    }
                }
            }
        },
        // 3. 확인, 취소 버튼들
        confirmButton = {
            TextButton(
                onClick = {
                    selectedDevice?.let { onDeviceSelected(it) } // 선택한 기기를 알려주고
                    onDismissRequest() // 다이얼로그 닫기
                },
                // 4. 기기를 선택해야만 '연결' 버튼이 활성화!
                enabled = selectedDevice != null
            ) {
                Text("연결")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("취소")
            }
        }
    )
}
