package com.quranaudio.common.ad

import android.content.Context
import android.util.Log
import java.util.Calendar

/**
 * 插屏广告频控。
 *
 * 此前 InterstitialAdManager.showAdIfAvailable() 只检查两件事：是否订阅、缓存是否过期。
 * **没有最小间隔、没有单次会话上限、没有单日上限**——
 * 只要触发点命中就播。一个活跃新用户在 D0 就可能吃到 4~5 个全屏广告。
 *
 * 默认值与 app 模块的 AdPolicy 保持一致；App 启动时会调用 [configure] 同步，
 * 使 AdPolicy 成为唯一的调参入口。即使 configure 未被调用（如后台进程），
 * 这里的默认值也已经能保护用户。
 */
object InterstitialFrequencyCap {

    private const val TAG = "InterstitialCap"
    private const val PREFS = "ad_frequency_cap"

    private const val K_FIRST_SEEN = "first_seen_at"
    private const val K_LAST_SHOWN = "last_shown_at"
    private const val K_DAY_KEY = "day_key"
    private const val K_DAY_COUNT = "day_count"

    // ---- 可由 AdPolicy 覆盖的配置（默认值与 AdPolicy 一致）----
    @Volatile private var graceHours: Long = 0L
    @Volatile private var minIntervalMinutes: Long = 3L
    @Volatile private var maxPerSession: Int = 2
    @Volatile private var maxPerDay: Int = 5

    /** 本次进程内已展示次数。进程重建即归零，等价于「会话」。 */
    @Volatile private var sessionCount: Int = 0

    @JvmStatic
    fun configure(
        graceHours: Long,
        minIntervalMinutes: Long,
        maxPerSession: Int,
        maxPerDay: Int
    ) {
        this.graceHours = graceHours
        this.minIntervalMinutes = minIntervalMinutes
        this.maxPerSession = maxPerSession
        this.maxPerDay = maxPerDay
    }

    /**
     * 是否允许现在展示插屏。
     *
     * @return 允许返回 null；不允许返回拒绝原因（用于埋点归因）
     */
    @JvmStatic
    fun blockReason(context: Context): String? {
        return try {
            val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()

            // 首次见到该用户的时间。graceHours 配置为 0 时不会拦截；保留该字段，
            // 便于将来只通过策略配置恢复保护期而无需迁移用户数据。
            var firstSeen = sp.getLong(K_FIRST_SEEN, 0L)
            if (firstSeen == 0L) {
                firstSeen = now
                sp.edit().putLong(K_FIRST_SEEN, firstSeen).apply()
            }
            if (now - firstSeen < graceHours * 60 * 60 * 1000L) {
                return "new_user_grace"
            }

            if (sessionCount >= maxPerSession) {
                return "session_cap"
            }

            val lastShown = sp.getLong(K_LAST_SHOWN, 0L)
            if (lastShown > 0L && now - lastShown < minIntervalMinutes * 60 * 1000L) {
                return "min_interval"
            }

            if (dayCount(sp, now) >= maxPerDay) {
                return "daily_cap"
            }

            null
        } catch (t: Throwable) {
            // 频控自身出错时放行，避免把广告收入直接归零
            Log.w(TAG, "blockReason failed, allowing", t)
            null
        }
    }

    /** 插屏真正展示后调用，推进各计数。 */
    @JvmStatic
    fun onShown(context: Context) {
        try {
            val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            sessionCount++
            sp.edit()
                .putLong(K_LAST_SHOWN, now)
                .putString(K_DAY_KEY, todayKey(now))
                .putInt(K_DAY_COUNT, dayCount(sp, now) + 1)
                .apply()
            Log.d(TAG, "shown: session=$sessionCount day=${dayCount(sp, now)}")
        } catch (t: Throwable) {
            Log.w(TAG, "onShown failed", t)
        }
    }

    /** 当天已展示次数。跨自然日自动归零。 */
    private fun dayCount(sp: android.content.SharedPreferences, now: Long): Int {
        val today = todayKey(now)
        return if (sp.getString(K_DAY_KEY, "") == today) sp.getInt(K_DAY_COUNT, 0) else 0
    }

    /** 用本地日历日做 key，符合用户对「今天」的直觉（而非 UTC 或固定 24h 窗口）。 */
    private fun todayKey(now: Long): String {
        val c = Calendar.getInstance()
        c.timeInMillis = now
        return "${c.get(Calendar.YEAR)}-${c.get(Calendar.DAY_OF_YEAR)}"
    }
}
