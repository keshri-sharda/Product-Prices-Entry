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
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Firebase app context at the entry point of the application
    if (FirebaseApp.getApps(applicationContext).isEmpty()) {
      try {
        val prefs = getSharedPreferences("price_list_prefs", MODE_PRIVATE)
        val savedApiKey = prefs.getString("firebase_api_key", "")?.trim()
        val savedProjectId = prefs.getString("firebase_project_id", "")?.trim()
        val savedAppId = prefs.getString("firebase_app_id", "")?.trim()

        val apiKey = if (!savedApiKey.isNullOrBlank()) savedApiKey else {
          val bcKey = try {
            val field = BuildConfig::class.java.getField("FIREBASE_API_KEY")
            field.get(null) as? String
          } catch (e: Exception) { "" }
          if (!bcKey.isNullOrBlank() && bcKey != "YOUR_FIREBASE_API_KEY") bcKey else "placeholder_api_key_for_firestore_backup_use"
        }

        val projectId = if (!savedProjectId.isNullOrBlank()) savedProjectId else {
          val bcProj = try {
            val field = BuildConfig::class.java.getField("FIREBASE_PROJECT_ID")
            field.get(null) as? String
          } catch (e: Exception) { "" }
          if (!bcProj.isNullOrBlank() && bcProj != "YOUR_FIREBASE_PROJECT_ID") bcProj else "shop-pilot-sync"
        }

        val appId = if (!savedAppId.isNullOrBlank()) savedAppId else {
          val bcApp = try {
            val field = BuildConfig::class.java.getField("FIREBASE_APPLICATION_ID")
            field.get(null) as? String
          } catch (e: Exception) { "" }
          if (!bcApp.isNullOrBlank() && bcApp != "YOUR_FIREBASE_APPLICATION_ID") bcApp else "1:1733abcc-51ce-4d1f-a853-03b97c1cabc5:android:default"
        }

        val options = FirebaseOptions.Builder()
          .setApplicationId(appId)
          .setProjectId(projectId)
          .setApiKey(apiKey)
          .build()

        try {
          val existingApp = FirebaseApp.getInstance()
          if (existingApp.options.apiKey != apiKey || existingApp.options.projectId != projectId) {
            existingApp.delete()
            FirebaseApp.initializeApp(applicationContext, options)
          }
        } catch (e: Exception) {
          try {
            FirebaseApp.initializeApp(applicationContext, options)
          } catch (ex: Exception) {
            ex.printStackTrace()
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

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
