/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
package com.quran.quranaudio.online.quran_module.interfaceUtils.readerIndex

import android.content.Context

interface FragReaderIndexCallback {
    fun scrollToTop(smooth: Boolean)
    fun sort(ctx: Context)
}