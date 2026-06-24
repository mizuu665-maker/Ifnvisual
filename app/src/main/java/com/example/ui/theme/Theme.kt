package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = IfnBlue,
    secondary = IfnCyan,
    tertiary = IfnCyanDark,
    background = DarkBg,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = DarkBg,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = IfnBlue,
    secondary = IfnCyan,
    tertiary = IfnBlueDark,
    background = LightBg,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = DarkBg,
    onBackground = androidx.compose.ui.graphics.Color(0xFF0F172A),
    onSurface = androidx.compose.ui.graphics.Color(0xFF0F172A),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF475569)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color by default to preserve custom branding color schema
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
