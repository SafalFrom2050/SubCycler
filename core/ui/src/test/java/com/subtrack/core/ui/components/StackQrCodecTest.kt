package com.subtrack.core.ui.components

import com.subtrack.core.domain.backup.SubscriptionBackupCodec
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StackQrCodecTest {

    @Test
    fun shortTransferLinkEncodesAndDecodesCleanlyThroughOfflineQrMatrix() {
        val subs = listOf(
            Subscription(name = "Netflix", cost = 17.99, currency = "USD", startDate = LocalDate.of(2026, 11, 27)),
            Subscription(name = "Disney+", cost = 15.99, currency = "USD", startDate = LocalDate.of(2026, 11, 27)),
            Subscription(
                name = "Spotify",
                cost = 11.99,
                currency = "USD",
                category = SubscriptionCategory.MUSIC,
                strategy = RotationStrategy.CONTINUOUS,
                startDate = LocalDate.of(2026, 11, 27)
            )
        )
        val link = SubscriptionBackupCodec.toTransferLink(subs, includePersonalHistory = false)
        val matrix = generateQrMatrix(link)
        assertNotNull(matrix)
        matrix!!
        assertTrue(matrix.width >= 21)

        // Rasterize the QrMatrix into a pure-JVM ARGB pixel buffer with quiet zone and decode back
        val quiet = 4
        val scale = 4
        val imgW = (matrix.width + quiet * 2) * scale
        val imgH = (matrix.height + quiet * 2) * scale
        val pixels = IntArray(imgW * imgH) { 0xFFFFFFFF.toInt() }
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (!matrix.get(x, y)) continue
                val startX = (x + quiet) * scale
                val startY = (y + quiet) * scale
                for (dy in 0 until scale) {
                    for (dx in 0 until scale) {
                        pixels[(startY + dy) * imgW + (startX + dx)] = 0xFF02151B.toInt()
                    }
                }
            }
        }

        val decodedQrText = decodeQrFromArgb(pixels, imgW, imgH)
        assertEquals(link, decodedQrText)

        val decodedSubs = SubscriptionBackupCodec.decodeTransferPayload(decodedQrText!!).getOrThrow()
        assertEquals(subs, decodedSubs)
    }
}
