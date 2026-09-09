package com.example.services

import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * JavaScript-to-native bridge for Android actions.
 * Exposes:
 * - openApp(appName)
 * - makeCall(phoneNumber)
 * - callContact(contactName)
 * - openWhatsApp()
 * - openUrl(url)
 */
class AndroidBridge(
    private val actionManager: AndroidActionManager,
    private val onActionExecuted: ((String, Boolean, String) -> Unit)? = null
) {

    @JavascriptInterface
    fun openApp(appName: String): String {
        val result = actionManager.openApp(appName)
        onActionExecuted?.invoke("openApp($appName)", result.success, result.message)
        return JSONObject().apply {
            put("success", result.success)
            put("action", "openApp")
            put("message", result.message)
        }.toString()
    }

    @JavascriptInterface
    fun makeCall(phoneNumber: String): String {
        val result = actionManager.makeCall(phoneNumber)
        onActionExecuted?.invoke("makeCall($phoneNumber)", result.success, result.message)
        return JSONObject().apply {
            put("success", result.success)
            put("action", "makeCall")
            put("message", result.message)
        }.toString()
    }

    @JavascriptInterface
    fun callContact(contactName: String): String {
        val result = actionManager.callContact(contactName)
        onActionExecuted?.invoke("callContact($contactName)", result.success, result.message)
        return JSONObject().apply {
            put("success", result.success)
            put("action", "callContact")
            put("message", result.message)
            put("details", result.details ?: "")
        }.toString()
    }

    @JavascriptInterface
    fun openWhatsApp(): String {
        val result = actionManager.openWhatsApp()
        onActionExecuted?.invoke("openWhatsApp()", result.success, result.message)
        return JSONObject().apply {
            put("success", result.success)
            put("action", "openWhatsApp")
            put("message", result.message)
        }.toString()
    }

    @JavascriptInterface
    fun openUrl(url: String): String {
        val result = actionManager.openUrl(url)
        onActionExecuted?.invoke("openUrl($url)", result.success, result.message)
        return JSONObject().apply {
            put("success", result.success)
            put("action", "openUrl")
            put("message", result.message)
        }.toString()
    }

    @JavascriptInterface
    fun adjustVolume(direction: String, streamType: String? = "media", levelPercent: Int = -1): String {
        val lvl = if (levelPercent in 0..100) levelPercent else null
        val result = actionManager.adjustVolume(direction, streamType, lvl)
        onActionExecuted?.invoke("adjustVolume($direction, $streamType)", result.success, result.message)
        return JSONObject().apply {
            put("success", result.success)
            put("action", "adjustVolume")
            put("message", result.message)
        }.toString()
    }

    @JavascriptInterface
    fun installApp(packageNameOrAppName: String): String {
        val result = actionManager.installApp(packageNameOrAppName)
        val resolvedPackage = actionManager.resolvePackageName(packageNameOrAppName)
        onActionExecuted?.invoke("installApp($packageNameOrAppName)", result.success, result.message)
        return JSONObject().apply {
            put("success", result.success)
            put("action", "installApp")
            put("target", packageNameOrAppName)
            put("packageName", resolvedPackage)
            put("uri", "market://details?id=$resolvedPackage")
            put("message", result.message)
        }.toString()
    }

    @JavascriptInterface
    fun playMusic(query: String, platform: String? = "youtube"): String {
        val result = actionManager.playMusic(query, platform)
        onActionExecuted?.invoke("playMusic($query, $platform)", result.success, result.message)
        return JSONObject().apply {
            put("success", result.success)
            put("action", "playMusic")
            put("message", result.message)
        }.toString()
    }

    @JavascriptInterface
    @JvmOverloads
    fun controlMedia(command: String, targetApp: String? = null): String {
        val result = actionManager.controlMedia(command, targetApp)
        onActionExecuted?.invoke("controlMedia($command, ${targetApp ?: "all"})", result.success, result.message)
        return JSONObject().apply {
            put("success", result.success)
            put("action", "controlMedia")
            put("command", command)
            put("targetApp", targetApp ?: "media")
            put("message", result.message)
        }.toString()
    }

    @JavascriptInterface
    fun isNativeBridgeAvailable(): Boolean {
        return true
    }
}
