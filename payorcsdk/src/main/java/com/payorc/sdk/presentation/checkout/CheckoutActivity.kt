package com.payorc.sdk.presentation.checkout

import com.payorc.sdk.presentation.checkout.components.PayOrcText

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import com.payorc.sdk.core.network.WebViewDiffLogger
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import ai.tabby.android.data.TabbyResult
import ai.tabby.android.factory.TabbyFactory
import android.annotation.SuppressLint
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.domain.model.*
import com.payorc.sdk.localization.PayorcLanguageProvider
import com.payorc.sdk.presentation.checkout.components.*
import com.payorc.sdk.ui.components.PayorcSdkUiConstants
import com.payorc.sdk.core.googlepay.GooglePayHelper

class CheckoutActivity : ComponentActivity() {
    private val viewModel: CheckoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launchMethod = intent.getStringExtra("EXTRA_LAUNCH_METHOD")
        setContent {
            PayorcLanguageProvider {
                Surface(color = Color.Transparent) {
                    PayOrcCheckoutContent(
                        viewModel = viewModel,
                        onClose = { finish() },
                        launchMethod = launchMethod
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayOrcCheckoutContent(
    viewModel: CheckoutViewModel = viewModel(),
    onClose: () -> Unit,
    launchMethod: String? = null
) {
    PayorcLanguageProvider {
        PayOrcCheckoutContentBody(
            viewModel = viewModel,
            onClose = onClose,
            launchMethod = launchMethod
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayOrcCheckoutContentBody(
    viewModel: CheckoutViewModel = viewModel(),
    onClose: () -> Unit,
    launchMethod: String? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val paymentState by viewModel.paymentState.collectAsState()
    val savedCards by viewModel.savedCards.collectAsState()
    val savedCardsLoading by viewModel.savedCardsLoading.collectAsState()
    val googlePayReady by viewModel.googlePayReady.collectAsState()

    val googlePayHelper = remember { GooglePayHelper(context as Activity) }

    val tabbyLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val tabbyResult = if (android.os.Build.VERSION.SDK_INT >= 33) {
                result.data?.getParcelableExtra("extra.tabbyResult", TabbyResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra("extra.tabbyResult")
            }
            viewModel.handleTabbyResult(tabbyResult)
        } else {
            viewModel.resetPaymentState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadConfig { config ->
            val gPayConfig = config.paymentMethods.find { it.type == "GOOGLE_PAY" }?.googlePayConfig
            if (gPayConfig != null) {
                googlePayHelper.isReadyToPay(gPayConfig)
            } else false
        }
    }

    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.Success) {
            val successState = paymentState as PaymentState.Success
            PayOrcCheckout.current3DSCallback?.onSuccess(successState.tokenData.transactionId)
            PayOrcCheckout.current3DSCallback = null

            PayOrcCheckout.currentCallback?.onAddCardSuccess(successState.tokenData)
            PayOrcCheckout.currentCallback?.onSuccessPayment(successState.merchantResponse)
            PayOrcCheckout.currentCallback = null

            if (launchMethod == "CARD") {
                onClose()
            }
        }
        
        if (paymentState is PaymentState.Failure) {
            if (launchMethod == "CARD") {
                PayOrcCheckout.currentCallback?.onFailure((paymentState as PaymentState.Failure).message)
                PayOrcCheckout.currentCallback = null
                onClose()
            }
        }
        
        if (paymentState is PaymentState.LaunchTabbySdk) {
            val product = (paymentState as PaymentState.LaunchTabbySdk).product
            val intent = TabbyFactory.tabby().createCheckoutIntent(product)
            tabbyLauncher.launch(intent)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (paymentState is PaymentState.RedirectTo3DS || paymentState is PaymentState.RedirectToTabby)
                    Color.White
                else
                    Color.Black.copy(alpha = 0.4f)
            )
            .then(
                if (paymentState !is PaymentState.RedirectTo3DS && paymentState !is PaymentState.RedirectToTabby) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
                } else Modifier
            ),
        contentAlignment = if (paymentState is PaymentState.RedirectTo3DS || paymentState is PaymentState.RedirectToTabby)
            Alignment.TopCenter
        else
            Alignment.BottomCenter
    ) {
        if (paymentState is PaymentState.RedirectTo3DS) {
            ThreeDSRedirectScreen(
                redirectUrl = (paymentState as PaymentState.RedirectTo3DS).url,
                onTokenCaptured = { tokenData, code, merchantResponse ->
                    if (code == "CARD_VERIFIED") {
                        viewModel.handleAddCardSuccess(tokenData)
                    } else {
                        viewModel.handlePaymentSuccess(tokenData, merchantResponse)
                    }
                },
                onFailure = { reason -> viewModel.reportFailure(reason) }
            )
        } else if (paymentState is PaymentState.RedirectToTabby) {
            TabbyRedirectScreen(
                redirectUrl = (paymentState as PaymentState.RedirectToTabby).url,
                onSuccess = { viewModel.handleTabbySuccess() },
                onFailure = { reason -> viewModel.reportFailure(reason) }
            )
        } else {
            val isProcessing = paymentState is PaymentState.Processing
            
            // Show processing screen at the top level for all flows (direct and standard)
            if (isProcessing) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .clickable(enabled = false) { /* Prevent click-through */ },
                    color = PayorcSdkUiConstants.brandColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                    ) {
                        // Drag handle
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.LightGray)
                            )
                        }
                        ProcessingScreen(
                            tokenData = (paymentState as PaymentState.Processing).tokenData,
                            message = (paymentState as PaymentState.Processing).message
                        )
                    }
                }
            } else if (launchMethod != "CARD") {
                // Only show the background surface if NOT in direct CARD launch method and NOT processing
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .clickable(enabled = false) { /* Prevent click through */ },
                    color = PayorcSdkUiConstants.brandColor
                ) {
                    when (val state = uiState) {
                        is CheckoutUiState.Loading -> LoadingScreen()
                        is CheckoutUiState.Success -> {
                            PaymentOptionsContent(
                                config = state.config,
                                paymentState = paymentState,
                                savedCards = savedCards,
                                savedCardsLoading = savedCardsLoading,
                                googlePayReady = googlePayReady,
                                onProcessPayment = { holder, number, expiry, cvv ->
                                    viewModel.processCardPayment(
                                        holder,
                                        number,
                                        expiry,
                                        cvv,
                                        state.config.paymentMethods.firstOrNull { it.type == "CARD" }?.schemes.orEmpty()
                                    )
                                },
                                onExecutePayment = { tokenData, cvv ->
                                    viewModel.executePaymentWithToken(tokenData, cvv)
                                },
                                onUpdateCheckoutRequest = { updatedRequest ->
                                    viewModel.updateCheckoutRequest { updatedRequest }
                                },
                                onTabbyClick = { tabbyConfig ->
                                    viewModel.initiateTabbyPayment(tabbyConfig)
                                },
                                onGooglePayClick = {
                                    val gPayConfig = state.config.paymentMethods.find { it.type == "GOOGLE_PAY" }?.googlePayConfig
                                    val request = PayOrcSdk.instance.currentCheckoutRequest
                                    if (gPayConfig != null && request != null) {
                                        val requestJson = googlePayHelper.createPaymentDataRequest(request, gPayConfig)
                                        val paymentDataRequest = com.google.android.gms.wallet.PaymentDataRequest.fromJson(requestJson)
                                        
                                        com.google.android.gms.wallet.AutoResolveHelper.resolveTask(
                                            googlePayHelper.getPaymentLauncher().loadPaymentData(paymentDataRequest),
                                            context as Activity,
                                            991
                                        )
                                    }
                                },
                                onClose = onClose,
                                onRetry = { viewModel.resetPaymentState() },
                                launchMethod = launchMethod
                            )
                        }
                        is CheckoutUiState.Error -> ErrorScreen(state.message)
                    }
                }
            } else if (uiState is CheckoutUiState.Success) {
                // If direct launch, we still need to render PaymentOptionsContent 
                // because it hosts the ModalBottomSheet, but it is invisible (no background surface).
                PaymentOptionsContent(
                    config = (uiState as CheckoutUiState.Success).config,
                    paymentState = paymentState,
                    onProcessPayment = { holder, number, expiry, cvv ->
                        viewModel.processCardPayment(
                            holder,
                            number,
                            expiry,
                            cvv,
                            (uiState as CheckoutUiState.Success).config.paymentMethods.firstOrNull { it.type == "CARD" }?.schemes.orEmpty()
                        )
                    },
                    onExecutePayment = { tokenData, cvv -> viewModel.executePaymentWithToken(tokenData, cvv) },
                    onUpdateCheckoutRequest = { updatedRequest -> viewModel.updateCheckoutRequest { updatedRequest } },
                    onTabbyClick = { viewModel.initiateTabbyPayment(it) },
                    onGooglePayClick = {},
                    onClose = onClose,
                    onRetry = { viewModel.resetPaymentState() },
                    launchMethod = launchMethod
                )
            } else if (uiState is CheckoutUiState.Loading) {
                LoadingScreen()
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = PayorcSdkUiConstants.loadingIndicatorColor)
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PayOrcText(text = "Error", style = MaterialTheme.typography.headlineMedium, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
            PayOrcText(text = message, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentOptionsContent(
    config: CheckoutConfig,
    paymentState: PaymentState,
    savedCards: List<SavedCard> = emptyList(),
    savedCardsLoading: Boolean = false,
    googlePayReady: Boolean = false,
    onProcessPayment: (String, String, String, String) -> Unit,
    onExecutePayment: (PaymentTokenData, String) -> Unit,
    onUpdateCheckoutRequest: (PayOrcCheckoutRequest) -> Unit,
    onTabbyClick: (TabbyConfig) -> Unit,
    onGooglePayClick: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    launchMethod: String? = null
) {
    var showCardForm by remember { mutableStateOf(false) }
    var showSavedCardList by remember { mutableStateOf(false) }
    var showSavedCardsBottomSheet by remember { mutableStateOf(false) }
    var showUseNewCardBottomSheet by remember { mutableStateOf(launchMethod == "CARD") }
    var selectedMethod by remember { mutableStateOf<String?>(if (launchMethod == "CARD") "CARD" else null) }
    var selectedCardData by remember { mutableStateOf<SavedCard?>(null) }
    var tempSelectedCardForSheet by remember { mutableStateOf<SavedCard?>(null) }
    var savedCardCvv by remember { mutableStateOf("") }

    // Card form state variables
    var cardHolder by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }

    // State for initial sheet loading
    var isOpeningCardSheet by remember { mutableStateOf(launchMethod == "CARD") }

    // New Card Bottom Sheet state variables (separate MM/YY fields)
    var newCardCardHolder by remember { mutableStateOf("") }
    var newCardNumber by remember { mutableStateOf("") }
    var newCardExpiryMonth by remember { mutableStateOf("") }
    var newCardExpiryYear by remember { mutableStateOf("") }
    var newCardCvv by remember { mutableStateOf("") }
    var newCardIsLoading by remember { mutableStateOf(false) }

    // Validation state for each field
    var cardHolderError by remember { mutableStateOf<String?>(null) }
    var cardNumberError by remember { mutableStateOf<String?>(null) }
    var expiryError by remember { mutableStateOf<String?>(null) }
    var cvvError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var billingError by remember { mutableStateOf<String?>(null) }
    var shippingError by remember { mutableStateOf<String?>(null) }

    // New card form validation
    var newCardHolderError by remember { mutableStateOf<String?>(null) }
    var newCardNumberError by remember { mutableStateOf<String?>(null) }
    var newCardExpiryMonthError by remember { mutableStateOf<String?>(null) }
    var newCardExpiryYearError by remember { mutableStateOf<String?>(null) }
    var newCardCvvError by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(paymentState) {
        if (paymentState is PaymentState.ValidationError) {
            validationError = (paymentState as PaymentState.ValidationError).message
        } else if (paymentState is PaymentState.Processing || paymentState is PaymentState.Success || paymentState is PaymentState.RedirectTo3DS || paymentState is PaymentState.RedirectToTabby) {
            validationError = null
            showUseNewCardBottomSheet = false
        }
    }

    val checkoutRequest = PayOrcSdk.instance.currentCheckoutRequest
    
    // Capture the INITIAL state of customer details from the app
    val initialCustomerName = remember { checkoutRequest?.customerName ?: "" }
    val initialCustomerEmail = remember { checkoutRequest?.customerEmail ?: "" }
    val initialCustomerMobile = remember { checkoutRequest?.customerDetails?.mobile ?: "" }
    
    val isNameProvidedByApp = initialCustomerName.isNotBlank()
    val isEmailProvidedByApp = initialCustomerEmail.isNotBlank()
    val isMobileProvidedByApp = initialCustomerMobile.isNotBlank()
    
    val showNameField = config.fieldVisibility.nameVisible && !isNameProvidedByApp
    val showEmailField = config.fieldVisibility.emailVisible && !isEmailProvidedByApp
    val showMobileField = config.fieldVisibility.mobileVisible && !isMobileProvidedByApp
    val showBillingSection = config.flowConfig.showBillingSection && checkoutRequest?.billingAddress?.isComplete() == false
    val showShippingSection = config.fieldVisibility.shippingVisible && config.fieldVisibility.shippingAddressVisible && checkoutRequest?.shippingAddress?.isComplete() == false

    var customerName by rememberSaveable(Unit) { mutableStateOf(initialCustomerName) }
    var customerEmail by rememberSaveable(Unit) { mutableStateOf(initialCustomerEmail) }
    var customerMobileCode by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.customerMobileCode.orEmpty()) }
    var customerMobile by rememberSaveable(Unit) { mutableStateOf(initialCustomerMobile) }

