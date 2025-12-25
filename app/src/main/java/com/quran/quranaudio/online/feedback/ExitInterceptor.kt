package com.quran.quranaudio.online.feedback

import android.app.Activity
import android.app.AlertDialog
import android.util.Log

/**
 * 退出拦截管理器
 * 
 * 功能：
 * 1. 仅在用户即将退出应用时拦截（不拦截正常导航）
 * 2. 如果停留时间不足1分钟，弹出挽留对话框
 * 3. 收集退出原因
 * 
 * ⚠️ 重要：只拦截"退出应用"行为，不拦截正常的返回导航
 */
class ExitInterceptor(private val activity: Activity) {
    
    companion object {
        private const val TAG = "ExitInterceptor"
        private const val MIN_STAY_DURATION_MS = 60 * 1000L // 1分钟
        private const val DOUBLE_BACK_PRESS_INTERVAL = 2000L // 2秒
    }
    
    // 页面进入时间
    private var pageEnterTime: Long = System.currentTimeMillis()
    
    // 上次按返回键的时间
    private var lastBackPressTime: Long = 0
    
    // 是否已显示挽留对话框
    private var hasShownExitDialog = false
    
    /**
     * 重置进入时间（页面切换时调用）
     */
    fun resetEnterTime() {
        pageEnterTime = System.currentTimeMillis()
        hasShownExitDialog = false
    }
    
    /**
     * 处理返回键
     * @return true 表示拦截（阻止退出应用），false 表示放行（允许正常返回或退出）
     * 
     * ⚠️ 关键逻辑：
     * 1. 只有在用户即将退出应用时才拦截
     * 2. 正常的页面返回导航不应该被拦截
     * 3. MainActivity 是根 Activity，按返回键会退出应用
     */
    fun onBackPressed(): Boolean {
        val currentTime = System.currentTimeMillis()
        val stayDuration = currentTime - pageEnterTime
        
        Log.d(TAG, "⬅️ onBackPressed() - stayDuration: ${stayDuration/1000}s")
        
        // 条件1：停留时间小于1分钟
        if (stayDuration < MIN_STAY_DURATION_MS && !hasShownExitDialog) {
            // 条件2：连续两次按返回键（2秒内）- 确认用户真的想退出
            if (currentTime - lastBackPressTime < DOUBLE_BACK_PRESS_INTERVAL) {
                Log.d(TAG, "🚨 Double back press detected - showing exit dialog")
                // 显示挽留对话框
                showExitDialog()
                lastBackPressTime = 0 // 重置
                return true // 拦截退出
            } else {
                // 第一次按返回键
                lastBackPressTime = currentTime
                android.widget.Toast.makeText(
                    activity,
                    "再按一次退出应用",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "⚠️ First back press - waiting for second press")
                return true // 拦截退出（等待第二次按键）
            }
        }
        
        // 放行情况：
        // 1. 停留时间超过1分钟 - 用户已经充分使用了应用
        // 2. 已显示过对话框 - 避免重复打扰
        Log.d(TAG, "✅ Back press released - stayDuration: ${stayDuration/1000}s, hasShownExitDialog: $hasShownExitDialog")
        return false
    }
    
    /**
     * 显示退出挽留对话框
     */
    private fun showExitDialog() {
        hasShownExitDialog = true
        
        val options = arrayOf(
            "加载太慢",
            "界面太乱",
            "功能不会用",
            "广告太多",
            "其他"
        )
        
        AlertDialog.Builder(activity)
            .setTitle("没找到想要的内容吗？")
            .setItems(options) { dialog, which ->
                val selectedReason = options[which]
                
                // 静默提交反馈
                submitExitFeedback(selectedReason)
                
                // 允许退出
                dialog.dismiss()
                activity.finishAffinity()
            }
            .setNegativeButton("继续使用") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
        
        Log.d(TAG, "📱 Exit dialog shown (stay duration < 1 min)")
    }
    
    /**
     * 静默提交退出反馈
     */
    private fun submitExitFeedback(reason: String) {
        try {
            FeedbackManager.Companion.getInstance().submitFeedback(
                context = activity,
                emotion = FeedbackEmotion.HATE, // 快速退出视为不满意
                selectedTags = listOf(reason),
                comment = "用户快速退出（停留不足1分钟）",
                onSuccess = {
                    Log.d(TAG, "✅ Exit feedback submitted: $reason")
                },
                onFailure = { e ->
                    Log.w(TAG, "⚠️ Exit feedback failed: ${e.message}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to submit exit feedback", e)
        }
    }
}

