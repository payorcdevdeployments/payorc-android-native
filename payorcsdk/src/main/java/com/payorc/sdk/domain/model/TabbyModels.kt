package com.payorc.sdk.domain.model

/**
 * Tabby Configuration
 */
data class TabbyConfig(
    val apiKey: String,
    val merchantCode: String = "ae"  // Default to UAE
)
