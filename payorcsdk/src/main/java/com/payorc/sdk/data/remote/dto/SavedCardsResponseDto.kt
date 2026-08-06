package com.payorc.sdk.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SavedCardsResponseDto(
    val data: List<SavedCardDto> = emptyList(),
    val message: String? = null,
    val status: String? = null,
    val code: String? = null
)

@Serializable
data class SavedCardDto(
    @SerialName("payment_token") val paymentToken: String,
    val name: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("card_network") val cardNetwork: String? = null,
    @SerialName("last_4_digit") val last4Digit: String? = null,
    @SerialName("mask_card_number") val maskCardNumber: String? = null,
    @SerialName("card_scheme") val cardScheme: String? = null,
    @SerialName("card_type") val cardType: String? = null,
    val expiry: String? = null,
    @SerialName("m_customer_id") val mCustomerId: String? = null,
    @SerialName("card_brand") val cardBrand: String? = null,
    @SerialName("psp_info") val pspInfo: String? = null
)
