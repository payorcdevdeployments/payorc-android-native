package com.payorc.sdk.presentation.checkout.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isUnspecified
import com.payorc.sdk.localization.PayorcLocalization
import com.payorc.sdk.ui.components.PayorcSdkUiConstants

/**
 * A wrapper for Jetpack Compose Text that automatically respects
 * SDK-wide customizations like letter spacing, max lines, font weight, etc.
 */
@Composable
fun PayOrcText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PayorcSdkUiConstants.textPrimaryColor,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow? = null,
    softWrap: Boolean? = null,
    maxLines: Int? = null,
    minLines: Int = 1,
    localize: Boolean = true,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    style: TextStyle = LocalTextStyle.current
) {
    // Determine the resolved styling values
    
    val resolvedFontStyle = fontStyle ?: when (PayorcSdkUiConstants.textFontStyle?.lowercase()) {
        "italic" -> FontStyle.Italic
        "normal" -> FontStyle.Normal
        else -> null
    }
    
    val resolvedFontWeight = fontWeight ?: PayorcSdkUiConstants.textFontWeight
    
    val resolvedFontSize = if (fontSize != TextUnit.Unspecified) fontSize else PayorcSdkUiConstants.textFontSize
    
    val resolvedLetterSpacing = if (letterSpacing != TextUnit.Unspecified) {
        letterSpacing
    } else {
        PayorcSdkUiConstants.textLetterSpacing ?: TextUnit.Unspecified
    }

    val resolvedMaxLines = maxLines ?: PayorcSdkUiConstants.textMaxLines ?: Int.MAX_VALUE
    
    val resolvedOverflow = overflow ?: when (PayorcSdkUiConstants.textOverflow?.lowercase()) {
        "clip" -> TextOverflow.Clip
        "ellipsis" -> TextOverflow.Ellipsis
        "visible" -> TextOverflow.Visible
        else -> TextOverflow.Clip
    }

    val resolvedSoftWrap = softWrap ?: PayorcSdkUiConstants.merchantTextConfig?.softWrap ?: true

    val resolvedDecoration = textDecoration ?: when (PayorcSdkUiConstants.merchantTextConfig?.decoration?.lowercase()) {
        "none" -> TextDecoration.None
        "underline" -> TextDecoration.Underline
        "linethrough" -> TextDecoration.LineThrough
        else -> null
    }

    val resolvedTextAlign = textAlign ?: when (PayorcSdkUiConstants.merchantTextConfig?.textAlign) {
        TextAlign.Left -> TextAlign.Left
        TextAlign.Right -> TextAlign.Right
        TextAlign.Center -> TextAlign.Center
        TextAlign.Justify -> TextAlign.Justify
        TextAlign.Start -> TextAlign.Start
        TextAlign.End -> TextAlign.End
        else -> null
    }

    val resolvedWordSpacing = PayorcSdkUiConstants.textWordSpacing ?: TextUnit.Unspecified
    
    // Merge everything into a final TextStyle to apply parameters cleanly
    val finalStyle = style.copy(
        fontSize = resolvedFontSize,
        fontWeight = resolvedFontWeight,
        fontStyle = resolvedFontStyle,
        fontFamily = fontFamily,
        letterSpacing = resolvedLetterSpacing,
        textDecoration = resolvedDecoration,
        textAlign = resolvedTextAlign ?: style.textAlign ?: TextAlign.Unspecified,
        lineHeight = lineHeight
    )
    
    Text(
        text = (if (localize) PayorcLocalization.localize(text) else text).withWordSpacing(resolvedWordSpacing),
        modifier = modifier,
        color = color,
        overflow = resolvedOverflow,
        softWrap = resolvedSoftWrap,
        maxLines = resolvedMaxLines,
        minLines = minLines,
        onTextLayout = onTextLayout ?: {},
        style = finalStyle
    )
}

private fun String.withWordSpacing(wordSpacing: TextUnit): AnnotatedString {
    if (wordSpacing.isUnspecified || wordSpacing.value == 0f || none { it.isWhitespace() }) {
        return AnnotatedString(this)
    }

    val builder = AnnotatedString.Builder(this)
    forEachIndexed { index, char ->
        if (char.isWhitespace()) {
            builder.addStyle(
                style = SpanStyle(letterSpacing = wordSpacing),
                start = index,
                end = index + 1
            )
        }
    }
    return builder.toAnnotatedString()
}
