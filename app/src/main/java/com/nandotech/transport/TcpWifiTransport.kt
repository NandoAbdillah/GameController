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
import java.net.InetSocketAddress
import java.net.Socket

class TcpWifiTransport : Transport {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _logFlow = MutableStateFlow<List<String>>(emptyList())
    override val logFlow: StateFlow<List<String>> = _logFlow

    override val transportType: String = "Wi-Fi Hotspot (TCP Stable)"

    // TCP Socket Elements
    private var tcpSocket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val tcpPort = 7890

    // Njukut ID HP gae salam kenal (podo karo USB)
    private val deviceId = android.os.Build.MODEL.take(15)

    private var heartbeatJob: Job? = null

    override fun addLog(message: String) {
        Log.d("TcpWifiTransport", message)
        _logFlow.update { (listOf("[TCP Wi-Fi] $message") + it).take(50) }
    }

    override fun connect(address: String?) {
        if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        val targetIp = address ?: "192.168.137.1"
        addLog("Mencoba nyambung nang PC Bridge: $targetIp:$tcpPort...")

        scope.launch {
            try {
                closeConnections()

                val socket = Socket().apply {
                    connect(InetSocketAddress(targetIp, tcpPort), 5000)
                    tcpNoDelay = true
                }

                // Masukno proses Handshake persis kaya USB
                if (setupAndHandshake(socket)) {
                    startHeartbeat()
                }

            } catch (e: Exception) {
                if (isActive) {
                    addLog("Gagal nyambung nang PC: ${e.localizedMessage}")
                    _connectionState.value = ConnectionState.ERROR
                    closeConnections()
                }
            }
        }
    }

    // Fungsi Handshake dicolong teko UsbTransport
    private fun setupAndHandshake(socket: Socket): Boolean {
        return try {
            tcpSocket = socket
            outputStream = socket.getOutputStream()
            inputStream  = socket.getInputStream()

            // Kirim Handshake (64 byte)
            val hs = InputSerializer.buildHandshake(deviceId)
            outputStream!!.write(hs)
            outputStream!!.flush()
            addLog("Handshake terkirim, menunggu ACK...")

            // Baca HandshakeAck (64 byte) dari PC
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
            addLog("Handshake OK! Siap kirim input nang PC.")
            true
        } catch (e: Exception) {
            addLog("Handshake gagal: ${e.message}")
            closeConnections()
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
                    _connectionState.value = ConnectionState.ERROR
                    closeConnections()
                    break
                }
            }
        }
    }

    private fun closeConnections() {
        try { outputStream?.close() } catch (e: Exception) {}
        try { inputStream?.close() } catch (e: Exception) {}
        try { tcpSocket?.close() } catch (e: Exception) {}
        outputStream = null
        inputStream = null
        tcpSocket = null
    }

    override fun disconnect() {
        heartbeatJob?.cancel()
        scope.launch {
            // Kirim packet pamitan lek sik nyambung
            if (_connectionState.value == ConnectionState.CONNECTED) {
                try {
                    outputStream?.write(InputSerializer.buildDisconnect(deviceId))
                    outputStream?.flush()
                } catch (_: Exception) {}
            }
            closeConnections()
            if (_connectionState.value != ConnectionState.ERROR) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
            addLog("Koneksi TCP diputus.")
        }
    }

    override fun sendInput(state: InputState): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        val stream = outputStream ?: return false

        return try {
            val binaryData = InputSerializer.serialize(state)
            stream.write(binaryData)
            stream.flush()
            true
        } catch (e: Exception) {
            addLog("Gagal ngirim data (TCP Error): ${e.localizedMessage}")
            _connectionState.value = ConnectionState.ERROR
            closeConnections()
            false
        }
    }

    override fun sendHeartbeat(): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        val stream = outputStream ?: return false

        return try {
            // Heartbeat kudu 64 byte nggae fungsi buildHeartbeat (Dudu 4 byte maneh!)
            val hb = InputSerializer.buildHeartbeat(deviceId)
            stream.write(hb)
            stream.flush()
            true
        } catch (e: Exception) {
            false
        }
    }
}