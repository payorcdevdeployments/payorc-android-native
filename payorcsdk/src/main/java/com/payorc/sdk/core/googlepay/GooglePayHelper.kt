package com.payorc.sdk.core.googlepay

import android.app.Activity
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.PayOrcEnvironment
import com.payorc.sdk.domain.model.GooglePayConfig
import com.payorc.sdk.domain.model.PayOrcCheckoutRequest
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class GooglePayHelper(private val activity: Activity) {

    private val paymentsClient: PaymentsClient by lazy {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(
                if (PayOrcSdk.instance.environment == PayOrcEnvironment.PRODUCTION)
                    WalletConstants.ENVIRONMENT_PRODUCTION
                else
                    WalletConstants.ENVIRONMENT_TEST
            )
            .build()
        Wallet.getPaymentsClient(activity, walletOptions)
    }

    private val baseRequest = JSONObject().apply {
        put("apiVersion", 2)
        put("apiVersionMinor", 0)
    }

    private fun getGatewayTokenizationSpecification(config: GooglePayConfig): JSONObject {
        // Log all possible identifiers to help troubleshooting
        Log.d("GooglePayHelper", """
            ========== MERCHANT CONFIG DEBUG ==========
            gatewayMerchantId: ${config.gatewayMerchantId}
            merchantId: ${config.merchantId}
            psp: ${config.psp}
            merchantName: ${config.merchantName}
            countryCode: ${config.countryCode}
            PayOrcSdk.merchantKey: ${PayOrcSdk.instance.merchantKey}
            ==========================================
        """.trimIndent())
        
        // Strategy: Try gatewayMerchantId first (it should be the main gateway merchant ID)
        // If empty, try merchantId, if empty use merchantKey
        val effectiveId = when {
            config.gatewayMerchantId.isNotBlank() && config.gatewayMerchantId.length > 2 -> {
                Log.d("GooglePayHelper", "Using gatewayMerchantId: ${config.gatewayMerchantId}")
                config.gatewayMerchantId
            }
            config.merchantId.isNotBlank() && config.merchantId.length > 2 -> {
                Log.d("GooglePayHelper", "Using merchantId: ${config.merchantId}")
                config.merchantId
            }
            else -> {
                Log.w("GooglePayHelper", "Both IDs empty/invalid, falling back to PayOrcSdk.merchantKey: ${PayOrcSdk.instance.merchantKey}")
                PayOrcSdk.instance.merchantKey
            }
        }
        
        // Use psp as gateway name if provided, otherwise "payorc"
        val gatewayName = if (!config.psp.isNullOrBlank()) {
            Log.d("GooglePayHelper", "Using PSP as gateway: ${config.psp}")
            config.psp.lowercase()
        } else {
            Log.w("GooglePayHelper", "PSP not provided, defaulting to 'payorc'. This may cause OR BIBED 06 error if 'payorc' is not registered with Google Pay.")
            "payorc"
        }
        
        Log.d("GooglePayHelper", "Final Gateway Config -> Gateway: $gatewayName, MerchantID: $effectiveId")
        
        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject().apply {
                put("gateway", gatewayName)
                put("gatewayMerchantId", effectiveId)
            })
        }
    }

    private val allowedCardNetworks = JSONArray(listOf("AMEX", "DISCOVER", "INTERAC", "JCB", "MASTERCARD", "VISA"))
    private val allowedCardAuthMethods = JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS"))

    private fun getBaseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {
            put("type", "CARD")
            put("parameters", JSONObject().apply {
                put("allowedAuthMethods", allowedCardAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
            })
        }
    }

    private fun getCardPaymentMethod(config: GooglePayConfig): JSONObject {
        val cardPaymentMethod = getBaseCardPaymentMethod()
        cardPaymentMethod.put("tokenizationSpecification", getGatewayTokenizationSpecification(config))
        return cardPaymentMethod
    }

    suspend fun isReadyToPay(config: GooglePayConfig): Boolean {
        val isReadyToPayRequestJson = JSONObject(baseRequest.toString())
        isReadyToPayRequestJson.put("allowedPaymentMethods", JSONArray().put(getBaseCardPaymentMethod()))
        val request = IsReadyToPayRequest.fromJson(isReadyToPayRequestJson.toString())
        
        return try {
            paymentsClient.isReadyToPay(request).await()
        } catch (e: ApiException) {
            Log.e("GooglePayHelper", "isReadyToPay failed", e)
            false
        }
    }

    fun createPaymentDataRequest(request: PayOrcCheckoutRequest, config: GooglePayConfig): String {
        val paymentDataRequest = JSONObject(baseRequest.toString())
        paymentDataRequest.put("allowedPaymentMethods", JSONArray().put(getCardPaymentMethod(config)))
        paymentDataRequest.put("transactionInfo", JSONObject().apply {
            put("totalPrice", request.amount.toPlainString())
            put("totalPriceStatus", "FINAL")
            put("currencyCode", request.currency)
            // Use country code from config if available, otherwise default to "SA"
            put("countryCode", config.countryCode ?: "SA")
        })
        paymentDataRequest.put("merchantInfo", JSONObject().apply {
            // Use merchant name from config if available, otherwise default to "PayOrc Merchant"
            put("merchantName", config.merchantName ?: "PayOrc Merchant")
        })
        
        // Comprehensive logging for debugging merchant configuration issues
        Log.d("GooglePayHelper", """
            ========== PAYMENT REQUEST DEBUG ==========
            Amount: ${request.amount.toPlainString()}
            Currency: ${request.currency}
            Merchant Name: ${config.merchantName ?: "PayOrc Merchant"}
            Country Code: ${config.countryCode ?: "SA"}
            Complete Request JSON:
            ${paymentDataRequest.toString(2)}
            ==========================================
        """.trimIndent())
        
        return paymentDataRequest.toString()
    }

    fun getPaymentLauncher(): PaymentsClient = paymentsClient

    /**
     * Returns the allowed payment methods JSON string required by PayButton.
     */
    fun getAllowedPaymentMethodsJson(): String {
        return JSONArray().put(getBaseCardPaymentMethod()).toString()
    }
}
