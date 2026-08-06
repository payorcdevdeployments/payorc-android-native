# PayOrc Android SDK Integration Guide

Android SDK for PayOrc checkout: payment method sheet, Google Pay, Tabby, pay with card, add-card/card-entry flow, and related SDK UI. Merchant keys and the PayOrc service SDK drive checkout customization such as colors, available payment methods, wallet configuration, and merchant branding.

This guide intentionally documents only the merchant-facing integration surface. PayOrc handles the checkout flow details inside the SDK.

---

## Requirements

- Android application with `minSdk` 24 or higher.
- A PayOrc merchant key and merchant secret.
- PayOrc Maven repository access.
- Google Pay merchant configuration if Google Pay is enabled for your account.
- Tabby merchant configuration if Tabby is enabled for your account.

---

## Installation

Add the Maven repository in the root project settings file:

- Kotlin DSL project: `<project-root>/settings.gradle.kts`
- Groovy project: `<project-root>/settings.gradle`

This file is located at the root of your Android project, next to files such as `build.gradle`, `gradle.properties`, and the `app/` module folder.

### Kotlin DSL

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            name = "PayOrc"
            url = uri("https://maven.pkg.github.com/payorcgitsrvc/sdk-native-android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                    ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

### Groovy

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        maven {
            name = "PayOrc"
            url = uri("https://maven.pkg.github.com/payorcgitsrvc/sdk-native-android")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Add the SDK dependency in your app module Gradle file:

- Kotlin DSL project: `<project-root>/app/build.gradle.kts`
- Groovy project: `<project-root>/app/build.gradle`

Use the dependency block inside the Android application module, not the root project Gradle file.

### Kotlin DSL

```kotlin
dependencies {
    implementation("com.payorc:native-sdk:1.0.0")
}
```

### Groovy

```groovy
dependencies {
    implementation 'com.payorc:native-sdk:1.0.0'
}
```

Import the SDK:

Use these imports in the Kotlin file where you initialize or launch the SDK, for example:

- `Application` class, such as `<project-root>/app/src/main/java/com/example/app/MerchantApplication.kt`
- Checkout `Activity`, such as `<project-root>/app/src/main/java/com/example/app/CheckoutActivity.kt`
- Compose checkout screen file

```kotlin
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.PayOrcEnvironment
import com.payorc.sdk.PayorcLanguage
import com.payorc.sdk.presentation.checkout.PayOrcCheckout
```

---

## Permissions

The SDK AAR declares the Internet permission:

Normally this permission is merged automatically from the SDK. If your team keeps all permissions explicit in the host app, add it in:

`<project-root>/app/src/main/AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

This is required for checkout customization, payment-method loading, payment processing, wallet flows, Tabby flows, and SDK-hosted web content.

The SDK also contributes its checkout activities through manifest merge. No additional activity declaration is normally required in the merchant app.

---

## 1. `PayOrcSdk.init` - Configure the SDK

Call once at startup before using `PayOrcSdk.instance` or presenting checkout.

Recommended location:

`<project-root>/app/src/main/java/<your-package>/MerchantApplication.kt`

```kotlin
PayOrcSdk.init(
    context = applicationContext,
    merchantKey = "YOUR_MERCHANT_KEY",
    merchantSecret = "YOUR_MERCHANT_SECRET",
    environment = PayOrcEnvironment.SANDBOX,
    language = PayorcLanguage.english
)
```

Full `Application` example:

```kotlin
class MerchantApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        PayOrcSdk.init(
            context = this,
            merchantKey = BuildConfig.PAYORC_MERCHANT_KEY,
            merchantSecret = BuildConfig.PAYORC_MERCHANT_SECRET,
            environment = if (BuildConfig.DEBUG) {
                PayOrcEnvironment.SANDBOX
            } else {
                PayOrcEnvironment.PRODUCTION
            },
            language = PayorcLanguage.english
        )
    }
}
```

### Parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `context` | `Context` | yes | Android context used to configure the SDK. |
| `merchantKey` | `String` | yes | PayOrc merchant key. |
| `merchantSecret` | `String` | yes | PayOrc merchant secret. |
| `environment` | `PayOrcEnvironment` | yes | `SANDBOX` or `PRODUCTION` API/gateway behavior. |
| `language` | `PayorcLanguage` | no | SDK language: `english` or `arabic`. Default is `english`. |
| `appId` | `String?` | no | Optional application identifier sent with SDK requests. |
| `appVersion` | `String?` | no | Optional application version sent with SDK requests. |
| `fetchCheckoutCustomizationOnInit` | `Boolean` | no | Whether checkout customization should be fetched during initialization. Default is `true`. |
| `checkoutCustomizationCurrency` | `String` | no | Currency used for the initial customization lookup. Default is `AED`. |
| `checkoutCustomizationAmount` | `Double` | no | Amount used for the initial customization lookup. Default is `1.0`. |

### Return Value

`PayOrcSdk.init` returns the configured `PayOrcSdk` singleton. After initialization, use:

```kotlin
val sdk = PayOrcSdk.instance
```

You can guard access with:

```kotlin
if (PayOrcSdk.isInitialized) {
    // SDK is ready.
}
```

---

## 2. `PayOrcSdk.customization` - Host UI Overrides

`PayOrcSdk.customization(...)` is an optional static call used to override the SDK checkout UI from the host Android app. Use it when the merchant app needs PayOrc checkout to match its brand colors, button style, field style, text colors, labels, or validation messages.

Call it after `PayOrcSdk.init(...)` and before checkout is opened. Omitted parameters keep previous values, PayOrc dashboard/API-driven values, or SDK defaults.

Place this code before checkout is opened, for example:

- immediately after `PayOrcSdk.init(...)` in your `Application` class if customization is global;
- in your checkout `Activity.onCreate(...)` before calling `PayOrcCheckout.start(...)`;
- in your checkout ViewModel/controller before presenting the SDK UI.

Required imports for most customization examples:

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payorc.sdk.PayOrcSdk
import com.payorc.sdk.PayorcInputBorderStyle
import com.payorc.sdk.domain.model.CardFormError
import com.payorc.sdk.domain.model.PayorcAppBarStyle
import com.payorc.sdk.domain.model.PayorcEdgeInsets
import com.payorc.sdk.domain.model.PayorcGuidanceStyle
import com.payorc.sdk.domain.model.PayorcSdkAddCardFormCustomization
import com.payorc.sdk.domain.model.PayorcSdkAppTextFieldCustomization
import com.payorc.sdk.domain.model.PayorcSdkBottomSheetCustomization
import com.payorc.sdk.domain.model.PayorcSdkButtonCustomization
import com.payorc.sdk.domain.model.PayorcSdkCardFormValidationCustomization
import com.payorc.sdk.domain.model.PayorcSdkTextCustomization
```

