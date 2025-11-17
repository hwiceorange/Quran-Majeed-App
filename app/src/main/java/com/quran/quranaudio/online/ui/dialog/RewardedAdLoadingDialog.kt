package com.quran.quranaudio.online.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.DialogRewardedAdLoadingBinding

/**
 * 激励广告加载对话框
 * 
 * 功能：
 * 1. 显示Loading动画
 * 2. 5秒倒计时
 * 3. 广告未准备好时显示错误提示和重试按钮
 * 4. 5秒后显示关闭按钮
 */
class RewardedAdLoadingDialog(
    context: Context,
    private val onAdReady: () -> Unit,
    private val onRetry: () -> Unit,
    private val onDismiss: () -> Unit
) : Dialog(context, R.style.Theme_AppCompat_Dialog) {

    private val binding: DialogRewardedAdLoadingBinding
    private var countDownTimer: CountDownTimer? = null
    private var remainingSeconds = 5

    companion object {
        private const val TAG = "RewardedAdLoading"
        private const val COUNTDOWN_DURATION_MS = 5000L // 5 seconds
        private const val COUNTDOWN_INTERVAL_MS = 1000L // 1 second
    }

    init {
        binding = DialogRewardedAdLoadingBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        
        initViews()
        startCountdown()
    }

    private fun initViews() {
        // 关闭按钮（初始隐藏）
        binding.btnClose.setOnClickListener {
            Log.d(TAG, "Close button clicked")
            dismissAndCallback()
        }

        // 重试按钮
        binding.btnRetry.setOnClickListener {
            Log.d(TAG, "Retry button clicked")
            resetToLoading()
            onRetry()
        }
    }

    /**
     * 开始倒计时
     */
    private fun startCountdown() {
        countDownTimer?.cancel()
        
        countDownTimer = object : CountDownTimer(COUNTDOWN_DURATION_MS, COUNTDOWN_INTERVAL_MS) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt() + 1
                updateCountdownText()
                Log.d(TAG, "Countdown: $remainingSeconds seconds remaining")
            }

            override fun onFinish() {
                remainingSeconds = 0
                updateCountdownText()
                showCloseButton()
                Log.d(TAG, "Countdown finished, showing close button")
            }
        }.start()
    }

    /**
     * 更新倒计时文本
     */
    private fun updateCountdownText() {
        if (remainingSeconds > 0) {
            binding.tvCountdown.text = context.getString(
                R.string.please_wait_seconds,
                remainingSeconds
            )
        } else {
            binding.tvCountdown.text = context.getString(R.string.you_can_close_now)
        }
    }

    /**
     * 显示关闭按钮（5秒后）
     */
    private fun showCloseButton() {
        binding.btnClose.visibility = View.VISIBLE
    }

    /**
     * 广告已准备好，通知并关闭
     */
    fun onAdReadyToShow() {
        Log.d(TAG, "Ad ready to show")
        countDownTimer?.cancel()
        dismiss()
        onAdReady()
    }

    /**
     * 广告加载失败，显示错误提示
     */
    fun showAdNotReadyError() {
        Log.d(TAG, "Ad not ready, showing error message")
        
        countDownTimer?.cancel()
        
        // 隐藏加载UI
        binding.progressBar.visibility = View.GONE
        binding.tvTitle.visibility = View.GONE
        binding.tvCountdown.visibility = View.GONE
        
        // 显示错误UI
        binding.errorContainer.visibility = View.VISIBLE
        binding.btnClose.visibility = View.VISIBLE
    }

    /**
     * 重置到加载状态（用于重试）
     */
    private fun resetToLoading() {
        // 显示加载UI
        binding.progressBar.visibility = View.VISIBLE
        binding.tvTitle.visibility = View.VISIBLE
        binding.tvCountdown.visibility = View.VISIBLE
        
        // 隐藏错误UI
        binding.errorContainer.visibility = View.GONE
        binding.btnClose.visibility = View.GONE
        
        // 重新开始倒计时
        remainingSeconds = 5
        startCountdown()
    }

    /**
     * 关闭对话框并回调
     */
    private fun dismissAndCallback() {
        countDownTimer?.cancel()
        dismiss()
        onDismiss()
    }

    override fun onStop() {
        super.onStop()
        countDownTimer?.cancel()
    }

    override fun dismiss() {
        countDownTimer?.cancel()
        super.dismiss()
    }
}

