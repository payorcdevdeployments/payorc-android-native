package com.payorc.sdk.presentation.embedded.tabby

import ai.tabby.android.data.TabbyResult
import ai.tabby.android.factory.TabbyFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.payorc.sdk.domain.model.PayOrcCheckoutRequest
import com.payorc.sdk.localization.PayorcLanguageProvider
import com.payorc.sdk.presentation.checkout.components.PayOrcText
import com.payorc.sdk.ui.components.PayorcSdkUiConstants

// ---------------------------------------------------------------------------
// Tabby brand colour (purple/indigo used across Tabby's brand assets)
// ---------------------------------------------------------------------------
private val TabbyBrandColor = Color(0xFF3D33A0)

/**
 * # PayorcEmbeddedTabbyButton
 *
 * A self-contained Jetpack Compose button that drives the full Tabby
 * "Buy Now Pay Later" payment flow.
 *
 * ## Merchant Integration
 * ```kotlin
 * PayorcEmbeddedTabbyButton(
 *     paymentRequest = myPaymentRequest,
 *     onTabbyAuthorized = {
 *         // Payment confirmed — proceed with your order
 *     },
 *     onTabbyError = { error ->
 *         // Surface the error to the user
 *         Log.e("Checkout", error?.message ?: "Tabby failed")
 *     }
 * )
 * ```
 *
 * ## What it does internally
 * 1. Calls PayOrc backend to initialize a Tabby order (`/sdk/tabby/init`).
 * 2. Sets up the Tabby Android SDK and creates a session.
 * 3. Launches the Tabby native checkout activity.
 * 4. On AUTHORIZED result, calls PayOrc to confirm the payment (`/sdk/tabby/confirm`).
 * 5. Fires [onTabbyAuthorized] on success or [onTabbyError] on any failure.
 *
 * @param paymentRequest     The merchant's checkout request (order, customer, address details).
 * @param onTabbyAuthorized  Invoked when payment is fully confirmed with the PayOrc backend.
 * @param onTabbyError       Invoked on any failure. [Throwable] may be null for business errors.
 * @param modifier           Standard Compose modifier.
 * @param buttonColor        Background colour of the button. Defaults to Tabby brand purple.
 * @param isEnabled          Whether the button is interactive.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PayorcEmbeddedTabbyButton(
    paymentRequest: PayOrcCheckoutRequest,
    onTabbyAuthorized: (Map<String, Any?>) -> Unit = {},
    onTabbyError: (Throwable?) -> Unit,
    modifier: Modifier = Modifier,
    buttonColor: Color = TabbyBrandColor,
    isEnabled: Boolean = true
) {
    // -----------------------------------------------------------------------
    // ViewModel — scoped to this composable's lifecycle, created with a
    // factory so it receives the merchant's paymentRequest directly.
    // -----------------------------------------------------------------------
    val viewModel: EmbeddedTabbyViewModel = viewModel(
        key = "embedded_tabby_${paymentRequest.orderId}",
        factory = EmbeddedTabbyViewModel.Factory(paymentRequest)
    )

    val state by viewModel.state.collectAsStateWithLifecycle()

    // -----------------------------------------------------------------------
    // Activity result launcher for the Tabby native checkout
    // -----------------------------------------------------------------------
    val tabbyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val tabbyResult: TabbyResult? = if (android.os.Build.VERSION.SDK_INT >= 33) {
            result.data?.getParcelableExtra("extra.tabbyResult", TabbyResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra("extra.tabbyResult")
        }
        viewModel.handleTabbyResult(tabbyResult)
    }

    // -----------------------------------------------------------------------
    // React to state transitions
    // -----------------------------------------------------------------------
    LaunchedEffect(state) {
        when (val s = state) {
            is EmbeddedTabbyState.LaunchTabby -> {
                val intent = TabbyFactory.tabby().createCheckoutIntent(s.product)
                tabbyLauncher.launch(intent)
            }
            is EmbeddedTabbyState.Authorized -> {
                onTabbyAuthorized(s.merchantResponse)
                viewModel.reset()
            }
            is EmbeddedTabbyState.Error -> {
                onTabbyError(s.cause ?: Throwable(s.message))
                viewModel.reset()
            }
            else -> { /* Idle / Loading — handled by UI below */ }
        }
    }

    // -----------------------------------------------------------------------
    // UI
    // -----------------------------------------------------------------------
    val isLoading = state is EmbeddedTabbyState.Loading
    val buttonEnabled = isEnabled && !isLoading

    PayorcLanguageProvider {
        Button(
            onClick = { viewModel.startFlow() },
            enabled = buttonEnabled,
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
                    label = "tabby_button_content"
                ) { loading ->
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        PayOrcText(
                            text = "Pay with Tabby",
                            color = Color.White,
                            fontWeight = PayorcSdkUiConstants.buttonFontWeight
                        )
                    }
                }
            }
        }
    }
}
