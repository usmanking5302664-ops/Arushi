package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActionResult
import com.example.data.AssistantState
import com.example.data.ChatMessage
import com.example.data.MessageSender
import com.example.services.AndroidActionManager
import com.example.services.AndroidBridge
import com.example.services.AudioPlayer
import com.example.services.GeminiLiveService
import com.example.services.VoiceInputManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArushiViewModel(application: Application) : AndroidViewModel(application) {

    val actionManager = AndroidActionManager(application.applicationContext)
    val audioPlayer = AudioPlayer(application.applicationContext)
    val geminiService = GeminiLiveService(actionManager)

    // JavaScript bridge for Android actions
    val androidBridge = AndroidBridge(actionManager) { action, success, msg ->
        _activeAction.value = ActionResult(success, action, msg)
        addMessage(
            ChatMessage(
                sender = MessageSender.SYSTEM,
                text = "Native Bridge: $action -> $msg",
                actionResult = ActionResult(success, action, msg)
            )
        )
    }

    private val _assistantState = MutableStateFlow(AssistantState.IDLE)
    val assistantState: StateFlow<AssistantState> = _assistantState.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _activeAction = MutableStateFlow<ActionResult?>(null)
    val activeAction: StateFlow<ActionResult?> = _activeAction.asStateFlow()

    private val _partialSpeech = MutableStateFlow("")
    val partialSpeech: StateFlow<String> = _partialSpeech.asStateFlow()

    private val _currentLanguage = MutableStateFlow("Hindi & English (Multi-Language)")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _hasMicPermission = MutableStateFlow(false)
    val hasMicPermission: StateFlow<Boolean> = _hasMicPermission.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(false)
    val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

    private val _hasCallPermission = MutableStateFlow(false)
    val hasCallPermission: StateFlow<Boolean> = _hasCallPermission.asStateFlow()

    private val voiceInputManager = VoiceInputManager(
        context = application.applicationContext,
        onResult = { text ->
            _partialSpeech.value = ""
            processUserInput(text)
        },
        onPartialResult = { partial ->
            _partialSpeech.value = partial
        },
        onListeningStateChanged = { listening ->
            if (listening) {
                _assistantState.value = AssistantState.LISTENING
            } else if (_assistantState.value == AssistantState.LISTENING) {
                _assistantState.value = AssistantState.IDLE
            }
        },
        onError = { errMsg ->
            _partialSpeech.value = ""
            if (_assistantState.value == AssistantState.LISTENING) {
                _assistantState.value = AssistantState.IDLE
            }
        }
    )

    init {
        audioPlayer.onPlaybackStateChanged = { playing ->
            if (playing) {
                _assistantState.value = AssistantState.SPEAKING
            } else {
                if (_assistantState.value == AssistantState.SPEAKING) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        }

        // Welcome message
        addMessage(
            ChatMessage(
                sender = MessageSender.ARUSHI,
                text = "Hello! I am Arushi. You can speak to me in Hindi, English, Hinglish, or regional languages. Say 'WhatsApp kholo', 'Call Mom', or 'Call Usman'!"
            )
        )
    }

    fun updatePermissions(mic: Boolean, contacts: Boolean, call: Boolean) {
        _hasMicPermission.value = mic
        _hasContactsPermission.value = contacts
        _hasCallPermission.value = call
    }

    /**
     * User interruption: User interrupts Arushi while speaking.
     * Current audio stops immediately, state switches to listening or idle.
     */
    fun interruptArushi() {
        audioPlayer.stop()
        _assistantState.value = AssistantState.IDLE
    }

    fun toggleListening() {
        // If speaking, interrupt first
        if (_assistantState.value == AssistantState.SPEAKING) {
            interruptArushi()
            startListening()
            return
        }

        if (_assistantState.value == AssistantState.LISTENING) {
            voiceInputManager.stopListening()
            _assistantState.value = AssistantState.IDLE
        } else {
            startListening()
        }
    }

    fun startListening() {
        interruptArushi()
        _partialSpeech.value = ""
        voiceInputManager.startListening()
    }

    fun stopListening() {
        voiceInputManager.stopListening()
        _assistantState.value = AssistantState.IDLE
    }

    fun processUserInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        // If assistant was speaking, interrupt immediately
        interruptArushi()

        // Append user message
        addMessage(ChatMessage(sender = MessageSender.USER, text = trimmed))

        viewModelScope.launch {
            _assistantState.value = AssistantState.THINKING

            val result = geminiService.sendMessage(trimmed) { actionName ->
                _assistantState.value = AssistantState.EXECUTING_ACTION
            }

            // Update active action if one was executed
            if (result.executedAction != null) {
                _activeAction.value = result.executedAction
            }

            // Update detected language tag
            updateLanguageDisplay(geminiService.currentLanguage)

            // Add Arushi message
            addMessage(
                ChatMessage(
                    sender = MessageSender.ARUSHI,
                    text = result.text,
                    language = geminiService.currentLanguage,
                    actionResult = result.executedAction
                )
            )

            // Voice response: play audio from Gemini or fallback TTS
            if (!result.audioBase64.isNullOrBlank() && result.audioMimeType != null) {
                _assistantState.value = AssistantState.SPEAKING
                val played = audioPlayer.playAudioData(result.audioMimeType, result.audioBase64) {
                    _assistantState.value = AssistantState.IDLE
                }
                if (!played) {
                    audioPlayer.speakFallbackText(result.text, geminiService.currentLanguage) {
                        _assistantState.value = AssistantState.IDLE
                    }
                }
            } else {
                _assistantState.value = AssistantState.SPEAKING
                audioPlayer.speakFallbackText(result.text, geminiService.currentLanguage) {
                    _assistantState.value = AssistantState.IDLE
                }
            }
        }
    }

    private fun updateLanguageDisplay(langCode: String) {
        _currentLanguage.value = when (langCode.lowercase()) {
            "hi-in", "hi" -> "Hindi (हिंदी)"
            "mr-in", "mr" -> "Marathi (मराठी)"
            "gu-in", "gu" -> "Gujarati (ગુજરાતી)"
            "ta-in", "ta" -> "Tamil (தமிழ்)"
            "te-in", "te" -> "Telugu (తెలుగు)"
            "bn-in", "bn" -> "Bengali (বাংলা)"
            "pa-in", "pa" -> "Punjabi (ਪੰਜਾਬੀ)"
            "ur-in", "ur" -> "Urdu (اردو)"
            else -> "English & Multilingual"
        }
    }

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }

    fun dismissActionBanner() {
        _activeAction.value = null
    }

    fun clearChat() {
        interruptArushi()
        geminiService.resetConversation()
        _messages.value = emptyList()
        _activeAction.value = null
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        voiceInputManager.stopListening()
    }
}
