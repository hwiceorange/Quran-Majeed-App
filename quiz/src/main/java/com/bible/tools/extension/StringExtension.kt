package com.bible.tools.extension

import android.util.Log

fun String.firstUpper():String{
    return this.replaceFirstChar(Char::uppercaseChar)
}

fun formatTime(hour:Int,minute:Int):String{
    val hourStr = if (hour < 10) {
        "0$hour"
    } else {
        hour.toString()
    }
    val minuteStr = if (minute < 10) {
        "0$minute"
    } else {
        minute.toString()
    }
    return "$hourStr:$minuteStr"
}

fun String.logi(){
    Log.i("QURAN",this)
}