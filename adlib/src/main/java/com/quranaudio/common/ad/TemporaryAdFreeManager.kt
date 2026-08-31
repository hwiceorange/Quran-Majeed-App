package com.quranaudio.common.ad

import android.content.Context

/** A bounded local entitlement granted only after a rewarded-ad earned callback. */
object TemporaryAdFreeManager {
    private const val PREFS = "temporary_ad_free"
    private const val KEY_GRANTED_AT = "granted_at"
    private const val KEY_EXPIRES_AT = "expires_at"
    const val DURATION_MS = 60L * 60L * 1000L

    @JvmStatic
    fun grantOneHour(context: Context, now: Long = System.currentTimeMillis()): Long {
        val expiresAt = now + DURATION_MS
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_GRANTED_AT, now).putLong(KEY_EXPIRES_AT, expiresAt).apply()
        return expiresAt
    }

    @JvmStatic
    fun isActive(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val grantedAt = prefs.getLong(KEY_GRANTED_AT, 0L)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        val valid = isWindowValid(grantedAt, expiresAt, now)
        if (!valid && (grantedAt != 0L || expiresAt != 0L)) prefs.edit().clear().apply()
        return valid
    }

    internal fun isWindowValid(grantedAt: Long, expiresAt: Long, now: Long): Boolean =
        grantedAt > 0L && now >= grantedAt && expiresAt > now &&
            expiresAt - grantedAt in 1..DURATION_MS

    @JvmStatic
    fun remainingMillis(context: Context, now: Long = System.currentTimeMillis()): Long {
        if (!isActive(context, now)) return 0L
        val expiresAt = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_EXPIRES_AT, 0L)
        return (expiresAt - now).coerceIn(0L, DURATION_MS)
    }
}
