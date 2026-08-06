package com.payorc.sdk.presentation.checkout

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.payorc.sdk.core.network.WebViewDiffLogger
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject

class PayOrc3DSActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val redirectUrl = intent.getStringExtra("REDIRECT_URL") ?: run {
            PayOrcCheckout.current3DSCallback?.onFailure("Missing redirect URL")
            PayOrcCheckout.current3DSCallback = null
            finish()
            return
        }

        setContent {
            MaterialTheme {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                var showBottomSheet by remember { mutableStateOf(true) }

                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheet = false
                            PayOrcCheckout.current3DSCallback?.onFailure("3DS authorization was cancelled")
                            PayOrcCheckout.current3DSCallback = null
                            finish()
                        },
                        sheetState = sheetState
                    ) {
                        Payment3DSRedirectScreen(
                            redirectUrl = redirectUrl,
                            onTransactionComplete = { transactionId ->
                                showBottomSheet = false
                                runOnUiThread {
                                    PayOrcCheckout.current3DSCallback?.onSuccess(transactionId)
                                    PayOrcCheckout.current3DSCallback = null
                                    finish()
                                }
                            },
                            onFailure = { reason ->
                                showBottomSheet = false
                                runOnUiThread {
                                    PayOrcCheckout.current3DSCallback?.onFailure(reason)
                                    PayOrcCheckout.current3DSCallback = null
                                    finish()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Payment3DSRedirectScreen(
    redirectUrl: String,
    onTransactionComplete: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    BackHandler(enabled = true) {
        onFailure("3DS authorization was cancelled")
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
        factory = { context ->
            WebView.setWebContentsDebuggingEnabled(true)
            val wv = WebView(context)
            wv.apply {
                // FORCE THE SAME CONFIG AS FLUTTER WEBVIEW_FLUTTER PLUGIN
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    
                    // Flutter webview_flutter plugin DEFAULTS:
                    setSupportMultipleWindows(false) // Flutter's default is FALSE
                    javaScriptCanOpenWindowsAutomatically = false // Flutter's default is FALSE
                    useWideViewPort = false // Flutter's default is FALSE
                    loadWithOverviewMode = false // Flutter's default is FALSE
                    
                    // Revert to RAW system User-Agent (don't strip identifiers)
                    // userAgentString = null // Use default
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE // Flutter's default
                    }
                }

                // Enable cookies and third-party cookies (critical for bank 3DS/OTP verification domains)
                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    cookieManager.setAcceptThirdPartyCookies(this, true)
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postMessage(raw: String) {
                        handlePaymentPostback(raw, onTransactionComplete, onFailure)
                    }
                }, "PayOrc3DS")

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                        val msg = consoleMessage?.message() ?: ""
                        val source = consoleMessage?.sourceId() ?: ""
                        val line = consoleMessage?.lineNumber() ?: 0
                        android.util.Log.d("PayOrc3DS_Diag", "Console [$source:$line]: $msg")

                        if (msg.contains("ACS iframe onload event triggered") || msg.contains("SEND OTP TRIGGERED 2")) {
                            android.util.Log.d("PayOrc3DS_Diag", "ACS/OTP event detected. Inspecting...")
                            inspectDomDeeply(wv)
                        }
                        
                        if (msg.contains("postback message") || msg.contains("status\":\"success\"")) {
                            handlePaymentPostback(msg, onTransactionComplete, onFailure)
                        }
                        return true
                    }

                    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                        android.util.Log.d("PayOrc3DS_Diag", "JS Alert: $message")
                        return super.onJsAlert(view, url, message, result)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: android.webkit.SslErrorHandler?,
                        error: android.net.http.SslError?
                    ) {
                        android.util.Log.w("PayOrc3DS_Diag", "SSL Error: $error")
                        handler?.proceed()
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        android.util.Log.d("PayOrc3DS_Diag", "Page Started: $url")
                        cookieManager.flush()
                        
                        view?.let { 
                            WebViewDiffLogger.dumpSettings("PayOrc_Native", it)
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        android.util.Log.d("PayOrc3DS_Diag", "Page Finished: $url")
                        cookieManager.flush()
                        
                        inspectDomDeeply(wv)

                        view?.evaluateJavascript(
                            "(function() {" +
                                    "if (window.__payOrcHooked) return;" +
                                    "window.__payOrcHooked = true;" +
                                    "window.parent.postMessage = function(msg) { try { PayOrc3DS.postMessage(typeof msg === 'string' ? msg : JSON.stringify(msg)); } catch(e){} };" +
                                    "window.top.postMessage = function(msg) { try { PayOrc3DS.postMessage(typeof msg === 'string' ? msg : JSON.stringify(msg)); } catch(e){} };" +
                            "})()"
                        ) { }
                    }

                    override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            android.util.Log.e("PayOrc3DS_Diag", "WebView Error [${error?.errorCode}]: ${error?.description} for ${request?.url}")
                        }
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                        android.util.Log.d("PayOrc3DS_Diag", "Navigating/Redirect to: ${request?.url}")
                        return false
                    }
                }
                loadUrl(redirectUrl)
            }
            wv
        }
    )
}

