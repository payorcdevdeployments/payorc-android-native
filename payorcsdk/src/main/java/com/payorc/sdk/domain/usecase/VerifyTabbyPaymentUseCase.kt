package com.payorc.sdk.domain.usecase

import com.payorc.sdk.domain.repository.WalletPaymentRepository

/**
 * Verify Tabby Payment Use Case
 * 
 * Logic to verify the status of a Tabby payment after redirection.
 */
class VerifyTabbyPaymentUseCase(
    private val repository: WalletPaymentRepository
) {
    suspend operator fun invoke(
        paymentId: String
    ): Result<Boolean> {
        return repository.verifyTabbyPayment("", paymentId)
    }
}
