package com.payorc.sdk.core.network

object PayOrcGatewayUrls {
    const val PRODUCTION_BASE_URL = "https://gateway.payorc.com/service-sdk/api/v1/"
//    const val SANDBOX_BASE_URL = "https://dev-gateway.payorc.com/service-sdk/api/v1/"
    const val SANDBOX_BASE_URL = "https://gateway.payorc.com/service-sdk/api/v1/"

    const val CHECKOUT_CUSTOMIZATION = "sdk/checkout/customization"
    const val PAYMENT = "sdk/payment"
    const val WALLET_PAYMENT = "sdk/wallet/payment"
    const val ADD_CARD = "sdk/add-card"
    const val CUSTOMER_CARDS = "sdk/customer/cards"
    const val TABBY_INIT = "sdk/tabby/init"
    const val TABBY_CONFIRM = "sdk/tabby/confirm"
}
