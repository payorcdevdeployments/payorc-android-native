package com.payorc.sdk.presentation.checkout.components

import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
import com.google.android.gms.wallet.button.PayButton
import com.payorc.sdk.core.googlepay.GooglePayHelper
import com.payorc.sdk.domain.model.GooglePayConfig
import com.payorc.sdk.domain.model.PayOrcCheckoutRequest
import kotlinx.coroutines.launch

/**
 * Official Google Pay Button Widget
 * 
 * Uses the official Google Pay button provided by Google Play Services Wallet API.
 * 
 * @param googlePayConfig Configuration for Google Pay (merchant ID, gateway, etc.)
 * @param checkoutRequest The current checkout request with payment details
 * @param onPaymentSuccess Callback when payment succeeds
 * @param onPaymentError Callback when payment fails
 * @param isEnabled Whether the button is enabled for interaction
 * @param modifier Modifier for layout customization
 */
@Composable
fun GooglePayButtonWidget(
    googlePayConfig: GooglePayConfig,
    checkoutRequest: PayOrcCheckoutRequest,
    modifier: Modifier = Modifier,
    onPaymentSuccess: () -> Unit = {},
    onPaymentError: (String) -> Unit = {},
    isEnabled: Boolean = true
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    
    val googlePayHelper = remember {
        GooglePayHelper(context as Activity)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PayButton(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    initialize(
                        ButtonOptions.newBuilder()
                            .setAllowedPaymentMethods(googlePayHelper.getAllowedPaymentMethodsJson())
                            .setButtonType(ButtonConstants.ButtonType.PAY)
                            .setButtonTheme(ButtonConstants.ButtonTheme.DARK)
                            .build()
                    )
                    setOnClickListener {
                        if (isEnabled && !isLoading) {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    Log.d("GooglePayButton", "Official Google Pay button clicked")
                                    
                                    // Create the payment request
                                    val requestJson = googlePayHelper.createPaymentDataRequest(
                                        checkoutRequest, 
                                        googlePayConfig
                                    )
                                    val paymentDataRequest = PaymentDataRequest.fromJson(requestJson)
                                    
                                    // Launch Google Pay
                                    AutoResolveHelper.resolveTask(
                                        googlePayHelper.getPaymentLauncher().loadPaymentData(paymentDataRequest),
                                        context as Activity,
                                        991
                                    )
                                    
                                    Log.d("GooglePayButton", "Google Pay payment initiated")
                                    onPaymentSuccess()
                                } catch (e: Exception) {
                                    Log.e("GooglePayButton", "Failed to initiate Google Pay: ${e.message}", e)
                                    
                                    // Detect common error patterns
                                    val errorMessage = when {
                                        e.message?.contains("OR BIBED 06") == true -> {
                                            "Merchant not configured for Google Pay. Please contact support and verify Paymob account has Google Pay enabled for merchant ID 53609."
                                        }
                                        e.message?.contains("DEVELOPER_ERROR") == true -> {
                                            "Configuration error. Check that PSP gateway and merchant IDs are correct."
                                        }
                                        e.message?.contains("NOT_READY") == true -> {
                                            "Device or account not ready for Google Pay."
                                        }
                                        else -> e.message ?: "Unknown Google Pay error"
                                    }
                                    
                                    Log.e("GooglePayButton", "Processed Error: $errorMessage")
                                    onPaymentError(errorMessage)
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    }
                }
            },
            update = { view ->
                view.isEnabled = isEnabled && !isLoading
            }
        )
    }
}
