package com.nandotech.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nandotech.data.ConnectionState
import com.nandotech.data.InputState
import com.nandotech.sensor.SensorReader
import com.nandotech.transport.Transport
import com.nandotech.transport.UsbTransport
import com.nandotech.transport.TcpWifiTransport
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.absoluteValue

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    // Transports
    val usbTransport = UsbTransport()
    val wifiTransport = TcpWifiTransport()

    // Configuration states
    private val _selectedTransportIndex = MutableStateFlow(0) // 0 = USB, 1 = Wi-Fi
    val selectedTransportIndex: StateFlow<Int> = _selectedTransportIndex

    // Reactive bindings
    val activeTransport: StateFlow<Transport> = _selectedTransportIndex
        .map { if (it == 0) usbTransport else wifiTransport }
        .stateIn(viewModelScope, SharingStarted.Eagerly, usbTransport)

    val connectionState: StateFlow<ConnectionState> = activeTransport
        .flatMapLatest { it.connectionState }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ConnectionState.DISCONNECTED)

    val logLines: StateFlow<List<String>> = activeTransport
        .flatMapLatest { it.logFlow }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Sensors
    val sensorReader = SensorReader(application)

    // Controller specific settings (state-backed)
    val analogDeadzone = MutableStateFlow(0.08f) // 8% Default Analog drift deadzone
    val gyroActive = MutableStateFlow(true)
    val targetIpAddress = MutableStateFlow("192.168.49.1") // Default WiFi Direct Owner IP

    // Internal state cache for assembling rapid UDP reports
    @Volatile
    private var cachedInput = InputState()
    
    // Live UI bindable state flow
    private val _uiInputState = MutableStateFlow(InputState())
    val uiInputState: StateFlow<InputState> = _uiInputState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        // Start high-performance streaming tick loop when connected
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state == ConnectionState.CONNECTED) {
                    startStreamingLoop()
                    if (gyroActive.value) {
                        sensorReader.startListening()
                    }
                } else {
                    stopStreamingLoop()
                    sensorReader.stopListening()
                }
            }
        }

        // Keep sensor calibration sync'd with user parameters
        viewModelScope.launch {
            gyroActive.collect { active ->
                if (active && connectionState.value == ConnectionState.CONNECTED) {
                    sensorReader.startListening()
                } else {
                    sensorReader.stopListening()
                }
            }
        }
    }

    fun selectTransport(index: Int) {
        if (connectionState.value == ConnectionState.CONNECTED || connectionState.value == ConnectionState.CONNECTING) {
            activeTransport.value.disconnect()
        }
        _selectedTransportIndex.value = index
    }

    fun connect() {
        val transport = activeTransport.value
        if (transport is TcpWifiTransport) {
            transport.connect(targetIpAddress.value)
        } else {
            transport.connect()
        }
    }

    fun disconnect() {
        activeTransport.value.disconnect()
    }

    fun calibrateSensors() {
        sensorReader.calibrate()
        activeTransport.value.addLog("Gyro and Accelerometer calibrated to neutral orientation.")
    }

    fun resetSensorCalibration() {
        sensorReader.resetCalibration()
        activeTransport.value.addLog("Sensor offsets cleared to default values.")
    }

    // High performance 60Hz gamepad telemetry emitter
    private fun startStreamingLoop() {
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                // Read and inject gyroscope readings safely
                var updated = cachedInput
                if (gyroActive.value) {
                    updated = updated.copy(
                        gyroX = sensorReader.currentGyroX,
                        gyroY = sensorReader.currentGyroY,
                        gyroZ = sensorReader.currentGyroZ,
                        accelX = sensorReader.currentAccelX,
                        accelY = sensorReader.currentAccelY,
                        accelZ = sensorReader.currentAccelZ
                    )
                }
                
                // Keep UI updated periodically
                _uiInputState.value = updated
                
                // Transmit binary payload to the active pipeline
                activeTransport.value.sendInput(updated)
                
                delay(16) // ~60Hz transmission rate
            }
        }
    }

    private fun stopStreamingLoop() {
        streamingJob?.cancel()
        streamingJob = null
    }

    // Tactile interface triggers for synchronous immediate updates
    fun updateButtons(buttonGroupMask: Int, isPressed: Boolean) {
        synchronized(this) {
            cachedInput = cachedInput.withButton(buttonGroupMask, isPressed)
        }
        _uiInputState.value = cachedInput
        // Fire urgent event for negligible click lag
        viewModelScope.launch(Dispatchers.Default) {
            activeTransport.value.sendInput(cachedInput)
        }
    }

    fun updateLeftAnalog(x: Float, y: Float) {
        val dead = analogDeadzone.value
        val normalizedX = applyDeadzone(x, dead)
        val normalizedY = applyDeadzone(y, dead)
        
        synchronized(this) {
            cachedInput = cachedInput.copy(
                leftStickX = normalizedX,
                leftStickY = normalizedY,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateRightAnalog(x: Float, y: Float) {
        val dead = analogDeadzone.value
        val normalizedX = applyDeadzone(x, dead)
        val normalizedY = applyDeadzone(y, dead)
        
        synchronized(this) {
            cachedInput = cachedInput.copy(
                rightStickX = normalizedX,
                rightStickY = normalizedY,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateTriggers(left: Float, right: Float) {
        synchronized(this) {
            cachedInput = cachedInput.copy(
                leftTrigger = left,
                rightTrigger = right,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun updateTouchpad(x: Float, y: Float, state: Int) {
        synchronized(this) {
            cachedInput = cachedInput.copy(
                touchpadX = x,
                touchpadY = y,
                touchpadState = state,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun applyDeadzone(value: Float, deadzone: Float): Float {
        return if (value.absoluteValue < deadzone) {
            0f
        } else {
            // Re-scale the analog output from deadzone edge to 1.0f for linear responsiveness
            val sign = if (value < 0) -1f else 1f
            val proportion = (value.absoluteValue - deadzone) / (1f - deadzone)
            (proportion * sign).coerceIn(-1f, 1f)
        }
    }

    override fun onCleared() {
        super.onCleared()
        usbTransport.disconnect()
        wifiTransport.disconnect()
        sensorReader.stopListening()
    }
}
