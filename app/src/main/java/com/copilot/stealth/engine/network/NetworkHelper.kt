package com.copilot.stealth.engine.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

/**
 * Utility to verify real-time active internet connectivity.
 * Used for zero-latency instant routing between Cloud AI and On-Device Local Neural engines.
 */
object NetworkHelper {

    fun isOnline(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                activeNetworkInfo != null && activeNetworkInfo.isConnected
            }
        } catch (_: Exception) {
            // In case of permission/security quirk, fallback to optimistic online check
            true
        }
    }
}
