package com.payorc.sdk.presentation.embedded.googlepay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.core.googlepay.GooglePayHelper
import com.payorc.sdk.domain.model.GooglePayConfig

class PayOrcGooglePayDirectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()
        val paymentRequest = PayOrcSdk.instance.currentCheckoutRequest

        if (paymentRequest == null) {
            completeWithError(requestId, IllegalStateException("Missing current checkout request"))
            return
        }

        val googlePayConfig = buildGooglePayConfig()
        if (googlePayConfig == null) {
            completeWithError(requestId, IllegalStateException("Google Pay configuration is missing"))
            return
        }

        launchGooglePay(paymentRequest, googlePayConfig, requestId)
    }

    private fun buildGooglePayConfig(): GooglePayConfig? {
        val checkoutCustomization = PayOrcSdk.instance.checkoutCustomization ?: return null
        val googlePayMethod = checkoutCustomization.availableMethods
            .firstOrNull { it.type.equals("GOOGLE_PAY", ignoreCase = true) }
            ?: return null

        return GooglePayConfig(
            merchantId = googlePayMethod.merchantId,
            gatewayMerchantId = googlePayMethod.gatewayMerchantId,
            psp = googlePayMethod.psp.ifBlank { null },
            merchantName = checkoutCustomization.merchantDetails?.merchantName,
            countryCode = null
        )
    }

    private fun launchGooglePay(
        paymentRequest: com.payorc.sdk.domain.model.PayOrcCheckoutRequest,
        googlePayConfig: GooglePayConfig,
        requestId: String
    ) {
        val googlePayHelper = GooglePayHelper(this)

        try {
            val requestJson = googlePayHelper.createPaymentDataRequest(paymentRequest, googlePayConfig)
            val paymentDataRequest = PaymentDataRequest.fromJson(requestJson)
            AutoResolveHelper.resolveTask(
                googlePayHelper.getPaymentLauncher().loadPaymentData(paymentDataRequest),
                this,
                REQUEST_CODE_GOOGLE_PAY
            )
        } catch (e: Exception) {
            completeWithError(requestId, e)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != REQUEST_CODE_GOOGLE_PAY) {
            return
        }

        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()

        when (resultCode) {
            Activity.RESULT_OK -> {
                val paymentData = data?.let { PaymentData.getFromIntent(it) }
                val paymentJson = paymentData?.toJson()
                completeWithSuccess(requestId, mapOf(
                    "status" to "success",
                    "paymentData" to paymentJson
                ))
            }
            Activity.RESULT_CANCELED -> {
                completeWithError(requestId, Throwable("Google Pay was cancelled by the user"))
            }
            AutoResolveHelper.RESULT_ERROR -> {
                val status = data?.let { AutoResolveHelper.getStatusFromIntent(it) }
                val statusMessage = status?.statusMessage ?: "Google Pay failed"
                completeWithError(requestId, Throwable(statusMessage))
            }
            else -> {
                completeWithError(requestId, Throwable("Unexpected Google Pay result: $resultCode"))
            }
        }

        finish()
    }

    private fun completeWithSuccess(requestId: String, result: Map<String, Any?>) {
        PayOrcSdk.completeGooglePayFlow(requestId, result = result)
    }

    private fun completeWithError(requestId: String, error: Throwable) {
        PayOrcSdk.completeGooglePayFlow(requestId, error = error)
        finish()
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra.payorc.googlepay.request_id"
        private const val REQUEST_CODE_GOOGLE_PAY = 1001
    }
}
