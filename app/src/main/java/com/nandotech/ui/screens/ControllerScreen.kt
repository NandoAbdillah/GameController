package com.nandotech.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandotech.data.ConnectionState
import com.nandotech.data.InputState
import com.nandotech.ui.theme.*
import com.nandotech.viewmodel.ConnectionViewModel
import kotlin.math.sqrt

@Composable
fun ControllerScreen(
    viewModel: ConnectionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inputState by viewModel.uiInputState.collectAsState()
    val connState by viewModel.connectionState.collectAsState()
    val gyroActive by viewModel.gyroActive.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // TOP BARS AND TRIGGERS
        Column(modifier = Modifier.fillMaxSize()) {
            
            // L2/R2 Analog triggers and L1/R1 Digital buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE BUMPERS & TRIGGERS
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // L2 Trigger
                    TriggerGaugeButton(
                        label = "L2 TRIGGER",
                        isActive = inputState.leftTrigger > 0.05f,
                        analogValue = inputState.leftTrigger,
                        onPressed = { val trig = if (it) 1f else 0f; viewModel.updateTriggers(trig, inputState.rightTrigger) }
                    )
                    
                    // L1 Bumper
                    GamepadTouchButton(
                        label = "L1",
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 8.dp),
                        modifier = Modifier.width(75.dp).height(42.dp),
                        activeColor = CyberPurple,
                        onPressed = { viewModel.updateButtons(InputState.BTN_L1, it) }
                    )
                }

                // CENTER STATS
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back command button
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.dp, CyberCyan.copy(0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reconnect Screen",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Gyro Calibrate shortcut
                    IconButton(
                        onClick = { viewModel.calibrateSensors() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.dp, CyberPurple.copy(0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Calibrate sensor",
                            tint = CyberPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Connection Status Dot
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (connState == ConnectionState.CONNECTED) NeonGreen.copy(0.15f) else AlertRed.copy(0.15f))
                            .border(1.dp, if (connState == ConnectionState.CONNECTED) NeonGreen else AlertRed, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (connState == ConnectionState.CONNECTED) "LINKED 60HZ" else "DISCONNECTED",
                            color = if (connState == ConnectionState.CONNECTED) NeonGreen else AlertRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // RIGHT SIDE BUMPERS & TRIGGERS
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // R1 Bumper
                    GamepadTouchButton(
                        label = "R1",
                        shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 8.dp),
                        modifier = Modifier.width(75.dp).height(42.dp),
                        activeColor = CyberPurple,
                        onPressed = { viewModel.updateButtons(InputState.BTN_R1, it) }
                    )

                    // R2 Trigger
                    TriggerGaugeButton(
                        label = "R2 TRIGGER",
                        isActive = inputState.rightTrigger > 0.05f,
                        analogValue = inputState.rightTrigger,
                        onPressed = { val trig = if (it) 1f else 0f; viewModel.updateTriggers(inputState.leftTrigger, trig) }
                    )
                }
            }

            // CORE DUAL SIDE LAYOUT (Split Left / Center / Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // =============== LEFT CONTROLLER PLATE (D-PAD & L-STICK) ===============
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Digital Cross D-Pad
                        VirtualDpad(onPress = { buttonMask, pressed ->
                            viewModel.updateButtons(buttonMask, pressed)
                        })

                        // Left Joystick Stick
                        VirtualAnalogStick(
                            tagLabel = "L-STICK",
                            onValueChange = { x, y -> viewModel.updateLeftAnalog(x, y) }
                        )
                    }
                }

                // =============== CENTER UTILITY (START / SELECT / TOUCHPAD) ===============
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Select & Start row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GamepadTouchButton(
                            label = "SELECT",
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.width(80.dp).height(32.dp),
                            fontSize = 10.sp,
                            activeColor = CyberCyan,
                            onPressed = { viewModel.updateButtons(InputState.BTN_SELECT, it) }
                        )

                        GamepadTouchButton(
                            label = "START",
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.width(80.dp).height(32.dp),
                            fontSize = 10.sp,
                            activeColor = CyberPurple,
                            onPressed = { viewModel.updateButtons(InputState.BTN_START, it) }
                        )
                    }

                    // Gesture / Touchpad Zone
                    VirtualTouchpad(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp)),
                        onInput = { x, y, state -> viewModel.updateTouchpad(x, y, state) }
                    )

                    // Gyro telemetry crosshair status visualizer
                    if (gyroActive) {
                        GyroCrosshairIndicator(
                            gyroX = inputState.gyroX,
                            gyroY = inputState.gyroY,
                            gymZ = inputState.gyroZ,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("GYRO EMULATOR DISABLED", color = Color.DarkGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // =============== RIGHT CONTROLLER PLATE (ACTION BUTTONS & R-STICK) ===============
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Right Joystick Stick
                        VirtualAnalogStick(
                            tagLabel = "R-STICK",
                            onValueChange = { x, y -> viewModel.updateRightAnalog(x, y) }
                        )

                        // Diamond Action Buttons Group
                        DiamondActionButtons(
                            onPress = { buttonMask, pressed ->
                                viewModel.updateButtons(buttonMask, pressed)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GamepadTouchButton(
    label: String,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    activeColor: Color = CyberCyan,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    onPressed: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (pressed) {
                    Brush.radialGradient(listOf(activeColor.copy(alpha = 0.4f), Color.Transparent))
                } else {
                    Brush.verticalGradient(listOf(DarkSurface, DarkSurface))
                }
            )
            .border(1.dp, if (pressed) activeColor else DarkCardBorder, shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            pressed = true
                            onPressed(true)
                            awaitRelease()
                        } finally {
                            pressed = false
                            onPressed(false)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (pressed) activeColor else LightGray,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun TriggerGaugeButton(
    label: String,
    isActive: Boolean,
    analogValue: Float,
    onPressed: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(isActive) }

    Row(
        modifier = modifier
            .width(135.dp)
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurface)
            .border(1.dp, if (isPressed) CyberCyan else DarkCardBorder, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPressed = true
                            onPressed(true)
                            awaitRelease()
                        } finally {
                            isPressed = false
                            onPressed(false)
                        }
                    }
                )
            }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = if (isPressed) CyberCyan else Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            // Gauge Bar Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(if (isPressed) 1f else analogValue)
                        .background(if (isPressed) CyberCyan else CyberPurple)
                )
            }
        }
        Text(
            text = if (isPressed) "100%" else String.format("%d%%", (analogValue * 100).toInt()),
            color = if (isPressed) CyberCyan else LightGray,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun VirtualAnalogStick(
    tagLabel: String,
    onValueChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerSizeDp = 135.dp
    val thumbSizeDp = 48.dp
    val density = LocalDensity.current
    
    val containerSizePx = with(density) { containerSizeDp.toPx() }
    val radiusPx = containerSizePx / 2
    
    // Joystick Center Offset
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(containerSizeDp),
        contentAlignment = Alignment.Center
    ) {
        // Outer track canvas decoration
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(listOf(DarkSurface, Color.Black)),
                radius = radiusPx,
                center = center
            )
            drawCircle(
                color = DarkCardBorder,
                radius = radiusPx,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            // Interlocking Target Crosshairs
            drawLine(
                color = DarkCardBorder.copy(alpha = 0.5f),
                start = Offset(center.x - radiusPx, center.y),
                end = Offset(center.x + radiusPx, center.y),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = DarkCardBorder.copy(alpha = 0.5f),
                start = Offset(center.x, center.y - radiusPx),
                end = Offset(center.x, center.y + radiusPx),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Tactile central visual indicator
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.x.toInt(), thumbOffset.y.toInt()) }
                .size(thumbSizeDp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(CyberCyan, CyberPurple),
                        radius = with(density) { (thumbSizeDp / 2).toPx() }
                    )
                )
                .border(2.dp, LightGray, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            thumbOffset = Offset.Zero
                            onValueChange(0f, 0f)
                        },
                        onDragCancel = {
                            thumbOffset = Offset.Zero
                            onValueChange(0f, 0f)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val rawOffset = thumbOffset + dragAmount
                            val distance = sqrt(rawOffset.x * rawOffset.x + rawOffset.y * rawOffset.y)

                            // Constraint touch coordinates inside the boundary circle radius
                            val maxRadius = radiusPx - with(density) { (thumbSizeDp / 2).toPx() }
                            val constrainedOffset = if (distance > maxRadius) {
                                Offset(
                                    x = rawOffset.x * (maxRadius / distance),
                                    y = rawOffset.y * (maxRadius / distance)
                                )
                            } else {
                                rawOffset
                            }

                            thumbOffset = constrainedOffset
                            
                            // Emit raw Joy coordinates normalized into high precision bounds -1.0 to 1.0f
                            onValueChange(
                                constrainedOffset.x / maxRadius,
                                -(constrainedOffset.y / maxRadius) // Invert Y axis for PC analogs
                            )
                        }
                    )
                }
        )
        Text(
            text = tagLabel,
            color = Color.White.copy(0.12f),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun DiamondActionButtons(
    onPress: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = 44.dp
    Box(
        modifier = modifier
            .size(135.dp),
        contentAlignment = Alignment.Center
    ) {
        // Y button (Top)
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            RoundTouchButton("Y", CyberPurple, buttonSize) { onPress(InputState.BTN_Y, it) }
        }
        // X button (Left)
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            RoundTouchButton("X", CyberCyan, buttonSize) { onPress(InputState.BTN_X, it) }
        }
        // B button (Right)
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            RoundTouchButton("B", CyberCyan, buttonSize) { onPress(InputState.BTN_B, it) }
        }
        // A button (Bottom)
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            RoundTouchButton("A", CyberPurple, buttonSize) { onPress(InputState.BTN_A, it) }
        }
    }
}

