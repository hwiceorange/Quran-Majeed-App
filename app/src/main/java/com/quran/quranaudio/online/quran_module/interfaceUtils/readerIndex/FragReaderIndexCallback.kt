
package com.quran.quranaudio.online.quran_module.interfaceUtils.readerIndex

import android.content.Context

interface FragReaderIndexCallback {
    fun scrollToTop(smooth: Boolean)
    fun sort(ctx: Context)
}