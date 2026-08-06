package com.payorc.sdk.domain.usecase

import ai.tabby.android.data.TabbySession
import com.payorc.sdk.domain.model.TabbyBuyer
import com.payorc.sdk.domain.repository.WalletPaymentRepository

/**
 * Create Tabby Session Use Case
 * 
 * Logic to initiate a Tabby payment session.
 */
class CreateTabbySessionUseCase(
    private val repository: WalletPaymentRepository
) {
    suspend operator fun invoke(
        merchantCode: String,
        amount: String,
        currency: String,
        buyer: TabbyBuyer,
        orderId: String,
        payorcOrderId: String? = null,
        successUrl: String = "payorc://tabby/success",
        cancelUrl: String = "payorc://tabby/cancel",
        failureUrl: String = "payorc://tabby/failure"
    ): Result<TabbySession> {
        return repository.createTabbySession(
            merchantCode = merchantCode,
            amount = amount,
            currency = currency,
            buyer = buyer,
            orderId = orderId,
            payorcOrderId = payorcOrderId,
            successUrl = successUrl,
            cancelUrl = cancelUrl,
            failureUrl = failureUrl
        )
    }
}
