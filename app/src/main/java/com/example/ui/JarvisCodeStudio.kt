package com.example.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyanJarvis
import com.example.ui.theme.TertiaryJarvis
import com.example.ui.theme.TextSlate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisCodeStudioModal(
    onDismiss: () -> Unit,
    onGenerateCodePrompt: (String) -> Unit,
    currentGeneratedCode: String = "",
    isAiGenerating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Web App Sandbox, 1: Code Editor, 2: Self-Modifier
    var userPromptText by remember { mutableStateOf("") }
    var codeContent by remember { mutableStateOf(currentGeneratedCode.ifBlank { DEFAULT_STARTER_WEB_APP }) }

    LaunchedEffect(currentGeneratedCode) {
        if (currentGeneratedCode.isNotBlank()) {
            codeContent = extractOrCleanHtml(currentGeneratedCode)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CyanJarvis.copy(alpha = 0.15f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "AI Code Studio",
                            tint = CyanJarvis,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "J.A.R.V.I.S. AI CODE STUDIO",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CyanJarvis,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Web App Generator & Live In-App Sandbox Engine",
                            fontSize = 11.sp,
                            color = TextSlate
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Code Studio",
                        tint = TextSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = CyanJarvis,
                divider = {}
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Web Sandbox", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Source Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Prompt Input Field for Code Generation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, CyanJarvis.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = userPromptText,
                        onValueChange = { userPromptText = it },
                        placeholder = { Text("Ask J.A.R.V.I.S. to build a web app, game, or tool...", fontSize = 12.sp, color = TextSlate) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanJarvis,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Button(
                        onClick = {
                            if (userPromptText.isNotBlank()) {
                                val prompt = "Write a complete single-file standalone HTML/CSS/JS interactive web app for: $userPromptText. Wrap in <html> tag with modern sleek dark UI."
                                onGenerateCodePrompt(prompt)
                                userPromptText = ""
                            }
                        },
                        enabled = !isAiGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanJarvis),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (isAiGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Build App", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Build", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Preset Prompts Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Tic-Tac-Toe Game" to "Build a sleek dark modern Tic-Tac-Toe web game with score tracker",
                    "Live Stopwatch" to "Build a neon digital stopwatch web app with lap timing",
                    "Calculators" to "Build a sleek scientific calculator web app with glassmorphism UI",
                    "Particle Simulator" to "Build an interactive HTML5 canvas particle force simulation web app"
                ).forEach { (label, prompt) ->
                    AssistChip(
                        onClick = { onGenerateCodePrompt(prompt) },
                        label = { Text(label, fontSize = 10.sp, color = CyanJarvis) },
                        border = BorderStroke(1.dp, CyanJarvis.copy(alpha = 0.3f)),
                        colors = AssistChipDefaults.assistChipColors(containerColor = CyanJarvis.copy(alpha = 0.08f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content Area based on Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(2.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Web App Sandbox Preview in WebView
                        LiveWebSandboxView(htmlContent = codeContent)
                    }
                    1 -> {
                        // Source Code Display & Copy Tools
                        SourceCodeEditorView(
                            code = codeContent,
                            onCodeChange = { codeContent = it },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("JARVIS Web Code", codeContent)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveWebSandboxView(htmlContent: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowContentAccess = true
                settings.allowFileAccess = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadDataWithBaseURL("https://jarvis-app.local/", htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://jarvis-app.local/", htmlContent, "text/html", "UTF-8", null)
        },
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A), RoundedCornerShape(14.dp))
    )
}

@Composable
fun SourceCodeEditorView(
    code: String,
    onCodeChange: (String) -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STANDALONE HTML / JS / CSS SOURCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyanJarvis,
                fontFamily = FontFamily.Monospace
            )

            IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Code", tint = CyanJarvis, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = code,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxSize(),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF38BDF8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanJarvis.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}

private fun extractOrCleanHtml(raw: String): String {
    if (raw.contains("<!DOCTYPE html>", ignoreCase = true) || raw.contains("<html", ignoreCase = true)) {
        val startIndex = raw.indexOf("<!DOCTYPE html>", ignoreCase = true).let { if (it != -1) it else raw.indexOf("<html", ignoreCase = true) }
        val endIndex = raw.lastIndexOf("</html>", ignoreCase = true)
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return raw.substring(startIndex, endIndex + 7)
        }
    }
    // Clean markdown codeblocks
    return raw.replace("```html", "").replace("```xml", "").replace("```", "").trim()
}

private val DEFAULT_STARTER_WEB_APP = """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>J.A.R.V.I.S. Interactive AI Web Studio</title>
    <style>
        body {
            background-color: #0F172A;
            color: #F8FAFC;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            height: 100vh;
            margin: 0;
            text-align: center;
            padding: 20px;
        }
        .card {
            background: rgba(30, 41, 59, 0.8);
            border: 1px solid #06B6D4;
            box-shadow: 0 0 25px rgba(6, 182, 212, 0.35);
            border-radius: 16px;
            padding: 24px;
            max-width: 380px;
            width: 100%;
        }
        h2 { color: #38BDF8; margin-top: 0; font-size: 20px; }
        p { color: #94A3B8; font-size: 13px; line-height: 1.5; }
        .btn {
            background: linear-gradient(135deg, #06B6D4, #3B82F6);
            color: white;
            border: none;
            padding: 10px 20px;
            font-weight: bold;
            border-radius: 8px;
            cursor: pointer;
            margin-top: 12px;
            transition: all 0.2s ease;
        }
        .btn:hover { transform: scale(1.05); }
        .counter { font-size: 36px; font-weight: bold; color: #10B981; margin: 10px 0; }
    </style>
</head>
<body>
    <div class="card">
        <h2>⚡ J.A.R.V.I.S. AI Web Sandbox</h2>
        <p>Type any web app prompt above to generate and execute interactive web applications live inside this sandbox!</p>
        <div class="counter" id="countVal">0</div>
        <button class="btn" onclick="increment()">Click Interactive Pulse</button>
    </div>
    <script>
        let count = 0;
        function increment() {
            count++;
            document.getElementById('countVal').innerText = count;
        }
    </script>
</body>
</html>
""".trimIndent()
