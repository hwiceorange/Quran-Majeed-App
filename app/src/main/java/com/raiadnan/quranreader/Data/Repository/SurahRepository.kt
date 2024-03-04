package com.raiadnan.quranreader.Data.Repository

import com.raiadnan.quranreader.Data.database.QuranDao
import com.raiadnan.quranreader.Data.database.entities.Surah

class SurahRepository(private val quranDao: QuranDao) {

    fun getAllSurahs(): List<Surah> {
        return quranDao.getSurahs()
    }

    fun getSurahByNumber(surahNumber: Int): Surah = quranDao.getSurahByNumber(surahNumber)

    fun getListSurahs(surahNumber: Int): List<Surah> = quranDao.getSurahList(surahNumber)
}