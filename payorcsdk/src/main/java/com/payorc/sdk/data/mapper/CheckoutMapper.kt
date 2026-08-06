package com.payorc.sdk.data.mapper

import androidx.compose.ui.graphics.Color
import com.payorc.sdk.domain.model.*

object CheckoutMapper {

    fun mapToDomain(dto: CheckoutCustomizationResponse): CheckoutConfig {
        val data = dto.data
        val merchant = data?.merchantDetails
        val company = data?.companyDetails

        return CheckoutConfig(
            paymentMethods = data?.availableMethods?.filter { it.show == 1 && it.type.uppercase() != "APPLE_PAY" }
                ?.map { methodDto ->
                    PaymentMethod(
                        type = methodDto.type.uppercase(),
                        sequence = methodDto.sequence,
                        schemes = methodDto.schemes,
                        googlePayConfig = if (methodDto.type.equals("GOOGLE_PAY", ignoreCase = true)) {
                            GooglePayConfig(
                                merchantId = methodDto.merchantId,
                                gatewayMerchantId = methodDto.gatewayMerchantId,
                                psp = methodDto.psp,
                                merchantName = merchant?.merchantName,
                                countryCode = null  // Will use backend default or "SA"
                            )
                        } else null,
                        tabbyConfig = if (methodDto.type.equals("TABBY", ignoreCase = true)) {
                            val apiKey = methodDto.tabbyApiKey.ifEmpty { methodDto.publicKey }
                            val merchantCode = methodDto.tabbyMerchantCode.ifEmpty { methodDto.merchantCode }
                            
                            android.util.Log.d("PayOrc_Debug", "Tabby Mapping: apiKey='$apiKey', merchantCode='$merchantCode'")

                            if (apiKey.isNotEmpty()) {
                                TabbyConfig(
                                    apiKey = apiKey,
                                    merchantCode = merchantCode.ifEmpty { "ae" }  // Only fallback to "ae" if absolutely empty
                                )
                            } else null
                        } else null
                    )
                }?.sortedWith(compareByDescending<PaymentMethod> { it.type == "GOOGLE_PAY" }.thenBy { it.sequence }) ?: emptyList(),

            branding = BrandingConfig(
                brandColor = parseHexColor(merchant?.brandColor),
                buttonColor = parseHexColor(merchant?.buttonColor),
                accentColor = parseHexColor(merchant?.accentColor),
                fontName = merchant?.fontName,
                merchantName = merchant?.merchantName,
                iconUrl = merchant?.icon,
                logoUrl = merchant?.logo
            ),

            fieldVisibility = FieldVisibilityConfig(
                nameVisible = merchant?.nameVisible == 1,
                emailVisible = merchant?.emailVisible == 1,
                mobileVisible = merchant?.mobileVisible == 1,
                shippingVisible = merchant?.shippingVisible == 1,
                amountVisible = merchant?.amountVisible == 1,
                remarkVisible = merchant?.remarkVisible == 1,
                remarkLabel = merchant?.remarkLabel,
                shippingAddressVisible = merchant?.shippingAddressVisible == 1
            ),
            flowConfig = CheckoutFlowConfig(
                showBillingSection = data?.showNewCardBillingSection == true
            ),

            companyAssets = CompanyAssetsConfig(
                logoUrl = company?.logo,
                favIconUrl = company?.favIcon,
                footerBannerUrl = company?.footerBanner,
                sdkPayorcLogoUrl = data?.payorcLogo
            ),

            termsAndConditions = company?.termsAndCondition
        )
    }

    private fun parseHexColor(hex: String?): Color? {
        if (hex.isNullOrBlank()) return null
        return try {
            var colorStr = hex.trim()
            if (colorStr.startsWith("#")) colorStr = colorStr.substring(1)
            
            val colorInt = when (colorStr.length) {
                3 -> {
                    val r = colorStr[0].toString().repeat(2)
                    val g = colorStr[1].toString().repeat(2)
                    val b = colorStr[2].toString().repeat(2)
                    android.graphics.Color.parseColor("#FF$r$g$b")
                }
                6 -> android.graphics.Color.parseColor("#FF$colorStr")
                8 -> android.graphics.Color.parseColor("#$colorStr")
                else -> return null
            }
            Color(colorInt)
        } catch (e: Exception) {
            null
        }
    }
}
