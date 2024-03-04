package com.raiadnan.quranreader.Data.Repository

import com.raiadnan.quranreader.Data.database.QuranDao
import com.raiadnan.quranreader.Data.database.entities.Chapter

class ChapterRepository(private val quranDao: QuranDao) {

    fun getChapters(categoryId:Int):List<Chapter>{
        return quranDao.getChapters(categoryId)
    }
}