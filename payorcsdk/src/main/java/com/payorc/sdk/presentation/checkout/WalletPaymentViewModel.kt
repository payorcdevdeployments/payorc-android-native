package com.payorc.sdk.presentation.checkout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.domain.model.PaymentTokenData
import com.payorc.sdk.domain.usecase.ExecuteWalletPaymentUseCase
import com.payorc.sdk.domain.usecase.FetchSavedCardsUseCase
import com.payorc.sdk.domain.model.SavedCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Specialized ViewModel for wallet payment flow (Google Pay, Apple Pay, etc.).
 * Handles wallet payment execution and saved cards management.
 */
class WalletPaymentViewModel : ViewModel() {

    private val executeWalletPaymentUseCase = ExecuteWalletPaymentUseCase(PayOrcSdk.instance.payOrcRepo)
    private val fetchSavedCardsUseCase = FetchSavedCardsUseCase(PayOrcSdk.instance.payOrcRepo)

    private val _walletPaymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val walletPaymentState: StateFlow<PaymentState> = _walletPaymentState.asStateFlow()

    private val _savedCards = MutableStateFlow<List<SavedCard>>(emptyList())
    val savedCards: StateFlow<List<SavedCard>> = _savedCards.asStateFlow()

    private val _savedCardsLoading = MutableStateFlow(false)
    val savedCardsLoading: StateFlow<Boolean> = _savedCardsLoading.asStateFlow()

    private val _googlePayReady = MutableStateFlow(false)
    val googlePayReady: StateFlow<Boolean> = _googlePayReady.asStateFlow()

    private var lastPaymentOrderId: String? = null

    /**
     * Set Google Pay readiness state.
     */
    fun setGooglePayReady(ready: Boolean) {
        _googlePayReady.value = ready
    }

    /**
     * Execute wallet payment with provided token.
     */
    fun executeWalletPayment(paymentToken: String, orderId: String) {
        viewModelScope.launch {
            _walletPaymentState.value = PaymentState.Processing(
                message = "Processing wallet payment...",
                tokenData = PaymentTokenData(
                    paymentToken = paymentToken,
                    transactionId = "",
                    orderId = orderId,
                    scheme = "GOOGLE_PAY",
                    maskCardNumber = "Google Pay"
                )
            )
            try {
                val response = executeWalletPaymentUseCase.execute(paymentToken, orderId)
                if (response.isSuccess) {
                    lastPaymentOrderId = response.data?.pOrderId
                    if (response.data?.transactionId != null) {
                        _walletPaymentState.value = PaymentState.Success(
                            PaymentTokenData(
                                paymentToken = paymentToken,
                                transactionId = response.data.transactionId!!,
                                orderId = orderId,
                                scheme = "GOOGLE_PAY",
                                maskCardNumber = "Google Pay"
                            )
                        )
                    } else {
                        _walletPaymentState.value = PaymentState.Failure("Missing transaction ID in wallet response")
                    }
                } else {
                    lastPaymentOrderId = response.data?.pOrderId
                    reportFailure(response.message ?: "Wallet payment failed")
                }
            } catch (e: Exception) {
                Log.e("WalletPaymentViewModel", "executeWalletPayment failed", e)
                _walletPaymentState.value = PaymentState.Failure(e.message ?: "Wallet payment encountered an error")
            }
        }
    }

    /**
     * Fetch saved cards for the customer.
     */
    fun fetchSavedCards(customerId: String, orderId: String) {
        viewModelScope.launch {
            _savedCardsLoading.value = true
            try {
                val result = fetchSavedCardsUseCase(customerId, orderId)
                if (result.isSuccess) {
                    _savedCards.value = result.data
                } else {
                    Log.e("WalletPaymentViewModel", "Failed to fetch saved cards: ${result.message}")
                }
            } catch (e: Exception) {
                Log.e("WalletPaymentViewModel", "Error fetching saved cards", e)
            } finally {
                _savedCardsLoading.value = false
            }
        }
    }

    /**
     * Report payment failure and fetch saved cards for retry.
     */
    fun reportFailure(message: String) {
        _walletPaymentState.value = PaymentState.Failure(message)

        val sdk = PayOrcSdk.instance
        val customerId = sdk.currentCheckoutRequest?.customerId
        val pOrderId = lastPaymentOrderId

        if (!customerId.isNullOrBlank() && !pOrderId.isNullOrBlank()) {
            fetchSavedCards(customerId, pOrderId)
        }
    }

    /**
     * Reset payment state to idle.
     */
    fun resetPaymentState() {
        _walletPaymentState.value = PaymentState.Idle
    }
}
