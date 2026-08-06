package com.payorc.sdk.presentation.checkout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CardSchemesDisplay(
    modifier: Modifier = Modifier,
    schemes: List<String> = listOf("VISA", "MASTERCARD", "AMEX", "JCB")
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        schemes.forEach { scheme ->
            CardSchemeBadge(scheme = scheme)
        }
    }
}
