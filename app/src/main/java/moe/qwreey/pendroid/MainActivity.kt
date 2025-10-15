package moe.qwreey.pendroid

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import moe.qwreey.pendroid.components.AppStateEffect
import moe.qwreey.pendroid.components.BtHIDService
import moe.qwreey.pendroid.components.ConnectDialog
import moe.qwreey.pendroid.components.FullscreenMode
import moe.qwreey.pendroid.components.KeepScreenOn
import moe.qwreey.pendroid.components.BtHIDPacketWriter
import moe.qwreey.pendroid.components.PermissionGate
import moe.qwreey.pendroid.components.WSPacketWriter
import moe.qwreey.pendroid.components.WSService
import moe.qwreey.pendroid.components.drawGridBehind
import moe.qwreey.pendroid.components.motionbox.MotionBox
import moe.qwreey.pendroid.ui.theme.PendroidTheme

class MainActivity : ComponentActivity() {
    var keyDownHandle: ((Int, KeyEvent?) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FullscreenMode()
            PendroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PermissionGate {
                        MainView(
                            activity = this,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyDownHandle?.invoke(keyCode, event) == true) return true
        return super.onKeyDown(keyCode, event)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission") // Permissions are checked and ensured by PermissionGate
@Composable
fun MainView(modifier: Modifier = Modifier, activity: MainActivity? = null) {
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showConnectionDialog by remember { mutableStateOf(false) }
    var hidConnected by remember { mutableStateOf(false) }
    var wsConnected by remember { mutableStateOf(false) }

    // 서비스 핸들링
    val hidService = remember { BtHIDService(context,
        handleDisconnectedAll = {
            hidConnected = false
        },
        handleConnected = {
            hidConnected = true
        },
    ) }
    val hidPacketWriter = remember { BtHIDPacketWriter(hidService) }
    val wsPacketWriter = remember { WSPacketWriter(context, null) }

    // 네이티브 볼륨 처리 수행
    var volumeUpPressed by remember { mutableStateOf(0) }
    var volumeDownPressed by remember { mutableStateOf(0) }
    activity?.keyDownHandle = { keyCode, event ->
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                volumeUpPressed++
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                volumeDownPressed++
                true
            }
            else -> false
        }
    }

    // 앱이 포그라운드로 돌아올 때 hid 서비스 초기화
    AppStateEffect(
        onAppForegrounded = {
            hidService.initService()
            wsPacketWriter.wsService = WSService(23227).apply {
                isTcpNoDelay = true
                isReuseAddr = true
                openHandle = { conn ->
                    conn?.send(wsPacketWriter.getInit())
                    wsConnected = true
                }
                closeHandle = { conn, code, reason ->
                    wsConnected = false
                }
                start()
            }
        },
        onAppBackgrounded = {
            wsPacketWriter.wsService?.stop()
        }
    )

    LaunchedEffect(key1 = volumeUpPressed) {
        if (volumeUpPressed > 0) {
            showBottomSheet = !showBottomSheet
        }
    }
    LaunchedEffect(key1 = volumeDownPressed) {
        if (volumeDownPressed > 0) {
            showConnectionDialog = !showConnectionDialog
        }
    }

    if (wsConnected || hidConnected) {
        KeepScreenOn()
    }

    if (showConnectionDialog) {
        ConnectDialog(
            devices = hidService.listDevices(),
            onDeviceSelected = { address ->
                hidService.connectTo(address)
                showConnectionDialog = false
            }
        )
    }
    
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                // 바깥쪽을 누르거나 아래로 쓸어내리면 스위치를 끈다!
                showBottomSheet = false
            },
            sheetState = sheetState
        ) {
            // 4. 여기에 설정 화면 내용을 채우면 돼!
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("설정창")
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = {
                    // 닫기 버튼을 누르면 스르륵 닫히게~
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showBottomSheet = false
                        }
                    }
                }) {
                    Text("닫기")
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
    MotionBox(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0, 0, 0))
            .drawGridBehind(
                gridSize = 30.dp, // 격자 한 칸의 크기
                gridColor = Color(24,24,24) // 격자 선 색상
            ),
        fingerCallback = { handle ->
            if (wsPacketWriter.hasConnection()) {
                wsPacketWriter.writeFinger(handle)
            } else {
                hidPacketWriter.writeFinger(handle)
            }
        },
        stylusCallback = { handle ->
            if (wsPacketWriter.hasConnection()) {
                wsPacketWriter.writeStylus(handle)
            } else {
                hidPacketWriter.writeStylus(handle)
            }
        },
//        content = { Box {
//            DraggableButton( onClick = { showBottomSheet = true } )
//        } }
    )
}

@Preview(showBackground = true)
@Composable
fun MainViewPreview() {
    PendroidTheme {
        MainView()
    }
}
