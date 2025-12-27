package com.quran.quranaudio.online.Utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.ActivityResultLauncher
import com.google.firebase.auth.FirebaseAuth
import com.quran.quranaudio.online.R

/**
 * Account Upgrade Dialog - 账户升级提示
 * 
 * 功能：
 * - 在匿名用户坚持7天后显示
 * - 提示用户关联Google账号以永久保存数据
 * - 提供"立即关联"和"稍后提醒"选项
 */
class AccountUpgradeDialog {
    
    companion object {
        private const val TAG = "AccountUpgradeDialog"
        private const val PREFS_NAME = "account_upgrade_prefs"
        private const val KEY_LAST_PROMPT_DATE = "last_prompt_date"
        private const val KEY_PROMPT_COUNT = "prompt_count"
        private const val MAX_PROMPT_COUNT = 5  // 最多提示5次
        
        /**
         * 显示账户升级提示
         * 
         * @param activity Activity
         * @param currentStreak 当前连续天数
         * @param signInLauncher Google登录的ActivityResultLauncher
         * @param googleAuthManager GoogleAuthManager实例
         */
        @JvmStatic
        fun show(
            activity: Activity,
            currentStreak: Int,
            signInLauncher: ActivityResultLauncher<Intent>?,
            googleAuthManager: GoogleAuthManager?
        ) {
            try {
                Log.d(TAG, "🎉 Showing account upgrade dialog")
                Log.d(TAG, "   → Current streak: $currentStreak days")
                
                // 检查是否应该显示（避免频繁打扰）
                if (!shouldShowPrompt(activity)) {
                    Log.d(TAG, "⏭️ Skipping prompt - shown recently or max count reached")
                    return
                }
                
                // 检查用户是否匿名
                val isAnonymous = FirebaseAuth.getInstance().currentUser?.isAnonymous == true
                if (!isAnonymous) {
                    Log.d(TAG, "⏭️ User is not anonymous, skipping prompt")
                    return
                }
                
                // 更新提示记录
                recordPromptShown(activity)
                
                // 显示对话框
                AlertDialog.Builder(activity)
                    .setTitle("🎉 " + activity.getString(R.string.upgrade_account_title))
                    .setMessage(
                        activity.getString(R.string.upgrade_account_message, currentStreak)
                    )
                    .setPositiveButton(activity.getString(R.string.upgrade_account_link_now)) { dialog, _ ->
                        dialog.dismiss()
                        Log.d(TAG, "✅ User clicked 'Link Now'")
                        startAccountLinking(activity, signInLauncher, googleAuthManager)
                    }
                    .setNegativeButton(activity.getString(R.string.upgrade_account_remind_later)) { dialog, _ ->
                        dialog.dismiss()
                        Log.d(TAG, "⏭️ User clicked 'Remind Later'")
                    }
                    .setCancelable(true)
                    .show()
                
                Log.d(TAG, "✅ Account upgrade dialog shown successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to show account upgrade dialog", e)
            }
        }
        
        /**
         * 启动账户关联流程
         */
        private fun startAccountLinking(
            activity: Activity,
            signInLauncher: ActivityResultLauncher<Intent>?,
            googleAuthManager: GoogleAuthManager?
        ) {
            try {
                if (googleAuthManager == null) {
                    Log.e(TAG, "❌ GoogleAuthManager is null")
                    android.widget.Toast.makeText(
                        activity,
                        "Account linking is not available",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                
                if (signInLauncher == null) {
                    Log.e(TAG, "❌ SignInLauncher is null")
                    android.widget.Toast.makeText(
                        activity,
                        "Account linking is not available",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                
                Log.d(TAG, "→ Starting Google Sign-In for account linking...")
                val signInIntent = googleAuthManager.getSignInIntent()
                signInLauncher.launch(signInIntent)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to start account linking", e)
                android.widget.Toast.makeText(
                    activity,
                    "Failed to start account linking: ${e.message}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
        
        /**
         * 检查是否应该显示提示
         * - 每天最多显示1次
         * - 总共最多显示5次
         */
        private fun shouldShowPrompt(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // 检查提示次数
            val promptCount = prefs.getInt(KEY_PROMPT_COUNT, 0)
            if (promptCount >= MAX_PROMPT_COUNT) {
                Log.d(TAG, "Max prompt count reached: $promptCount")
                return false
            }
            
            // 检查上次提示日期
            val lastPromptDate = prefs.getString(KEY_LAST_PROMPT_DATE, "")
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            
            if (lastPromptDate == today) {
                Log.d(TAG, "Already prompted today: $today")
                return false
            }
            
            return true
        }
        
        /**
         * 记录提示已显示
         */
        private fun recordPromptShown(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentCount = prefs.getInt(KEY_PROMPT_COUNT, 0)
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                .format(java.util.Date())
            
            prefs.edit()
                .putString(KEY_LAST_PROMPT_DATE, today)
                .putInt(KEY_PROMPT_COUNT, currentCount + 1)
                .apply()
            
            Log.d(TAG, "Prompt recorded - Count: ${currentCount + 1}, Date: $today")
        }
        
        /**
         * 重置提示计数（用于测试或用户关联账户后）
         */
        @JvmStatic
        fun resetPromptCount(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove(KEY_LAST_PROMPT_DATE)
                .remove(KEY_PROMPT_COUNT)
                .apply()
            
            Log.d(TAG, "✅ Prompt count reset")
        }
    }
}

