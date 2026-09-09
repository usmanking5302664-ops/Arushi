package com.example.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.ActionResult
import com.example.data.ContactItem

class AndroidActionManager(private val context: Context) {

    // Pre-seeded contacts for emulator testing (e.g., Mom, Usman, Dad, Rahul)
    private val defaultDemoContacts = listOf(
        ContactItem("d1", "Mom", "+91 98765 43211"),
        ContactItem("d2", "Mummy", "+91 98765 43211"),
        ContactItem("d3", "Usman", "+91 98765 43212"),
        ContactItem("d4", "Dad", "+91 98765 43213"),
        ContactItem("d5", "Papa", "+91 98765 43213"),
        ContactItem("d6", "Rahul Sharma", "+91 98765 43214"),
        ContactItem("d7", "Rahul Verma", "+91 98765 43215")
    )

    fun openWhatsApp(): ActionResult {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.whatsapp")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                ActionResult(true, "openWhatsApp", "WhatsApp opened successfully on your device.")
            } else {
                // Try deep link fallback
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send"))
                webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (webIntent.resolveActivity(pm) != null) {
                    context.startActivity(webIntent)
                    ActionResult(true, "openWhatsApp", "Opened WhatsApp via web link.")
                } else {
                    ActionResult(false, "openWhatsApp", "WhatsApp is not installed on this device.")
                }
            }
        } catch (e: Exception) {
            ActionResult(false, "openWhatsApp", "Could not open WhatsApp: ${e.localizedMessage}")
        }
    }

    fun openApp(appName: String): ActionResult {
        val trimmed = appName.trim().lowercase()
        return try {
            when {
                trimmed.contains("whatsapp") -> openWhatsApp()
                trimmed.contains("youtube") -> launchPackageOrIntent(
                    "com.google.android.youtube",
                    "https://www.youtube.com",
                    "YouTube"
                )
                trimmed.contains("instagram") -> launchPackageOrIntent(
                    "com.instagram.android",
                    "https://www.instagram.com",
                    "Instagram"
                )
                trimmed.contains("chrome") -> launchPackageOrIntent(
                    "com.android.chrome",
                    "https://www.google.com",
                    "Google Chrome"
                )
                trimmed.contains("setting") -> {
                    val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    ActionResult(true, "openApp", "Device Settings opened successfully.")
                }
                trimmed.contains("camera") -> {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (cameraIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(cameraIntent)
                        ActionResult(true, "openApp", "Camera opened successfully.")
                    } else {
                        ActionResult(false, "openApp", "Camera app is not available.")
                    }
                }
                trimmed.contains("map") -> launchPackageOrIntent(
                    "com.google.android.apps.maps",
                    "https://maps.google.com",
                    "Google Maps"
                )
                else -> {
                    // Try to search installed packages by name or label
                    val foundPackage = findAppPackageByName(trimmed)
                    if (foundPackage != null) {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(foundPackage)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                            return ActionResult(true, "openApp", "$appName opened successfully.")
                        }
                    }
                    ActionResult(false, "openApp", "App '$appName' is not installed on this device.")
                }
            }
        } catch (e: Exception) {
            ActionResult(false, "openApp", "Failed to open $appName: ${e.localizedMessage}")
        }
    }

    fun openUrl(url: String): ActionResult {
        return try {
            val formatted = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formatted)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult(true, "openUrl", "Opened URL: $formatted")
        } catch (e: Exception) {
            ActionResult(false, "openUrl", "Could not open URL: ${e.localizedMessage}")
        }
    }

    fun makeCall(phoneNumber: String): ActionResult {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isBlank()) {
            return ActionResult(false, "makeCall", "Invalid phone number provided.")
        }

        return try {
            val hasCallPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED

            if (hasCallPermission) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                ActionResult(true, "makeCall", "Calling $cleanNumber directly.")
            } else {
                // Safe confirmation / dialer fallback
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                ActionResult(true, "makeCall", "Phone dialer opened with number $cleanNumber for confirmation.")
            }
        } catch (e: Exception) {
            ActionResult(false, "makeCall", "Could not initiate call: ${e.localizedMessage}")
        }
    }

    fun callContact(contactName: String): ActionResult {
        val trimmedQuery = contactName.trim()
        if (trimmedQuery.isBlank()) {
            return ActionResult(false, "callContact", "Please specify a contact name to call.")
        }

        val matches = searchContacts(trimmedQuery)

        return when {
            matches.isEmpty() -> {
                ActionResult(
                    success = false,
                    actionType = "callContact",
                    message = "No contact found matching '$trimmedQuery'. Please verify the name."
                )
            }
            matches.size == 1 -> {
                val target = matches.first()
                val callResult = makeCall(target.phoneNumber)
                ActionResult(
                    success = callResult.success,
                    actionType = "callContact",
                    message = "Found contact '${target.name}' (${target.phoneNumber}). ${callResult.message}",
                    details = target.phoneNumber
                )
            }
            else -> {
                // Multiple matches - do NOT guess! Ask user which one to call
                val namesList = matches.joinToString(", ") { "${it.name} (${it.phoneNumber})" }
                ActionResult(
                    success = false,
                    actionType = "callContact",
                    message = "I found ${matches.size} contacts matching '$trimmedQuery': $namesList. Which one should I call?",
                    details = "MULTIPLE_MATCHES"
                )
            }
        }
    }

    fun searchContacts(query: String): List<ContactItem> {
        val queryLower = query.lowercase().trim()
        val results = mutableListOf<ContactItem>()

        // Check if READ_CONTACTS permission is granted to query device Contacts Provider
        val hasReadPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (hasReadPermission) {
            try {
                val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
                val selectionArgs = arrayOf("%$queryLower%")

                val cursor = context.contentResolver.query(
                    uri,
                    projection,
                    selection,
                    selectionArgs,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )

                cursor?.use {
                    val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
                    val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (it.moveToNext()) {
                        val id = it.getString(idCol) ?: ""
                        val name = it.getString(nameCol) ?: ""
                        val number = it.getString(numCol) ?: ""
                        if (number.isNotBlank()) {
                            results.add(ContactItem(id, name, number))
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore and fallback
            }
        }

        // If no results found in device contacts (or in emulator without contacts entered),
        // match against demo contacts (Mom/Mummy, Usman, Dad/Papa, Rahul Sharma, Rahul Verma)
        if (results.isEmpty()) {
            val demoMatches = defaultDemoContacts.filter {
                it.name.lowercase().contains(queryLower) ||
                        (queryLower in listOf("mom", "mother", "mummy", "maa") && it.name.lowercase() in listOf("mom", "mummy")) ||
                        (queryLower in listOf("dad", "father", "papa") && it.name.lowercase() in listOf("dad", "papa"))
            }.distinctBy { it.phoneNumber }
            results.addAll(demoMatches)
        }

        return results
    }

    private fun launchPackageOrIntent(packageName: String, fallbackUrl: String, appTitle: String): ActionResult {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(true, "openApp", "$appTitle opened successfully.")
        } else {
            // Fallback to web URL
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            ActionResult(true, "openApp", "$appTitle not installed locally, opened in browser.")
        }
    }

    private fun findAppPackageByName(appNameLower: String): String? {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in apps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(appNameLower) || app.packageName.lowercase().contains(appNameLower)) {
                return app.packageName
            }
        }
        return null
    }
}
