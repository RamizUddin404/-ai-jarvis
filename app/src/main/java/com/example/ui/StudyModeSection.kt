package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.StudySessionEntity
import com.example.ui.theme.CyanJarvis
import com.example.ui.theme.GreenSecure
import com.example.ui.theme.TextSlate

@Composable
fun StudyModeModal(
    activeSession: StudySessionEntity?,
    onDismiss: () -> Unit,
    onStartSession: (String) -> Unit,
    onRecordNote: (String) -> Unit,
    onEndSession: () -> Unit
) {
    var topicInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Study Mode",
                            tint = CyanJarvis,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STUDY MODE",
                            color = CyanJarvis,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSlate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (activeSession == null) {
                    Text(
                        text = "Start a new active study session to log key concepts, facts, or questions encountered. AI will generate a summary upon completion.",
                        color = TextSlate,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        label = { Text("Study Topic (e.g. Quantum Physics, Android Dev)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onStartSession(topicInput)
                            topicInput = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanJarvis)
                    ) {
                        Text("Start Study Session", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CyanJarvis.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanJarvis.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "ACTIVE SESSION",
                                color = CyanJarvis,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = activeSession.topic,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Session Notes / Logged Questions:",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (activeSession.sessionNotes.isNotBlank()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Text(
                                text = activeSession.sessionNotes,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            placeholder = { Text("Log concept, fact, or question...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (noteInput.isNotBlank()) {
                                    onRecordNote(noteInput)
                                    noteInput = ""
                                }
                            },
                            modifier = Modifier
                                .background(CyanJarvis, RoundedCornerShape(12.dp))
                                .size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NoteAdd,
                                contentDescription = "Add Note",
                                tint = MaterialTheme.colorScheme.background
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onEndSession,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "End Session", tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("End Session & Generate Summary", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StudySummaryModal(
    session: StudySessionEntity?,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onSaveSummary: (Int, String) -> Unit,
    onEditSummary: (Int, String) -> Unit,
    onDiscardSummary: (Int) -> Unit
) {
    if (session == null && !isGenerating) return

    var isEditing by remember { mutableStateOf(false) }
    var summaryEditText by remember(session?.aiSummary) { mutableStateOf(session?.aiSummary ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Summary",
                            tint = CyanJarvis,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI SESSION SUMMARY",
                            color = CyanJarvis,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSlate)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isGenerating) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = CyanJarvis)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Generating AI Summary...",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Highlighting key concepts, facts, and questions.",
                            color = TextSlate,
                            fontSize = 12.sp
                        )
                    }
                } else if (session != null) {
                    Text(
                        text = "Topic: ${session.topic}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditing) {
                        OutlinedTextField(
                            value = summaryEditText,
                            onValueChange = { summaryEditText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 150.dp, max = 300.dp),
                            label = { Text("Edit Summary") }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isEditing = false }) {
                                Text("Cancel", color = TextSlate)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onEditSummary(session.id, summaryEditText)
                                    isEditing = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyanJarvis)
                            ) {
                                Text("Done", color = MaterialTheme.colorScheme.background)
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CyanJarvis.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = session.aiSummary.ifBlank { "No summary generated." },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { onDiscardSummary(session.id) }
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Discard", tint = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Discard", color = Color(0xFFEF4444))
                            }

                            Row {
                                OutlinedButton(
                                    onClick = {
                                        summaryEditText = session.aiSummary
                                        isEditing = true
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = CyanJarvis)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", color = CyanJarvis)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        onSaveSummary(session.id, session.aiSummary)
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenSecure)
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
