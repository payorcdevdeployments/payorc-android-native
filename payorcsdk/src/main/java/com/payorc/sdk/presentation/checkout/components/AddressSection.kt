package com.payorc.sdk.presentation.checkout.components

import com.payorc.sdk.presentation.checkout.components.PayOrcText

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BillingOrShippingAddressSection(
    line1: String,
    line2: String,
    city: String,
    province: String,
    country: String,
    pin: String,
    onLine1Change: (String) -> Unit,
    onLine2Change: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onProvinceChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onPinChange: (String) -> Unit
) {
    PayOrcTextField(
        value = line1,
        onValueChange = onLine1Change,
        label = { PayOrcText("Address Line 1") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(12.dp))

    PayOrcTextField(
        value = line2,
        onValueChange = onLine2Change,
        label = { PayOrcText("Address Line 2 (Optional)") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(12.dp))

    PayOrcTextField(
        value = city,
        onValueChange = onCityChange,
        label = { PayOrcText("City") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            PayOrcTextField(
                value = province,
                onValueChange = onProvinceChange,
                label = { PayOrcText("State/Province") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            PayOrcTextField(
                value = pin,
                onValueChange = onPinChange,
                label = { PayOrcText("ZIP/PIN") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))

    PayOrcTextField(
        value = country,
        onValueChange = onCountryChange,
        label = { PayOrcText("Country") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}
