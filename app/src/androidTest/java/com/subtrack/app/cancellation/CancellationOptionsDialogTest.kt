package com.subtrack.app.cancellation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.subtrack.core.domain.model.CancellationLinks
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.ui.components.CancellationOptionsDialog
import com.subtrack.core.ui.components.PendingSubscriptionActionDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun pendingRecoveryPopupExposesCompletedAndCancelActions() {
        var completed = false
        var cancelled = false
        compose.setContent {
            PendingSubscriptionActionDialog(
                pendingAction = PendingSubscriptionAction(
                    subscriptionId = 1L,
                    subscriptionName = "Netflix",
                    actionType = SubscriptionActionProgressType.UNSUBSCRIBE
                ),
                onCompleted = { completed = true },
                onCancel = { cancelled = true }
            )
        }
        compose.onNodeWithText("Unsubscribing from Netflix").assertIsDisplayed()
        compose.onNodeWithText("Completed").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle {
            assertTrue(completed)
            assertTrue(cancelled)
        }
    }

    @Test
    fun manifestRequestsNoInternetPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = packageInfo.requestedPermissions.orEmpty().toSet()
        assertFalse(Manifest.permission.INTERNET in requested)
        assertFalse("android.permission.USE_EXACT_ALARM" in requested)
    }
}
