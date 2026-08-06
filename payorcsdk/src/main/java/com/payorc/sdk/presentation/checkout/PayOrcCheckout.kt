package com.payorc.sdk.presentation.checkout

import android.content.Context
import androidx.compose.runtime.Composable
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.domain.model.PayOrcCheckoutRequest

import com.payorc.sdk.domain.model.PaymentTokenData

/**
 * Callback for the checkout process.
 */
interface PayOrcCheckoutCallback {
    fun onAddCardSuccess(tokenData: PaymentTokenData) {}
    fun onSuccessPayment(merchantResponse: Map<String, Any?>) {}
    fun onFailure(reason: String)
}

interface PayOrc3DSCallback {
    fun onSuccess(transactionId: String)
    fun onFailure(reason: String)
}

/**
 * Handles the checkout process.
 */
class PayOrcCheckout {

    companion object {
        internal var currentCallback: PayOrcCheckoutCallback? = null
        internal var current3DSCallback: PayOrc3DSCallback? = null

        /**
         * Starts the checkout process.
         * @param context The context from which to start the checkout.
         * @param request The checkout request containing transaction details.
         * @param callback The callback to receive checkout results.
         * @param launchMethod Optional method to launch directly (e.g. "CARD" for direct card entry)
         */
        fun start(
            context: Context, 
            request: PayOrcCheckoutRequest, 
            callback: PayOrcCheckoutCallback,
            launchMethod: String? = null
        ) {
            // Verify initialization
            PayOrcSdk.instance.setCheckoutRequest(request)
            currentCallback = callback
            
            val intent = android.content.Intent(context, CheckoutActivity::class.java).apply {
                launchMethod?.let { putExtra("EXTRA_LAUNCH_METHOD", it) }
            }
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        /**
         * A Composable for developers using Jetpack Compose who want to embed 
         * the checkout directly into their own UI/Sheets.
         */
        @Composable
        fun PayOrcCheckoutSheet(
            request: PayOrcCheckoutRequest,
            onClose: () -> Unit,
            onSuccessPayment: (Map<String, Any?>) -> Unit,
            onFailure: (String) -> Unit
        ) {
            PayOrcSdk.instance.setCheckoutRequest(request)
            currentCallback = object : PayOrcCheckoutCallback {
                override fun onSuccessPayment(merchantResponse: Map<String, Any?>) = onSuccessPayment(merchantResponse)
                override fun onFailure(reason: String) = onFailure(reason)
            }
            
            PayOrcCheckoutContent(onClose = onClose)
        }

        /**
         * Starts a standalone 3DS authentication flow.
         * @param context The context from which to start.
         * @param redirectUrl The 3DS redirect URL.
         * @param callback The callback to receive 3DS results.
         */
        fun start3DS(
            context: Context,
            redirectUrl: String,
            callback: PayOrc3DSCallback
        ) {
            current3DSCallback = callback
            val intent = android.content.Intent(context, PayOrc3DSActivity::class.java).apply {
                putExtra("REDIRECT_URL", redirectUrl)
            }
            context.startActivity(intent)
        }
    }
}
