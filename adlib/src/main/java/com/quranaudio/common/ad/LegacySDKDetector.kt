package com.quranaudio.common.ad

import android.util.Log

/**
 * Detects legacy/problematic SDKs that may cause conflicts.
 * 
 * Used to detect old APK versions that still contain removed SDKs like StartApp.
 * These SDKs can cause thread deadlocks when initializing Google Ads.
 */
object LegacySDKDetector {
    
    private const val TAG = "LegacySDKDetector"
    
    /**
     * Check if StartApp SDK classes are present in the APK.
     * If present, it means this is an old APK that needs to be uninstalled.
     * 
     * @return true if StartApp SDK is detected (old APK), false if clean
     */
    fun hasStartAppSDK(): Boolean {
        return try {
            // Try to load StartApp SDK class
            Class.forName("com.startapp.sdk.internal.qn")
            Log.w(TAG, "⚠️ StartApp SDK detected! This is an OLD APK version.")
            Log.w(TAG, "⚠️ Please UNINSTALL and REINSTALL the app to avoid deadlocks.")
            true
        } catch (e: ClassNotFoundException) {
            // Good - StartApp not present
            Log.d(TAG, "✅ No legacy StartApp SDK detected")
            false
        } catch (e: Exception) {
            // Other errors - assume clean
            Log.d(TAG, "✅ Legacy SDK check completed")
            false
        }
    }
    
    /**
     * Check if Pangle SDK classes are present in the APK.
     * 
     * @return true if Pangle SDK is detected (old APK), false if clean
     */
    fun hasPangleSDK(): Boolean {
        return try {
            Class.forName("com.bytedance.sdk.openadsdk.multipro.aidl.BinderPoolService")
            Log.w(TAG, "⚠️ Pangle SDK detected! This is an OLD APK version.")
            true
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "✅ No legacy Pangle SDK detected")
            false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check for any legacy SDKs that should not be present.
     * 
     * @return true if any problematic legacy SDK is found
     */
    fun hasLegacySDKs(): Boolean {
        val hasStartApp = hasStartAppSDK()
        val hasPangle = hasPangleSDK()
        
        return hasStartApp || hasPangle
    }
    
    /**
     * Get a user-friendly message about detected legacy SDKs.
     */
    fun getLegacySDKWarningMessage(): String? {
        if (!hasLegacySDKs()) {
            return null
        }
        
        val sdks = mutableListOf<String>()
        if (hasStartAppSDK()) sdks.add("StartApp")
        if (hasPangleSDK()) sdks.add("Pangle")
        
        return """
            Old app version detected!
            
            This version contains outdated SDKs (${sdks.joinToString(", ")}) that can cause crashes.
            
            Please:
            1. Uninstall this version completely
            2. Reinstall the latest version
            
            This will fix stability issues.
        """.trimIndent()
    }
}