private fun inspectDomDeeply(view: WebView?) {
    if (view == null) return
    
    view.evaluateJavascript(
        """
        (function() {
            try {
                var results = {};
                results.url = window.location.href;
                results.userAgent = navigator.userAgent;
                
                // 1. Dimensions
                results.body = { w: document.body.offsetWidth, h: document.body.offsetHeight };

                // 2. Iframe Analysis
                results.iframes = Array.from(document.querySelectorAll('iframe')).map(i => {
                    var style = window.getComputedStyle(i);
                    return {
                        id: i.id,
                        src: i.src,
                        visible: i.offsetWidth > 0 && i.offsetHeight > 0,
                        dim: { w: i.offsetWidth, h: i.offsetHeight },
                        style: { display: style.display, vis: style.visibility }
                    };
                });

                // 3. OTP Discovery
                results.otpElements = Array.from(document.querySelectorAll('input')).filter(el => 
                    el.type === 'password' || el.type === 'tel' || el.id.toLowerCase().includes('otp')
                ).map(el => ({ id: el.id, vis: el.offsetWidth > 0 }));

                return JSON.stringify(results);
            } catch (e) {
                return JSON.stringify({ error: e.message });
            }
        })()
        """.trimIndent()
    ) { result ->
        android.util.Log.d("PayOrc3DS_DOM", "DOM State: $result")
    }
}

private fun handlePaymentPostback(raw: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
    if (raw.isBlank() || raw.contains("MessageType") || raw.contains("CardinalJWT")) return
    val normalized = raw.trim().removeSurrounding("\"").replace("\\\"", "\"")
    val jsonText = extractPaymentJsonObject(normalized) ?: return
    try {
        val root = JSONObject(jsonText)
        val data = root.optJSONObject("data") ?: root
        val status = root.optString("status", data.optString("status", "")).lowercase().trim()
        val code = root.optString("code", data.optString("code", "")).uppercase().trim()
        val reason = data.optString("reason", root.optString("message", "Verification failed"))
        val transactionId = data.optString("transaction_id", root.optString("transaction_id", ""))
        val orderStatus = data.optString("order_status", root.optString("order_status", "")).uppercase().trim()
        val redirectUrl = data.optString("redirect_url", root.optString("redirect_url", "")).trim()

        if (status in listOf("wait", "challenge", "postback", "proceed", "pending", "await_3ds")) return

        val isTerminalSuccess = code == "CARD_VERIFIED" ||
                orderStatus in listOf("SUCCESS", "AUTHORIZED", "AUTHORISED", "CAPTURED", "COMPLETED") ||
                (status == "success" && orderStatus.isBlank() && code == "00" && raw.contains("postback message"))

        val isTerminalFailure = status in listOf("failed", "failure", "error") ||
                orderStatus in listOf("FAILED", "DECLINED") ||
                code in listOf("PAYMENT_FAILED", "FAILED")

        when {
            isTerminalSuccess -> {
                onSuccess(transactionId.ifBlank { "unknown" })
            }
            isTerminalFailure -> {
                onFailure(reason)
            }
        }
    } catch (e: Exception) {
        android.util.Log.d("PayOrc3DS", "Postback error", e)
    }
}

private fun extractPaymentJsonObject(input: String): String? {
    val start = input.indexOf('{')
    val end = input.lastIndexOf('}')
    return if (start >= 0 && end > start) input.substring(start, end + 1) else null
}
