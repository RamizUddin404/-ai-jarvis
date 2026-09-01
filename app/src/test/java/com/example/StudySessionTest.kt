package com.example

import com.example.data.StudySessionEntity
import org.junit.Assert.*
import org.junit.Test

class StudySessionTest {

    @Test
    fun testStudySessionCreationAndProperties() {
        val startTime = System.currentTimeMillis()
        val session = StudySessionEntity(
            topic = "Quantum Computing",
            startTime = startTime,
            sessionNotes = "Superposition, Qubits, Entanglement"
        )

        assertEquals("Quantum Computing", session.topic)
        assertEquals(startTime, session.startTime)
        assertEquals(0L, session.endTime)
        assertEquals("Superposition, Qubits, Entanglement", session.sessionNotes)
        assertEquals("", session.aiSummary)
        assertFalse(session.isSummarySaved)
    }

    @Test
    fun testStudySessionSummarySavingAndEditing() {
        val session = StudySessionEntity(
            id = 1,
            topic = "Machine Learning",
            startTime = System.currentTimeMillis(),
            aiSummary = "Initial Summary: Neural networks and gradient descent."
        )

        val editedSession = session.copy(
            aiSummary = "Updated Summary: Deep Learning, Neural Networks, Backpropagation."
        )

        assertEquals("Updated Summary: Deep Learning, Neural Networks, Backpropagation.", editedSession.aiSummary)

        val savedSession = editedSession.copy(isSummarySaved = true)
        assertTrue(savedSession.isSummarySaved)

        val discardedSummarySession = savedSession.copy(aiSummary = "", isSummarySaved = false)
        assertEquals("", discardedSummarySession.aiSummary)
        assertFalse(discardedSummarySession.isSummarySaved)
    }
}
