# PayOrc Android SDK

The official Android SDK for integrating PayOrc payments.

## Features
- **Hosted Checkout**: Pre-built UI for quick integration.
- **Embedded Components**: Jetpack Compose buttons for Cards and Tabby.
- **Dynamic Theming**: Match the SDK UI to your app's branding.
- **Secure**: Built-in 3D Secure handling and card validation.

## Documentation
For a full integration guide, including code examples and customization options, please refer to [INTEGRATION_GUIDE.md](payorcsdk/INTEGRATION_GUIDE.md).

## Quick Start
```kotlin
PayOrcSdk.init(context, merchantKey, merchantSecret, PayOrcEnvironment.SANDBOX)
```
