package com.quran.quranaudio.quiz.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.quran.quranaudio.quiz.extension.logd
import com.quran.quranaudio.quiz.databinding.DialogLoadingBinding

class LoadingDialog(context: Context, loadMsg: String = "") : MyComponentDialog(context) {

    private val TAG = "LoadingDialog"
    val binding = DialogLoadingBinding.inflate(LayoutInflater.from(context))
    init {
        logd("init",TAG)
        setContentView(binding.root)
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        binding.loadTv.text = loadMsg
    }
}