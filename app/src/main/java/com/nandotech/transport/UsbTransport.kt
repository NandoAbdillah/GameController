package com.nandotech.transport

import android.util.Log
import com.nandotech.data.ConnectionState
import com.nandotech.data.InputSerializer
import com.nandotech.data.InputState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class UsbTransport : Transport {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _logFlow = MutableStateFlow<List<String>>(emptyList())
    override val logFlow: StateFlow<List<String>> = _logFlow

    override val transportType: String = "USB (ADB Port Forward)"

    // Harus sama dengan TcpReceiver.DefaultPort di .NET
    private val localPort = 7890

    private val deviceId = android.os.Build.MODEL.take(15)

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null

    override fun addLog(message: String) {
        Log.d("UsbTransport", message)
        _logFlow.update { (listOf("[USB] $message") + it).take(50) }
    }

    override fun connect(address: String?) {
        if (_connectionState.value == ConnectionState.CONNECTED ||
            _connectionState.value == ConnectionState.CONNECTING) return

        _connectionState.value = ConnectionState.CONNECTING
        addLog("Connecting ke localhost:$localPort via ADB reverse...")

        connectionJob?.cancel()
        connectionJob = scope.launch {
            while (isActive) {
                try {
                    val socket = Socket("127.0.0.1", localPort)
                    addLog("Socket terhubung ke PC")
                    if (setupAndHandshake(socket)) {
                        startHeartbeat()
                        while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                            delay(500)
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        addLog("Gagal connect: ${e.message}, retry 3s...")
                        _connectionState.value = ConnectionState.CONNECTING
                        delay(3000)
                    }
                }
            }
        }
    }

    private fun setupAndHandshake(socket: Socket): Boolean {
        return try {
            socket.tcpNoDelay = true
            clientSocket = socket
            outputStream = socket.getOutputStream()
            inputStream  = socket.getInputStream()

            // Kirim Handshake (64 byte)
            val hs = InputSerializer.buildHandshake(deviceId)
            outputStream!!.write(hs)
            outputStream!!.flush()
            addLog("Handshake terkirim, menunggu ACK...")

            // Baca HandshakeAck (64 byte) dari .NET
            val ack = ByteArray(InputSerializer.PACKET_SIZE)
            var totalRead = 0
            while (totalRead < InputSerializer.PACKET_SIZE) {
                val n = inputStream!!.read(ack, totalRead, InputSerializer.PACKET_SIZE - totalRead)
                if (n <= 0) {
                    addLog("Koneksi putus saat baca ACK")
                    return false
                }
                totalRead += n
            }

            // Cek type byte [0] == HandshakeAck (0x02)
            if (ack[0] != InputSerializer.TYPE_HANDSHAKE_ACK) {
                addLog("ACK tidak valid: ${ack[0]}")
                return false
            }

            _connectionState.value = ConnectionState.CONNECTED
            addLog("Handshake OK! Siap kirim input.")
            true
        } catch (e: Exception) {
            addLog("Handshake gagal: ${e.message}")
            cleanupSocket()
            false
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(3000)
                if (!sendHeartbeat()) {
                    addLog("Heartbeat gagal, koneksi terputus.")
                    _connectionState.value = ConnectionState.CONNECTING
                    cleanupSocket()
                    break
                }
            }
        }
    }

    override fun disconnect() {
        heartbeatJob?.cancel()
        connectionJob?.cancel()
        // Kirim disconnect packet kalau masih connect
        if (_connectionState.value == ConnectionState.CONNECTED) {
            try {
                outputStream?.write(InputSerializer.buildDisconnect(deviceId))
                outputStream?.flush()
            } catch (_: Exception) {}
        }
        cleanupSocket()
        _connectionState.value = ConnectionState.DISCONNECTED
        addLog("Disconnected.")
    }

    private fun cleanupSocket() {
        try { outputStream?.close() } catch (_: Exception) {}
        try { inputStream?.close()  } catch (_: Exception) {}
        try { clientSocket?.close() } catch (_: Exception) {}
        try { serverSocket?.close() } catch (_: Exception) {}
        outputStream = null
        inputStream  = null
        clientSocket = null
        serverSocket = null
    }

    override fun sendInput(state: InputState): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        return try {
            val data = InputSerializer.serialize(state)
            outputStream?.write(data)
            outputStream?.flush()
            true
        } catch (e: Exception) {
            addLog("Send error: ${e.message}")
            _connectionState.value = ConnectionState.CONNECTING
            cleanupSocket()
            false
        }
    }

    override fun sendHeartbeat(): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        return try {
            // Heartbeat harus 64 byte — bukan 4 byte!
            val hb = InputSerializer.buildHeartbeat(deviceId)
            outputStream?.write(hb)
            outputStream?.flush()
            true
        } catch (_: Exception) { false }
    }
}