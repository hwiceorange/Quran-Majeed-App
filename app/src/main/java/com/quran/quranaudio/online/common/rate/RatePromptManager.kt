package com.quran.quranaudio.online.common.rate

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.RatingBar
import com.quran.quranaudio.online.R
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

object RatePromptManager {

    private const val PREFS_NAME = "rate_prompt_prefs"
    private const val KEY_HAS_HIGH_RATED = "has_high_rated"
    private const val KEY_LAST_DISMISS_TIME = "last_dismiss_time"
    private const val KEY_LAST_PROMPT_TIME = "last_prompt_time"
    private const val KEY_DAILY_PROMPT_DATE = "daily_prompt_date"
    private const val KEY_DAILY_PROMPT_COUNT = "daily_prompt_count"
    private const val KEY_LAST_USAGE_DATE = "last_usage_date"
    private const val KEY_CONSECUTIVE_DAYS = "consecutive_days"
    private const val KEY_LAST_CONSEC_PROMPT_DATE = "last_consec_prompt_date"
    private const val KEY_LAST_PRAYER_PROMPT_DATE = "last_prayer_prompt_date"
    private const val KEY_LAST_SURAH_PROMPT_DATE = "last_surah_prompt_date"

    private const val COOLDOWN_MILLIS = 7L * 24 * 60 * 60 * 1000 // 7 days

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    private val scheduledTasks = ConcurrentHashMap<FragmentActivity, Runnable>()
    private var dialogVisible = false

    private enum class RateTrigger { SURAH_COMPLETED, CONSECUTIVE_USAGE, PRAYER_TRACKED }

    @JvmStatic
    fun onReaderUsage(activity: FragmentActivity) {
        val prefs = getPrefs(activity)
        val today = getToday()
        val lastUsage = prefs.getString(KEY_LAST_USAGE_DATE, null)
        var consecutiveDays = prefs.getInt(KEY_CONSECUTIVE_DAYS, 0)

        if (lastUsage == today) {
            return
        }

        consecutiveDays = if (lastUsage != null && isNextDay(lastUsage, today)) {
            consecutiveDays + 1
        } else {
            1
        }

        prefs.edit()
            .putString(KEY_LAST_USAGE_DATE, today)
            .putInt(KEY_CONSECUTIVE_DAYS, consecutiveDays)
            .apply()

        if (consecutiveDays >= 3) {
            val lastPromptDay = prefs.getString(KEY_LAST_CONSEC_PROMPT_DATE, null)
            if (lastPromptDay != today && shouldPrompt(activity)) {
                prefs.edit().putString(KEY_LAST_CONSEC_PROMPT_DATE, today).apply()
                schedulePrompt(activity, RateTrigger.CONSECUTIVE_USAGE, 3000L)
            }
        }
    }

    @JvmStatic
    fun onSurahCompleted(activity: FragmentActivity) {
        val prefs = getPrefs(activity)
        if (!shouldPrompt(activity)) {
            return
        }
        prefs.edit().putString(KEY_LAST_SURAH_PROMPT_DATE, getToday()).apply()
        schedulePrompt(activity, RateTrigger.SURAH_COMPLETED, 5000L)
    }

    @JvmStatic
    fun onPrayerTracked(activity: FragmentActivity) {
        val prefs = getPrefs(activity)
        val today = getToday()
        val lastPromptDay = prefs.getString(KEY_LAST_PRAYER_PROMPT_DATE, null)
        if (lastPromptDay == today) {
            return
        }

        if (!shouldPrompt(activity)) {
            return
        }

        prefs.edit().putString(KEY_LAST_PRAYER_PROMPT_DATE, today).apply()
        schedulePrompt(activity, RateTrigger.PRAYER_TRACKED, 3000L)
    }

    @JvmStatic
    fun cancelScheduledPrompt(activity: FragmentActivity) {
        val runnable = scheduledTasks.remove(activity)
        if (runnable != null) {
            activity.runOnUiThread {
                activity.window?.decorView?.removeCallbacks(runnable)
            }
        }
    }

    private fun schedulePrompt(activity: FragmentActivity, trigger: RateTrigger, delayMs: Long) {
        if (dialogVisible) {
            return
        }

        cancelScheduledPrompt(activity)

        val runnable = Runnable {
            scheduledTasks.remove(activity)
            if (!activity.isFinishing && !activity.isDestroyed && shouldPrompt(activity)) {
                showRateDialog(activity, trigger)
            }
        }

        scheduledTasks[activity] = runnable
        activity.runOnUiThread {
            activity.window?.decorView?.postDelayed(runnable, delayMs)
        }
    }

    private fun showRateDialog(activity: FragmentActivity, trigger: RateTrigger) {
        if (dialogVisible) {
            return
        }

        dialogVisible = true

        val dialog = Dialog(activity)
        dialog.setCancelable(false)

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_rate_experience, null, false)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val prefs = getPrefs(activity)
        markPromptShown(prefs)

