package com.payorc.sdk.domain.usecase

import com.payorc.sdk.domain.model.PaymentResponse
import com.payorc.sdk.domain.repository.PayOrcRepository

class ExecuteWalletPaymentUseCase(private val repository: PayOrcRepository) {
    suspend fun execute(paymentToken: String, orderId: String): PaymentResponse {
        return repository.submitWalletPayment(paymentToken, orderId)
    }
}
