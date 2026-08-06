package com.payorc.sdk.presentation.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.tabby.android.data.Product
import com.payorc.sdk.domain.model.TabbyBuyer
import com.payorc.sdk.domain.usecase.CreateTabbySessionUseCase
import com.payorc.sdk.domain.usecase.VerifyTabbyPaymentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tabby ViewModel
 * 
 * Manages the state of the Tabby payment flow.
 */
class TabbyViewModel(
    private val createTabbySessionUseCase: CreateTabbySessionUseCase,
    private val verifyTabbyPaymentUseCase: VerifyTabbyPaymentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<TabbyUiState>(TabbyUiState.Idle)
    val uiState: StateFlow<TabbyUiState> = _uiState.asStateFlow()

    fun createSession(
        merchantCode: String,
        amount: String,
        currency: String,
        buyer: TabbyBuyer,
        orderId: String
    ) {
        viewModelScope.launch {
            _uiState.value = TabbyUiState.Loading
            createTabbySessionUseCase(
                merchantCode = merchantCode,
                amount = amount,
                currency = currency,
                buyer = buyer,
                orderId = orderId
            )
                .onSuccess { session ->
                    _uiState.value = TabbyUiState.SessionCreated(session.availableProducts)
                }
                .onFailure { error ->
                    _uiState.value = TabbyUiState.Error(error.message ?: "Failed to create Tabby session")
                }
        }
    }

    fun verifyPayment(paymentId: String) {
        viewModelScope.launch {
            _uiState.value = TabbyUiState.Loading
            verifyTabbyPaymentUseCase(paymentId)
                .onSuccess { isSuccess ->
                    if (isSuccess) {
                        _uiState.value = TabbyUiState.Success
                    } else {
                        _uiState.value = TabbyUiState.Error("Payment verification failed")
                    }
                }
                .onFailure { error ->
                    _uiState.value = TabbyUiState.Error(error.message ?: "Failed to verify payment")
                }
        }
    }

    fun onUserCancelled() {
        _uiState.value = TabbyUiState.Cancelled
    }
}

sealed class TabbyUiState {
    object Idle : TabbyUiState()
    object Loading : TabbyUiState()
    data class SessionCreated(val products: List<Product>) : TabbyUiState()
    object Success : TabbyUiState()
    object Cancelled : TabbyUiState()
    data class Error(val message: String) : TabbyUiState()
}
