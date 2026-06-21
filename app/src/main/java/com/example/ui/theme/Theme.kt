package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Palettes
private val LightSageColorScheme = lightColorScheme(
    primary = LightSagePrimary,
    onPrimary = LightSageOnPrimary,
    primaryContainer = LightSagePrimaryContainer,
    secondary = LightSageSecondary,
    secondaryContainer = LightSageSecondaryContainer,
    background = LightSageBackground,
    surface = LightSageSurface,
    onBackground = Color(0xFF1B1D1E),
    onSurface = Color(0xFF1B1D1E)
)
private val DarkSageColorScheme = darkColorScheme(
    primary = DarkSagePrimary,
    onPrimary = DarkSageOnPrimary,
    primaryContainer = DarkSagePrimaryContainer,
    secondary = DarkSageSecondary,
    secondaryContainer = DarkSageSecondaryContainer,
    background = DarkSageBackground,
    surface = DarkSageSurface
)

private val LightBlueColorScheme = lightColorScheme(
    primary = LightBluePrimary,
    onPrimary = LightBlueOnPrimary,
    primaryContainer = LightBluePrimaryContainer,
    secondary = LightBlueSecondary,
    secondaryContainer = LightBlueSecondaryContainer,
    background = LightBlueBackground,
    surface = LightBlueSurface,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)
private val DarkBlueColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = DarkBlueOnPrimary,
    primaryContainer = DarkBluePrimaryContainer,
    secondary = DarkBlueSecondary,
    secondaryContainer = DarkBlueSecondaryContainer,
    background = DarkBlueBackground,
    surface = DarkBlueSurface
)

private val LightCrimsonColorScheme = lightColorScheme(
    primary = LightCrimsonPrimary,
    onPrimary = LightCrimsonOnPrimary,
    primaryContainer = LightCrimsonPrimaryContainer,
    secondary = LightCrimsonSecondary,
    secondaryContainer = LightCrimsonSecondaryContainer,
    background = LightCrimsonBackground,
    surface = LightCrimsonSurface,
    onBackground = Color(0xFF1C0D12),
    onSurface = Color(0xFF1C0D12)
)
private val DarkCrimsonColorScheme = darkColorScheme(
    primary = DarkCrimsonPrimary,
    onPrimary = DarkCrimsonOnPrimary,
    primaryContainer = DarkCrimsonPrimaryContainer,
    secondary = DarkCrimsonSecondary,
    secondaryContainer = DarkCrimsonSecondaryContainer,
    background = DarkCrimsonBackground,
    surface = DarkCrimsonSurface
)

private val LightTealColorScheme = lightColorScheme(
    primary = LightTealPrimary,
    onPrimary = LightTealOnPrimary,
    primaryContainer = LightTealPrimaryContainer,
    secondary = LightTealSecondary,
    secondaryContainer = LightTealSecondaryContainer,
    background = LightTealBackground,
    surface = LightTealSurface,
    onBackground = Color(0xFF0B1414),
    onSurface = Color(0xFF0B1414)
)
private val DarkTealColorScheme = darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = DarkTealOnPrimary,
    primaryContainer = DarkTealPrimaryContainer,
    secondary = DarkTealSecondary,
    secondaryContainer = DarkTealSecondaryContainer,
    background = DarkTealBackground,
    surface = DarkTealSurface
)

private val LightGoldenColorScheme = lightColorScheme(
    primary = LightGoldenPrimary,
    onPrimary = LightGoldenOnPrimary,
    primaryContainer = LightGoldenPrimaryContainer,
    secondary = LightGoldenSecondary,
    secondaryContainer = LightGoldenSecondaryContainer,
    background = LightGoldenBackground,
    surface = LightGoldenSurface,
    onBackground = Color(0xFF14110B),
    onSurface = Color(0xFF14110B)
)
private val DarkGoldenColorScheme = darkColorScheme(
    primary = DarkGoldenPrimary,
    onPrimary = DarkGoldenOnPrimary,
    primaryContainer = DarkGoldenPrimaryContainer,
    secondary = DarkGoldenSecondary,
    secondaryContainer = DarkGoldenSecondaryContainer,
    background = DarkGoldenBackground,
    surface = DarkGoldenSurface
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  palette: String = "SAGE",
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> {
        when (palette.uppercase()) {
          "BLUE" -> DarkBlueColorScheme
          "CRIMSON" -> DarkCrimsonColorScheme
          "TEAL" -> DarkTealColorScheme
          "GOLDEN" -> DarkGoldenColorScheme
          else -> DarkSageColorScheme
        }
      }
      else -> {
        when (palette.uppercase()) {
          "BLUE" -> LightBlueColorScheme
          "CRIMSON" -> LightCrimsonColorScheme
          "TEAL" -> LightTealColorScheme
          "GOLDEN" -> LightGoldenColorScheme
          else -> LightSageColorScheme
        }
      }
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
