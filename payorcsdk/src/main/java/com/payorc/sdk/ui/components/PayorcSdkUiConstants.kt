package com.payorc.sdk.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payorc.sdk.PayorcInputBorderStyle
import com.payorc.sdk.domain.model.*
import com.payorc.sdk.localization.PayorcLocalization
import com.payorc.sdk.ui.theme.AppColors

/**
 * SDK-wide UI constants and dynamic colors.
 * Follows the Resolution Logic: Merchant Override > API Response > SDK Default.
 */
object PayorcSdkUiConstants {
    
    // --- MERCHANT OVERRIDES (Set via PayOrcSdk.customization) ---
    var merchantBrandColor by mutableStateOf<Color?>(null)
    var merchantAccentColor by mutableStateOf<Color?>(null)
    var merchantBorderColor by mutableStateOf<Color?>(null)
    var merchantInputBorderStyle by mutableStateOf<PayorcInputBorderStyle?>(null)
    var merchantGuidanceStyle by mutableStateOf<PayorcGuidanceStyle?>(null)
    var merchantAppBarStyle by mutableStateOf<PayorcAppBarStyle?>(null)
    var merchantTextPrimaryColor by mutableStateOf<Color?>(null)
    var merchantTextSecondaryColor by mutableStateOf<Color?>(null)

    var merchantButtonConfig by mutableStateOf<PayorcSdkButtonCustomization?>(null)
    var merchantTextConfig by mutableStateOf<PayorcSdkTextCustomization?>(null)
    var merchantValidationConfig by mutableStateOf<PayorcSdkCardFormValidationCustomization?>(null)
    var merchantAddCardFormConfig by mutableStateOf<PayorcSdkAddCardFormCustomization?>(null)
    var merchantAppTextFieldConfig by mutableStateOf<PayorcSdkAppTextFieldCustomization?>(null)
    var merchantBottomSheetConfig by mutableStateOf<PayorcSdkBottomSheetCustomization?>(null)

    // --- API VALUES (Set via fetchCheckoutCustomization) ---
    var apiBrandColor by mutableStateOf<Color?>(null)
    var apiButtonColor by mutableStateOf<Color?>(null)
    var apiAccentColor by mutableStateOf<Color?>(null)
    var apiBorderColor by mutableStateOf<Color?>(null)
    var apiTextPrimaryColor by mutableStateOf<Color?>(null)
    var apiTextSecondaryColor by mutableStateOf<Color?>(null)
    var apiInputBorderStyle by mutableStateOf<PayorcInputBorderStyle?>(null)
    var apiAppBarStyle by mutableStateOf<PayorcAppBarStyle?>(null)
    var apiFontName by mutableStateOf<String?>(null)
    var apiFontSize by mutableStateOf<androidx.compose.ui.unit.TextUnit?>(null)
    
    // New SDK Options from API
    var apiFontWeight by mutableStateOf<FontWeight?>(null)
    var apiFontStyle by mutableStateOf<String?>(null)
    var apiMaxLines by mutableStateOf<Int?>(null)
    var apiLetterSpacing by mutableStateOf<androidx.compose.ui.unit.TextUnit?>(null)
    var apiWordSpacing by mutableStateOf<androidx.compose.ui.unit.TextUnit?>(null)
    var apiOverflow by mutableStateOf<String?>(null)

    // --- RESOLVED VALUES (Publicly used by the SDK UI) ---

    val brandColor: Color
        get() = merchantBrandColor ?: apiBrandColor ?: Color.Gray

    val accentColor: Color
        get() = merchantAccentColor ?: apiAccentColor ?: AppColors.Primary

    val borderColor: Color
        get() = merchantBorderColor ?: apiBorderColor ?: Color(0xFFCCCCCC)

    val inputBorderStyle: PayorcInputBorderStyle
        get() = merchantInputBorderStyle ?: apiInputBorderStyle ?: PayorcInputBorderStyle.underline

    val guidanceStyle: PayorcGuidanceStyle
        get() = merchantGuidanceStyle ?: PayorcGuidanceStyle.hint

    val appBarStyle: PayorcAppBarStyle
        get() = merchantAppBarStyle ?: apiAppBarStyle ?: PayorcAppBarStyle.standard

    /**
     * Contrast-aware primary text color on the brand background.
     */
    val onBrandBackgroundPrimary: Color
        get() {
            val base = textPrimaryColor
            if (merchantTextPrimaryColor != null) return base
            val luminance = 0.299f * brandColor.red + 0.587f * brandColor.green + 0.114f * brandColor.blue
            return if (luminance > 0.5f) Color.Black else Color.White
        }

