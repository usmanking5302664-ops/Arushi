package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.services.AndroidActionManager
import com.example.services.AndroidBridge
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var actionManager: AndroidActionManager
  private lateinit var bridge: AndroidBridge

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    actionManager = AndroidActionManager(context)
    bridge = AndroidBridge(actionManager)
  }

  @Test
  fun `read string from context`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("Arushi", appName)
  }

  @Test
  fun `action manager handles openWhatsApp gracefully`() {
    val result = actionManager.openWhatsApp()
    assertEquals("openWhatsApp", result.actionType)
  }

  @Test
  fun `action manager handles makeCall safely`() {
    val result = actionManager.makeCall("9876543210")
    assertTrue(result.success)
    assertEquals("makeCall", result.actionType)
  }

  @Test
  fun `action manager searches contacts and matches Mom`() {
    val result = actionManager.callContact("Mom")
    assertTrue(result.success)
    assertEquals("callContact", result.actionType)
    assertTrue(result.message.contains("Mom") || result.message.contains("98765 43211"))
  }

  @Test
  fun `action manager searches contacts and matches Usman`() {
    val result = actionManager.callContact("Usman")
    assertTrue(result.success)
    assertEquals("callContact", result.actionType)
    assertTrue(result.message.contains("Usman"))
  }

  @Test
  fun `action manager handles multiple contact matches without guessing`() {
    // "Rahul" has Rahul Sharma and Rahul Verma
    val result = actionManager.callContact("Rahul")
    assertFalse(result.success)
    assertEquals("MULTIPLE_MATCHES", result.details)
    assertTrue(result.message.contains("Rahul Sharma"))
    assertTrue(result.message.contains("Rahul Verma"))
  }

  @Test
  fun `action manager handles unknown contact safely`() {
    val result = actionManager.callContact("NonExistentPersonXyz123")
    assertFalse(result.success)
    assertTrue(result.message.contains("No contact found"))
  }

  @Test
  fun `android bridge executes actions and returns valid JSON`() {
    assertTrue(bridge.isNativeBridgeAvailable())

    val callJson = JSONObject(bridge.makeCall("9876543210"))
    assertTrue(callJson.getBoolean("success"))
    assertEquals("makeCall", callJson.getString("action"))

    val contactJson = JSONObject(bridge.callContact("Usman"))
    assertTrue(contactJson.getBoolean("success"))
    assertEquals("callContact", contactJson.getString("action"))

    val appJson = JSONObject(bridge.openApp("YouTube"))
    assertEquals("openApp", appJson.getString("action"))
  }
}
