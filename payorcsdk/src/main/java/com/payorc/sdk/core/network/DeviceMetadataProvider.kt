package com.payorc.sdk.core.network

import android.content.Context
import android.os.Build
import java.util.*

object DeviceMetadataProvider {

    fun getAppId(context: Context): String = context.packageName

    fun getAppVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    private fun getPrefs(context: Context) = 
        context.getSharedPreferences("payorc_sdk_prefs", Context.MODE_PRIVATE)

    fun getDeviceId(context: Context): String {
        val prefs = getPrefs(context)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id ?: ""
    }

    fun getBrowserToken(context: Context): String {
        val prefs = getPrefs(context)
        var token = prefs.getString("browser_token", null)
        if (token == null) {
            token = UUID.randomUUID().toString()
            prefs.edit().putString("browser_token", token).apply()
        }
        return token ?: ""
    }

    fun getDeviceModel(): String = Build.MODEL
    fun getDeviceOS(): String = "Android"
    fun getDeviceBrand(): String = Build.MANUFACTURER

    fun getHeaders(
        context: Context, 
        appIdOverride: String? = null, 
        appVersionOverride: String? = null,
        clientIp: String? = null,
        clientIpCountry: String? = null
    ): Map<String, String> {
        return mutableMapOf(
            "X-App-ID" to (appIdOverride ?: getAppId(context)),
            "X-Device-Id" to getDeviceId(context),
            "X-Device-OS" to getDeviceOS(),
            "X-Device-Model" to getDeviceModel(),
            "X-App-Version" to (appVersionOverride ?: getAppVersion(context)),
            "X-Browser-Token" to getBrowserToken(context),
            "X-SDK-Name" to "payorc-sdk",
            "X-SDK-Version" to "1.0.0",
            "X-DEVICE-BRAND" to getDeviceBrand(),
            "X-IP" to (clientIp ?: "127.0.0.1"),
            "X-IP-COUNTRY" to (clientIpCountry ?: "ZZ")
        )
    }
}
