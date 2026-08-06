package com.payorc.sdk.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class TabbyConfirmRequestDto(
    @SerialName("order_id")
    val orderId: String,
    @SerialName("tabby_payment_id")
    val tabbyPaymentId: String
)

@Serializable
data class TabbyConfirmResponseDto(
    val data: JsonElement? = null,
    val message: String? = null,
    val status: String? = null,
    val code: String? = null
) {
    val isSuccess: Boolean get() = code == "00" || status?.lowercase() == "success"
}
