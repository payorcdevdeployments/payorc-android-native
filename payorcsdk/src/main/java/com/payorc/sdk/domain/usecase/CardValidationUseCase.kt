package com.payorc.sdk.domain.usecase

import java.util.Calendar

data class CardValidationResult(
    val isValid: Boolean,
    val cardScheme: String? = null,
    val errorMessage: String? = null
)

class CardValidationUseCase(private val validator: PaymentFormValidator = PaymentFormValidator()) {

    fun execute(
        holderName: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        supportedSchemes: List<String> = emptyList()
    ): CardValidationResult {
        android.util.Log.d("PayOrcValidation", "Executing card validation")
        val nameResult = validator.validateCardHolder(holderName)
        if (!nameResult.isValid) {
            android.util.Log.w("PayOrcValidation", "Name invalid: ${nameResult.errorMessage}")
            return CardValidationResult(false, null, nameResult.errorMessage)
        }

        val cardResult = validator.validateCardNumber(cardNumber)
        if (!cardResult.isValid) {
            android.util.Log.w("PayOrcValidation", "Card number invalid: ${cardResult.errorMessage}")
            return CardValidationResult(false, null, cardResult.errorMessage)
        }

        val expiryResult = validator.validateExpiry(expiry)
        if (!expiryResult.isValid) {
            android.util.Log.w("PayOrcValidation", "Expiry invalid: ${expiryResult.errorMessage}")
            return CardValidationResult(false, null, expiryResult.errorMessage)
        }

        val cvvResult = validator.validateCvv(cvv)
        if (!cvvResult.isValid) {
            android.util.Log.w("PayOrcValidation", "CVV invalid: ${cvvResult.errorMessage}")
            return CardValidationResult(false, null, cvvResult.errorMessage)
        }

        val trimmedCard = cardNumber.filter { it.isDigit() }
        val scheme = detectCardScheme(trimmedCard) ?: "CARD"
        android.util.Log.d("PayOrcValidation", "Detected scheme: $scheme")

        // Align with Flutter: Relax enforcement for unknown schemes, but keep restriction if supportedSchemes is explicit
        if (scheme != "CARD" && supportedSchemes.isNotEmpty() && supportedSchemes.none { it.equals(scheme, ignoreCase = true) }) {
            android.util.Log.w("PayOrcValidation", "Scheme $scheme not supported by merchant. Supported: $supportedSchemes")
            return CardValidationResult(false, scheme, "Card scheme $scheme is not supported")
        }

        android.util.Log.d("PayOrcValidation", "Card validation successful")
        return CardValidationResult(true, scheme)
    }

    private fun detectCardScheme(digits: String): String? {
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
}
