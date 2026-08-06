package com.payorc.sdk.domain.model

data class SavedCardsResult(
    val data: List<SavedCard> = emptyList(),
    val message: String? = null,
    val status: String? = null,
    val code: String? = null
) {
    val isSuccess: Boolean
        get() = code == "00" || status?.lowercase() == "success"
}
