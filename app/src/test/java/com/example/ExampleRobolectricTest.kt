package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.PriceListViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Shop Price List", appName)
  }

  @Test
  fun `profile creation sets state and updates preferences`() {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = PriceListViewModel(context)
    
    // Create profile
    viewModel.handleProfileCreation("John Doe", "Apex Mart")
    
    // Verify properties
    assertEquals("Apex Mart", viewModel.customShopName)
    assertEquals("John Doe", viewModel.cloudUsername)
    assertEquals(Screen.FOLDERS, viewModel.currentScreen)
    assertTrue(viewModel.isLoggedIn)
    assertTrue(viewModel.showWelcomeUser)
    
    // Verify SharedPreferences
    val prefs = context.getSharedPreferences("price_list_prefs", Context.MODE_PRIVATE)
    assertTrue(prefs.getBoolean("is_profile_created", false))
    assertEquals("Apex Mart", prefs.getString("settings_shop_name", ""))
    assertEquals("John Doe", prefs.getString("cloud_username", ""))
  }
}
