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

    val volJson = JSONObject(bridge.adjustVolume("up"))
    assertTrue(volJson.getBoolean("success"))
    assertEquals("adjustVolume", volJson.getString("action"))

    val installJson = JSONObject(bridge.installApp("WhatsApp"))
    assertTrue(installJson.getBoolean("success"))
    assertEquals("installApp", installJson.getString("action"))
    assertEquals("com.whatsapp", installJson.getString("packageName"))
    assertEquals("market://details?id=com.whatsapp", installJson.getString("uri"))

    val installPackageJson = JSONObject(bridge.installApp("org.telegram.messenger"))
    assertTrue(installPackageJson.getBoolean("success"))
    assertEquals("org.telegram.messenger", installPackageJson.getString("packageName"))
    assertEquals("market://details?id=org.telegram.messenger", installPackageJson.getString("uri"))

    val musicJson = JSONObject(bridge.playMusic("Top hits", "youtube"))
    assertTrue(musicJson.getBoolean("success"))
    assertEquals("playMusic", musicJson.getString("action"))

    val controlJson = JSONObject(bridge.controlMedia("play", "youtube"))
    assertTrue(controlJson.getBoolean("success"))
    assertEquals("controlMedia", controlJson.getString("action"))
    assertEquals("play", controlJson.getString("command"))
    assertEquals("youtube", controlJson.getString("targetApp"))
  }

  @Test
  fun `action manager controls media with play pause and skip commands`() {
    val playResult = actionManager.controlMedia("play", "youtube")
    assertTrue(playResult.success)
    assertEquals("controlMedia", playResult.actionType)
    assertTrue(playResult.message.contains("YouTube"))

    val pauseResult = actionManager.controlMedia("pause")
    assertTrue(pauseResult.success)
    assertEquals("controlMedia", pauseResult.actionType)
    assertTrue(pauseResult.message.contains("Paused"))

    val skipResult = actionManager.controlMedia("skip", "com.spotify.music")
    assertTrue(skipResult.success)
    assertEquals("controlMedia", skipResult.actionType)
    assertTrue(skipResult.message.contains("Skipped") || skipResult.message.contains("media button"))
  }

  @Test
  fun `installApp deep links directly to Play Store install page using market details URI`() {
    val result = actionManager.installApp("com.instagram.android")
    assertTrue(result.success)
    assertEquals("installApp", result.actionType)
    assertTrue(result.message.contains("market") || result.message.contains("Play Store"))

    val resolved = actionManager.resolvePackageName("com.instagram.android")
    assertEquals("com.instagram.android", resolved)

    val resolvedNamed = actionManager.resolvePackageName("WhatsApp")
    assertEquals("com.whatsapp", resolvedNamed)
  }

  @Test
  fun `action manager adjusts volume up and down`() {
    val upResult = actionManager.adjustVolume("up", "media")
    assertTrue(upResult.success)
    assertEquals("adjustVolume", upResult.actionType)

    val downResult = actionManager.adjustVolume("down", "media")
    assertTrue(downResult.success)
    assertEquals("adjustVolume", downResult.actionType)

    val muteResult = actionManager.adjustVolume("mute", "media")
    assertTrue(muteResult.success)
    assertEquals("adjustVolume", muteResult.actionType)
  }

  @Test
  fun `action manager installs app from play store`() {
    val result = actionManager.installApp("WhatsApp")
    assertTrue(result.success)
    assertEquals("installApp", result.actionType)
    assertTrue(result.message.contains("Play Store"))
  }

  @Test
  fun `action manager plays music on youtube`() {
    val result = actionManager.playMusic("Arijit Singh hits", "youtube")
    assertTrue(result.success)
    assertEquals("playMusic", result.actionType)
    assertTrue(result.message.contains("YouTube"))
  }
}
