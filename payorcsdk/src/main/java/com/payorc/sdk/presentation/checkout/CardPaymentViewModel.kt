package com.payorc.sdk.presentation.checkout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.domain.model.AddCardRequest
import com.payorc.sdk.domain.model.ExecutePaymentRequest
import com.payorc.sdk.domain.model.PaymentTokenData
import com.payorc.sdk.domain.usecase.CardValidationUseCase
import com.payorc.sdk.domain.usecase.ExecutePaymentUseCase
import com.payorc.sdk.domain.usecase.TokenizeCardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Specialized ViewModel for card payment flow.
 * Handles card validation, tokenization, and payment execution.
 */
class CardPaymentViewModel : ViewModel() {

    private val validationUseCase = CardValidationUseCase()
    private val tokenizeCardUseCase = TokenizeCardUseCase(PayOrcSdk.instance.payOrcRepo)
    private val executePaymentUseCase = ExecutePaymentUseCase(PayOrcSdk.instance.payOrcRepo)

    private val _cardPaymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val cardPaymentState: StateFlow<PaymentState> = _cardPaymentState.asStateFlow()

    var lastPaymentOrderId: String? = null
        internal set

    /**
     * Process direct card payment (card number, expiry, CVV).
     */
    fun processCardPayment(
        holderName: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        supportedSchemes: List<String> = emptyList()
    ) {
        // Validate checkout request first
        val validationError = validateCheckoutRequest()
        if (validationError != null) {
            _cardPaymentState.value = PaymentState.ValidationError(validationError)
            return
        }

        // Validate card details
        val validationResult = validationUseCase.execute(
            holderName = holderName,
            cardNumber = cardNumber,
            expiry = expiry,
            cvv = cvv,
            supportedSchemes = supportedSchemes
        )

        if (!validationResult.isValid) {
            _cardPaymentState.value = PaymentState.ValidationError(validationResult.errorMessage ?: "Please fill all card details correctly")
            return
        }

        viewModelScope.launch {
            // Create a temporary token data to show card info during processing if possible
            val tempTokenData = PaymentTokenData(
                paymentToken = "",
                transactionId = "",
                orderId = "",
                scheme = com.payorc.sdk.presentation.checkout.components.detectCardScheme(cardNumber) ?: "",
                maskCardNumber = if (cardNumber.length >= 4) "**** **** **** ${cardNumber.takeLast(4)}" else cardNumber
            )
            _cardPaymentState.value = PaymentState.Processing("Processing payment...", tempTokenData)
            try {
                val sdk = PayOrcSdk.instance
                val checkoutRequest = sdk.currentCheckoutRequest ?: throw IllegalStateException("Checkout request not found")

                // Parse expiry date
                val sanitizedExpiry = expiry.filter { it.isDigit() }
                val month = if (sanitizedExpiry.length >= 2) sanitizedExpiry.substring(0, 2) else ""
                val year = if (sanitizedExpiry.length >= 4) sanitizedExpiry.substring(2, 4) else if (sanitizedExpiry.length >= 3) sanitizedExpiry.substring(2) else ""

                // Build payment request
                val execRequest = ExecutePaymentRequest(
                    paymentToken = "",
                    amount = checkoutRequest.amount.toPlainString(),
                    currency = checkoutRequest.currency,
                    orderId = checkoutRequest.orderId,
                    description = checkoutRequest.description,
                    cvv = cvv,
                    cardHolderName = holderName,
                    cardNumber = cardNumber.filter { it.isDigit() },
                    expiryMonth = month,
                    expiryYear = year
                )

                // Execute payment
                val paymentResponse = executePaymentUseCase.execute(execRequest)
                if (paymentResponse.isSuccess) {
                    lastPaymentOrderId = paymentResponse.data?.pOrderId
                    if (!paymentResponse.data?.redirectUrl.isNullOrBlank()) {
                        // Payment requires 3DS redirect
                        _cardPaymentState.value = PaymentState.RedirectTo3DS(paymentResponse.data!!.redirectUrl!!)
                    } else if (!paymentResponse.data?.transactionId.isNullOrBlank()) {
                        // Payment successful
                        _cardPaymentState.value = PaymentState.Success(
                            PaymentTokenData(
                                paymentToken = "",
                                transactionId = paymentResponse.data!!.transactionId!!,
                                orderId = checkoutRequest.orderId,
                                scheme = "",
                                maskCardNumber = ""
                            ),
                            mapOf(
                                "status" to paymentResponse.status,
                                "code" to paymentResponse.code,
                                "message" to paymentResponse.message,
                                "data" to mapOf(
                                    "transaction_id" to paymentResponse.data.transactionId,
                                    "p_order_id" to paymentResponse.data.pOrderId,
                                    "m_order_id" to paymentResponse.data.mOrderId,
                                    "amount" to paymentResponse.data.amount,
                                    "currency" to paymentResponse.data.currency,
                                    "status" to paymentResponse.data.status
                                )
                            )
                        )
                    } else {
                        _cardPaymentState.value = PaymentState.Failure("Missing transaction ID")
                    }
                } else {
                    lastPaymentOrderId = paymentResponse.data?.pOrderId
                    reportFailure(paymentResponse.message ?: "Payment execution failed")
                }
            } catch (e: Exception) {
                Log.e("CardPaymentViewModel", "processCardPayment failed", e)
                _cardPaymentState.value = PaymentState.Failure(e.message ?: "Payment flow failed")
            }
        }
    }

