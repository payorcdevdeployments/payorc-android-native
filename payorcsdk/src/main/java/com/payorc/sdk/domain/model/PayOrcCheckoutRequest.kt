package com.payorc.sdk.domain.model

import java.math.BigDecimal

data class PayOrcCheckoutRequest(
    val paymentToken: String = "",
    val orderDetails: List<OrderDetails> = emptyList(),
    val customerDetails: CustomerDetails? = null,
    val billingDetails: BillingDetails? = null,
    val shippingDetails: ShippingDetails? = null,
    val urls: PayOrcUrls? = null,
    val action: String = "AUTH"
) {
    // Helper properties for legacy compatibility or easier internal access
    val orderId: String get() = orderDetails.firstOrNull()?.mOrderId ?: ""
    val amount: BigDecimal get() = try { BigDecimal(orderDetails.firstOrNull()?.amount ?: "0") } catch(e: Exception) { BigDecimal.ZERO }
    val currency: String get() = orderDetails.firstOrNull()?.currency ?: ""
    val customerId: String get() = customerDetails?.mCustomerId ?: ""
    val customerName: String get() = customerDetails?.name ?: ""
    val customerEmail: String get() = customerDetails?.email ?: ""
    val customerMobile: String get() = customerDetails?.mobile ?: ""
    val customerMobileCode: String get() = customerDetails?.code ?: ""
    val description: String get() = orderDetails.firstOrNull()?.description ?: ""
    
    // Internal address mapping for existing components
    val billingAddress: PayOrcAddress get() = billingDetails?.let {
        PayOrcAddress(it.addressLine1, it.addressLine2, it.city, it.province, it.country, it.pin)
    } ?: PayOrcAddress()
    
    val shippingAddress: PayOrcAddress get() = shippingDetails?.let {
        PayOrcAddress(it.addressLine1, it.addressLine2, it.city, it.province, it.country, it.pin)
    } ?: PayOrcAddress()
}

data class OrderDetails(
    val mOrderId: String,
    val amount: String,
    val convenienceFee: String = "0",
    val quantity: String = "1",
    val currency: String,
    val description: String = ""
)

data class CustomerDetails(
    val mCustomerId: String,
    val name: String = "",
    val email: String = "",
    val mobile: String = "",
    val code: String = ""
)

data class BillingDetails(
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val pin: String = ""
)

data class ShippingDetails(
    val shippingName: String = "",
    val shippingEmail: String = "",
    val shippingCode: String = "",
    val shippingMobile: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val pin: String = "",
    val locationPin: String = "",
    val shippingCurrency: String = "",
    val shippingAmount: String = "0"
)

data class PayOrcUrls(
    val webhookUrl: String = ""
)

data class PayOrcAddress(
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val province: String = "",
    val country: String = "",
    val pin: String = ""
)
