package com.subtrack.app.cancellation

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import com.subtrack.core.domain.model.CancellationLinks
import com.subtrack.core.ui.components.openCancellationDestination
import org.junit.Assert.*
import org.junit.Test

class CancellationLinkLauncherTest {
    @Test
    fun opensBrowsableHttpsWithoutPinningBrowserOrRequiringInstalledProviderApp() {
        var launched: Intent? = null
        val context = object : ContextWrapper(null) {
            override fun startActivity(intent: Intent) { launched = intent }
        }
        assertTrue(openCancellationDestination(context, CancellationLinks.googlePlay))
        assertEquals(Intent.ACTION_VIEW, launched!!.action)
        assertEquals(CancellationLinks.googlePlay.url, launched!!.dataString)
        assertTrue(launched!!.hasCategory(Intent.CATEGORY_BROWSABLE))
        assertNull(launched!!.component)
        assertNull(launched!!.`package`)
    }

    @Test
    fun missingOrBlockedBrowserReturnsFailureInsteadOfCrashing() {
        listOf(ActivityNotFoundException(), SecurityException()).forEach { failure ->
            val context = object : ContextWrapper(null) {
                override fun startActivity(intent: Intent) { throw failure }
            }
            assertFalse(openCancellationDestination(context, CancellationLinks.googlePlay))
        }
    }

    @Test
    fun rejectsNonHttpsDestinationsBeforeLaunching() {
        val context = object : ContextWrapper(null) {
            override fun startActivity(intent: Intent) { fail("Unsafe destination was launched") }
        }
        listOf("http://example.com", "javascript:alert(1)", "intent://example", "https:///missing-host").forEach {
            assertFalse(openCancellationDestination(context, CancellationLinks.apple.copy(url = it)))
        }
    }
}
