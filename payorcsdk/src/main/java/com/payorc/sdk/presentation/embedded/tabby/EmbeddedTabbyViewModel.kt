package com.payorc.sdk.presentation.embedded.tabby

import ai.tabby.android.data.TabbyResult
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.payorc.sdk.PayOrcEnvironment
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.data.mapper.CheckoutMapper
import com.payorc.sdk.data.mapper.TabbyMerchantResponseMapper
import com.payorc.sdk.data.remote.dto.TabbyConfirmResponseDto
import com.payorc.sdk.data.repository.WalletPaymentRepositoryImpl
import com.payorc.sdk.domain.model.PayOrcCheckoutRequest
import com.payorc.sdk.domain.model.TabbyBuyer
import com.payorc.sdk.domain.model.TabbyConfig
import com.payorc.sdk.domain.usecase.CreateTabbySessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "EmbeddedTabbyVM"

/**
 * Self-contained ViewModel for the embedded Tabby payment button.
 *
 * Unlike [com.payorc.sdk.presentation.checkout.TabbyPaymentViewModel], this ViewModel:
 * - Accepts [paymentRequest] directly instead of reading from global SDK state.
 * - Reports results via [EmbeddedTabbyState] rather than through checkout-scoped callbacks.
 * - Automatically resolves the correct merchant_code from the backend (same as the checkout sheet).
 * - Can be instantiated independently of [CheckoutActivity].
 *
 * All API, repository, and use-case layers are reused verbatim from the existing SDK.
 *
 * @param paymentRequest The merchant's checkout request for this payment.
 */
