package com.nandotech.data

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

object InputSerializer {
    const val PACKET_SIZE = 64
    const val PROTOCOL_VERSION: Byte = 1

    const val TYPE_HANDSHAKE: Byte     = 0x01
    const val TYPE_HANDSHAKE_ACK: Byte = 0x02
    const val TYPE_INPUT_STATE: Byte   = 0x10
    const val TYPE_HEARTBEAT: Byte     = 0x20
    const val TYPE_DISCONNECT: Byte    = 0xFF.toByte()

    // Float -1.0..1.0 -> Byte -128..127
    private fun floatToStickByte(v: Float): Byte {
        val clamped = v.coerceIn(-1f, 1f)
        return if (clamped >= 0f) (clamped * 127f).roundToInt().toByte()
        else (clamped * 128f).roundToInt().toByte()
    }

    // Float 0.0..1.0 -> Byte 0..255 (stored as signed, .NET baca sebagai unsigned)
    private fun floatToTriggerByte(v: Float): Byte {
        return (v.coerceIn(0f, 1f) * 255f).roundToInt().and(0xFF).toByte()
    }

    // Float gyro/accel -> Short (millideg/s atau milli-g)
    private fun floatToShort(v: Float, scale: Float = 1000f): Short {
        return (v * scale).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    private fun putFixedString(buf: ByteBuffer, s: String, maxLen: Int) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        val copy = minOf(bytes.size, maxLen - 1)
        buf.put(bytes, 0, copy)
        repeat(maxLen - copy) { buf.put(0) }
    }

    fun buildHandshake(deviceId: String, token: String = ""): ByteArray {
        val buf = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TYPE_HANDSHAKE)                         // [0]  type
        buf.put(PROTOCOL_VERSION)                       // [1]  version
        buf.putShort(0)                                 // [2]  sequence
        buf.putLong(System.currentTimeMillis())         // [4]  timestamp
        putFixedString(buf, deviceId, 16)               // [12] deviceId
        buf.putInt(0)                                   // [28] buttons
        repeat(6) { buf.put(0) }                        // [32] sticks+triggers
        repeat(6) { buf.putShort(0) }                   // [38] gyro+accel (6x short = 12 bytes)
        buf.put(0)                                      // [50] touchFlags
        buf.put(0)                                      // [51] heartbeatFlag
        putFixedString(buf, token, 12)                  // [52] pairingToken
        return buf.array()
    }

    fun serialize(state: InputState): ByteArray {
        val buf = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)

        buf.put(TYPE_INPUT_STATE)                              // [0]
        buf.put(PROTOCOL_VERSION)                              // [1]
        buf.putShort(state.sequenceNumber.toShort())           // [2]
        buf.putLong(System.currentTimeMillis())                // [4]
        putFixedString(buf, state.deviceId, 16)                // [12]
        buf.putInt(state.buttons)                              // [28]
        buf.put(floatToStickByte(state.leftStickX))            // [32]
        buf.put(floatToStickByte(state.leftStickY))            // [33]
        buf.put(floatToStickByte(state.rightStickX))           // [34]
        buf.put(floatToStickByte(state.rightStickY))           // [35]
        buf.put(floatToTriggerByte(state.leftTrigger))         // [36]
        buf.put(floatToTriggerByte(state.rightTrigger))        // [37]
        buf.putShort(floatToShort(state.gyroX))                // [38]
        buf.putShort(floatToShort(state.gyroY))                // [40]
        buf.putShort(floatToShort(state.gyroZ))                // [42]
        buf.putShort(floatToShort(state.accelX))               // [44]
        buf.putShort(floatToShort(state.accelY))               // [46]
        buf.putShort(floatToShort(state.accelZ))               // [48]

        // touchFlags: encode touchpadState ke bit 0, ada touch = bit set
        val touchFlags = if (state.touchpadState > 0) 0x01.toByte() else 0x00.toByte()
        buf.put(touchFlags)                                    // [50]
        buf.put(if (state.isHeartbeat) 1 else 0)               // [51]
        putFixedString(buf, state.pairingToken, 12)            // [52]

        return buf.array()
    }

    fun buildHeartbeat(deviceId: String): ByteArray {
        val buf = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TYPE_HEARTBEAT)
        buf.put(PROTOCOL_VERSION)
        buf.putShort(0)
        buf.putLong(System.currentTimeMillis())
        putFixedString(buf, deviceId, 16)
        // pad fields nol
        repeat(22) { buf.put(0) }   // buttons(4) + sticks(6) + gyro(6) + accel(6) = 22 bytes
        buf.put(1)                  // [51] heartbeatFlag
        putFixedString(buf, "", 12)
        return buf.array()
    }

    fun buildDisconnect(deviceId: String): ByteArray {
        val buf = ByteBuffer.allocate(PACKET_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(TYPE_DISCONNECT)
        buf.put(PROTOCOL_VERSION)
        buf.putShort(0)
        buf.putLong(System.currentTimeMillis())
        putFixedString(buf, deviceId, 16)
        repeat(35) { buf.put(0) }
        return buf.array()
    }
}