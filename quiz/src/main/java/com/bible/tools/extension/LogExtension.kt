package com.bible.tools.extension

import android.util.Log
import com.quran.quranaudio.quiz.BuildConfig

fun logdt(msg: String, tag: String = "bible_app") {
    if (BuildConfig.DEBUG) {
        Log.d(tag, "[${Thread.currentThread().name}] $msg")
    }
}

fun logd(msg: String, tag: String = "bible_app") {
    if (BuildConfig.DEBUG) {
        Log.d(tag, msg)
    }
}

fun loge(msg: String, tag: String = "bible_app") {
    if (BuildConfig.DEBUG) {
        Log.e(tag, msg)
    }
}
