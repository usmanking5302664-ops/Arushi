package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickPrompt(
    val label: String,
    val icon: ImageVector? = null,
    val tag: String
)

@Composable
fun QuickPromptsRow(
    onPromptSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val prompts = listOf(
        QuickPrompt("WhatsApp kholo", Icons.AutoMirrored.Filled.Message, "whatsapp_kholo"),
        QuickPrompt("Open WhatsApp", Icons.AutoMirrored.Filled.Message, "open_whatsapp"),
        QuickPrompt("Call Usman", Icons.Default.Call, "call_usman"),
        QuickPrompt("Mummy ko call karo", Icons.Default.Call, "call_mummy"),
        QuickPrompt("Call 9876543210", Icons.Default.Call, "call_number"),
        QuickPrompt("Hindi mein baat karo", Icons.Default.Language, "lang_hindi"),
        QuickPrompt("Talk to me in English", Icons.Default.Language, "lang_english"),
        QuickPrompt("Hinglish mein baat karo", Icons.Default.Language, "lang_hinglish"),
        QuickPrompt("Open YouTube", Icons.Default.SmartButton, "open_youtube"),
        QuickPrompt("Open Settings", Icons.Default.SmartButton, "open_settings"),
        QuickPrompt("Hello Assistant", Icons.Default.Language, "hello_assistant")
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("quick_prompts_row")
    ) {
        prompts.forEach { prompt ->
            AssistChip(
                onClick = { onPromptSelected(prompt.label) },
                label = {
                    Text(
                        text = prompt.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)
                    )
                },
                leadingIcon = prompt.icon?.let {
                    {
                        Icon(
                            imageVector = it,
                            contentDescription = prompt.label,
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.testTag("prompt_${prompt.tag}")
            )
        }
    }
}
