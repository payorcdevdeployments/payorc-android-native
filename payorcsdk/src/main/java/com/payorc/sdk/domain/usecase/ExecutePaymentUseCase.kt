package com.payorc.sdk.domain.usecase

import com.payorc.sdk.domain.model.ExecutePaymentRequest
import com.payorc.sdk.domain.model.PaymentResponse
import com.payorc.sdk.domain.repository.PayOrcRepository

class ExecutePaymentUseCase(
    private val repository: PayOrcRepository
) {
    suspend fun execute(request: ExecutePaymentRequest): PaymentResponse {
        return repository.submitPayment(request)
    }
}
