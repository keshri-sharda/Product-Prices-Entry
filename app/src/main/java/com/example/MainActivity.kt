package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.PriceListAppScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PriceListViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val priceListViewModel: PriceListViewModel = viewModel()
      val darkTheme = when (priceListViewModel.appThemeMode) {
          "LIGHT" -> false
          "DARK" -> true
          else -> androidx.compose.foundation.isSystemInDarkTheme()
      }
      MyApplicationTheme(darkTheme = darkTheme, palette = priceListViewModel.appThemePalette) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          val currentDensity = LocalDensity.current
          CompositionLocalProvider(
            LocalDensity provides Density(
              density = currentDensity.density,
              fontScale = currentDensity.fontScale * priceListViewModel.fontSizeScale
            )
          ) {
            PriceListAppScreen(viewModel = priceListViewModel)
          }
        }
      }
    }
  }
}
