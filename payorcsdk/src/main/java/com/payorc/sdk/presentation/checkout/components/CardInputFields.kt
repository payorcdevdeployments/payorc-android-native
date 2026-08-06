package com.payorc.sdk.presentation.checkout.components

import com.payorc.sdk.presentation.checkout.components.PayOrcText

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.payorc.sdk.ui.components.PayorcSdkUiConstants
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection

@Composable
fun CardNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val numberTextStyle = ltrNumberTextStyle()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        PayOrcTextField(
            value = value,
            onValueChange = { val digits = it.filter { char -> char.isDigit() }; if (digits.length <= 19) onValueChange(digits) },
            label = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.cardNumberLabel ?: "Card Number") },
            placeholder = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.cardNumberHint ?: "0000 0000 0000 0000") },
            trailingIcon = trailingIcon,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            visualTransformation = CardNumberVisualTransformation(),
            modifier = modifier.fillMaxWidth(),
            isError = isError,
            textStyle = numberTextStyle,
            supportingText = { if (errorMessage != null) PayOrcText(errorMessage, color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
        )
    }
}

@Composable
fun ExpiryField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val numberTextStyle = ltrNumberTextStyle()
    PayOrcTextField(
        value = value,
        onValueChange = { val digits = it.filter { char -> char.isDigit() }; if (digits.length <= 4) onValueChange(digits) },
        label = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.expiryMonthLabel ?: "Expiry (MM/YY)") },
        placeholder = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.expiryMonthHint ?: "MM/YY") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = ExpiryVisualTransformation(),
        modifier = modifier.fillMaxWidth(),
        isError = isError,
        textStyle = numberTextStyle,
        supportingText = { if (errorMessage != null) PayOrcText(errorMessage, color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
    )
}

@Composable
fun MonthField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val numberTextStyle = ltrNumberTextStyle()
    PayOrcTextField(
        value = value,
        onValueChange = { val digits = it.filter { char -> char.isDigit() }; if (digits.length <= 2) onValueChange(digits) },
        label = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.expiryMonthLabel ?: "MM") },
        placeholder = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.expiryMonthHint ?: "MM") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        isError = isError,
        singleLine = true,
        textStyle = numberTextStyle,
        supportingText = { if (errorMessage != null) PayOrcText(errorMessage, color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
    )
}

@Composable
fun YearField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val numberTextStyle = ltrNumberTextStyle()
    PayOrcTextField(
        value = value,
        onValueChange = { val digits = it.filter { char -> char.isDigit() }; if (digits.length <= 2) onValueChange(digits) },
        label = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.expiryYearLabel ?: "YY") },
        placeholder = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.expiryYearHint ?: "YY") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
        isError = isError,
        singleLine = true,
        textStyle = numberTextStyle,
        supportingText = { if (errorMessage != null) PayOrcText(errorMessage, color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
    )
}

@Composable
fun CVVField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    val numberTextStyle = ltrNumberTextStyle()
    PayOrcTextField(
        value = value,
        onValueChange = { val digits = it.filter { char -> char.isDigit() }; if (digits.length <= 4) onValueChange(digits) },
        label = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.cvvLabel ?: "CVV") },
        placeholder = { PayOrcText(PayorcSdkUiConstants.addCardFormConfig?.cvvHint ?: "123") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = PasswordVisualTransformation(),
        modifier = modifier.fillMaxWidth(),
        isError = isError,
        textStyle = numberTextStyle,
        supportingText = { if (errorMessage != null) PayOrcText(errorMessage, color = Color.Red, style = androidx.compose.material3.MaterialTheme.typography.labelSmall) }
    )
}

@Composable
private fun ltrNumberTextStyle() = LocalTextStyle.current.copy(
    textDirection = TextDirection.Ltr,
    textAlign = TextAlign.Left
)

@Composable
fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = Color(0xFFFFEBEE),
        shape = RoundedCornerShape(12.dp)
    ) {
        PayOrcText(
            text = message,
            color = Color(0xFFD32F2F),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun PayButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = PayorcSdkUiConstants.buttonBackgroundColor
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(PayorcSdkUiConstants.buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor,
            contentColor = PayorcSdkUiConstants.buttonForegroundColor,
            disabledContainerColor = PayorcSdkUiConstants.buttonDisabledBackgroundColor,
            disabledContentColor = PayorcSdkUiConstants.buttonDisabledForegroundColor
        ),
        shape = RoundedCornerShape(PayorcSdkUiConstants.buttonBorderRadius),
        border = if (PayorcSdkUiConstants.buttonSideBorderColor != Color.Transparent) 
            BorderStroke(1.dp, PayorcSdkUiConstants.buttonSideBorderColor) else null
    ) {
        PayOrcText(text = label, fontWeight = PayorcSdkUiConstants.buttonFontWeight)
    }
}

@Composable
fun LoadingOverlay(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PayorcSdkUiConstants.loadingIndicatorColor
                )
                PayOrcText(text = message)
            }
        }
    }
}

