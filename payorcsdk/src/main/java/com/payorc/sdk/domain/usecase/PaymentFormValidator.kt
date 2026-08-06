package com.payorc.sdk.domain.usecase

import android.util.Patterns
import com.payorc.sdk.domain.model.ValidationResult
import com.payorc.sdk.ui.components.PayorcSdkUiConstants
import java.util.Calendar

class PaymentFormValidator {

    fun validateName(name: String, isRequired: Boolean = true): ValidationResult {
        if (isRequired && name.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("customer_name", "required")
            )
        }
        return ValidationResult(isValid = true)
    }

    fun validateEmail(email: String, isRequired: Boolean = true): ValidationResult {
        if (isRequired && email.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("email", "required")
            )
        }
        if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("email", "invalid")
            )
        }
        return ValidationResult(isValid = true)
    }

    fun validateMobile(mobile: String, isRequired: Boolean = true): ValidationResult {
        if (isRequired && mobile.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("mobile", "required")
            )
        }
        return ValidationResult(isValid = true)
    }

    fun validateCardHolder(name: String, isRequired: Boolean = true): ValidationResult {
        if (isRequired && name.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("name", "required")
            )
        }
        return ValidationResult(isValid = true)
    }

    fun validateCardNumber(number: String): ValidationResult {
        val trimmedCard = number.filter { it.isDigit() }
        
        if (trimmedCard.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("number", "required")
            )
        }
        android.util.Log.d("PayOrcValidator", "Validating card number: ${number.take(6)}... (length: ${trimmedCard.length})")
        if (trimmedCard.length !in 13..19) {
            android.util.Log.w("PayOrcValidator", "Invalid length: ${trimmedCard.length}")
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("number", "invalid")
            )
        }
        android.util.Log.d("PayOrcValidator", "Card number length valid")
        // Removed Luhn check to match Flutter SDK behavior
        return ValidationResult(isValid = true)
    }

    /**
     * Validates a combined expiry string (MMYY or MM/YY).
     * If the month is provided but the year is missing, returns "year required".
     */
    fun validateExpiry(expiry: String): ValidationResult {
        val trimmedExpiry = expiry.filter { it.isDigit() }
        
        if (trimmedExpiry.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("expiry_month", "required")
            )
        }
        
        if (trimmedExpiry.length < 2) {
             return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("expiry_month", "invalid")
            )
        }

        if (trimmedExpiry.length < 4) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("expiry_year", "required")
            )
        }

        val month = trimmedExpiry.substring(0, 2).toIntOrNull()
        val year = trimmedExpiry.substring(2, 4).toIntOrNull()

        if (month == null || month !in 1..12) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("expiry_month", "invalid")
            )
        }

        if (year == null) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("expiry_year", "invalid")
            )
        }

        if (isExpired(month, year)) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("card", "invalid", "Card has expired")
            )
        }

        return ValidationResult(isValid = true)
    }

    fun validateExpiryMonth(month: String, isRequired: Boolean = true): ValidationResult {
        val trimmed = month.filter { it.isDigit() }
        if (isRequired && trimmed.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("expiry_month", "required")
            )
        }
        if (trimmed.isNotBlank()) {
            val m = trimmed.toIntOrNull()
            if (m == null || m !in 1..12) {
                return ValidationResult(
                    isValid = false,
                    errorMessage = PayorcSdkUiConstants.getValidationError("expiry_month", "invalid")
                )
            }
        }
        return ValidationResult(isValid = true)
    }

    fun validateExpiryYear(year: String, isRequired: Boolean = true): ValidationResult {
        val trimmed = year.filter { it.isDigit() }
        if (isRequired && trimmed.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("expiry_year", "required")
            )
        }
        if (trimmed.isNotBlank() && trimmed.length < 2) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("expiry_year", "invalid")
            )
        }
        return ValidationResult(isValid = true)
    }

    fun validateCvv(cvv: String): ValidationResult {
        val trimmedCvv = cvv.filter { it.isDigit() }
        
        if (trimmedCvv.isBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("cvv", "required")
            )
        }
        if (trimmedCvv.length !in 3..4) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("cvv", "invalid")
            )
        }
        return ValidationResult(isValid = true)
    }

    fun validateBillingAddress(
        line1: String,
        city: String,
        country: String,
        isRequired: Boolean
    ): ValidationResult {
        if (isRequired && (line1.isBlank() || city.isBlank() || country.isBlank())) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("billing_address", "required")
            )
        }
        return ValidationResult(isValid = true)
    }

    fun validateShippingAddress(
        line1: String,
        city: String,
        country: String,
        isRequired: Boolean
    ): ValidationResult {
        if (isRequired && (line1.isBlank() || city.isBlank() || country.isBlank())) {
            return ValidationResult(
                isValid = false,
                errorMessage = PayorcSdkUiConstants.getValidationError("shipping_address", "required")
            )
        }
        return ValidationResult(isValid = true)
    }

    // --- Helpers ---

    fun isExpired(month: Int, yearTwoDigits: Int): Boolean {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR) % 100
        val currentMonth = calendar.get(Calendar.MONTH) + 1
        return yearTwoDigits < currentYear || (yearTwoDigits == currentYear && month < currentMonth)
    }

    fun isValidLuhn(number: String): Boolean {
        return number.reversed().map { it.digitToIntOrNull() ?: 0 }
            .mapIndexed { index, digit -> if (index % 2 == 1) digit * 2 else digit }
            .map { if (it > 9) it - 9 else it }
            .sum() % 10 == 0
    }
}