    /**
     * Contrast-aware secondary text color on the brand background.
     */
    val onBrandBackgroundSecondary: Color
        get() {
            val base = textSecondaryColor
            if (merchantTextSecondaryColor != null) return base
            val luminance = 0.299f * brandColor.red + 0.587f * brandColor.green + 0.114f * brandColor.blue
            return if (luminance > 0.5f) Color.DarkGray else Color.White.copy(alpha = 0.72f)
        }

    // --- NEW CONFIG RESOLUTIONS ---
    val addCardFormConfig: PayorcSdkAddCardFormCustomization?
        get() = merchantAddCardFormConfig
        
    val appTextFieldConfig: PayorcSdkAppTextFieldCustomization?
        get() = merchantAppTextFieldConfig
        
    val bottomSheetConfig: PayorcSdkBottomSheetCustomization?
        get() = merchantBottomSheetConfig

    // --- BUTTON RESOLUTION ---
    val buttonBackgroundColor: Color
        get() = merchantButtonConfig?.backgroundColor ?: apiButtonColor ?: AppColors.Dark

    val buttonForegroundColor: Color
        get() = merchantButtonConfig?.foregroundColor ?: Color.White

    val buttonDisabledBackgroundColor: Color
        get() = merchantButtonConfig?.disabledBackgroundColor ?: Color.Gray

    /**
     * Text/icon color for the disabled button state.
     * Auto-computed as the opposite of [buttonDisabledBackgroundColor] luminance:
     *   dark disabled background → White text
     *   light disabled background → Black text
     */
    val buttonDisabledForegroundColor: Color
        get() {
            val bg = buttonDisabledBackgroundColor
            val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
            return if (luminance > 0.5f) Color.Black else Color.White
        }

    val buttonSideBorderColor: Color
        get() = merchantButtonConfig?.sideBorderColor ?: Color.Transparent

    val buttonBorderRadius: androidx.compose.ui.unit.Dp
        get() = merchantButtonConfig?.borderRadius ?: 12.dp

    val buttonHeight: androidx.compose.ui.unit.Dp
        get() = merchantButtonConfig?.height ?: 56.dp

    val buttonFontWeight: FontWeight
        get() = merchantButtonConfig?.fontWeight ?: FontWeight.Bold

    val loadingIndicatorColor: Color
        get() = merchantButtonConfig?.loadingIndicatorColor ?: accentColor

    // --- TEXT RESOLUTION ---
    val textPrimaryColor: Color
        get() = merchantTextPrimaryColor ?: merchantTextConfig?.primary ?: apiTextPrimaryColor ?: AppColors.TextPrimary

    val textSecondaryColor: Color
        get() = merchantTextSecondaryColor ?: merchantTextConfig?.secondary ?: apiTextSecondaryColor ?: AppColors.TextSecondary

    val textFontSize: androidx.compose.ui.unit.TextUnit
        get() = merchantTextConfig?.fontSize ?: apiFontSize ?: 14.sp

    val bodyFontFamily: String?
        get() = merchantTextConfig?.bodyFontFamily ?: apiFontName
        
    val textFontWeight: FontWeight?
        get() = merchantTextConfig?.fontWeight ?: apiFontWeight
        
    val textFontStyle: String?
        get() = merchantTextConfig?.fontStyle ?: apiFontStyle
        
    val textMaxLines: Int?
        get() = merchantTextConfig?.maxLines ?: apiMaxLines
        
    val textLetterSpacing: androidx.compose.ui.unit.TextUnit?
        get() = merchantTextConfig?.letterSpacing ?: apiLetterSpacing
        
    val textWordSpacing: androidx.compose.ui.unit.TextUnit?
        get() = merchantTextConfig?.wordSpacing ?: apiWordSpacing
        
    val textOverflow: String?
        get() = merchantTextConfig?.overflow ?: apiOverflow