@Composable
fun CardSchemeBadge(scheme: String?, modifier: Modifier = Modifier) {
    if (scheme.isNullOrBlank()) return
    
    val normalized = scheme.uppercase().replace("_", "")
    
    val drawableRes = when {
        normalized.contains("VISA") -> com.payorc.sdk.R.drawable.ic_visa
        normalized.contains("MASTER") -> com.payorc.sdk.R.drawable.ic_mastercard
        normalized.contains("AMEX") || normalized.contains("AMERICAN") -> com.payorc.sdk.R.drawable.ic_amex
        normalized.contains("JCB") -> com.payorc.sdk.R.drawable.ic_jcb
        normalized.contains("DINERS") -> com.payorc.sdk.R.drawable.ic_diners
        normalized.contains("DISCOVER") -> com.payorc.sdk.R.drawable.ic_discover
        normalized.contains("MAESTRO") -> com.payorc.sdk.R.drawable.ic_maestro
        else -> null
    }

    if (drawableRes != null) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = drawableRes),
            contentDescription = scheme,
            modifier = modifier.size(width = 36.dp, height = 24.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        // Fallback for mada or unknown
        Surface(
            modifier = modifier.size(width = 36.dp, height = 24.dp),
            shape = RoundedCornerShape(4.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (normalized.contains("MADA")) {
                    PayOrcText(
                        text = "mada",
                        color = Color(0xFF2E7D32),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else if (normalized.contains("UNION")) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        PayOrcText("Union", color = Color(0xFFD32F2F), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        PayOrcText("Pay", color = Color(0xFF0D47A1), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    val fallbackText = when {
                        normalized.contains("DINERS") -> "DCI"
                        else -> normalized.take(3)
                    }
                    PayOrcText(
                        text = fallbackText,
                        color = Color.DarkGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

class CardNumberVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 19) text.text.substring(0..18) else text.text
        val out = buildString {
            for (i in trimmed.indices) {
                append(trimmed[i])
                if (i % 4 == 3 && i != trimmed.lastIndex) append(' ')
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, trimmed.length)
                if (clampedOffset == trimmed.length) return out.length
                val spacesBefore = clampedOffset / 4
                return (clampedOffset + spacesBefore).coerceAtMost(out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val transformedOffset = offset.coerceIn(0, out.length)
                val original = when {
                    transformedOffset <= 4 -> transformedOffset
                    transformedOffset <= 9 -> transformedOffset - 1
                    transformedOffset <= 14 -> transformedOffset - 2
                    transformedOffset <= 19 -> transformedOffset - 3
                    transformedOffset <= 24 -> transformedOffset - 4
                    else -> transformedOffset - 5
                }
                return original.coerceIn(0, trimmed.length)
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

class ExpiryVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 4) text.text.substring(0..3) else text.text
        val out = buildString {
            for (i in trimmed.indices) {
                append(trimmed[i])
                if (i == 1 && i != trimmed.lastIndex) append('/')
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clampedOffset = offset.coerceIn(0, trimmed.length)
                if (clampedOffset == trimmed.length) return out.length
                return if (clampedOffset <= 1) clampedOffset else clampedOffset + 1
            }

            override fun transformedToOriginal(offset: Int): Int {
                val transformedOffset = offset.coerceIn(0, out.length)
                return if (transformedOffset <= 2) transformedOffset else (transformedOffset - 1).coerceAtMost(trimmed.length)
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

fun detectCardScheme(cardNumber: String): String? {
    val digits = cardNumber.filter { it.isDigit() }
    if (digits.isEmpty()) return null

    val first = digits[0]
    val firstTwo = if (digits.length >= 2) digits.substring(0, 2) else ""
    val firstFour = if (digits.length >= 4) digits.substring(0, 4) else ""
    val firstSix = if (digits.length >= 6) digits.substring(0, 6).toIntOrNull() else null

    // Visa: starts with 4
    if (first == '4') return "VISA"

    // American Express: 34 or 37
    if (firstTwo == "34" || firstTwo == "37") return "AMEX"

    // Mastercard: 51-55, 2221-2720
    if (first == '5') {
        val two = firstTwo.toIntOrNull()
        if (two != null && two in 51..55) return "MASTERCARD"
    }
    if (firstFour.length == 4) {
        val n = firstFour.toIntOrNull()
        if (n != null && n in 2221..2720) return "MASTERCARD"
    }

    // Discover: 6011, 644-649, 65
    if (firstFour == "6011") return "DISCOVER"
    if (digits.length >= 3) {
        val n = digits.substring(0, 3).toIntOrNull()
        if (n != null && n in 644..649) return "DISCOVER"
    }
    if (firstTwo == "65") return "DISCOVER"

    // JCB: 3528-3589
    if (firstSix != null && firstSix in 3528..3589) return "JCB"

    // Diners Club: 36, 38, 300-305
    if (firstTwo == "36" || firstTwo == "38") return "DINERS"
    if (digits.length >= 3) {
        val n = digits.substring(0, 3).toIntOrNull()
        if (n != null && n in 300..305) return "DINERS"
    }

    // Maestro
    val maestroPrefixes = listOf("5018", "5020", "5038", "5893", "6304", "6759", "6761", "6762", "6763")
    if (firstFour.length == 4 && maestroPrefixes.contains(firstFour)) return "MAESTRO"

    return null
}
