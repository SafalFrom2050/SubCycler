package com.subtrack.core.data.preferences

import com.subtrack.core.domain.testing.FakeUserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesTest {

    private val repository = FakeUserPreferencesRepository()

    @Test
    fun testDefaultPreferences() = runTest {
        val prefs = repository.userPreferences.first()
        assertEquals("USD", prefs.currency)
        assertEquals(3, prefs.notificationLeadDays)
        assertEquals(true, prefs.isAutoCancelApprovalRequired)
        assertEquals(false, prefs.isOnboardingCompleted)
    }

    @Test
    fun testUpdatePreferences() = runTest {
        repository.setCurrency("EUR")
        repository.setNotificationLeadDays(5)
        repository.setAutoCancelApprovalRequired(false)
        repository.setOnboardingCompleted(true)

        val updated = repository.userPreferences.first()
        assertEquals("EUR", updated.currency)
        assertEquals(5, updated.notificationLeadDays)
        assertEquals(false, updated.isAutoCancelApprovalRequired)
        assertEquals(true, updated.isOnboardingCompleted)
    }
}