    // --- VALIDATION MESSAGE RESOLUTION ---
    fun getValidationError(field: String, type: String, default: String? = null): String {
        val config = merchantValidationConfig
        
        // Define dynamic fallbacks based on field name
        val readableField = when(field.lowercase()) {
            "name" -> "Cardholder name"
            "customer_name" -> "Name"
            "number" -> "Card number"
            "expiry_month" -> "MM"
            "expiry_year" -> "YY"
            "cvv" -> "CVV"
            "card" -> "Card"
            "email" -> "Email"
            "mobile" -> "Mobile number"
            "billing_address" -> "Billing address"
            "shipping_address" -> "Shipping address"
            else -> field.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        
        val dynamicDefault = if (type == "required") "$readableField required" else "Invalid $readableField"
        
        val rawMessage = if (config == null) {
            default ?: dynamicDefault
        } else {
            when (field.lowercase()) {
                "name" -> if (type == "required") config.cardHolderNameError?.required else config.cardHolderNameError?.invalid
                "number" -> if (type == "required") config.cardNumberError?.required else config.cardNumberError?.invalid
                "expiry_month" -> if (type == "required") config.expiryMonthError?.required else config.expiryMonthError?.invalid
                "expiry_year" -> if (type == "required") config.expiryYearError?.required else config.expiryYearError?.invalid
                "cvv" -> if (type == "required") config.cvvError?.required else config.cvvError?.invalid
                "card" -> if (type == "required") config.invalidCardError?.required else config.invalidCardError?.invalid
                else -> null
            } ?: default ?: dynamicDefault
        }

        // Apply placeholder replacement
        return PayorcLocalization.localize(
            rawMessage.replace(
                "\$fieldname",
                PayorcLocalization.localize(readableField),
                ignoreCase = true
            )
        )
    }

    fun clear() {
        merchantBrandColor = null
        merchantAccentColor = null
        merchantBorderColor = null
        merchantInputBorderStyle = null
        merchantGuidanceStyle = null
        merchantAppBarStyle = null
        merchantTextPrimaryColor = null
        merchantTextSecondaryColor = null
        merchantButtonConfig = null
        merchantTextConfig = null
        merchantValidationConfig = null
        merchantAddCardFormConfig = null
        merchantAppTextFieldConfig = null
        merchantBottomSheetConfig = null
        
        apiBrandColor = null
        apiButtonColor = null
        apiAccentColor = null
        apiBorderColor = null
        apiTextPrimaryColor = null
        apiTextSecondaryColor = null
        apiInputBorderStyle = null
        apiAppBarStyle = null
        apiFontName = null
        apiFontSize = null
        apiFontWeight = null
        apiFontStyle = null
        apiMaxLines = null
        apiLetterSpacing = null
        apiWordSpacing = null
        apiOverflow = null
    }

    fun applyFromMerchantDetails(details: MerchantDetails?) {
        if (details == null) return
        
        apiBrandColor = parseHexColor(details.brandColor)
        apiButtonColor = parseHexColor(details.buttonColor)
        apiAccentColor = parseHexColor(details.accentColor)
        apiBorderColor = parseHexColor(details.borderColor)
        apiTextPrimaryColor = parseHexColor(details.textPrimary)
        apiTextSecondaryColor = parseHexColor(details.textSecondary)
        apiFontName = details.fontName?.trim()?.takeIf { it.isNotEmpty() }
        
        apiInputBorderStyle = when (details.fieldBorder?.trim()?.lowercase()) {
            "outline" -> PayorcInputBorderStyle.outline
            "underline" -> PayorcInputBorderStyle.underline
            else -> null
        }

        apiAppBarStyle = when (details.appStyle?.trim()?.lowercase()) {
            "standard" -> PayorcAppBarStyle.standard
            "nativeios" -> PayorcAppBarStyle.nativeIos
            "nativeandroid" -> PayorcAppBarStyle.nativeAndroid
            "nativeandroid_standard" -> PayorcAppBarStyle.nativeAndroid_Standard
            else -> null
        }
        
        val sdkOpts = details.sdkOptions
        if (sdkOpts != null) {
            apiFontWeight = when (sdkOpts.fontWeight?.lowercase()) {
                "bold" -> FontWeight.Bold
                "medium" -> FontWeight.Medium
                "light" -> FontWeight.Light
                "normal" -> FontWeight.Normal
                "semibold" -> FontWeight.SemiBold
                else -> null
            }
            apiFontStyle = sdkOpts.fontStyle
            apiMaxLines = sdkOpts.maxLines?.toIntOrNull()
            apiLetterSpacing = sdkOpts.letterSpacing?.toFloatOrNull()?.sp
            apiWordSpacing = sdkOpts.wordSpacing?.toFloatOrNull()?.sp
            apiOverflow = sdkOpts.overflow
        }
    }

    private fun parseHexColor(hex: String?): Color? {
        if (hex == null) return null
        val s = hex.trim().removePrefix("#")
        return try {
            when (s.length) {
                3 -> {
                    val r = s[0].toString().repeat(2)
                    val g = s[1].toString().repeat(2)
                    val b = s[2].toString().repeat(2)
                    Color(android.graphics.Color.parseColor("#FF$r$g$b"))
                }
                6 -> Color(android.graphics.Color.parseColor("#FF$s"))
                8 -> Color(android.graphics.Color.parseColor("#$s"))
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
