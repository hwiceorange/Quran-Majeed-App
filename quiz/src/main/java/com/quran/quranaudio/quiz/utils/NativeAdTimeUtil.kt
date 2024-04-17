package com.quran.quranaudio.quiz.utils

import android.util.Log
import com.quran.quranaudio.quiz.base.CloudManager

object NativeAdTimeUtil {
    private val TAG = "NativeAdTimeUtil"

    private val showTimeMap = hashMapOf<String, Long>()

    fun isIntercept(functionTag: String):Boolean {
        val refreshTime = CloudManager.getNativeIntervalTime()
        val lastTime = showTimeMap[functionTag] ?: return false
        val offsetTime = System.currentTimeMillis() - lastTime
        Log.d(TAG, "isIntercept: showTimeMap = $showTimeMap, refreshTime = $refreshTime, interval time = $offsetTime")
        return offsetTime < refreshTime
    }

    fun saveTime(functionTag: String, time:Long) {
        showTimeMap[functionTag] = time
    }
}