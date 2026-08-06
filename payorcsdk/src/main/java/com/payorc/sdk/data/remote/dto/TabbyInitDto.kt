package com.payorc.sdk.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TabbyInitRequestDto(
    val data: TabbyInitDataDto
)

@Serializable
data class TabbyInitDataDto(
    val action: String = "AUTH",
    @SerialName("class")
    val ecomClass: String = "ECOM",
    @SerialName("capture_method")
    val captureMethod: String = "MANUAL",
    val type: String = "TABBY",
    @SerialName("merchant_code")
    val merchantCode: String? = null,
    @SerialName("customer_details")
    val customerDetails: CustomerDetailsDto? = null,
    @SerialName("billing_details")
    val billingDetails: AddressDetailsDto? = null,
    @SerialName("order_details")
    val orderDetails: OrderDetailsDto? = null,
    @SerialName("shipping_details")
    val shippingDetails: ShippingDetailsDto? = null,
    val urls: Map<String, String>? = null,
    val parameters: List<Map<String, String>>? = null,
    @SerialName("custom_data")
    val customData: List<Map<String, String>>? = null
)

@Serializable
data class TabbyInitResponseDto(
    val data: TabbyInitResponseDataDto? = null,
    val message: String? = null,
    val status: String? = null,
    val code: String? = null
) {
    val isSuccess: Boolean get() = code == "00" || status?.lowercase() == "success"
}

@Serializable
data class TabbyInitResponseDataDto(
    @SerialName("order_id")
    val orderId: Long? = null,
    val mode: String? = null,
    @SerialName("merchant_code")
    val merchantCode: String? = null,
    val password: String? = null,
    @SerialName("extra_key")
    val extraKey: String? = null,
    @SerialName("tabby_api_keys")
    val tabbyApiKeys: String? = null,
    @SerialName("tabby_session_config")
    val tabbySessionConfig: String? = null
)