### Basic Example

This matches the Flutter SDK README style: border style, app bar style, brand color, button color, accent color, text colors, and border color.

```kotlin
PayOrcSdk.customization(
    inputBorderStyle = PayorcInputBorderStyle.outline,
    appBarStyle = PayorcAppBarStyle.nativeIos,
    brandColor = Color(0xFF000000),
    button = PayorcSdkButtonCustomization(
        backgroundColor = Color(0xFF000000)
    ),
    accentColor = Color(0xFF000000),
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF000000),
    borderColor = Color(0xFF000000)
)
```

In the Android SDK, Flutter's `buttonColor` equivalent is:

```kotlin
button = PayorcSdkButtonCustomization(
    backgroundColor = Color(0xFF000000)
)
```

### Resolution Order

The SDK resolves visual values in this order:

| Priority | Source | Description |
| --- | --- | --- |
| 1 | Merchant app override | Values passed to `PayOrcSdk.customization(...)`. |
| 2 | PayOrc checkout customization | Branding and payment-method configuration fetched from PayOrc. |
| 3 | SDK default | Built-in fallback values. |

This means merchant app values passed through `PayOrcSdk.customization(...)` take priority over dashboard/API values for the fields they override.

### Main Parameters

| Parameter | Type | Description |
| --- | --- | --- |
| `inputBorderStyle` | `PayorcInputBorderStyle?` | `outline` or `underline` for SDK text fields. |
| `guidanceStyle` | `PayorcGuidanceStyle?` | Controls whether field guidance appears as hint text or label text. |
| `appBarStyle` | `PayorcAppBarStyle?` | SDK app bar style. |
| `brandColor` | `Color?` | Sheet/header brand fill. Fully transparent colors are ignored. |
| `button` | `PayorcSdkButtonCustomization?` | Primary CTA button styling. Use `backgroundColor` for Flutter-style `buttonColor`. |
| `accentColor` | `Color?` | Secondary accent color for highlights, loaders, and selected states where used. |
| `text` | `PayorcSdkTextCustomization?` | Typography and text styling options. |
| `textPrimary` | `Color?` | Direct primary text override. |
| `textSecondary` | `Color?` | Direct secondary text override. |
| `cardFormValidation` | `PayorcSdkCardFormValidationCustomization?` | Card form validation message overrides. |
| `borderColor` | `Color?` | Default border/stroke color for SDK chrome. |
| `addCardForm` | `PayorcSdkAddCardFormCustomization?` | Card form title, button title, labels, and hints. |
| `appTextField` | `PayorcSdkAppTextFieldCustomization?` | Text field height, corner radius, border color, and padding. |
| `bottomSheet` | `PayorcSdkBottomSheetCustomization?` | Bottom sheet padding, spacing, and corner radius. |

