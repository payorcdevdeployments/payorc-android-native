package com.payorc.sdk.domain.usecase

/**
 * Represents the result of a field or form validation.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
) {
    companion object {
        fun success() = ValidationResult(isValid = true)
        fun failure(message: String) = ValidationResult(isValid = false, errorMessage = message)
    }
}
