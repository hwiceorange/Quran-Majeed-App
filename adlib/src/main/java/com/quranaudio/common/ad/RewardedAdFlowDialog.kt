package com.quranaudio.common.ad

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.quran.quranaudio.common.ad.R

internal class RewardedAdFlowDialog(
    context: Context,
    private val rewardDescription: String,
    private val onRetry: () -> Unit,
    private val onUserCancel: () -> Unit
) : Dialog(context) {

    private lateinit var progress: ProgressBar
    private lateinit var title: TextView
    private lateinit var message: TextView
    private lateinit var retry: MaterialButton
    private var notifyingCancellation = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_rewarded_ad_flow)
        setCancelable(true)
        setCanceledOnTouchOutside(true)

        progress = findViewById(R.id.rewardedFlowProgress)
        title = findViewById(R.id.rewardedFlowTitle)
        message = findViewById(R.id.rewardedFlowMessage)
        retry = findViewById(R.id.rewardedFlowRetry)
        retry.setOnClickListener { onRetry() }

        setOnCancelListener {
            if (notifyingCancellation) onUserCancel()
        }
        showLoading()
    }

    override fun onStart() {
        super.onStart()
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                dimAmount = 0.56f
            }
        }
    }

    fun showLoading() {
        notifyingCancellation = true
        if (!::progress.isInitialized) return
        progress.visibility = android.view.View.VISIBLE
        retry.visibility = android.view.View.GONE
        title.setText(R.string.rewarded_flow_preparing)
        message.text = if (rewardDescription.isBlank()) {
            context.getString(R.string.rewarded_flow_cancel_hint)
        } else {
            context.getString(R.string.rewarded_flow_reward_message, rewardDescription)
        }
    }

    fun showUnavailable() {
        notifyingCancellation = true
        if (!::progress.isInitialized) return
        progress.visibility = android.view.View.GONE
        retry.visibility = android.view.View.VISIBLE
        title.setText(R.string.rewarded_flow_unavailable)
        message.setText(R.string.rewarded_flow_retry_message)
    }

    fun dismissForAd() {
        notifyingCancellation = false
        dismiss()
    }
}
