package com.payorc.sdk.core.network

import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.util.Log

object WebViewDiffLogger {
    fun dumpSettings(tag: String, webView: WebView) {
        val settings = webView.settings
        val logData = StringBuilder("\n--- WebView Settings Dump [$tag] ---\n")
        
        try {
            logData.append("javaScriptEnabled: ${settings.javaScriptEnabled}\n")
            logData.append("domStorageEnabled: ${settings.domStorageEnabled}\n")
            logData.append("databaseEnabled: ${settings.databaseEnabled}\n")
            logData.append("allowFileAccess: ${settings.allowFileAccess}\n")
            logData.append("allowContentAccess: ${settings.allowContentAccess}\n")
            logData.append("loadWithOverviewMode: ${settings.loadWithOverviewMode}\n")
            logData.append("useWideViewPort: ${settings.useWideViewPort}\n")
            logData.append("javaScriptCanOpenWindowsAutomatically: ${settings.javaScriptCanOpenWindowsAutomatically}\n")
            logData.append("supportMultipleWindows: ${settings.supportMultipleWindows()}\n")
            logData.append("userAgentString: ${settings.userAgentString}\n")
            logData.append("builtInZoomControls: ${settings.builtInZoomControls}\n")
            logData.append("displayZoomControls: ${settings.displayZoomControls}\n")
            logData.append("loadsImagesAutomatically: ${settings.loadsImagesAutomatically}\n")
            logData.append("mediaPlaybackRequiresUserGesture: ${settings.mediaPlaybackRequiresUserGesture}\n")
            logData.append("textZoom: ${settings.textZoom}\n")
            logData.append("offscreenPreRaster: ${if (android.os.Build.VERSION.SDK_INT >= 23) settings.offscreenPreRaster else "N/A"}\n")
            logData.append("safeBrowsingEnabled: ${if (android.os.Build.VERSION.SDK_INT >= 26) settings.safeBrowsingEnabled else "N/A"}\n")
            logData.append("mixedContentMode: ${if (android.os.Build.VERSION.SDK_INT >= 21) settings.mixedContentMode else "N/A"}\n")
            logData.append("cacheMode: ${settings.cacheMode}\n")
            
            val cm = CookieManager.getInstance()
            logData.append("acceptCookie: ${cm.acceptCookie()}\n")
            logData.append("acceptThirdPartyCookies: ${if (android.os.Build.VERSION.SDK_INT >= 21) cm.acceptThirdPartyCookies(webView) else "N/A"}\n")
            
            Log.d("WebViewDiff", logData.toString())
        } catch (e: Exception) {
            Log.e("WebViewDiff", "Error dumping settings", e)
        }
    }
}
