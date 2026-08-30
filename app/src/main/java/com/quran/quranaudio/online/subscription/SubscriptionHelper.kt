package com.quran.quranaudio.online.subscription

import android.content.Context
import android.content.Intent
import android.app.Activity
import androidx.appcompat.app.AlertDialog
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.analytics.RetentionFunnel

/**
 * 订阅功能辅助类
 * 提供订阅状态检查和导航功能
 */
object SubscriptionHelper {

    private const val PREFS_NAME = "subscription_prefs"
    private const val KEY_IS_SUBSCRIBED = "is_subscribed"
    private const val KEY_PRODUCT_ID = "product_id"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"

    /**
     * 检查用户是否已订阅
     */
    fun isUserSubscribed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_SUBSCRIBED, false)
    }

    /**
     * 获取订阅的产品ID
     */
    fun getSubscribedProductId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PRODUCT_ID, null)
    }

    /**
     * 检查用户是否订阅了年度套餐
     */
    fun isYearlySubscriber(context: Context): Boolean {
        return getSubscribedProductId(context) == BillingManager.YEARLY_PLAN_ID
    }

    /**
     * 检查用户是否订阅了月度套餐
     */
    fun isMonthlySubscriber(context: Context): Boolean {
        return getSubscribedProductId(context) == BillingManager.MONTHLY_PLAN_ID
    }

    /**
     * 获取上次检查订阅状态的时间
     */
    fun getLastCheckTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
    }

    /**
     * 启动订阅页面
     */
    @JvmOverloads
    fun launchSubscriptionPage(context: Context, source: String = "unknown") {
        val intent = Intent(context, SubscriptionActivity::class.java)
        intent.putExtra(SubscriptionActivity.EXTRA_SOURCE, source)
        context.startActivity(intent)
    }

    // ============================================================
    // 情境化订阅触发（价值交付后再谈钱，替代仅首装硬弹）
    // ============================================================
    private const val PROMPT_PREFS = "subscription_prompt_prefs"
    private const val KEY_PROMPT_COUNT = "prompt_count"
    private const val KEY_LAST_PROMPT_TIME = "last_prompt_time"
    private const val KEY_ENGAGED_SESSIONS = "engaged_sessions"

    private const val MIN_INTERVAL_MS = 2L * 24 * 60 * 60 * 1000  // 两次提示最少间隔 2 天
    private const val MAX_LIFETIME_PROMPTS = 6
    // 首次真实阅读价值交付后即可邀请；后续节点逐步拉开，避免打断每日敬拜习惯。
    private val TRIGGER_AT_SESSIONS = intArrayOf(1, 3, 7, 14, 30, 60)

    /**
     * 情境化订阅提示：仅对"已体验价值的非订阅用户"在自然节点软性触发。
     *
     * 触发条件(全部满足)：
     * - 未订阅
     * - 已有阅读价值(hasReadingValue=true，如已读过经文)
     * - 命中预设的第 N 次 engaged 会话
     * - 距上次提示 >= 2 天，且一生提示 < 6 次
     *
     * 这修正了"仅首装硬弹付费墙"(价值交付前要钱、转化极低)的问题。
     *
     * @return true 表示本次展示了订阅页
     */
    fun maybeShowContextualPrompt(context: Context, hasReadingValue: Boolean): Boolean {
        try {
            if (isUserSubscribed(context) || !hasReadingValue) return false
            val activity = context as? Activity ?: return false
            if (activity.isFinishing || activity.isDestroyed) return false

            val prefs = context.getSharedPreferences(PROMPT_PREFS, Context.MODE_PRIVATE)

            // engaged 会话按"天"计：同一天多次回到首页/切 tab 只算一次，
            // 使 {3,8,20} 对应使用的第 3/8/20 天，而非 onResume 次数(避免首日被 tab 切换灌满)
            val today = (System.currentTimeMillis() / (24L * 60 * 60 * 1000)).toInt()
            val lastDay = prefs.getInt("last_engaged_day", -1)
            if (today == lastDay) return false  // 今天已计过，不再累加也不触发

            val sessions = prefs.getInt(KEY_ENGAGED_SESSIONS, 0) + 1
            prefs.edit()
                .putInt(KEY_ENGAGED_SESSIONS, sessions)
                .putInt("last_engaged_day", today)
                .apply()

            if (!TRIGGER_AT_SESSIONS.contains(sessions)) return false

            val count = prefs.getInt(KEY_PROMPT_COUNT, 0)
            if (count >= MAX_LIFETIME_PROMPTS) return false

            val lastTime = prefs.getLong(KEY_LAST_PROMPT_TIME, 0L)
            if (System.currentTimeMillis() - lastTime < MIN_INTERVAL_MS) return false

            prefs.edit()
                .putInt(KEY_PROMPT_COUNT, count + 1)
                .putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis())
                .apply()

            // A soft invitation after value delivery avoids breaking reading, prayer or dhikr.
            RetentionFunnel.subscription(
                activity, "prompt_shown", "reading_value_prompt", "none", "none", "shown"
            )
            val dialog = AlertDialog.Builder(activity)
                .setTitle(R.string.subscription_context_title)
                .setMessage(R.string.subscription_context_message)
                .setPositiveButton(R.string.subscription_context_view) { _, _ ->
                    RetentionFunnel.subscription(
                        activity, "prompt_action", "reading_value_prompt", "none", "none", "view"
                    )
                    launchSubscriptionPage(activity, "reading_value_prompt")
                }
                .setNegativeButton(R.string.subscription_context_not_now) { _, _ ->
                    RetentionFunnel.subscription(
                        activity, "prompt_action", "reading_value_prompt", "none", "none", "not_now"
                    )
                }
                .create()
            dialog.setOnCancelListener {
                RetentionFunnel.subscription(
                    activity, "prompt_action", "reading_value_prompt", "none", "none", "dismissed"
                )
            }
            dialog.show()
            return true
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * 清除订阅状态（用于测试）
     */
    fun clearSubscriptionStatus(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * 获取订阅状态的可读字符串
     */
    fun getSubscriptionStatusString(context: Context): String {
        return if (isUserSubscribed(context)) {
            val productId = getSubscribedProductId(context)
            when (productId) {
                BillingManager.YEARLY_PLAN_ID -> "Subscribed (Yearly)"
                BillingManager.MONTHLY_PLAN_ID -> "Subscribed (Monthly)"
                else -> "Subscribed (Unknown)"
            }
        } else {
            "Not Subscribed"
        }
    }
}
