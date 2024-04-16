package com.bible.tools.utils

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

object Tasks {
    private var sMainHandler: Handler
    private val sThreadHandler: Handler

    init {
        sMainHandler = Handler(Looper.getMainLooper())
        val t = HandlerThread("daemon-handler-thread")
        t.start()
        sThreadHandler = Handler(t.getLooper())
    }


    /**
     * @param r
     * @return
     */
    fun postByUI(r: Runnable): Boolean {
        return sMainHandler.post(r)
    }

    /**
     * @param r
     * @param delayMillis
     * @return
     */
    fun postDelayedByUI(r: Runnable, delayMillis: Long): Boolean {
        return sMainHandler.postDelayed(
            r,
            delayMillis
        )
    }

    /**
     * 取消UI线程任务
     *
     * @param r
     */
    fun cancelUITask(r: Runnable) {
       sMainHandler.removeCallbacks(r)
    }

    /**
     * @param r
     * @return
     */
    fun postByThread(r: Runnable): Boolean {
        return sThreadHandler.post(r)
    }

    /**
     * @param r
     * @param delayMillis
     * @return
     */
    fun postDelayedByThread(r: Runnable, delayMillis: Long): Boolean {
        return sThreadHandler.postDelayed(
            r,
            delayMillis
        )
    }

    /**
     * 取消后台线程任务
     *
     * @param r
     */
    fun cancelThreadTask(r: Runnable) {
        if (r != null) {
            sThreadHandler.removeCallbacks(r)
        }
    }

    fun removeAllTask(){
        sMainHandler.removeCallbacksAndMessages(null)
    }
}