package com.bible.tools.utils

import com.bible.tools.base.Constants
import com.bible.tools.extension.SPTools

object UserInfoUtils {
    fun isNewUser(): Boolean = SPTools.getBoolean(Constants.KEY_IS_NEW_USER, true)
    fun setOldUser() {
        SPTools.put(Constants.KEY_IS_NEW_USER, false)
    }

}