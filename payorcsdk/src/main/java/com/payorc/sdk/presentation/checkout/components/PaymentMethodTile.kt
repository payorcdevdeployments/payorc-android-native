package com.payorc.sdk.presentation.checkout.components

import com.payorc.sdk.presentation.checkout.components.PayOrcText

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.payorc.sdk.domain.model.PaymentMethod
import com.payorc.sdk.ui.components.PayorcSdkUiConstants

/**
 * Returns black or white, whichever has higher contrast against [background],
 * using the standard relative luminance formula.
 */
private fun contrastColorFor(background: Color): Color {
    val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue
    return if (luminance > 0.5f) Color.Black else Color.White
}

@Composable
fun PaymentMethodTile(
    method: PaymentMethod,
    isSelected: Boolean,
    accentColor: Color = PayorcSdkUiConstants.accentColor,
    brandColor: Color = PayorcSdkUiConstants.brandColor,
    onClick: () -> Unit
) {
    // Unselected  → background is accentColor,   border = accentColor,   text = white (unless accent is white)
    // Selected    → background is transparent,    border = contrast(brand), text = contrast(brand)
    val isAccentNearWhite = accentColor.red > 0.9f && accentColor.green > 0.9f && accentColor.blue > 0.9f
    val backgroundColor = if (isSelected) Color.Transparent else accentColor
    val contentColor = if (isSelected) contrastColorFor(brandColor) else if (isAccentNearWhite) contrastColorFor(accentColor) else Color.White
    val tileBorderColor = if (isSelected) contrastColorFor(brandColor) else accentColor

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(PayorcSdkUiConstants.buttonHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(PayorcSdkUiConstants.buttonBorderRadius),
        color = backgroundColor,
        border = BorderStroke(1.dp, tileBorderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Icon
            if (method.type == "CARD") {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                val iconRes = when (method.type) {
                    "GOOGLE_PAY" -> com.payorc.sdk.R.drawable.ic_google_pay
                    "APPLE_PAY" -> com.payorc.sdk.R.drawable.ic_apple_pay
                    "TABBY" -> com.payorc.sdk.R.drawable.ic_tabby
                    "SADAD" -> com.payorc.sdk.R.drawable.ic_sadad
                    "URPAY" -> com.payorc.sdk.R.drawable.ic_urpay
                    "SAVED_CARD", "STORED_CARD" -> com.payorc.sdk.R.drawable.ic_payment
                    else -> com.payorc.sdk.R.drawable.ic_payment
                }
                
                val iconTint = if (method.type == "STORED_CARD" || method.type == "SAVED_CARD") contentColor else Color.Unspecified

                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Center/Left Text
            PayOrcText(
                text = getMethodDisplayName(method.type),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                modifier = Modifier.weight(1f)
            )
            
            // Right Logos for Card or Arrow for others
            if (method.type == "CARD") {
                CardSchemesDisplay(
                    schemes = listOf("VISA", "MASTERCARD", "AMEX", "JCB")
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun getMethodDisplayName(type: String): String = when (type) {
    "GOOGLE_PAY" -> "Google Pay"
    "APPLE_PAY" -> "Apple Pay"
    "CARD" -> "Pay with Card"
    "STORED_CARD" -> "Use Stored Card"
    "TABBY" -> "Tabby"
    "SADAD" -> "Sadad"
    "URPAY" -> "Urpay"
    else -> type.lowercase().replaceFirstChar { it.uppercase() }.replace("_", " ")
}
