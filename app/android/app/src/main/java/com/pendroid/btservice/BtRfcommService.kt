package com.pendroid.btservice

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import okio.ByteString.Companion.decodeHex
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID


class BtRfcommService(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName() = "BtRfcommService"
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var connectingThread: ConnectingThread? = null
    private var connectedThread: ConnectedThread? = null

    companion object {
        private val STATE_CONNECTING = 2 // now initiating an outgoing connection
        private val STATE_CONNECTED = 3 // now connected to a remote device
        private val STATE_FAILED = 4
        private val STATE_DATA = 5
        private val STATE_READY = 6
        private val STATE_DISCONNECTED = 7
        private val SERVICE_UUID_SECURE = UUID.fromString("09c8e685-c2ca-49f3-98d5-2bacd8670a16")
        private val SERVICE_UUID_INSECURE = UUID.fromString("09c8e685-c2ca-49f3-98d5-2bacd8670a16")
    }

    private fun sendEvent(data: Any) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit("onStateChanged", data)
    }

    // 모든 연결 종료
    private fun cancelAll() {
        if (connectingThread != null) {
            connectingThread!!.cancel()
            connectingThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    @ReactMethod
    public fun connectTo(address: String) {
        // Cancel any connection threads
        cancelAll()

        // Start the thread to connect with the given device
        val device = bluetoothAdapter.getRemoteDevice(address)
        connectingThread = ConnectingThread(device, true)
        connectingThread!!.start()

        sendEvent(STATE_CONNECTING)
    }

    @ReactMethod
    public fun write(content: String) {
        connectedThread!!.write(content.decodeHex().toByteArray())
    }

    @Synchronized
    private fun onConnected(socket: BluetoothSocket, device: BluetoothDevice, socketType: String) {
        // Cancel any connection threads
        cancelAll()

        // Create connected thread
        connectedThread = ConnectedThread(socket, socketType)
        connectedThread!!.start()
        sendEvent(STATE_CONNECTED)
    }

    @Synchronized
    private fun onReady() {
        sendEvent(STATE_READY)
    }

    @Synchronized
    private fun onConnectionFailed() {
        sendEvent(STATE_FAILED)
    }

    @Synchronized
    private fun onDisconnected() {
        sendEvent(STATE_DISCONNECTED)
    }

    @Synchronized
    private fun onData(buffer: ByteArray, bytes: Int) {
        sendEvent(STATE_DATA)
    }

    private inner class ConnectedThread(private val socket: BluetoothSocket, private val socketType: String) : Thread() {
        private var input: InputStream? = null
        private var output: OutputStream? = null

        override fun run() {
            // Get the BluetoothSocket input and output streams
            Log.d("com.pendroid", "ConnectedThread started, $socketType")
            try {
                input = socket.inputStream
                output = socket.outputStream
            } catch (e: IOException) {
                Log.e("com.pendroid", "temp sockets not created", e)
            }

            onReady()

            val buffer = ByteArray(1024)
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (socket.isConnected) {
                try {
                    // Read from the InputStream
                    bytes = input!!.read(buffer)
                    onData(buffer, bytes)
                } catch (e: IOException) {
                    onDisconnected()
                    break
                }
            }
        }

        fun write(buffer: ByteArray) {
            try {
                output!!.write(buffer)
                output!!.flush()
            } catch (e: Exception) {
                Log.e("com.pendroid", "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                socket.close()
            } catch (e: IOException) {
                Log.e("com.pendroid", "close() of connect socket failed", e)
            }
        }
    }

    private inner class ConnectingThread(private val device: BluetoothDevice, private val secure: Boolean) : Thread() {
        private var socket: BluetoothSocket? = null
        private lateinit var socketType: String

        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
        private fun createSocket() {
            try {
                socket = if (secure) {
                    device.createRfcommSocketToServiceRecord(
                        SERVICE_UUID_SECURE
                    )
                } else {
                    device.createInsecureRfcommSocketToServiceRecord(
                        SERVICE_UUID_INSECURE
                    )
                }
            } catch (e: IOException) {
                Log.e("com.pendroid", "Socket Type: $socketType create() failed", e)
            }
        }

        @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT])
        override fun run() {
            socketType = if (secure) "Secure" else "Insecure"
            Log.i("com.pendroid", "ConnectThread started. SocketType:$socketType")
            name = "ConnectThread$socketType"

            // Create bluetooth socket
            createSocket()
            if (socket == null) {
                onConnectionFailed()
                Log.i("com.pendroid", "Socket creation failed")
                return
            }

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                socket!!.connect()
            } catch (e: Exception) {
                Log.i("com.pendroid", "Socket creation failed $e")
                // Close the socket
                try {
                    socket!!.close()
                } catch (e2: IOException) {
                    Log.e("com.pendroid", "unable to close() $socketType socket during connection failure $e2")
                }
                onConnectionFailed()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this) {
                connectingThread = null
            }

            // Start the connected thread
            onConnected(socket!!, device, socketType)
        }

        fun cancel() {
            try {
                socket!!.close()
            } catch (e: IOException) {
                Log.e("com.pendroid", "close() of connect $socketType socket failed", e)
            }
        }
    }

    // Keep: Required for RN built in Event Emitter Calls.
    @ReactMethod
    fun addListener(type: String?) {}
    @ReactMethod
    fun removeListeners(type: Int?) {}
}