        val ratingBar = view.findViewById<RatingBar>(R.id.rate_experience_rating_bar)
        val closeButton = view.findViewById<ImageButton>(R.id.rate_experience_close)
        val submitButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_submit_rating)
        val maybeLaterButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_maybe_later)

        // 设置默认5星
        ratingBar.rating = 5f

        // Submit Rating 按钮点击
        submitButton?.setOnClickListener {
            val rating = ratingBar.rating
            if (rating > 0) {
                handleRating(activity, dialog, rating)
            }
        }

        // Maybe Later 按钮点击
        maybeLaterButton?.setOnClickListener {
            markDismissed(activity)
            dialog.dismiss()
        }

        // 关闭按钮点击
        closeButton?.setOnClickListener {
            markDismissed(activity)
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            dialogVisible = false
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun handleRating(activity: FragmentActivity, dialog: Dialog, rating: Float) {
        val prefs = getPrefs(activity)

        if (rating >= 4f) {
            prefs.edit()
                .putBoolean(KEY_HAS_HIGH_RATED, true)
                .apply()
            dialog.dismiss()
            openPlayStore(activity)
        } else {
            markDismissed(activity)
            dialog.dismiss()
            showFeedbackDialog(activity)
        }
    }

    private fun showFeedbackDialog(activity: FragmentActivity) {
        val dialog = Dialog(activity)
        dialog.setCancelable(false)

        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_rate_feedback, null, false)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val closeButton = view.findViewById<ImageButton>(R.id.feedback_close)
        val submitButton = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_submit_feedback)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.feedback_input_layout)
        val editText = view.findViewById<TextInputEditText>(R.id.feedback_input)

        // 自动聚焦到输入框
        editText?.requestFocus()
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        // Submit 按钮点击
        submitButton?.setOnClickListener {
            val message = editText?.text?.toString()?.trim().orEmpty()
            if (message.isNotEmpty()) {
                sendFeedbackEmail(activity, message)
                dialog.dismiss()
            } else {
                inputLayout?.error = "Please enter your feedback"
            }
        }

        // 关闭按钮点击
        closeButton?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun sendFeedbackEmail(activity: FragmentActivity, message: String) {
        val recipientEmail = "lecheng2019@gmail.com"
        val subject = activity.getString(R.string.rate_prompt_email_subject)
        
        // 使用 mailto URI 构建完整的邮件链接（更好的兼容性）
        val uriText = "mailto:$recipientEmail" +
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(message)
        
        val uri = Uri.parse(uriText)
        val intent = Intent(Intent.ACTION_SENDTO, uri)
        
        // 添加额外的 EXTRA，确保各种邮件客户端都能识别
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, message)
        
        Log.d("RatePromptManager", "📧 Sending feedback email with message: $message")

        try {
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
                Log.d("RatePromptManager", "✅ Email intent launched successfully")
            } else {
                Toast.makeText(activity, R.string.rate_prompt_feedback_email_error, Toast.LENGTH_LONG).show()
                Log.e("RatePromptManager", "❌ No email app found")
            }
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.rate_prompt_feedback_email_error, Toast.LENGTH_LONG).show()
            Log.e("RatePromptManager", "❌ Failed to launch email intent", e)
        }
    }

    private fun openPlayStore(activity: FragmentActivity) {
        val packageName = activity.packageName
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (e: ActivityNotFoundException) {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    private fun shouldPrompt(activity: FragmentActivity): Boolean {
        if (dialogVisible) {
            return false
        }

        val prefs = getPrefs(activity)
        if (prefs.getBoolean(KEY_HAS_HIGH_RATED, false)) {
            return false
        }

        val now = System.currentTimeMillis()
        val lastDismiss = prefs.getLong(KEY_LAST_DISMISS_TIME, 0L)
        if (lastDismiss > 0 && now - lastDismiss < COOLDOWN_MILLIS) {
            return false
        }

        val today = getToday()
        val storedDate = prefs.getString(KEY_DAILY_PROMPT_DATE, null)
        var dailyCount = prefs.getInt(KEY_DAILY_PROMPT_COUNT, 0)

        if (storedDate == null || storedDate != today) {
            prefs.edit()
                .putString(KEY_DAILY_PROMPT_DATE, today)
                .putInt(KEY_DAILY_PROMPT_COUNT, 0)
                .apply()
            dailyCount = 0
        }

        if (dailyCount >= 1) {
            return false
        }

        return true
    }

    private fun markPromptShown(prefs: SharedPreferences) {
        val editor = prefs.edit()
        editor.putLong(KEY_LAST_PROMPT_TIME, System.currentTimeMillis())
        editor.putString(KEY_DAILY_PROMPT_DATE, getToday())
        editor.putInt(KEY_DAILY_PROMPT_COUNT, 1)
        editor.apply()
    }

    private fun markDismissed(activity: FragmentActivity) {
        getPrefs(activity).edit().putLong(KEY_LAST_DISMISS_TIME, System.currentTimeMillis()).apply()
    }

    private fun getToday(): String {
        return dateFormat.format(Date())
    }

    private fun isNextDay(previousDay: String, today: String): Boolean {
        return try {
            val calendar = Calendar.getInstance()
            val previousDate = dateFormat.parse(previousDay)
            val todayDate = dateFormat.parse(today)
            if (previousDate == null || todayDate == null) {
                false
            } else {
                calendar.time = previousDate
                calendar.add(Calendar.DATE, 1)
                val expected = dateFormat.format(calendar.time)
                expected == today
            }
        } catch (e: ParseException) {
            Log.w("RatePromptManager", "Failed to parse date for consecutive usage", e)
            false
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}


