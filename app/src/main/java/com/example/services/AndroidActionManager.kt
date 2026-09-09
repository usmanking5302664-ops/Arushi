package com.example.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.view.KeyEvent
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

    fun adjustVolume(
        direction: String,
        streamType: String? = "media",
        levelPercent: Int? = null
    ): ActionResult {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return ActionResult(false, "adjustVolume", "Audio service unavailable on device.")

            val stream = when {
                streamType?.contains("ring", ignoreCase = true) == true -> AudioManager.STREAM_RING
                streamType?.contains("alarm", ignoreCase = true) == true -> AudioManager.STREAM_ALARM
                streamType?.contains("notif", ignoreCase = true) == true -> AudioManager.STREAM_NOTIFICATION
                streamType?.contains("call", ignoreCase = true) == true ||
                        streamType?.contains("voice", ignoreCase = true) == true -> AudioManager.STREAM_VOICE_CALL
                else -> AudioManager.STREAM_MUSIC
            }

            val streamName = when (stream) {
                AudioManager.STREAM_RING -> "ringer"
                AudioManager.STREAM_ALARM -> "alarm"
                AudioManager.STREAM_NOTIFICATION -> "notification"
                AudioManager.STREAM_VOICE_CALL -> "call"
                else -> "media"
            }

            val maxVol = audioManager.getStreamMaxVolume(stream)

            if (levelPercent != null) {
                val clamped = levelPercent.coerceIn(0, 100)
                val target = (clamped * maxVol) / 100
                audioManager.setStreamVolume(stream, target, AudioManager.FLAG_SHOW_UI)
                return ActionResult(
                    true,
                    "adjustVolume",
                    "Set $streamName volume to $clamped%."
                )
            }

            val lowerDir = direction.trim().lowercase()
            when {
                lowerDir in listOf("up", "raise", "increase", "higher", "louder", "badhao") -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    val newVol = audioManager.getStreamVolume(stream)
                    val pct = if (maxVol > 0) (newVol * 100) / maxVol else 0
                    ActionResult(true, "adjustVolume", "Turned $streamName volume up to $pct%.")
                }
                lowerDir in listOf("down", "lower", "decrease", "softer", "kam", "ghatao") -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                    val newVol = audioManager.getStreamVolume(stream)
                    val pct = if (maxVol > 0) (newVol * 100) / maxVol else 0
                    ActionResult(true, "adjustVolume", "Turned $streamName volume down to $pct%.")
                }
                lowerDir in listOf("mute", "silent", "quiet", "shant") -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "adjustVolume", "Muted $streamName volume.")
                }
                lowerDir in listOf("unmute", "max", "full") -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "adjustVolume", "Unmuted $streamName volume.")
                }
                else -> {
                    audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                    ActionResult(true, "adjustVolume", "Adjusted $streamName volume.")
                }
            }
        } catch (e: Exception) {
            ActionResult(false, "adjustVolume", "Could not adjust volume: ${e.localizedMessage}")
        }
    }

    private val commonAppPackageMap = mapOf(
        "whatsapp" to "com.whatsapp",
        "whatsapp business" to "com.whatsapp.w4b",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "messenger" to "com.facebook.orca",
        "youtube" to "com.google.android.youtube",
        "spotify" to "com.spotify.music",
        "telegram" to "org.telegram.messenger",
        "snapchat" to "com.snapchat.android",
        "twitter" to "com.twitter.android",
        "x" to "com.twitter.android",
        "tiktok" to "com.zhiliaoapp.musically",
        "netflix" to "com.netflix.mediaclient",
        "amazon" to "com.amazon.mShop.android.shopping",
        "flipkart" to "com.flipkart.android",
        "paytm" to "net.one97.paytm",
        "phonepe" to "com.phonepe.app",
        "gpay" to "com.google.android.apps.nbu.paisa.user",
        "google pay" to "com.google.android.apps.nbu.paisa.user",
        "chrome" to "com.android.chrome",
        "gmail" to "com.google.android.gm",
        "google maps" to "com.google.android.apps.maps",
        "maps" to "com.google.android.apps.maps",
        "uber" to "com.ubercab",
        "swiggy" to "in.swiggy.android",
        "zomato" to "com.application.zomato",
        "truecaller" to "com.truecaller"
    )

    fun resolvePackageName(appIdentifier: String): String {
        val trimmed = appIdentifier.trim()
        // If it looks like a valid package name (contains at least one dot and no spaces)
        if (trimmed.contains(".") && !trimmed.contains(" ")) {
            return trimmed
        }
        val lower = trimmed.lowercase()
        // Check direct match in map first
        commonAppPackageMap[lower]?.let { return it }

        // Check if any key in map is contained as a word in the query
        commonAppPackageMap.entries.firstOrNull { lower.contains(it.key) }?.let { return it.value }

        // Strip command phrases using word boundaries so words like "whatsapp" are preserved
        val cleanWord = lower
            .replace(Regex("\\b(arushi|the app|from the play store|from play store|play store se|install karo|install|download karo|download|play store|store)\\b"), " ")
            .replace(Regex("\\bapp\\b"), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        return commonAppPackageMap[cleanWord]
            ?: commonAppPackageMap.entries.firstOrNull { cleanWord.contains(it.key) }?.value
            ?: if (cleanWord.isNotBlank()) "com.${cleanWord.replace(" ", "")}" else "com.whatsapp"
    }

    fun installApp(appIdentifier: String): ActionResult {
        val cleanInput = appIdentifier.trim()
        if (cleanInput.isBlank()) {
            return ActionResult(false, "installApp", "Please specify which app you want to install.")
        }

        val packageName = resolvePackageName(cleanInput)
        val playStoreMarketUri = Uri.parse("market://details?id=$packageName")
        val webFallbackUri = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")

        return try {
            val pm = context.packageManager
            // 1. Construct ACTION_VIEW Intent with Google Play Store URI scheme
            val playStoreIntent = Intent(Intent.ACTION_VIEW, playStoreMarketUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }

            // Target Play Store app if present on device
            if (pm.getLaunchIntentForPackage("com.android.vending") != null) {
                playStoreIntent.setPackage("com.android.vending")
            }

            if (playStoreIntent.resolveActivity(pm) != null) {
                context.startActivity(playStoreIntent)
                ActionResult(
                    true,
                    "installApp",
                    "Opening Google Play Store install page for '$cleanInput' ($packageName)."
                )
            } else {
                // Fallback to web browser Play Store details page
                val webIntent = Intent(Intent.ACTION_VIEW, webFallbackUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ActionResult(
                    true,
                    "installApp",
                    "Opening Play Store install page for '$cleanInput' ($packageName) in browser."
                )
            }
        } catch (e: Exception) {
            ActionResult(false, "installApp", "Could not open Play Store install page for '$cleanInput': ${e.localizedMessage}")
        }
    }

    fun playMusic(query: String, platform: String? = "youtube"): ActionResult {
        val cleanQuery = query.trim().ifBlank { "Top music hits" }
        val targetPlatform = platform?.lowercase()?.trim() ?: "youtube"

        return try {
            val pm = context.packageManager
            if (targetPlatform.contains("spotify")) {
                val spotifyUri = Uri.parse("spotify:search:${Uri.encode(cleanQuery)}")
                val spotifyIntent = Intent(Intent.ACTION_VIEW, spotifyUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (pm.getLaunchIntentForPackage("com.spotify.music") != null) {
                    spotifyIntent.setPackage("com.spotify.music")
                    context.startActivity(spotifyIntent)
                    return ActionResult(true, "playMusic", "Playing '$cleanQuery' on Spotify.")
                }
            }

            // Default YouTube playback / search
            val ytUrl = "https://www.youtube.com/results?search_query=${Uri.encode(cleanQuery)}"
            val ytIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (pm.getLaunchIntentForPackage("com.google.android.youtube") != null) {
                ytIntent.setPackage("com.google.android.youtube")
            }

            context.startActivity(ytIntent)
            ActionResult(true, "playMusic", "Playing '$cleanQuery' on YouTube.")
        } catch (e: Exception) {
            ActionResult(false, "playMusic", "Could not play '$cleanQuery': ${e.localizedMessage}")
        }
    }

    fun controlMedia(command: String, targetApp: String? = null): ActionResult {
        val cleanCommand = command.trim().lowercase()
        val cleanTarget = targetApp?.trim()?.lowercase()

        val targetPackage = when {
            cleanTarget == null || cleanTarget.isBlank() || cleanTarget == "media" -> null
            cleanTarget.contains("youtube") -> "com.google.android.youtube"
            cleanTarget.contains("spotify") -> "com.spotify.music"
            cleanTarget.contains(".") -> targetApp?.trim()
            else -> resolvePackageName(cleanTarget)
        }

        val (keyCode, actionLabel) = when {
            cleanCommand in listOf("play", "resume", "start", "chalao", "shuru", "gaana bajao", "unpause") ->
                Pair(KeyEvent.KEYCODE_MEDIA_PLAY, "Playing media")
            cleanCommand in listOf("pause", "hold", "roko", "ruk jao") ->
                Pair(KeyEvent.KEYCODE_MEDIA_PAUSE, "Paused media")
            cleanCommand in listOf("stop", "band karo") ->
                Pair(KeyEvent.KEYCODE_MEDIA_STOP, "Stopped media")
            cleanCommand in listOf("skip", "next", "aage", "next song", "next track", "agla gaana") ->
                Pair(KeyEvent.KEYCODE_MEDIA_NEXT, "Skipped to next track")
            cleanCommand in listOf("previous", "prev", "back", "pichla gaana", "pichhe") ->
                Pair(KeyEvent.KEYCODE_MEDIA_PREVIOUS, "Returned to previous track")
            else ->
                Pair(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Toggled media playback")
        }

        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)

            // 1. Dispatch through AudioManager
            audioManager?.dispatchMediaKeyEvent(eventDown)
            audioManager?.dispatchMediaKeyEvent(eventUp)

            // 2. Dispatch ACTION_MEDIA_BUTTON intent broadcast
            val downIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, eventDown)
                if (targetPackage != null) {
                    setPackage(targetPackage)
                }
            }
            context.sendOrderedBroadcast(downIntent, null)

            val upIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, eventUp)
                if (targetPackage != null) {
                    setPackage(targetPackage)
                }
            }
            context.sendOrderedBroadcast(upIntent, null)

            val targetDesc = if (targetPackage != null) {
                if (targetPackage.contains("youtube")) "on YouTube"
                else if (targetPackage.contains("spotify")) "on Spotify"
                else "on $targetPackage"
            } else "on active player"

            ActionResult(
                true,
                "controlMedia",
                "$actionLabel $targetDesc via media button intent."
            )
        } catch (e: Exception) {
            ActionResult(false, "controlMedia", "Could not dispatch media control: ${e.localizedMessage}")
        }
    }

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