### Color Parameters

| Parameter | Description | Example |
| --- | --- | --- |
| `brandColor` | Main brand surface color used by SDK checkout surfaces/header areas. | `Color(0xFF003B5C)` |
| `accentColor` | Supporting accent color used for highlights and loading indicator fallback. | `Color(0xFF00A3A3)` |
| `textPrimary` | Shortcut for overriding primary SDK text color. | `Color(0xFF101828)` |
| `textSecondary` | Shortcut for overriding secondary SDK text color. | `Color(0xFF667085)` |
| `borderColor` | Default SDK border/stroke color. | `Color(0xFFE4E7EC)` |
| `button.backgroundColor` | Main CTA button color. | `Color(0xFF101828)` |

Fully transparent `brandColor`, `accentColor`, `textPrimary`, `textSecondary`, and `borderColor` values are ignored by the SDK so existing/API/default values can continue to apply.

### Field and App Bar Styles

#### `PayorcInputBorderStyle`

| Value | Description |
| --- | --- |
| `PayorcInputBorderStyle.outline` | SDK text fields use an outline border. |
| `PayorcInputBorderStyle.underline` | SDK text fields use underline-only styling. |

#### `PayorcGuidanceStyle`

| Value | Description |
| --- | --- |
| `PayorcGuidanceStyle.hint` | Field guidance appears as hint/placeholder text. |
| `PayorcGuidanceStyle.label` | Field guidance appears as label text. |

#### `PayorcAppBarStyle`

| Value | Description |
| --- | --- |
| `PayorcAppBarStyle.standard` | Standard SDK app bar style. |
| `PayorcAppBarStyle.nativeIos` | iOS-like app bar presentation. |
| `PayorcAppBarStyle.nativeAndroid` | Android-native app bar presentation. |
| `PayorcAppBarStyle.nativeAndroid_Standard` | Android standard variant. |

### Button Customization

Use `PayorcSdkButtonCustomization` to style the main SDK action buttons.

```kotlin
PayOrcSdk.customization(
    button = PayorcSdkButtonCustomization(
        backgroundColor = Color(0xFF101828),
        foregroundColor = Color.White,
        disabledBackgroundColor = Color(0xFFD0D5DD),
        sideBorderColor = Color.Transparent,
        borderRadius = 10.dp,
        height = 52.dp,
        fontWeight = FontWeight.SemiBold,
        loadingIndicatorColor = Color(0xFF12B76A)
    )
)
```

| Field | Type | Description | Default Behavior |
| --- | --- | --- | --- |
| `backgroundColor` | `Color?` | Primary CTA button background. | PayOrc API button color or SDK default. |
| `foregroundColor` | `Color?` | Button text/icon color. | `Color.White`. |
| `disabledBackgroundColor` | `Color?` | Disabled button background color. | `Color.Gray`. |
| `sideBorderColor` | `Color?` | Button side/border color. | `Color.Transparent`. |
| `borderRadius` | `Dp?` | Button corner radius. | `12.dp`. |
| `height` | `Dp?` | Button height. | `56.dp`. |
| `fontWeight` | `FontWeight?` | Button text weight. | `FontWeight.Bold`. |
| `loadingIndicatorColor` | `Color?` | Loader color used on button/loading states. | `accentColor`. |

### Text and Typography Customization

