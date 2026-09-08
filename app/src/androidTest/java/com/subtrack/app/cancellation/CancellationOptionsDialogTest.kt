package com.subtrack.app.cancellation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.subtrack.core.domain.model.CancellationLinks
import com.subtrack.core.ui.components.CancellationOptionsDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

/** Compile locally; execute later on a device/emulator. Never contacts or cancels a real account. */
class CancellationOptionsDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun choosingServiceRequestsOnlyItsLinkAndKeepsDialogOpen() {
        val opened = mutableListOf<String>()
        var dismissed = false
        compose.setContent {
            CancellationOptionsDialog("Netflix", onDismiss = { dismissed = true }, onOpenLink = {
                opened += it.url
                true
            })
        }
        compose.onNodeWithText("Netflix membership").performScrollTo().performClick()
        compose.onNodeWithText("Cancel subscription").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(listOf(CancellationLinks.forService("Netflix")!!.url), opened)
            assertFalse(dismissed)
        }
    }

    @Test
    fun unknownServiceStillOffersStoreBillingAndDirectBillingGuidance() {
        compose.setContent { CancellationOptionsDialog("My gym", {}, { true }) }
        compose.onNodeWithText("No service link is available", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Netflix membership").assertDoesNotExist()
        compose.onNodeWithText("Google Play subscriptions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Apple billing instructions").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun launchFailureOffersCopyableUrlAndRetryClearsTheError() {
        var canOpen = false
        compose.setContent { CancellationOptionsDialog("Netflix", {}, { canOpen }) }
        compose.onNodeWithText("Netflix membership").performScrollTo().performClick()
        compose.onNodeWithText("Copy link").performScrollTo().performClick()
        compose.onNodeWithText("Link copied").assertIsDisplayed()
        compose.onNodeWithText(CancellationLinks.forService("Netflix")!!.url).assertIsDisplayed()
        compose.runOnIdle { canOpen = true }
        compose.onNodeWithText("Netflix membership").performScrollTo().performClick()
        compose.onNodeWithText("Couldn't open the link", substring = true).assertDoesNotExist()
    }
}
