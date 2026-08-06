package com.payorc.sdk.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FetchSavedCardsRequestDto(
    @SerialName("m_customer_id") val customerId: String,
    @SerialName("order_id") val orderId: String
)
