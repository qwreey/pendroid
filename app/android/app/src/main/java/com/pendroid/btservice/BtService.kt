package com.pendroid.btservice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat.startActivityForResult
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray

class BtService(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName() = "BtService"
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    companion object {
        private const val REQUEST_ENABLE_BT = 99
    }

    // 디바이스들을 검색하기 (연결된 디바이스들)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    @ReactMethod
    public fun searchPaired(): WritableArray {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices
        val array = Arguments.createArray()
        pairedDevices?.forEach { device ->
            array.pushMap(
                Arguments.createMap().apply {
                    putString("name", device.name)
                    putString("address", device.address)
                }
            )
        }
        return array
    }

    // 블루투스를 켜도록 요청
    @ReactMethod
    public fun ensureBluetooth() {
        if (bluetoothAdapter.isEnabled) return
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(reactContext.currentActivity!!, enableBtIntent, REQUEST_ENABLE_BT, null)
    }
}
