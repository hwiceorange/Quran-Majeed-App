package com.quran.quranaudio.quiz.utils

import com.quran.quranaudio.quiz.base.CloudManager

object AbTestUtil {
    const val FloatWindowOriginName = "bible_style"
    const val FloatWindowTestName = "universal_style"
    var remoteFloatWindowName = ""
    fun isFloatWindowTestB(): Boolean {
        if (remoteFloatWindowName.isEmpty()) {
            remoteFloatWindowName = CloudManager.getFloatWindowTestGroupName()
        }
        return FloatWindowTestName == remoteFloatWindowName
    }
}