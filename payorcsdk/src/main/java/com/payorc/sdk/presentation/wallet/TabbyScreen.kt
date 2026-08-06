package com.payorc.sdk.presentation.wallet

import ai.tabby.android.factory.TabbyFactory
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.payorc.sdk.localization.PayorcLanguageProvider
import com.payorc.sdk.presentation.checkout.components.PayOrcText

/**
 * Tabby Screen
 * 
 * Main screen for Tabby payment flow using official SDK.
 */
@Composable
fun TabbyScreen(
    viewModel: TabbyViewModel,
    onPaymentSuccess: (String) -> Unit,
    onPaymentError: (String) -> Unit,
    onPaymentCancelled: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val tabbyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("PayOrc_Tabby", "TabbyScreen result received: code=${result.resultCode}, data=${result.data}")
        if (result.resultCode == Activity.RESULT_OK) {
            onPaymentSuccess("Payment authorized")
        } else {
            android.util.Log.w("PayOrc_Tabby", "TabbyScreen activity cancelled or failed")
            onPaymentCancelled()
        }
    }

    PayorcLanguageProvider {
    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is TabbyUiState.Idle -> {
                // Should not happen if initiated correctly
            }
            is TabbyUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = com.payorc.sdk.ui.components.PayorcSdkUiConstants.loadingIndicatorColor
                )
            }
            is TabbyUiState.SessionCreated -> {
                LaunchedEffect(state.products) {
                    val product = state.products.firstOrNull()
                    if (product != null) {
                        val intent = TabbyFactory.tabby().createCheckoutIntent(product)
                        tabbyLauncher.launch(intent)
                    } else {
                        onPaymentError("No products available")
                    }
                }
            }
            is TabbyUiState.Success -> {
                LaunchedEffect(Unit) {
                    onPaymentSuccess("Payment authorized successfully")
                }
            }
            is TabbyUiState.Cancelled -> {
                LaunchedEffect(Unit) {
                    onPaymentCancelled()
                }
            }
            is TabbyUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    PayOrcText(text = "Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { onPaymentCancelled() }) {
                        PayOrcText("Close")
                    }
                }
            }
        }
    }
    }
}
