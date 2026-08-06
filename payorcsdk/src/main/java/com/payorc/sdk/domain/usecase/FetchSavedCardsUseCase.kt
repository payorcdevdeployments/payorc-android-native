package com.payorc.sdk.domain.usecase

import com.payorc.sdk.domain.model.SavedCardsResult
import com.payorc.sdk.domain.repository.PayOrcRepository

class FetchSavedCardsUseCase(private val repository: PayOrcRepository) {
    suspend operator fun invoke(customerId: String, orderId: String): SavedCardsResult {
        return repository.fetchSavedCards(customerId, orderId)
    }
}
