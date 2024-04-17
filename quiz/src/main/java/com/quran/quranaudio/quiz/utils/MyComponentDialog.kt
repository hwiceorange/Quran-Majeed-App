package com.quran.quranaudio.quiz.utils

import android.app.Dialog
import android.content.Context
import android.view.View
import androidx.annotation.StyleRes
import com.quran.quranaudio.quiz.R

open class MyComponentDialog @JvmOverloads constructor(
    context: Context,
    @StyleRes themeResId: Int = 0
) : Dialog(context, themeResId) {
    fun setDialogContentView(view: View){
        super.setContentView(view)
        val layoutParams = window!!.attributes
        layoutParams.width =
            (context.resources.displayMetrics.widthPixels - context.resources.getDimension(
                R.dimen.dp_48
            )).toInt()
        window!!.setDimAmount(0.3f)
    }
}