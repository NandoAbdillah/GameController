package com.nandotech.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = CyberCyan,
    secondary = CyberPurple,
    tertiary = NeonGreen,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onSecondary = LightGray,
    onBackground = LightGray,
    onSurface = LightGray,
    error = AlertRed
  )

private val LightColorScheme = DarkColorScheme // Forced dark theme by default for elite gamer UI styling

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode for gaming UI
  dynamicColor: Boolean = false, // Disable dynamic colors to keep elite custom design branding intact
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
