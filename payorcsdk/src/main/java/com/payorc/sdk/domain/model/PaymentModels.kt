package com.payorc.sdk.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class ExecutePaymentRequest(
    val paymentToken: String,
    val amount: String,
    val currency: String,
    val orderId: String,
    val description: String? = null,
    val cvv: String? = null,
    val cardHolderName: String? = null,
    val cardNumber: String? = null,
    val expiryMonth: String? = null,
    val expiryYear: String? = null
)

@Serializable
data class PaymentResponse(
    val data: PaymentData? = null,
    val message: String? = null,
    val status: String? = null,
    val code: String? = null
) {
    val isSuccess: Boolean get() = code == "00" || status?.lowercase() == "success"
}

@Serializable
data class PaymentData(
    @SerialName("transaction_id") val transactionId: String? = null,
    @SerialName("p_order_id") val pOrderId: String? = null,
    @SerialName("m_order_id") val mOrderId: String? = null,
    @SerialName("redirect_url") val redirectUrl: String? = null,
    val authCode: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val status: String? = null
)

sealed interface PaymentResult {
    object Loading : PaymentResult
    data class Success(val transactionId: String) : PaymentResult
    data class Failure(val message: String) : PaymentResult
    object Pending : PaymentResult
    data class RedirectTo3DS(val url: String) : PaymentResult
}