class EmbeddedTabbyViewModel(
    private val paymentRequest: PayOrcCheckoutRequest
) : ViewModel() {

    // Repositories and use-cases — reused from existing SDK layers
    private val walletRepo = WalletPaymentRepositoryImpl(PayOrcSdk.instance.context)
    private val createTabbySessionUseCase = CreateTabbySessionUseCase(walletRepo)

    private val _state = MutableStateFlow<EmbeddedTabbyState>(EmbeddedTabbyState.Idle)
    val state: StateFlow<EmbeddedTabbyState> = _state.asStateFlow()

    // Persisted across async steps
    private var payorcOrderId: String? = null
    private var tabbySession: ai.tabby.android.data.TabbySession? = null

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Begin the Tabby payment flow.
     * Safe to call multiple times — re-entrant calls while [EmbeddedTabbyState.Loading]
     * or [EmbeddedTabbyState.LaunchTabby] are no-ops.
     */
    fun startFlow() {
        val current = _state.value
        if (current is EmbeddedTabbyState.Loading || current is EmbeddedTabbyState.LaunchTabby) {
            Log.w(TAG, "startFlow() ignored — flow already in progress: $current")
            return
        }
        viewModelScope.launch { runFlow() }
    }

    /**
     * Handle the result returned by the Tabby native checkout activity.
     * Must be called after the merchant activity launcher receives its result.
     */
    fun handleTabbyResult(result: TabbyResult?) {
        Log.d(TAG, "handleTabbyResult: result=${result?.result}")

        if (result == null) {
            emitError("Tabby returned no result", null)
            return
        }

        when (result.result) {
            TabbyResult.Result.AUTHORIZED -> {
                Log.d(TAG, "Tabby AUTHORIZED — confirming with PayOrc backend")
                viewModelScope.launch { confirmPayment() }
            }
            TabbyResult.Result.REJECTED -> emitError("Tabby payment was declined", null)
            TabbyResult.Result.CLOSED    -> emitError("Tabby payment was cancelled", null)
            TabbyResult.Result.EXPIRED   -> emitError("Tabby session expired", null)
        }
    }

    /**
     * Reset state to [EmbeddedTabbyState.Idle] so the button can be tapped again.
     */
    fun reset() {
        _state.value = EmbeddedTabbyState.Idle
        payorcOrderId = null
        tabbySession = null
    }

    // -------------------------------------------------------------------------
    // Private flow steps
    // -------------------------------------------------------------------------

    private suspend fun runFlow() {
        try {
            // Step 1 — Resolve TabbyConfig from the backend (same call as the checkout sheet)
            // This ensures merchant_code is always "Payorc AE" (or whatever the server returns)
            // rather than a hardcoded fallback.
            _state.value = EmbeddedTabbyState.Loading("Loading payment config...")
            val tabbyConfig = resolveTabbyConfig() ?: run {
                emitError("Tabby is not available for this merchant", null)
                return
            }
            Log.d(TAG, "Resolved TabbyConfig: merchantCode=${tabbyConfig.merchantCode}")

            // Step 2 — Initialize Tabby payment with PayOrc backend
            _state.value = EmbeddedTabbyState.Loading("Initializing Tabby...")
            val sdk = PayOrcSdk.instance
            sdk.setCheckoutRequest(paymentRequest)

            val initResponse = sdk.payOrcRepo.initializeTabbyPayment(
                checkoutRequest = paymentRequest,
                merchantCode    = tabbyConfig.merchantCode
            )

            if (!initResponse.isSuccess) {
                emitError(initResponse.message ?: "Failed to initialize Tabby payment", null)
                return
            }

            payorcOrderId = initResponse.data?.orderId?.toString()
            if (payorcOrderId.isNullOrBlank()) {
                emitError("PayOrc did not return an order ID", null)
                return
            }
            Log.d(TAG, "Tabby init success — payorcOrderId=$payorcOrderId")

            // Step 3 — Set up Tabby SDK; prefer API key from init response, fall back to config
            _state.value = EmbeddedTabbyState.Loading("Setting up Tabby session...")
            val apiKey = initResponse.data?.password?.takeIf { it.isNotBlank() }
                ?: tabbyConfig.apiKey
            val environment = if (sdk.environment == PayOrcEnvironment.PRODUCTION) "production" else "stage"
            walletRepo.setupTabby(apiKey, environment)

            // Step 4 — Build buyer from payment request
            val buyer = TabbyBuyer(
                email = paymentRequest.customerEmail,
                phone = paymentRequest.customerMobileCode + paymentRequest.customerMobile,
                name  = paymentRequest.customerName
            )

            // Step 5 — Create Tabby session
            _state.value = EmbeddedTabbyState.Loading("Creating Tabby session...")
            val sessionResult = createTabbySessionUseCase(
                merchantCode  = tabbyConfig.merchantCode,
                amount        = paymentRequest.amount.toPlainString(),
                currency      = paymentRequest.currency,
                buyer         = buyer,
                orderId       = paymentRequest.orderId,
                payorcOrderId = payorcOrderId
            )

            sessionResult
                .onSuccess { session ->
                    tabbySession = session
                    val product = session.availableProducts.firstOrNull()
                    if (product != null) {
                        Log.d(TAG, "Tabby session created — launching checkout")
                        _state.value = EmbeddedTabbyState.LaunchTabby(product)
                    } else {
                        emitError("No Tabby products available for this transaction", null)
                    }
                }
                .onFailure { ex ->
                    Log.e(TAG, "Tabby session creation failed", ex)
                    emitError("Failed to create Tabby session: ${ex.message}", ex)
                }

        } catch (ex: Exception) {
            Log.e(TAG, "Unexpected error during Tabby flow", ex)
            emitError("Unexpected error: ${ex.message}", ex)
        }
    }

    /**
     * Fetch [TabbyConfig] from the checkout customization API — the same endpoint
     * the checkout sheet calls. This guarantees [TabbyConfig.merchantCode] is always
     * the server-side value (e.g. "Payorc AE") with no hardcoded fallbacks.
     */
    private suspend fun resolveTabbyConfig(): TabbyConfig? {
        return try {
            val response = PayOrcSdk.instance.payOrcRepo.fetchCheckoutCustomization(
                currency = paymentRequest.currency,
                amount   = paymentRequest.amount.toDouble()
            )
            if (!response.isSuccess) {
                Log.w(TAG, "Customization fetch failed: ${response.message}")
                return null
            }
            val config = CheckoutMapper.mapToDomain(response)
            config.paymentMethods.find { it.type == "TABBY" }?.tabbyConfig
                ?: run {
                    Log.w(TAG, "No TABBY method in customization response")
                    null
                }
        } catch (ex: Exception) {
            Log.e(TAG, "Error fetching customization for TabbyConfig", ex)
            null
        }
    }

    private suspend fun confirmPayment() {
        _state.value = EmbeddedTabbyState.Loading("Confirming payment...")
        try {
            val orderId = payorcOrderId
            if (orderId.isNullOrBlank()) {
                emitError("Missing order ID for confirmation", null)
                return
            }

            val tabbyPaymentId = tabbySession?.paymentId
            if (tabbyPaymentId.isNullOrBlank()) {
                emitError("Missing Tabby payment ID for confirmation", null)
                return
            }

            val confirmResponse = PayOrcSdk.instance.payOrcRepo.confirmTabbyPayment(
                orderId        = orderId,
                tabbyPaymentId = tabbyPaymentId
            )

            if (confirmResponse.isSuccess) {
                Log.d(TAG, "Payment confirmed — tabbyPaymentId=$tabbyPaymentId, orderId=$orderId")
                _state.value = EmbeddedTabbyState.Authorized(
                    tabbyPaymentId = tabbyPaymentId,
                    payorcOrderId  = orderId,
                    merchantResponse = TabbyMerchantResponseMapper.toMerchantResponse(confirmResponse)
                )
            } else {
                emitError(confirmResponse.message ?: "Payment confirmation failed", null)
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error during payment confirmation", ex)
            emitError("Confirmation error: ${ex.message}", ex)
        }
    }

    private fun emitError(message: String, cause: Throwable?) {
        Log.e(TAG, "EmbeddedTabby error: $message", cause)
        _state.value = EmbeddedTabbyState.Error(message = message, cause = cause)
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Factory for creating [EmbeddedTabbyViewModel] with the merchant's [paymentRequest].
     *
     * Usage:
     * ```kotlin
     * val vm: EmbeddedTabbyViewModel = viewModel(
     *     factory = EmbeddedTabbyViewModel.Factory(paymentRequest)
     * )
     * ```
     */
    class Factory(
        private val paymentRequest: PayOrcCheckoutRequest
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(EmbeddedTabbyViewModel::class.java)) {
                return EmbeddedTabbyViewModel(paymentRequest) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
