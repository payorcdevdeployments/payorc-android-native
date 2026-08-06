package com.payorc.sdk.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutCustomizationResponse(
    val data: CheckoutCustomizationData? = null,
    val message: String? = null,
    val status: String? = null,
    val code: String? = null
) {
    val isSuccess: Boolean
        get() = code == "00" || status?.lowercase() == "success"
}

@Serializable
data class CheckoutCustomizationData(
    @SerialName("available_methods") val availableMethods: List<AvailablePaymentMethod> = emptyList(),
    @SerialName("sdk_payorc_logo") val payorcLogo: String? = null,
    @SerialName("merchant_details") val merchantDetails: MerchantDetails? = null,
    @SerialName("show_new_card_billing_section") val showNewCardBillingSection: Boolean? = null,
    @SerialName("company_details") val companyDetails: CompanyDetails? = null
)

@Serializable
data class AvailablePaymentMethod(
    val type: String,
    val sequence: Int = 0,
    val show: Int = 0,
    val schemes: List<String> = emptyList(),
    val psp: String = "",
    val identifier: String = "",
    @SerialName("merchant_id") val merchantId: String = "",
    @SerialName("gateway_merchant_id") val gatewayMerchantId: String = "",
    @SerialName("public_key") val publicKey: String = "",
    @SerialName("merchant_code") val merchantCode: String = "",
    @SerialName("tabby_api_key") val tabbyApiKey: String = "",
    @SerialName("tabby_merchant_code") val tabbyMerchantCode: String = ""
)

@Serializable
data class MerchantDetails(
    val theme: String? = null,
    val icon: String? = null,
    val logo: String? = null,
    @SerialName("use_logo") val useLogo: Int? = null,
    @SerialName("we_accept_image") val weAcceptImage: String? = null,
    @SerialName("brand_color") val brandColor: String? = null,
    @SerialName("button_color") val buttonColor: String? = null,
    @SerialName("accent_color") val accentColor: String? = null,
    @SerialName("branding_language") val brandingLanguage: String? = null,
    @SerialName("font_name") val fontName: String? = null,
    @SerialName("amount_visible") val amountVisible: Int? = null,
    @SerialName("shipping_address_visible") val shippingAddressVisible: Int? = null,
    @SerialName("location_visible") val locationVisible: Int? = null,
    @SerialName("mobile_visible") val mobileVisible: Int? = null,
    @SerialName("cobranding_logo_visible") val cobrandingLogoVisible: Int? = null,
    @SerialName("cobranding_logo_filename") val cobrandingLogoFilename: String? = null,
    @SerialName("convenience_visible") val convenienceVisible: Int? = null,
    @SerialName("shipping_fee_visible") val shippingFeeVisible: Int? = null,
    @SerialName("email_visible") val emailVisible: Int? = null,
    @SerialName("name_visible") val nameVisible: Int? = null,
    @SerialName("remark_visible") val remarkVisible: Int? = null,
    @SerialName("remark_label") val remarkLabel: String? = null,
    @SerialName("cobranding_logo") val cobrandingLogo: String? = null,
    @SerialName("merchant_name") val merchantName: String? = null,
    @SerialName("company_name") val companyName: String? = null,
    @SerialName("tc_link") val tcLink: String? = null,
    @SerialName("use_logo_instead_icon") val useLogoInsteadIcon: Int? = null,
    @SerialName("shipping_visible") val shippingVisible: Int? = null,
    @SerialName("border_color") val borderColor: String? = null,
    @SerialName("app_style") val appStyle: String? = null,
    @SerialName("text_primary") val textPrimary: String? = null,
    @SerialName("text_secondary") val textSecondary: String? = null,
    @SerialName("autoselect_color") val autoselectColor: Long? = null,
    @SerialName("supported_card_schemes") val supportedCardSchemes: List<String> = emptyList(),
    @SerialName("field_border") val fieldBorder: String? = "outline",
    @SerialName("sdk_options") val sdkOptions: SdkOptions? = null
)

@Serializable
data class SdkOptions(
    @SerialName("font_weight") val fontWeight: String? = null,
    @SerialName("font_style") val fontStyle: String? = null,
    @SerialName("max_lines") val maxLines: String? = null,
    @SerialName("letter_spacing") val letterSpacing: String? = null,
    @SerialName("word_spacing") val wordSpacing: String? = null,
    @SerialName("overflow") val overflow: String? = null
)

@Serializable
data class CompanyDetails(
    @SerialName("fav_icon") val favIcon: String? = null,
    val logo: String? = null,
    @SerialName("letter_head") val letterHead: String? = null,
    @SerialName("footer_banner") val footerBanner: String? = null,
    val title: String? = null,
    @SerialName("terms_and_condition") val termsAndCondition: String? = null
)
