package com.payorc.sdk.domain.usecase

import com.payorc.sdk.domain.model.CheckoutConfig
import com.payorc.sdk.domain.model.PayOrcCheckoutRequest
import com.payorc.sdk.domain.model.isComplete

data class CheckoutValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

class CheckoutValidationUseCase {

    fun execute(request: PayOrcCheckoutRequest, config: CheckoutConfig): CheckoutValidationResult {
        if (config.fieldVisibility.nameVisible && request.customerName.isBlank()) {
            return CheckoutValidationResult(false, "Customer name is required")
        }

        if (config.fieldVisibility.emailVisible && request.customerEmail.isBlank()) {
            return CheckoutValidationResult(false, "Customer email is required")
        }

        if (config.fieldVisibility.mobileVisible && request.customerMobile.isBlank()) {
            return CheckoutValidationResult(false, "Customer mobile number is required")
        }

        if (config.fieldVisibility.mobileVisible && request.customerMobile.isNotBlank() && request.customerMobileCode.isBlank()) {
            return CheckoutValidationResult(false, "Customer mobile country code is required")
        }

        if (config.flowConfig.showBillingSection && !request.billingAddress.isComplete()) {
            return CheckoutValidationResult(false, "Billing address is required")
        }

        if (config.fieldVisibility.shippingVisible && config.fieldVisibility.shippingAddressVisible && !request.shippingAddress.isComplete()) {
            return CheckoutValidationResult(false, "Shipping address is required")
        }

        return CheckoutValidationResult(true)
    }
}
