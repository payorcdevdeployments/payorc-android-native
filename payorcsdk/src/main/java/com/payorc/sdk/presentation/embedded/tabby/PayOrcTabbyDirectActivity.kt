package com.payorc.sdk.presentation.embedded.tabby

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.tabby.android.data.TabbyResult
import ai.tabby.android.factory.TabbyFactory
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.localization.PayorcLanguageProvider

class PayOrcTabbyDirectActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID).orEmpty()

        setContent {
            PayorcLanguageProvider {
                PayOrcTabbyDirectScreen(requestId = requestId)
            }
        }
    }

    companion object {
        const val EXTRA_REQUEST_ID = "extra.payorc.tabby.request_id"
    }
}

@Composable
private fun PayOrcTabbyDirectScreen(requestId: String) {
    val context = LocalContext.current
    val activity = context as? Activity
    val paymentRequest = PayOrcSdk.instance.currentCheckoutRequest

    if (paymentRequest == null) {
        LaunchedEffect(requestId) {
            PayOrcSdk.completeTabbyFlow(requestId, error = IllegalStateException("Missing payment request"))
            activity?.finish()
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val viewModel: EmbeddedTabbyViewModel = viewModel(
        key = "tabby_direct_$requestId",
        factory = EmbeddedTabbyViewModel.Factory(paymentRequest)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    val tabbyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val tabbyResult: TabbyResult? = if (android.os.Build.VERSION.SDK_INT >= 33) {
            result.data?.getParcelableExtra("extra.tabbyResult", TabbyResult::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra("extra.tabbyResult")
        }
        viewModel.handleTabbyResult(tabbyResult)
    }

    LaunchedEffect(Unit) {
        viewModel.startFlow()
    }

    LaunchedEffect(state) {
        when (val currentState = state) {
            is EmbeddedTabbyState.LaunchTabby -> {
                val intent = TabbyFactory.tabby().createCheckoutIntent(currentState.product)
                tabbyLauncher.launch(intent)
            }
            is EmbeddedTabbyState.Authorized -> {
                PayOrcSdk.completeTabbyFlow(
                    requestId = requestId,
                    merchantResponse = currentState.merchantResponse.ifEmpty {
                        mapOf(
                            "data" to emptyMap<String, Any?>(),
                            "message" to "Tabby authorized",
                            "status" to "success",
                            "code" to "00"
                        )
                    }
                )
                activity?.finish()
            }
            is EmbeddedTabbyState.Error -> {
                PayOrcSdk.completeTabbyFlow(
                    requestId = requestId,
                    error = currentState.cause ?: Throwable(currentState.message)
                )
                activity?.finish()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
