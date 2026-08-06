package com.payorc.sdk.presentation.checkout

import ai.tabby.android.data.Product
import ai.tabby.android.data.TabbyResult
import ai.tabby.android.data.TabbySession
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.data.mapper.CheckoutMapper
import com.payorc.sdk.data.repository.WalletPaymentRepositoryImpl
import com.payorc.sdk.domain.model.AddCardRequest
import com.payorc.sdk.domain.model.CheckoutCustomizationResponse
import com.payorc.sdk.domain.model.SavedCard
import com.payorc.sdk.domain.model.TabbyConfig
import com.payorc.sdk.domain.usecase.CheckoutValidationResult
import com.payorc.sdk.domain.usecase.CheckoutValidationUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class CheckoutViewModel : ViewModel() {

    // Specialized payment ViewModels
    private val cardPaymentViewModel = CardPaymentViewModel()
    private val walletPaymentViewModel = WalletPaymentViewModel()
    private val tabbyPaymentViewModel = TabbyPaymentViewModel()

    // Validation UseCases
    // Validation UseCase
    private val checkoutValidationUseCase = CheckoutValidationUseCase()

    // Configuration State
    private var currentConfig: com.payorc.sdk.domain.model.CheckoutConfig? = null

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Loading)
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    // Aggregated payment state from all specialized ViewModels
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState: StateFlow<PaymentState> = _paymentState.asStateFlow()

    // Expose specialized ViewModel states for UI consumption
    val cardPaymentState: StateFlow<PaymentState> = cardPaymentViewModel.cardPaymentState
    val walletPaymentState: StateFlow<PaymentState> = walletPaymentViewModel.walletPaymentState
    val tabbyPaymentState: StateFlow<PaymentState> = tabbyPaymentViewModel.tabbyPaymentState

    // Expose wallet ViewModel properties
    val savedCards: StateFlow<List<SavedCard>> = walletPaymentViewModel.savedCards
    val savedCardsLoading: StateFlow<Boolean> = walletPaymentViewModel.savedCardsLoading
    val googlePayReady: StateFlow<Boolean> = walletPaymentViewModel.googlePayReady
    val availableTabbyProducts: StateFlow<List<Product>> = tabbyPaymentViewModel.availableTabbyProducts
    
    // Expose lastPaymentOrderId from card payment for compatibility
    val lastPaymentOrderId: String?
        get() = cardPaymentViewModel.lastPaymentOrderId

    init {
        // Aggregate payment states from all specialized ViewModels
        viewModelScope.launch {
            combine(
                cardPaymentViewModel.cardPaymentState,
                walletPaymentViewModel.walletPaymentState,
                tabbyPaymentViewModel.tabbyPaymentState
            ) { cardState, walletState, tabbyState ->
                // Update main payment state based on active payment method
                when {
                    cardState !is PaymentState.Idle -> cardState
                    walletState !is PaymentState.Idle -> walletState
                    tabbyState !is PaymentState.Idle -> tabbyState
                    else -> PaymentState.Idle
                }
            }.collect { _paymentState.value = it }
        }
    }
    fun setGooglePayReady(ready: Boolean) {
        walletPaymentViewModel.setGooglePayReady(ready)
    }

    fun updateCheckoutRequest(update: (com.payorc.sdk.domain.model.PayOrcCheckoutRequest) -> com.payorc.sdk.domain.model.PayOrcCheckoutRequest) {
        PayOrcSdk.instance.currentCheckoutRequest?.let { currentRequest ->
            PayOrcSdk.instance.setCheckoutRequest(update(currentRequest))
        }
    }

    fun validateCheckoutRequest(): CheckoutValidationResult? {
        val request = PayOrcSdk.instance.currentCheckoutRequest
        val config = currentConfig
        return if (request == null || config == null) {
            null
        } else {
            checkoutValidationUseCase.execute(request, config).takeIf { !it.isValid }
        }
    }

    fun loadConfig(checkGooglePayReady: suspend (com.payorc.sdk.domain.model.CheckoutConfig) -> Boolean) {
        viewModelScope.launch {
            _uiState.value = CheckoutUiState.Loading
            try {
                val sdk = PayOrcSdk.instance
                
                // 1. Fetch/Get Customization
                val customizationDeferred = CompletableDeferred<com.payorc.sdk.domain.model.CheckoutConfig?>()
                val currentData = sdk.checkoutCustomization
                if (currentData != null) {
                    customizationDeferred.complete(CheckoutMapper.mapToDomain(CheckoutCustomizationResponse(data = currentData, code = "00", status = "success")))
                } else {
                    sdk.fetchCheckoutCustomization { success ->
                        if (success) {
                            val newData = sdk.checkoutCustomization
                            if (newData != null) {
                                customizationDeferred.complete(CheckoutMapper.mapToDomain(CheckoutCustomizationResponse(data = newData, code = "00", status = "success")))
                            } else {
                                customizationDeferred.complete(null)
                            }
                        } else {
                            customizationDeferred.complete(null)
                        }
                    }
                }
                
                val config = customizationDeferred.await()
                if (config == null) {
                    _uiState.value = CheckoutUiState.Error("Failed to load configuration")
                    return@launch
                }

                // 2. Check Google Pay Readiness (Sequential to prevent flickering)
                val isGPayReady = checkGooglePayReady(config)
                setGooglePayReady(isGPayReady)
                
                // 3. Update UI once EVERYTHING is ready
                currentConfig = config
                _uiState.value = CheckoutUiState.Success(config)

            } catch (e: Exception) {
                Log.e("CheckoutViewModel", "loadConfig failed", e)
                _uiState.value = CheckoutUiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    private fun fetchSavedCards(customerId: String, orderId: String) {
        walletPaymentViewModel.fetchSavedCards(customerId, orderId)
    }

    /**
     * Delegate card payment processing to CardPaymentViewModel.
     */
    fun processCardPayment(
        holderName: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        supportedSchemes: List<String> = emptyList()
    ) {
        cardPaymentViewModel.processCardPayment(holderName, cardNumber, expiry, cvv, supportedSchemes)
    }

    /**
     * Delegate wallet payment execution to WalletPaymentViewModel.
     */
    fun executeWalletPayment(paymentToken: String, orderId: String) {
        walletPaymentViewModel.executeWalletPayment(paymentToken, orderId)
    }

    /**
     * Reset payment state across all specialized ViewModels.
     */
    fun resetPaymentState() {
        cardPaymentViewModel.resetPaymentState()
        walletPaymentViewModel.resetPaymentState()
        tabbyPaymentViewModel.resetPaymentState()
        _paymentState.value = PaymentState.Idle
    }

    /**
     * Delegate Tabby payment initiation to TabbyPaymentViewModel.
     */
    fun initiateTabbyPayment(
        tabbyConfig: com.payorc.sdk.domain.model.TabbyConfig
    ) {
        tabbyPaymentViewModel.initiateTabbyPayment(tabbyConfig)
    }

    /**
     * Delegate Tabby result handling to TabbyPaymentViewModel.
     */
    fun handleTabbyResult(tabbyResult: TabbyResult?) {
        tabbyPaymentViewModel.handleTabbyResult(tabbyResult)
    }

    /**
     * Delegate Tabby success handling to TabbyPaymentViewModel (backward compatibility).
     */
    fun handleTabbySuccess() {
        tabbyPaymentViewModel.handleTabbySuccess()
    }

    /**
     * Delegate card add success to CardPaymentViewModel.
     */
    fun handleAddCardSuccess(tokenData: com.payorc.sdk.domain.model.PaymentTokenData) {
        cardPaymentViewModel.handleAddCardSuccess(tokenData)
    }

    /**
     * Delegate payment success to CardPaymentViewModel.
     */
    fun handlePaymentSuccess(tokenData: com.payorc.sdk.domain.model.PaymentTokenData, merchantResponse: Map<String, Any?> = emptyMap()) {
        _paymentState.value = PaymentState.Success(tokenData, merchantResponse)
    }

    /**
     * Delegate execute payment with token to CardPaymentViewModel.
     */
    fun executePaymentWithToken(tokenData: com.payorc.sdk.domain.model.PaymentTokenData, cvv: String) {
        cardPaymentViewModel.executePaymentWithToken(tokenData, cvv)
    }

    fun reportFailure(message: String) {
        _paymentState.value = PaymentState.Failure(message)
        
        val sdk = PayOrcSdk.instance
        val customerId = sdk.currentCheckoutRequest?.customerId
        val pOrderId = lastPaymentOrderId
        
        if (!customerId.isNullOrBlank() && !pOrderId.isNullOrBlank()) {
            fetchSavedCards(customerId, pOrderId)
        }
    }


}

sealed class CheckoutUiState {
    object Loading : CheckoutUiState()
    data class Success(val config: com.payorc.sdk.domain.model.CheckoutConfig) : CheckoutUiState()
    data class Error(val message: String) : CheckoutUiState()
}

sealed class PaymentState {
    object Idle : PaymentState()
    data class ValidationError(val message: String) : PaymentState()
    data class Processing(val message: String, val tokenData: com.payorc.sdk.domain.model.PaymentTokenData? = null) : PaymentState()
    data class RedirectTo3DS(val url: String) : PaymentState()
    data class RedirectToTabby(val url: String) : PaymentState()
    data class LaunchTabbySdk(val product: Product) : PaymentState()
    data class CardVerified(val tokenData: com.payorc.sdk.domain.model.PaymentTokenData, val cvv: String) : PaymentState()
    data class Success(val tokenData: com.payorc.sdk.domain.model.PaymentTokenData, val merchantResponse: Map<String, Any?> = emptyMap()) : PaymentState()
    data class Failure(val message: String) : PaymentState()
}
