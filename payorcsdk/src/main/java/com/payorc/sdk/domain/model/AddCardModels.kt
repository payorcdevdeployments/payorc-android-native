package com.payorc.sdk.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class AddCardRequest(
    val cardHolderName: String,
    val cardNumber: String,
    val expiryMonth: String,
    val expiryYear: String,
    val cvv: String,
    val saveCard: Boolean = false
)

@Serializable
data class AddCardResponse(
    val data: AddCardData? = null,
    val message: String? = null,
    val status: String? = null,
    val code: String? = null
) {
    val isSuccess: Boolean get() = code == "00" || status?.lowercase() == "success"
}

@Serializable
data class AddCardData(
    @SerialName("payment_token") val paymentToken: String? = null,
    @SerialName("redirect_url") val redirectUrl: String? = null,
    @SerialName("m_order_id") val merchantOrderId: String? = null,
    @SerialName("p_order_id") val paymentOrderId: String? = null,
    val status: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val reason: String? = null,
    val cardBin: String? = null,
    val cardLast4: String? = null,
    val cardScheme: String? = null
)

data class PaymentTokenData(
    val paymentToken: String,
    val transactionId: String,
    val orderId: String,
    val scheme: String,
    val maskCardNumber: String
)

sealed interface AddCardResult {
    object Loading : AddCardResult
    data class Success(val tokenData: PaymentTokenData) : AddCardResult
    data class Failure(val message: String) : AddCardResult
}
