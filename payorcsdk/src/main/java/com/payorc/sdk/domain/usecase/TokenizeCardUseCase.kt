package com.payorc.sdk.domain.usecase

import com.payorc.sdk.domain.model.AddCardRequest
import com.payorc.sdk.domain.model.AddCardResponse
import com.payorc.sdk.domain.repository.PayOrcRepository

class TokenizeCardUseCase(
    private val repository: PayOrcRepository
) {
    suspend fun execute(request: AddCardRequest): AddCardResponse {
        return repository.addCard(request)
    }
}
