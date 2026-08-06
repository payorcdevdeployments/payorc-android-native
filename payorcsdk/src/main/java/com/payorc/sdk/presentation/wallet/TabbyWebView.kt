package com.payorc.sdk.presentation.wallet

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Tabby Web View
 * 
 * Component to display Tabby's checkout web interface.
 */
@Composable
fun TabbyWebView(
    url: String,
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    onFailure: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // Enable cookies and third-party cookies (critical for bank 3DS/OTP verification domains)
                android.webkit.CookieManager.getInstance().let { cookieManager ->
                    cookieManager.setAcceptCookie(true)
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        cookieManager.setAcceptThirdPartyCookies(this, true)
                    }
                }
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val currentUrl = request?.url?.toString() ?: ""
                        return when {
                            currentUrl.contains("payorc://tabby/success") -> {
                                onSuccess()
                                true
                            }
                            currentUrl.contains("payorc://tabby/cancel") -> {
                                onCancel()
                                true
                            }
                            currentUrl.contains("payorc://tabby/failure") -> {
                                onFailure("Payment failed")
                                true
                            }
                            else -> false
                        }
                    }
                }
                loadUrl(url)
            }
        },
        modifier = modifier
    )
}
