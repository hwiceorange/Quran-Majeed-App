package com.quran.quranaudio.quiz.utils

import com.quran.quranaudio.quiz.base.Constants
import com.quran.quranaudio.quiz.extension.SPTools

object UserInfoUtils {
    fun isNewUser(): Boolean = SPTools.getBoolean(Constants.KEY_IS_NEW_USER, true)
    fun setOldUser() {
        SPTools.put(Constants.KEY_IS_NEW_USER, false)
    }

}