package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RobloxDarkColorScheme = darkColorScheme(
  primary = NeonPurple,
  onPrimary = Color.White,
  primaryContainer = SurfaceElevated,
  onPrimaryContainer = NeonCyanGlow,
  secondary = NeonCyan,
  onSecondary = CyberBlack,
  secondaryContainer = SurfaceCard,
  onSecondaryContainer = NeonCyan,
  tertiary = NeonPink,
  onTertiary = Color.White,
  background = CyberBlack,
  onBackground = TextPrimary,
  surface = SurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = SurfaceCard,
  onSurfaceVariant = TextSecondary,
  outline = SurfaceCardBorder,
  error = NeonRed,
  onError = Color.White
)

@Composable
fun RobloxShortsTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = RobloxDarkColorScheme,
    typography = Typography,
    content = content
  )
}
