package com.payorc.sdk

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.payorc.sdk.core.network.PayOrcGatewayUrls
import com.payorc.sdk.data.remote.api.PayOrcApiService
import com.payorc.sdk.data.repository.PayOrcRepositoryImpl
import com.payorc.sdk.domain.model.*
import com.payorc.sdk.localization.PayorcLocalization
import com.payorc.sdk.domain.repository.PayOrcRepository
import com.payorc.sdk.ui.components.PayorcSdkUiConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class PayOrcEnvironment {
    SANDBOX, PRODUCTION
}

enum class PayorcInputBorderStyle {
    outline, underline
}


enum class PayorcLanguage {
    english, arabic
}

class PayOrcSdk private constructor(
    val merchantKey: String,
    val merchantSecret: String,
    val environment: PayOrcEnvironment,
    val appId: String? = null,
    val appVersion: String? = null,
    val context: Context
) {
    private data class PendingTabbyRequest(
        val launchContext: Context,
        val onAuthorized: (Context, Map<String, Any?>) -> Unit,
        val onError: (Throwable?) -> Unit
    )

    private data class PendingGooglePayRequest(
        val launchContext: Context,
        val onResult: (Context, Map<String, Any?>) -> Unit,
        val onError: (Throwable?) -> Unit
    )

    private val pendingTabbyRequests = ConcurrentHashMap<String, PendingTabbyRequest>()
    private val pendingGooglePayRequests = ConcurrentHashMap<String, PendingGooglePayRequest>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    internal val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
        encodeDefaults = true
    }
    
    private val okHttpClient = OkHttpClient.Builder().apply {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        addInterceptor(loggingInterceptor)
    }.build()
    
    private val baseUrl = if (environment == PayOrcEnvironment.PRODUCTION) {
        PayOrcGatewayUrls.PRODUCTION_BASE_URL
    } else {
        PayOrcGatewayUrls.SANDBOX_BASE_URL
    }

    private val apiService: PayOrcApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(PayOrcApiService::class.java)

    val payOrcRepo: PayOrcRepository = PayOrcRepositoryImpl(apiService, merchantKey, merchantSecret, context)

    var checkoutCustomization: CheckoutCustomizationData? = null
        private set
        
    var currentCheckoutRequest: PayOrcCheckoutRequest? = null
        private set

    var clientIp: String? = null
    var clientIpCountry: String? = null

    fun getDeviceHeaders(context: Context): Map<String, String> {
        return com.payorc.sdk.core.network.DeviceMetadataProvider.getHeaders(
            context = context,
            appIdOverride = appId,
            appVersionOverride = appVersion,
            clientIp = clientIp,
            clientIpCountry = clientIpCountry
        )
    }

    fun setCheckoutRequest(request: PayOrcCheckoutRequest) {
        this.currentCheckoutRequest = request
    }

    companion object {
        @Volatile
        private var INSTANCE: PayOrcSdk? = null

        @JvmStatic
        fun tabby(
            context: Context,
            paymentRequest: PayOrcCheckoutRequest,
            onTabbyAuthorized: (Context, Map<String, Any?>) -> Unit,
            onTabbyError: (Throwable?) -> Unit
        ) {
            instance.launchTabby(context, paymentRequest, onTabbyAuthorized, onTabbyError)
        }

        @JvmStatic
        fun googlePay(
            context: Context,
            paymentRequest: PayOrcCheckoutRequest,
            onGooglePayAuthorized: (Context, Map<String, Any?>) -> Unit,
            onGooglePayError: (Throwable?) -> Unit
        ) {
            instance.launchGooglePay(context, paymentRequest, onGooglePayAuthorized, onGooglePayError)
        }

        internal fun completeTabbyFlow(
            requestId: String,
            merchantResponse: Map<String, Any?>? = null,
            error: Throwable? = null
        ) {
            val sdk = INSTANCE ?: return
            val pendingRequest = sdk.pendingTabbyRequests.remove(requestId) ?: return

            sdk.scope.launch {
                if (error != null) {
                    pendingRequest.onError(error)
                } else {
                    pendingRequest.onAuthorized(pendingRequest.launchContext, merchantResponse ?: emptyMap())
                }
            }
        }

        internal fun completeGooglePayFlow(
            requestId: String,
            result: Map<String, Any?>? = null,
            error: Throwable? = null
        ) {
            val sdk = INSTANCE ?: return
            val pendingRequest = sdk.pendingGooglePayRequests.remove(requestId) ?: return

            sdk.scope.launch {
                if (error != null) {
                    pendingRequest.onError(error)
                } else {
                    pendingRequest.onResult(pendingRequest.launchContext, result ?: emptyMap())
                }
            }
        }

        private var _language by mutableStateOf(PayorcLanguage.english)

        val currentLanguage: PayorcLanguage
            get() = _language

        val currentLocale: Locale
            get() = PayorcLocalization.currentLocale

        fun setLanguage(language: PayorcLanguage) {
            _language = language
        }

        fun init(
            context: Context,
            merchantKey: String,
            merchantSecret: String,
            environment: PayOrcEnvironment,
            language: PayorcLanguage = PayorcLanguage.english,
            appId: String? = null,
            appVersion: String? = null,
            fetchCheckoutCustomizationOnInit: Boolean = true,
            checkoutCustomizationCurrency: String = "AED",
            checkoutCustomizationAmount: Double = 1.0
        ): PayOrcSdk {
            INSTANCE?.let { existing ->
                _language = language
                return existing
            }

            return synchronized(this) {
                INSTANCE?.let { existing ->
                    _language = language
                    return existing
                }

                val instance = PayOrcSdk(
                    merchantKey = merchantKey,
                    merchantSecret = merchantSecret,
                    environment = environment,
                    appId = appId,
                    appVersion = appVersion,
                    context = context.applicationContext
                )
                INSTANCE = instance
                _language = language

                if (fetchCheckoutCustomizationOnInit) {
                    instance.scope.launch {
                        instance.loadCheckoutCustomization(
                            checkoutCustomizationCurrency,
                            checkoutCustomizationAmount
                        )
                    }
                }
                instance
            }
        }

        val instance: PayOrcSdk
            get() = INSTANCE ?: throw IllegalStateException("PayOrcSdk not initialized. Call init() first.")

        val isInitialized: Boolean
            get() = INSTANCE != null

        /**
         * Comprehensive UI customization method.
         * Allows merchants to override SDK styling for buttons, text, validation messages, etc.
         */
        fun customization(
            inputBorderStyle: PayorcInputBorderStyle? = null,
            guidanceStyle: PayorcGuidanceStyle? = null,
            appBarStyle: PayorcAppBarStyle? = null,
            brandColor: Color? = null,
            button: PayorcSdkButtonCustomization? = null,
            accentColor: Color? = null,
            text: PayorcSdkTextCustomization? = null,
            textPrimary: Color? = null,
            textSecondary: Color? = null,
            cardFormValidation: PayorcSdkCardFormValidationCustomization? = null,
            borderColor: Color? = null,
            addCardForm: PayorcSdkAddCardFormCustomization? = null,
            appTextField: PayorcSdkAppTextFieldCustomization? = null,
            bottomSheet: PayorcSdkBottomSheetCustomization? = null
        ) {
            if (inputBorderStyle != null) PayorcSdkUiConstants.merchantInputBorderStyle = inputBorderStyle
            if (guidanceStyle != null) PayorcSdkUiConstants.merchantGuidanceStyle = guidanceStyle
            if (appBarStyle != null) PayorcSdkUiConstants.merchantAppBarStyle = appBarStyle

            if (brandColor != null && brandColor.alpha != 0f) PayorcSdkUiConstants.merchantBrandColor = brandColor
            if (accentColor != null && accentColor.alpha != 0f) PayorcSdkUiConstants.merchantAccentColor = accentColor
            if (borderColor != null && borderColor.alpha != 0f) PayorcSdkUiConstants.merchantBorderColor = borderColor
            
            if (textPrimary != null && textPrimary.alpha != 0f) PayorcSdkUiConstants.merchantTextPrimaryColor = textPrimary
            if (textSecondary != null && textSecondary.alpha != 0f) PayorcSdkUiConstants.merchantTextSecondaryColor = textSecondary

            if (button != null) PayorcSdkUiConstants.merchantButtonConfig = button
            if (text != null) PayorcSdkUiConstants.merchantTextConfig = text
            if (cardFormValidation != null) PayorcSdkUiConstants.merchantValidationConfig = cardFormValidation
            if (addCardForm != null) PayorcSdkUiConstants.merchantAddCardFormConfig = addCardForm
            if (appTextField != null) PayorcSdkUiConstants.merchantAppTextFieldConfig = appTextField
            if (bottomSheet != null) PayorcSdkUiConstants.merchantBottomSheetConfig = bottomSheet
        }
    }

    fun fetchCheckoutCustomization(
        currency: String = "AED",
        amount: Double = 1.0,
        onComplete: (Boolean) -> Unit = {}
    ) {
        scope.launch {
            loadCheckoutCustomization(currency, amount)
            onComplete(checkoutCustomization != null)
        }
    }

    private suspend fun loadCheckoutCustomization(
        currency: String,
        amount: Double
    ) {
        try {
            val response = payOrcRepo.fetchCheckoutCustomization(currency, amount)
            if (response.isSuccess && response.data != null) {
                checkoutCustomization = response.data
                PayorcSdkUiConstants.applyFromMerchantDetails(response.data.merchantDetails)
                Log.d("PayOrcSdk", "Customization fetched successfully")
            } else {
                Log.e("PayOrcSdk", "Failed to fetch customization: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e("PayOrcSdk", "Error fetching customization", e)
        }
    }

    fun executePayment(
        request: ExecutePaymentRequest,
        onComplete: (PaymentResult) -> Unit
    ) {
        scope.launch {
            try {
                onComplete(PaymentResult.Loading)
                val response = payOrcRepo.submitPayment(request)
                if (response.isSuccess) {
                    if (response.data?.redirectUrl != null) {
                        onComplete(PaymentResult.RedirectTo3DS(response.data.redirectUrl))
                    } else if (response.data?.transactionId != null) {
                        onComplete(PaymentResult.Success(response.data.transactionId))
                    } else {
                        onComplete(PaymentResult.Failure("Missing transaction ID"))
                    }
                } else {
                    onComplete(PaymentResult.Failure(response.message ?: "Payment failed"))
                }
            } catch (e: Exception) {
                Log.e("PayOrcSdk", "Error executing payment", e)
                onComplete(PaymentResult.Failure(e.message ?: "Unknown error executing payment"))
            }
        }
    }

    fun launchTabby(
        context: Context,
        paymentRequest: PayOrcCheckoutRequest,
        onTabbyAuthorized: (Context, Map<String, Any?>) -> Unit,
        onTabbyError: (Throwable?) -> Unit
    ) {
        val requestId = UUID.randomUUID().toString()
        setCheckoutRequest(paymentRequest)
        pendingTabbyRequests[requestId] = PendingTabbyRequest(
            launchContext = context,
            onAuthorized = onTabbyAuthorized,
            onError = onTabbyError
        )

        val intent = Intent(
            context,
            com.payorc.sdk.presentation.embedded.tabby.PayOrcTabbyDirectActivity::class.java
        ).apply {
            putExtra(
                com.payorc.sdk.presentation.embedded.tabby.PayOrcTabbyDirectActivity.EXTRA_REQUEST_ID,
                requestId
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun launchGooglePay(
        context: Context,
        paymentRequest: PayOrcCheckoutRequest,
        onGooglePayResult: (Context, Map<String, Any?>) -> Unit,
        onGooglePayError: (Throwable?) -> Unit
    ) {
        val requestId = UUID.randomUUID().toString()
        setCheckoutRequest(paymentRequest)
        pendingGooglePayRequests[requestId] = PendingGooglePayRequest(
            launchContext = context,
            onResult = onGooglePayResult,
            onError = onGooglePayError
        )

        val intent = Intent(
            context,
            com.payorc.sdk.presentation.embedded.googlepay.PayOrcGooglePayDirectActivity::class.java
        ).apply {
            putExtra(
                com.payorc.sdk.presentation.embedded.googlepay.PayOrcGooglePayDirectActivity.EXTRA_REQUEST_ID,
                requestId
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun fetchSavedCards(
        customerId: String,
        orderId: String,
        onComplete: (SavedCardsResult) -> Unit
    ) {
        scope.launch {
            try {
                val response = payOrcRepo.fetchSavedCards(customerId, orderId)
                onComplete(response)
            } catch (e: Exception) {
                Log.e("PayOrcSdk", "Error fetching saved cards", e)
                onComplete(SavedCardsResult(message = e.message ?: "Unknown error", status = "fail"))
            }
        }
    }
}
