package com.quran.quranaudio.online.feedback

import android.app.Activity
import android.app.AlertDialog
import android.util.Log

/**
 * 退出拦截管理器
 * 
 * 功能：
 * 1. 监听用户连续两次返回键
 * 2. 如果停留时间不足1分钟，弹出挽留对话框
 * 3. 收集退出原因
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
     * @return true 表示拦截，false 表示放行
     */
    fun onBackPressed(): Boolean {
        val currentTime = System.currentTimeMillis()
        val stayDuration = currentTime - pageEnterTime
        
        // 条件1：停留时间小于1分钟
        if (stayDuration < MIN_STAY_DURATION_MS && !hasShownExitDialog) {
            // 条件2：连续两次按返回键（2秒内）
            if (currentTime - lastBackPressTime < DOUBLE_BACK_PRESS_INTERVAL) {
                // 显示挽留对话框
                showExitDialog()
                lastBackPressTime = 0 // 重置
                return true // 拦截退出
            } else {
                lastBackPressTime = currentTime
                android.widget.Toast.makeText(
                    activity,
                    "再按一次退出应用",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return true // 拦截退出
            }
        }
        
        // 停留时间超过1分钟，或已显示过对话框，放行
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
            FeedbackManager.getInstance().submitFeedback(
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

