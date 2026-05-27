package com.nandotech.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandotech.ui.theme.*
import com.nandotech.viewmodel.ConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ConnectionViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Live reactive states
    val deadzone by viewModel.analogDeadzone.collectAsState()
    val isGyroActive by viewModel.gyroActive.collectAsState()
    
    // Direct sensor reader access (non-reactive settings that get synced immediately)
    var gyroSens by remember { mutableStateOf(viewModel.sensorReader.sensitivity) }
    var gyroSmooth by remember { mutableStateOf(viewModel.sensorReader.smoothing) }
    var isFiltered by remember { mutableStateOf(viewModel.sensorReader.isFilteringEnabled) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Upper Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "CONTROLLER ENGINE CONFIG",
                    color = CyberCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Refine axis deadzones, hardware calibration, and filter parameters",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First Grid half: Joystick controls
            Column(modifier = Modifier.weight(1f)) {
                SettingsCardHeader(title = "ANALOG JOYSTICKS DEADBAND")
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Virtual Joysticks Deadzone", color = LightGray, fontSize = 13.sp)
                            Text(
                                text = "${(deadzone * 100).toInt()}%",
                                color = CyberCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Suppresses static drift at stick center coordinates. Re-filters inputs linearly.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Slider(
                            value = deadzone,
                            onValueChange = { viewModel.analogDeadzone.value = it },
                            valueRange = 0.0f..0.30f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberCyan,
                                activeTrackColor = CyberCyan,
                                inactiveTrackColor = DarkCardBorder
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                SettingsCardHeader(title = "PHYSICAL SENSORS CALIBRATION")
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Calibers gyroscope and accelerometer axes offsets. Center the phone in your hands before calibration.",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.calibrateSensors() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("CALIBRATE GYRO", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            
                            OutlinedButton(
                                onClick = { viewModel.resetSensorCalibration() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(AlertRed.copy(0.4f))),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("CLEAR", fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // Second Grid half: Motion sensor / Gyro
            Column(modifier = Modifier.weight(1f)) {
                SettingsCardHeader(title = "GYRO MOTION EMULATION")
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Toggle Gyro Sensor Emulation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Gyro Enable Pitch/Yaw/Roll", color = LightGray, fontSize = 13.sp)
                                Text("Toggles hardware sensor data reading.", color = Color.Gray, fontSize = 10.sp)
                            }
                            Switch(
                                checked = isGyroActive,
                                onCheckedChange = { viewModel.gyroActive.value = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeonGreen,
                                    checkedTrackColor = NeonGreen.copy(0.3f),
                                    uncheckedBorderColor = DarkCardBorder
                                )
                            )
                        }

                        if (isGyroActive) {
                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = DarkCardBorder)

                            // Gyro Sensitivity slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Gyro Sensitivity Multiplier", color = LightGray, fontSize = 13.sp)
                                Text(
                                    text = String.format("%.1fx", gyroSens),
                                    color = CyberPurple,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Slider(
                                value = gyroSens,
                                onValueChange = {
                                    gyroSens = it
                                    viewModel.sensorReader.sensitivity = it
                                },
                                valueRange = 0.5f..5.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyberPurple,
                                    activeTrackColor = CyberPurple,
                                    inactiveTrackColor = DarkCardBorder
                                )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Low Pass Filter active toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Low Pass Smoothing Filter", color = LightGray, fontSize = 13.sp)
                                Switch(
                                    checked = isFiltered,
                                    onCheckedChange = {
                                        isFiltered = it
                                        viewModel.sensorReader.isFilteringEnabled = it
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = CyberCyan,
                                        checkedTrackColor = CyberCyan.copy(0.3f),
                                        uncheckedBorderColor = DarkCardBorder
                                    )
                                )
                            }

                            if (isFiltered) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Smoothing Dampening factor", color = Color.Gray, fontSize = 11.sp)
                                    Text(
                                        text = "${(gyroSmooth * 100).toInt()}%",
                                        color = CyberCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Slider(
                                    value = gyroSmooth,
                                    onValueChange = {
                                        gyroSmooth = it
                                        viewModel.sensorReader.smoothing = it
                                    },
                                    valueRange = 0.0f..0.90f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyberCyan,
                                        activeTrackColor = CyberCyan,
                                        inactiveTrackColor = DarkCardBorder
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // System Compatibility Card Info
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = BorderStroke(1.dp, Color(0xFF1B1B1C)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "V-PAD SYSTEM SPECS",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Gyro sensor: ${if (viewModel.sensorReader.isGyroAvailable) "AVAILABLE (READY)" else "NOT FOUND_ [Aiming disabled]"}  |  " +
                               "Accelerometer: ${if (viewModel.sensorReader.isAccelAvailable) "AVAILABLE (READY)" else "NOT FOUND_"}",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCardHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
    )
}
