package com.subtrack.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CurrencyScopeNotice(
    currencyCode: String,
    excludedCurrencyCount: Int,
    modifier: Modifier = Modifier
) {
    val exclusionPhrase = if (excludedCurrencyCount == 1) {
        "1 service in another currency isn't included"
    } else {
        "$excludedCurrencyCount services in other currencies aren't included"
    }
    val amberPalette = ExpressivePalettes.softAmber()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = amberPalette.container
    ) {
        Text(
            text = "Totals show $currencyCode only. $exclusionPhrase because SubCycler doesn't guess exchange rates.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = amberPalette.content
        )
    }
}
