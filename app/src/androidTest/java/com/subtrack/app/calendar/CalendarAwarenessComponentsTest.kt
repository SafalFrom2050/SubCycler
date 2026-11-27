package com.subtrack.app.calendar

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CalendarSuggestionIdentity
import com.subtrack.core.domain.model.CalendarSuggestionType
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.ui.components.CalendarSelectionSheet
import com.subtrack.core.ui.components.CalendarSettingsSheet
import com.subtrack.core.ui.components.CalendarSuggestionCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class CalendarAwarenessComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun manifestRequestsReadOnlyCalendarAccess() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = packageInfo.requestedPermissions.orEmpty().toSet()

        assertTrue(Manifest.permission.READ_CALENDAR in requested)
        assertTrue(Manifest.permission.WRITE_CALENDAR !in requested)
    }

    @Test
    fun settingsIsOffByDefaultAndEnableIsExplicit() {
        var requested = false
        compose.setContent {
            CalendarSettingsSheet(
                enabled = false,
                selectedCalendarCount = 0,
                accessIssue = null,
                notificationsAvailable = true,
                onEnabledChange = { requested = it },
                onOpenCalendars = {},
                onDismiss = {}
            )
        }

        compose.onNodeWithText("Notifications & Calendar").performClick()
        compose.onNodeWithText("Calendar-aware suggestions").assertIsDisplayed()
        compose.onNodeWithText("Use selected device calendars", substring = true).assertIsDisplayed()
        compose.onAllNodes(isToggleable())[0].assertIsOff().performClick()
        compose.runOnIdle { assertTrue(requested) }
    }

    @Test
    fun selectorStartsUncheckedAndShowsAccountContext() {
        var confirmed: Set<Long>? = null
        compose.setContent {
            CalendarSelectionSheet(
                calendars = listOf(
                    DeviceCalendar(7, "Personal", "person@example.com", "Google"),
                    DeviceCalendar(9, "Personal", "work@example.com", "Exchange")
                ),
                initiallySelectedIds = emptySet(),
                onConfirm = { confirmed = it },
                onDismiss = {}
            )
        }

        compose.onNodeWithText("person@example.com · Google").assertIsDisplayed()
        compose.onNodeWithText("work@example.com · Exchange").assertIsDisplayed()
        compose.onNodeWithText("Use selected").assertIsNotEnabled()
        compose.onAllNodes(isToggleable())[0].assertIsOff().performClick()
        compose.onNodeWithText("Use selected").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(setOf(7L), confirmed) }
    }

    @Test
    fun suggestionExposesReviewAndDismissWithoutMutatingSubscriptionState() {
        var reviewed = false
        var dismissed = false
        val suggestion = CalendarAwareSuggestion(
            identity = CalendarSuggestionIdentity(1, "7:10:100", CalendarSuggestionType.TRAVEL_REVIEW_CANCELLATION),
            subscriptionName = "Netflix",
            decisionDate = LocalDate.of(2026, 9, 15),
            occurrenceEndDateExclusive = LocalDate.of(2026, 9, 17),
            confidence = 100
        )
        compose.setContent {
            CalendarSuggestionCard(
                suggestion = suggestion,
                onPrimaryAction = { reviewed = true },
                onDismiss = { dismissed = true }
            )
        }

        compose.onNodeWithText("Review cancellation").performClick()
        compose.onNodeWithText("Dismiss").performClick()
        compose.runOnIdle {
            assertTrue(reviewed)
            assertTrue(dismissed)
        }
    }

    @Test
    fun changingCurrencyInSettingsShowsManualConversionWarningBeforeApplying() {
        var appliedCurrency: String? = null
        compose.setContent {
            CalendarSettingsSheet(
                enabled = false,
                selectedCalendarCount = 0,
                accessIssue = null,
                notificationsAvailable = true,
                onEnabledChange = {},
                onOpenCalendars = {},
                onDismiss = {},
                currencyCode = "USD",
                hasExistingSubscriptions = true,
                onCurrencySelected = { appliedCurrency = it }
            )
        }

        compose.onNodeWithText("App currency").performScrollTo().performClick()
        compose.onNodeWithText("Choose currency").assertIsDisplayed()
        compose.onNodeWithText("Search code or currency").performTextInput("JPY")
        compose.onNodeWithText("Japanese Yen").performClick()
        compose.onNodeWithText("Switch app currency to JPY?").assertIsDisplayed()
        compose.onNodeWithText("does not convert currencies automatically", substring = true).assertIsDisplayed()
        compose.runOnIdle { assertEquals(null, appliedCurrency) }

        compose.onNodeWithText("Switch to JPY").performClick()
        compose.runOnIdle { assertEquals("JPY", appliedCurrency) }
    }
}
