package com.payorc.sdk.presentation.checkout.components

import com.payorc.sdk.presentation.checkout.components.PayOrcText

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Reusable component to display "Powered by PayOrc" with logo or text.
 */
@Composable
fun PoweredByPayOrc(
    logoUrl: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PayOrcText(
            text = "Powered by",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = "PayOrc Logo",
                modifier = Modifier
                    .height(24.dp)
                    .fillMaxWidth(0.3f),
                contentScale = ContentScale.Fit
            )
        } else {
            PayOrcText(
                text = "PayOrc",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF00A651), // PayOrc Brand Green
                fontWeight = FontWeight.Bold
            )
        }
    }
}