    /**
     * Tokenize a new card and handle response.
     */
    fun tokenizeCard(request: AddCardRequest) {
        viewModelScope.launch {
            _cardPaymentState.value = PaymentState.Processing("Tokenizing card...")
            try {
                val result = tokenizeCardUseCase.execute(request)
                if (result.isSuccess) {
                    val tokenData = result.data
                    if (tokenData != null) {
                        val paymentTokenData = PaymentTokenData(
                            paymentToken = tokenData.paymentToken ?: "",
                            transactionId = "",
                            orderId = "",
                            scheme = "",
                            maskCardNumber = ""
                        )
                        _cardPaymentState.value = PaymentState.CardVerified(paymentTokenData, "")
                    } else {
                        reportFailure("Failed to tokenize card: No token data")
                    }
                } else {
                    reportFailure(result.message ?: "Card tokenization failed")
                }
            } catch (e: Exception) {
                Log.e("CardPaymentViewModel", "tokenizeCard failed", e)
                reportFailure(e.message ?: "Tokenization error")
            }
        }
    }

    /**
     * Execute payment using a previously tokenized card.
     */
    fun executePaymentWithToken(tokenData: PaymentTokenData, cvv: String) {
        Log.d("CardPaymentViewModel", "executePaymentWithToken called: token=${tokenData.paymentToken}, scheme=${tokenData.scheme}")
        viewModelScope.launch {
            _cardPaymentState.value = PaymentState.Processing("Processing payment...", tokenData)
            Log.d("CardPaymentViewModel", "Payment state set to Processing")
            try {
                val sdk = PayOrcSdk.instance
                val checkoutRequest = sdk.currentCheckoutRequest ?: throw IllegalStateException("Checkout request not found")

                val execRequest = ExecutePaymentRequest(
                    paymentToken = tokenData.paymentToken,
                    amount = checkoutRequest.amount.toPlainString(),
                    currency = checkoutRequest.currency,
                    orderId = checkoutRequest.orderId,
                    description = checkoutRequest.description,
                    cvv = cvv.ifBlank { null }
                )

                Log.d("CardPaymentViewModel", "Executing payment request with orderId=${checkoutRequest.orderId}")
                val paymentResponse = executePaymentUseCase.execute(execRequest)
                Log.d("CardPaymentViewModel", "Payment response received. isSuccess=${paymentResponse.isSuccess}, pOrderId=${paymentResponse.data?.pOrderId}")
                
                if (paymentResponse.isSuccess) {
                    lastPaymentOrderId = paymentResponse.data?.pOrderId
                    if (!paymentResponse.data?.redirectUrl.isNullOrBlank()) {
                        Log.d("CardPaymentViewModel", "Setting state to RedirectTo3DS")
                        _cardPaymentState.value = PaymentState.RedirectTo3DS(paymentResponse.data!!.redirectUrl!!)
                    } else if (!paymentResponse.data?.transactionId.isNullOrBlank()) {
                        Log.d("CardPaymentViewModel", "Setting state to Success with transactionId=${paymentResponse.data!!.transactionId}")
                        _cardPaymentState.value = PaymentState.Success(
                            tokenData.copy(transactionId = paymentResponse.data!!.transactionId!!),
                            mapOf(
                                "status" to paymentResponse.status,
                                "code" to paymentResponse.code,
                                "message" to paymentResponse.message,
                                "data" to mapOf(
                                    "transaction_id" to paymentResponse.data.transactionId,
                                    "p_order_id" to paymentResponse.data.pOrderId,
                                    "m_order_id" to paymentResponse.data.mOrderId,
                                    "amount" to paymentResponse.data.amount,
                                    "currency" to paymentResponse.data.currency,
                                    "status" to paymentResponse.data.status
                                )
                            )
                        )
                    } else {
                        Log.d("CardPaymentViewModel", "Setting state to Failure: Missing transaction ID")
                        _cardPaymentState.value = PaymentState.Failure("Missing transaction ID")
                    }
                } else {
                    lastPaymentOrderId = paymentResponse.data?.pOrderId
                    Log.d("CardPaymentViewModel", "Payment failed: ${paymentResponse.message}")
                    reportFailure(paymentResponse.message ?: "Payment execution failed")
                }
            } catch (e: Exception) {
                Log.e("CardPaymentViewModel", "executePaymentWithToken failed", e)
                _cardPaymentState.value = PaymentState.Failure(e.message ?: "Payment failed")
            }
        }
    }

    /**
     * Handle successful card addition.
     */
    fun handleAddCardSuccess(tokenData: PaymentTokenData) {
        _cardPaymentState.value = PaymentState.CardVerified(tokenData, "")
    }

    /**
     * Report payment failure.
     */
    fun reportFailure(message: String) {
        _cardPaymentState.value = PaymentState.Failure(message)
    }

    /**
     * Reset payment state to idle.
     */
    fun resetPaymentState() {
        _cardPaymentState.value = PaymentState.Idle
    }

    /**
     * Validate checkout request from SDK.
     */
    private fun validateCheckoutRequest(): String? {
        val request = PayOrcSdk.instance.currentCheckoutRequest
        if (request == null) {
            return "Checkout request is missing"
        }
        return null
    }
}
