package com.payorc.sdk.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddCardRequestDto(
    val data: AddCardRequestDataDto
)

@Serializable
data class AddCardRequestDataDto(
    val action: String = "AUTH_REVERSAL",
    @SerialName("class") val requestClass: String = "ECOM",
    @SerialName("capture_method") val captureMethod: String = "MANUAL",
    val type: String = "CARD",
    @SerialName("customer_details") val customerDetails: CustomerDetailsDto,
    @SerialName("billing_details") val billingDetails: AddressDetailsDto,
    @SerialName("order_details") val orderDetails: OrderDetailsDto,
    @SerialName("card_details") val cardDetails: AddCardCardDetailsDto,
    @SerialName("shipping_details") val shippingDetails: ShippingDetailsDto,
    val urls: Map<String, String>? = null,
    val parameters: List<Map<String, String>> = emptyList(),
    @SerialName("custom_data") val customData: List<Map<String, String>> = emptyList()
)

@Serializable
data class AddCardCardDetailsDto(
    @SerialName("payment_token") val paymentToken: String = "",
    @SerialName("card_holder_name") val cardHolderName: String,
    @SerialName("card_number") val cardNumber: String,
    val cvv: String,
    val expiry: String
)
