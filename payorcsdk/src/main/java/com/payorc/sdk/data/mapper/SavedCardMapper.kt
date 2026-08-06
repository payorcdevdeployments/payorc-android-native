package com.payorc.sdk.data.mapper

import com.payorc.sdk.data.remote.dto.SavedCardDto
import com.payorc.sdk.domain.model.SavedCard

object SavedCardMapper {
    fun mapToDomain(dto: SavedCardDto): SavedCard {
        val resolvedMask = dto.maskCardNumber?.takeIf { it.isNotBlank() }
            ?: dto.last4Digit?.let { "•••• •••• •••• $it" }
            ?: ""

        val resolvedScheme = dto.cardScheme?.takeIf { it.isNotBlank() }
            ?: dto.cardNetwork
            ?: ""

        val resolvedExpiry = dto.expiry?.takeIf { it.isNotBlank() }
            ?: dto.expiryDate

        return SavedCard(
            paymentToken = dto.paymentToken,
            maskCardNumber = resolvedMask,
            cardScheme = resolvedScheme,
            cardType = dto.cardType,
            expiry = resolvedExpiry,
            customerId = dto.mCustomerId,
            cardBrand = dto.cardBrand,
            pspInfo = dto.pspInfo
        )
    }

    fun mapToDomainList(dtos: List<SavedCardDto>): List<SavedCard> {
        return dtos.map { mapToDomain(it) }
    }
}