Use `PayorcSdkTextCustomization` to configure SDK text color and typography.

```kotlin
PayOrcSdk.customization(
    text = PayorcSdkTextCustomization(
        primary = Color(0xFF101828),
        secondary = Color(0xFF667085),
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp,
        wordSpacing = 0.5.sp,
        textAlign = TextAlign.Start,
        fontStyle = "normal",
        maxLines = 2,
        overflow = "ellipsis",
        decoration = "none",
        softWrap = true
    )
)
```

| Field | Type | Description |
| --- | --- | --- |
| `primary` | `Color?` | Primary SDK text color. |
| `secondary` | `Color?` | Secondary SDK text color. |
| `bodyFontFamily` | `String?` | Body font family name when supported by SDK text rendering/API configuration. |
| `titleFontFamily` | `String?` | Title font family name when supported by SDK text rendering/API configuration. |
| `fontWeight` | `FontWeight?` | Text weight, such as `Normal`, `Medium`, `SemiBold`, or `Bold`. |
| `fontSize` | `TextUnit?` | Text size, for example `14.sp`. |
| `letterSpacing` | `TextUnit?` | Spacing between letters, for example `0.2.sp`. |
| `wordSpacing` | `TextUnit?` | Additional spacing around word gaps, for example `0.5.sp`. |
| `textAlign` | `TextAlign?` | Text alignment, such as `TextAlign.Start`, `Center`, or `End`. |
| `fontStyle` | `String?` | Text style string. Supported values are `"normal"` and `"italic"`. |
| `maxLines` | `Int?` | Maximum text lines for SDK text components where applied. |
| `overflow` | `String?` | Overflow behavior. Supported values are `"clip"`, `"ellipsis"`, and `"visible"`. |
| `decoration` | `String?` | Text decoration. Supported values are `"none"`, `"underline"`, and `"linethrough"`. |
| `softWrap` | `Boolean?` | Whether text can wrap to the next line. |

Use typography overrides carefully. Very large font sizes, tight `maxLines`, or aggressive spacing can cause clipping on smaller Android screens.

### Add Card Form Text

Use `PayorcSdkAddCardFormCustomization` to customize the visible text in the card form.

```kotlin
PayOrcSdk.customization(
    addCardForm = PayorcSdkAddCardFormCustomization(
        titleUseNewCard = "Use a new card",
        submitButtonTitleVerify = "Pay securely",
        cardNumberLabel = "Card number",
        cardNumberHint = "1234 5678 9012 3456",
        expiryMonthLabel = "Month",
        expiryMonthHint = "MM",
        expiryYearLabel = "Year",
        expiryYearHint = "YY",
        cvvLabel = "CVV",
        cvvHint = "123"
    )
)
```

| Field | Description |
| --- | --- |
| `titleUseNewCard` | Title shown for the new-card section. |
| `submitButtonTitleVerify` | Main form submit/verify button text. |
| `cardNumberHint` | Placeholder/hint for card number. |
| `cardNumberLabel` | Label for card number. |
| `expiryMonthHint` | Placeholder/hint for expiry month. |
| `expiryMonthLabel` | Label for expiry month. |
| `expiryYearHint` | Placeholder/hint for expiry year. |
| `expiryYearLabel` | Label for expiry year. |
| `cvvHint` | Placeholder/hint for CVV. |
| `cvvLabel` | Label for CVV. |

### Validation Message Customization

Use `PayorcSdkCardFormValidationCustomization` when you want merchant-specific validation messages for card fields.

```kotlin
PayOrcSdk.customization(
    cardFormValidation = PayorcSdkCardFormValidationCustomization(
        cardNumberError = CardFormError(
            required = "Please enter your card number",
            invalid = "Enter a valid card number"
        ),
        cardHolderNameError = CardFormError(
            required = "Please enter the cardholder name",
            invalid = "Enter a valid cardholder name"
        ),
        expiryMonthError = CardFormError(
            required = "Expiry month is required",
            invalid = "Enter a valid expiry month"
        ),
        expiryYearError = CardFormError(
            required = "Expiry year is required",
            invalid = "Enter a valid expiry year"
        ),
        cvvError = CardFormError(
            required = "CVV is required",
            invalid = "Enter a valid CVV"
        )
    )
)
```

