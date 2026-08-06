package com.payorc.sdk.presentation.checkout.components

import com.payorc.sdk.presentation.checkout.components.PayOrcText

import android.R.attr.delay
import android.provider.CalendarContract
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payorc.sdk.ui.components.PayorcSdkUiConstants
import com.payorc.sdk.domain.model.PaymentTokenData
import com.payorc.sdk.ui.theme.AppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.time.delay

@Composable
fun TabbyProcessingScreen(message: String) {

    // Parse progress from message if it exists (e.g., "Initializing Payment... 45%")
    val parsedProgress = remember(message) {
        val match = Regex("(\\d+)%").find(message)
        match?.groupValues?.get(1)?.toFloatOrNull()?.div(100f)
    }

    var internalProgress by remember { mutableFloatStateOf(0f) }

    // Use parsed progress if available, otherwise drift slowly
    val displayProgress = parsedProgress ?: internalProgress

    LaunchedEffect(parsedProgress) {
        if (parsedProgress == null) {
            // Drifting while waiting for the first progress or between steps
            while (internalProgress < 0.95f) {
                delay(100)
                internalProgress += 0.002f
            }
        } else {
            // Animate internal progress to catch up with parsed progress
            val target = parsedProgress
            if (target > internalProgress) {
                val step = (target - internalProgress) / 10f
                repeat(10) {
                    delay(20)
                    internalProgress += step
                }
            }
            internalProgress = target
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LinearProgressIndicator(
            strokeCap = StrokeCap.Butt,
            progress = { displayProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(35.dp),
            color = Color(0xFF39FFBD),
            trackColor = Color.LightGray
        )
    }
}

@Composable
fun ProcessingScreen(
    tokenData: PaymentTokenData?,
    message: String? = null
) {
    Column(
        modifier = Modifier            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(32.dp))
        
        // Circular progress with card icon
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = PayorcSdkUiConstants.loadingIndicatorColor,
                strokeWidth = 4.dp
            )
            Icon(
                painter = painterResource(id = com.payorc.sdk.R.drawable.ic_payment), // generic card
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        PayOrcText(
            text = "Submitting your order",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        PayOrcText(
            text = "Please don't leave this page before the end of the order submission.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Linear Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PayOrcText(
                text = message ?: "Processing payment...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.width(16.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF424242),
                trackColor = Color.LightGray
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Card info summary
        if (tokenData != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = com.payorc.sdk.R.drawable.ic_payment),
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        PayOrcText(
                            text = tokenData.maskCardNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        PayOrcText(
                            text = tokenData.scheme.replaceFirstChar { it.uppercase() },
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Secure",
                            tint = Color(0xFF0277BD),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        PayOrcText(
                            text = "Secure",
                            color = Color(0xFF0277BD),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Warning box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFF8E1)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFBC02D),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                PayOrcText(
                    text = "Do not close or refresh this page during payment processing.",
                    color = Color(0xFF424242),
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun FailedHeader(errorMessage: String, tokenData: PaymentTokenData?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Red circle X
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFFFEBEE), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Failed",
                tint = Color(0xFFD32F2F),
                modifier = Modifier
                    .size(32.dp)
                    .border(2.dp, Color(0xFFD32F2F), CircleShape)
                    .padding(4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        PayOrcText(
            text = "Payment Failed",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PayorcSdkUiConstants.textSecondaryColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        PayOrcText(
            text = "Your transaction could not be completed. Please try again or use a different payment method.",
            style = MaterialTheme.typography.bodyMedium,
            color = PayorcSdkUiConstants.textPrimaryColor,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Red tinted card summary box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF5F5), // Light red background
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (tokenData != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            modifier = Modifier.size(40.dp, 28.dp),
                            shadowElevation = 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = com.payorc.sdk.R.drawable.ic_payment),
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            PayOrcText(
                                text = tokenData.maskCardNumber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            PayOrcText(
                                text = tokenData.scheme.replaceFirstChar { it.uppercase() },
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFEBEE)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                PayOrcText(
                                    text = "Declined",
                                    color = Color(0xFFD32F2F),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Error message
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFF57C00),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PayOrcText(
                        text = errorMessage,
                        color = Color(0xFF424242),
                        fontSize = 13.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CardAddedScreen(
    tokenData: PaymentTokenData,
    initialCvv: String,
    buttonColor: Color = PayorcSdkUiConstants.buttonBackgroundColor,
    onMakePayment: (String) -> Unit,
    onClose: () -> Unit
) {
    var cvv by remember { mutableStateOf(initialCvv) }
    val needsCvv = initialCvv.isBlank() // 3DS path — CVV was not captured from form
    val brandColor = PayorcSdkUiConstants.brandColor
    val onBrandColor = PayorcSdkUiConstants.onBrandBackgroundPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Green success circle
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color(0xFFE8F5E9), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Card Added",
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        PayOrcText(
            text = "Card Added Successfully!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = onBrandColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        PayOrcText(
            text = "Your card has been verified. Tap the button below to complete the payment.",
            style = MaterialTheme.typography.bodyMedium,
            color = PayorcSdkUiConstants.onBrandBackgroundSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card summary tile
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF5F5F5),
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    modifier = Modifier.size(40.dp, 28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(id = com.payorc.sdk.R.drawable.ic_payment),
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    PayOrcText(
                        text = tokenData.maskCardNumber.ifBlank { "•••• •••• •••• ••••" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                    if (tokenData.scheme.isNotBlank()) {
                        PayOrcText(
                            text = tokenData.scheme.replaceFirstChar { it.uppercase() },
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure",
                        tint = Color(0xFF0277BD),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    PayOrcText(
                        text = "Secure",
                        color = Color(0xFF0277BD),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CVV input — always shown so user can confirm/re-enter
        Column(modifier = Modifier.fillMaxWidth()) {
            PayOrcTextField(
                value = cvv,
                onValueChange = { if (it.length <= 4) cvv = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { PayOrcText("CVV", style = MaterialTheme.typography.labelSmall, color = Color.Gray) },
                placeholder = { PayOrcText("•••", color = Color.LightGray) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure",
                        tint = Color(0xFF0277BD),
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
            if (needsCvv) {
                PayOrcText(
                    text = "Please enter your CVV to complete the payment",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Make Payment button
        Button(
            onClick = { onMakePayment(cvv) },
            enabled = cvv.length >= 3,
            modifier = Modifier
                .fillMaxWidth()
                .height(PayorcSdkUiConstants.buttonHeight),
            shape = RoundedCornerShape(PayorcSdkUiConstants.buttonBorderRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = PayorcSdkUiConstants.buttonForegroundColor,
                disabledContainerColor = PayorcSdkUiConstants.buttonDisabledBackgroundColor,
                disabledContentColor = PayorcSdkUiConstants.buttonDisabledForegroundColor
            ),
            border = if (PayorcSdkUiConstants.buttonSideBorderColor != Color.Transparent) 
                BorderStroke(1.dp, PayorcSdkUiConstants.buttonSideBorderColor) else null
        ) {
            PayOrcText(
                text = PayorcSdkUiConstants.addCardFormConfig?.submitButtonTitleVerify ?: "Make Payment",
                fontWeight = FontWeight.Bold,
                color = PayorcSdkUiConstants.buttonForegroundColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            PayOrcText(
                text = "Cancel",
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun UseNewCardBottomSheet(
    cardHolderName: String,
    onCardHolderNameChange: (String) -> Unit,
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    expiryMonth: String,
    onExpiryMonthChange: (String) -> Unit,
    expiryYear: String,
    onExpiryYearChange: (String) -> Unit,
    cvv: String,
    onCvvChange: (String) -> Unit,
    cardHolderError: String? = null,
    cardNumberError: String? = null,
    expiryMonthError: String? = null,
    expiryYearError: String? = null,
    cvvError: String? = null,
    onCardHolderFocused: () -> Unit = {},
    onCardNumberFocused: () -> Unit = {},
    onExpiryMonthFocused: () -> Unit = {},
    onExpiryYearFocused: () -> Unit = {},
    onCvvFocused: () -> Unit = {},
    isLoading: Boolean = false,
    buttonColor: Color = PayorcSdkUiConstants.buttonBackgroundColor,
    showCardholderField: Boolean = true,
    showCardNumberField: Boolean = true,
    showExpiryFields: Boolean = true,
    showCvvField: Boolean = true,
    payorcLogoUrl: String? = null,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    isButtonEnabled: Boolean = false,
    globalError: String? = null,
) {
    // Detect card scheme from card number
    val detectedCardScheme = remember(cardNumber) {
        detectCardScheme(cardNumber)
    }
    val brandColor = PayorcSdkUiConstants.brandColor
    val onBrandColor = PayorcSdkUiConstants.onBrandBackgroundPrimary

    val paddingConfig = PayorcSdkUiConstants.bottomSheetConfig?.padding
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = paddingConfig?.left ?: 20.dp,
                top = paddingConfig?.top ?: 0.dp,
                end = paddingConfig?.right ?: 20.dp,
                bottom = paddingConfig?.bottom ?: 40.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading && cardHolderName.isBlank() && cardNumber.isBlank() && cvv.isBlank()) {
            // Initial loading state for the sheet itself - Show ONLY the loader
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PayorcSdkUiConstants.loadingIndicatorColor)
            }
        } else {
            // Header Section with Dropdown Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PayOrcText(
                    text = PayorcSdkUiConstants.addCardFormConfig?.titleUseNewCard ?: "Add a new card",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
//                    color = onBrandColor,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F7F9))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close",
                        modifier = Modifier.size(15.dp),
                        tint = Color(0xFF5F6060)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            PayOrcText(
                text = "Enter your card details securely",
                textAlign = TextAlign.Start,
                modifier = Modifier.align(Alignment.Start)
            )
            
            if (!globalError.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                ErrorBanner(
                    message = globalError,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cardholder Name Field - shown only if not provided by app
            if (showCardholderField) {
                PayOrcTextField(
                    value = cardHolderName,
                    onValueChange = onCardHolderNameChange,
                    label = { PayOrcText("Card Holder Name") },
                    placeholder = { PayOrcText("Enter name on card") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onCardHolderFocused() },
                    singleLine = true,
                    isError = cardHolderError != null
                )
                FieldErrorText(cardHolderError)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Card Number Field - shown only if not provided by app
            if (showCardNumberField) {
                CardNumberField(
                    value = cardNumber,
                    onValueChange = onCardNumberChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onCardNumberFocused() },
                    isError = cardNumberError != null,
                    errorMessage = null,
                    trailingIcon = detectedCardScheme?.let { scheme ->
                        { CardSchemeBadge(scheme = scheme, modifier = Modifier.size(32.dp, 24.dp)) }
                    }
                )
                FieldErrorText(cardNumberError)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Expiry (MM/YY) and CVV Row - shown only if not provided by app
            if (showExpiryFields || showCvvField) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (showExpiryFields) {
                        MonthField(
                            value = expiryMonth,
                            onValueChange = onExpiryMonthChange,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { if (it.isFocused) onExpiryMonthFocused() },
                            isError = expiryMonthError != null,
                            errorMessage = null
                        )
                    }
                    
                    if (showExpiryFields) {
                        YearField(
                            value = expiryYear,
                            onValueChange = onExpiryYearChange,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { if (it.isFocused) onExpiryYearFocused() },
                            isError = expiryYearError != null,
                            errorMessage = null
                        )
                    }
                    
                    if (showCvvField) {
                        CVVField(
                            value = cvv,
                            onValueChange = onCvvChange,
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { if (it.isFocused) onCvvFocused() },
                            isError = cvvError != null,
                            errorMessage = null
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (showExpiryFields) {
                        FieldErrorText(expiryMonthError, modifier = Modifier.weight(1f))
                        FieldErrorText(expiryYearError, modifier = Modifier.weight(1f))
                    }
                    if (showCvvField) {
                        FieldErrorText(cvvError, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Disclaimer Text
            PayOrcText(
                text = "By providing your card information, you allow us to charge your card for future payments in accordance with their terms.",
                style = MaterialTheme.typography.labelMedium,
//                color = PayorcSdkUiConstants.onBrandBackgroundSecondary,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Start,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Confirm Button
            if (isLoading) {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PayorcSdkUiConstants.buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor.copy(alpha = 0.6f),
                        disabledContainerColor = buttonColor.copy(alpha = 0.6f),
                        contentColor = PayorcSdkUiConstants.buttonForegroundColor
                    ),
                    shape = RoundedCornerShape(PayorcSdkUiConstants.buttonBorderRadius)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PayorcSdkUiConstants.loadingIndicatorColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    PayOrcText(text = "Processing...", fontWeight = PayorcSdkUiConstants.buttonFontWeight, color = PayorcSdkUiConstants.buttonForegroundColor)
                }
            } else {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PayorcSdkUiConstants.buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PayorcSdkUiConstants.buttonBackgroundColor,
                        contentColor = PayorcSdkUiConstants.buttonForegroundColor,
                        disabledContainerColor = PayorcSdkUiConstants.buttonDisabledBackgroundColor,
                        disabledContentColor = PayorcSdkUiConstants.buttonDisabledForegroundColor
                    ),
                    shape = RoundedCornerShape(PayorcSdkUiConstants.buttonBorderRadius),
                    border = if (PayorcSdkUiConstants.buttonSideBorderColor != Color.Transparent) 
                        BorderStroke(1.dp, PayorcSdkUiConstants.buttonSideBorderColor) else null
                ) {
                    PayOrcText(text = PayorcSdkUiConstants.addCardFormConfig?.submitButtonTitleVerify ?: "Confirm", fontWeight = PayorcSdkUiConstants.buttonFontWeight, fontSize = 16.sp, color = PayorcSdkUiConstants.buttonForegroundColor)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Powered By PayOrc
            PoweredByPayOrc(logoUrl = payorcLogoUrl)
        }
    }
}

@Composable
private fun FieldErrorText(
    message: String?,
    modifier: Modifier = Modifier
) {
    if (message.isNullOrBlank()) return

    PayOrcText(
        text = message,
        color = Color.Red,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}
