package com.payorc.sdk.presentation.checkout.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import com.payorc.sdk.PayorcInputBorderStyle
import com.payorc.sdk.ui.components.PayorcSdkUiConstants

/**
 * A wrapper for TextField that respects the SDK's inputBorderStyle customization.
 *
 * Label text, placeholder/hint text, typed input text, and cursor are always rendered
 * in a color that contrasts against the brand background — so they remain visible
 * regardless of whether the brand color is black, white, or any other hue.
 */
@Composable
fun PayOrcTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current
) {
    val style = PayorcSdkUiConstants.inputBorderStyle
    val appTextFieldConfig = PayorcSdkUiConstants.appTextFieldConfig
    
    val shape = RoundedCornerShape(appTextFieldConfig?.borderRadius ?: (PayorcSdkUiConstants.buttonBorderRadius / 2))
    val height = appTextFieldConfig?.height
    val padding = appTextFieldConfig?.padding
    val borderColor = appTextFieldConfig?.borderColor ?: PayorcSdkUiConstants.borderColor

    val guidanceStyle = PayorcSdkUiConstants.guidanceStyle

    val actualLabel = if (guidanceStyle == com.payorc.sdk.domain.model.PayorcGuidanceStyle.label) label else null
    val actualPlaceholder = if (guidanceStyle == com.payorc.sdk.domain.model.PayorcGuidanceStyle.hint) (label ?: placeholder) else placeholder

    // Use the merchant/API-configured primary and secondary text colors everywhere.
    val onBrand = PayorcSdkUiConstants.textPrimaryColor
    val onBrandSecondary = PayorcSdkUiConstants.textSecondaryColor

    var fieldModifier = modifier.fillMaxWidth()
    if (height != null) {
        fieldModifier = fieldModifier.height(height)
    }
    if (padding != null) {
        fieldModifier = fieldModifier.padding(
            start = padding.left,
            top = padding.top,
            end = padding.right,
            bottom = padding.bottom
        )
    }

    if (style == PayorcInputBorderStyle.outline) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier,
            label = actualLabel,
            placeholder = actualPlaceholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            enabled = enabled,
            textStyle = textStyle,
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                // Border
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor.copy(alpha = 0.5f),
                // Typed text & cursor — must contrast against brand background
                focusedTextColor = onBrand,
                unfocusedTextColor = onBrand,
                disabledTextColor = onBrand.copy(alpha = 0.5f),
                cursorColor = onBrand,
                // Label (floating label above the field)
                focusedLabelColor = onBrand,
                unfocusedLabelColor = onBrandSecondary,
                disabledLabelColor = onBrandSecondary.copy(alpha = 0.5f),
                // Placeholder / hint text
                focusedPlaceholderColor = onBrandSecondary,
                unfocusedPlaceholderColor = onBrandSecondary,
                disabledPlaceholderColor = onBrandSecondary.copy(alpha = 0.5f),
                // Icons inside the field
                focusedLeadingIconColor = onBrand,
                unfocusedLeadingIconColor = onBrandSecondary,
                focusedTrailingIconColor = onBrand,
                unfocusedTrailingIconColor = onBrandSecondary,
            )
        )
    } else {
        // Underline style (Material 3 TextField with transparent container)
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = fieldModifier,
            label = actualLabel,
            placeholder = actualPlaceholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            supportingText = supportingText,
            isError = isError,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            enabled = enabled,
            textStyle = textStyle,
            colors = TextFieldDefaults.colors(
                // Container (background) — keep transparent
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                // Underline
                focusedIndicatorColor = borderColor,
                unfocusedIndicatorColor = borderColor.copy(alpha = 0.5f),
                // Typed text & cursor — must contrast against brand background
                focusedTextColor = onBrand,
                unfocusedTextColor = onBrand,
                disabledTextColor = onBrand.copy(alpha = 0.5f),
                cursorColor = onBrand,
                // Label (floating label above the field)
                focusedLabelColor = onBrand,
                unfocusedLabelColor = onBrandSecondary,
                disabledLabelColor = onBrandSecondary.copy(alpha = 0.5f),
                // Placeholder / hint text
                focusedPlaceholderColor = onBrandSecondary,
                unfocusedPlaceholderColor = onBrandSecondary,
                disabledPlaceholderColor = onBrandSecondary.copy(alpha = 0.5f),
                // Icons inside the field
                focusedLeadingIconColor = onBrand,
                unfocusedLeadingIconColor = onBrandSecondary,
                focusedTrailingIconColor = onBrand,
                unfocusedTrailingIconColor = onBrandSecondary,
            )
        )
    }
}
