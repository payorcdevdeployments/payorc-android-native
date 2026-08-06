package com.payorc.sdk.domain.repository

import com.payorc.sdk.domain.model.CheckoutCustomizationResponse

interface PayOrcRepository {
    suspend fun fetchCheckoutCustomization(
        currency: String,
        amount: Double
    ): CheckoutCustomizationResponse

    suspend fun addCard(
        request: com.payorc.sdk.domain.model.AddCardRequest
    ): com.payorc.sdk.domain.model.AddCardResponse

    suspend fun submitPayment(
        request: com.payorc.sdk.domain.model.ExecutePaymentRequest
    ): com.payorc.sdk.domain.model.PaymentResponse

    suspend fun submitWalletPayment(
        paymentToken: String,
        orderId: String
    ): com.payorc.sdk.domain.model.PaymentResponse

    suspend fun fetchSavedCards(
        customerId: String,
        orderId: String
    ): com.payorc.sdk.domain.model.SavedCardsResult

    suspend fun initializeTabbyPayment(
        checkoutRequest: com.payorc.sdk.domain.model.PayOrcCheckoutRequest,
        merchantCode: String? = null
    ): com.payorc.sdk.data.remote.dto.TabbyInitResponseDto

    suspend fun confirmTabbyPayment(
        orderId: String,
        tabbyPaymentId: String
    ): com.payorc.sdk.data.remote.dto.TabbyConfirmResponseDto
}
