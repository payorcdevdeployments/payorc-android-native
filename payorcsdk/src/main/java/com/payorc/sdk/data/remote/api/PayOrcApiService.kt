package com.payorc.sdk.data.remote.api

import com.payorc.sdk.core.network.PayOrcGatewayUrls
import com.payorc.sdk.domain.model.CheckoutCustomizationResponse
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.POST

interface PayOrcApiService {

    @POST(PayOrcGatewayUrls.CHECKOUT_CUSTOMIZATION)
    suspend fun fetchCheckoutCustomization(
        @HeaderMap headers: Map<String, String>,
        @Body body: String
    ): CheckoutCustomizationResponse

    @POST(PayOrcGatewayUrls.ADD_CARD)
    suspend fun addCard(
        @HeaderMap headers: Map<String, String>,
        @Body body: String
    ): com.payorc.sdk.domain.model.AddCardResponse

    @POST(PayOrcGatewayUrls.PAYMENT)
    suspend fun submitPayment(
        @HeaderMap headers: Map<String, String>,
        @Body body: String
    ): com.payorc.sdk.domain.model.PaymentResponse

    @POST(PayOrcGatewayUrls.WALLET_PAYMENT)
    suspend fun submitWalletPayment(
        @HeaderMap headers: Map<String, String>,
        @Body body: String
    ): com.payorc.sdk.domain.model.PaymentResponse

    @POST(PayOrcGatewayUrls.CUSTOMER_CARDS)
    suspend fun fetchSavedCards(
        @HeaderMap headers: Map<String, String>,
        @Body body: String
    ): com.payorc.sdk.data.remote.dto.SavedCardsResponseDto

    @POST(PayOrcGatewayUrls.TABBY_INIT)
    suspend fun initializeTabby(
        @HeaderMap headers: Map<String, String>,
        @Body body: String
    ): com.payorc.sdk.data.remote.dto.TabbyInitResponseDto

    @POST(PayOrcGatewayUrls.TABBY_CONFIRM)
    suspend fun confirmTabbyPayment(
        @HeaderMap headers: Map<String, String>,
        @Body body: String
    ): com.payorc.sdk.data.remote.dto.TabbyConfirmResponseDto
}
