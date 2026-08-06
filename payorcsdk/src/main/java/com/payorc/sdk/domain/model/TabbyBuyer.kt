package com.payorc.sdk.domain.model

import kotlinx.serialization.Serializable

/**
 * Tabby Buyer Model
 * 
 * Represents the customer information for a Tabby transaction.
 */
@Serializable
data class TabbyBuyer(
    val email: String,
    val phone: String,
    val name: String,
    val dob: String? = null
)
