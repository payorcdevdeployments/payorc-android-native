package com.payorc.sdk.domain.model

import androidx.compose.ui.graphics.Color

data class CheckoutConfig(
    val paymentMethods: List<PaymentMethod>,
    val branding: BrandingConfig,
    val fieldVisibility: FieldVisibilityConfig,
    val flowConfig: CheckoutFlowConfig,
    val companyAssets: CompanyAssetsConfig,
    val termsAndConditions: String?
)

data class PaymentMethod(
    val type: String,
    val sequence: Int,
    val schemes: List<String>,
    val googlePayConfig: GooglePayConfig? = null,
    val tabbyConfig: TabbyConfig? = null
)

data class GooglePayConfig(
    val merchantId: String,
    val gatewayMerchantId: String,
    val psp: String? = null,
    val merchantName: String? = null,
    val countryCode: String? = null
)

data class BrandingConfig(
    val brandColor: Color?,
    val buttonColor: Color?,
    val accentColor: Color?,
    val fontName: String?,
    val merchantName: String?,
    val iconUrl: String?,
    val logoUrl: String?
)

data class FieldVisibilityConfig(
    val nameVisible: Boolean,
    val emailVisible: Boolean,
    val mobileVisible: Boolean,
    val shippingVisible: Boolean,
    val amountVisible: Boolean,
    val remarkVisible: Boolean,
    val remarkLabel: String?,
    val shippingAddressVisible: Boolean
)

data class CheckoutFlowConfig(
    val showBillingSection: Boolean
)

data class CompanyAssetsConfig(
    val logoUrl: String?,
    val favIconUrl: String?,
    val footerBannerUrl: String?,
    val sdkPayorcLogoUrl: String?
)
