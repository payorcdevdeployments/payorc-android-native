package com.payorc.sdk.data.repository

import android.content.Context
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.core.network.DeviceMetadataProvider
import com.payorc.sdk.core.network.ErrorResponseExtractor
import com.payorc.sdk.core.security.SignatureGenerator
import com.payorc.sdk.data.remote.api.PayOrcApiService
import com.payorc.sdk.domain.model.CheckoutCustomizationResponse
import com.payorc.sdk.domain.repository.PayOrcRepository
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import retrofit2.HttpException
import java.io.IOException

class PayOrcRepositoryImpl(
    private val apiService: PayOrcApiService,
    private val merchantKey: String,
    private val merchantSecret: String,
    private val context: Context
) : PayOrcRepository {

    override suspend fun fetchCheckoutCustomization(
        currency: String,
        amount: Double
    ): CheckoutCustomizationResponse {
        val bodyMap = buildJsonObject {
            putJsonObject("data") {
                put("currency", currency)
                put("amount", amount.toString())
            }
        }
        
        val bodyString = bodyMap.toString()
        val headers = generateAuthHeaders(bodyString)
        
        return try {
            apiService.fetchCheckoutCustomization(headers, bodyString)
        } catch (e: Exception) {
            handleApiError(
                e,
                { message, code ->
                    CheckoutCustomizationResponse(
                        message = message.ifBlank { "Failed to fetch checkout customization" },
                        status = "fail",
                        code = code
                    )
                }
            )
        }
    }

    override suspend fun addCard(request: com.payorc.sdk.domain.model.AddCardRequest): com.payorc.sdk.domain.model.AddCardResponse {
        val checkoutRequest = PayOrcSdk.instance.currentCheckoutRequest ?: throw IllegalStateException("Checkout request not found")
        
        val bodyDto = com.payorc.sdk.data.remote.dto.AddCardRequestDto(
            data = com.payorc.sdk.data.remote.dto.AddCardRequestDataDto(
                action = checkoutRequest.action,
                customerDetails = com.payorc.sdk.data.remote.dto.CustomerDetailsDto(
                    mCustomerId = checkoutRequest.customerId,
                    name = checkoutRequest.customerName,
                    email = checkoutRequest.customerEmail,
                    mobile = checkoutRequest.customerMobile,
                    code = checkoutRequest.customerMobileCode
                ),
                billingDetails = com.payorc.sdk.data.remote.dto.AddressDetailsDto(
                    addressLine1 = checkoutRequest.billingAddress.addressLine1,
                    addressLine2 = checkoutRequest.billingAddress.addressLine2,
                    city = checkoutRequest.billingAddress.city,
                    province = checkoutRequest.billingAddress.province,
                    country = checkoutRequest.billingAddress.country,
                    pin = checkoutRequest.billingAddress.pin
                ),
                orderDetails = com.payorc.sdk.data.remote.dto.OrderDetailsDto(
                    mOrderId = checkoutRequest.orderId,
                    currency = checkoutRequest.currency,
                    description = checkoutRequest.description,
                    amount = checkoutRequest.amount.toString()
                ),
                cardDetails = com.payorc.sdk.data.remote.dto.AddCardCardDetailsDto(
                    cardHolderName = request.cardHolderName,
                    cardNumber = request.cardNumber.replace(" ", ""),
                    cvv = request.cvv,
                    expiry = formatExpiry(request.expiryMonth, request.expiryYear)
                ),
                shippingDetails = com.payorc.sdk.data.remote.dto.ShippingDetailsDto(
                    shippingName = checkoutRequest.customerName,
                    shippingEmail = checkoutRequest.customerEmail,
                    shippingCode = checkoutRequest.customerMobileCode,
                    shippingMobile = checkoutRequest.customerMobile,
                    addressLine1 = checkoutRequest.shippingAddress.addressLine1,
                    addressLine2 = checkoutRequest.shippingAddress.addressLine2,
                    city = checkoutRequest.shippingAddress.city,
                    province = checkoutRequest.shippingAddress.province,
                    country = checkoutRequest.shippingAddress.country,
                    pin = checkoutRequest.shippingAddress.pin,
                    shippingCurrency = checkoutRequest.currency,
                    shippingAmount = "0.00"
                ),
                urls = checkoutRequest.urls?.webhookUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { mapOf("webhook_url" to it) }
            )
        )
        
        val bodyString = PayOrcSdk.instance.json.encodeToString(com.payorc.sdk.data.remote.dto.AddCardRequestDto.serializer(), bodyDto)
        val headers = generateAuthHeaders(bodyString)

        return try {
            apiService.addCard(headers, bodyString)
        } catch (e: Exception) {
            handleApiError(
                e,
                { message, code ->
                    com.payorc.sdk.domain.model.AddCardResponse(message = message, status = "fail", code = code)
                }
            )
        }
    }

    private fun formatExpiry(month: String, year: String): String {
        val normalizedMonth = month.padStart(2, '0').take(2)
        val normalizedYear = when {
            year.length == 4 -> year
            year.length == 2 -> "20$year"
            else -> year.padStart(2, '0')
        }
        return "$normalizedMonth/$normalizedYear"
    }

    private fun buildWebhookUrls(checkoutRequest: com.payorc.sdk.domain.model.PayOrcCheckoutRequest) =
        checkoutRequest.urls?.webhookUrl
            ?.takeIf { it.isNotBlank() }
            ?.let { mapOf("webhook_url" to it) }

    override suspend fun submitPayment(request: com.payorc.sdk.domain.model.ExecutePaymentRequest): com.payorc.sdk.domain.model.PaymentResponse {
        val checkoutRequest = PayOrcSdk.instance.currentCheckoutRequest ?: throw IllegalStateException("Checkout request not found")

        val bodyMap = buildJsonObject {
            putJsonObject("data") {
                put("action", checkoutRequest.action)
                put("type", "CARD")
                put("capture_method", "MANUAL")
                put("class", "ECOM")
                
                putJsonObject("customer_details") {
                    put("m_customer_id", checkoutRequest.customerId)
                    put("name", checkoutRequest.customerName)
                    put("email", checkoutRequest.customerEmail)
                    put("mobile", checkoutRequest.customerMobile)
                    put("code", checkoutRequest.customerMobileCode)
                }
                
                putJsonObject("billing_details") {
                    put("address_line1", checkoutRequest.billingAddress.addressLine1)
                    put("address_line2", checkoutRequest.billingAddress.addressLine2 ?: "")
                    put("city", checkoutRequest.billingAddress.city)
                    put("province", checkoutRequest.billingAddress.province)
                    put("country", checkoutRequest.billingAddress.country)
                    put("pin", checkoutRequest.billingAddress.pin)
                }
                
                putJsonObject("order_details") {
                    put("m_order_id", request.orderId)
                    put("amount", request.amount)
                    put("currency", request.currency)
                    put("convenience_fee", "0.00")
                    put("description", request.description ?: "")
                }
                
                putJsonObject("card_details") {
                    put("payment_token", request.paymentToken)
                    request.cardNumber?.let { put("card_number", it) }
                    request.cardHolderName?.let { put("card_holder_name", it) }
                    if (!request.expiryMonth.isNullOrBlank() && !request.expiryYear.isNullOrBlank()) {
                        put("expiry", formatExpiry(request.expiryMonth, request.expiryYear))
                    }
                    request.cvv?.let { put("cvv", it) }
                }
                
                putJsonObject("shipping_details") {
                    put("shipping_name", checkoutRequest.customerName)
                    put("shipping_email", checkoutRequest.customerEmail)
                    put("shipping_code", checkoutRequest.customerMobileCode)
                    put("shipping_mobile", checkoutRequest.customerMobile)
                    put("address_line1", checkoutRequest.shippingAddress.addressLine1)
                    put("address_line2", checkoutRequest.shippingAddress.addressLine2 ?: "")
                    put("city", checkoutRequest.shippingAddress.city)
                    put("province", checkoutRequest.shippingAddress.province)
                    put("country", checkoutRequest.shippingAddress.country)
                    put("pin", checkoutRequest.shippingAddress.pin)
                    put("shipping_currency", checkoutRequest.currency)
                    put("shipping_amount", "0.00")
                }

                checkoutRequest.urls?.webhookUrl?.takeIf { it.isNotBlank() }?.let { webhookUrl ->
                    putJsonObject("urls") {
                        put("webhook_url", webhookUrl)
                    }
                }
            }
        }
        val bodyString = bodyMap.toString()
        val headers = generateAuthHeaders(bodyString)

        return try {
            apiService.submitPayment(headers, bodyString)
        } catch (e: Exception) {
            handleApiError(
                e,
                { message, code ->
                    com.payorc.sdk.domain.model.PaymentResponse(message = message, status = "fail", code = code)
                }
            )
        }
    }

    override suspend fun submitWalletPayment(
        paymentToken: String,
        orderId: String
    ): com.payorc.sdk.domain.model.PaymentResponse {
        val checkoutRequest = PayOrcSdk.instance.currentCheckoutRequest ?: throw IllegalStateException("Checkout request not found")

        val bodyMap = buildJsonObject {
            putJsonObject("data") {
                put("action", checkoutRequest.action)
                put("type", "GOOGLE_PAY")
                put("capture_method", "MANUAL")
                put("class", "ECOM")
                
                putJsonObject("customer_details") {
                    put("m_customer_id", checkoutRequest.customerId)
                    put("name", checkoutRequest.customerName)
                    put("email", checkoutRequest.customerEmail)
                    put("mobile", checkoutRequest.customerMobile)
                    put("code", checkoutRequest.customerMobileCode)
                }
                
                putJsonObject("billing_details") {
                    put("address_line1", checkoutRequest.billingAddress.addressLine1)
                    put("address_line2", checkoutRequest.billingAddress.addressLine2 ?: "")
                    put("city", checkoutRequest.billingAddress.city)
                    put("province", checkoutRequest.billingAddress.province)
                    put("country", checkoutRequest.billingAddress.country)
                    put("pin", checkoutRequest.billingAddress.pin)
                }
                
                putJsonObject("order_details") {
                    put("m_order_id", orderId)
                    put("amount", checkoutRequest.amount.toString())
                    put("currency", checkoutRequest.currency)
                    put("convenience_fee", "0.00")
                    put("description", checkoutRequest.description)
                }
                
                putJsonObject("card_details") {
                    put("payment_token", paymentToken)
                }
                
                putJsonObject("shipping_details") {
                    put("shipping_name", checkoutRequest.customerName)
                    put("shipping_email", checkoutRequest.customerEmail)
                    put("shipping_code", checkoutRequest.customerMobileCode)
                    put("shipping_mobile", checkoutRequest.customerMobile)
                    put("address_line1", checkoutRequest.shippingAddress.addressLine1)
                    put("address_line2", checkoutRequest.shippingAddress.addressLine2 ?: "")
                    put("city", checkoutRequest.shippingAddress.city)
                    put("province", checkoutRequest.shippingAddress.province)
                    put("country", checkoutRequest.shippingAddress.country)
                    put("pin", checkoutRequest.shippingAddress.pin)
                    put("shipping_currency", checkoutRequest.currency)
                    put("shipping_amount", "0.00")
                }

                checkoutRequest.urls?.webhookUrl?.takeIf { it.isNotBlank() }?.let { webhookUrl ->
                    putJsonObject("urls") {
                        put("webhook_url", webhookUrl)
                    }
                }
            }
        }
        val bodyString = bodyMap.toString()
        val headers = generateAuthHeaders(bodyString)

        return try {
            apiService.submitWalletPayment(headers, bodyString)
        } catch (e: Exception) {
            handleApiError(
                e,
                { message, code ->
                    com.payorc.sdk.domain.model.PaymentResponse(message = message, status = "fail", code = code)
                }
            )
        }
    }

    override suspend fun fetchSavedCards(
        customerId: String,
        orderId: String
    ): com.payorc.sdk.domain.model.SavedCardsResult {
        val requestDto = com.payorc.sdk.data.remote.dto.FetchSavedCardsRequestDto(
            customerId = customerId,
            orderId = orderId
        )

        val bodyString = PayOrcSdk.instance.json.encodeToString(
            com.payorc.sdk.data.remote.dto.FetchSavedCardsRequestDto.serializer(),
            requestDto
        )

        val headers = generateAuthHeaders(bodyString)

        return try {
            val response = apiService.fetchSavedCards(headers, bodyString)
            com.payorc.sdk.domain.model.SavedCardsResult(
                data = com.payorc.sdk.data.mapper.SavedCardMapper.mapToDomainList(response.data),
                message = response.message,
                status = response.status,
                code = response.code
            )
        } catch (e: Exception) {
            handleApiError(
                e,
                { message, code ->
                    com.payorc.sdk.domain.model.SavedCardsResult(
                        message = message.ifBlank { "Failed to fetch saved cards" },
                        status = "fail",
                        code = code
                    )
                }
            )
        }
    }

    override suspend fun initializeTabbyPayment(
        checkoutRequest: com.payorc.sdk.domain.model.PayOrcCheckoutRequest,
        merchantCode: String?
    ): com.payorc.sdk.data.remote.dto.TabbyInitResponseDto {
        // Build Tabby Init request using DTOs
        val tabbyRequest = com.payorc.sdk.data.remote.dto.TabbyInitRequestDto(
            data = com.payorc.sdk.data.remote.dto.TabbyInitDataDto(
                action = checkoutRequest.action,
                ecomClass = "ECOM",
                captureMethod = "MANUAL",
                type = "TABBY",
                merchantCode = merchantCode,
                customerDetails = com.payorc.sdk.data.remote.dto.CustomerDetailsDto(
                    mCustomerId = checkoutRequest.customerId,
                    name = checkoutRequest.customerName ?: "",
                    email = checkoutRequest.customerEmail ?: "",
                    mobile = checkoutRequest.customerMobile ?: "",
                    code = checkoutRequest.customerMobileCode ?: ""
                ),
                billingDetails = com.payorc.sdk.data.remote.dto.AddressDetailsDto(
                    addressLine1 = checkoutRequest.billingAddress.addressLine1,
                    addressLine2 = checkoutRequest.billingAddress.addressLine2 ?: "",
                    city = checkoutRequest.billingAddress.city,
                    province = checkoutRequest.billingAddress.province,
                    country = checkoutRequest.billingAddress.country,
                    pin = checkoutRequest.billingAddress.pin
                ),
                orderDetails = com.payorc.sdk.data.remote.dto.OrderDetailsDto(
                    mOrderId = checkoutRequest.orderId,
                    amount = checkoutRequest.amount.toString(),
                    currency = checkoutRequest.currency,
                    convenienceFee = "",
                    description = checkoutRequest.description ?: ""
                ),
                shippingDetails = com.payorc.sdk.data.remote.dto.ShippingDetailsDto(
                    shippingName = checkoutRequest.customerName ?: "",
                    shippingEmail = checkoutRequest.customerEmail ?: "",
                    shippingCode = checkoutRequest.customerMobileCode ?: "",
                    shippingMobile = checkoutRequest.customerMobile ?: "",
                    addressLine1 = checkoutRequest.shippingAddress.addressLine1,
                    addressLine2 = checkoutRequest.shippingAddress.addressLine2 ?: "",
                    city = checkoutRequest.shippingAddress.city,
                    province = checkoutRequest.shippingAddress.province,
                    country = checkoutRequest.shippingAddress.country,
                    pin = checkoutRequest.shippingAddress.pin,
                    shippingCurrency = checkoutRequest.currency,
                    shippingAmount = ""
                ),
                urls = checkoutRequest.urls?.webhookUrl
                    ?.takeIf { it.isNotBlank() }
                    ?.let { mapOf("webhook_url" to it) }
            )
        )
        
        val bodyString = PayOrcSdk.instance.json.encodeToString(
            com.payorc.sdk.data.remote.dto.TabbyInitRequestDto.serializer(),
            tabbyRequest
        )
        val headers = generateAuthHeaders(bodyString)
        
        return try {
            apiService.initializeTabby(headers, bodyString)
        } catch (e: Exception) {
            handleApiError(
                e,
                { message, code ->
                    com.payorc.sdk.data.remote.dto.TabbyInitResponseDto(
                        message = message.ifBlank { "Failed to initialize Tabby payment" },
                        status = "fail",
                        code = code
                    )
                }
            )
        }
    }

    override suspend fun confirmTabbyPayment(
        orderId: String,
        tabbyPaymentId: String
    ): com.payorc.sdk.data.remote.dto.TabbyConfirmResponseDto {
        // Build Tabby Confirm request using DTO
        val tabbyConfirmRequest = com.payorc.sdk.data.remote.dto.TabbyConfirmRequestDto(
            orderId = orderId,
            tabbyPaymentId = tabbyPaymentId
        )
        
        val bodyString = PayOrcSdk.instance.json.encodeToString(
            com.payorc.sdk.data.remote.dto.TabbyConfirmRequestDto.serializer(),
            tabbyConfirmRequest
        )
        val headers = generateAuthHeaders(bodyString)
        
        return try {
            apiService.confirmTabbyPayment(headers, bodyString)
        } catch (e: Exception) {
            handleApiError(
                e,
                { message, code ->
                    com.payorc.sdk.data.remote.dto.TabbyConfirmResponseDto(
                        message = message.ifBlank { "Failed to confirm Tabby payment" },
                        status = "fail",
                        code = code
                    )
                }
            )
        }
    }

    /**
     * Helper method to generate authenticated request headers with signature.
     * Encapsulates the repetitive logic of timestamp generation, signature creation, and header construction.
     */
    private fun generateAuthHeaders(bodyString: String): Map<String, String> {
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        val signature = SignatureGenerator.generate(merchantKey, merchantSecret, timestamp, bodyString)
        
        return PayOrcSdk.instance.getDeviceHeaders(context).toMutableMap().apply {
            put("merchant-key", merchantKey)
            put("merchant-secret", merchantSecret)
            put("X-Timestamp", timestamp)
            put("X-Signature", signature)
            put("Content-Type", "application/json")
        }
    }

    /**
     * Helper method to handle common API error responses.
     * Reduces duplication of error handling across all API methods.
     */
    private inline fun <reified T> handleApiError(
        exception: Exception,
        failureResponse: (message: String, code: String) -> T
    ): T {
        return when (exception) {
            is HttpException -> {
                val message = ErrorResponseExtractor.extractErrorMessage(exception)
                failureResponse(
                    message.ifBlank { "API request failed" },
                    exception.code().toString()
                )
            }
            is IOException -> failureResponse("Network error. Please check your internet connection.", "NETWORK_ERROR")
            else -> failureResponse(exception.message ?: "An unknown error occurred", "UNKNOWN_ERROR")
        }
    }
}
