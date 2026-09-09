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
    fun isNativeBridgeAvailable(): Boolean {
        return true
    }
}