| Field | Description |
| --- | --- |
| `cardHolderNameError` | Required/invalid messages for cardholder name. |
| `cardNumberError` | Required/invalid messages for card number. |
| `expiryMonthError` | Required/invalid messages for expiry month. |
| `expiryYearError` | Required/invalid messages for expiry year. |
| `cvvError` | Required/invalid messages for CVV. |
| `invalidCardError` | Required/invalid messages for general invalid-card states. |

`CardFormError` contains:

| Field | Description |
| --- | --- |
| `required` | Message shown when the field is empty. |
| `invalid` | Message shown when the field value is invalid. |

### Text Field Chrome

Use `PayorcSdkAppTextFieldCustomization` to adjust field height, radius, border color, and padding.

```kotlin
PayOrcSdk.customization(
    appTextField = PayorcSdkAppTextFieldCustomization(
        height = 64.dp,
        borderRadius = 12.dp,
        borderColor = Color(0xFFE4E7EC),
        padding = PayorcEdgeInsets.symmetric(horizontal = 12.dp, vertical = 8.dp)
    )
)
```

| Field | Type | Description |
| --- | --- | --- |
| `borderRadius` | `Dp?` | Text field corner radius. |
| `height` | `Dp?` | Text field height. |
| `borderColor` | `Color?` | Text field border color. |
| `padding` | `PayorcEdgeInsets?` | Text field content padding. |

### Bottom Sheet Chrome

Use `PayorcSdkBottomSheetCustomization` to adjust checkout sheet spacing and rounded corners.

```kotlin
PayOrcSdk.customization(
    bottomSheet = PayorcSdkBottomSheetCustomization(
        padding = PayorcEdgeInsets.all(16.dp),
        spacing = 16.dp,
        cornerRadius = 20.dp
    )
)
```

| Field | Type | Description |
| --- | --- | --- |
| `padding` | `PayorcEdgeInsets?` | Bottom sheet content padding. |
| `spacing` | `Dp?` | Spacing between bottom sheet elements. |
| `cornerRadius` | `Dp?` | Bottom sheet corner radius. |

### `PayorcEdgeInsets`

`PayorcEdgeInsets` is used for padding values.

```kotlin
PayorcEdgeInsets.all(16.dp)
PayorcEdgeInsets.symmetric(horizontal = 12.dp, vertical = 8.dp)
PayorcEdgeInsets.only(left = 12.dp, top = 8.dp, right = 12.dp, bottom = 8.dp)
```

| Field | Type | Description |
| --- | --- | --- |
| `left` | `Dp` | Left inset. |
| `top` | `Dp` | Top inset. |
| `right` | `Dp` | Right inset. |
| `bottom` | `Dp` | Bottom inset. |

### Complete Branding Example

Use this when you want a consistent merchant-branded checkout UI.

```kotlin
PayOrcSdk.customization(
    inputBorderStyle = PayorcInputBorderStyle.outline,
    guidanceStyle = PayorcGuidanceStyle.label,
    appBarStyle = PayorcAppBarStyle.nativeAndroid,
    brandColor = Color(0xFF003B5C),
    accentColor = Color(0xFF00A3A3),
    textPrimary = Color(0xFF102A43),
    textSecondary = Color(0xFF627D98),
    borderColor = Color(0xFFD9E2EC),
    button = PayorcSdkButtonCustomization(
        backgroundColor = Color(0xFF003B5C),
        foregroundColor = Color.White,
        disabledBackgroundColor = Color(0xFFCBD5E1),
        borderRadius = 8.dp,
        height = 52.dp,
        fontWeight = FontWeight.SemiBold,
        loadingIndicatorColor = Color(0xFF00A3A3)
    ),
    text = PayorcSdkTextCustomization(
        primary = Color(0xFF102A43),
        secondary = Color(0xFF627D98),
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        overflow = "ellipsis"
    ),
    appTextField = PayorcSdkAppTextFieldCustomization(
        height = 64.dp,
        borderRadius = 12.dp,
        borderColor = Color(0xFFD9E2EC),
        padding = PayorcEdgeInsets.symmetric(horizontal = 12.dp, vertical = 8.dp)
    ),
    bottomSheet = PayorcSdkBottomSheetCustomization(
        padding = PayorcEdgeInsets.all(16.dp),
        spacing = 16.dp,
        cornerRadius = 20.dp
    )
)
```

