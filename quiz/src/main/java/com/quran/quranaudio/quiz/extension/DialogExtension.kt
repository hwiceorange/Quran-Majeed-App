package com.quran.quranaudio.quiz.extension

import android.content.Context
import com.quran.quranaudio.quiz.utils.LoadingDialog

fun showLoadingDialog(context: Context): LoadingDialog {
    return LoadingDialog(context).apply { show() }
}

fun hideLoadingDialog(dialog: LoadingDialog?){
    dialog?.dismiss()
}