package com.payorc.sdk.domain.model

data class SavedCard(
    val paymentToken: String,
    val maskCardNumber: String,
    val cardScheme: String,
    val cardType: String?,
    val expiry: String?,
    val customerId: String?,
    val cardBrand: String?,
    val pspInfo: String?
)
