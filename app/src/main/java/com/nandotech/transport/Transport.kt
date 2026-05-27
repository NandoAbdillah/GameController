package com.nandotech.transport

import com.nandotech.data.ConnectionState
import com.nandotech.data.InputState
import kotlinx.coroutines.flow.StateFlow

interface Transport {
    val connectionState: StateFlow<ConnectionState>
    val transportType: String // e.g., "USB" or "Wi-Fi Direct"
    
    fun connect(address: String? = null)
    fun disconnect()
    fun sendInput(state: InputState): Boolean
    fun sendHeartbeat(): Boolean
    val logFlow: StateFlow<List<String>> // Flow of log lines for debugging
    fun addLog(message: String)
}
