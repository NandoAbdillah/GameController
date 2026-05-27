package com.nandotech.data

data class InputState(
    val deviceId: String = "",
    val sequenceNumber: Int = 0,

    // Sticks: Float -1.0..1.0 (cocok dengan ControllerScreen & ViewModel)
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,

    // Triggers: Float 0.0..1.0
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,

    // Gyro/Accel: Float (rad/s dan m/s²) dari SensorReader
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,

    // Buttons bitmask
    val buttons: Int = 0,

    // Touch
    val touchpadX: Float = 0f,
    val touchpadY: Float = 0f,
    val touchpadState: Int = 0,  // 0=idle, 1=touch, 2=drag

    val isHeartbeat: Boolean = false,
    val pairingToken: String = "",
    val timestamp: Long = 0L,
) {
    // Helper: set/clear button bit
    fun withButton(mask: Int, pressed: Boolean): InputState {
        val newButtons = if (pressed) buttons or mask else buttons and mask.inv()
        return copy(buttons = newButtons)
    }

    // Button constants — identik dengan C# ButtonFlags enum
    companion object {
        const val BTN_A          = 1 shl 0
        const val BTN_B          = 1 shl 1
        const val BTN_X          = 1 shl 2
        const val BTN_Y          = 1 shl 3
        const val BTN_L1         = 1 shl 4   // LB
        const val BTN_R1         = 1 shl 5   // RB
        const val BTN_START      = 1 shl 6
        const val BTN_SELECT     = 1 shl 7   // Back
        const val BTN_L_STICK    = 1 shl 8
        const val BTN_R_STICK    = 1 shl 9
        const val BTN_DPAD_UP    = 1 shl 10
        const val BTN_DPAD_DOWN  = 1 shl 11
        const val BTN_DPAD_LEFT  = 1 shl 12
        const val BTN_DPAD_RIGHT = 1 shl 13
        const val BTN_GUIDE      = 1 shl 14
    }
}