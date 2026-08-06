package com.payorc.sdk.domain.model

fun PayOrcCheckoutRequest.hasCustomerName(): Boolean = customerName.isNotBlank()

fun PayOrcCheckoutRequest.hasCustomerEmail(): Boolean = customerEmail.isNotBlank()

fun PayOrcCheckoutRequest.hasCustomerMobile(): Boolean = customerMobile.isNotBlank() && customerMobileCode.isNotBlank()

fun PayOrcAddress.isComplete(): Boolean =
    addressLine1.isNotBlank() &&
        city.isNotBlank() &&
        province.isNotBlank() &&
        country.isNotBlank() &&
        pin.isNotBlank()

fun PayOrcAddress.hasAnyAddressData(): Boolean =
    listOf(addressLine1, addressLine2, city, province, country, pin).any { it.isNotBlank() }
