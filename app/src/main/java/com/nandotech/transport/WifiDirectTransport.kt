package com.nandotech.transport

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import com.nandotech.data.ConnectionState
import com.nandotech.data.InputSerializer
import com.nandotech.data.InputState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WifiDirectTransport : Transport {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _logFlow = MutableStateFlow<List<String>>(emptyList())
    override val logFlow: StateFlow<List<String>> = _logFlow

    override val transportType: String = "Wi-Fi Direct (UDP Low-Latency)"

    // UDP Streaming Socket
    private var datagramSocket: DatagramSocket? = null
    private var targetInetAddress: InetAddress? = null
    private val udpPort = 5002

    // WiFi Direct P2P elements
    private var p2pManager: WifiP2pManager? = null
    private var p2pChannel: WifiP2pManager.Channel? = null
    private val _discoveredPeers = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<WifiP2pDevice>> = _discoveredPeers

    private var heartbeatJob: Job? = null

    override fun addLog(message: String) {
        Log.d("WifiDirectTransport", message)
        _logFlow.update { (listOf("[WiFi] $message") + it).take(50) }
    }

    /**
     * Initializes the P2P components if available
     */
    fun checkAndInitializeP2p(manager: WifiP2pManager?, channel: WifiP2pManager.Channel?) {
        this.p2pManager = manager
        this.p2pChannel = channel
        addLog("WiFi Direct services prepared.")
    }

    fun updatePeersList(peers: List<WifiP2pDevice>) {
        _discoveredPeers.value = peers
        addLog("Discovered ${peers.size} peer(s) over Wi-Fi Direct.")
    }

    override fun connect(address: String?) {
        if (_connectionState.value == ConnectionState.CONNECTED || _connectionState.value == ConnectionState.CONNECTING) {
            addLog("Already active or establishing connection.")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        val targetIp = address ?: "192.168.49.1" // standard Android P2P group owner IP
        addLog("Connecting low-latency UDP stream to PC bridge at IP: $targetIp...")

        scope.launch {
            try {
                datagramSocket?.close()
                datagramSocket = DatagramSocket().apply {
                    sendBufferSize = 1024
                }
                
                targetInetAddress = InetAddress.getByName(targetIp)
                _connectionState.value = ConnectionState.CONNECTED
                addLog("UDP Socket opened. Low-latency input pipeline ready on port $udpPort!")
                
                startHeartbeat()
            } catch (e: Exception) {
                addLog("Failed to initiate socket target: ${e.message}")
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }

    /**
     * Set target address directly when Wi-Fi Direct connection negotiation completes.
     */
    fun setupOnConnectionSuccess(p2pInfo: WifiP2pInfo) {
        addLog("Wi-Fi Direct Negotiated. Group Formed: ${p2pInfo.groupFormed}, Owner: ${p2pInfo.isGroupOwner}")
        if (p2pInfo.groupFormed) {
            val ownerIp = p2pInfo.groupOwnerAddress?.hostAddress ?: "192.168.49.1"
            addLog("P2P Owner IP Resolved: $ownerIp")
            connect(ownerIp)
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(3000)
                sendHeartbeat()
            }
        }
    }

    override fun disconnect() {
        heartbeatJob?.cancel()
        try {
            datagramSocket?.close()
        } catch (e: Exception) { /* ignore */ }
        datagramSocket = null
        targetInetAddress = null

        if (_connectionState.value != ConnectionState.ERROR) {
            _connectionState.value = ConnectionState.DISCONNECTED
        }
        addLog("UDP connection released.")
    }

    override fun sendInput(state: InputState): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        val socket = datagramSocket ?: return false
        val targetAddress = targetInetAddress ?: return false

        return try {
            val binaryData = InputSerializer.serialize(state)
            val packet = DatagramPacket(binaryData, binaryData.size, targetAddress, udpPort)
            socket.send(packet)
            true
        } catch (e: IOException) {
            addLog("UDP transmit error: ${e.localizedMessage}")
            _connectionState.value = ConnectionState.ERROR
            disconnect()
            false
        }
    }

    override fun sendHeartbeat(): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        val socket = datagramSocket ?: return false
        val targetAddress = targetInetAddress ?: return false

        return try {
            // UDP keep-alive/heartbeat
            val ping = byteArrayOf(0, 0, 0, 0)
            val packet = DatagramPacket(ping, ping.size, targetAddress, udpPort)
            socket.send(packet)
            true
        } catch (e: Exception) {
            false
        }
    }
}
