package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.AssistantState
import com.example.ui.components.ActionStatusBanner
import com.example.ui.components.BridgeDebugSheet
import com.example.ui.components.ConversationList
import com.example.ui.components.PulsingOrb
import com.example.ui.components.QuickPromptsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArushiScreen(
    viewModel: ArushiViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val state by viewModel.assistantState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val activeAction by viewModel.activeAction.collectAsState()
    val partialSpeech by viewModel.partialSpeech.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val hasMicPermission by viewModel.hasMicPermission.collectAsState()
    val hasContactsPermission by viewModel.hasContactsPermission.collectAsState()
    val hasCallPermission by viewModel.hasCallPermission.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showBridgeSheet by remember { mutableStateOf(false) }

    // Permissions launcher
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        viewModel.updatePermissions(
            mic = perms[Manifest.permission.RECORD_AUDIO] == true,
            contacts = perms[Manifest.permission.READ_CONTACTS] == true,
            call = perms[Manifest.permission.CALL_PHONE] == true
        )
    }

    LaunchedEffect(Unit) {
        val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val contacts = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val call = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        viewModel.updatePermissions(mic, contacts, call)

        if (!mic || !contacts || !call) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    when (state) {
                                        AssistantState.SPEAKING -> Color(0xFF818CF8)
                                        AssistantState.LISTENING -> Color(0xFF10B981)
                                        AssistantState.THINKING, AssistantState.EXECUTING_ACTION -> Color(0xFFF59E0B)
                                        AssistantState.IDLE -> Color(0xFF94A3B8)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Arushi",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "AI Voice",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentLanguage,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showBridgeSheet = true },
                        modifier = Modifier.testTag("bridge_info_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Native Action Bridge Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear Conversation",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Action Banner (shows when an action like WhatsApp, Call, etc. was executed)
            ActionStatusBanner(
                action = activeAction,
                onDismiss = { viewModel.dismissActionBanner() }
            )

            // Visualizer Hero Orb & Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                PulsingOrb(
                    state = state,
                    onClick = {
                        if (state == AssistantState.SPEAKING) {
                            viewModel.interruptArushi()
                        } else {
                            viewModel.toggleListening()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (state) {
                        AssistantState.LISTENING -> "Listening... (tap orb to stop)"
                        AssistantState.THINKING -> "Understanding request..."
                        AssistantState.EXECUTING_ACTION -> "Executing device action..."
                        AssistantState.SPEAKING -> "Speaking... (tap orb to interrupt)"
                        AssistantState.IDLE -> "Tap orb or mic to speak with Arushi"
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = when (state) {
                            AssistantState.LISTENING -> Color(0xFF10B981)
                            AssistantState.SPEAKING -> MaterialTheme.colorScheme.primary
                            AssistantState.THINKING, AssistantState.EXECUTING_ACTION -> Color(0xFFF59E0B)
                            AssistantState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
            }

            // Dialogue / Conversation list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                ConversationList(
                    messages = messages,
                    partialSpeech = partialSpeech,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Quick prompt chips (for test cases like "WhatsApp kholo", "Call Usman", etc.)
            QuickPromptsRow(
                onPromptSelected = { prompt ->
                    viewModel.processUserInput(prompt)
                }
            )

            // Bottom Input Controls (Mic + Text)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = {
                        Text(
                            text = "Ask Arushi or type command...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank()) {
                                viewModel.processUserInput(textInput)
                                textInput = ""
                                keyboardController?.hide()
                            }
                        }
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_input_field")
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (textInput.isNotBlank()) {
                    IconButton(
                        onClick = {
                            viewModel.processUserInput(textInput)
                            textInput = ""
                            keyboardController?.hide()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    FloatingActionButton(
                        onClick = {
                            if (state == AssistantState.SPEAKING) {
                                viewModel.interruptArushi()
                            } else {
                                viewModel.toggleListening()
                            }
                        },
                        shape = CircleShape,
                        containerColor = if (state == AssistantState.LISTENING) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("voice_mic_button")
                    ) {
                        Icon(
                            imageVector = when (state) {
                                AssistantState.LISTENING -> Icons.Default.Stop
                                AssistantState.SPEAKING -> Icons.Default.MicOff
                                else -> Icons.Default.Mic
                            },
                            contentDescription = if (state == AssistantState.LISTENING) "Stop Listening" else "Start Listening",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Bridge details modal sheet
    BridgeDebugSheet(
        isOpen = showBridgeSheet,
        actionManager = viewModel.actionManager,
        hasMicPermission = hasMicPermission,
        hasContactsPermission = hasContactsPermission,
        hasCallPermission = hasCallPermission,
        onRequestPermissions = {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS,
                    Manifest.permission.CALL_PHONE
                )
            )
        },
        onDismiss = { showBridgeSheet = false }
    )
}
