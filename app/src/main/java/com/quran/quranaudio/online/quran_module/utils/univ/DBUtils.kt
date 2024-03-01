
package com.quran.quranaudio.online.quran_module.utils.univ

object DBUtils {
    @JvmStatic
    fun createDBSelection(vararg cols: String?): String {
        val selection = StringBuilder()
        val l = cols.size
        val lastIndex = l - 1
        for (i in 0 until l) {
            val col = cols[i]
            selection.append(col).append("=?")
            if (i < lastIndex) {
                selection.append(" AND ")
            }
        }
        return selection.toString()
    }
}
