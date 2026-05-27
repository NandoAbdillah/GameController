package com.nandotech.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class SensorReader(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // Available sensors
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Configurable parameters
    var sensitivity = 1.5f
    var smoothing = 0.3f // 0 = raw, 1 = max damped/smoothed
    var isFilteringEnabled = true

    // Current calibrated offsets
    private var gyroOffsetX = 0f
    private var gyroOffsetY = 0f
    private var gyroOffsetZ = 0f

    private var accelOffsetX = 0f
    private var accelOffsetY = 0f
    private var accelOffsetZ = 0f

    // Running filtered values
    var currentGyroX = 0f
    var currentGyroY = 0f
    var currentGyroZ = 0f

    var currentAccelX = 0f
    var currentAccelY = 0f
    var currentAccelZ = 0f

    // Track active registration state
    val isGyroAvailable = gyroSensor != null
    val isAccelAvailable = accelSensor != null
    var isListening = false
        private set

    fun startListening() {
        if (isListening) return
        isListening = true
        gyroSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        accelSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        sensorManager.unregisterListener(this)
    }

    /**
     * Snapshots the immediate current readings as the flat absolute neutral baseline offsets.
     */
    fun calibrate() {
        // We capture current raw state (before offsets) as the neutral bias
        gyroOffsetX += currentGyroX / sensitivity
        gyroOffsetY += currentGyroY / sensitivity
        gyroOffsetZ += currentGyroZ / sensitivity

        accelOffsetX += currentAccelX
        accelOffsetY += currentAccelY
        accelOffsetZ += (currentAccelZ - 9.81f) // Accounts for gravity on Z-axis when lying flat
    }

    fun resetCalibration() {
        gyroOffsetX = 0f
        gyroOffsetY = 0f
        gyroOffsetZ = 0f
        accelOffsetX = 0f
        accelOffsetY = 0f
        accelOffsetZ = 0f
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                val rawX = event.values[0]
                val rawY = event.values[1]
                val rawZ = event.values[2]

                // Subtract calibrated offset
                val calibratedX = (rawX - gyroOffsetX) * sensitivity
                val calibratedY = (rawY - gyroOffsetY) * sensitivity
                val calibratedZ = (rawZ - gyroOffsetZ) * sensitivity

                if (isFilteringEnabled) {
                    // Low-Pass Filter: smoothed = alpha * previous + (1 - alpha) * current
                    // When smoothing = 0 -> alpha = 0 (fully raw). When smoothing = 1 -> alpha = 0.95 (max smoothed)
                    val alpha = smoothing * 0.95f
                    currentGyroX = alpha * currentGyroX + (1f - alpha) * calibratedX
                    currentGyroY = alpha * currentGyroY + (1f - alpha) * calibratedY
                    currentGyroZ = alpha * currentGyroZ + (1f - alpha) * calibratedZ
                } else {
                    currentGyroX = calibratedX
                    currentGyroY = calibratedY
                    currentGyroZ = calibratedZ
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val rawX = event.values[0]
                val rawY = event.values[1]
                val rawZ = event.values[2]

                val calibratedX = rawX - accelOffsetX
                val calibratedY = rawY - accelOffsetY
                val calibratedZ = rawZ - accelOffsetZ

                if (isFilteringEnabled) {
                    val alpha = smoothing * 0.95f
                    currentAccelX = alpha * currentAccelX + (1f - alpha) * calibratedX
                    currentAccelY = alpha * currentAccelY + (1f - alpha) * calibratedY
                    currentAccelZ = alpha * currentAccelZ + (1f - alpha) * calibratedZ
                } else {
                    currentAccelX = calibratedX
                    currentAccelY = calibratedY
                    currentAccelZ = calibratedZ
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No action required
    }
}
