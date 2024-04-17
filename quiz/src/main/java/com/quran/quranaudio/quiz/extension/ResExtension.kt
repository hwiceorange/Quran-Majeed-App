package com.quran.quranaudio.quiz.extension

import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.quran.quranaudio.quiz.base.BaseApp

fun Int.getResString(): String {
    return BaseApp.instance!!.resources!!.getString(this)
}

fun Int.getResString(formatArgs: String): String {

    return BaseApp.instance!!.resources!!.getString(this, formatArgs)
}

fun Int.getResString(formatArgs1: String, formatArgs2: String): String {
    return BaseApp.instance!!.resources!!.getString(this, formatArgs1, formatArgs2)
}

fun Int.getResColor():Int{
    return ContextCompat.getColor(BaseApp.instance!!, this)
}

fun Int.getResDrawable(): Drawable?{
    return ContextCompat.getDrawable(BaseApp.instance!!, this)
}
fun Int.getDimension(): Float{
    return BaseApp.instance!!.resources!!.getDimension(this)
}