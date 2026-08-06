package com.payorc.sdk.domain.model

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)
