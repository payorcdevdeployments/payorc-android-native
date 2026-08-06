package com.payorc.sdk.data.repository

import ai.tabby.android.data.*
import ai.tabby.android.data.Currency
import ai.tabby.android.factory.TabbyFactory
import ai.tabby.android.internal.network.TabbyEnvironment
import com.payorc.sdk.domain.model.TabbyBuyer
import com.payorc.sdk.domain.repository.WalletPaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.*

/**
 * Wallet Payment Repository Implementation
 * 
 * Implementation of Tabby payment operations using the official Tabby Android SDK.
 */
class WalletPaymentRepositoryImpl(
    private val context: android.content.Context
) : WalletPaymentRepository {

    override suspend fun setupTabby(apiKey: String, environment: String) {
        val env = if (environment.equals("production", ignoreCase = true)) {
            TabbyEnvironment.Prod
        } else {
            TabbyEnvironment.Stage
        }
        // Suspend setup in 2.0.0
        TabbyFactory.setup(context, apiKey, env)
    }

    override suspend fun createTabbySession(
        merchantCode: String,
        amount: String,
        currency: String,
        buyer: TabbyBuyer,
        orderId: String,
        payorcOrderId: String?,
        successUrl: String,
        cancelUrl: String,
        failureUrl: String
    ): Result<TabbySession> = withContext(Dispatchers.IO) {
        try {
            val tabby = TabbyFactory.tabby()
            // Build meta object with PayOrc order_id if available
            val meta = if (!payorcOrderId.isNullOrBlank()) {
                mapOf("order_id" to payorcOrderId)
            } else {
                emptyMap()
            }
            val tabbyPayment = TabbyPayment(
                amount = BigDecimal(amount),
                currency = Currency.valueOf(currency.uppercase()),
                description = "Order $orderId",
                buyer = Buyer(
                    email = buyer.email,
                    phone = buyer.phone,
                    name = buyer.name
                ),
                order = Order(
                    refId = orderId,
                    items = emptyList()
                ),
                shippingAddress = null,
                buyerHistory = BuyerHistory(
                    registeredSince = Date(),
                    loyaltyLevel = 0,
                    wishlistCount = 0,
                    isSocialNetworksConnected = false,
                    isPhoneNumberVerified = false,
                    isEmailVerified = false
                ),
                orderHistory = emptyList(),
                meta = meta
            )

            val session = tabby.createSession(
                merchantCode = merchantCode,
                lang = Lang.EN,
                payment = tabbyPayment
            )

            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun verifyTabbyPayment(
        apiKey: String,
        paymentId: String
    ): Result<Boolean> {
        return Result.success(true)
    }
}
