package com.payorc.sdk.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PayorcSdkButtonCustomization(
    val backgroundColor: Color? = null,
    val foregroundColor: Color? = null,
    val disabledBackgroundColor: Color? = null,
    val sideBorderColor: Color? = null,
    val borderRadius: Dp? = null,
    val height: Dp? = null,
    val fontWeight: FontWeight? = null,
    val loadingIndicatorColor: Color? = null
)

data class PayorcSdkTextCustomization(
    val primary: Color? = null,
    val secondary: Color? = null,
    val bodyFontFamily: String? = null,
    val titleFontFamily: String? = null,
    val fontWeight: FontWeight? = null,
    val fontSize: TextUnit? = null,
    val letterSpacing: TextUnit? = null,
    val textAlign: TextAlign? = null,
    val fontStyle: String? = null,
    val maxLines: Int? = null,
    val wordSpacing: TextUnit? = null,
    val overflow: String? = null,
    val decoration: String? = null,
    val softWrap: Boolean? = null
)

data class CardFormError(
    val required: String? = null,
    val invalid: String? = null
)

data class PayorcSdkCardFormValidationCustomization(
    val cardHolderNameError: CardFormError? = null,
    val cardNumberError: CardFormError? = null,
    val expiryMonthError: CardFormError? = null,
    val expiryYearError: CardFormError? = null,
    val cvvError: CardFormError? = null,
    val invalidCardError: CardFormError? = null
)

enum class PayorcGuidanceStyle {
    hint, label,
}

enum class PayorcAppBarStyle {
    standard, nativeIos, nativeAndroid, nativeAndroid_Standard
}

data class PayorcEdgeInsets(
    val left: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp
) {
    companion object {
        fun all(value: Dp) = PayorcEdgeInsets(value, value, value, value)
        fun symmetric(horizontal: Dp = 0.dp, vertical: Dp = 0.dp) = PayorcEdgeInsets(horizontal, vertical, horizontal, vertical)
        fun only(left: Dp = 0.dp, top: Dp = 0.dp, right: Dp = 0.dp, bottom: Dp = 0.dp) = PayorcEdgeInsets(left, top, right, bottom)
    }
}

data class PayorcSdkAddCardFormCustomization(
    val titleUseNewCard: String? = null,
    val submitButtonTitleVerify: String? = null,
    val cardNumberHint: String? = null,
    val cardNumberLabel: String? = null,
    val expiryMonthHint: String? = null,
    val expiryMonthLabel: String? = null,
    val expiryYearHint: String? = null,
    val expiryYearLabel: String? = null,
    val cvvHint: String? = null,
    val cvvLabel: String? = null
)

data class PayorcSdkAppTextFieldCustomization(
    val borderRadius: Dp? = null,
    val height: Dp? = null,
    val borderColor: Color? = null,
    val padding: PayorcEdgeInsets? = null
)

data class PayorcSdkBottomSheetCustomization(
    val padding: PayorcEdgeInsets? = null,
    val spacing: Dp? = null,
    val cornerRadius: Dp? = null
)
