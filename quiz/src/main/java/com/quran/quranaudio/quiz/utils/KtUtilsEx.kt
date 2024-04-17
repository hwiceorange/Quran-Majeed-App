package com.quran.quranaudio.quiz.utils

import com.quran.quranaudio.quiz.BuildConfig


fun isDebug(): Boolean {
    return BuildConfig.DEBUG
}

/**
 * 控制是否使用测试广告
 */
fun useTestAD() : Boolean {
    return BuildConfig.DEBUG
}