@Composable
fun VirtualDpad(
    onPress: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val dpadSize = 135.dp
    val buttonSize = 44.dp

    Box(
        modifier = modifier.size(dpadSize),
        contentAlignment = Alignment.Center
    ) {
        // Back panel Cross layout
        Box(
            modifier = Modifier
                .size(width = 135.dp, height = 46.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
        )
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 135.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp))
        )

        // Direction buttons
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            RoundArrowButton("▲", buttonSize) { onPress(InputState.BTN_DPAD_UP, it) }
        }
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            RoundArrowButton("◀", buttonSize) { onPress(InputState.BTN_DPAD_LEFT, it) }
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            RoundArrowButton("▶", buttonSize) { onPress(InputState.BTN_DPAD_RIGHT, it) }
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            RoundArrowButton("▼", buttonSize) { onPress(InputState.BTN_DPAD_DOWN, it) }
        }
    }
}

@Composable
fun RoundTouchButton(
    label: String,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    onPressed: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPressed) color.copy(alpha = 0.35f) else DarkSurface)
            .border(2.dp, if (isPressed) color else color.copy(alpha = 0.5f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPressed = true
                            onPressed(true)
                            awaitRelease()
                        } finally {
                            isPressed = false
                            onPressed(false)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.White else color,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun RoundArrowButton(
    label: String,
    size: androidx.compose.ui.unit.Dp,
    onPressed: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isPressed) CyberCyan.copy(alpha = 0.25f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPressed = true
                            onPressed(true)
                            awaitRelease()
                        } finally {
                            isPressed = false
                            onPressed(false)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) CyberCyan else LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
fun VirtualTouchpad(
    modifier: Modifier = Modifier,
    onInput: (Float, Float, Int) -> Unit
) {
    var touchX by remember { mutableStateOf(0f) }
    var touchY by remember { mutableStateOf(0f) }
    var touchState by remember { mutableStateOf(0) } // 0 idle, 1 touch, 2 drag

    Box(
        modifier = modifier
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // relative coordinate mapping
                        touchX = offset.x / size.width
                        touchY = offset.y / size.height
                        touchState = 1
                        onInput(touchX, touchY, 1)
                    },
                    onDragEnd = {
                        touchX = 0f
                        touchY = 0f
                        touchState = 0
                        onInput(0f, 0f, 0)
                    },
                    onDragCancel = {
                        touchX = 0f
                        touchY = 0f
                        touchState = 0
                        onInput(0f, 0f, 0)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        touchX = change.position.x / size.width
                        touchY = change.position.y / size.height
                        touchState = 2
                        onInput(touchX, touchY, 2)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (touchState > 0) {
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.3f),
                    radius = 16.dp.toPx(),
                    center = Offset(touchX * size.width, touchY * size.height)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GESTURE ZONE",
                color = Color.DarkGray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            if (touchState > 0) {
                Text(
                    text = String.format("X: %.2f | Y: %.2f", touchX, touchY),
                    color = CyberCyan.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun GyroCrosshairIndicator(
    gyroX: Float,
    gyroY: Float,
    gymZ: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(6.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GYROSCOPE AIM FLIGHT SENSOR",
                color = NeonGreen,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gyro meter reading
                SensorBarGauge("X", gyroX, -4f..4f, CyberCyan)
                SensorBarGauge("Y", gyroY, -4f..4f, CyberPurple)
                SensorBarGauge("Z", gymZ, -4f..4f, NeonGreen)
            }
        }
    }
}

@Composable
fun SensorBarGauge(
    axis: String,
    value: Float,
    range: ClosedRange<Float>,
    color: Color
) {
    val progress = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("$axis:", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(5.dp)
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(color)
            )
        }
    }
}
