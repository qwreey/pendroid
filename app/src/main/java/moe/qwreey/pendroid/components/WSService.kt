package moe.qwreey.pendroid.components

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress

class WSService(val socketPort: Int, var openHandle: (WebSocket?) -> Unit = {}): WebSocketServer(InetSocketAddress(socketPort)) {
    var hasConnection: Boolean = false
        private set

    override fun onOpen(
        conn: WebSocket?,
        handshake: ClientHandshake?
    ) {
        hasConnection = true
        openHandle(conn)
    }

    override fun onClose(
        conn: WebSocket?,
        code: Int,
        reason: String?,
        remote: Boolean
    ) {
        hasConnection = connections.size != 0
    }

    override fun onMessage(conn: WebSocket?, message: String?) {
    }

    override fun onError(conn: WebSocket?, ex: Exception?) {
    }

    override fun onStart() {
    }
}
