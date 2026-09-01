package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StudyHistoryModal(
    studySessions: List<StudySessionEntity>,
    onDismiss: () -> Unit,
    onSaveSummary: (Int, String) -> Unit,
    onEditSummary: (Int, String) -> Unit,
    onDiscardSummary: (Int) -> Unit,
    onDeleteSession: (Int) -> Unit
) {
    var selectedSessionForDetail by remember { mutableStateOf<StudySessionEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Study History",
                            tint = CyanJarvis,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STUDY HISTORY",
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

                if (studySessions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No study sessions logged yet.\nStart a Study Session to record notes and generate AI summaries.",
                            color = TextSlate,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(studySessions) { session ->
                            StudySessionHistoryItem(
                                session = session,
                                onClick = { selectedSessionForDetail = session },
                                onDelete = { onDeleteSession(session.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedSessionForDetail != null) {
        val currentSession = studySessions.find { it.id == selectedSessionForDetail?.id } ?: selectedSessionForDetail
        StudySummaryModal(
            session = currentSession,
            isGenerating = false,
            onDismiss = { selectedSessionForDetail = null },
            onSaveSummary = onSaveSummary,
            onEditSummary = onEditSummary,
            onDiscardSummary = onDiscardSummary
        )
    }
}

@Composable
fun StudySessionHistoryItem(
    session: StudySessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateString = remember(session.startTime) {
        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(session.startTime))
        } catch (e: Exception) {
            ""
        }
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (session.isSummarySaved) GreenSecure.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.topic,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateString,
                    color = TextSlate,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                if (session.aiSummary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (session.isSummarySaved) GreenSecure.copy(alpha = 0.15f) else CyanJarvis.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (session.isSummarySaved) "Saved AI Summary" else "Generated AI Summary",
                            color = if (session.isSummarySaved) GreenSecure else CyanJarvis,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "No Summary",
                        color = TextSlate,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
