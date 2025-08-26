package moe.qwreey.pendroid.components

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
fun PermissionGate(
    // 권한이 모두 허용되었을 때 보여줄 실제 앱 화면
    content: @Composable () -> Unit
) {
    // 현재 컨텍스트에서 Activity를 가져오기. (앱을 종료할 때 필요)
    val activity = LocalContext.current as Activity

    // 권한 상태와 종료 다이얼로그 표시 여부를 관리할 상태 변수들
    var permissionsGranted by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // 필요한 블루투스 권한 목록을 안드로이드 버전에 맞게 정의
    val bluetoothPermissions = remember { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        listOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    } }

    // 권한 요청 결과를 처리할 런처(launcher)를 생성
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 사용자가 권한 요청에 응답하면 이 부분이 실행됨
        val allPermissionsGranted = permissions.values.all { it }
        if (!allPermissionsGranted) {
            // 하나라도 거부된 권한이 있다면, 사용자에게 알려주고 앱을 종료
            // 여기서는 종료 안내 다이얼로그를 띄우도록 상태를 변경
            showExitDialog = true
        } else {
            // 모든 권한이 허용되었다면, 상태를 변경해서 실제 앱 화면을 보여줌
            permissionsGranted = true
        }
    }

    // 권한 상태에 따라 다른 화면을 보여줌
    if (permissionsGranted) {
        // 권한이 모두 허용되었다면, 메인 컨텐츠를 보여줌
        content()
    } else if (showExitDialog) {
        // 권한이 거부되어 종료 다이얼로그를 띄워야 할 때
        AlertDialog(
            onDismissRequest = { /* 사용자가 바깥을 눌러도 닫히지 않게 비워둠 */ },
            title = { Text("알림") },
            text = { Text("블루투스를 허용해야 어플리케이션을 사용할 수 있습니다.") },
            confirmButton = {
                Button(onClick = {
                    // 확인 버튼을 누르면 앱을 종료해.
                    activity.finish()
                }) {
                    Text("확인")
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(bluetoothPermissions.toTypedArray())
    }
}
