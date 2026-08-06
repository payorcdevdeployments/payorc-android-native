package com.payorc.sdk.domain.repository

import ai.tabby.android.data.TabbySession
import com.payorc.sdk.domain.model.TabbyBuyer

/**
 * Wallet Payment Repository Interface
 * 
 * Defines the contract for Tabby payment operations.
 */
interface WalletPaymentRepository {
    /**
     * Setups Tabby SDK
     */
    suspend fun setupTabby(apiKey: String, environment: String)

    /**
     * Creates a Tabby checkout session
     */
    suspend fun createTabbySession(
        merchantCode: String,
        amount: String,
        currency: String,
        buyer: TabbyBuyer,
        orderId: String,
        payorcOrderId: String? = null,
        successUrl: String,
        cancelUrl: String,
        failureUrl: String
    ): Result<TabbySession>

    /**
     * Verifies a Tabby payment
     */
    suspend fun verifyTabbyPayment(
        apiKey: String,
        paymentId: String
    ): Result<Boolean>
}
