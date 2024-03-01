package com.quran.quranaudio.online.quran_module.utils.exceptions

import android.content.Context
import com.quran.quranaudio.online.quran_module.utils.Log
import com.quran.quranaudio.online.quran_module.utils.app.NotificationUtils
import org.apache.commons.lang3.exception.ExceptionUtils

class CustomExceptionHandler(
    private val ctx: Context
) : Thread.UncaughtExceptionHandler {
    private val defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, exc: Throwable) {
        Log.saveCrash(ctx, exc)
        NotificationUtils.showCrashNotification(ctx, ExceptionUtils.getStackTrace(exc))
        defaultExceptionHandler?.uncaughtException(thread, exc)
    }
}
