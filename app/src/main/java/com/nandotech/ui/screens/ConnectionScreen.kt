package com.nandotech.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nandotech.data.ConnectionState
import com.nandotech.ui.theme.*
import com.nandotech.viewmodel.ConnectionViewModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    onEnterController: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val selectedTransportIndex by viewModel.selectedTransportIndex.collectAsState()
    val logLines by viewModel.logLines.collectAsState()
    val targetIp by viewModel.targetIpAddress.collectAsState()

    var showIpDialog by remember { mutableStateOf(false) }
    var inputIp by remember { mutableStateOf(targetIp) }
    val logListState = rememberLazyListState()

    // Auto-scroll log ke bawah
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) logListState.animateScrollToItem(logLines.size - 1)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(12.dp)
    ) {

        // ── HEADER ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "V-PAD BRIDGE",
                    color = CyberCyan,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "Connection status dashboard",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            // Status badge
            val (badgeColor, badgeText) = when (connectionState) {
                ConnectionState.CONNECTED   -> NeonGreen to "● CONNECTED"
                ConnectionState.CONNECTING  -> CyberPurple to "◌ CONNECTING"
                ConnectionState.ERROR       -> AlertRed to "✕ ERROR"
                ConnectionState.DISCONNECTED -> Color.Gray to "○ OFFLINE"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(badgeText, color = badgeColor, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── TRANSPORT SELECTOR ───────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurface)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                .padding(3.dp)
        ) {
            listOf("USB / ADB", "WI-FI LAN").forEachIndexed { index, label ->
                val selected = selectedTransportIndex == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected)
                                Brush.horizontalGradient(listOf(CyberCyan.copy(.2f), CyberPurple.copy(.2f)))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { viewModel.selectTransport(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (selected) CyberCyan else Color.Gray,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // ── MAIN BODY: kiri instruksi+tombol, kanan log ─────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── KIRI: instruksi + tombol ─────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Instruksi
                Column(
                    modifier = Modifier
                        .weight(1f) // Mengambil sisa space yang gak dipake tombol
                        .verticalScroll(rememberScrollState()) // Biar instruksinya bisa di-scroll pas landscape
                ) {
                    Text(
                        "SETUP INSTRUKSI",
                        color = LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedTransportIndex == 0) {
                        StepRow(1, "USB Debugging aktif di HP.")
                        StepRow(2, "Colok HP ke PC via kabel USB.")
                        StepRow(3, "Jalankan di PowerShell PC:")
                        CodeBox("adb reverse tcp:7890 tcp:7890")
                        StepRow(4, "Klik START di Android Bridge PC.")
                        StepRow(5, "Tap tombol CONNECT di bawah.")
                    } else {
                        StepRow(1, "HP & PC dalam WiFi yang sama.")
                        StepRow(2, "Klik START di Android Bridge PC.")
                        StepRow(3, "Set IP PC di bawah ini:")

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .clickable { inputIp = targetIp; showIpDialog = true }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("IP PC TARGET", color = Color.Gray, fontSize = 9.sp)
                                Text(targetIp, color = CyberCyan, fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Icon(Icons.Default.Edit, null, tint = CyberPurple, modifier = Modifier.size(18.dp))
                        }
                        StepRow(4, "Tap CONNECT di bawah.")
                    }
                }
                // ── TOMBOL CONNECT — selalu di bawah, tidak bisa hilang ──
                Column {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol utama connect/disconnect
                    val btnColor = when (connectionState) {
                        ConnectionState.CONNECTED    -> AlertRed
                        ConnectionState.CONNECTING   -> CyberPurple
                        ConnectionState.ERROR        -> AlertRed
                        ConnectionState.DISCONNECTED -> CyberCyan
                    }
                    val btnText = when (connectionState) {
                        ConnectionState.CONNECTED    -> "DISCONNECT"
                        ConnectionState.CONNECTING   -> "CONNECTING..."
                        ConnectionState.ERROR        -> "RETRY CONNECT"
                        ConnectionState.DISCONNECTED -> "CONNECT"
                    }

                    Button(
                        onClick = {
                            if (connectionState == ConnectionState.CONNECTED ||
                                connectionState == ConnectionState.CONNECTING) {
                                viewModel.disconnect()
                            } else {
                                viewModel.connect()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = btnColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (connectionState == ConnectionState.CONNECTING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            btnText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Tombol GO TO CONTROLLER — muncul hanya saat connected
                    if (connectionState == ConnectionState.CONNECTED) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onEnterController,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonGreen,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "BUKA CONTROLLER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // ── KANAN: log terminal ──────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
                    .border(1.dp, Color(0xFF1E1E1E), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "DIAGNOSTICS LOG",
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "${logLines.size} lines",
                        color = Color.DarkGray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (logLines.isEmpty()) {
                        Text(
                            "Menunggu koneksi...",
                            color = Color.DarkGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        LazyColumn(
                            state = logListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(logLines) { line ->
                                Text(
                                    text = line,
                                    color = when {
                                        line.contains("error", true) ||
                                                line.contains("fail", true) ||
                                                line.contains("gagal", true) -> AlertRed
                                        line.contains("OK", true) ||
                                                line.contains("berhasil", true) -> NeonGreen
                                        else -> LightGray
                                    },
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── IP DIALOG ────────────────────────────────────────────────
    if (showIpDialog) {
        AlertDialog(
            onDismissRequest = { showIpDialog = false },
            title = { Text("IP PC Target", color = CyberCyan, fontFamily = FontFamily.Monospace) },
            text = {
                OutlinedTextField(
                    value = inputIp,
                    onValueChange = { inputIp = it },
                    singleLine = true,
                    placeholder = { Text("192.168.x.x", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = DarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.targetIpAddress.value = inputIp
                    showIpDialog = false
                }) {
                    Text("SIMPAN", color = NeonGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showIpDialog = false }) {
                    Text("BATAL", color = Color.Gray)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun StepRow(index: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "$index.",
            color = CyberPurple,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(18.dp)
        )
        Text(text, color = LightGray.copy(.85f), fontSize = 12.sp)
    }
}

@Composable
private fun CodeBox(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(code, color = NeonGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}