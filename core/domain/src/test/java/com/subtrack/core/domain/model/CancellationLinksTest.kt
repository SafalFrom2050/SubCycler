package com.subtrack.core.domain.model

import java.net.URI
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class CancellationLinksTest {
    @Test
    fun everyOnboardingPresetHasAnOfficialDestination() {
        PresetLibrary.presets.forEach {
            assertNotNull("Missing cancellation route for ${it.name}", CancellationLinks.forService(it.name))
        }
    }

    @Test
    fun aliasesIgnoreCaseAndWhitespaceEvenWithTurkishDeviceLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals(CancellationLinks.forService("Disney+"), CancellationLinks.forService("  DISNEY   PLUS  "))
            assertEquals(CancellationLinks.forService("Max (HBO)"), CancellationLinks.forService("HBO Max"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun customNamesDoNotBecomeUrlsOrMatchPartOfAnotherServiceName() {
        listOf("", "Netflix alternative", "https://example.com", "javascript:alert(1)", "My gym").forEach {
            assertNull(CancellationLinks.forService(it))
            assertEquals(listOf(CancellationLinks.googlePlay, CancellationLinks.apple), CancellationLinks.destinationsFor(it))
        }
    }

    @Test
    fun billingAlternativesRemainAvailableWithoutDuplicateAppleInstructions() {
        val netflixOptions = CancellationLinks.destinationsFor("Netflix")
        assertEquals(CancellationLinks.forService("Netflix"), netflixOptions.first())
        assertTrue(netflixOptions.containsAll(listOf(CancellationLinks.googlePlay, CancellationLinks.apple)))
        assertEquals(2, CancellationLinks.destinationsFor("Apple Music").size)
    }

    @Test
    fun catalogUsesOnlyHttpsOnExplicitOfficialHostsWithoutEmbeddedCredentials() {
        val officialHosts = setOf(
            "www.netflix.com", "www.disneyplus.com", "www.hbomax.com", "www.youtube.com",
            "www.spotify.com", "www.primevideo.com", "help.openai.com", "support.claude.com",
            "one.google.com", "www.notion.com", "account.microsoft.com", "www.playstation.com",
            "support.apple.com", "play.google.com",
            "secure.hulu.com", "www.crunchyroll.com", "ec.nintendo.com"
        )
        PresetLibrary.presets.flatMap { CancellationLinks.destinationsFor(it.name) }.forEach {
            val uri = URI(it.url)
            assertEquals("https", uri.scheme)
            assertTrue("Unexpected host: ${uri.host}", uri.host in officialHosts)
            assertNull(uri.userInfo)
            assertEquals(-1, uri.port)
        }
    }
}
