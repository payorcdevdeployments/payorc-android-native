package com.payorc.sdk.core.security

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SignatureGenerator {
    fun generate(
        merchantKey: String,
        merchantSecret: String,
        timestamp: String,
        bodyJson: String
    ): String {
        val payload = "$merchantKey$timestamp$bodyJson"
        val secretKey = SecretKeySpec(merchantSecret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(secretKey)
        val hash = mac.doFinal(payload.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }.lowercase()
    }
}