### Notes

- Merchant overrides take priority over checkout customization fetched from PayOrc.
- Call customization before launching checkout.
- Use colors with sufficient contrast for accessibility.
- Keep button height at least `48.dp` for accessible touch targets.
- Test long text, Arabic text, and smaller Android screens when changing typography or field sizes.

---

## 3. `PayOrcCheckout.start` - Payment Method Checkout

Presents the PayOrc checkout flow. The SDK loads the available payment methods from PayOrc configuration and shows supported options such as card, Google Pay, and Tabby depending on merchant setup.

Place this code where the customer taps your Pay/Checkout button, usually in:

- an Activity click listener;
- a Fragment button callback;
- a Compose button `onClick`;
- your checkout controller after validating the order.

```kotlin
PayOrcCheckout.start(
    context = this,
    request = request,
    callback = object : PayOrcCheckoutCallback {
        override fun onAddCardSuccess(tokenData: PaymentTokenData) {
            // User completed card/add-card flow.
            // Continue merchant order handling.
        }

        override fun onFailure(reason: String) {
            // Checkout or flow error.
        }
    }
)
```

### Parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `context` | `Context` | yes | Context used to launch the SDK checkout activity. |
| `request` | `PayOrcCheckoutRequest` | yes | Checkout payload containing order, customer, billing, shipping, and URL data. |
| `callback` | `PayOrcCheckoutCallback` | yes | Callback for checkout success/failure events. |
| `launchMethod` | `String?` | no | Optional direct method. Use `"CARD"` to open the card flow directly. |

### Direct Card Flow

Use this in the same location as normal checkout launch when you want to open card entry directly instead of the full payment-method selection flow.

```kotlin
PayOrcCheckout.start(
    context = this,
    request = request,
    callback = callback,
    launchMethod = "CARD"
)
```

### Compose Checkout Sheet

For Jetpack Compose screens:

Place this inside a composable checkout screen when you want to render the SDK checkout content as part of your Compose UI.

```kotlin
PayOrcCheckout.PayOrcCheckoutSheet(
    request = request,
    onClose = {
        // Close your containing UI.
    },
    onAddCardSuccess = { tokenData ->
        // User completed card/add-card flow.
    },
    onFailure = { reason ->
        // Show checkout error.
    }
)
```

---

## 4. Build a `PayOrcCheckoutRequest`

Build this request immediately before launching checkout, or keep it in your checkout ViewModel/state holder after your order has been created. The request should contain the final amount, currency, order ID, customer details, and address details for the current payment attempt.

```kotlin
val request = PayOrcCheckoutRequest(
    orderDetails = listOf(
        OrderDetails(
            mOrderId = "order_${System.currentTimeMillis()}",
            amount = "100.00",
            currency = "AED",
            description = "Order payment"
        )
    ),
    customerDetails = CustomerDetails(
        mCustomerId = "customer_123",
        name = "John Doe",
        email = "john.doe@example.com",
        mobile = "500000000",
        code = "+971"
    ),
    billingDetails = BillingDetails(
        addressLine1 = "Dubai Marina",
        addressLine2 = "Tower A",
        city = "Dubai",
        province = "Dubai",
        country = "AE",
        pin = "00000"
    ),
    shippingDetails = ShippingDetails(
        shippingName = "John Doe",
        shippingEmail = "john.doe@example.com",
        shippingCode = "+971",
        shippingMobile = "500000000",
        addressLine1 = "Dubai Marina",
        city = "Dubai",
        province = "Dubai",
        country = "AE",
        pin = "00000"
    ),
    urls = PayOrcUrls(
        success = "https://merchant.example.com/payment/success",
        cancel = "https://merchant.example.com/payment/cancel",
        failure = "https://merchant.example.com/payment/failure"
    )
)
```

### `PayOrcCheckoutRequest`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `paymentToken` | `String` | no | Existing payment token, when used by the merchant flow. |
| `orderDetails` | `List<OrderDetails>` | yes | Order amount, currency, merchant order ID, and description. |
| `customerDetails` | `CustomerDetails?` | recommended | Customer identity and contact details. |
| `billingDetails` | `BillingDetails?` | recommended | Billing address. |
| `shippingDetails` | `ShippingDetails?` | no | Shipping contact and address. |
| `urls` | `PayOrcUrls?` | no | Merchant success, cancel, and failure URLs. |