    var billingLine1 by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.billingAddress?.addressLine1.orEmpty()) }
    var billingLine2 by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.billingAddress?.addressLine2.orEmpty()) }
    var billingCity by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.billingAddress?.city.orEmpty()) }
    var billingProvince by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.billingAddress?.province.orEmpty()) }
    var billingCountry by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.billingAddress?.country.orEmpty()) }
    var billingPin by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.billingAddress?.pin.orEmpty()) }

    var shippingLine1 by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.shippingAddress?.addressLine1.orEmpty()) }
    var shippingLine2 by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.shippingAddress?.addressLine2.orEmpty()) }
    var shippingCity by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.shippingAddress?.city.orEmpty()) }
    var shippingProvince by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.shippingAddress?.province.orEmpty()) }
    var shippingCountry by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.shippingAddress?.country.orEmpty()) }
    var shippingPin by rememberSaveable(Unit) { mutableStateOf(checkoutRequest?.shippingAddress?.pin.orEmpty()) }

    fun updateCustomerInfo() {
        checkoutRequest?.let {
            onUpdateCheckoutRequest(
                it.copy(
                    customerDetails = (it.customerDetails ?: CustomerDetails(mCustomerId = "")).copy(
                        name = customerName,
                        email = customerEmail,
                        code = customerMobileCode,
                        mobile = customerMobile
                    )
                )
            )
        }
    }

    fun updateBillingInfo() {
        checkoutRequest?.let {
            onUpdateCheckoutRequest(
                it.copy(
                    billingDetails = BillingDetails(
                        addressLine1 = billingLine1,
                        addressLine2 = billingLine2,
                        city = billingCity,
                        province = billingProvince,
                        country = billingCountry,
                        pin = billingPin
                    )
                )
            )
        }
    }

    fun updateShippingInfo() {
        checkoutRequest?.let {
            onUpdateCheckoutRequest(
                it.copy(
                    shippingDetails = (it.shippingDetails ?: ShippingDetails()).copy(
                        addressLine1 = shippingLine1,
                        addressLine2 = shippingLine2,
                        city = shippingCity,
                        province = shippingProvince,
                        country = shippingCountry,
                        pin = shippingPin
                    )
                )
            )
        }
    }

    val validator = remember { com.payorc.sdk.domain.usecase.PaymentFormValidator() }

    fun validatePaymentForm(): Boolean {
        var isValid = true
        if (showNameField) {
            val result = validator.validateName(customerName)
            nameError = result.errorMessage
            if (!result.isValid) isValid = false
        } else nameError = null
        if (showEmailField) {
            val result = validator.validateEmail(customerEmail)
            emailError = result.errorMessage
            if (!result.isValid) isValid = false
        } else emailError = null
        if (showMobileField) {
            val result = validator.validateMobile(customerMobile)
            mobileError = result.errorMessage
            if (!result.isValid) isValid = false
        } else mobileError = null
        if (!isNameProvidedByApp) {
            val result = validator.validateCardHolder(cardHolder)
            cardHolderError = result.errorMessage
            if (!result.isValid) isValid = false
        } else cardHolderError = null
        val numberResult = validator.validateCardNumber(cardNumber)
        cardNumberError = numberResult.errorMessage
        if (!numberResult.isValid) isValid = false
        val expiryResult = validator.validateExpiry(expiry)
        expiryError = expiryResult.errorMessage
        if (!expiryResult.isValid) isValid = false
        val cvvResult = validator.validateCvv(cvv)
        cvvError = cvvResult.errorMessage
        if (!cvvResult.isValid) isValid = false
        if (showBillingSection) {
            val result = validator.validateBillingAddress(billingLine1, billingCity, billingCountry, true)
            billingError = result.errorMessage
            if (!result.isValid) isValid = false
        } else billingError = null
        if (showShippingSection) {
            val result = validator.validateShippingAddress(shippingLine1, shippingCity, shippingCountry, true)
            shippingError = result.errorMessage
            if (!result.isValid) isValid = false
        } else shippingError = null
        return isValid
    }

    val buttonColor = PayorcSdkUiConstants.buttonBackgroundColor

    // Hide the main options content if launching directly into card sheet
    if (launchMethod != "CARD") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
        ) {
            // Drag handle
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.LightGray))
            }

            when (paymentState) {
                is PaymentState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Success", tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        PayOrcText(text = "Payment Successful", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        PayOrcText(text = "Transaction ID: ${paymentState.tokenData.transactionId}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onClose,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                        ) {
                            PayOrcText("Done", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                is PaymentState.Processing -> {
                    if (paymentState.tokenData?.scheme == "TABBY" || paymentState.message.contains("Tabby", ignoreCase = true)) {
                        TabbyProcessingScreen(message = paymentState.message)
                    } else {
                        ProcessingScreen(tokenData = paymentState.tokenData, message = paymentState.message)
                    }
                }
                else -> {
                    if (showCardForm && paymentState !is PaymentState.Failure) {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(horizontal = 16.dp, vertical = 16.dp).verticalScroll(rememberScrollState())
                        ) {
                            PayOrcText(text = "Use New Card", style = MaterialTheme.typography.titleMedium)
                            PayOrcText(text = "Enter your card details securely", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(16.dp))
                            if (showNameField || showEmailField || showMobileField || showBillingSection || showShippingSection) {
                                PayOrcText(text = "Customer Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            if (showNameField) {
                                PayOrcTextField(value = customerName, onValueChange = { customerName = it; updateCustomerInfo() }, label = { PayOrcText("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = nameError != null, supportingText = { if (nameError != null) PayOrcText(nameError!!, color = Color.Red, style = MaterialTheme.typography.labelSmall) })
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (showEmailField) {
                                PayOrcTextField(value = customerEmail, onValueChange = { customerEmail = it; updateCustomerInfo() }, label = { PayOrcText("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = emailError != null, supportingText = { if (emailError != null) PayOrcText(emailError!!, color = Color.Red, style = MaterialTheme.typography.labelSmall) })
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (showMobileField) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    PayOrcTextField(value = customerMobileCode, onValueChange = { customerMobileCode = it; updateCustomerInfo() }, label = { PayOrcText("Code") }, modifier = Modifier.width(100.dp), singleLine = true, placeholder = { PayOrcText("+971") })
                                    PayOrcTextField(value = customerMobile, onValueChange = { customerMobile = it; updateCustomerInfo() }, label = { PayOrcText("Phone") }, modifier = Modifier.weight(1f), singleLine = true, isError = mobileError != null, supportingText = { if (mobileError != null) PayOrcText(mobileError!!, color = Color.Red, style = MaterialTheme.typography.labelSmall) })
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (showBillingSection) {
                                PayOrcText(text = "Billing Address", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))
                                BillingOrShippingAddressSection(line1 = billingLine1, line2 = billingLine2, city = billingCity, province = billingProvince, country = billingCountry, pin = billingPin, onLine1Change = { billingLine1 = it; updateBillingInfo() }, onLine2Change = { billingLine2 = it; updateBillingInfo() }, onCityChange = { billingCity = it; updateBillingInfo() }, onProvinceChange = { billingProvince = it; updateBillingInfo() }, onCountryChange = { billingCountry = it; updateBillingInfo() }, onPinChange = { billingPin = it; updateBillingInfo() })
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (showShippingSection) {
                                PayOrcText(text = "Shipping Address", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))
                                BillingOrShippingAddressSection(line1 = shippingLine1, line2 = shippingLine2, city = shippingCity, province = shippingProvince, country = shippingCountry, pin = shippingPin, onLine1Change = { shippingLine1 = it; updateShippingInfo() }, onLine2Change = { shippingLine2 = it; updateShippingInfo() }, onCityChange = { shippingCity = it; updateShippingInfo() }, onProvinceChange = { shippingProvince = it; updateShippingInfo() }, onCountryChange = { shippingCountry = it; updateShippingInfo() }, onPinChange = { shippingPin = it; updateShippingInfo() })
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            if (!isNameProvidedByApp) {
                                PayOrcText(text = "Card Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                PayOrcTextField(value = cardHolder, onValueChange = { cardHolder = it }, label = { PayOrcText("Cardholder Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true, isError = cardHolderError != null, supportingText = { if (cardHolderError != null) PayOrcText(cardHolderError!!, color = Color.Red, style = MaterialTheme.typography.labelSmall) })
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            CardNumberField(value = cardNumber, onValueChange = { cardNumber = it }, modifier = Modifier.fillMaxWidth(), isError = cardNumberError != null, errorMessage = cardNumberError)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                ExpiryField(value = expiry, onValueChange = { expiry = it }, modifier = Modifier.weight(1f), isError = expiryError != null, errorMessage = expiryError)
                                Spacer(modifier = Modifier.width(12.dp))
                                CVVField(value = cvv, onValueChange = { cvv = it }, modifier = Modifier.weight(1f), isError = cvvError != null, errorMessage = cvvError)
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { if (validatePaymentForm()) { val nameToUse = if (isNameProvidedByApp) customerName else cardHolder; onProcessPayment(nameToUse, cardNumber, expiry, cvv) } },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                            ) { PayOrcText("Pay Now", fontWeight = FontWeight.Bold, color = Color.White) }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                        if (paymentState is PaymentState.Failure) {
                            FailedHeader(errorMessage = paymentState.message, tokenData = null)
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                                PayOrcText(text = "TRY ANOTHER METHOD", textAlign = TextAlign.Center, color = Color.Gray, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        } else if (showCardForm) {
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }

                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(horizontal = 16.dp, vertical = 16.dp)) {
                            val googlePayMethod = config.paymentMethods.find { it.type == "GOOGLE_PAY" }
                            if (googlePayMethod != null && googlePayReady) {
                                item {
                                    PaymentMethodTile(method = googlePayMethod, isSelected = selectedMethod == googlePayMethod.type, onClick = { if (paymentState is PaymentState.Failure) onRetry(); selectedMethod = googlePayMethod.type; selectedCardData = null; showSavedCardList = false })
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            if (paymentState is PaymentState.Failure && savedCards.isNotEmpty()) {
                                item {
                                    PaymentMethodTile(method = PaymentMethod(type = "STORED_CARD", sequence = 0, schemes = emptyList()), isSelected = selectedMethod == "STORED_CARD", onClick = { selectedMethod = "STORED_CARD"; selectedCardData = null; showCardForm = false; showSavedCardsBottomSheet = true })
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            val otherMethods = config.paymentMethods.filter { it.type != "GOOGLE_PAY" && it.type != "STORED_CARD" }
                            if (otherMethods.isEmpty() && savedCards.isEmpty() && (googlePayMethod == null || !googlePayReady) && !savedCardsLoading) {
                                item { PayOrcText("No payment methods available.", modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center) }
                            } else {
                                items(otherMethods) { method ->
                                    PaymentMethodTile(method = method, isSelected = selectedMethod == method.type, onClick = { if (paymentState is PaymentState.Failure) onRetry(); selectedMethod = method.type; selectedCardData = null })
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            if (config.paymentMethods.isNotEmpty() || showSavedCardList || savedCards.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    when {
                                        selectedMethod == "GOOGLE_PAY" && googlePayReady -> {
                                            val gPayConfig = config.paymentMethods.find { it.type == "GOOGLE_PAY" }?.googlePayConfig
                                            if (gPayConfig != null && checkoutRequest != null) {
                                                GooglePayButtonWidget(googlePayConfig = gPayConfig, checkoutRequest = checkoutRequest, isEnabled = true, modifier = Modifier.fillMaxWidth())
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PayorcSdkUiConstants.buttonDisabledBackgroundColor)) { PayOrcText("Cancel",color = PayorcSdkUiConstants.buttonDisabledForegroundColor, fontWeight = FontWeight.Medium) }
                                            } else {
                                                Button(onClick = { if (selectedMethod == "GOOGLE_PAY") onGooglePayClick() }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = buttonColor)) { PayOrcText(text = "Confirm", fontWeight = FontWeight.Bold, color = Color.White) }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PayorcSdkUiConstants.buttonDisabledBackgroundColor)) { PayOrcText("Cancel",color = PayorcSdkUiConstants.buttonDisabledForegroundColor ,fontWeight = FontWeight.Medium) }
                                            }
                                        }
                                        selectedMethod == "TABBY" -> {
                                            val tabbyConfig = config.paymentMethods.find { it.type == "TABBY" }?.tabbyConfig
                                            if (tabbyConfig != null && checkoutRequest != null) {
                                                TabbyButtonWidget(isEnabled = true, buttonColor = buttonColor, modifier = Modifier.fillMaxWidth(), onClick = { onTabbyClick(tabbyConfig) })
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PayorcSdkUiConstants.buttonDisabledBackgroundColor)) { PayOrcText("Cancel", color = PayorcSdkUiConstants.buttonDisabledForegroundColor, fontWeight = FontWeight.Medium) }
                                            } else {
                                                Button(onClick = { /* Fallback */ }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = buttonColor)) { PayOrcText(text = "Confirm", fontWeight = FontWeight.Bold, color = Color.White) }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PayorcSdkUiConstants.buttonDisabledBackgroundColor)) { PayOrcText("Cancel", color = PayorcSdkUiConstants.buttonDisabledForegroundColor, fontWeight = FontWeight.Medium) }
                                            }
                                        }
                                        else -> {
                                            Button(
                                                onClick = {
                                                    if (selectedMethod == "CARD") { isOpeningCardSheet = true; showUseNewCardBottomSheet = true }
                                                    else if (selectedMethod == "STORED_CARD" && selectedCardData != null) { onExecutePayment(PaymentTokenData(paymentToken = selectedCardData!!.paymentToken, transactionId = "", orderId = "", scheme = selectedCardData!!.cardScheme, maskCardNumber = selectedCardData!!.maskCardNumber), "") }
                                                },
                                                enabled = selectedMethod != null,
                                                modifier = Modifier.fillMaxWidth().height(PayorcSdkUiConstants.buttonHeight),
                                                shape = RoundedCornerShape(PayorcSdkUiConstants.buttonBorderRadius),
                                                colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                                            ) {
                                                if (isOpeningCardSheet) {
                                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PayorcSdkUiConstants.loadingIndicatorColor, strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    PayOrcText(text = "Opening...", fontWeight = FontWeight.Bold)
                                                } else { PayOrcText(text = "Confirm", fontWeight = FontWeight.Bold, color = Color.White) }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PayorcSdkUiConstants.buttonDisabledBackgroundColor)) { PayOrcText("Cancel", color = PayorcSdkUiConstants.buttonDisabledForegroundColor,  fontWeight = FontWeight.Medium) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            UniversalFooter(config = config)
        }
    }

    if (showSavedCardsBottomSheet) {
        val cornerRadius = PayorcSdkUiConstants.bottomSheetConfig?.cornerRadius ?: 20.dp
        val paddingConfig = PayorcSdkUiConstants.bottomSheetConfig?.padding
        
        ModalBottomSheet(
            onDismissRequest = {
                showSavedCardsBottomSheet = false
                tempSelectedCardForSheet = null
                savedCardCvv = ""
            },
            shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
            containerColor = PayorcSdkUiConstants.brandColor,
            scrimColor = Color.Black.copy(alpha = 0.32f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = paddingConfig?.left ?: 16.dp, top = paddingConfig?.top ?: 0.dp, end = paddingConfig?.right ?: 16.dp, bottom = paddingConfig?.bottom ?: 24.dp)
            ) {
                PayOrcText(text = "Select Card", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                if (savedCardsLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PayorcSdkUiConstants.loadingIndicatorColor) }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(savedCards) { card ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { tempSelectedCardForSheet = card }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = tempSelectedCardForSheet?.paymentToken == card.paymentToken, onClick = { tempSelectedCardForSheet = card })
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    PayOrcText(text = card.maskCardNumber, fontWeight = FontWeight.Bold)
                                    PayOrcText(text = card.cardScheme, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (tempSelectedCardForSheet != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        PayOrcTextField(value = savedCardCvv, onValueChange = { if (it.length <= 4) savedCardCvv = it }, label = { PayOrcText("CVV") }, placeholder = { PayOrcText("Enter 3-digit CVV") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), visualTransformation = PasswordVisualTransformation())
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (tempSelectedCardForSheet != null && savedCardCvv.isNotBlank()) {
                                onExecutePayment(PaymentTokenData(paymentToken = tempSelectedCardForSheet!!.paymentToken, transactionId = "", orderId = "", scheme = tempSelectedCardForSheet!!.cardScheme, maskCardNumber = tempSelectedCardForSheet!!.maskCardNumber), savedCardCvv)
                                showSavedCardsBottomSheet = false; tempSelectedCardForSheet = null; savedCardCvv = ""
                            }
                        },
                        enabled = tempSelectedCardForSheet != null && savedCardCvv.length >= 3,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = buttonColor)
                    ) { PayOrcText("Confirm Payment", color = Color.White, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.height(16.dp))
                    UniversalFooter(config = config)
                }
            }
        }
    }

    if (showUseNewCardBottomSheet) {
        val cornerRadius = PayorcSdkUiConstants.bottomSheetConfig?.cornerRadius ?: 20.dp
        
        ModalBottomSheet(
            onDismissRequest = {
                showUseNewCardBottomSheet = false
                newCardCardHolder = ""; newCardNumber = ""; newCardExpiryMonth = ""; newCardExpiryYear = ""; newCardCvv = ""
                newCardHolderError = null; newCardNumberError = null; newCardExpiryMonthError = null; newCardExpiryYearError = null; newCardCvvError = null
                validationError = null
                if (launchMethod == "CARD") onClose()
            },
            shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
            containerColor = PayorcSdkUiConstants.brandColor,
            scrimColor = Color.Black.copy(alpha = 0.32f),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            LaunchedEffect(showUseNewCardBottomSheet) {
                if (showUseNewCardBottomSheet) {
                    kotlinx.coroutines.delay(800)
                    isOpeningCardSheet = false
                }
            }
            UseNewCardBottomSheet(
                isButtonEnabled = (newCardNumber.isNotBlank() && newCardExpiryMonth.isNotBlank() && newCardExpiryYear.isNotBlank() && newCardCvv.isNotBlank()),
                cardHolderName = newCardCardHolder, onCardHolderNameChange = { newCardCardHolder = it },
                cardNumber = newCardNumber, onCardNumberChange = { newCardNumber = it },
                expiryMonth = newCardExpiryMonth, onExpiryMonthChange = { newCardExpiryMonth = it },
                expiryYear = newCardExpiryYear, onExpiryYearChange = { newCardExpiryYear = it },
                cvv = newCardCvv, onCvvChange = { newCardCvv = it },
                cardHolderError = newCardHolderError, cardNumberError = newCardNumberError, expiryMonthError = newCardExpiryMonthError, expiryYearError = newCardExpiryYearError, cvvError = newCardCvvError,
                onCardHolderFocused = { newCardHolderError = null },
                onCardNumberFocused = { newCardNumberError = null },
                onExpiryMonthFocused = { newCardExpiryMonthError = null },
                onExpiryYearFocused = { newCardExpiryYearError = null },
                onCvvFocused = { newCardCvvError = null },
                isLoading = newCardIsLoading || isOpeningCardSheet,
                buttonColor = PayorcSdkUiConstants.brandColor,
                showCardholderField = !isNameProvidedByApp, showCardNumberField = true, showExpiryFields = true, showCvvField = true,
                onConfirm = {
                    android.util.Log.d("PayOrcActivity", "Confirm button clicked in UseNewCardBottomSheet")
                    var hasError = false
                    if (!isNameProvidedByApp) { val nameResult = validator.validateCardHolder(newCardCardHolder); newCardHolderError = nameResult.errorMessage; if (!nameResult.isValid) hasError = true }
                    val numberResult = validator.validateCardNumber(newCardNumber); newCardNumberError = numberResult.errorMessage; if (!numberResult.isValid) hasError = true
                    val combinedExpiry = newCardExpiryMonth + newCardExpiryYear
                    newCardExpiryMonthError = null; newCardExpiryYearError = null
                    val monthResult = validator.validateExpiryMonth(newCardExpiryMonth); val yearResult = validator.validateExpiryYear(newCardExpiryYear)
                    if (!monthResult.isValid) { newCardExpiryMonthError = monthResult.errorMessage; hasError = true }
                    if (!yearResult.isValid) { newCardExpiryYearError = yearResult.errorMessage; hasError = true }
                    if (monthResult.isValid && yearResult.isValid) { val expiryResult = validator.validateExpiry(combinedExpiry); if (!expiryResult.isValid) { newCardExpiryMonthError = expiryResult.errorMessage; hasError = true } }
                    val cvvResult = validator.validateCvv(newCardCvv); newCardCvvError = cvvResult.errorMessage; if (!cvvResult.isValid) hasError = true
                    
                    if (!hasError) {
                        android.util.Log.d("PayOrcActivity", "Form valid, calling onProcessPayment")
                        val nameToUse = if (isNameProvidedByApp) customerName else newCardCardHolder
                        newCardIsLoading = true
                        onProcessPayment(nameToUse, newCardNumber, combinedExpiry, newCardCvv)
                        // Do not close sheet here; wait for paymentState to transition
                        newCardCardHolder = ""; newCardNumber = ""; newCardExpiryMonth = ""; newCardExpiryYear = ""; newCardCvv = ""
                        newCardIsLoading = false
                    } else {
                        android.util.Log.w("PayOrcActivity", "Form has errors, onProcessPayment NOT called")
                    }
                },
                onClose = {
                    showUseNewCardBottomSheet = false; isOpeningCardSheet = false
                    newCardCardHolder = ""; newCardNumber = ""; newCardExpiryMonth = ""; newCardExpiryYear = ""; newCardCvv = ""
                    validationError = null
                    if (launchMethod == "CARD") onClose()
                },
                payorcLogoUrl = config.companyAssets.sdkPayorcLogoUrl,
                globalError = validationError
            )
        }
    }
}

@Composable
fun UniversalFooter(config: CheckoutConfig) {
    PoweredByPayOrc(logoUrl = config.companyAssets.sdkPayorcLogoUrl)
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabbyRedirectScreen(
    redirectUrl: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        TopAppBar(
            title = { PayOrcText("") },
            navigationIcon = {
                IconButton(onClick = { onFailure("User cancelled") }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(3.dp), color = Color(0xFF39FFBD), trackColor = Color.Transparent)
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView.setWebContentsDebuggingEnabled(true)
                    android.webkit.WebView(context).apply {
                        setBackgroundColor(android.graphics.Color.WHITE)
                        
                        // Enable cookies and third-party cookies (critical for bank 3DS/OTP verification domains)
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            allowFileAccess = true
                            allowContentAccess = true
                            saveFormData = true
                            
                            // Force initial scale to ensure iframes render at intended size
                            setInitialScale(1)

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                allowUniversalAccessFromFileURLs = true
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            
                            // "Clean" mobile User-Agent (strips wv/Version/4.0 identifiers)
                            userAgentString = userAgentString
                                .replace("; wv", "")
                                .replace("Version/4.0 ", "")
                        }

                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                android.util.Log.d("PayOrcWebView", "Tabby Progress: $newProgress%")
                                isLoading = newProgress < 100
                            }

                            override fun onCreateWindow(
                                view: android.webkit.WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                val transport = resultMsg?.obj as? android.webkit.WebView.WebViewTransport
                                transport?.webView = view
                                resultMsg?.sendToTarget()
                                return true
                            }
                        }

                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                android.util.Log.d("PayOrcWebView", "Tabby Page started: $url")
                                isLoading = true
                                cookieManager.flush()
                            }
                            
                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                android.util.Log.d("PayOrcWebView", "Tabby Page finished: $url")
                                isLoading = false
                                cookieManager.flush()
                            }

                            override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    android.util.Log.e("PayOrcWebView", "Tabby Error: ${error?.description} (${error?.errorCode}) at ${request?.url}")
                                }
                            }

                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: ""
                                when {
                                    url.contains("payorc.sdk/tabby/success") -> { onSuccess(); return true }
                                    url.contains("payorc.sdk/tabby/cancel") -> { onFailure("Tabby payment cancelled"); return true }
                                    url.contains("payorc.sdk/tabby/failure") -> { onFailure("Tabby payment failed"); return true }
                                }
                                return false
                            }
                        }
                        loadUrl(redirectUrl)
                    }
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreeDSRedirectScreen(
    redirectUrl: String,
    onTokenCaptured: (PaymentTokenData, String, Map<String, Any?>) -> Unit,
    onFailure: (String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        TopAppBar(
            title = { PayOrcText("") },
            navigationIcon = {
                IconButton(onClick = { onFailure("User cancelled") }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        )
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = PayorcSdkUiConstants.brandColor,
                trackColor = Color.Transparent
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView.setWebContentsDebuggingEnabled(true)
                    android.webkit.WebView(context).apply {
                        setBackgroundColor(android.graphics.Color.WHITE)
                        
                        // Force hardware acceleration for iframe compositing
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                        // Enable cookies and third-party cookies (critical for bank 3DS/OTP verification domains)
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            cookieManager.setAcceptThirdPartyCookies(this, true)
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            allowFileAccess = true
                            allowContentAccess = true
                            saveFormData = true
                            
                            // Force initial scale to ensure iframes render at intended size
                            setInitialScale(1)

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                                allowUniversalAccessFromFileURLs = true
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                safeBrowsingEnabled = false
                            }
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            
                            // "Clean" mobile User-Agent (strips wv/Version/4.0 identifiers)
                            userAgentString = userAgentString
                                .replace("; wv", "")
                                .replace("Version/4.0 ", "")
                        }

                        addJavascriptInterface(object {
                            @android.webkit.JavascriptInterface
                            fun postMessage(message: String) {
                                post { handlePostback(message, onTokenCaptured, onFailure) }
                            }
                        }, "Android")

                        webChromeClient = object : android.webkit.WebChromeClient() {
                            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                android.util.Log.d("PayOrcWebView", "Progress: $newProgress%")
                                isLoading = newProgress < 100
                            }

                            override fun onCreateWindow(
                                view: android.webkit.WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                val transport = resultMsg?.obj as? android.webkit.WebView.WebViewTransport
                                transport?.webView = view
                                resultMsg?.sendToTarget()
                                return true
                            }

                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                val msg = consoleMessage?.message() ?: ""
                                val source = consoleMessage?.sourceId() ?: ""
                                val line = consoleMessage?.lineNumber() ?: 0
                                android.util.Log.d("PayOrc3DS_Diag", "Console [$source:$line]: $msg")
                                
                                if (msg.contains("payment_token") || msg.contains("CARD_VERIFIED")) {
                                    handlePostback(msg, onTokenCaptured, onFailure)
                                }
                                return true
                            }

                            override fun onJsAlert(view: android.webkit.WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                                android.util.Log.d("PayOrcWebView", "JS Alert: $message")
                                return super.onJsAlert(view, url, message, result)
                            }
                        }

                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onReceivedSslError(view: android.webkit.WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                                android.util.Log.w("PayOrcWebView", "SSL Error: $error")
                                handler?.proceed()
                            }

                            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                android.util.Log.d("PayOrcWebView", "Page started: $url")
                                isLoading = true
                                cookieManager.flush()
                                
                                view?.let { 
                                    WebViewDiffLogger.dumpSettings("PayOrc_Native", it)
                                }
                            }

                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                android.util.Log.d("PayOrc3DS_Diag", "Page Finished: $url")
                                isLoading = false
                                cookieManager.flush()
                                
                                // DOM Inspection
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var results = {};
                                        results.url = window.location.href;
                                        results.iframes = Array.from(document.querySelectorAll('iframe')).map(i => ({
                                            id: i.id,
                                            src: i.src,
                                            visible: i.offsetWidth > 0 && i.offsetHeight > 0,
                                            rect: i.getBoundingClientRect()
                                        }));
                                        results.otpInput = !!document.querySelector('input[type="password"], input[type="text"][id*="otp"], input[id*="sms"]');
                                        results.bodySize = { w: document.body.offsetWidth, h: document.body.offsetHeight };
                                        return JSON.stringify(results);
                                    })()
                                    """.trimIndent()
                                ) { result ->
                                    android.util.Log.d("PayOrc3DS_DOM", "DOM State: $result")
                                }

                                view?.evaluateJavascript(
                                    "(function() {" +
                                            "if (window.__payOrcHooked) return;" +
                                            "window.__payOrcHooked = true;" +
                                            "window.parent.postMessage = function(msg) { try { Android.postMessage(typeof msg === 'string' ? msg : JSON.stringify(msg)); } catch(e){} };" +
                                            "window.top.postMessage = function(msg) { try { Android.postMessage(typeof msg === 'string' ? msg : JSON.stringify(msg)); } catch(e){} };" +
                                    "})()"
                                ) { }
                            }

                            override fun onReceivedError(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                super.onReceivedError(view, request, error)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                    android.util.Log.e("PayOrcWebView", "Error: ${error?.description} (${error?.errorCode}) at ${request?.url}")
                                }
                            }
                        }
                        loadUrl(redirectUrl)
                    }
                }
            )
        }
    }
}

private fun handlePostback(message: String, onTokenCaptured: (PaymentTokenData, String, Map<String, Any?>) -> Unit, onFailure: (String) -> Unit) {
    try {
        if (message.isBlank() || message.contains("MessageType") || message.contains("CardinalJWT")) return
        val json = extractJsonObject(message) ?: return
        val obj = org.json.JSONObject(json)
        val data = if (obj.has("data") && obj.get("data") is org.json.JSONObject) obj.getJSONObject("data") else obj

        val status = obj.optString("status", data.optString("status", "")).lowercase().trim()
        val code = obj.optString("code", data.optString("code", "")).uppercase().trim()
        val orderStatus = data.optString("order_status", obj.optString("order_status", "")).uppercase().trim()
        val redirectUrl = data.optString("redirect_url", obj.optString("redirect_url", "")).trim()

        // 1. Ignore in-flight / setup statuses
        if (status in listOf("wait", "challenge", "postback", "proceed", "pending", "await_3ds")) return

        // 2. Ignore non-terminal 3DS states
        val needs3ds = (orderStatus == "AWAIT_3DS" || orderStatus == "PENDING") || redirectUrl.isNotBlank()
        if (needs3ds) return

        // 3. Require explicit terminal order_status or code/status to complete
        val isTerminalSuccess = code == "CARD_VERIFIED" ||
                orderStatus in listOf("SUCCESS", "AUTHORIZED", "AUTHORISED", "CAPTURED", "COMPLETED") ||
                (status == "success" && orderStatus.isBlank() && code == "00" && message.contains("postback message"))

        val isTerminalFailure = status in listOf("failed", "failure", "error") ||
                orderStatus in listOf("FAILED", "DECLINED") ||
                code in listOf("PAYMENT_FAILED", "FAILED")

        if (isTerminalSuccess) {
            val tokenData = PaymentTokenData(
                paymentToken = data.optString("payment_token", data.optString("m_payment_token", "")),
                transactionId = data.optString("transaction_id"),
                orderId = data.optString("order_id", data.optString("p_order_id", "")),
                scheme = data.optString("scheme"),
                maskCardNumber = data.optString("mask_card_number")
            )
            onTokenCaptured(tokenData, code.ifBlank { "00" }, jsonObjectToMap(obj))
        } else if (isTerminalFailure) {
            val errorMsg = data.optString("reason")
                .ifBlank { obj.optString("message", obj.optString("error_message", "Transaction failed")) }
            onFailure(errorMsg)
        }
    } catch (e: Exception) {
        // Silently ignore non-terminal script logs
    }
}

private fun jsonObjectToMap(obj: org.json.JSONObject): Map<String, Any?> {
    return obj.keys().asSequence().associateWith { key ->
        when (val value = obj.get(key)) {
            is org.json.JSONArray -> jsonArrayToList(value)
            is org.json.JSONObject -> jsonObjectToMap(value)
            org.json.JSONObject.NULL -> null
            else -> value
        }
    }
}

private fun jsonArrayToList(array: org.json.JSONArray): List<Any?> {
    return (0 until array.length()).map { index ->
        when (val value = array.get(index)) {
            is org.json.JSONArray -> jsonArrayToList(value)
            is org.json.JSONObject -> jsonObjectToMap(value)
            org.json.JSONObject.NULL -> null
            else -> value
        }
    }
}

private fun extractJsonObject(input: String): String? {
    val start = input.indexOf('{')
    val end = input.lastIndexOf('}')
    return if (start >= 0 && end > start) input.substring(start, end + 1) else null
}
