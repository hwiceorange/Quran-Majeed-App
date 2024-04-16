/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
package com.quran.quranaudio.online.quran_module.components.quran.subcomponents

import java.io.Serializable

class Footnote() : Serializable {
    @JvmField
    var chapterNo = 0

    @JvmField
    var verseNo = 0

    @JvmField
    var number = 0

    @JvmField
    var text = ""

    @JvmField
    var bookSlug = ""

    private constructor(footnote: Footnote) : this() {
        chapterNo = footnote.chapterNo
        verseNo = footnote.verseNo
        number = footnote.number
        text = footnote.text
        bookSlug = footnote.bookSlug
    }

    fun copy(): Footnote {
        return Footnote(this)
    }

    override fun toString(): String {
        return "Footnote{number=$chapterNo:$verseNo:$number, text='$text'}"
    }
}
