package moe.qwreey.pendroid.components

import android.bluetooth.BluetoothDevice
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

@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun ConnectDialog(
    devices: List<BluetoothDevice>,
    onDeviceSelected: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {}
) {
    var selectedDevice by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("연결할 기기 선택") },
        text = {
            LazyColumn {
                items(devices) { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDevice = device.address }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (device.address == selectedDevice),
                            onClick = { selectedDevice = device.address }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(device.name)
                    }
                }
            }
        },

        // 3. 확인, 취소 버튼들
        confirmButton = {
            TextButton(
                onClick = {
                    selectedDevice?.let { onDeviceSelected(it) }
                    onDismissRequest()
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
