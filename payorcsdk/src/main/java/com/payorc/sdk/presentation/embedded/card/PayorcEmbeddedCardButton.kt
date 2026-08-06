package com.payorc.sdk.presentation.embedded.card

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.payorc.sdk.domain.model.PayOrcCheckoutRequest
import com.payorc.sdk.domain.model.PaymentTokenData
import com.payorc.sdk.localization.PayorcLanguageProvider
import com.payorc.sdk.presentation.checkout.PayOrcCheckout
import com.payorc.sdk.presentation.checkout.PayOrcCheckoutCallback
import com.payorc.sdk.presentation.checkout.components.PayOrcText
import com.payorc.sdk.ui.components.PayorcSdkUiConstants

/**
 * # PayorcEmbeddedCardButton
 *
 * A self-contained Jetpack Compose button that launches the card payment flow
 * using the standard PayOrc bottom sheet.
 *
 * ## Merchant Integration
 * ```kotlin
 * PayorcEmbeddedCardButton(
 *     paymentRequest = myPaymentRequest,
 *     onPaymentSuccess = { merchantResponse ->
 *         // Payment confirmed
 *     },
 *     onPaymentError = { reason ->
 *         // Surface the error
 *     }
 * )
 * ```
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PayorcEmbeddedCardButton(
    paymentRequest: PayOrcCheckoutRequest,
    onPaymentSuccess: (Map<String, Any?>) -> Unit,
    onPaymentError: (String) -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = PayorcSdkUiConstants.merchantButtonConfig?.backgroundColor ?: Color(0xFF0052CC),
    isEnabled: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    // Reset isLoading whenever the host activity pauses (meaning the checkout sheet has opened)
    // or resumes (if we need a safety reset).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_RESUME) && isLoading) {
                isLoading = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PayorcLanguageProvider {
        Button(
            onClick = {
                isLoading = true
                PayOrcCheckout.start(
                    context = context,
                    request = paymentRequest,
                    callback = object : PayOrcCheckoutCallback {
                        override fun onSuccessPayment(merchantResponse: Map<String, Any?>) {
                            isLoading = false
                            onPaymentSuccess(merchantResponse)
                        }

                        override fun onFailure(reason: String) {
                            isLoading = false
                            onPaymentError(reason)
                        }
                    },
                    launchMethod = "CARD"
                )
            },
            enabled = isEnabled && !isLoading,
            modifier = modifier
                .fillMaxWidth()
                .height(PayorcSdkUiConstants.buttonHeight),
            shape = RoundedCornerShape(PayorcSdkUiConstants.buttonBorderRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                disabledContainerColor = PayorcSdkUiConstants.buttonDisabledBackgroundColor,
                disabledContentColor = PayorcSdkUiConstants.buttonDisabledForegroundColor
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = isLoading,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith
                            fadeOut(animationSpec = tween(200))
                    },
                    label = "card_button_content"
                ) { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        PayOrcText(
                            text = "Pay with Card",
                            color = Color.White,
                            fontWeight = PayorcSdkUiConstants.buttonFontWeight
                        )
                    }
                }
            }
        }
    }
}
