package com.payorc.sdk.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Shared DTOs for common request/response structures used across multiple endpoints.
 * These are reused by Card, Tabby, and other payment operations to reduce duplication.
 */

@Serializable
data class CustomerDetailsDto(
    @SerialName("m_customer_id")
    val mCustomerId: String = "",
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val code: String = ""
)

@Serializable
data class AddressDetailsDto(
    @SerialName("address_line1")
    val addressLine1: String = "",
    @SerialName("address_line2")
    val addressLine2: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val pin: String = ""
)

@Serializable
data class ShippingDetailsDto(
    @SerialName("shipping_name")
    val shippingName: String = "",
    @SerialName("shipping_email")
    val shippingEmail: String = "",
    @SerialName("shipping_code")
    val shippingCode: String = "",
    @SerialName("shipping_mobile")
    val shippingMobile: String = "",
    @SerialName("address_line1")
    val addressLine1: String = "",
    @SerialName("address_line2")
    val addressLine2: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val pin: String = "",
    @SerialName("shipping_currency")
    val shippingCurrency: String = "",
    @SerialName("shipping_amount")
    val shippingAmount: String = ""
) {
    companion object {
        fun fromAddress(address: AddressDetailsDto, shipping: ShippingDetailsDto?): ShippingDetailsDto {
            return ShippingDetailsDto(
                shippingName = shipping?.shippingName ?: "",
                shippingEmail = shipping?.shippingEmail ?: "",
                shippingCode = shipping?.shippingCode ?: "",
                shippingMobile = shipping?.shippingMobile ?: "",
                addressLine1 = address.addressLine1,
                addressLine2 = address.addressLine2,
                city = address.city,
                province = address.province,
                country = address.country,
                pin = address.pin,
                shippingCurrency = shipping?.shippingCurrency ?: "",
                shippingAmount = shipping?.shippingAmount ?: ""
            )
        }
    }
}

@Serializable
data class OrderDetailsDto(
    @SerialName("m_order_id")
    val mOrderId: String = "",
    val amount: String = "",
    val currency: String = "",
    @SerialName("convenience_fee")
    val convenienceFee: String = "",
    val description: String = ""
)
