package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.OpenRouterPresets
import com.example.ui.theme.CyanJarvis
import com.example.ui.theme.GreenSecure
import com.example.ui.theme.SecondaryJarvis
import com.example.ui.theme.TertiaryJarvis
import com.example.ui.theme.TextSlate

@Composable
fun ApiKeySetupBanner(
    onSetupClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(CyanJarvis.copy(alpha = 0.8f), SecondaryJarvis.copy(alpha = 0.5f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onSetupClick() },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CyanJarvis.copy(alpha = 0.15f), CircleShape)
                    .border(1.dp, CyanJarvis.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API Key",
                    tint = CyanJarvis,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OPENROUTER API KEY NEEDED",
                    color = CyanJarvis,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Add your OpenRouter key manually in Settings to enable AI voice and chat responses.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Button(
                onClick = onSetupClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyanJarvis),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Add Key",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun OpenRouterApiConfigSection(
    currentApiKey: String,
    currentModel: String,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onTestConnection: ((key: String, model: String, callback: (Boolean, String) -> Unit) -> Unit)? = null
) {
    var apiKeyText by remember(currentApiKey) { mutableStateOf(currentApiKey) }
    var selectedModel by remember(currentModel) { mutableStateOf(currentModel) }
    var isKeyVisible by remember { mutableStateOf(false) }
    var isCustomModelSelected by remember(currentModel) {
        mutableStateOf(OpenRouterPresets.popularModels.none { it.id == currentModel })
    }
    var customModelText by remember(currentModel) {
        mutableStateOf(if (OpenRouterPresets.popularModels.none { it.id == currentModel }) currentModel else "")
    }

    val clipboardManager = LocalClipboardManager.current
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var saveFeedback by remember { mutableStateOf(false) }

    val isKeyConfigured = apiKeyText.isNotBlank() || 
        (BuildConfig.OPENROUTER_API_KEY.isNotBlank() && BuildConfig.OPENROUTER_API_KEY != "MY_OPENROUTER_API_KEY")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Section Header with Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(CyanJarvis.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, CyanJarvis.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "API Key",
                        tint = CyanJarvis,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "OPENROUTER API KEY",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Enter key manually — no coding or rebuilds required",
                        color = TextSlate,
                        fontSize = 11.sp
                    )
                }
            }

            // Status Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isKeyConfigured) GreenSecure.copy(alpha = 0.15f)
                        else Color(0xFFFFB84D).copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (isKeyConfigured) GreenSecure.copy(alpha = 0.5f)
                        else Color(0xFFFFB84D).copy(alpha = 0.5f),
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isKeyConfigured) "● CONFIGURED" else "● NOT SET",
                    color = if (isKeyConfigured) GreenSecure else Color(0xFFFFB84D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // API Key Input Field
        OutlinedTextField(
            value = apiKeyText,
            onValueChange = {
                apiKeyText = it
                onApiKeyChange(it)
                testResult = null
                saveFeedback = true
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("OpenRouter API Key (sk-or-v1-...)") },
            placeholder = { Text("sk-or-v1-xxxxxxxxxxxxxxxxxxxx", color = TextSlate.copy(alpha = 0.6f)) },
            visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isKeyConfigured) CyanJarvis else TextSlate,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (apiKeyText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                apiKeyText = ""
                                onApiKeyChange("")
                                testResult = null
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSlate,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text
                            if (!clip.isNullOrBlank()) {
                                apiKeyText = clip.trim()
                                onApiKeyChange(clip.trim())
                                testResult = null
                                saveFeedback = true
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste from Clipboard",
                            tint = CyanJarvis,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { isKeyVisible = !isKeyVisible },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isKeyVisible) "Hide Key" else "Show Key",
                            tint = TextSlate,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanJarvis,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick action row & instructions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Get your key at openrouter.ai/keys",
                color = TextSlate,
                fontSize = 11.sp
            )

            AnimatedVisibility(
                visible = saveFeedback,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = GreenSecure,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Saved automatically",
                        color = GreenSecure,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // AI Model Selection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = CyanJarvis,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "SELECT AI MODEL",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Model Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OpenRouterPresets.popularModels.forEach { model ->
                val isSelected = selectedModel == model.id && !isCustomModelSelected
                Surface(
                    onClick = {
                        selectedModel = model.id
                        isCustomModelSelected = false
                        onModelChange(model.id)
                        testResult = null
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CyanJarvis.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                    border = borderStyle(isSelected)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = model.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CyanJarvis else MaterialTheme.colorScheme.onBackground
                            )
                            if (model.badge.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (model.badge == "FREE") GreenSecure.copy(alpha = 0.2f)
                                            else CyanJarvis.copy(alpha = 0.2f)
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = model.badge,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (model.badge == "FREE") GreenSecure else CyanJarvis
                                    )
                                }
                            }
                        }
                        Text(
                            text = model.provider,
                            fontSize = 10.sp,
                            color = TextSlate
                        )
                    }
                }
            }

            // Custom Model option chip
            Surface(
                onClick = {
                    isCustomModelSelected = true
                    if (customModelText.isNotBlank()) {
                        selectedModel = customModelText
                        onModelChange(customModelText)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                color = if (isCustomModelSelected) CyanJarvis.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                border = borderStyle(isCustomModelSelected)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Custom Model...",
                        fontSize = 12.sp,
                        fontWeight = if (isCustomModelSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCustomModelSelected) CyanJarvis else MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Enter any ID",
                        fontSize = 10.sp,
                        color = TextSlate
                    )
                }
            }
        }

        // Custom Model Text input if custom is selected
        if (isCustomModelSelected) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = customModelText,
                onValueChange = {
                    customModelText = it
                    selectedModel = it
                    onModelChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. meta-llama/llama-3.3-70b-instruct", fontSize = 12.sp, color = TextSlate) },
                label = { Text("Custom Model ID", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanJarvis,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Test Connection Button & Result
        if (onTestConnection != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        val keyToTest = when {
                            apiKeyText.isNotBlank() -> apiKeyText.trim()
                            BuildConfig.OPENROUTER_API_KEY.isNotBlank() && BuildConfig.OPENROUTER_API_KEY != "MY_OPENROUTER_API_KEY" -> BuildConfig.OPENROUTER_API_KEY.trim()
                            else -> ""
                        }
                        if (keyToTest.isEmpty()) {
                            testResult = Pair(false, "Please enter an API key above before testing.")
                            return@OutlinedButton
                        }
                        isTesting = true
                        testResult = null
                        onTestConnection(keyToTest, selectedModel) { success, message ->
                            isTesting = false
                            testResult = Pair(success, message)
                        }
                    },
                    enabled = !isTesting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyanJarvis
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = !isTesting).copy(
                        brush = Brush.horizontalGradient(listOf(CyanJarvis, TertiaryJarvis))
                    )
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = CyanJarvis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying Connection...", fontSize = 12.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test API Connection", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text(
                    text = "Selected: ${selectedModel.substringAfterLast("/")}",
                    color = TextSlate,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Test Result Banner
            testResult?.let { (success, message) ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (success) GreenSecure.copy(alpha = 0.12f) else Color(0xFFFF5555).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (success) GreenSecure.copy(alpha = 0.4f) else Color(0xFFFF5555).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (success) GreenSecure else Color(0xFFFF5555),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = message,
                            color = if (success) GreenSecure else Color(0xFFFF8888),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun borderStyle(isSelected: Boolean): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        color = if (isSelected) CyanJarvis else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    )
}