### `OrderDetails`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `mOrderId` | `String` | yes | Unique merchant order ID. |
| `amount` | `String` | yes | Amount as a decimal string, for example `"100.00"`. |
| `convenienceFee` | `String` | no | Convenience fee. Default is `"0"`. |
| `quantity` | `String` | no | Quantity. Default is `"1"`. |
| `currency` | `String` | yes | Currency code, for example `"AED"`. |
| `description` | `String` | no | Order description. |

### `CustomerDetails`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `mCustomerId` | `String` | yes | Merchant customer ID. |
| `name` | `String` | recommended | Customer name. |
| `email` | `String` | recommended | Customer email. |
| `mobile` | `String` | recommended | Customer mobile number without country code. |
| `code` | `String` | recommended | Mobile country code, for example `"+971"`. |

### `BillingDetails`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `addressLine1` | `String` | recommended | Address line 1. |
| `addressLine2` | `String` | no | Address line 2. |
| `city` | `String` | recommended | City. |
| `province` | `String` | no | State/province/emirate. |
| `country` | `String` | recommended | Country code. |
| `pin` | `String` | no | Postal/ZIP/PIN code. |

### `ShippingDetails`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `shippingName` | `String` | no | Recipient name. |
| `shippingEmail` | `String` | no | Recipient email. |
| `shippingCode` | `String` | no | Recipient country code. |
| `shippingMobile` | `String` | no | Recipient mobile number. |
| `addressLine1` | `String` | no | Shipping address line 1. |
| `addressLine2` | `String` | no | Shipping address line 2. |
| `city` | `String` | no | Shipping city. |
| `province` | `String` | no | Shipping state/province/emirate. |
| `country` | `String` | no | Shipping country code. |
| `pin` | `String` | no | Postal/ZIP/PIN code. |
| `locationPin` | `String` | no | Location pin/reference. |
| `shippingCurrency` | `String` | no | Shipping fee currency. |
| `shippingAmount` | `String` | no | Shipping fee amount. |

### `PayOrcUrls`

| Field | Type | Required | Description |
| --- | --- | --- | --- |
| `success` | `String` | no | Merchant success URL. |
| `cancel` | `String` | no | Merchant cancel URL. |
| `failure` | `String` | no | Merchant failure URL. |

---

## 5. Callback Handling

### `PayOrcCheckoutCallback`

| Method | Description |
| --- | --- |
| `onAddCardSuccess(tokenData: PaymentTokenData)` | Called when the user completes the card/add-card flow. |
| `onFailure(reason: String)` | Called when checkout cannot complete or a flow error occurs. |

### `PaymentTokenData`

| Field | Type | Description |
| --- | --- | --- |
| `paymentToken` | `String` | Token returned by the SDK flow. |
| `transactionId` | `String` | PayOrc transaction identifier. |
| `orderId` | `String` | Merchant/order identifier associated with the flow. |
| `scheme` | `String` | Card/payment scheme. |
| `maskCardNumber` | `String` | Masked card number for display. |

Example:

```kotlin
override fun onAddCardSuccess(tokenData: PaymentTokenData) {
    Log.d("PayOrc", "Order: ${tokenData.orderId}")
    Log.d("PayOrc", "Transaction: ${tokenData.transactionId}")
    Log.d("PayOrc", "Card: ${tokenData.maskCardNumber}")

    // Continue merchant-side order confirmation.
}

override fun onFailure(reason: String) {
    Log.e("PayOrc", "Checkout failed: $reason")
    // Show a user-friendly message and allow retry.
}
```

---

## 6. Embedded Payment Options

The Android SDK also provides Jetpack Compose components for merchants who want direct payment-option buttons in their own UI.

Use these components only inside Jetpack Compose screens, for example:

`<project-root>/app/src/main/java/<your-package>/CheckoutScreen.kt`

### Embedded Card Button

```kotlin
PayorcEmbeddedCardButton(
    paymentRequest = request,
    onPaymentSuccess = { tokenData ->
        // User completed card/add-card flow.
    },
    onPaymentError = { reason ->
        // Show card flow error.
    },
    modifier = Modifier.fillMaxWidth(),
    isEnabled = true
)
```

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `paymentRequest` | `PayOrcCheckoutRequest` | yes | Checkout payload. |
| `onPaymentSuccess` | `(PaymentTokenData) -> Unit` | yes | Called on card flow success. |
| `onPaymentError` | `(String) -> Unit` | yes | Called on card flow error. |
| `modifier` | `Modifier` | no | Compose modifier. |
| `buttonColor` | `Color` | no | Button background color. |
| `isEnabled` | `Boolean` | no | Enables/disables the button. |

