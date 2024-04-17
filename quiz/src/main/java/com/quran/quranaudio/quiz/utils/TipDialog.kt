package com.quran.quranaudio.quiz.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.LinearLayout
import com.quran.quranaudio.quiz.extension.getResString
import com.quran.quranaudio.quiz.extension.logd
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.databinding.DialogQuizTipBinding

class TipDialog(context: Context, cancel: () -> Unit, sure: () -> Unit) :
    MyComponentDialog(context) {

    private val TAG = "DeleteMarkDialog"
    val binding = DialogQuizTipBinding.inflate(LayoutInflater.from(context))

    init {
        logd("init", TAG)
        setContentView(binding.root)
        val layoutParams = window!!.attributes as WindowManager.LayoutParams
        layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
        window!!.attributes = layoutParams
        window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        binding.cancelTv.setOnClickListener {
            dismiss()
            cancel.invoke()
        }
        binding.sureTv.setOnClickListener {
            dismiss()
            sure.invoke()
        }
    }

    fun show(msg:String,title:String = R.string.bible_tip.getResString()) {
        binding.msgTv.text = msg
        binding.titleTv.text = title
        super.show()
    }
}