package com.nandotech

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nandotech.data.ConnectionState
import com.nandotech.ui.screens.ConnectionScreen
import com.nandotech.ui.screens.ControllerScreen
import com.nandotech.ui.screens.SettingsScreen
import com.nandotech.ui.theme.*
import com.nandotech.viewmodel.ConnectionViewModel

class MainActivity : ComponentActivity() {

    private var wifiP2pManager: WifiP2pManager? = null
    private var p2pChannel: WifiP2pManager.Channel? = null

    // Register permissions launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val nearbyGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.NEARBY_WIFI_DEVICES] == true
        } else {
            true
        }

        if (locationGranted || nearbyGranted) {
            initWifiDirectP2P()
        } else {
            Toast.makeText(this, "Wi-Fi Direct peer scanning will be degraded without scanning permissions.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Prompt permissions
        checkAndPromptPermissions()

        setContent {
            MyApplicationTheme {
                val viewModel: ConnectionViewModel = viewModel()
                
                // Track active screen:
                // "PANEL" = Settings/Discovery Tabs split
                // "CONTROLLER" = Immersive Fullscreen Controller
                var currentNavigationState by remember { mutableStateOf("PANEL") }
                
                // Track selected configuration split tab inside the PANEL state
                var selectedPanelTab by remember { mutableStateOf(0) } // 0 = CONNECTION, 1 = SETTINGS

                // Inject P2P manager reference when setup
//                LaunchedEffect(wifiP2pManager, p2pChannel) {
//                    viewModel.wifiTransport.checkAndInitializeP2p(wifiP2pManager, p2pChannel)
//                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    Crossfade(targetState = currentNavigationState, label = "ScreenTransition") { navigationState ->
                        when (navigationState) {
                            "PANEL" -> {
                                MainPanelLayout(
                                    selectedTab = selectedPanelTab,
                                    onTabSelected = { selectedPanelTab = it },
                                    onImmersiveControllerLaunch = { currentNavigationState = "CONTROLLER" },
                                    viewModel = viewModel
                                )
                            }
                            "CONTROLLER" -> {
                                ControllerScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { currentNavigationState = "PANEL" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndPromptPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            initWifiDirectP2P()
        }
    }

    private fun initWifiDirectP2P() {
        try {
            wifiP2pManager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
            p2pChannel = wifiP2pManager?.initialize(this, mainLooper, null)
        } catch (e: Exception) {
            // Devices without Wi-Fi direct fallback gracefully
        }
    }
}

@Composable
fun MainPanelLayout(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onImmersiveControllerLaunch: () -> Unit,
    viewModel: ConnectionViewModel
) {
    val connectionState by viewModel.connectionState.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // LEFT RAIL SIDE BAR (Futuristic cyberpunk active menu tabs)
        Column(
            modifier = Modifier
                .width(190.dp)
                .fillMaxHeight()
                .background(DarkSurface)
                .border(2.dp, DarkCardBorder, RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Branding Logo Group
                Row(
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.horizontalGradient(listOf(CyberCyan, CyberPurple)))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "V-PAD PRO",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actionable navigation tabs
                MenuNavigationRailItem(
                    icon = Icons.Default.Home,
                    label = "DIAGNOSTICS",
                    isSelected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                MenuNavigationRailItem(
                    icon = Icons.Default.Settings,
                    label = "ENGINE TUNER",
                    isSelected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
            }

            // Quick launch launcher banner section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.4f))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CONTROLLER IMMERSION",
                    color = Color.Gray,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                
                Button(
                    onClick = onImmersiveControllerLaunch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (connectionState == ConnectionState.CONNECTED) CyberCyan else DarkGray,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "GO ALIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // RIGHT PAGE DISPLAY FRAME
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            when (selectedTab) {
                0 -> ConnectionScreen(
                    viewModel = viewModel,
                    onEnterController = onImmersiveControllerLaunch,
                    modifier = Modifier.fillMaxSize()
                )
                1 -> SettingsScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun MenuNavigationRailItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) {
                    Modifier.background(Brush.horizontalGradient(listOf(CyberCyan.copy(alpha = 0.12f), Color.Transparent)))
                } else {
                    Modifier
                }
            )
            .border(
                1.dp,
                if (isSelected) CyberCyan.copy(0.2f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) CyberCyan else Color.Gray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}
