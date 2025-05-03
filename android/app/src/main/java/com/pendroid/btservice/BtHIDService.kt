package com.pendroid.btservice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import okio.ByteString.Companion.decodeHex
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

class BtHIDService(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName() = "BtHIDService"
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val service = HIDService()

    companion object {
        private const val ID_STYLUS = 1
        private const val ID_TOUCHPAD = 2

        private fun intToLittleEndianBytes(value: Int, len: Int): IntArray {
            val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            buffer.putInt(value)
            return buffer
                .array()
                .sliceArray(0 ..< len)
                .map { item -> item.toUByte().toInt() }
                .toIntArray()
        }
    }

    private val displayMetrics by lazy { DisplayMetrics().also {
        metrics -> (reactContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getMetrics(metrics)
    } }

    private val fingerDescriptor by lazy { intArrayOf(
        0x05, 0x0D, // Usage Page (Digitizer)
        0x09, 0x05, // Usage (Touch Pad)
        0xA1, 0x01, // Collection (Application)
        0x85, ID_TOUCHPAD, // Report ID

        0x05, 0x0D, // Usage Page (Digitizer)
        0x09, 0x22, // Usage (Finger)
        0xA1, 0x02, // Collection (Logical)

        // 터치 접촉 여부
        0x05, 0x0D, // Usage Page (Digitizer)
        0x09, 0x47, // Usage (Confidence)
        0x09, 0x42, // USAGE (Tip switch)
        0x15, 0x00, // Logical Maximum (0)
        0x25, 0x01, // Logical Maximum (1)
        0x75, 0x01, // Report Size (1)
        0x95, 0x02, // Report Count (2)
        0x81, 0x02, // Input (Data,Var,Abs)

        // 접촉중인 터치 ID
        0x05, 0x0D, // Usage Page (Digitizer)
        0x09, 0x51, // Usage (Contact Identifier)
        0x15, 0x00, // Logical Maximum (0)
        0x25, 0x04, // Logical Maximum (4)
        0x75, 0x02, // Report Size (2)
        0x95, 0x01, // Report Count (1)
        0x81, 0x02, // Input (Data,Var,Abs)

        // padding
        0x75, 0x01, // Report Size (1)
        0x95, 0x04, // Report Count (4)
        0x81, 0x03, // Input (Cnst,Var,Abs)

        // 위치 X 를 정의합니다
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x30, // Usage (X)
        0x15, 0x00, // Logical Minimum (0)
        0x26, *intToLittleEndianBytes(displayMetrics.widthPixels, 2), // Logical Maximum (width)
        0x35, 0x00, // Physical Minimum (0)
        0x46, *intToLittleEndianBytes(displayMetrics.widthPixels.times(0.95).toInt(), 2), // Physical Maximum (width)
        0x55, 0x0E, // Unit Exponent (-2)
        0x65, 0x11, // Unit (SILinear: cm)
        0x75, 0x10, // Report Size (16)
        0x95, 0x01, // Report Count (1)
        0x81, 0x02, // Input (Data,Var,Abs)
        // 2 bytes

        // 위치 Y 를 정의합니다
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x31, // Usage (Y)
        0x15, 0x00, // Logical Minimum (0)
        0x26, *intToLittleEndianBytes(displayMetrics.heightPixels, 2), // Logical Maximum (height)
        0x35, 0x00, // Physical Minimum (0)
        0x46, *intToLittleEndianBytes(displayMetrics.heightPixels.times(0.95).toInt(), 2), // Physical Maximum (height)
        0x55, 0x0E, // Unit Exponent (-2)
        0x65, 0x11, // Unit (SILinear: cm)
        0x75, 0x10, // Report Size (16)
        0x95, 0x01, // Report Count (1)
        0x81, 0x02, // Input (Data,Var,Abs)
        // 2 bytes

        0xC0, // End Collection

        0x05, 0x0d, // Usage Page (Digitizers)
        0x09, 0x54, // Usage (Contact Count)
        0x15, 0x00, // Logical Minimum (0)
        0x25, 0x04, // Logical Maximum (4)
        0x75, 0x08, // Report Size (8)
        0x95, 0x01, // Report Count (1)
        // 1 bytes

        0xC0, // End Collection
    ) }

    private val stylusDescriptor by lazy { intArrayOf(
        // 디지타이저 컬랙션을 생성
        0x05, 0x0D, // Usage Page (Digitizer)
        0x09, 0x02, // Usage (Pen)
        0xA1, 0x01, // Collection (Application)
        0x85, ID_STYLUS, // Report ID
        0x09, 0x20, // Usage (Stylus)
        0xA1, 0x00, // Collection (Physical)

        // nth bit: 7 6 5 4 3 2 1 0
        //          . . . . . I B T
        // 팁 다운, 사이드버튼, 호버를 정의합니다
        // 남는 공간은 padding 으로 채워넣습니다
        0x09, 0x42, // Usage (Tip Switch)
        0x09, 0x45, // Usage (Eraser Switch)
        0x09, 0x32, // Usage (In Range)
        0x15, 0x00, // Logical Minimum (0)
        0x25, 0x01, // Logical Maximum (1)
        0x75, 0x01, // Report Size (1)
        0x95, 0x03, // Report Count (3)
        0x81, 0x02, // Input (Data,Var,Abs)
        0x75, 0x01, // Report Size (1)
        0x95, 0x05, // Report Count (5)
        0x81, 0x03, // Input (Cnst,Var,Abs)
        // 1 bytes

        // 압력을 정의합니다, 16비트 정수
        0x05, 0x0D,       // Usage Page (Digitizer)
        0x09, 0x30,       // Usage (Tip Pressure)
        0x15, 0x00,       // Logical Minimum (0)
        0x26, 0xFF, 0x0F, // Logical Maximum (4095)
        0x35, 0x00,       // Physical Minimum (0)
        0x46, 0xFF, 0x0F, // Physical Maximum (4095)
        0x75, 0x10,       // Report Size (16)
        0x95, 0x01,       // Report Count (1)
        0x81, 0x02,       // Input (Data,Var,Abs)
        // 2 bytes

        // 틸트 X, 틸트 Y 를 순서대로 정의하며, deg 유닛을 가집니다
        // tilt * 10^-2 지수표현을 사용하며, 두개의 16 비트를 사용합니다
        0x05, 0x0D,       // Usage Page (Digitizer)
        0x09, 0x3D,       // Usage (X Tilt)
        0x09, 0x3E,       // Usage (Y Tilt)
        0x16, 0xD8, 0xDC, // Logical Minimum (-9000)
        0x26, 0x28, 0x23, // Logical Maximum (9000)
        0x36, 0xD8, 0xDC, // Physical Minimum (-9000)
        0x46, 0x28, 0x23, // Physical Maximum (9000)
        0x65, 0x14,       // Unit (Eng Rot: Degree)
        0x55, 0x0E,       // Unit Exponent (-2)
        0x75, 0x10,       // Report Size (16)
        0x95, 0x02,       // Report Count (2)
        0x81, 0x02,       // Input (Data,Var,Abs)
        // 4 bytes

        // 위치 X 를 정의합니다
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x30, // Usage (X)
        0x15, 0x00, // Logical Minimum (0)
        0x26, *intToLittleEndianBytes(displayMetrics.widthPixels, 2), // Logical Maximum (width)
        0x35, 0x00, // Physical Minimum (0)
        0x46, *intToLittleEndianBytes(displayMetrics.widthPixels, 2), // Physical Maximum (width)
        0x55, 0x0E, // Unit Exponent (-2)
        0x65, 0x13, // Unit (Inch,EngLinear)
        0x75, 0x10, // Report Size (16)
        0x95, 0x01, // Report Count (1)
        0x81, 0x02, // Input (Data,Var,Abs)
        // 2 bytes

        // 위치 Y 를 정의합니다
        0x05, 0x01, // Usage Page (Generic Desktop)
        0x09, 0x31, // Usage (Y)
        0x15, 0x00, // Logical Minimum (0)
        0x26, *intToLittleEndianBytes(displayMetrics.heightPixels, 2), // Logical Maximum (height)
        0x35, 0x00, // Physical Minimum (0)
        0x46, *intToLittleEndianBytes(displayMetrics.heightPixels, 2), // Physical Maximum (height)
        0x55, 0x0E, // Unit Exponent (-2)
        0x65, 0x13, // Unit (Inch,EngLinear)
        0x75, 0x10, // Report Size (16)
        0x95, 0x01, // Report Count (1)
        0x81, 0x02, // Input (Data,Var,Abs)
        // 2 bytes

        0xC0, // End Collection
        0xC0, // End Collection
    ) }

    private val hidDescriptor by lazy { intArrayOf(
        *stylusDescriptor,
        *fingerDescriptor,
    ).map { i -> i.toByte() }.toByteArray() }

    private val hidQos by lazy { BluetoothHidDeviceAppQosSettings(
        BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
        0,
        0,
        0,
        0,
        BluetoothHidDeviceAppQosSettings.MAX
    ) }

    private val hidSdp by lazy { BluetoothHidDeviceAppSdpSettings(
        "Pendroid",
        "Android stylus device",
        "Qwreey",
        BluetoothHidDevice.SUBCLASS2_DIGITIZER_TABLET,
        hidDescriptor
    ) }

    private fun emitJSEvent(id: String, data: Any?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(id, data)
    }

    private inner class HIDService : BluetoothProfile.ServiceListener, BluetoothHidDevice.Callback() {
        val executor = Executors.newSingleThreadExecutor()!!
        var registered = false // 앱의 등록됨 여부
        var hidProxy: BluetoothHidDevice? = null // HID 서비스

        // 연결되어있는 디바이스를 HID 앱에 연결하기
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun reconnectOldDevice() {
            for (oldDevice in hidProxy!!.getDevicesMatchingConnectionStates(
                intArrayOf(
                    BluetoothProfile.STATE_DISCONNECTED,
                )
            )) {
                Log.i(
                    "com.pendroid",
                    "reconnectOldDevice: connect to ${oldDevice.name}"
                )
                hidProxy!!.connect(oldDevice)
            }
        }

        // HID 장치에서 이벤트 받음
        override fun onGetReport(
            device: BluetoothDevice,
            type: Byte,
            id: Byte,
            bufferSize: Int
        ) {
            super.onGetReport(device, type, id, bufferSize)
            Log.i(
                "com.pendroid",
                "onGetReport: device=$device type=$type id=$id bufferSize=$bufferSize"
            )
        }

        // HID 장치에 이벤트 전송
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun sendReport(byteContent: ByteArray, inputId: Int) {
            if (hidProxy == null) return
            for (device in hidProxy!!.connectedDevices) {
                service.hidProxy!!.sendReport(device, inputId, byteContent)
            }
        }

        // HID 장치 연결 상태 변경 핸들
        override fun onConnectionStateChanged(
            device: BluetoothDevice,
            state: Int
        ) {
            super.onConnectionStateChanged(device, state)
            Log.i(
                "com.pendroid",
                "onConnectionStateChanged: device=$device state=$state"
            )
        }

        // 앱 상태를 다시 서비스에 register
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun registerApp() {
            hidProxy!!.unregisterApp()
            hidProxy!!.registerApp(
                hidSdp,
                null,
                hidQos,
                executor,
                this
            )
        }

        // 앱 상태 변경 핸들 (등록됨/해제됨)
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            Log.i(
                "com.pendroid",
                "onAppStatusChanged: registered=$registered"
            )
            this.registered = registered
            if (registered) {
                reconnectOldDevice()
                emitJSEvent("onAppRegistered", null)
            } else {
                emitJSEvent("onAppUnregistered", null)
            }
        }

        // 서비스 생성 핸들 => 서비스에 앱 생성
        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d("com.pendroid", "onServiceConnected: Got HID device")
                hidProxy = proxy as BluetoothHidDevice
                registerApp()
                emitJSEvent("onServiceReady", null)
            }
        }

        // 서비스 제거 핸들 => 서비스에서 앱 제거
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                Log.d("com.pendroid", "onServiceDisconnected: Lost HID device")
                hidProxy = null
            }

            emitJSEvent("onServiceDestroyed", null)
        }
    }

    // 서비스를 준비시킵니다
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @ReactMethod
    public fun initService() {
        if (service.registered) return
        if (service.hidProxy == null) {
            bluetoothAdapter.getProfileProxy(reactContext, service, BluetoothProfile.HID_DEVICE)
        } else {
            service.registerApp()
        }
    }

    // 서비스를 중지합니다
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @ReactMethod
    public fun dropService() {
        if (service.hidProxy != null) {
            service.hidProxy!!.unregisterApp()
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, service.hidProxy!!)
        }
    }

    // 장치를 연결합니다
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @ReactMethod
    public fun connectTo(address: String) {
        for (oldDevice in service.hidProxy!!.getDevicesMatchingConnectionStates(
            intArrayOf(
                BluetoothProfile.STATE_CONNECTING,
                BluetoothProfile.STATE_CONNECTED,
            )
        )) {
            service.hidProxy!!.disconnect(oldDevice)
        }

        val device = bluetoothAdapter.getRemoteDevice(address)

        if (device != null) {
            service.hidProxy!!.connect(device);
        }
    }

    // 이벤트를 전송합니다
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @ReactMethod
    public fun writeReport(hexContent: String, inputId: Int) {
        service.sendReport(
            hexContent.decodeHex().toByteArray(),
            inputId
        )
    }

    // Keep: Required for RN built in Event Emitter Calls.
    @ReactMethod
    fun addListener(type: String?) {}
    @ReactMethod
    fun removeListeners(type: Int?) {}
}
