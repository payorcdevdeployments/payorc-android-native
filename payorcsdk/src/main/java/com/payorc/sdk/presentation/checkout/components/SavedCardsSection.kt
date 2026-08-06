package com.payorc.sdk.presentation.checkout.components

import com.payorc.sdk.presentation.checkout.components.PayOrcText

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payorc.sdk.domain.model.SavedCard

@Composable
fun SavedCardsSection(
    savedCards: List<SavedCard>,
    selectedToken: String?,
    onCardSelected: (SavedCard) -> Unit,
    onAddNewCard: () -> Unit,
    buttonColor: Color = Color(0xFF0066CC)
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        savedCards.forEach { card ->
            SavedCardTile(
                card = card,
                isSelected = card.paymentToken == selectedToken,
                onClick = { onCardSelected(card) },
                buttonColor = buttonColor
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddNewCard() },
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = buttonColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                PayOrcText(
                    text = "Add New Card",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun SavedCardTile(
    card: SavedCard,
    isSelected: Boolean,
    onClick: () -> Unit,
    buttonColor: Color
) {
    val backgroundColor = if (isSelected) buttonColor.copy(alpha = 0.08f) else buttonColor
    val contentColor = if (isSelected) buttonColor else Color.White
    val borderColor = if (isSelected) buttonColor else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, borderColor) else null
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardSchemeBadge(scheme = card.cardScheme, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                PayOrcText(
                    text = card.maskCardNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                if (!card.expiry.isNullOrBlank()) {
                    PayOrcText(
                        text = "Expires ${card.expiry}",
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = com.payorc.sdk.R.drawable.ic_payment),
                    contentDescription = "Unselected",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
