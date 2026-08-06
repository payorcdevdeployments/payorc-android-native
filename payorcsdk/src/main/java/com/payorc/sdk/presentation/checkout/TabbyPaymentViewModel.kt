package com.payorc.sdk.presentation.checkout

import ai.tabby.android.data.Product
import ai.tabby.android.data.TabbyResult
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.domain.model.PaymentTokenData
import com.payorc.sdk.domain.model.TabbyBuyer
import com.payorc.sdk.domain.model.TabbyConfig
import com.payorc.sdk.domain.usecase.CreateTabbySessionUseCase
import com.payorc.sdk.data.repository.WalletPaymentRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Specialized ViewModel for Tabby payment flow.
 * Handles Tabby initialization, session creation, and payment confirmation.
 */
class TabbyPaymentViewModel : ViewModel() {

    private val walletRepo = WalletPaymentRepositoryImpl(PayOrcSdk.instance.context)
    private val createTabbySessionUseCase = CreateTabbySessionUseCase(walletRepo)

    private val _tabbyPaymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val tabbyPaymentState: StateFlow<PaymentState> = _tabbyPaymentState.asStateFlow()

    private val _availableTabbyProducts = MutableStateFlow<List<Product>>(emptyList())
    val availableTabbyProducts: StateFlow<List<Product>> = _availableTabbyProducts.asStateFlow()

    private var currentTabbySession: ai.tabby.android.data.TabbySession? = null
    private var currentPayOrcTabbyOrderId: String? = null