### Embedded Tabby Button

```kotlin
PayorcEmbeddedTabbyButton(
    paymentRequest = request,
    onTabbyAuthorized = {
        // Tabby authorized.
    },
    onTabbyError = { error ->
        // Show Tabby error.
    },
    modifier = Modifier.fillMaxWidth(),
    isEnabled = true
)
```

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `paymentRequest` | `PayOrcCheckoutRequest` | yes | Checkout payload. |
| `onTabbyAuthorized` | `() -> Unit` | yes | Called when Tabby authorization succeeds. |
| `onTabbyError` | `(Throwable?) -> Unit` | yes | Called when Tabby fails or is rejected. |
| `modifier` | `Modifier` | no | Compose modifier. |
| `buttonColor` | `Color` | no | Button background color. |
| `isEnabled` | `Boolean` | no | Enables/disables the button. |

---

## Enums

### `PayOrcEnvironment`

| Value | Meaning |
| --- | --- |
| `SANDBOX` | Sandbox/test endpoints. |
| `PRODUCTION` | Production endpoints. |

### `PayorcLanguage`

| Value | Effect |
| --- | --- |
| `english` | English language. |
| `arabic` | Arabic language. |

### `PayorcInputBorderStyle`

| Value | Use |
| --- | --- |
| `outline` | Bordered text fields. |
| `underline` | Underline-only text fields. |

### `PayorcAppBarStyle`

| Value | Use |
| --- | --- |
| `standard` | Standard SDK app bar. |
| `nativeIos` | iOS-style app bar behavior. |
| `nativeAndroid` | Android-style app bar behavior. |
| `nativeAndroid_Standard` | Android standard variant. |

---

## Core Types

- `PayOrcSdk` - SDK singleton. Call `PayOrcSdk.init` before using `PayOrcSdk.instance`.
- `PayOrcCheckout` - Hosted checkout entry point.
- `PayOrcCheckoutRequest` - Checkout payload containing order, customer, billing, shipping, and URL details.
- `PaymentTokenData` - Success payload returned from card/add-card flow callbacks.
- `PayorcEmbeddedCardButton` - Compose card payment button.
- `PayorcEmbeddedTabbyButton` - Compose Tabby payment button.

---

## Related APIs

| API | Purpose |
| --- | --- |
| `PayOrcCheckout.start` | Opens the SDK checkout flow. |
| `PayOrcCheckout.PayOrcCheckoutSheet` | Compose checkout sheet entry point. |
| `PayorcEmbeddedCardButton` | Direct card payment button for Compose UI. |
| `PayorcEmbeddedTabbyButton` | Direct Tabby payment button for Compose UI. |

---

## Best Practices

- Initialize the SDK once before checkout.
- Keep sandbox and production credentials separate.
- Do not commit production merchant credentials.
- Use a unique `mOrderId` for each payment attempt.
- Provide customer name, email, and mobile for payment methods that require customer information.
- Apply `PayOrcSdk.customization` before opening checkout.
- Handle callbacks idempotently to avoid duplicate order updates.
- Show user-friendly error messages from `onFailure`, `onPaymentError`, or `onTabbyError`.

---

## Troubleshooting

| Issue | Resolution |
| --- | --- |
| SDK import cannot be resolved | Verify Maven repository, credentials, artifact ID, and version. |
| `PayOrcSdk not initialized` | Call `PayOrcSdk.init` before opening checkout or rendering embedded buttons. |
| Payment method does not appear | Confirm the method is enabled in PayOrc merchant configuration and request amount/currency are valid. |
| Tabby does not appear or fails | Confirm Tabby is enabled for the merchant and customer/order fields are complete. |
| Google Pay does not appear | Test on a supported device/account and verify Google Pay merchant configuration. |
| Custom colors do not appear | Call `PayOrcSdk.customization` after init and before checkout. |
| Checkout error callback is received | Show the message from `onFailure` and allow the customer to retry. |
