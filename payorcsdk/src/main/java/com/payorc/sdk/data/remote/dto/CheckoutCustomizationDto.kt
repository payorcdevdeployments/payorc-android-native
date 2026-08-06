package com.payorc.sdk.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckoutCustomizationDto(
    val data: CheckoutCustomizationDataDto? = null,
    val message: String? = null,
    val status: String? = null,
    val code: String? = null
)

@Serializable
data class CheckoutCustomizationDataDto(
    @SerialName("available_methods") val availableMethods: List<AvailablePaymentMethodDto> = emptyList(),
    @SerialName("sdk_payorc_logo") val payorcLogo: String? = null,
    @SerialName("merchant_details") val merchantDetails: MerchantDetailsDto? = null,
    @SerialName("show_new_card_billing_section") val showNewCardBillingSection: Boolean? = null,
    @SerialName("company_details") val companyDetails: CompanyDetailsDto? = null
)

@Serializable
data class AvailablePaymentMethodDto(
    val type: String,
    val sequence: Int = 0,
    val show: Int = 0,
    val schemes: List<String> = emptyList(),
    val psp: String = "",
    val identifier: String = "",
    @SerialName("merchant_id") val merchantId: String = "",
    @SerialName("gateway_merchant_id") val gatewayMerchantId: String = "",
    @SerialName("public_key") val publicKey: String = "",
    @SerialName("merchant_code") val merchantCode: String = ""
)

@Serializable
data class MerchantDetailsDto(
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
    @SerialName("supported_card_schemes") val supportedCardSchemes: List<String> = emptyList(),
    @SerialName("field_border") val fieldBorder: String? = "outline"
)

@Serializable
data class CompanyDetailsDto(
    @SerialName("fav_icon") val favIcon: String? = null,
    val logo: String? = null,
    @SerialName("letter_head") val letterHead: String? = null,
    @SerialName("footer_banner") val footerBanner: String? = null,
    val title: String? = null,
    @SerialName("terms_and_condition") val termsAndCondition: String? = null
)