    /**
     * Initiate Tabby payment flow.
     * Calls Tabby Init API, creates session, and launches Tabby SDK.
     */
    fun initiateTabbyPayment(tabbyConfig: TabbyConfig) {
        viewModelScope.launch {
            val tokenData = PaymentTokenData(
                paymentToken = "tabby_init",
                transactionId = "",
                orderId = PayOrcSdk.instance.currentCheckoutRequest?.orderId ?: "",
                scheme = "TABBY",
                maskCardNumber = "Tabby"
            )
            
            _tabbyPaymentState.value = PaymentState.Processing(
                message = "Initializing Payment... 10%",
                tokenData = tokenData
            )
            try {
                val sdk = PayOrcSdk.instance
                val checkoutRequest = sdk.currentCheckoutRequest
                if (checkoutRequest == null) {
                    _tabbyPaymentState.value = PaymentState.Failure("Checkout request is missing")
                    return@launch
                }

                // 1. Call Tabby Init API to validate order and get order_id
                val initResponse = sdk.payOrcRepo.initializeTabbyPayment(checkoutRequest, tabbyConfig.merchantCode)
                
                _tabbyPaymentState.value = PaymentState.Processing(
                    message = "Preparing Tabby Session... 40%",
                    tokenData = tokenData
                )
                
                if (!initResponse.isSuccess) {
                    _tabbyPaymentState.value = PaymentState.Failure(
                        initResponse.message ?: "Failed to initialize Tabby payment"
                    )
                    return@launch
                }

                // Store order_id for later confirmation
                currentPayOrcTabbyOrderId = initResponse.data?.orderId?.toString()
                if (currentPayOrcTabbyOrderId.isNullOrBlank()) {
                    _tabbyPaymentState.value = PaymentState.Failure("Failed to get order ID")
                    return@launch
                }

                // 2. Use API key from response (password field) or fall back to config
                val apiKeyToUse = initResponse.data?.password ?: tabbyConfig.apiKey
                walletRepo.setupTabby(
                    apiKeyToUse,
                    if (sdk.environment == com.payorc.sdk.PayOrcEnvironment.PRODUCTION) "production" else "stage"
                )
                
                _tabbyPaymentState.value = PaymentState.Processing(
                    message = "Creating Secure Session... 70%",
                    tokenData = tokenData
                )

                // 3. Map Buyer details
                val buyer = TabbyBuyer(
                    email = checkoutRequest.customerEmail ?: "",
                    phone = (checkoutRequest.customerMobileCode ?: "") + (checkoutRequest.customerMobile ?: ""),
                    name = checkoutRequest.customerName ?: ""
                )

                // 4. Create Session with order_id in meta
                val result = createTabbySessionUseCase(
                    merchantCode = tabbyConfig.merchantCode,
                    amount = checkoutRequest.amount.toPlainString(),
                    currency = checkoutRequest.currency,
                    buyer = buyer,
                    orderId = checkoutRequest.orderId,
                    payorcOrderId = currentPayOrcTabbyOrderId
                )

                result.onSuccess { session ->
                    _availableTabbyProducts.value = session.availableProducts
                    currentTabbySession = session
                    val selectedProduct = session.availableProducts.firstOrNull()
                    if (selectedProduct != null) {
                        _tabbyPaymentState.value = PaymentState.Processing(
                            message = "Launching Tabby... 95%",
                            tokenData = tokenData
                        )
                        _tabbyPaymentState.value = PaymentState.LaunchTabbySdk(selectedProduct)
                    } else {
                        _tabbyPaymentState.value = PaymentState.Failure("No Tabby products available for this transaction")
                    }
                }.onFailure { error ->
                    Log.e("TabbyPaymentViewModel", "Tabby initiation failed", error)
                    _tabbyPaymentState.value = PaymentState.Failure("Tabby error: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e("TabbyPaymentViewModel", "Tabby initiation failed", e)
                _tabbyPaymentState.value = PaymentState.Failure("Tabby payment failed to start")
            }
        }
    }

    /**
     * Handle Tabby SDK result (AUTHORIZED, REJECTED, CLOSED, EXPIRED).
     */
    fun handleTabbyResult(tabbyResult: TabbyResult?) {
        // Log consolidated data for debugging
        val sessionData = currentTabbySession
        val logMessage = """
            {
              "tabby_session": {
                "id": "${sessionData?.id}",
                "payment_id": "${sessionData?.paymentId}",
                "status": "${sessionData?.status}"
              },
              "tabby_result": {
                "result": "${tabbyResult?.result}"
              }
            }
        """.trimIndent()

        Log.d("PayOrc_Tabby_Final", "Consolidated Tabby Data:\n$logMessage")
        Log.d("PayOrc_Tabby", "Handling result in ViewModel: $tabbyResult, resultEnum=${tabbyResult?.result}")

        if (tabbyResult == null) {
            Log.e("PayOrc_Tabby", "TabbyResult is NULL")
            reportFailure("Tabby payment failed (No response)")
            return
        }

        when (tabbyResult.result) {
            TabbyResult.Result.AUTHORIZED -> {
                Log.d("PayOrc_Tabby", "Tabby payment AUTHORIZED")
                // Call confirmation API
                confirmTabbyPayment()
            }
            TabbyResult.Result.REJECTED -> {
                Log.w("PayOrc_Tabby", "Tabby payment REJECTED")
                reportFailure("Tabby payment was rejected")
            }
            TabbyResult.Result.CLOSED -> {
                Log.i("PayOrc_Tabby", "Tabby UI CLOSED by user")
                reportFailure("Tabby payment was cancelled")
            }
            TabbyResult.Result.EXPIRED -> {
                Log.w("PayOrc_Tabby", "Tabby session EXPIRED")
                reportFailure("Tabby session expired")
            }
        }
    }

    /**
     * Confirm Tabby payment after SDK authorization.
     */
    private fun confirmTabbyPayment() {
        viewModelScope.launch {
            _tabbyPaymentState.value = PaymentState.Processing(
                message = "Confirming payment...",
                tokenData = PaymentTokenData(
                    paymentToken = "tabby_confirm",
                    transactionId = currentTabbySession?.paymentId ?: "",
                    orderId = currentPayOrcTabbyOrderId ?: "",
                    scheme = "TABBY",
                    maskCardNumber = "Tabby"
                )
            )
            try {
                if (currentPayOrcTabbyOrderId.isNullOrBlank()) {
                    _tabbyPaymentState.value = PaymentState.Failure("Missing order ID for confirmation")
                    return@launch
                }

                // Get Tabby payment ID from session
                val tabbyPaymentId = currentTabbySession?.paymentId
                if (tabbyPaymentId.isNullOrBlank()) {
                    _tabbyPaymentState.value = PaymentState.Failure("Missing Tabby payment ID")
                    return@launch
                }

                // Call confirmation API
                val confirmResponse = PayOrcSdk.instance.payOrcRepo.confirmTabbyPayment(
                    orderId = currentPayOrcTabbyOrderId!!,
                    tabbyPaymentId = tabbyPaymentId
                )

                if (confirmResponse.isSuccess) {
                    Log.d("PayOrc_Tabby", "Payment confirmed successfully")
                    handleTabbySuccess(tabbyPaymentId)
                } else {
                    Log.w("PayOrc_Tabby", "Payment confirmation failed: ${confirmResponse.message}")
                    reportFailure(confirmResponse.message ?: "Payment confirmation failed")
                }
            } catch (e: Exception) {
                Log.e("TabbyPaymentViewModel", "Error confirming payment", e)
                reportFailure("Error confirming payment: ${e.message}")
            }
        }
    }

    /**
     * Handle successful Tabby payment completion.
     */
    private fun handleTabbySuccess(tabbyPaymentId: String) {
        _tabbyPaymentState.value = PaymentState.Success(
            PaymentTokenData(
                paymentToken = "tabby_payment",
                transactionId = tabbyPaymentId,
                orderId = currentPayOrcTabbyOrderId ?: PayOrcSdk.instance.currentCheckoutRequest?.orderId ?: "",
                scheme = "TABBY",
                maskCardNumber = "Tabby"
            )
        )
        // Call success callback if available
        PayOrcCheckout.current3DSCallback?.onSuccess(tabbyPaymentId)
    }

    /**
     * Handle successful Tabby payment (backward compatibility).
     */
    fun handleTabbySuccess() {
        handleTabbySuccess(currentTabbySession?.paymentId ?: "tabby_${System.currentTimeMillis()}")
    }

    /**
     * Report payment failure.
     */
    fun reportFailure(message: String) {
        _tabbyPaymentState.value = PaymentState.Failure(message)
    }

    /**
     * Reset payment state to idle.
     */
    fun resetPaymentState() {
        _tabbyPaymentState.value = PaymentState.Idle
    }

    /**
     * Get current Tabby order ID.
     */
    fun getCurrentPayOrcTabbyOrderId(): String? = currentPayOrcTabbyOrderId

    /**
     * Get current Tabby session.
     */
    fun getCurrentTabbySession(): ai.tabby.android.data.TabbySession? = currentTabbySession
}
